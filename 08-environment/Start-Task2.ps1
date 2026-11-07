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
& (Join-Path $PSScriptRoot 'Start-MouseMES.ps1') -SkipBuild:$SkipBuild -BackendPort $BackendPort -FrontendPort $FrontendPort -ProcessPrefix 'task2-' -ArtifactName 'mouse-mes-task2-1.0.0'
Write-Host "Task 2: http://127.0.0.1:$FrontendPort/knowledge"
Write-Host 'The API key is held in this process environment and is not saved to Git.'
