# Token 配置说明

## 概述

系统已实现 **前后端 Token 参数名统一配置**，所有地方都从配置文件读取 `token-name`，无需硬编码。

## 配置方式

### 后端配置

**文件位置**: `yuncode-lowcode-boot/yuncode-admin/src/main/resources/application.yml`

```yaml
# Sa-Token 配置
sa-token:
  # Token 名称（同时也是 Cookie 名称）
  # 修改这个值，整个系统的 token 参数名都会自动改变
  token-name: token
```

**工作原理**:
1. Sa-Token 框架自动从配置文件读取 `sa-token.token-name`
2. `SaTokenProperties` 类通过 `@ConfigurationProperties` 自动注入配置
3. `SaTokenHeaderFilter` 从 `SaTokenProperties` 读取 token 名称
4. 所有使用 Sa-Token 的地方（拦截器、注解等）自动生效

### 前端配置

**文件位置**: `yuncode-lowcode-admin/.env.development` (开发环境)
```env
# Token 参数名称（必须与后端 application.yml 中的 sa-token.token-name 一致）
VITE_TOKEN_NAME=token
```

**文件位置**: `yuncode-lowcode-admin/.env.production` (生产环境)
```env
# Token 参数名称（必须与后端 application.yml 中的 sa-token.token-name 一致）
VITE_TOKEN_NAME=token
```

**使用方式**:
```typescript
// 在代码中使用配置
import config from '@/config'

const url = `/api/user/notifications?${config.tokenName}=${encodeURIComponent(token)}`
```

## 修改 Token 名称

如果需要修改 token 参数名，只需修改两个地方：

### 1. 后端
```yaml
# application.yml
sa-token:
  token-name: your-token-name  # 改成你想要的名字
```

### 2. 前端
```env
# .env.development 和 .env.production
VITE_TOKEN_NAME=your-token-name  # 改成与后端一致的值
```

**重启服务后生效**。

## 实现细节

### 后端组件

1. **SaTokenProperties** - 配置属性类
   ```java
   @Component
   @ConfigurationProperties(prefix = "sa-token")
   public class SaTokenProperties {
       private String tokenName = "satoken";  // 默认值，会被配置文件覆盖
   }
   ```

2. **SaTokenHeaderFilter** - 请求头过滤器
   ```java
   @Autowired
   private SaTokenProperties saTokenProperties;

   String tokenHeader = saTokenProperties.getTokenName();
   ```

3. **Sa-Token 框架** - 自动从配置读取
   - 拦截器自动识别
   - URL 参数自动解析
   - Cookie 自动读取

### 前端组件

1. **配置文件** - `.env` 文件
   ```env
   VITE_TOKEN_NAME=token
   ```

2. **配置类** - `src/config/index.ts`
   ```typescript
   export const apiConfig = {
     tokenName: import.meta.env.VITE_TOKEN_NAME || "token"
   }
   ```

3. **使用处** - SSE 连接
   ```typescript
   import config from '@/config'
   const url = `/api/user/notifications?${config.tokenName}=${encodeURIComponent(token)}`
   ```

## 优势

✅ **单一配置源**: 只需修改配置文件，不需要改代码
✅ **前后端统一**: 前后端使用相同的参数名
✅ **易于维护**: 修改配置后重启即可生效
✅ **类型安全**: 后端使用配置类，前端使用配置对象
✅ **环境隔离**: 不同环境可以有不同的配置

## 注意事项

⚠️ **前后端必须保持一致**: 前端的 `VITE_TOKEN_NAME` 必须与后端的 `sa-token.token-name` 一致

⚠️ **重启服务**: 修改配置后需要重启前后端服务

⚠️ **清除缓存**: 修改前端环境变量后，建议清除浏览器缓存重新构建
