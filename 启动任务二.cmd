@echo off
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp008-environment\Start-Task2.ps1"
if errorlevel 1 (
  echo Task 2 startup failed. See the error above and .runtime\logs.
  pause
  exit /b 1
)
start "" "http://127.0.0.1:5176/knowledge"
pause
