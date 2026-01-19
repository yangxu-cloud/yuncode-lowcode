@echo off
REM SkyWalking 启动脚本 (Windows)
echo ==================================
echo SkyWalking 启动脚本
echo ==================================
echo.

REM 检查 Docker Desktop 是否运行
docker ps >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: Docker 未运行，请先启动 Docker Desktop
    pause
    exit /b 1
)

echo [√] Docker 正在运行
echo.

REM 创建必要的目录
echo [√] 创建数据目录...
if not exist "data\es" mkdir data\es
if not exist "data\prometheus" mkdir data\prometheus
if not exist "config\oap" mkdir config\oap
if not exist "config\prometheus" mkdir config\prometheus
echo [√] 数据目录创建成功
echo.

REM 启动 SkyWalking
echo [√] 启动 SkyWalking 服务...
docker-compose up -d

echo.
echo [√] 等待服务启动...
timeout /t 15 /nobreak >nul

echo.
echo [√] 检查服务状态...
docker-compose ps

echo.
echo ==================================
echo SkyWalking 启动成功！
echo ==================================
echo.
echo 访问地址：
echo   - SkyWalking UI: http://localhost:8088
echo   - Prometheus:   http://localhost:9090
echo   - Elasticsearch: http://localhost:9200
echo.
echo 默认账号密码：
echo   - 默认无密码
echo.
echo 停止服务：
echo   stop.bat
echo.
echo 查看日志：
echo   docker-compose logs -f [oap^|ui^|elasticsearch^|prometheus]
echo.
pause
