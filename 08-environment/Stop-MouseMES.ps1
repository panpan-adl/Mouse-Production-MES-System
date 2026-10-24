. (Join-Path $PSScriptRoot 'Use-Environment.ps1')
foreach ($name in @('stage34-demo-frontend', 'stage34-demo-backend', 'frontend', 'backend')) {
    $record = Join-Path $RuntimeRoot "$name.process.json"
    if (-not (Test-Path -LiteralPath $record)) { continue }
    $owned = Get-Content -LiteralPath $record -Raw | ConvertFrom-Json
    $process = Get-Process -Id $owned.pid -ErrorAction SilentlyContinue
    $sameStart = $false
    if ($process) {
        if ($owned.startTicks) { $sameStart = $process.StartTime.ToUniversalTime().Ticks -eq [long]$owned.startTicks }
        else {
            $recordTime = if ($owned.startTime -is [DateTime]) { $owned.startTime.ToUniversalTime() } else { [DateTime]::Parse($owned.startTime, [Globalization.CultureInfo]::InvariantCulture, [Globalization.DateTimeStyles]::RoundtripKind).ToUniversalTime() }
            $sameStart = [Math]::Abs(($process.StartTime.ToUniversalTime() - $recordTime).TotalMilliseconds) -lt 1
        }
    }
    if ($process -and $process.Path -eq $owned.path -and $sameStart) {
        Stop-Process -Id $process.Id
        Write-Host "$name stopped."
    }
    Remove-Item -LiteralPath $record
}
Write-Host 'MySQL and Redis remain running. No database files were deleted.'
