@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo Running unit tests...
call sbt test
if errorlevel 1 (
  echo Tests FAILED.
  pause
  exit /b 1
)
echo All tests passed.
pause

endlocal
