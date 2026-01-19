@echo off
REM SkyWalking 停止脚本 (Windows)

echo ==================================
echo 停止 SkyWalking 服务
echo ==================================
echo.

docker-compose down

echo.
echo SkyWalking 服务已停止
echo.
echo 如需删除数据卷，请运行：
echo   docker-compose down -v
echo.
pause
