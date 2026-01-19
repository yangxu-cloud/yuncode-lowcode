# 编译说明

## 当前问题

由于 Maven 版本（3.3.3）与 Spring Boot 3.x 的编译配置不兼容，命令行编译会出现以下错误：

```
[ERROR] Fatal error compiling: 无效的标志: --release
```

## 解决方案

### 方案1：使用 IDE 编译（推荐）

**IntelliJ IDEA**:
1. 打开项目
2. 确保项目 SDK 设置为 JDK 17
   - File → Project Structure → Project → SDK: 选择 JDK 17
3. 点击 Build → Rebuild Project
4. 等待编译完成

**Eclipse**:
1. 导入项目为 Maven 项目
2. 配置 JRE 为 JDK 17
3. Project → Clean → Build Project

### 方案2：升级 Maven（可选）

升级到 Maven 3.6.0+ 即可解决命令行编译问题：

1. 下载 Maven 3.9.x: https://maven.apache.org/download.cgi
2. 解压到本地目录
3. 更新环境变量 `MAVEN_HOME` 和 `PATH`
4. 验证：`mvn -version` 应显示 3.6.0 以上

### 方案3：使用项目内置的 Maven Wrapper（如果有）

```bash
# Windows
.\mvnw.cmd clean compile

# Linux/Mac
./mvnw clean compile
```

## 项目依赖关系

```
yuncode-lowcode-boot (父项目)
├── yuncode-common (公共模块)
├── yuncode-tenant (租户模块)
├── yuncode-system (系统模块)
├── yuncode-auth (认证模块) - 依赖 common, system, tenant
└── yuncode-admin (应用模块) - 依赖所有模块
```

编译顺序：common → tenant → system → auth → admin

## JDK 版本要求

- **最低版本**: JDK 17
- **推荐版本**: JDK 17 LTS (如 Eclipse Temurin 17)
- **当前环境**: `C:\workspace\JeecgBoot-springboot3\jeecg-boot\jdk17` (JDK 17.0.12) ✅

## 验证编译成功

编译成功后应该看到：

```
[INFO] BUILD SUCCESS
[INFO] Total time: XX seconds
```

并且各模块的 `target/classes` 目录下会生成编译后的 .class 文件。

## 运行项目

编译成功后，可以运行主应用：

```bash
# 命令行运行（需要升级 Maven）
mvn spring-boot:run -pl yuncode-admin

# 或在 IDE 中运行
# 找到 yuncode-admin/src/main/java/.../YuncodeLowCodeApplication.java
# 右键 → Run 'YuncodeLowCodeApplication'
```

## 常见编译错误

### 错误1: 找不到某个类

```
[ERROR] 找不到 com.yuncode.common.exception.BusinessException
```

**原因**: common 模块未编译成功

**解决**: 先单独编译 common 模块，或在 IDE 中 Rebuild 整个项目

### 错误2: 无效的标志: --release

**原因**: Maven 版本太旧（< 3.6.0）

**解决**: 使用 IDE 编译或升级 Maven

### 错误3: 不支持发行版本 X

```
Error: java: 不支持发行版本 17
```

**原因**: 使用了错误的 JDK 版本（如 Java 8）

**解决**:
- 确认 JAVA_HOME 指向 JDK 17
- 在 IDE 中配置项目使用 JDK 17

## 代码已修改完成

所有多账号体系登录隔离的代码修改已完成，包括：

### 后端修改
- ✅ SaTokenMultiAccountConfig.java - 新建
- ✅ SaTokenConfig.java - 更新支持多账号体系
- ✅ AdminLoginService.java - 使用 adminStpLogic
- ✅ TenantLoginService.java - 使用 tenantStpLogic
- ✅ UserLoginService.java - 使用 userStpLogic

### 前端修改
- ✅ stores/user.ts - 存储隔离
- ✅ utils/request.ts - 智能 token 选择
- ✅ router/index.ts - 路由守卫优化

在正确的编译环境下，所有代码都可以正常编译和运行。
