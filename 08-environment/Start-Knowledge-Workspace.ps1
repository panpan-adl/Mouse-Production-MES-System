param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
$kgWorkspaceRoot = Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $kgWorkspaceRoot

Write-Host '[1/4] Configure the Volcano Ark model. Load local settings, or prompt if no API key is configured.'
. (Join-Path $PSScriptRoot 'Use-Knowledge-Ark.ps1')
. (Join-Path $PSScriptRoot 'Use-Environment.ps1')

Write-Host '[2/4] Check the graph database.'
$kgNeoHome = Join-Path $RuntimeRoot 'knowledge-integration\neo4j-community-5.26.0'
$kgNeoPasswordFile = Join-Path $RuntimeRoot 'knowledge-integration\neo4j-password.txt'
if (-not $env:MES_KNOWLEDGE_NEO4J_URL) {
    $env:MES_KNOWLEDGE_NEO4J_URL = if (Test-Path -LiteralPath $kgNeoHome) { 'http://127.0.0.1:17474' } else { 'http://127.0.0.1:7474' }
}
$kgNeoUri = [Uri]$env:MES_KNOWLEDGE_NEO4J_URL
$kgUseBundledNeo = $kgNeoUri.Host -in @('127.0.0.1','localhost') -and $kgNeoUri.Port -eq 17474
if (-not $env:MES_KNOWLEDGE_NEO4J_PASSWORD) {
    if ($kgUseBundledNeo -and (Test-Path -LiteralPath $kgNeoPasswordFile)) {
        $env:MES_KNOWLEDGE_NEO4J_PASSWORD = (Get-Content -LiteralPath $kgNeoPasswordFile -Raw).Trim()
    } else {
        $kgNeoSecure = Read-Host 'Enter the password for your Neo4j instance (input is hidden)' -AsSecureString
        $kgNeoPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($kgNeoSecure)
        try { $env:MES_KNOWLEDGE_NEO4J_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($kgNeoPointer) }
        finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($kgNeoPointer); $kgNeoSecure.Dispose() }
    }
}
if ($kgUseBundledNeo -and -not (Test-LocalPort 17474)) {
    $kgNeoLauncher = Join-Path $kgNeoHome 'bin\neo4j.ps1'
    if (-not (Test-Path -LiteralPath $kgNeoLauncher)) { throw 'Local Neo4j is not installed. Start your configured Neo4j instance first.' }
    Start-Process -FilePath 'powershell.exe' -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-File',"`"$kgNeoLauncher`"",'console') -WindowStyle Hidden -WorkingDirectory $kgNeoHome -RedirectStandardOutput (Join-Path $LogRoot 'knowledge-neo4j.stdout.log') -RedirectStandardError (Join-Path $LogRoot 'knowledge-neo4j.stderr.log') | Out-Null
    Wait-LocalPort 17474 60
}

Write-Host '[3/4] Check MySQL, Redis and the frontend.'
if (-not (Test-LocalPort 3306) -or -not (Test-LocalPort 6379) -or -not (Test-LocalPort 5173)) {
    & (Join-Path $PSScriptRoot 'Start-MouseMES.ps1') -SkipBuild
}
Write-Host '[4/4] Build and restart the knowledge backend. Build output follows.'
& (Join-Path $PSScriptRoot 'Restart-Knowledge-Backend.ps1') -SkipBuild:$SkipBuild
Write-Host ''
Write-Host 'Knowledge workspace is ready: http://127.0.0.1:5173/knowledge'
Write-Host 'The API key is not printed. Optional knowledge.local.json is excluded from Git.'
