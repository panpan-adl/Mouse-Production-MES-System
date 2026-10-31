$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Use-Environment.ps1')
$recordPath = Join-Path $RuntimeRoot 'four-tier-backend.process.json'
$record = Get-Content -LiteralPath $recordPath -Raw | ConvertFrom-Json
$backend = Get-Process -Id $record.pid -ErrorAction SilentlyContinue
if ($backend -and ($backend.Path -ne $record.path -or $backend.StartTime.ToUniversalTime().Ticks -ne [long]$record.startTicks)) { throw 'Backend identity mismatch.' }
$instances = Get-Content -LiteralPath (Join-Path $RuntimeRoot 'profile-fix-instances.json') -Raw | ConvertFrom-Json
$instance = $instances | Where-Object { $_.CommandLine -like '*--server.port=8093*' } | Select-Object -First 1
if (-not $instance) { throw 'Original backend arguments missing.' }
$fixedJar = Join-Path $ProjectRoot '01-backend\target\mouse-mes-work-fix-v2.jar'
if (-not (Test-Path -LiteralPath $fixedJar)) { throw 'Repair package missing.' }
$arguments = $instance.CommandLine.Substring($instance.CommandLine.IndexOf(' -Dfile.encoding')).Replace('mouse-mes-1.0.0.jar', 'mouse-mes-work-fix-v2.jar')
if ($backend) { Stop-Process -Id $backend.Id; Wait-Process -Id $backend.Id -Timeout 15 -ErrorAction SilentlyContinue }
$started = Start-Process -FilePath $JavaExe -ArgumentList $arguments -WindowStyle Hidden -WorkingDirectory (Join-Path $ProjectRoot '01-backend') -RedirectStandardOutput (Join-Path $RuntimeRoot 'work-fix.stdout.log') -RedirectStandardError (Join-Path $RuntimeRoot 'work-fix.stderr.log') -PassThru
@{pid=$started.Id;startTicks=$started.StartTime.ToUniversalTime().Ticks;path=$JavaExe;port=8093;name='four-tier-backend'} | ConvertTo-Json | Set-Content -LiteralPath $recordPath
Wait-LocalPort 8093 60
Write-Host 'Backend restarted. Refresh the browser.'
