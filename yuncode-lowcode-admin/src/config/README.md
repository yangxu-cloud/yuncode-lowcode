# 后端接口配置说明

## 配置文件位置

所有后端接口相关配置都在 `src/config/index.ts` 文件中。

## 环境变量文件

- `.env.example` - 环境变量示例文件
- `.env.development` - 开发环境配置
- `.env.production` - 生产环境配置

## 配置说明

### 方式一：使用环境变量（推荐）

1. 复制 `.env.example` 为 `.env.local`：
```bash
cp .env.example .env.local
```

2. 修改 `.env.local` 中的配置：
```env
# 后端 API 地址
VITE_API_BASE_URL=http://localhost:8080
```

### 方式二：直接修改配置文件

编辑 `src/config/index.ts`：

```typescript
export const apiConfig = {
  // 后端服务地址
  baseURL: "http://localhost:8080",

  // 请求超时时间（毫秒）
  timeout: 10000,
};
```

## 开发环境配置

### 选项 1：直接连接后端

适用于后端和前端分开开发的情况。

在 `.env.development` 中配置：
```env
VITE_API_BASE_URL=http://localhost:8080
```

### 选项 2：使用 Vite 代理

适用于需要解决跨域问题的情况。

1. 在 `.env.development` 中配置：
```env
VITE_API_BASE_URL=/api
```

2. 在 `vite.config.ts` 中配置代理：
```typescript
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})
```

## 生产环境配置

在 `.env.production` 中配置：
```env
# 使用相对路径，通过 Nginx 代理
VITE_API_BASE_URL=/api
```

## 常见配置示例

### 本地开发（后端在 8080 端口）
```env
VITE_API_BASE_URL=http://localhost:8080
```

### 本地开发（后端在 9090 端口）
```env
VITE_API_BASE_URL=http://localhost:9090
```

### 局域网访问（后端在其他机器）
```env
VITE_API_BASE_URL=http://192.168.1.100:8080
```

### 生产环境（Nginx 代理）
```env
VITE_API_BASE_URL=/api
```

## 配置优先级

1. `.env.local` - 最高优先级（会被 git 忽略）
2. `.env.development` / `.env.production` - 环境特定配置
3. `.env.example` - 默认配置
4. `src/config/index.ts` - 代码中的默认值

## 注意事项

1. **修改环境变量后需要重启开发服务器**
2. `.env.local` 文件会被 git 忽略，适合存放敏感信息
3. 不要在代码中硬编码后端地址
4. 生产环境部署时，确保 Nginx 配置了正确的代理规则

## Nginx 配置示例

生产环境 Nginx 配置：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    location / {
        root /var/www/yuncode-lowcode-admin;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 代理
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```
