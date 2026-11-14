param([string]$PythonExe = (Join-Path $PSScriptRoot '.venv\Scripts\python.exe'))
$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $PSScriptRoot
$config = Join-Path $project '01-backend\config\application.properties'
if (-not $env:MES_FACE_SECRET -and (Test-Path -LiteralPath $config)) {
    $entry = [IO.File]::ReadLines($config) | Where-Object { $_.StartsWith('mes.face.secret=') } | Select-Object -First 1
    if ($entry) { $env:MES_FACE_SECRET = $entry.Substring('mes.face.secret='.Length) }
}
if (!$env:MES_FACE_SECRET -or $env:MES_FACE_SECRET.Length -lt 32) {
    throw 'Set MES_FACE_SECRET or configure mes.face.secret in the local Spring configuration.'
}
if (!(Test-Path -LiteralPath $PythonExe)) {
    throw 'Create 09-face-inference/.venv and install requirements.txt first.'
}
& $PythonExe (Join-Path $PSScriptRoot 'app.py')
exit $LASTEXITCODE