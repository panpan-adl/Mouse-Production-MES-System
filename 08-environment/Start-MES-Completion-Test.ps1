. (Join-Path $PSScriptRoot 'Use-Environment.ps1')
$testState = Get-Content -LiteralPath (Join-Path $RuntimeRoot 'mes-completion-database.json') -Raw | ConvertFrom-Json
$testDatabase = [string]$testState.database
if ($testDatabase -notmatch '^mouse_mes_completion_test_[0-9_]+$') { throw 'Only an isolated completion test database is permitted.' }
if (Test-LocalPort 8085) { throw 'Port 8085 is occupied. Stop the recorded test process before restarting.' }
$testLibPath = Join-Path $RuntimeRoot 'completion-libs'
if (-not (Test-Path (Join-Path $testLibPath 'spring-boot-*.jar'))) {
    $testJar = Join-Path $ProjectRoot '01-backend/target/mouse-mes-1.0.0.jar'
    if (-not (Test-Path $testJar)) {
        & $MavenCmd -q -f (Join-Path $ProjectRoot '01-backend/pom.xml') "-Dmaven.repo.local=$($LocalConfig.mavenRepository)" '-DskipTests' package
        if ($LASTEXITCODE -ne 0) { throw 'Build the backend package before isolated verification.' }
    }
    New-Item -ItemType Directory -Path $testLibPath -Force | Out-Null
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead($testJar)
    try {
        foreach ($entry in $archive.Entries) {
            if ($entry.FullName -like 'BOOT-INF/lib/*.jar') {
                [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry,(Join-Path $testLibPath $entry.Name),$true)
            }
        }
    } finally { $archive.Dispose() }
}
$testClasspath = (Join-Path $ProjectRoot '01-backend/target/classes') + ';' + (Join-Path $RuntimeRoot 'completion-libs/*')
$testUrl = "jdbc:mysql://127.0.0.1:3306/${testDatabase}?useUnicode=true&useSSL=false&characterEncoding=utf-8&serverTimezone=GMT%2b8&allowPublicKeyRetrieval=true"
Start-Process -FilePath $JavaExe -ArgumentList @('-Dfile.encoding=UTF-8','-cp',"`"$testClasspath`"",'com.mouse.mes.MouseMesApplication','--server.address=127.0.0.1','--server.port=8085',"--spring.datasource.url=$testUrl",'--spring.data.redis.database=1') -WorkingDirectory $ProjectRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $RuntimeRoot 'completion-test.stdout.log') -RedirectStandardError (Join-Path $RuntimeRoot 'completion-test.stderr.log') | Out-Null
Wait-LocalPort 8085
Save-OwnedProcess 'completion-test-backend' 8085
Write-Host 'MES completion isolation: http://127.0.0.1:8085'
