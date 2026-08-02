@echo off
setlocal
cd /d "%~dp0"
title SmartStudy Tests
mvn clean test
pause
endlocal
