@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo ========================================
echo  Install deploy prerequisites
echo ========================================
echo.
echo This installs Firebase CLI globally (one time per PC).
echo Node.js must already be installed.
echo.

where node >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Node.js is not installed.
    echo Download LTS: https://nodejs.org/
    goto :pause_end
)

where npm >nul 2>&1
if errorlevel 1 (
    echo [ERROR] npm is not installed.
    goto :pause_end
)

echo Installing firebase-tools ...
call npm install -g firebase-tools
if errorlevel 1 (
    echo.
    echo Install failed. Try running this window as Administrator,
    echo or run manually: npm install -g firebase-tools
    goto :pause_end
)

echo.
firebase --version
echo.
echo [OK] Firebase CLI installed.

:pause_end
echo.
pause
