@echo off
chcp 65001 >nul
cd /d "%~dp0..\admin-web"
if not exist node_modules (
  echo Installing admin-web dependencies ...
  call npm install --no-audit --no-fund
)
echo Admin web: http://localhost:5181
call npm run dev
pause
