# 快速启动指南

## 前置条件检查

在启动项目之前，请确保以下环境已安装：

```bash
# 检查 Java 版本（需要 JDK 17+）
java -version

# 检查 Node.js 版本（需要 Node.js 20+）
node -v

# 检查 MySQL 版本（需要 MySQL 8.0+）
mysql --version

# 检查 Redis 是否运行
redis-cli ping

# 检查 Maven 版本（需要 Maven 3.8+）
mvn -v
```

## 一、数据库初始化

### 1. 创建数据库

```bash
# 登录 MySQL
mysql -u root -p

# 执行以下 SQL
CREATE DATABASE yuncode_lowcode CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE yuncode_lowcode;

# 退出 MySQL
EXIT;
```

### 2. 导入表结构和初始数据

```bash
# 方式1：在 MySQL 客户端中执行 docs/DATABASE.md 中的 SQL 语句

# 方式2：使用命令行（假设已将 SQL 保存到文件中）
mysql -u root -p yuncode_lowcode < docs/init.sql
```

### 3. 验证数据库

```bash
mysql -u root -p yuncode_lowcode
```

```sql
-- 查看所有表
SHOW TABLES;

-- 应该看到以下表：
-- sys_tenant
-- sys_user
-- sys_role
-- sys_menu
-- sys_user_role
-- sys_role_menu
-- sys_oper_log

-- 查看初始数据
SELECT * FROM sys_tenant;
SELECT * FROM sys_user WHERE username = 'admin';
```

## 二、后端启动

### 1. 配置文件修改

编辑 [yuncode-lowcode-boot/yuncode-admin/src/main/resources/application.yml](../yuncode-lowcode-boot/yuncode-admin/src/main/resources/application.yml)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/yuncode_lowcode?...
    username: root          # 修改为你的 MySQL 用户名
    password: root          # 修改为你的 MySQL 密码

  data:
    redis:
      host: localhost       # 修改为你的 Redis 地址
      port: 6379            # 修改为你的 Redis 端口
      password:             # 如果有密码，填写密码
```

### 2. 编译项目

```bash
cd yuncode-lowcode-boot

# 清理并编译所有模块
mvn clean install -DskipTests

# 如果编译成功，会看到 BUILD SUCCESS
```

### 3. 启动应用

```bash
cd yuncode-admin

# 方式1：使用 Maven 启动
mvn spring-boot:run

# 方式2：使用 IDE 启动
# 在 IDEA 中打开 YuncodeAdminApplication.java，右键运行
```

### 4. 验证后端启动

```bash
# 查看日志，应该看到：
# ======================================
#  Yuncode LowCode Platform Started!
# ======================================

# 访问健康检查
curl http://localhost:8080/api/actuator/health

# 访问 API 文档
# 浏览器打开：http://localhost:8080/api/doc.html
```

## 三、前端启动

### 1. 安装依赖

```bash
cd yuncode-lowcode-admin

# 使用 npm 安装
npm install

# 或使用 pnpm（更快）
pnpm install

# 或使用 yarn
yarn install
```

### 2. 配置代理（已配置）

查看 [vite.config.ts](../yuncode-lowcode-admin/vite.config.ts) 中的代理配置：

```typescript
server: {
  port: 3000,
  proxy: {
    "/api": {
      target: "http://localhost:8080",
      changeOrigin: true
    }
  }
}
```

### 3. 启动开发服务器

```bash
# 使用 npm
npm run dev

# 或使用 pnpm
pnpm dev

# 或使用 yarn
yarn dev
```

### 4. 访问前端

```bash
# 浏览器打开
http://localhost:3000

# 应该看到登录页面
```

## 四、测试登录

### 1. 在前端登录页面输入

```
用户名：admin
密码：admin123
```

### 2. 登录成功后

- 应该跳转到首页仪表盘
- 可以看到统计数据
- 左侧菜单显示系统管理

## 五、常见问题

### 1. 端口被占用

**问题：** `Port 8080 is already in use`

**解决：**
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <进程ID> /F

# Linux/Mac
lsof -ti:8080 | xargs kill -9
```

### 2. 数据库连接失败

**问题：** `Could not create connection to database server`

**解决：**
- 检查 MySQL 是否启动
- 检查用户名密码是否正确
- 检查数据库名称是否正确

### 3. Redis 连接失败

**问题：** `Unable to connect to Redis`

**解决：**
```bash
# 启动 Redis
# Windows
redis-server.exe

# Linux/Mac
redis-server

# 验证连接
redis-cli ping
# 应该返回 PONG
```

### 4. 前端依赖安装失败

**问题：** `npm install` 报错

**解决：**
```bash
# 清理缓存
npm cache clean --force

# 使用淘宝镜像
npm config set registry https://registry.npmmirror.com

# 重新安装
npm install
```

### 5. Maven 编译失败

**问题：** `mvn clean install` 报错

**解决：**
```bash
# 跳过测试编译
mvn clean install -DskipTests

# 如果还是失败，清理后重试
mvn clean
rm -rf ~/.m2/repository/com/yuncode
mvn install -DskipTests
```

## 六、开发环境配置建议

### 1. IDE 配置

**IDEA（推荐用于后端）：**
- 安装 Lombok 插件
- 安装 MyBatis 插件
- 设置 JDK 为 17
- 启用注解处理

**VSCode（推荐用于前端）：**
- 安装 Volar 插件（Vue 3 支持）
- 安装 TypeScript 插件
- 安装 ESLint 插件
- 安装 Prettier 插件

### 2. Git 配置

```bash
# 创建 .gitignore
cd yuncode-lowcode

# 后端 .gitignore
cat > .gitignore << EOF
# IDE
.idea/
*.iml
.vscode/

# Build
target/
dist/
node_modules/

# Logs
*.log

# OS
.DS_Store
Thumbs.db
EOF
```

### 3. 环境变量配置（可选）

```bash
# 创建 .env 文件（前端）
cd yuncode-lowcode-admin
cat > .env.development << EOF
VITE_API_BASE_URL=http://localhost:8080/api
VITE_APP_TITLE=Yuncode LowCode
EOF
```

## 七、下一步

启动成功后，你可以：

1. ✅ 查看 API 文档：http://localhost:8080/api/doc.html
2. ✅ 访问前端页面：http://localhost:3000
3. ⏳ 开发新的功能模块
4. ⏳ 实现用户登录认证
5. ⏳ 实现用户管理 CRUD
6. ⏳ 实现导航管理功能

## 八、获取帮助

- 查看项目文档：[docs/README.md](README.md)
- 查看数据库设计：[docs/DATABASE.md](DATABASE.md)
- 查看项目总结：[docs/PROJECT_SUMMARY.md](PROJECT_SUMMARY.md)

祝开发愉快！🚀
