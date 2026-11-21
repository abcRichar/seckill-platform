@echo off
chcp 65001 >nul
if exist "D:\Redis\redis-server.exe" (
  echo Starting Redis from D:\Redis ...
  start "" "D:\Redis\redis-server.exe" "D:\Redis\redis.windows.conf"
) else (
  echo [ERROR] D:\Redis\redis-server.exe not found.
  echo Install Redis or run it with Docker:
  echo   docker run -d --name redis -p 6379:6379 redis:7
)
