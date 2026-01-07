@echo off
cd /d "%~dp0"
echo Stopping Academic Analytics System...
echo (Please wait while containers shut down)

:: Stop containers and remove network (Data stays safe)
docker compose down

echo.
echo System Stopped Successfully.
:: Waits 2 seconds so you can read the message, then closes automatically.
:: This is safe for uninstallers (unlike 'pause').
timeout /t 2 >nul