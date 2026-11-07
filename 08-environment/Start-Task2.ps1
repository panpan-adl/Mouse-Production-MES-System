param([switch]$SkipBuild, [int]$BackendPort = 8086, [int]$FrontendPort = 5176)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Use-Environment.ps1')
$env:MES_AI_MODE = 'remote'
if (-not $env:MES_AI_BASE_URL) { $env:MES_AI_BASE_URL = 'https://ark.cn-beijing.volces.com/api/v3' }
if (-not $env:MES_AI_MODEL) { $env:MES_AI_MODEL = 'doubao-seed-2-1-lite-260915' }
if (-not $env:MES_EMBED_MODEL) { $env:MES_EMBED_MODEL = 'doubao-embedding-vision-251215' }
if (-not $env:MES_RAG_STORAGE) { $env:MES_RAG_STORAGE = Join-Path $RuntimeRoot 'rag-files' }
if (-not $env:MES_AI_API_KEY) {
    $secret = Read-Host 'Enter your Ark API Key (input hidden)' -AsSecureString
    $env:MES_AI_API_KEY = [System.Net.NetworkCredential]::new('', $secret).Password
}
if ([string]::IsNullOrWhiteSpace($env:MES_AI_API_KEY)) { throw 'API Key is required.' }
$env:MES_KNOWLEDGE_ENABLED = 'true'
if (-not $env:MES_KNOWLEDGE_MODEL_BASE_URL) { $env:MES_KNOWLEDGE_MODEL_BASE_URL = $env:MES_AI_BASE_URL }
if (-not $env:MES_KNOWLEDGE_MODEL) { $env:MES_KNOWLEDGE_MODEL = $env:MES_AI_MODEL }
if (-not $env:MES_KNOWLEDGE_MODEL_API_KEY) { $env:MES_KNOWLEDGE_MODEL_API_KEY = $env:MES_AI_API_KEY }
if (-not $env:MES_KNOWLEDGE_NEO4J_URL -and $LocalConfig.neo4jUrl) { $env:MES_KNOWLEDGE_NEO4J_URL = [string]$LocalConfig.neo4jUrl }
if (-not $env:MES_KNOWLEDGE_NEO4J_PASSWORD -and $LocalConfig.neo4jPassword) { $env:MES_KNOWLEDGE_NEO4J_PASSWORD = [string]$LocalConfig.neo4jPassword }
if (-not $env:MES_KNOWLEDGE_NEO4J_URL -or -not $env:MES_KNOWLEDGE_NEO4J_PASSWORD) {
    throw 'Configure Neo4j URL/password in server environment variables or config.local.json before starting.'
}
$neoUri = [Uri]$env:MES_KNOWLEDGE_NEO4J_URL
if ($neoUri.Host -in @('127.0.0.1','localhost') -and -not (Test-LocalPort $neoUri.Port)) {
    if (-not $LocalConfig.neo4jHome) { throw 'Start the configured Neo4j instance first.' }
    $env:NEO4J_HOME = [string]$LocalConfig.neo4jHome
    $neoLauncher = Join-Path $env:NEO4J_HOME 'bin\neo4j.ps1'
    Start-Process 'powershell.exe' -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-File',"`"$neoLauncher`"",'console') -WindowStyle Hidden -WorkingDirectory $env:NEO4J_HOME -RedirectStandardOutput (Join-Path $LogRoot 'knowledge-neo4j.stdout.log') -RedirectStandardError (Join-Path $LogRoot 'knowledge-neo4j.stderr.log') | Out-Null
    Wait-LocalPort $neoUri.Port 60
}
if (Test-LocalPort $BackendPort) {
    $record = Join-Path $RuntimeRoot 'task2-backend.process.json'
    if (-not (Test-Path -LiteralPath $record)) { throw "Port $BackendPort is already in use. Stop the existing backend before starting task 2." }
    $owned = Get-Content -LiteralPath $record -Raw | ConvertFrom-Json
    $process = Get-Process -Id $owned.pid -ErrorAction SilentlyContinue
    if (-not $process -or $process.Path -ne $owned.path -or $process.StartTime.ToUniversalTime().Ticks -ne [long]$owned.startTicks) {
        throw 'Existing backend ownership could not be verified. Stop it manually before continuing.'
    }
    Stop-Process -Id $process.Id
    Wait-Process -Id $process.Id -Timeout 15 -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $record
}
& (Join-Path $PSScriptRoot 'Start-MouseMES.ps1') -SkipBuild:$SkipBuild -BackendPort $BackendPort -FrontendPort $FrontendPort -ProcessPrefix 'task2-' -ArtifactName 'mouse-mes-unified-1.0.0'
Write-Host "RAG + GraphRAG workspace: http://127.0.0.1:$FrontendPort/knowledge"
Write-Host 'The API key is held in this process environment and is not saved to Git.'
