@echo off
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -NoExit -File "%~dp008-environment\Start-Knowledge-Workspace.ps1"
