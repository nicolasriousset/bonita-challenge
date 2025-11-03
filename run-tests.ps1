# Test Automation Script
# This script runs all validation steps for the challenge

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  Bonita AI Agent Connector - Test Suite" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

$originalLocation = Get-Location
$projectRoot = $PSScriptRoot
$agentPath = Join-Path $projectRoot "rag-agent"
$connectorPath = Join-Path $projectRoot "bonita-connector-ai-agent"

$errors = @()

# Step 1: Check Python
Write-Host "[1/6] Checking Python..." -ForegroundColor Yellow
try {
    $pythonVersion = python --version 2>&1
    Write-Host "  ✓ Found: $pythonVersion" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Python not found!" -ForegroundColor Red
    $errors += "Python is required"
}

# Step 2: Check Java
Write-Host "[2/6] Checking Java..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    Write-Host "  ✓ Found: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Java not found!" -ForegroundColor Red
    $errors += "Java is required"
}

# Step 3: Check Maven
Write-Host "[3/6] Checking Maven..." -ForegroundColor Yellow
try {
    $mavenVersion = mvn -version 2>&1 | Select-Object -First 1
    Write-Host "  ✓ Found: $mavenVersion" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Maven not found!" -ForegroundColor Red
    $errors += "Maven is required"
}

if ($errors.Count -gt 0) {
    Write-Host ""
    Write-Host "Prerequisites missing:" -ForegroundColor Red
    $errors | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
    Write-Host ""
    Write-Host "Please install missing prerequisites and try again." -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  Starting RAG Agent" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# Step 4: Start RAG Agent
Write-Host "[4/6] Installing Python dependencies..." -ForegroundColor Yellow
Set-Location $agentPath
pip install -r requirements.txt | Out-Null
Write-Host "  ✓ Dependencies installed" -ForegroundColor Green

Write-Host "[4/6] Starting RAG Agent..." -ForegroundColor Yellow
$agentJob = Start-Job -ScriptBlock {
    param($path)
    Set-Location $path
    python main.py
} -ArgumentList $agentPath

Write-Host "  ⏳ Waiting for agent to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

# Check if agent is running
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8000/" -TimeoutSec 5 -UseBasicParsing
    Write-Host "  ✓ Agent running at http://localhost:8000" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Agent failed to start!" -ForegroundColor Red
    Stop-Job $agentJob
    Remove-Job $agentJob
    Set-Location $originalLocation
    exit 1
}

Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  Testing RAG Agent" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# Step 5: Test agent with sample query
Write-Host "[5/6] Testing agent with sample query..." -ForegroundColor Yellow
try {
    $body = @{
        task = "rag_qa"
        input = @{
            question = "What is the deadline for completing employee onboarding?"
        }
        params = @{
            top_k = 3
            min_confidence = 0.65
        }
    } | ConvertTo-Json -Depth 3

    $response = Invoke-RestMethod -Uri "http://localhost:8000/run" -Method Post -Body $body -ContentType "application/json"
    
    if ($response.status -eq "ok") {
        Write-Host "  ✓ Agent responded successfully" -ForegroundColor Green
        Write-Host "    Answer: $($response.output.answer.Substring(0, [Math]::Min(60, $response.output.answer.Length)))..." -ForegroundColor Gray
        Write-Host "    Confidence: $($response.output.confidence)" -ForegroundColor Gray
    } else {
        Write-Host "  ⚠ Agent responded with status: $($response.status)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  ✗ Agent test failed: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  Running Connector Integration Tests" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# Step 6: Run Maven tests
Write-Host "[6/6] Running integration tests..." -ForegroundColor Yellow
Set-Location $connectorPath

$testResult = mvn test 2>&1
$testExitCode = $LASTEXITCODE

if ($testExitCode -eq 0) {
    Write-Host "  ✓ All tests passed!" -ForegroundColor Green
    
    # Extract test statistics
    $testStats = $testResult | Select-String "Tests run:"
    if ($testStats) {
        Write-Host "    $($testStats.Line.Trim())" -ForegroundColor Gray
    }
} else {
    Write-Host "  ✗ Some tests failed" -ForegroundColor Red
    Write-Host "    Check output above for details" -ForegroundColor Yellow
}

# Cleanup
Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  Cleanup" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Stopping RAG Agent..." -ForegroundColor Yellow
Stop-Job $agentJob
Remove-Job $agentJob
Write-Host "  ✓ Agent stopped" -ForegroundColor Green

Set-Location $originalLocation

Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  Test Suite Complete!" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

if ($testExitCode -eq 0) {
    Write-Host "✓ All validation steps passed!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Cyan
    Write-Host "  1. Review the test output above" -ForegroundColor White
    Write-Host "  2. Check AI_USAGE_REPORT.md for AI tool usage details" -ForegroundColor White
    Write-Host "  3. See PROJECT_SUMMARY.md for complete challenge status" -ForegroundColor White
    Write-Host "  4. Read QUICKSTART.md for manual testing" -ForegroundColor White
    exit 0
} else {
    Write-Host "⚠ Some tests failed - review output above" -ForegroundColor Yellow
    exit 1
}
