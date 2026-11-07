param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Use-Environment.ps1')

if ($env:MES_KNOWLEDGE_ENABLED -ne 'true' -or [string]::IsNullOrWhiteSpace($env:MES_KNOWLEDGE_MODEL_API_KEY)) {
    throw 'Run this script in the PowerShell session where Use-Knowledge-Ark.ps1 loaded your API key. No service was stopped.'
}
$kgBackendDir = Join-Path $ProjectRoot '01-backend'
$kgJar = Join-Path $kgBackendDir 'target\mouse-mes-1.0.0.jar'
$kgRecord = Join-Path $RuntimeRoot 'backend.process.json'
$kgOldProcess = $null
Write-Host 'Checking backend ownership and configuration...'
if (Test-LocalPort 8080) {
    if (-not (Test-Path -LiteralPath $kgRecord)) { throw 'Port 8080 is occupied without an owned backend record. No process was stopped.' }
    $kgOwned = Get-Content -LiteralPath $kgRecord -Raw | ConvertFrom-Json
    $kgOldProcess = Get-Process -Id $kgOwned.pid -ErrorAction Stop
    if (-not $kgOwned.startTicks -or $kgOldProcess.Path -ne $JavaExe -or $kgOldProcess.StartTime.ToUniversalTime().Ticks -ne [long]$kgOwned.startTicks) {
        throw 'The backend process record no longer matches. No process was stopped.'
    }
    Write-Host 'Checking the process listening on port 8080...'
    $kgListener = Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction Stop | Select-Object -First 1
    if ($kgListener.OwningProcess -ne $kgOldProcess.Id) { throw 'Port 8080 is owned by another process. No process was stopped.' }
}
if (-not (Test-LocalPort 3306) -or -not (Test-LocalPort 6379)) {
    throw 'Start MySQL and Redis using Start-MouseMES.ps1 first. No service was stopped.'
}
$kgBackup = Join-Path $RuntimeRoot ('knowledge-backend-before-' + [DateTime]::Now.ToString('yyyyMMdd-HHmmss-fff') + '.jar')
if (Test-Path -LiteralPath $kgJar) { Copy-Item -LiteralPath $kgJar -Destination $kgBackup }
function Start-KnowledgeJar([string]$Path) {
    $kgLaunched = Start-Process -FilePath $JavaExe -ArgumentList @('-Dfile.encoding=UTF-8','-jar',"`"$Path`"",'--server.address=127.0.0.1','--spring.data.redis.host=127.0.0.1') -WindowStyle Hidden -WorkingDirectory $kgBackendDir -PassThru -RedirectStandardOutput (Join-Path $LogRoot 'backend.stdout.log') -RedirectStandardError (Join-Path $LogRoot 'backend.stderr.log')
    try { Wait-LocalPort 8080 60; Save-OwnedProcess 'backend' 8080 }
    catch { if (-not $kgLaunched.HasExited) { Stop-Process -Id $kgLaunched.Id -ErrorAction SilentlyContinue }; throw }
}
if ($kgOldProcess) { Write-Host 'Stopping the previous backend...'; Stop-Process -Id $kgOldProcess.Id; Wait-Process -InputObject $kgOldProcess -Timeout 20 -ErrorAction Stop }
try {
    if (-not $SkipBuild) {
        Write-Host 'Building the updated backend...'
        Push-Location $kgBackendDir
        try {
            & $MavenCmd "-Duser.home=$([Environment]::GetFolderPath('UserProfile'))" "-Dmaven.repo.local=$($LocalConfig.mavenRepository)" '-DskipTests' package
            if ($LASTEXITCODE -ne 0) { throw 'Backend build failed.' }
        } finally { Pop-Location }
    }
    Write-Host 'Starting the updated backend (up to 60 seconds)...'
    Start-KnowledgeJar $kgJar
    Write-Host 'Knowledge backend restarted on 8080. Frontend, MySQL, Redis and Neo4j were left running.'
    Write-Host 'Refresh http://127.0.0.1:5173/knowledge'
} catch {
    $kgFailure = $_
    if ($kgOldProcess -and (Test-Path -LiteralPath $kgBackup) -and -not (Test-LocalPort 8080)) {
        try { Start-KnowledgeJar $kgBackup; Write-Warning 'The previous backend package was restarted after the upgrade failed.' }
        catch { Write-Warning 'Automatic rollback failed. Check .runtime/logs/backend.stderr.log.' }
    }
    throw $kgFailure
}
