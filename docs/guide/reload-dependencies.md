# Maven 依赖重新加载指南

## 问题：依赖无法解析

这是因为 Spring Boot 版本变更后，Maven 需要重新下载依赖。

---

## 解决方案

### 方案 1：在 IDEA 中重新加载（推荐）⭐

#### 步骤 1：打开 Maven 工具窗口
```
View → Tool Windows → Maven
```
或者点击 IDEA 右侧的 `Maven` 面板

#### 步骤 2：重新加载项目
找到并点击以下按钮之一：
- 🔄 **Reload All Maven Projects** （刷新图标）
- 或者右键项目名称 → **Maven** → **Reload Project**

#### 步骤 3：等待依赖下载完成
查看 IDEA 底部的进度条，等待所有依赖下载完成。

---

### 方案 2：使用 IDEA 的 Invalidate Caches

如果方案 1 不起作用：

```
File → Invalidate Caches...
→ 勾选：
  ✓ Clear file system cache and Local History
  ✓ Clear downloaded shared indexes
→ 点击 "Invalidate and Restart"
```

重启后，IDEA 会自动重新索引 Maven 依赖。

---

### 方案 3：删除 target 目录后重新构建

#### Windows (PowerShell)
```powershell
cd "c:\workspace\ai progect\yuncode-lowcode\yuncode-lowcode-boot"
Get-ChildItem -Recurse -Directory -Filter "target" | Remove-Item -Recurse -Force
```

#### Windows (CMD)
```cmd
cd "c:\workspace\ai progect\yuncode-lowcode\yuncode-lowcode-boot"
for /d /r . %d in (target) do @if exist "%d" rd /s /q "%d"
```

删除后，在 IDEA 中：
```
Build → Rebuild Project
```

---

### 方案 4：配置 Maven 镜像（加速下载）

如果依赖下载很慢，可以配置阿里云镜像。

#### 编辑 Maven settings.xml

文件位置：`~/.m2/settings.xml`
（Windows: `C:\Users\你的用户名\.m2\settings.xml`）

```xml
<settings>
    <mirrors>
        <!-- 阿里云公共仓库 -->
        <mirror>
            <id>aliyun-public</id>
            <mirrorOf>*</mirrorOf>
            <name>Aliyun Public</name>
            <url>https://maven.aliyun.com/repository/public</url>
        </mirror>
    </mirrors>

    <profiles>
        <profile>
            <id>aliyun</id>
            <repositories>
                <repository>
                    <id>aliyun-public</id>
                    <url>https://maven.aliyun.com/repository/public</url>
                    <releases>
                        <enabled>true</enabled>
                    </releases>
                    <snapshots>
                        <enabled>true</enabled>
                    </snapshots>
                </repository>
            </repositories>
            <pluginRepositories>
                <pluginRepository>
                    <id>aliyun-plugin</id>
                    <url>https://maven.aliyun.com/repository/public</url>
                    <releases>
                        <enabled>true</enabled>
                    </releases>
                    <snapshots>
                        <enabled>true</enabled>
                    </snapshots>
                </pluginRepository>
            </pluginRepositories>
        </profile>
    </profiles>

    <activeProfiles>
        <activeProfile>aliyun</activeProfile>
    </activeProfiles>
</settings>
```

配置后，重新加载 Maven 项目。

---

### 方案 5：检查 IDEA 的 Maven 设置

```
File → Settings → Build, Execution, Deployment → Build Tools → Maven
```

确认以下设置：
- **Maven home path**: 使用 Bundled (Maven 3) 或指向正确的 Maven 安装目录
- **User settings file**: `~/.m2/settings.xml`
- **Local repository**: 默认即可
- **Always update snapshots**: 勾选（可选）

---

## 验证依赖是否正确加载

### 在 IDEA 中检查：
```
File → Project Structure → Modules → Dependencies
```

应该能看到以下依赖（无红色波浪线）：
- ✅ spring-boot-starter-web:3.1.8
- ✅ mybatis-plus-boot-starter:3.5.7
- ✅ sa-token-spring-boot3-starter:1.38.0
- ✅ hutool-all:5.8.24
- ✅ knife4j-openapi3-jakarta-spring-boot-starter:4.5.0

---

## 当前项目配置

```xml
<!-- Spring Boot -->
<version>3.1.8</version>

<!-- MyBatis-Plus -->
<mybatis-plus.version>3.5.7</mybatis-plus.version>

<!-- Java -->
<java.version>17</java.version>
```

---

## 如果以上方法都不行

### 最后的解决方案：

1. **关闭 IDEA**
2. **手动删除以下目录**：
   ```
   c:\workspace\ai progect\yuncode-lowcode\yuncode-lowcode-boot\yuncode-admin\target
   c:\workspace\ai progect\yuncode-lowcode\yuncode-lowcode-boot\yuncode-system\target
   c:\workspace\ai progect\yuncode-lowcode\yuncode-lowcode-boot\yuncode-tenant\target
   c:\workspace\ai progect\yuncode-lowcode\yuncode-lowcode-boot\yuncode-common\target
   c:\workspace\ai progect\yuncode-lowcode\yuncode-lowcode-boot\yuncode-auth\target
   ```

3. **删除 IDEA 缓存**（可选）：
   ```
   删除项目根目录下的 .idea 文件夹
   ```

4. **重新打开 IDEA**，导入项目：
   ```
   File → Open → 选择 yuncode-lowcode-boot 文件夹
   ```

5. **等待 IDEA 索引完成**（看底部进度条）

---

## 联系支持

如果问题持续，请提供：
1. IDEA 版本：`Help → About`
2. Maven 版本：在 IDEA 终端运行 `mvn -version`
3. 具体错误截图
