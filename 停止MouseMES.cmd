@echo off
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp008-environment\Stop-MouseMES.ps1"
pause
