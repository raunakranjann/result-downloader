@echo off
cd /d "%~dp0"

:: Check if port 2006 is already in use
netstat -ano | findstr :2006 > nul
if %errorlevel% equ 0 (
    echo App is already running. Opening browser...
    start http://localhost:2006/
    exit
)

:: Start Java (Windowless)
:: We use full path and quotes for safety
start "" ".\jre\bin\javaw.exe" -jar mainapplication.jar

:: Wait for boot (Wine workaround)
:: 'timeout' fails in Wine; 'ping' is the standard cross-platform delay
ping 127.0.0.1 -n 6 > nul

:: Open Browser
:: 'start microsoft-edge:' fails in Wine; 'start URL' uses the system default
start http://localhost:2006/