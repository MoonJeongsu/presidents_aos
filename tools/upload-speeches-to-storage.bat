@echo off
setlocal EnableExtensions
cd /d "%~dp0.."

call "%~dp0firebase.env.bat"
if "%FIREBASE_PROJECT_ID%"=="" set "FIREBASE_PROJECT_ID=presidential-speeches-a9f00"
if "%FIREBASE_STORAGE_BUCKET%"=="" set "FIREBASE_STORAGE_BUCKET=presidential-speeches-a9f00.firebasestorage.app"

echo ========================================
echo  Upload speeches to Firebase Storage
echo ========================================
echo Project: %FIREBASE_PROJECT_ID%
echo Bucket : %FIREBASE_STORAGE_BUCKET%
echo.

python "%~dp0upload_speeches_to_storage.py" --project %FIREBASE_PROJECT_ID% --bucket %FIREBASE_STORAGE_BUCKET%
if errorlevel 1 (
    echo.
    echo Upload failed.
    echo   pip install firebase-admin google-cloud-storage
    echo   gcloud auth application-default login
    pause
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0\_update-storage-bucket.ps1" -BucketName "%FIREBASE_STORAGE_BUCKET%"

echo.
echo [OK] All done. Rebuild the app in Android Studio.
pause
