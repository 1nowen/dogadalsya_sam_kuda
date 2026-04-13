@echo off
rem Needs: sbt, Docker.

setlocal EnableExtensions
cd /d "%~dp0"

echo [1/3] Docker images
call sbt "producerApp/Docker/publishLocal" "consumerApp/Docker/publishLocal"
if errorlevel 1 (
  echo sbt failed
  exit /b 1
)

echo [2/3] Compose cycle
docker compose down
docker compose up -d --force-recreate
if errorlevel 1 (
  echo compose failed
  exit /b 1
)

echo [3/3] Logs stream
docker compose logs -f

endlocal
