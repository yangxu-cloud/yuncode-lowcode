#!/bin/bash
# 生产环境启动脚本
# 自动生成 JWT 密钥（首次）并启动服务

BIN_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$BIN_DIR/.." && pwd)"

cd "$PROJECT_DIR"

# 检查 JWT 密钥
if [ -z "$JWT_SECRET_KEY" ]; then
    # 尝试从 .env 文件读取
    if [ -f .env ]; then
        source .env
    fi
fi

if [ -z "$JWT_SECRET_KEY" ]; then
    echo "========================================="
    echo "  JWT 密钥未设置，正在生成..."
    echo "========================================="
    JWT_SECRET_KEY=$(openssl rand -hex 32 2>/dev/null)
    if [ -n "$JWT_SECRET_KEY" ]; then
        echo "JWT_SECRET_KEY=$JWT_SECRET_KEY" > .env
        echo "密钥已保存到 .env 文件"
    else
        echo "错误：无法生成 JWT 密钥"
        echo "请先运行 bin/gen-jwt-key.sh 生成密钥"
        exit 1
    fi
fi

export JWT_SECRET_KEY

echo "========================================="
echo "  Yuncode LowCode Platform"
echo "  JWT 密钥: ${JWT_SECRET_KEY:0:8}... (前8位)"
echo "========================================="

# 启动后端服务
echo "启动 admin 服务..."
JAVA_HOME=${JAVA_HOME:-/c/tools/jdk17}
$JAVA_HOME/bin/java \
    -Dspring.profiles.active=prod \
    -jar yuncode-admin/target/yuncode-admin-*.jar &

echo "启动 gateway 服务（可选）..."
$JAVA_HOME/bin/java \
    -Dspring.profiles.active=prod \
    -jar yuncode-gateway/target/yuncode-gateway-*.jar &
