@echo off
chcp 65001 >nul
cd /d "%~dp0..\backend"

if "%DB_PASSWORD%"=="" (
  echo [ERROR] DB_PASSWORD is not set.
  echo Please run: set DB_PASSWORD=your_mysql_password
  pause
  exit /b 1
)
if "%REDIS_HOST%"=="" set REDIS_HOST=127.0.0.1
if "%REDIS_PORT%"=="" set REDIS_PORT=6379

echo Starting backend on http://localhost:9090 ...
call mvn spring-boot:run
pause
