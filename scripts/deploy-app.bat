@echo off
REM 构建并安装应用 JAR 到 apps/install/{appId}/lib/
REM 用法: deploy-app.bat qms0205

if "%1"=="" (
    echo Usage: %0 ^<app-module-name^>
    echo Example: %0 qms0205
    exit /b 1
)

set APP=%1
set APP_ID=com.yuncode.user.apps.%APP%
set PROJECT_DIR=%~dp0..

echo === Building app: %APP% ===

cd /d "%PROJECT_DIR%\yuncode-lowcode-boot"
call mvn package -pl "apps\install\%APP_ID%" -am -DskipTests -q

if %ERRORLEVEL% neq 0 (
    echo ERROR: Build failed
    exit /b 1
)

REM Find the JAR file (not sources, not original)
set INSTALL_DIR=%PROJECT_DIR%\yuncode-lowcode-boot\apps\install\%APP_ID%\lib
if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"

for /r "apps\install\%APP_ID%\target" %%f in (*.jar) do (
    echo %%f | findstr /v /i "sources javadoc original" >nul
    if not errorlevel 1 (
        copy /y "%%f" "%INSTALL_DIR%\"
        echo === Deployed %%f --^> apps\install\%APP_ID%\lib\ ===
        goto :done
    )
)

:done
echo HotAppDeployer will hot-load the JAR automatically.
