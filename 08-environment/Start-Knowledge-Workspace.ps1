param([switch]$SkipBuild, [int]$BackendPort = 8086, [int]$FrontendPort = 5176)
$ErrorActionPreference = 'Stop'
# Unified task-two/task-three startup; preserve the original command entry point.
& (Join-Path $PSScriptRoot 'Start-Task2.ps1') -SkipBuild:$SkipBuild -BackendPort $BackendPort -FrontendPort $FrontendPort