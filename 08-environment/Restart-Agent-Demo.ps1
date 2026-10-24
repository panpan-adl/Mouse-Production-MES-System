param(
    [ValidateSet('local','remote')][string]$Mode = 'local',
    [string]$BaseUrl = '',
    [string]$Model = ''
)
. (Join-Path $PSScriptRoot 'Use-Environment.ps1')
if ($Mode -eq 'remote') {
    if (-not $BaseUrl -or -not $Model -or -not $env:MES_AI_API_KEY) { throw 'Remote mode requires BaseUrl, Model and the MES_AI_API_KEY environment variable. No key is stored by this script.' }
    $agentUri = [Uri]$BaseUrl
    if (-not $agentUri.IsAbsoluteUri -or $agentUri.Scheme -notin @('http','https')) { throw 'BaseUrl must be an HTTP(S) URL.' }
}
if (Test-LocalPort 8082) {
    $agentProcessPath = Join-Path $RuntimeRoot 'stage34-demo-backend.process.json'
    if (-not (Test-Path -LiteralPath $agentProcessPath)) { throw 'Backend ownership record is missing; will not stop an unknown process.' }
    $agentRecord = Get-Content -LiteralPath $agentProcessPath -Raw | ConvertFrom-Json
    $agentProcess = Get-Process -Id $agentRecord.pid -ErrorAction Stop
    $agentListener = Get-NetTCPConnection -State Listen -LocalPort 8082 | Select-Object -First 1
    if ($agentListener.OwningProcess -ne $agentRecord.pid -or $agentProcess.StartTime.ToUniversalTime().Ticks -ne $agentRecord.startTicks) { throw 'Backend ownership changed; will not stop it.' }
    Stop-Process -Id $agentProcess.Id -ErrorAction Stop
    $agentProcess.WaitForExit(10000) | Out-Null
}
$env:MES_AI_MODE=$Mode
if ($Mode -eq 'remote') { $env:MES_AI_BASE_URL=$BaseUrl; $env:MES_AI_MODEL=$Model }
& (Join-Path $PSScriptRoot 'Start-Stage34-Demo.ps1')
Write-Host "Agent demo mode: $Mode. Open http://127.0.0.1:5174/assistant"
