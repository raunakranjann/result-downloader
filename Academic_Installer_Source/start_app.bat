@echo off
TITLE Academic Analytics Launcher
cls

echo =====================================================
echo      ACADEMIC ANALYTICS SYSTEM - LAUNCHER
echo =====================================================
echo.

:: -------------------------------------------------------
:: STEP 1: CHECK IF DOCKER IS INSTALLED
:: -------------------------------------------------------
echo [1/4] Checking system requirements...
docker --version >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    cls
    echo =====================================================
    echo  CRITICAL ERROR: Docker is not installed!
    echo =====================================================
    echo.
    echo  To run this application, you must install "Docker Desktop".
    echo  Please install it and restart your computer.
    echo.
    pause
    exit
)

:: -------------------------------------------------------
:: STEP 2: CHECK IF DOCKER IS RUNNING
:: -------------------------------------------------------
docker info >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    cls
    echo =====================================================
    echo  ERROR: Docker is installed but NOT RUNNING.
    echo =====================================================
    echo.
    echo  Please open "Docker Desktop" from your Start Menu.
    echo  Wait for the engine to start (green icon), then try again.
    echo.
    pause
    exit
)

:: -------------------------------------------------------
:: STEP 3: CLEANUP (Your Code)
:: -------------------------------------------------------
echo [2/4] Cleaning up old sessions...
docker rm -f academic_db academic_app >nul 2>&1

:: -------------------------------------------------------
:: STEP 4: START APPLICATION
:: -------------------------------------------------------
echo [3/4] Building and Starting Application...
echo       (This might take a moment...)
docker compose up -d --build

:: -------------------------------------------------------
:: STEP 5: LAUNCH BROWSER
:: -------------------------------------------------------
echo [4/4] Launching Browser...
timeout /t 5 >nul
start http://localhost:2006

echo.
echo =====================================================
echo  SUCCESS! App is running in the background.
echo  You can close this window now.
echo =====================================================
pause