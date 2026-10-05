@echo off
setlocal
cd /d "%~dp0"

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0..\..\..\tools\build-snapshot.ps1" -Dir "%~dp0"
set "RESULT=%ERRORLEVEL%"
if not "%RESULT%"=="0" echo Build FAILED.
pause
exit /b %RESULT%
