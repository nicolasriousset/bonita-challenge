# ========================================
# Création d'un package ZIP pour import dans Bonita Studio
# ========================================

param(
    [string]$OutputDir = "."
)

Write-Host "`n📦 Création du package du Connecteur AI Agent" -ForegroundColor Cyan
Write-Host "============================================`n" -ForegroundColor Cyan

$projectRoot = $PSScriptRoot
$connectorProject = Join-Path $projectRoot "bonita-connector-ai-agent"
$tempDir = Join-Path $env:TEMP "ai-agent-connector-package"
$zipFile = Join-Path $OutputDir "ai-agent-connector-1.0.0.zip"

# Nettoyer le répertoire temporaire
if (Test-Path $tempDir) {
    Remove-Item $tempDir -Recurse -Force
}

Write-Host "🔨 Compilation du connecteur..." -ForegroundColor Yellow
Push-Location $connectorProject
mvn clean package -DskipTests -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Erreur de compilation" -ForegroundColor Red
    Pop-Location
    exit 1
}
Pop-Location
Write-Host "✅ Compilation réussie`n" -ForegroundColor Green

# Créer la structure du package
Write-Host "📁 Création de la structure du package..." -ForegroundColor Yellow
$defDir = Join-Path $tempDir "ai-agent-connector"
New-Item -ItemType Directory -Path $defDir -Force | Out-Null

# Copier le fichier .def
$defFile = Join-Path $connectorProject "target\classes\ai-agent-connector.def"
Copy-Item -Path $defFile -Destination $defDir -Force

# Copier le JAR
$jarFile = Join-Path $connectorProject "target\bonita-connector-ai-agent-1.0.0-SNAPSHOT-jar-with-dependencies.jar"
Copy-Item -Path $jarFile -Destination "$defDir\bonita-connector-ai-agent-1.0.0-SNAPSHOT-impl.jar" -Force

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

Set-Content -Path "$defDir\ai-agent-connector.impl" -Value $implContent -Encoding UTF8

Write-Host "✅ Structure créée`n" -ForegroundColor Green

# Créer le fichier ZIP
Write-Host "🗜️  Création du fichier ZIP..." -ForegroundColor Yellow
if (Test-Path $zipFile) {
    Remove-Item $zipFile -Force
}

Compress-Archive -Path "$tempDir\*" -DestinationPath $zipFile -Force
Write-Host "✅ ZIP créé : $zipFile`n" -ForegroundColor Green

# Nettoyer
Remove-Item $tempDir -Recurse -Force

# Afficher les instructions
Write-Host "🎉 Package créé avec succès !" -ForegroundColor Green
Write-Host "============================`n" -ForegroundColor Green

Write-Host "📦 Fichier créé :" -ForegroundColor Cyan
Write-Host "   $zipFile`n" -ForegroundColor Gray

Write-Host "📝 INSTRUCTIONS D'IMPORT DANS BONITA STUDIO :" -ForegroundColor Cyan
Write-Host "   1. Ouvrez Bonita Studio" -ForegroundColor White
Write-Host "   2. Allez dans : Development > Connectors > Import connector..." -ForegroundColor White
Write-Host "      OU" -ForegroundColor Yellow
Write-Host "      File > Import > Bonita Content > Connector..." -ForegroundColor White
Write-Host "   3. Sélectionnez le fichier ZIP :" -ForegroundColor White
Write-Host "      $zipFile" -ForegroundColor Gray
Write-Host "   4. Cliquez sur 'Import'" -ForegroundColor White
Write-Host "   5. Le connecteur devrait apparaître dans la catégorie 'AI'`n" -ForegroundColor White

Write-Host "🚀 N'oubliez pas de démarrer le RAG Agent :" -ForegroundColor Cyan
Write-Host "   cd rag-agent-java" -ForegroundColor Gray
Write-Host "   java -jar target\rag-agent-1.0.0-SNAPSHOT.jar`n" -ForegroundColor Gray

Write-Host "✨ Bonne utilisation ! ✨`n" -ForegroundColor Cyan
