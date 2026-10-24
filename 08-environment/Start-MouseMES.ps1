param([switch]$SkipBuild)
. (Join-Path $PSScriptRoot 'Use-Environment.ps1')
foreach ($exe in @($JavaExe, $MavenCmd, $NodeExe, $NpmCmd, $LocalConfig.redisExe)) {
    if (-not (Test-Path -LiteralPath $exe)) { throw "Program not found: $exe" }
}
if (-not (Test-LocalPort 3306)) {
    if (-not (Test-Path -LiteralPath $LocalConfig.mysqlConfig)) { throw 'Configured MySQL ini file does not exist.' }
    $mysqlExe = Join-Path $LocalConfig.mysqlHome 'bin\mysqld.exe'
    Start-Process -FilePath $mysqlExe -ArgumentList @("--defaults-file=`"$($LocalConfig.mysqlConfig)`"", '--bind-address=127.0.0.1', '--mysqlx-bind-address=127.0.0.1', '--console') -WindowStyle Hidden -WorkingDirectory $RuntimeRoot -RedirectStandardOutput (Join-Path $LogRoot 'mysql.stdout.log') -RedirectStandardError (Join-Path $LogRoot 'mysql.stderr.log') | Out-Null
    Wait-LocalPort 3306
    Write-Host 'MySQL started using the existing data directory.'
} else { Write-Host 'MySQL: port 3306 is already listening.' }
if (-not (Test-LocalPort 6379)) {
    $redisDir = Join-Path $RuntimeRoot 'redis'
    New-Item -ItemType Directory -Path $redisDir -Force | Out-Null
    $redisConfig = Join-Path $redisDir 'redis.conf'
    $redisDataPath = $redisDir.Replace('\', '/')
    @"
bind 127.0.0.1
protected-mode yes
port 6379
dir "$redisDataPath"
dbfilename mouse-mes.rdb
appendonly yes
appendfilename mouse-mes.aof
save 900 1
save 300 10
loglevel notice
"@ | Set-Content -LiteralPath $redisConfig -Encoding ASCII
    Start-Process -FilePath $LocalConfig.redisExe -ArgumentList "`"$redisConfig`"" -WindowStyle Hidden -WorkingDirectory $redisDir -RedirectStandardOutput (Join-Path $LogRoot 'redis.stdout.log') -RedirectStandardError (Join-Path $LogRoot 'redis.stderr.log') | Out-Null
    Wait-LocalPort 6379
    Save-OwnedProcess 'redis' 6379
}
$pong = & $LocalConfig.redisCli -h 127.0.0.1 -p 6379 ping
if ($LASTEXITCODE -ne 0 -or $pong -ne 'PONG') { throw 'Redis PING failed. Check authentication and port ownership.' }
Write-Host 'Redis: PONG'
$backendDir = Join-Path $ProjectRoot '01-backend'
$frontendDir = Join-Path $ProjectRoot '02-frontend-web'
$jar = Join-Path $backendDir 'target\mouse-mes-1.0.0.jar'
if (-not $SkipBuild -or -not (Test-Path -LiteralPath $jar)) {
    Push-Location $backendDir
    try {
        & $MavenCmd "-Duser.home=$([Environment]::GetFolderPath('UserProfile'))" "-Dmaven.repo.local=$($LocalConfig.mavenRepository)" '-Dfile.encoding=UTF-8' '-DskipTests' package
        if ($LASTEXITCODE -ne 0) { throw 'Backend build failed.' }
    } finally { Pop-Location }
}
$vite = Join-Path $frontendDir 'node_modules\vite\bin\vite.js'
if (-not (Test-Path -LiteralPath $vite)) {
    Push-Location $frontendDir
    try {
        & $NpmCmd ci --prefer-offline --no-audit --no-fund --cache (Join-Path $RuntimeRoot 'npm-cache') --fetch-retries=1 --fetch-timeout=30000
        if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed.' }
    } finally { Pop-Location }
}
if (-not (Test-LocalPort 8080)) {
    Start-Process -FilePath $JavaExe -ArgumentList @('-Dfile.encoding=UTF-8', '-jar', "`"$jar`"", '--server.address=127.0.0.1', '--spring.data.redis.host=127.0.0.1') -WindowStyle Hidden -WorkingDirectory $backendDir -RedirectStandardOutput (Join-Path $LogRoot 'backend.stdout.log') -RedirectStandardError (Join-Path $LogRoot 'backend.stderr.log') | Out-Null
    Wait-LocalPort 8080 60
    Save-OwnedProcess 'backend' 8080
}
if (-not (Test-LocalPort 5173)) {
    Start-Process -FilePath $NodeExe -ArgumentList @("`"$vite`"", '--host', '127.0.0.1', '--port', '5173', '--strictPort') -WindowStyle Hidden -WorkingDirectory $frontendDir -RedirectStandardOutput (Join-Path $LogRoot 'frontend.stdout.log') -RedirectStandardError (Join-Path $LogRoot 'frontend.stderr.log') | Out-Null
    Wait-LocalPort 5173
    Save-OwnedProcess 'frontend' 5173
}
$page = Invoke-WebRequest -Uri 'http://127.0.0.1:5173/login' -UseBasicParsing -TimeoutSec 10
if ($page.StatusCode -ne 200) { throw 'Frontend HTTP check failed.' }
try {
    Invoke-WebRequest -Uri 'http://127.0.0.1:8080/api/materials' -UseBasicParsing -TimeoutSec 10 | Out-Null
    throw 'Protected API accepted an unauthenticated request. Check test-mode settings.'
} catch {
    if (-not $_.Exception.Response -or [int]$_.Exception.Response.StatusCode -ne 401) { throw }
}
Write-Host ''
Write-Host 'MouseMES is ready: http://127.0.0.1:5173/login'
Write-Host "Logs: $LogRoot"
Write-Host 'The original database was preserved. MinIO is optional and is not started.'
