@echo off
setlocal EnableExtensions
cd /d "%~dp0.."

echo ========================================
echo  Step 0: Prerequisites check
echo ========================================
echo.

set "MISSING=0"

where node >nul 2>&1
if errorlevel 1 (
    echo [X] Node.js not found. Install LTS from https://nodejs.org/
    set "MISSING=1"
) else (
    for /f "delims=" %%v in ('node --version') do echo [OK] Node.js %%v
)

where npm >nul 2>&1
if errorlevel 1 (
    echo [X] npm not found.
    set "MISSING=1"
) else (
    for /f "delims=" %%v in ('npm --version') do echo [OK] npm %%v
)

where firebase >nul 2>&1
if errorlevel 1 (
    echo [X] Firebase CLI not found.
    echo     Run: tools\install-prerequisites.bat
    set "MISSING=1"
) else (
    for /f "delims=" %%v in ('firebase --version') do echo [OK] Firebase CLI %%v
)

if not exist "tools\firebase.env.bat" (
    echo [X] tools\firebase.env.bat not found.
    echo     Run: tools\1-setup-config.bat
    echo     Or copy tools\firebase.env.example.bat to tools\firebase.env.bat
    set "MISSING=1"
) else (
    call "tools\firebase.env.bat"
    if "%FIREBASE_PROJECT_ID%"=="your-firebase-project-id" (
        echo [X] firebase.env.bat still has placeholder project ID.
        echo     Run: tools\1-setup-config.bat
        set "MISSING=1"
    ) else if "%FIREBASE_PROJECT_ID%"=="" (
        echo [X] FIREBASE_PROJECT_ID is empty in firebase.env.bat
        echo     Run: tools\1-setup-config.bat
        set "MISSING=1"
    ) else (
        echo [OK] tools\firebase.env.bat ^(%FIREBASE_PROJECT_ID%^)
    )
)

if not exist ".firebaserc" (
    echo [X] .firebaserc not found in project root.
    set "MISSING=1"
) else (
    echo [OK] .firebaserc exists
)

echo.
if "%MISSING%"=="1" (
    echo Some prerequisites are missing. Fix them before step 2.
    exit /b 1
)

echo All prerequisites look good.
exit /b 0
