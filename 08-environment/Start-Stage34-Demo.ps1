# Uses only the already verified, isolated acceptance database.
. (Join-Path $PSScriptRoot 'Use-Environment.ps1')
$stageState = Get-Content (Join-Path $RuntimeRoot 'stage34-test-database.json') -Raw | ConvertFrom-Json
$stageDatabase = [string]$stageState.database
if ($stageDatabase -notmatch '^mouse_mes_stage34_test_[0-9_]+$') { throw 'An isolated stage34 acceptance database is required.' }
if (-not (Test-Path (Join-Path $RuntimeRoot 'stage34-acceptance.json'))) { throw 'Complete isolated acceptance first.' }
$stageUrl = "jdbc:mysql://127.0.0.1:3306/${stageDatabase}?useUnicode=true&useSSL=false&characterEncoding=utf-8&serverTimezone=GMT%2b8&allowPublicKeyRetrieval=true"
if (Test-LocalPort 8082) {
    $stageListener = Get-NetTCPConnection -State Listen -LocalPort 8082 | Select-Object -First 1
    $stageCommand = (Get-CimInstance Win32_Process -Filter "ProcessId=$($stageListener.OwningProcess)").CommandLine
    if ($stageCommand -notlike "*${stageDatabase}*server.port=8082*" -and $stageCommand -notlike "*server.port=8082*${stageDatabase}*") { throw 'Port 8082 is occupied by a different process.' }
} else {
    Start-Process -FilePath $JavaExe -ArgumentList @('-Dfile.encoding=UTF-8','-jar',"`"$(Join-Path $ProjectRoot '01-backend/target/mouse-mes-1.0.0.jar')`"",'--server.address=127.0.0.1','--server.port=8082',"--spring.datasource.url=$stageUrl") -WorkingDirectory $ProjectRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $RuntimeRoot 'stage34-demo-backend.stdout.log') -RedirectStandardError (Join-Path $RuntimeRoot 'stage34-demo-backend.stderr.log') | Out-Null
    Wait-LocalPort 8082
}
if (-not (Test-LocalPort 5174)) {
    $env:VITE_PROXY_TARGET='http://127.0.0.1:8082'
    $env:VITE_DEMO_MODE='1'
    try {
        Start-Process -FilePath $NodeExe -ArgumentList @('node_modules/vite/bin/vite.js','--host','127.0.0.1','--port','5174','--strictPort') -WorkingDirectory (Join-Path $ProjectRoot '02-frontend-web') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $RuntimeRoot 'stage34-demo-frontend.stdout.log') -RedirectStandardError (Join-Path $RuntimeRoot 'stage34-demo-frontend.stderr.log') | Out-Null
        Wait-LocalPort 5174
    } finally { Remove-Item Env:VITE_PROXY_TARGET; Remove-Item Env:VITE_DEMO_MODE }
}
Save-OwnedProcess 'stage34-demo-backend' 8082
Save-OwnedProcess 'stage34-demo-frontend' 5174
Write-Host 'Isolated demonstration: http://127.0.0.1:5174/login'
Write-Host "Database: $stageDatabase (test fixtures; production database is unchanged)."
