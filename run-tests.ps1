# Test Automation Script
# This script runs all validation steps for the challenge

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  Bonita AI Agent Connector - Test Suite" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

$originalLocation = Get-Location
$projectRoot = $PSScriptRoot
$agentPath = Join-Path $projectRoot "rag-agent-java"
$connectorPath = Join-Path $projectRoot "bonita-connector-ai-agent"

$errors = @()

# Step 1: Check Java
Write-Host "[1/4] Checking Java..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    Write-Host "  ✓ Found: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Java not found!" -ForegroundColor Red
    $errors += "Java is required"
}

# Step 2: Check Maven
Write-Host "[2/4] Checking Maven..." -ForegroundColor Yellow
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
Write-Host "  Starting RAG Agent (Java/Spring Boot)" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# Step 3: Build and Start RAG Agent
Write-Host "[3/4] Building RAG Agent..." -ForegroundColor Yellow
Set-Location $agentPath
mvn clean package -DskipTests -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "  ✗ Build failed!" -ForegroundColor Red
    Set-Location $originalLocation
    exit 1
}
Write-Host "  ✓ Build successful" -ForegroundColor Green

Write-Host "[3/4] Starting RAG Agent..." -ForegroundColor Yellow
$agentJob = Start-Job -ScriptBlock {
    param($path)
    Set-Location $path
    java -jar target/rag-agent-1.0.0-SNAPSHOT.jar
} -ArgumentList $agentPath

Write-Host "  ⏳ Waiting for agent to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 8

# Check if agent is running
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8000/health" -Method Get -TimeoutSec 5
    if ($response.status -eq "ok") {
        Write-Host "  ✓ Agent running at http://localhost:8000" -ForegroundColor Green
    } else {
        throw "Unexpected response"
    }
} catch {
    Write-Host "  ✗ Agent failed to start!" -ForegroundColor Red
    Write-Host "  Error: $_" -ForegroundColor Red
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

# Step 4: Test agent with sample query
Write-Host "[4/4] Testing agent with sample query..." -ForegroundColor Yellow
try {
    $body = @{
        task = "rag_qa"
        input_data = @{
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

# Step 5: Run Maven tests
Write-Host "[4/4] Running integration tests..." -ForegroundColor Yellow
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
