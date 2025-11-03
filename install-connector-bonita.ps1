# ========================================
# Installation du Connecteur AI Agent dans Bonita Studio 2024.3
# ========================================

param(
    [string]$BonitaStudioPath = "C:\BonitaStudioCommunity-2024.3-u0",
    [string]$ProjectName = "getting-started-tutorial2",
    [string]$ConnectorVersion = "1.0.0-SNAPSHOT"
)

Write-Host "`n🚀 Installation du Connecteur AI Agent" -ForegroundColor Cyan

# Configuration
$projectRoot = $PSScriptRoot
$connectorProject = Join-Path $projectRoot "bonita-connector-ai-agent"
$workspacePath = Join-Path $BonitaStudioPath "workspace\$ProjectName"

Write-Host "📁 Workspace : $workspacePath" -ForegroundColor Gray

# Vérifier le workspace
if (-not (Test-Path $workspacePath)) {
    Write-Host "❌ Workspace non trouvé" -ForegroundColor Red
    exit 1
}

# Compiler le connecteur
Write-Host "`n🔨 Compilation..." -ForegroundColor Yellow
Push-Location $connectorProject
mvn clean package -DskipTests -q
Pop-Location

# Fichiers source
$jarFile = Join-Path $connectorProject "target\bonita-connector-ai-agent-$ConnectorVersion-jar-with-dependencies.jar"
$defFile = Join-Path $connectorProject "target\classes\ai-agent-connector.def"

# Structure connectors-dev
$connectorDir = Join-Path $workspacePath "connectors-dev\ai-agent-connector"
$defDir = Join-Path $connectorDir "definition"
$implDir = Join-Path $connectorDir "implementation"

# Créer la structure
New-Item -ItemType Directory -Path $defDir -Force | Out-Null
New-Item -ItemType Directory -Path $implDir -Force | Out-Null

# Copier les fichiers
Copy-Item -Path $defFile -Destination "$defDir\ai-agent-connector.def" -Force
Copy-Item -Path $jarFile -Destination "$implDir\bonita-connector-ai-agent-$ConnectorVersion-impl.jar" -Force

# Créer le fichier .impl
$implContent = @"
<?xml version="1.0" encoding="UTF-8"?>
<implementation:connectorImplementation xmlns:implementation="http://www.bonitasoft.org/ns/connector/implementation/6.0">
    <implementationId>ai-agent-connector-impl</implementationId>
    <implementationVersion>$ConnectorVersion</implementationVersion>
    <definitionId>ai-agent-connector</definitionId>
    <definitionVersion>1.0.0</definitionVersion>
    <implementationClassname>com.bonitasoft.connector.aiagent.AIAgentConnector</implementationClassname>
    <jarDependencies>
        <jarDependency>bonita-connector-ai-agent-$ConnectorVersion-impl.jar</jarDependency>
    </jarDependencies>
</implementation:connectorImplementation>
"@

Set-Content -Path "$implDir\ai-agent-connector.impl" -Value $implContent -Encoding UTF8

Write-Host "✅ Installation terminée !" -ForegroundColor Green
Write-Host "`nFichiers installés dans :" -ForegroundColor Cyan
Write-Host "  $connectorDir" -ForegroundColor Gray
Write-Host "`n⚠️  REDÉMARREZ Bonita Studio pour voir le connecteur !" -ForegroundColor Yellow
