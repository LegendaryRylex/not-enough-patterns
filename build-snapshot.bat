@echo off
setlocal
cd /d "%~dp0"

set "BASE="
for /f %%v in ('powershell -NoProfile -Command "(Get-ChildItem '%~dp0changelogs' -Filter *.md | Where-Object { $_.BaseName -match '^\d+(\.\d+)+$' } | ForEach-Object { [version]$_.BaseName } | Sort-Object | Select-Object -Last 1).ToString()"') do set "BASE=%%v"
if not defined BASE (
    echo Could not find a version changelog in changelogs\.
    pause
    exit /b 1
)

set "VERSION=%BASE%-snapshot"
echo Building nep %VERSION% ...
call "%~dp0gradlew.bat" build "-Pmod_version=%VERSION%"
if errorlevel 1 (
    echo.
    echo Build FAILED.
) else (
    echo.
    echo Done: build\libs\nep-%VERSION%.jar
)
pause
