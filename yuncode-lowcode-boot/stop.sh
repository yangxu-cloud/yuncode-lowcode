#!/bin/bash

# Yuncode LowCode Platform - 停止脚本
# 云码低代码平台一键停止

cd "$(dirname "$0")"

echo ""
echo "================================================"
echo "       Yuncode LowCode Platform"
echo "          正在停止服务..."
echo "================================================"
echo ""

# 读取 PID 并停止
if [ -f logs/admin.pid ]; then
    ADMIN_PID=$(cat logs/admin.pid)
    if kill -0 $ADMIN_PID 2>/dev/null; then
        echo "[1/2] 正在停止后端服务 (PID: $ADMIN_PID)..."
        kill $ADMIN_PID
        echo "[OK] 后端服务已停止"
    else
        echo "[提示] 后端服务未运行"
    fi
fi

if [ -f logs/gateway.pid ]; then
    GATEWAY_PID=$(cat logs/gateway.pid)
    if kill -0 $GATEWAY_PID 2>/dev/null; then
        echo "[2/2] 正在停止网关服务 (PID: $GATEWAY_PID)..."
        kill $GATEWAY_PID
        echo "[OK] 网关服务已停止"
    else
        echo "[提示] 网关服务未运行"
    fi
fi

# 备用方案：杀死所有包含 yuncode 的 java 进程
pkill -f "yuncode" 2>/dev/null
pkill -f "spring-boot:run" 2>/dev/null

# 清理 PID 文件
rm -f logs/admin.pid logs/gateway.pid

# 清理 target 目录（可选）
echo ""
echo "正在清理临时文件..."
rm -rf yuncode-admin/target yuncode-gateway/target
echo "[OK] 清理完成"

echo ""
echo "================================================"
echo ""
echo "       [停止完成]"
echo ""
echo "    所有服务已停止"
echo ""
echo "================================================"
echo ""
