@echo off
setlocal
cd /d "%~dp0"
title SmartStudy Launcher
if not exist pom.xml (
  echo ERROR: pom.xml was not found.
  echo Keep this launcher inside the SmartStudy project folder.
  pause
  exit /b 1
)
echo Starting SmartStudy...
echo Keep this window open while the application is running.
mvn javafx:run
if errorlevel 1 (
  echo.
  echo SmartStudy did not start. Read the error above.
  pause
)
endlocal
