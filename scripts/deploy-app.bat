@echo off
REM 构建并安装应用 JAR 到 apps/lib/
REM 用法: deploy-app.bat qms0205

if "%1"=="" (
    echo Usage: %0 ^<app-module-name^>
    echo Example: %0 qms0205
    exit /b 1
)

set APP=%1
set PROJECT_DIR=%~dp0..

echo === Building app: %APP% ===

cd /d "%PROJECT_DIR%\yuncode-lowcode-boot"
call mvn package -pl "apps/install/com.yuncode.user.apps.%APP%" -am -DskipTests -q

if %ERRORLEVEL% neq 0 (
    echo ERROR: Build failed
    exit /b 1
)

REM Find the JAR file (not sources, not original)
set APP_DIR=apps\install\com.yuncode.user.apps.%APP%
set APP_LIB=%PROJECT_DIR%\%APP_DIR%\lib
if not exist "%APP_LIB%" mkdir "%APP_LIB%"

for /r "%APP_DIR%\target" %%f in (*.jar) do (
    echo %%f | findstr /v /i "sources javadoc original" >nul
    if not errorlevel 1 (
        copy /y "%%f" "%APP_LIB%\"
        echo === Deployed %%f --^> %APP_DIR%\lib\ ===
        goto :done
    )
)

:done
echo The platform will hot-load the JAR automatically (no restart needed).
