@echo off
cd /d "%~dp0"
echo Starting Academic Analytics...

:: --- CLEANUP STEP (Fixes the Conflict Error) ---
echo Cleaning up old sessions...
docker rm -f academic_db academic_app >nul 2>&1
:: -----------------------------------------------

echo Building and Starting...
docker compose up -d --build
timeout /t 5
start http://localhost:2006