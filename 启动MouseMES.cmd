@echo off
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp008-environment\Start-MouseMES.ps1"
if errorlevel 1 (
  echo.
  echo Startup failed. See .runtime\logs.
  pause
  exit /b 1
)
start "" "http://127.0.0.1:5173/login"
pause
