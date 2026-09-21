@echo off
chcp 65001 >nul
REM =========================================
REM   Yuncode LowCode 生产构建脚本
REM =========================================

set BIN_DIR=%~dp0
set PROJECT_DIR=%BIN_DIR%..\

cd /d "%PROJECT_DIR%"

echo =========================================
echo   构建所有模块...
echo =========================================

REM 构建并打包
call mvn clean package -pl yuncode-admin -am -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo 构建失败！
    pause
    exit /b 1
)

REM 复制 JAR 文件
echo.
echo 构建完成！
echo Admin JAR: yuncode-admin/target/yuncode-admin-*.jar

echo.
echo 运行 bin\start-prod.bat 启动生产环境服务
pause
