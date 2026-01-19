# JDK 17 环境配置指南

## 为什么需要 JDK 17

本项目使用以下技术栈，这些技术要求 JDK 17 作为最低版本：

- **Spring Boot 3.1.8** - 要求 JDK 17+
- **Jakarta EE 10** - 使用 `jakarta.*` 包（Java 17+ 标准）
- **Sa-Token 1.38.0** - 最新版本需要 JDK 17+
- **Hutool 5.8.24** - 需要 JDK 17+

## 当前环境问题

- **系统 Java 版本**: Java 8 (1.8.0_162)
- **Maven 版本**: 3.3.3（不支持 `--release` 编译参数）
- **项目要求**: JDK 17+

## 解决方案

### 方案1：安装 JDK 17（推荐）

#### Windows 系统

1. **下载 JDK 17**
   - Oracle JDK 17: https://www.oracle.com/java/technologies/downloads/#java17
   - OpenJDK 17: https://adoptium.net/temurin/releases/?version=17

2. **安装 JDK 17**
   - 运行安装程序，按照默认设置安装
   - 记住安装路径（例如：`C:\Program Files\Java\jdk-17`）

3. **配置环境变量**

   右键"此电脑" → 属性 → 高级系统设置 → 环境变量

   **系统变量**：
   - 新建 `JAVA_HOME` = `C:\Program Files\Java\jdk-17`
   - 编辑 `Path`，添加：`%JAVA_HOME%\bin`

   **验证安装**：
   ```bash
   java -version
   # 应该显示：java version "17.x.x"
   ```

4. **升级 Maven（可选但推荐）**

   Maven 3.3.3 不支持 `--release` 参数，建议升级到 3.6.0+

   - 下载 Maven 3.9.x: https://maven.apache.org/download.cgi
   - 解压到：`C:\tools\apache-maven-3.9.9`
   - 配置环境变量：
     - `MAVEN_HOME` = `C:\tools\apache-maven-3.9.9`
     - `Path` 添加：`%MAVEN_HOME%\bin`

   **验证安装**：
   ```bash
   mvn -version
   # 应该显示：Apache Maven 3.9.9
   ```

### 方案2：使用 IDE 编译（无需修改系统环境）

#### IntelliJ IDEA

1. **配置项目 JDK**
   - File → Project Structure → Project
   - SDK: 选择或添加 JDK 17
   - Language Level: 17

2. **配置 Maven JDK**
   - File → Settings → Build, Execution, Deployment → Build Tools → Maven
   - Maven home path: 使用 IDE 内置 Maven 或指定 Maven 路径
   - JDK for importer: 选择 JDK 17

3. **重新导入项目**
   - 右键 pom.xml → Maven → Reload Project
   - 点击 Build → Rebuild Project

#### Eclipse

1. **安装 JDK 17**
   - 下载并安装 JDK 17

2. **配置 Eclipse**
   - Window → Preferences → Java → Installed JREs
   - Add → Standard VM → 选择 JDK 17 安装目录
   - 勾选 JDK 17 为默认 JRE

3. **配置项目**
   - 右键项目 → Properties → Java Build Path
   - Libraries → 修改 JRE 为 JDK 17

4. **更新 Maven 项目**
   - 右键项目 → Maven → Update Project

### 方案3：使用 Docker（开发环境）

创建 `docker-compose.yml`：

```yaml
version: '3.8'
services:
  app:
    image: maven:3.9-eclipse-temurin-17
    working_dir: /app
    volumes:
      - .:/app
      - ~/.m2:/root/.m2
    ports:
      - "8080:8080"
    command: mvn spring-boot:run
```

运行：
```bash
docker-compose up
```

## 编译项目

### 使用命令行（需要 JDK 17 + Maven 3.6+）

```bash
cd "c:\workspace\ai progect\yuncode-lowcode\yuncode-lowcode-boot"

# 清理编译
mvn clean compile

# 运行测试
mvn test

# 打包
mvn clean package

# 跳过测试打包
mvn clean package -DskipTests
```

### 使用 IDE

1. IntelliJ IDEA: 右键项目 → Rebuild Project
2. Eclipse: Project → Clean → Build Project

## 验证环境

运行以下命令验证配置：

```bash
# 检查 Java 版本（需要 17+）
java -version

# 检查 Maven 版本（需要 3.6+）
mvn -version

# 检查环境变量
echo %JAVA_HOME%
echo %MAVEN_HOME%
```

预期输出示例：
```
java version "17.0.9"
Apache Maven 3.9.9
JAVA_HOME=C:\Program Files\Java\jdk-17
MAVEN_HOME=C:\tools\apache-maven-3.9.9
```

## 常见问题

### Q1: 编译错误 "无效的标志: --release"

**原因**: Maven 版本太旧（< 3.6.0）

**解决**:
- 升级 Maven 到 3.6.0+
- 或使用 IDE 编译
- 或删除 pom.xml 中的 maven-compiler-plugin 配置，使用默认配置

### Q2: 找不到 java.lang 包

**原因**: 使用了错误的 JDK 版本

**解决**:
- 确认 JAVA_HOME 指向 JDK 17
- 在 IDE 中配置项目使用 JDK 17
- 运行 `java -version` 确认版本

### Q3: Spring Boot 启动失败

**原因**: 代码使用了 Java 17 特性，但运行时使用的是 Java 8

**解决**:
- 确保运行时环境也是 JDK 17
- 检查 IDE 的 Run Configuration 使用的 JRE

## 下载链接

### JDK 17 发行版

- **Oracle JDK 17**: https://www.oracle.com/java/technologies/downloads/#java17
- **Eclipse Temurin (OpenJDK)**: https://adoptium.net/temurin/releases/?version=17
- **Amazon Corretto (OpenJDK)**: https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/downloads-list.html

### Maven

- **Maven 3.9.9**: https://maven.apache.org/download.cgi
- **Maven Archive**: https://archive.apache.org/dist/maven/maven-3/

## 推荐配置

**生产环境推荐**:
- JDK: Eclipse Temurin 17 LTS
- Maven: 3.9.9
- IDE: IntelliJ IDEA 2023.2+

**快速安装（Windows）**:
```powershell
# 使用 Chocolatey（如果已安装）
choco install openjdk17 maven

# 使用 Scoop
scoop install java17-lts maven
```

## 下一步

配置好 JDK 17 环境后：

1. ✅ 编译项目：`mvn clean compile`
2. ✅ 运行测试：`mvn test`
3. ✅ 启动应用：`mvn spring-boot:run`
4. ✅ 测试多账号登录功能

查看 [MULTI_ACCOUNT_TEST_STEPS.md](MULTI_ACCOUNT_TEST_STEPS.md) 了解测试步骤。
