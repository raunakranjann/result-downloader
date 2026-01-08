@echo off
cd /d %~dp0

:: Check if port 2006 is already in use
netstat -ano | findstr :2006 > nul
if %errorlevel% equ 0 (
    echo App is already running. Opening browser...
    start microsoft-edge:http://localhost:2006/
    exit
)

:: Start Java (Windowless)
start /b .\jre\bin\javaw.exe -jar mainapplication.jar

:: Wait for boot
timeout /t 5 /nobreak > nul

:: Open Browser
start microsoft-edge:http://localhost:2006/