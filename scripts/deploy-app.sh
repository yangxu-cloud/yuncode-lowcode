#!/bin/bash
# 构建并安装应用 JAR 到 apps/install/{appId}/lib/
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
APP_ID="com.yuncode.user.apps.$APP"

echo "=== Building app: $APP ==="

# 构建应用 JAR
cd "$PROJECT_DIR/yuncode-lowcode-boot"
JAVA_HOME=/c/tools/jdk17 mvn package -pl "apps/install/$APP_ID" -am -DskipTests -q

# 查找生成的 JAR (不是 -sources 或 -javadoc)
JAR_FILE=$(find "apps/install/$APP_ID/target" -name "*.jar" ! -name "*sources*" ! -name "*javadoc*" ! -name "*original*" 2>/dev/null | head -1)

if [ -z "$JAR_FILE" ]; then
    echo "ERROR: No JAR found for app $APP"
    exit 1
fi

# 复制到 apps/install/{appId}/lib/（HotAppDeployer 热加载目录）
INSTALL_DIR="$PROJECT_DIR/yuncode-lowcode-boot/apps/install/$APP_ID/lib"
mkdir -p "$INSTALL_DIR"
cp "$JAR_FILE" "$INSTALL_DIR/"
echo "=== Deployed $JAR_FILE → apps/install/$APP_ID/lib/ ==="
echo "HotAppDeployer will hot-load the JAR automatically."
