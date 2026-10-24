$ErrorActionPreference = 'Stop'
$script:ProjectRoot = Split-Path $PSScriptRoot -Parent
$script:RuntimeRoot = Join-Path $ProjectRoot '.runtime'
$script:LogRoot = Join-Path $RuntimeRoot 'logs'
$configPath = Join-Path $PSScriptRoot 'config.local.json'
if (-not (Test-Path -LiteralPath $configPath)) {
    throw 'Copy 08-environment/config.example.json to config.local.json and set installed program paths.'
}
$script:LocalConfig = Get-Content -LiteralPath $configPath -Raw | ConvertFrom-Json
if (-not $env:SPRING_DATASOURCE_PASSWORD -and $LocalConfig.databasePassword) {
    $env:SPRING_DATASOURCE_PASSWORD = [string]$LocalConfig.databasePassword
}
$env:JAVA_HOME = $LocalConfig.javaHome
$env:Path = "$($LocalConfig.javaHome)\bin;$($LocalConfig.mavenHome)\bin;$($LocalConfig.nodeHome);$($LocalConfig.mysqlHome)\bin;$(Split-Path $LocalConfig.redisExe -Parent);$env:Path"
$script:JavaExe = Join-Path $LocalConfig.javaHome 'bin\java.exe'
$script:MavenCmd = Join-Path $LocalConfig.mavenHome 'bin\mvn.cmd'
$script:NodeExe = Join-Path $LocalConfig.nodeHome 'node.exe'
$script:NpmCmd = Join-Path $LocalConfig.nodeHome 'npm.cmd'
New-Item -ItemType Directory -Path $RuntimeRoot, $LogRoot -Force | Out-Null

function Test-LocalPort([int]$Port) {
    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $task = $client.ConnectAsync('127.0.0.1', $Port)
        if (-not $task.Wait(800)) { return $false }
        return $client.Connected
    } catch { return $false } finally { $client.Dispose() }
}

function Wait-LocalPort([int]$Port, [int]$Seconds = 45) {
    $deadline = [DateTime]::UtcNow.AddSeconds($Seconds)
    while ([DateTime]::UtcNow -lt $deadline) {
        if (Test-LocalPort $Port) { return }
        Start-Sleep -Milliseconds 500
    }
    throw "Port $Port did not become ready. See $LogRoot"
}

function Save-OwnedProcess([string]$Name, [int]$Port) {
    $listener = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $listener) { return }
    $process = Get-Process -Id $listener.OwningProcess -ErrorAction Stop
    @{ name = $Name; pid = $process.Id; startTicks = $process.StartTime.ToUniversalTime().Ticks; startTime = $process.StartTime.ToUniversalTime().ToString('o'); path = $process.Path; port = $Port } |
        ConvertTo-Json | Set-Content -LiteralPath (Join-Path $RuntimeRoot "$Name.process.json") -Encoding UTF8
}
