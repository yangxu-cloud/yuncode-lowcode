# FactoryBeanObjectType 错误排查指南

## 问题描述

```
java.lang.IllegalArgumentException: Invalid value type for attribute 'factoryBeanObjectType': java.lang.String
```

## 已完成的修复

✅ 1. 升级 MyBatis-Plus 版本：3.5.5 → 3.5.7
✅ 2. 移除所有 Mapper 接口上的 `@Mapper` 注解
✅ 3. 使用 `@MapperScan` 统一扫描 Mapper
✅ 4. 添加 MyBatisPlusConfig 配置类
✅ 5. 修改 @MapperScan 使用具体包路径数组

## 需要手动操作的重置步骤

### 方法 1: IDEA 缓存清理（推荐）

1. **在 IDEA 中操作**：
   ```
   File → Invalidate Caches... → 勾选以下选项：
   ✓ Clear file system cache and Local History
   ✓ Clear downloaded shared indexes
   ✓ Clear VCS Log caches and indexes
   → 点击 "Invalidate and Restart"
   ```

2. **重新构建项目**：
   ```
   Build → Rebuild Project
   ```

3. **重新加载 Maven**：
   ```
   右侧 Maven 面板 → 点击刷新按钮
   ```

### 方法 2: 命令行清理（如果 IDEA 不可用）

```bash
# Windows (PowerShell)
cd "c:\workspace\ai progect\yuncode-lowcode\yuncode-lowcode-boot"
Remove-Item -Recurse -Force target
Remove-Item -Recurse -Force .idea

# Linux/Mac
cd /path/to/yuncode-lowcode/yuncode-lowcode-boot
rm -rf target .idea
```

### 方法 3: 强制更新 Maven 依赖

```bash
# 删除本地仓库的 MyBatis-Plus 缓存
rm -rf ~/.m2/repository/com/baomidou/mybatis-plus-boot-starter
rm -rf ~/.m2/repository/com/baomidou/mybatis-plus-core

# 重新下载依赖
mvn dependency:purge-local-repository -DmanualInclude="com.baomidou:mybatis-plus-boot-starter"
mvn clean install -U
```

### 方法 4: 检查 Maven 设置

1. **确认 Maven 版本**：
   ```bash
   mvn -version
   # 建议使用 Maven 3.6.3 或更高版本
   ```

2. **检查 settings.xml**：
   ```xml
   <!-- ~/.m2/settings.xml -->
   <mirrors>
     <mirror>
       <id>aliyun</id>
       <mirrorOf>central</mirrorOf>
       <name>Aliyun Maven</name>
       <url>https://maven.aliyun.com/repository/public</url>
     </mirror>
   </mirrors>
   ```

## 验证修复

### 1. 检查依赖版本

在 IDEA 中查看：
```
File → Project Structure → Modules → Dependencies
```

确认 `mybatis-plus-boot-starter` 版本为 **3.5.7**

### 2. 检查 Mapper 配置

确认以下文件已修改：
- ✅ [pom.xml](yuncode-lowcode-boot/pom.xml) - mybatis-plus.version=3.5.7
- ✅ [YuncodeAdminApplication.java](yuncode-lowcode-boot/yuncode-admin/src/main/java/com/yuncode/admin/YuncodeAdminApplication.java) - @MapperScan 配置
- ✅ [SysLoginLogMapper.java](yuncode-lowcode-boot/yuncode-system/src/main/java/com/yuncode/system/mapper/SysLoginLogMapper.java) - 无 @Mapper 注解
- ✅ [SysUserMapper.java](yuncode-lowcode-boot/yuncode-system/src/main/java/com/yuncode/system/mapper/SysUserMapper.java) - 无 @Mapper 注解
- ✅ [SysTenantMapper.java](yuncode-lowcode-boot/yuncode-tenant/src/main/java/com/yuncode/tenant/mapper/SysTenantMapper.java) - 无 @Mapper 注解

### 3. 测试启动

```bash
# 在 IDEA 中直接运行
右键点击 YuncodeAdminApplication → Run 'YuncodeAdminApplication'
```

## 如果仍然失败

### 临时方案：排除 MyBatis 自动配置

如果上述方法都无效，可以尝试排除 MyBatis 的自动配置：

```java
// YuncodeAdminApplication.java
@SpringBootApplication(exclude = {
    // org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration.class
})
```

**⚠️ 警告**：这只是临时方案，会导致 MyBatis 功能无法使用。

### 替代方案：降级 Spring Boot

如果问题持续存在，可以考虑降级 Spring Boot 版本：

```xml
<!-- pom.xml -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.1.5</version> <!-- 从 3.2.0 降级到 3.1.5 -->
    <relativePath/>
</parent>
```

## 联系支持

如果以上所有方法都无法解决问题，请提供以下信息：

1. 完整的错误堆栈
2. Maven 版本：`mvn -version`
3. Java 版本：`java -version`
4. IDEA 版本：Help → About
5. `mvn dependency:tree` 的输出

## 相关链接

- [MyBatis-Plus 官方文档](https://baomidou.com/)
- [Spring Boot 3.x 迁移指南](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide)
- [MyBatis-Plus Spring Boot 3 兼容性](https://baomidou.com/pages/779a6e/)
