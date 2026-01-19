# Maven 依赖问题诊断和修复指南

## 问题描述
```
程序包 cn.dev33.satoken.stp 不存在
```

## 可能的原因

### 1. Maven 依赖未下载完成
Sa-Token 相关依赖可能还没有从 Maven 中央仓库下载完成。

### 2. IDEA 缓存问题
IDEA 可能还没有识别到新添加的依赖。

### 3. Maven 版本冲突
本地 Maven 的 `settings.xml` 配置可能有问题。

---

## 解决方案

### 方案 1：强制更新 Maven 依赖（推荐）

在 IDEA 的 Terminal 中执行：

```bash
cd yuncode-lowcode-boot

# 清理并重新下载依赖
mvn clean install -U

# 或者只下载依赖（不编译）
mvn dependency:resolve -U
```

**参数说明：**
- `-U` - 强制检查快照和依赖更新
- `clean` - 清理之前的编译结果

---

### 方案 2：在 IDEA 中刷新 Maven 项目

1. 打开 IDEA 右侧的 Maven 面板
2. 点击刷新按钮（Reload All Maven Projects）
3. 等待依赖下载完成

![刷新 Maven](https://www.jetbrains.com/help/idea/refreshing-maven-projects.png)

---

### 方案 3：清理 IDEA 缓存

1. **File** → **Invalidate Caches...**
2. 勾选以下选项：
   - ✅ Clear file system cache and Local History
   - ✅ Clear downloaded shared indexes
3. 点击 **Invalidate and Restart**

---

### 方案 4：检查 Maven 配置

检查 `~/.m2/settings.xml` 文件（或自定义 Maven 配置）：

```xml
<settings>
    <mirrors>
        <!-- 使用阿里云镜像（推荐） -->
        <mirror>
            <id>aliyun</id>
            <mirrorOf>central</mirrorOf>
            <name>Aliyun Maven</name>
            <url>https://maven.aliyun.com/repository/public</url>
        </mirror>
    </mirrors>
</settings>
```

---

### 方案 5：手动检查依赖

检查 `yuncode-auth/pom.xml` 是否包含以下依赖：

```xml
<dependencies>
    <!-- Sa-Token 权限认证 -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-spring-boot3-starter</artifactId>
    </dependency>

    <!-- Sa-Token 整合 Redis -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-redis-jackson</artifactId>
    </dependency>

    <!-- Sa-Token 整合 JWT -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-jwt</artifactId>
    </dependency>
</dependencies>
```

---

## 验证依赖是否正确安装

### 1. 检查本地 Maven 仓库

```bash
# 检查 Sa-Token 依赖是否存在
ls -la ~/.m2/repository/cn/dev33/sa-token/

# 应该看到以下目录：
# sa-token-jwt/
# sa-token-redis-jackson/
# sa-token-spring-boot3-starter/
```

### 2. 查看 Maven 依赖树

```bash
cd yuncode-lowcode-boot/yuncode-auth
mvn dependency:tree
```

应该看到：
```
[INFO] +- cn.dev33:sa-token-spring-boot3-starter:jar:1.38.0:compile
[INFO] |  +- cn.dev33:sa-token-core:jar:1.38.0:compile
[INFO] |  +- cn.dev33:sa-token-jwt:jar:1.38.0:compile
[INFO] +- cn.dev33:sa-token-redis-jackson:jar:1.38.0:compile
```

---

## 临时解决方案

如果上述方案都不行，可以暂时注释掉 JWT 配置类：

### 1. 暂时禁用 SaTokenJwtConfig.java

将文件重命名：
```bash
mv SaTokenJwtConfig.java SaTokenJwtConfig.java.bak
```

或者添加 `@Configuration` 注解的注释：
```java
//@Configuration  // 暂时禁用
public class SaTokenJwtConfig {
    // ...
}
```

### 2. 修改 application.yml

```yaml
# Sa-Token JWT 配置
sa-token-jwt:
  enable: false  # 暂时关闭 JWT
```

这样可以使用 Sa-Token 的基础模式（不使用 JWT），等依赖问题解决后再启用。

---

## 检查网络连接

如果使用 VPN 或代理，可能需要配置 Maven 代理：

**~/.m2/settings.xml**
```xml
<settings>
    <proxies>
        <proxy>
            <id>proxy</id>
            <active>true</active>
            <protocol>http</protocol>
            <host>proxy.host.com</host>
            <port>8080</port>
        </proxy>
    </proxies>
</settings>
```

---

## 常见错误和解决方法

### 错误 1: Could not resolve dependencies

**原因：** 无法连接到 Maven 仓库

**解决：** 配置阿里云镜像（参考方案 4）

### 错误 2: NullPointerException in StpLogic

**原因：** JWT 配置类加载失败

**解决：** 暂时禁用 JWT 配置类（参考临时解决方案）

### 错误 3: ClassNotFound: cn.dev33.satoken.stp.StpUtil

**原因：** Sa-Token 核心包未加载

**解决：** 执行 `mvn clean install -U`

---

## 推荐操作步骤

1. **首先尝试方案 1**（强制更新 Maven 依赖）
2. **如果还不行，尝试方案 2**（在 IDEA 中刷新 Maven）
3. **检查网络连接**（确保能访问 Maven 仓库）
4. **如果网络有问题，配置阿里云镜像**
5. **最后尝试方案 3**（清理 IDEA 缓存）

---

## 联系支持

如果以上方案都无法解决，请提供以下信息：

1. Maven 版本：`mvn -v`
2. Java 版本：`java -version`
3. IDEA 版本：Help → About
4. 完整错误日志：`mvn clean install > error.log`

---

**最后更新**: 2024-01-17
