# ADR-001: App 与平台分离架构

## 状态

2026-05-22 通过。等待实施。

## 背景

Yuncode LowCode 平台定位为低代码开发平台，未来由两类开发人员协作：

- **平台开发人员**：维护平台核心（common、auth、system、admin、gateway）
- **二级开发人员**：基于平台开发业务 App（如 qms0205），只关心自身业务逻辑

当前问题：
1. App 源码和平台源码混在同一个 Maven 项目里，多人开发互相干扰
2. `yuncode-common` 中既有平台内部基础设施又有 App 需要调用的 SDK，边界不清
3. 根 `pom.xml` 声明了所有 App 模块，新增 App 需要改根 POM
4. `apps/install/` 既是 Maven 源码目录又是运行时热加载目录，角色混淆

## 决策

### 1. App 和平台物理分离

App 源码从平台主项目中彻底移出，每个 App 独立维护：

```
平台项目（yuncode-lowcode-boot/）
├── yuncode-common/          ← 平台内部公共库
├── yuncode-platform-sdk/         ← [新增] 开放给二级开发的 SDK
├── yuncode-auth/
├── yuncode-system/
├── yuncode-admin/
├── yuncode-gateway/
├── apps/
│   └── install/             ← 运行时目录，存放构建好的 App JAR
│       └── com.yuncode.user.apps.xxx/
│           ├── lib/xxx.jar
│           └── metadata/    ← BO/表单/流程配置（未来扩展）
└── pom.xml                  ← 不再包含任何 App 模块

App 项目（独立仓库，如 qms0205/）
├── pom.xml                  ← 仅依赖 yuncode-platform-sdk
├── src/main/java/.../
├── metadata/
│   ├── bo-config.json
│   ├── form-config.json
│   └── workflow-config.json
└── deploy.bat               ← 构建后复制 JAR 到平台的 apps/install/ 目录
```

### 2. SDK 拆分

从 `yuncode-common` 中提取稳定的 SDK 接口到独立模块 `yuncode-platform-sdk`：

| 模块 | 内容 | 稳定性要求 |
|------|------|-----------|
| `yuncode-platform-sdk` | `Result`、`PageResult`、`BusinessException`、`SecurityUtil`、`AppContext`、元数据接口 | **高** — 大版本升级才改 |
| `yuncode-common` | 事件总线、TraceId、MyBatis-Plus 扩展、Sa-Token 扩展、内部工具 | **低** — 平台内部随时重构 |

二级开发人员只需要：
```xml
<dependency>
    <groupId>com.yuncode</groupId>
    <artifactId>yuncode-platform-sdk</artifactId>
    <version>1.x</version>
</dependency>
```

### 3. HotAppDeployer 不变

运行时架构已验证可行，不做改动：
- 启动时扫描 `apps/install/` 下所有子目录
- 以目录名（`com.yuncode.user.apps.xxx`）作为 appId
- 加载 `lib/*.jar` 中所有 Spring Bean
- 文件监听器实时检测 JAR 变更并热替换
- 元数据目录 `metadata/` 预留，后续扩展

### 4. appId 命名规范

保留 `com.yuncode.user.apps.` 前缀以区分业务 App：

```
目录名:  com.yuncode.user.apps.qms0205
appId:   com.yuncode.user.apps.qms0205
Maven artifactId: qms0205（App 独立项目中的标识）
```

### 5. 元数据驱动（远期）

App 打包时包含 `metadata/` 目录，部署时平台根据元数据自动构建：
- `bo-config.json` → 自动生成 BO 结构和数据库表
- `form-config.json` → 自动注册表单配置
- `workflow-config.json` → 自动构建流程定义
- `script-config.json` → 注册事件处理脚本

元数据是"开发环境构建、生产环境消费"的契约，格式需版本化管理。

## 影响

### 正面
- 平台和 App 独立演进，互不干扰
- 根 POM 不受 App 新增影响
- SDK 物理隔离，二级开发人员不可能引用到平台内部类
- 平台重构不影响已有 App

### 代价
- 需要一次性的模块拆分和代码搬移
- `yuncode-platform-sdk` 需要稳定的版本管理
- App 开发者需要独立建项目、引入 SDK、手动部署 JAR
- 平台本身的模块（auth、system、admin）需要同时依赖 common 和 sdk

## 实施步骤

1. 创建 `yuncode-platform-sdk` Maven 模块
2. 从 `yuncode-common` 搬运 SDK 类到新模块
3. 更新根 `pom.xml` 依赖关系和模块声明
4. 更新所有平台模块的依赖（auth、system、admin 等）
5. ~~根 POM 去掉 App 模块引用~~ → **已恢复**。App 模块仍保留在 root pom.xml 的 `<modules>` 中，确保 IDE 支持和统一编译。App 完全独立为单独仓库是远期目标，当前阶段保持一起编译更实用。
6. 更新 `deploy-app.sh` 脚本指向正确路径（已验证 `-pl` 方式仍可用）
7. 提交并验证编译
