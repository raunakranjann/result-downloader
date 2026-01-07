@echo off
TITLE Academic Analytics Launcher
cd /d "%~dp0"
cls

echo =====================================================
echo      ACADEMIC ANALYTICS SYSTEM - LAUNCHER
echo =====================================================
echo.

:: STEP 1: CHECK DOCKER INSTALLATION
echo [1/4] Checking system requirements...
docker --version >nul 2>&1
IF %ERRORLEVEL% EQU 0 GOTO DOCKER_INSTALLED

:: -- If we get here, Docker is MISSING --
echo.
echo CRITICAL ERROR: Docker is not installed!
echo Please install Docker Desktop and restart.
pause
exit

:DOCKER_INSTALLED
:: STEP 2: CHECK DOCKER RUNNING
docker info >nul 2>&1
IF %ERRORLEVEL% EQU 0 GOTO DOCKER_RUNNING

:: -- If we get here, Docker is STOPPED --
echo.
echo ERROR: Docker is installed but NOT RUNNING.
echo Please open Docker Desktop and wait for the green light.
pause
exit

:DOCKER_RUNNING
:: STEP 3: CLEANUP
echo [2/4] Cleaning up old sessions...
docker rm -f academic_db academic_app >nul 2>&1

:: STEP 4: START APP
echo [3/4] Building and Starting Application...
docker compose up -d --build

:: STEP 5: LAUNCH
echo [4/4] Launching Browser...
timeout /t 5 >nul
start http://localhost:2006

echo.
echo SUCCESS! App is running.
pause