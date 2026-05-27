@echo off
chcp 65001 >nul
title Yuncode LowCode Platform - 停止服务

echo.
echo ================================================
echo        Yuncode LowCode Platform
echo           正在停止服务...
echo ================================================
echo.

echo [1/2] 正在停止 Java 进程...
wmic process where "name='java.exe' and commandline like '%%yuncode%%'" delete >nul 2>&1
taskkill /F /FI "WINDOWTITLE eq Yuncode Admin*" >nul 2>&1
taskkill /F /FI "WINDOWTITLE eq Yuncode Gateway*" >nul 2>&1
taskkill /F /FI "IMAGENAME eq java.exe" >nul 2>&1

echo [OK] 服务已停止
echo.

echo [2/2] 正在清理临时文件...
rd /s /q "%~dp0yuncode-admin\target" >nul 2>&1
rd /s /q "%~dp0yuncode-gateway\target" >nul 2>&1
echo [OK] 清理完成
echo.

echo ================================================
echo.
echo        [停止完成]
echo.
echo     所有服务已停止
echo.
echo ================================================
echo.
pause
