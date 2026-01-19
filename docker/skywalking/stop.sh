#!/bin/bash

# SkyWalking 停止脚本

set -e

echo "=================================="
echo "停止 SkyWalking 服务"
echo "=================================="

# 停止所有服务
docker-compose down

echo ""
echo -e "${GREEN}SkyWalking 服务已停止${NC}"
echo ""
echo "如需删除数据卷，请运行："
echo "  docker-compose down -v"
echo ""
