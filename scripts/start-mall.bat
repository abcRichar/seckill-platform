@echo off
chcp 65001 >nul
cd /d "%~dp0..\mall-web"
if not exist node_modules (
  echo Installing mall-web dependencies ...
  call npm install --no-audit --no-fund
)
echo Mall web: http://localhost:5180
call npm run dev
pause
