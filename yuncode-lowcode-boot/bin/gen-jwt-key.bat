@echo off
chcp 65001 >nul
REM JWT 密钥生成工具（Windows）
REM 生成 32 字节随机十六进制密钥

echo =========================================
echo   JWT 密钥生成工具
echo =========================================
echo.

where node >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo 错误：需要 Node.js 来生成密钥
    echo 请安装 Node.js 后重试
    pause
    exit /b 1
)

for /f %%i in ('node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"') do set KEY=%%i

echo 密钥: %KEY%
echo.
echo 设置环境变量（当前会话）：
echo   set JWT_SECRET_KEY=%KEY%
echo.
echo 或设置为系统环境变量：
echo   setx JWT_SECRET_KEY %KEY%
echo.
echo 密钥已保存到 .env 文件
echo JWT_SECRET_KEY=%KEY% > .env
echo.
echo =========================================
pause
