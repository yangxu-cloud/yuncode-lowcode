#!/bin/bash

# Yuncode LowCode Platform - 启动脚本
# 云码低代码平台一键启动

cd "$(dirname "$0")"

echo ""
echo "================================================"
echo "       Yuncode LowCode Platform"
echo "            云码低代码平台"
echo "================================================"
echo ""

echo "[1/4] 正在检查环境..."
if ! command -v mvn &> /dev/null; then
    echo "[错误] 未找到 Maven，请先安装 Maven"
    exit 1
fi
echo "[OK] Maven 已就绪"
echo ""

echo "[2/4] 正在编译项目..."
mvn clean compile -DskipTests -q
if [ $? -ne 0 ]; then
    echo "[错误] 编译失败"
    exit 1
fi
echo "[OK] 编译成功"
echo ""

echo "[3/4] 正在启动后端服务..."
cd yuncode-admin
mvn spring-boot:run > ../logs/admin.log 2>&1 &
ADMIN_PID=$!
echo "[OK] 后端服务已启动 (PID: $ADMIN_PID)"
cd ..

echo "[4/4] 正在启动网关服务..."
cd yuncode-gateway
mvn spring-boot:run > ../logs/gateway.log 2>&1 &
GATEWAY_PID=$!
echo "[OK] 网关服务已启动 (PID: $GATEWAY_PID)"
cd ..

# 保存 PID
mkdir -p logs
echo $ADMIN_PID > logs/admin.pid
echo $GATEWAY_PID > logs/gateway.pid

echo ""
echo "================================================"
echo ""
echo "       [启动完成] 服务信息"
echo ""
echo "    [后端服务] http://localhost:8080"
echo "       - API 地址: http://localhost:8080/api"
echo "       - API 文档: http://localhost:8080/api/doc.html"
echo "       - 日志文件: logs/admin.log"
echo ""
echo "    [网关服务] http://localhost:9000"
echo "       - 网关状态: http://localhost:9000/gateway/status"
echo "       - 网关路由: http://localhost:9000/gateway/routes"
echo "       - 日志文件: logs/gateway.log"
echo ""
echo "    [前端配置] 修改 API 地址为 http://localhost:9000/api"
echo ""
echo "    [事件系统] http://localhost:8080/api/event/types"
echo ""
echo "================================================"
echo ""
echo "[提示] 使用 tail -f logs/admin.log 查看后端日志"
echo "[提示] 使用 tail -f logs/gateway.log 查看网关日志"
echo "[提示] 使用 ./stop.sh 可以一键停止所有服务"
echo ""
