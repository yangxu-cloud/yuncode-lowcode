#!/bin/bash

# SkyWalking 启动脚本
# 适用于 Linux/MacOS

set -e

echo "=================================="
echo "SkyWalking 启动脚本"
echo "=================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查 Docker
echo -e "${NC}检查 Docker 是否安装..."
if ! command -v docker &> /dev/null
then
    echo -e "${RED}错误: Docker 未安装，请先安装 Docker${NC}"
    exit 1
fi

if ! command -v docker-compose &> /dev/null
then
    echo -e "${RED}错误: Docker Compose 未安装，请先安装 Docker Compose${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Docker 和 Docker Compose 已安装${NC}"

# 检查端口占用
echo -e "${NC}检查端口占用..."

PORTS=(9200 9300 11800 12800 8088 9090)
OCCUPIED=0

for PORT in "${PORTS[@]}"; do
    if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null 2>&1 ; then
        echo -e "${YELLOW}警告: 端口 $PORT 已被占用${NC}"
        OCCUPIED=$((OCCUPIED + 1))
    fi
done

if [ $OCCUPIED -gt 0 ]; then
    echo -e "${RED}错误: 有端口被占用，请先检查并关闭占用这些端口的程序${NC}"
    exit 1
fi

echo -e "${GREEN}✓ 所有端口可用${NC}"

# 创建必要的目录
echo -e "${NC}创建数据目录..."
mkdir -p data/es
mkdir -p data/prometheus
mkdir -p config/oap
mkdir -p config/prometheus
echo -e "${GREEN}✓ 数据目录创建成功${NC}"

# 启动 SkyWalking
echo -e "${NC}启动 SkyWalking 服务..."
docker-compose up -d

# 等待服务启动
echo -e "${NC}等待服务启动..."
sleep 10

# 检查服务状态
echo -e "${NC}检查服务状态..."
docker-compose ps

# 检查 Elasticsearch
echo -e "${NC}等待 Elasticsearch 就绪..."
MAX_RETRIES=30
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -s http://localhost:9200/_cluster/health | grep -q '"status":"green"\|"status":"yellow"'; then
        echo -e "${GREEN}✓ Elasticsearch 已就绪${NC}"
        break
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
    echo -n "."
    sleep 2
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo -e "${RED}错误: Elasticsearch 启动超时${NC}"
    docker-compose logs elasticsearch
    exit 1
fi

# 检查 OAP Server
RETRY_COUNT=0
while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -s http://localhost:12800/healthcheck | grep -q "HEALTH"; then
        echo -e "${GREEN}✓ SkyWalking OAP Server 已就绪${NC}"
        break
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
    sleep 2
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo -e "${RED}错误: OAP Server 启动超时${NC}"
    docker-compose logs oap
    exit 1
fi

echo ""
echo "=================================="
echo -e "${GREEN}SkyWalking 启动成功！${NC}"
echo "=================================="
echo ""
echo -e "${GREEN}访问地址：${NC}"
echo "  - SkyWalking UI: http://localhost:8088"
echo "  - Prometheus:   http://localhost:9090"
echo "  - Elasticsearch: http://localhost:9200"
echo ""
echo -e "${YELLOW}默认账号密码：${NC}"
echo "  - 默认无密码"
echo ""
echo -e "${GREEN}停止服务：${NC}"
echo "  ./stop.sh"
echo ""
echo -e "${GREEN}查看日志：${NC}"
echo "  docker-compose logs -f [oap|ui|elasticsearch|prometheus]"
echo ""
