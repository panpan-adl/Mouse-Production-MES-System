. (Join-Path $PSScriptRoot 'Use-Environment.ps1')
$python = 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe'
if (-not (Test-Path -LiteralPath $python)) { throw 'Set the Python runtime path for this workstation before acceptance.' }
& (Join-Path $PSScriptRoot 'Stop-MES-Completion-Test.ps1')
& $python (Join-Path $ProjectRoot '07-tools-barcodes/build_mes_completion_schema.py')
if ($LASTEXITCODE -ne 0) { throw 'Schema generation failed.' }
& $MavenCmd -q -f (Join-Path $ProjectRoot '01-backend/pom.xml') "-Dmaven.repo.local=$($LocalConfig.mavenRepository)" '-Punit-test' test *> (Join-Path $RuntimeRoot 'mes-completion-unit.log')
if ($LASTEXITCODE -ne 0) { Get-Content (Join-Path $RuntimeRoot 'mes-completion-unit.log') -Tail 30; throw 'Backend unit tests failed.' }
& $python (Join-Path $ProjectRoot '07-tools-barcodes/mes_completion_database.py') --test
if ($LASTEXITCODE -ne 0) { throw 'Isolated migration verification failed.' }
& (Join-Path $PSScriptRoot 'Start-MES-Completion-Test.ps1')
& $python (Join-Path $ProjectRoot '07-tools-barcodes/verify_mes_completion_final.py') *> (Join-Path $RuntimeRoot 'mes-completion-acceptance.log')
if ($LASTEXITCODE -ne 0) { Get-Content (Join-Path $RuntimeRoot 'mes-completion-acceptance.log') -Tail 25; throw 'Completion acceptance failed.' }
Get-Content (Join-Path $RuntimeRoot 'mes-completion-acceptance.log') -Tail 12
