. (Join-Path $PSScriptRoot 'Use-Environment.ps1')
foreach ($entry in @(@('four-tier-frontend',5175),@('four-tier-backend',8093))) {
    $name=[string]$entry[0];$port=[int]$entry[1]
    if (-not (Test-LocalPort $port)) { continue }
    $recordPath=Join-Path $RuntimeRoot "$name.process.json"
    if (-not (Test-Path -LiteralPath $recordPath)) { throw "No ownership record for port $port; refusing to stop it." }
    $record=Get-Content -LiteralPath $recordPath -Raw | ConvertFrom-Json
    $owned=Get-Process -Id $record.pid -ErrorAction Stop
    $listener=Get-NetTCPConnection -State Listen -LocalPort $port | Select-Object -First 1
    if ($owned.StartTime.ToUniversalTime().Ticks -ne $record.startTicks -or $listener.OwningProcess -ne $record.pid) { throw "Ownership changed on port $port." }
    try { Stop-Process -Id $owned.Id -ErrorAction Stop }
    catch { & taskkill /PID $owned.Id /F; if ($LASTEXITCODE -ne 0) { throw "Could not stop owned process on port $port." } }
}
Write-Host 'Four-tier demo stopped; all databases and original demo services were preserved.'
