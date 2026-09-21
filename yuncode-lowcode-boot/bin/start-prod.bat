@echo off
chcp 65001 >nul
REM =========================================
REM   Yuncode LowCode 生产环境启动脚本
REM =========================================

set BIN_DIR=%~dp0
set PROJECT_DIR=%BIN_DIR%..\

cd /d "%PROJECT_DIR%"

REM 检查 JWT 密钥
if "%JWT_SECRET_KEY%"=="" (
    if exist .env (
        for /f "tokens=2 delims==" %%i in (.env) do set JWT_SECRET_KEY=%%i
    )
)

if "%JWT_SECRET_KEY%"=="" (
    echo =========================================
    echo   JWT 密钥未设置，正在生成...
    echo =========================================

    where node >nul 2>nul
    if %ERRORLEVEL% EQU 0 (
        for /f %%i in ('node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"') do set JWT_SECRET_KEY=%%i
        echo JWT_SECRET_KEY=%JWT_SECRET_KEY%>.env
        echo 密钥已保存到 .env 文件
    )
)

if "%JWT_SECRET_KEY%"=="" (
    echo 错误：无法生成 JWT 密钥
    echo 请先运行 bin\gen-jwt-key.bat 生成密钥
    pause
    exit /b 1
)

echo =========================================
echo   Yuncode LowCode Platform
echo   JWT 密钥: %JWT_SECRET_KEY:~0,8%... ^(前8位^)
echo =========================================

REM 启动 admin 服务
echo 启动 admin 服务...
start "yuncode-admin" cmd /c "mvn spring-boot:run -pl yuncode-admin -o"

REM 可选：启动 gateway
echo 启动 gateway 服务（可选）...
REM start "yuncode-gateway" cmd /c "mvn spring-boot:run -pl yuncode-gateway -o"

echo =========================================
echo   服务启动中...
echo   Admin: http://localhost:8080/api/doc.html
echo   Gateway: http://localhost:9000/gateway/status
echo =========================================
pause
