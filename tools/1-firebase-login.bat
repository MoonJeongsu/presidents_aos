@echo off
setlocal EnableExtensions
cd /d "%~dp0.."

echo ========================================
echo  Firebase login (once per PC)
echo ========================================
echo.
echo A browser window will open. Sign in with the Google account
echo that owns your Firebase / GCP project.
echo.

firebase login
if errorlevel 1 (
    echo.
    echo Login failed.
    exit /b 1
)

echo.
echo Login OK.
firebase projects:list
exit /b 0
