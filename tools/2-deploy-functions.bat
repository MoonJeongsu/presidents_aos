@echo off
setlocal EnableExtensions
cd /d "%~dp0.."

call "%~dp0firebase.env.bat"
if errorlevel 1 exit /b 1

if "%FIREBASE_PROJECT_ID%"=="" (
    echo FIREBASE_PROJECT_ID is empty. Edit tools\firebase.env.bat
    exit /b 1
)
if "%FIREBASE_PROJECT_ID%"=="your-firebase-project-id" (
    echo Replace placeholder project ID first.
    echo Run: tools\1-setup-config.bat
    exit /b 1
)
if "%FIREBASE_REGION%"=="" set "FIREBASE_REGION=us-central1"

echo ========================================
echo  Step 2: Deploy Cloud Functions
echo ========================================
echo Project : %FIREBASE_PROJECT_ID%
echo Region  : %FIREBASE_REGION%
echo.

call "%~dp00-check-prerequisites.bat"
if errorlevel 1 exit /b 1

echo Updating .firebaserc ...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0\_sync-firebaserc.ps1" -ProjectId "%FIREBASE_PROJECT_ID%"
if errorlevel 1 exit /b 1

echo.
echo Installing npm packages in functions\ ...
pushd functions
call npm install
if errorlevel 1 (
    popd
    exit /b 1
)

echo.
echo Building TypeScript ...
call npm run build
if errorlevel 1 (
    popd
    exit /b 1
)
popd

echo.
echo Deploying to Firebase (functions + storage rules) ...
firebase deploy --only functions,storage --project %FIREBASE_PROJECT_ID%
if errorlevel 1 (
    echo.
    echo Deploy failed. Common causes:
    echo   - Blaze billing not enabled
    echo   - Cloud Translation API not enabled in GCP
    echo   - Cloud Text-to-Speech API not enabled in GCP
    echo   - Wrong project ID
    exit /b 1
)

echo.
echo Deploy succeeded.
echo.
echo Next: run tools\3-update-app-urls.bat
exit /b 0
