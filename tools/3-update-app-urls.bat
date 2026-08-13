@echo off
setlocal EnableExtensions
cd /d "%~dp0.."

if not exist "%~dp0firebase.env.bat" (
    echo Missing tools\firebase.env.bat
    echo Copy tools\firebase.env.example.bat to tools\firebase.env.bat
    exit /b 1
)

call "%~dp0firebase.env.bat"
if "%FIREBASE_REGION%"=="" set "FIREBASE_REGION=us-central1"

if "%FIREBASE_PROJECT_ID%"=="" (
    echo FIREBASE_PROJECT_ID is empty. Edit tools\firebase.env.bat
    exit /b 1
)
if "%FIREBASE_PROJECT_ID%"=="your-firebase-project-id" (
    echo Replace your-firebase-project-id in tools\firebase.env.bat
    exit /b 1
)

echo ========================================
echo  Step 3: Update app function URLs
echo ========================================
echo Project : %FIREBASE_PROJECT_ID%
echo Region  : %FIREBASE_REGION%
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0\_update-strings-urls.ps1" -ProjectId "%FIREBASE_PROJECT_ID%" -Region "%FIREBASE_REGION%"
if errorlevel 1 exit /b 1

echo.
echo Done. Rebuild the app in Android Studio.
exit /b 0
