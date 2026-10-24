. (Join-Path $PSScriptRoot 'Use-Environment.ps1')
$recordPath = Join-Path $RuntimeRoot 'completion-test-backend.process.json'
if (Test-Path -LiteralPath $recordPath) {
    $record = Get-Content -LiteralPath $recordPath -Raw | ConvertFrom-Json
    $testProcess = Get-Process -Id $record.pid -ErrorAction SilentlyContinue
    if ($testProcess -and $testProcess.Path -eq $record.path -and $testProcess.StartTime.ToUniversalTime().Ticks -eq [long]$record.startTicks) { Stop-Process -Id $testProcess.Id; Wait-Process -Id $testProcess.Id -Timeout 15 -ErrorAction SilentlyContinue }
    Remove-Item -LiteralPath $recordPath
}
