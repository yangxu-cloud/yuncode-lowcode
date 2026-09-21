@echo off
chcp 65001 >nul
REM =========================================
REM   Yuncode LowCode 开发环境启动脚本
REM =========================================

echo =========================================
echo   Yuncode LowCode Platform - 开发模式
echo   开发环境使用默认 JWT 密钥
echo =========================================
echo.

REM 启动 admin 服务
echo [1/3] 启动 admin 服务 (端口 8080)...
start "yuncode-admin" cmd /c "mvn spring-boot:run -pl yuncode-admin -o"

REM 启动 gateway 服务（可选）
echo [2/3] 启动 gateway 服务 (端口 9000)...
REM start "yuncode-gateway" cmd /c "mvn spring-boot:run -pl yuncode-gateway -o"

REM 启动前端
echo [3/3] 启动前端 dev server (端口 3000)...
cd /d "%~dp0..\..\yuncode-pure-admin"
start "yuncode-frontend" cmd /c "pnpm dev"

echo =========================================
echo   服务启动中...
echo   Admin:  http://localhost:8080/api/doc.html
echo   Gateway: http://localhost:9000/gateway/status
echo   前端:    http://localhost:3000
echo =========================================
pause
