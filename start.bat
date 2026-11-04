@echo off
cd /d "%~dp0"
where py >nul 2>nul
if errorlevel 1 (
  python startup_app\run.py %*
) else (
  py -3 startup_app\run.py %*
)
if errorlevel 1 pause
