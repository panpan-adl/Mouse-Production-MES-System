# Reopens the verified disposable database, without changing the existing 5174 demonstration database.
. (Join-Path $PSScriptRoot 'Use-Environment.ps1')
$acceptanceRoot = Join-Path $RuntimeRoot 'four-tier-20261030'
$statePath = Join-Path $acceptanceRoot 'state.json'
if (-not (Test-Path -LiteralPath $statePath)) {
    throw 'Prepare an isolated database with 07-tools-barcodes/prepare_four_tier_acceptance.py first.'
}
$acceptanceState = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
$acceptanceDatabase = [string]$acceptanceState.database
if ($acceptanceDatabase -notmatch '^mouse_mes_four_tier_[0-9_]+$') { throw 'Only a disposable four-tier database is allowed.' }
function Assert-FourTierOwner([string]$Name,[int]$Port) {
    $path = Join-Path $RuntimeRoot "$Name.process.json"
    if (-not (Test-Path -LiteralPath $path)) { throw "Port $Port is occupied without an ownership record." }
    $record = Get-Content -LiteralPath $path -Raw | ConvertFrom-Json
    $process = Get-Process -Id $record.pid -ErrorAction Stop
    $listener = Get-NetTCPConnection -State Listen -LocalPort $Port | Select-Object -First 1
    if ($process.StartTime.ToUniversalTime().Ticks -ne $record.startTicks -or $listener.OwningProcess -ne $record.pid) { throw "Port $Port ownership changed." }
}
if (Test-LocalPort 8093) { Assert-FourTierOwner 'four-tier-backend' 8093 }
else {
    $jar = Join-Path $ProjectRoot '01-backend/target/mouse-mes-1.0.0.jar'
    if (-not (Test-Path -LiteralPath $jar)) { throw 'Build the backend package before starting the demo.' }
    $url = "jdbc:mysql://127.0.0.1:3306/${acceptanceDatabase}?useUnicode=true&useSSL=false&characterEncoding=utf-8&serverTimezone=GMT%2b8&allowPublicKeyRetrieval=true"
    Start-Process -FilePath $JavaExe -ArgumentList @('-Dfile.encoding=UTF-8','-jar',"`"$jar`"",'--server.address=127.0.0.1','--server.port=8093',"--spring.datasource.url=$url",'--spring.data.redis.database=5') -WorkingDirectory $ProjectRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $acceptanceRoot 'server.log') -RedirectStandardError (Join-Path $acceptanceRoot 'server-error.log') | Out-Null
    Wait-LocalPort 8093
    Save-OwnedProcess 'four-tier-backend' 8093
}
if (Test-LocalPort 5175) { Assert-FourTierOwner 'four-tier-frontend' 5175 }
else {
    $env:VITE_PROXY_TARGET='http://127.0.0.1:8093'
    $env:VITE_DEMO_MODE='1'
    try {
        Start-Process -FilePath $NodeExe -ArgumentList @('node_modules/vite/bin/vite.js','--host','127.0.0.1','--port','5175','--strictPort') -WorkingDirectory (Join-Path $ProjectRoot '02-frontend-web') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $acceptanceRoot 'frontend.log') -RedirectStandardError (Join-Path $acceptanceRoot 'frontend-error.log') | Out-Null
        Wait-LocalPort 5175
        Save-OwnedProcess 'four-tier-frontend' 5175
    } finally { Remove-Item Env:VITE_PROXY_TARGET;Remove-Item Env:VITE_DEMO_MODE }
}
Write-Host 'Four-tier demonstration: http://127.0.0.1:5175/login'
