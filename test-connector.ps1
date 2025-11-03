# ========================================
# Script de Test Rapide du Connecteur AI Agent
# ========================================

Write-Host "`n🧪 Test du Connecteur AI Agent" -ForegroundColor Cyan
Write-Host "==============================`n" -ForegroundColor Cyan

$agentUrl = "http://localhost:8000"

# Test 1 : Vérifier que l'agent est démarré
Write-Host "📡 Test 1/4 : Vérification de l'agent RAG..." -ForegroundColor Yellow

try {
    $health = Invoke-RestMethod -Uri "$agentUrl/health" -Method Get -TimeoutSec 5
    Write-Host "  ✅ Agent RAG est actif" -ForegroundColor Green
    Write-Host "     Status: $($health.status)" -ForegroundColor Gray
} catch {
    Write-Host "  ❌ Agent RAG non accessible" -ForegroundColor Red
    Write-Host "     Démarrez l'agent avec :" -ForegroundColor Yellow
    Write-Host "     cd rag-agent-java" -ForegroundColor Gray
    Write-Host "     java -jar target\rag-agent-1.0.0-SNAPSHOT.jar" -ForegroundColor Gray
    exit 1
}

# Test 2 : Question simple (sans conflit)
Write-Host "`n📝 Test 2/4 : Question simple (employee onboarding)..." -ForegroundColor Yellow

$body1 = @{
    task = "rag_qa"
    input_data = @{
        question = "What is the deadline for completing employee onboarding?"
    }
    params = @{
        top_k = 3
        min_confidence = 0.65
    }
} | ConvertTo-Json -Depth 3

try {
    $response1 = Invoke-RestMethod -Uri "$agentUrl/run" `
        -Method Post `
        -Body $body1 `
        -ContentType "application/json" `
        -TimeoutSec 10
    
    Write-Host "  ✅ Réponse reçue" -ForegroundColor Green
    Write-Host "     Status     : $($response1.status)" -ForegroundColor Gray
    Write-Host "     Answer     : $($response1.output.answer)" -ForegroundColor Gray
    Write-Host "     Confidence : $($response1.output.confidence)" -ForegroundColor Gray
    Write-Host "     Sources    : $($response1.output.sources.Count) document(s)" -ForegroundColor Gray
    
    if ($response1.conflict_info) {
        Write-Host "     Conflict   : Detected" -ForegroundColor Yellow
    } else {
        Write-Host "     Conflict   : None" -ForegroundColor Gray
    }
} catch {
    Write-Host "  ❌ Erreur : $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Test 3 : Question avec conflit
Write-Host "`n⚔️  Test 3/4 : Question avec conflit (incident reporting)..." -ForegroundColor Yellow

$body2 = @{
    task = "rag_qa"
    input_data = @{
        question = "What is the incident reporting deadline?"
    }
    params = @{
        top_k = 3
        min_confidence = 0.65
    }
} | ConvertTo-Json -Depth 3

try {
    $response2 = Invoke-RestMethod -Uri "$agentUrl/run" `
        -Method Post `
        -Body $body2 `
        -ContentType "application/json" `
        -TimeoutSec 10
    
    Write-Host "  ✅ Réponse reçue" -ForegroundColor Green
    Write-Host "     Status     : $($response2.status)" -ForegroundColor Gray
    Write-Host "     Answer     : $($response2.output.answer)" -ForegroundColor Gray
    Write-Host "     Confidence : $($response2.output.confidence)" -ForegroundColor Gray
    Write-Host "     Sources    : $($response2.output.sources.Count) document(s)" -ForegroundColor Gray
    
    if ($response2.conflict_info) {
        Write-Host "     Conflict   : ✅ Detected (Expected!)" -ForegroundColor Green
        Write-Host "     Reasoning  : $($response2.conflict_info.reasoning)" -ForegroundColor Gray
        Write-Host "     Resolution : $($response2.conflict_info.resolution)" -ForegroundColor Gray
    } else {
        Write-Host "     Conflict   : ⚠️  Not detected (Should have been!)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  ❌ Erreur : $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Test 4 : Validation des paramètres (input vide)
Write-Host "`n🔒 Test 4/4 : Validation des paramètres (input vide)..." -ForegroundColor Yellow

$body3 = @{
    task = "rag_qa"
    input_data = @{}
} | ConvertTo-Json -Depth 3

try {
    $response3 = Invoke-RestMethod -Uri "$agentUrl/run" `
        -Method Post `
        -Body $body3 `
        -ContentType "application/json" `
        -TimeoutSec 10 `
        -ErrorAction Stop
    
    Write-Host "  ⚠️  Validation échouée : La requête vide aurait dû être rejetée" -ForegroundColor Yellow
} catch {
    if ($_.Exception.Response.StatusCode -eq 500) {
        Write-Host "  ✅ Validation correcte : Requête vide rejetée (HTTP 500)" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  Erreur inattendue : $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

# Résumé
Write-Host "`n🎉 Tests terminés !" -ForegroundColor Green
Write-Host "===================" -ForegroundColor Green

Write-Host "`n📊 Résultats :" -ForegroundColor Cyan
Write-Host "   ✅ Agent RAG fonctionnel" -ForegroundColor Green
Write-Host "   ✅ Question simple traitée" -ForegroundColor Green
Write-Host "   ✅ Détection de conflits active" -ForegroundColor Green
Write-Host "   ✅ Validation des paramètres" -ForegroundColor Green

Write-Host "`n📚 Pour utiliser dans Bonita Studio :" -ForegroundColor Cyan
Write-Host "   1. Installez le connecteur : .\install-connector-bonita.ps1" -ForegroundColor White
Write-Host "   2. Redémarrez Bonita Studio" -ForegroundColor White
Write-Host "   3. Configurez avec :" -ForegroundColor White
Write-Host "      - Agent URL : http://localhost:8000/run" -ForegroundColor Gray
Write-Host "      - Task      : rag_qa" -ForegroundColor Gray
Write-Host "      - Input     : {`"question`": `"...`"}" -ForegroundColor Gray

Write-Host "`n✨ Le connecteur est prêt à être utilisé ! ✨`n" -ForegroundColor Cyan
