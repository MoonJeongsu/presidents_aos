@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo Running full deploy pipeline (step 2 + step 3)...
echo.

call "%~dp02-deploy-functions.bat"
if errorlevel 1 exit /b 1

call "%~dp03-update-app-urls.bat"
if errorlevel 1 exit /b 1

echo.
echo ========================================
echo  All done.
echo  Open Android Studio and Run the app.
echo ========================================
exit /b 0
