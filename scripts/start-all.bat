@echo off
chcp 65001 >nul
echo ============================================
echo  Seckill Platform - start all services
echo ============================================
echo.

start "seckill-redis" cmd /k "%~dp0start-redis.bat"
timeout /t 2 >nul

start "seckill-backend" cmd /k "%~dp0start-backend.bat"
echo Waiting for backend (9090) ...
timeout /t 10 >nul

start "seckill-mall" cmd /k "%~dp0start-mall.bat"
start "seckill-admin" cmd /k "%~dp0start-admin.bat"

echo.
echo Mall  : http://localhost:5180
echo Admin : http://localhost:5181
echo API   : http://localhost:9090/swagger-ui.html
echo.
echo Accounts: admin/123456 (admin), alice/123456 (mall)
pause
