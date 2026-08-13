@echo off
setlocal
set ROOT=%~dp0..
set SRC=%ROOT%\app\src\main\assets
set DST=%ROOT%\ios\PresidentialSpeeches\Resources

if not exist "%DST%" mkdir "%DST%"

copy /Y "%SRC%\presidents.json" "%DST%\presidents.json"
copy /Y "%SRC%\speeches_index.json" "%DST%\speeches_index.json"

echo Synced iOS JSON assets from Android.
