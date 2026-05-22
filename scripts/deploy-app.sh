#!/bin/bash
# 构建并安装应用 JAR 到 apps/lib/
# 用法: ./scripts/deploy-app.sh qms0205

set -e

APP=$1
if [ -z "$APP" ]; then
    echo "Usage: $0 <app-module-name>"
    echo "Example: $0 qms0205"
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "=== Building app: $APP ==="

# 构建应用 JAR
cd "$PROJECT_DIR/yuncode-lowcode-boot"
JAVA_HOME="C:\tools\jdk17" mvn package -pl "apps/install/com.yuncode.user.apps.$APP" -am -DskipTests -q

# 查找生成的 JAR (不是 -sources 或 -javadoc)
JAR_FILE=$(find "apps/install/com.yuncode.user.apps.$APP/target" -name "*.jar" ! -name "*sources*" ! -name "*javadoc*" ! -name "*original*" 2>/dev/null | head -1)

if [ -z "$JAR_FILE" ]; then
    echo "ERROR: No JAR found for app $APP"
    exit 1
fi

# 复制到 apps/lib/
mkdir -p "$PROJECT_DIR/apps/lib"
cp "$JAR_FILE" "$PROJECT_DIR/apps/lib/"
echo "=== Deployed $JAR_FILE → apps/lib/ ==="
echo "Restart the platform to load the app."
