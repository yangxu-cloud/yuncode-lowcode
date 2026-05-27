@echo off
chcp 65001 >nul
title Yuncode LowCode Platform - 云码低代码平台

echo.
echo ================================================
echo        Yuncode LowCode Platform
echo             云码低代码平台
echo ================================================
echo.

cd /d "%~dp0"

echo [1/4] 正在检查环境...
call mvn --version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到 Maven，请先安装 Maven
    pause
    exit /b 1
)
echo [OK] Maven 已就绪
echo.

echo [2/4] 正在编译项目...
call mvn clean compile -DskipTests -q
if errorlevel 1 (
    echo [错误] 编译失败
    pause
    exit /b 1
)
echo [OK] 编译成功
echo.

echo [3/4] 正在启动后端服务...
start "Yuncode Admin - 后端服务 (Port: 8080)" cmd /k "cd /d %~dp0yuncode-admin && mvn spring-boot:run"
timeout /t 5 /nobreak >nul

echo [4/4] 正在启动网关服务...
start "Yuncode Gateway - 网关服务 (Port: 9000)" cmd /k "cd /d %~dp0yuncode-gateway && mvn spring-boot:run"

echo.
echo ================================================
echo.
echo        [启动完成] 服务信息
echo.
echo     [后端服务] http://localhost:8080
echo        - API 地址: http://localhost:8080/api
echo        - API 文档: http://localhost:8080/api/doc.html
echo.
echo     [网关服务] http://localhost:9000
echo        - 网关状态: http://localhost:9000/gateway/status
echo        - 网关路由: http://localhost:9000/gateway/routes
echo.
echo     [前端配置] 修改 API 地址为 http://localhost:9000/api
echo.
echo     [事件系统] http://localhost:8080/api/event/types
echo.
echo ================================================
echo.
echo [提示] 请查看弹出的两个窗口以了解服务启动日志
echo [提示] 使用 stop.bat 可以一键停止所有服务
echo.
pause
