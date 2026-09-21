#!/bin/bash
# JWT 密钥生成工具
# 生成 32 字节随机十六进制密钥

KEY=$(openssl rand -hex 32 2>/dev/null || node -e "console.log(require('crypto').randomBytes(32).toString('hex'))" 2>/dev/null)

if [ -z "$KEY" ]; then
    echo "错误：需要 openssl 或 node.js 来生成密钥"
    exit 1
fi

echo "========================================="
echo "  JWT 密钥生成成功"
echo "========================================="
echo ""
echo "密钥: $KEY"
echo ""
echo "设置环境变量:"
echo "  export JWT_SECRET_KEY=\"$KEY\""
echo ""
echo "或写入 .env 文件:"
echo "  JWT_SECRET_KEY=$KEY" > .env
echo "  已写入 .env 文件"
echo "========================================="
