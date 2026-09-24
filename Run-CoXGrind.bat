@echo off
setlocal
cd /d "%~dp0"

where java >nul 2>&1
if errorlevel 1 (
  echo Java was not found.
  echo Install Java 17 or newer from https://adoptium.net/ and then double-click this file again.
  pause
  exit /b 1
)

if not exist "%~dp0gradlew.bat" (
  echo gradlew.bat is missing. This folder does not look like the full CoXGrind project.
  pause
  exit /b 1
)

echo Starting RuneLite with CoXGrind.
echo The first launch can take a few minutes while it downloads RuneLite.
echo Leave this window open while you play. Closing it closes the game.
echo.
call "%~dp0gradlew.bat" run
set EXITCODE=%ERRORLEVEL%
echo.
if not "%EXITCODE%"=="0" (
  echo CoXGrind did not start. Scroll up in this window for the error.
) else (
  echo RuneLite has closed.
)
pause
exit /b %EXITCODE%
