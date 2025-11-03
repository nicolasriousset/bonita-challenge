# ========================================
# Installation complète avec nettoyage du cache Bonita
# ========================================

param(
    [string]$BonitaStudioPath = "C:\BonitaStudioCommunity-2024.3-u0",
    [string]$ProjectName = "getting-started-tutorial2"
)

Write-Host "`n🧹 Nettoyage et Installation du Connecteur AI Agent" -ForegroundColor Cyan
Write-Host "===================================================`n" -ForegroundColor Cyan

# 1. Vérifier que Bonita Studio est fermé
Write-Host "📋 Étape 1/5 : Vérification..." -ForegroundColor Yellow

$bonitaProcess = Get-Process | Where-Object { $_.ProcessName -like "*bonita*" -or $_.ProcessName -like "*eclipse*" }
if ($bonitaProcess) {
    Write-Host "  ⚠️  ATTENTION : Bonita Studio semble être en cours d'exécution !" -ForegroundColor Red
    Write-Host "     Processus détectés : $($bonitaProcess.ProcessName -join ', ')" -ForegroundColor Yellow
    Write-Host "     FERMEZ Bonita Studio avant de continuer." -ForegroundColor Yellow
    
    $response = Read-Host "`n  Continuer quand même ? (y/N)"
    if ($response -ne 'y' -and $response -ne 'Y') {
        Write-Host "  ❌ Installation annulée" -ForegroundColor Red
        exit 1
    }
}
Write-Host "  ✅ Bonita Studio n'est pas en cours d'exécution" -ForegroundColor Green

# 2. Nettoyer le cache Eclipse/Bonita
Write-Host "`n🧹 Étape 2/5 : Nettoyage du cache..." -ForegroundColor Yellow

$workspacePath = Join-Path $BonitaStudioPath "workspace\$ProjectName"
$metadataPath = Join-Path $workspacePath ".metadata\.plugins\org.eclipse.core.runtime\.settings"

if (Test-Path $metadataPath) {
    Write-Host "  Suppression du cache Eclipse..." -ForegroundColor Gray
    Remove-Item "$metadataPath\org.bonitasoft.studio.*" -Force -ErrorAction SilentlyContinue
    Write-Host "  ✅ Cache nettoyé" -ForegroundColor Green
} else {
    Write-Host "  ℹ️  Pas de cache à nettoyer" -ForegroundColor Gray
}

# 3. Supprimer l'ancienne installation du connecteur
Write-Host "`n🗑️  Étape 3/5 : Suppression de l'ancienne version..." -ForegroundColor Yellow

$connectorDir = Join-Path $workspacePath "connectors-dev\ai-agent-connector"
if (Test-Path $connectorDir) {
    Remove-Item $connectorDir -Recurse -Force
    Write-Host "  ✅ Ancienne version supprimée" -ForegroundColor Green
} else {
    Write-Host "  ℹ️  Pas d'ancienne version" -ForegroundColor Gray
}

# 4. Compiler et installer le connecteur
Write-Host "`n🔨 Étape 4/5 : Compilation et installation..." -ForegroundColor Yellow

$projectRoot = $PSScriptRoot
$connectorProject = Join-Path $projectRoot "bonita-connector-ai-agent"

Push-Location $connectorProject
mvn clean package -DskipTests -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "  ❌ Erreur de compilation" -ForegroundColor Red
    Pop-Location
    exit 1
}
Pop-Location

# Créer la structure
$defDir = Join-Path $connectorDir "definition"
$implDir = Join-Path $connectorDir "implementation"
New-Item -ItemType Directory -Path $defDir -Force | Out-Null
New-Item -ItemType Directory -Path $implDir -Force | Out-Null

# Copier les fichiers
$jarFile = Join-Path $connectorProject "target\bonita-connector-ai-agent-1.0.0-SNAPSHOT-jar-with-dependencies.jar"
$defFile = Join-Path $connectorProject "target\classes\ai-agent-connector.def"

Copy-Item -Path $defFile -Destination "$defDir\ai-agent-connector.def" -Force
Copy-Item -Path $jarFile -Destination "$implDir\bonita-connector-ai-agent-1.0.0-SNAPSHOT-impl.jar" -Force

# Créer le fichier .impl
$implContent = @"
<?xml version="1.0" encoding="UTF-8"?>
<implementation:connectorImplementation xmlns:implementation="http://www.bonitasoft.org/ns/connector/implementation/6.0">
    <implementationId>ai-agent-connector-impl</implementationId>
    <implementationVersion>1.0.0-SNAPSHOT</implementationVersion>
    <definitionId>ai-agent-connector</definitionId>
    <definitionVersion>1.0.0</definitionVersion>
    <implementationClassname>com.bonitasoft.connector.aiagent.AIAgentConnector</implementationClassname>
    <jarDependencies>
        <jarDependency>bonita-connector-ai-agent-1.0.0-SNAPSHOT-impl.jar</jarDependency>
    </jarDependencies>
</implementation:connectorImplementation>
"@

Set-Content -Path "$implDir\ai-agent-connector.impl" -Value $implContent -Encoding UTF8

Write-Host "  ✅ Connecteur installé" -ForegroundColor Green

# 5. Créer un fichier de propriétés (optionnel pour Bonita)
Write-Host "`n📝 Étape 5/5 : Création des métadonnées..." -ForegroundColor Yellow

$propertiesContent = @"
ai-agent-connector.name=AI Agent Connector
ai-agent-connector.description=Generic connector for AI agents via HTTP
ai-agent-connector.category=AI
"@

Set-Content -Path "$defDir\ai-agent-connector.properties" -Value $propertiesContent -Encoding UTF8
Write-Host "  ✅ Métadonnées créées" -ForegroundColor Green

# Résumé final
Write-Host "`n🎉 Installation terminée avec succès !" -ForegroundColor Green
Write-Host "===================================`n" -ForegroundColor Green

Write-Host "📂 Structure installée :" -ForegroundColor Cyan
tree /F $connectorDir

Write-Host "`n⚠️  ÉTAPES SUIVANTES - IMPORTANT :" -ForegroundColor Yellow
Write-Host "`n   1️⃣  Démarrez le RAG Agent :" -ForegroundColor White
Write-Host "      cd rag-agent-java" -ForegroundColor Gray
Write-Host "      java -jar target\rag-agent-1.0.0-SNAPSHOT.jar`n" -ForegroundColor Gray

Write-Host "   2️⃣  Ouvrez Bonita Studio" -ForegroundColor White
Write-Host "      → Si déjà ouvert, faites : Help > Restart`n" -ForegroundColor Gray

Write-Host "   3️⃣  Cherchez le connecteur :" -ForegroundColor White
Write-Host "      → Clic droit sur une tâche" -ForegroundColor Gray
Write-Host "      → Connectors > Add connector..." -ForegroundColor Gray
Write-Host "      → Cherchez 'AI Agent Connector' dans la catégorie 'AI'`n" -ForegroundColor Gray

Write-Host "   📌 SI LE CONNECTEUR N'APPARAÎT TOUJOURS PAS :" -ForegroundColor Red
Write-Host "      → Vérifiez les logs : Window > Show View > Error Log" -ForegroundColor Yellow
Write-Host "      → Essayez : Project > Clean...`n" -ForegroundColor Yellow

Write-Host "✨ Le connecteur devrait maintenant être visible ! ✨`n" -ForegroundColor Cyan
