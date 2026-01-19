@echo off
REM 设置 JDK 17 路径
set JAVA_HOME=C:\workspace\JeecgBoot-springboot3\jeecg-boot\jdk17
set PATH=%JAVA_HOME%\bin;%PATH%

echo ========================================
echo 使用 JDK: %JAVA_HOME%
echo Java 版本:
java -version
echo ========================================
echo.

REM 进入项目目录
cd "C:\workspace\ai progect\yuncode-lowcode\yuncode-lowcode-boot"

echo 开始编译项目...
echo.

REM 先单独编译 common 模块
call "C:\tools\apache-maven-3.3.3\bin\mvn.cmd" clean install -pl yuncode-common -am -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [错误] common 模块编译失败！
    echo.
    echo 可能的原因：
    echo 1. Maven 版本太旧（当前: 3.3.3）
    echo 2. JDK 版本不正确
    echo.
    echo 建议：
    echo 使用 IntelliJ IDEA 或 Eclipse 打开项目并编译
    echo.
    pause
    exit /b 1
)

echo.
echo common 模块编译成功！继续编译其他模块...
echo.

REM 编译整个项目
call "C:\tools\apache-maven-3.3.3\bin\mvn.cmd" clean compile -DskipTests

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo 编译成功！
    echo ========================================
) else (
    echo.
    echo ========================================
    echo 编译失败！
    echo ========================================
    echo.
    echo 请查看上方的错误信息
    echo 如果是 "无效的标志: --release" 错误：
    echo   说明 Maven 版本太旧，建议使用 IDE 编译
    echo.
)

pause
