# Yuncode LowCode 项目结构说明

## 📁 当前目录结构

```
c:\workspace\ai progect\
└── yuncode-lowcode/                 # 🎯 主项目目录（统一管理）
    ├── yuncode-lowcode-boot/         # 🔧 后端项目
    │   ├── src/main/java/...         # Spring Boot 应用
    │   └── pom.xml                  # Maven 配置
    │
    ├── yuncode-lowcode-admin/        # 📦 原前端项目（保留作为参考）
    │   ├── src/
    │   │   ├── api/                  # 原有 API（已迁移到新项目）
    │   │   ├── stores/               # 原有状态管理（已迁移）
    │   │   ├── composables/          # 原有组合式函数（已迁移）
    │   │   └── views/                # 原有页面组件
    │   └── package.json
    │
    ├── yuncode-pure-admin/           # 🆕 新前端项目（基于 Pure Admin Thin）
    │   ├── src/
    │   │   ├── api/                  # ✅ API 接口（已迁移）
    │   │   ├── store/modules/        # ✅ 状态管理（已迁移）
    │   │   ├── router/               # ✅ 路由配置（已整合）
    │   │   │   └── modules/          # ✅ yuncode.ts 业务路由模块
    │   │   ├── composables/          # ✅ 组合式函数（已迁移）
    │   │   ├── config/               # ✅ 配置文件（已更新）
    │   │   ├── layout/               # ✅ Pure Admin 完整布局
    │   │   └── views/                # Pure Admin 原有页面
    │   ├── .env.development         # ✅ 开发环境配置
    │   ├── .env.production          # ✅ 生产环境配置
    │   └── vite.config.ts           # ✅ Vite 配置（含代理）
    │
    ├── .skills/                     # 🎯 AI Skills 目录
    │   └── 平台界面样式/             # 平台需求相关技能
    │       └── SKILL.md
    │
    ├── docker/                      # 🐳 Docker 配置
    ├── docs/                        # 📚 项目文档
    ├── skywalking-agent/            # 📊 SkyWalking 监控
    │
    ├── .git/                        # Git 版本控制
    ├── .gitignore
    ├── .vscode/                     # VS Code 配置
    │
    └── [其他文档文件]                # 技术文档、测试文件等
```

## 🎯 项目定位

### 1. yuncode-pure-admin（新前端 - 主开发）✅
**路径**: `yuncode-lowcode/yuncode-pure-admin/`
**用途**: 新的前端主项目，基于 Pure Admin Thin
**状态**: ✅ 核心迁移已完成，可正常使用

**功能**:
- ✅ 多用户登录系统（admin/user/tenant）
- ✅ API 接口完整对接
- ✅ 路由守卫逻辑整合
- ✅ SSE 踢出通知就绪
- ✅ Pure Admin 完整布局和组件

**开发服务器**: http://localhost:8848

### 2. yuncode-lowcode-boot（后端）
**路径**: `yuncode-lowcode/yuncode-lowcode-boot/`
**用途**: Spring Boot 后端服务
**端口**: http://localhost:8080

### 3. yuncode-lowcode-admin（原前端 - 参考）
**路径**: `yuncode-lowcode/yuncode-lowcode-admin/`
**用途**: 保留完整的原项目代码，作为参考

**作用**:
- 📖 参考原有的实现细节
- 🔍 查看多用户登录逻辑
- 📝 对比迁移前后的代码
- 🧪 测试原有功能

### 4. .skills（AI Skills）
**路径**: `yuncode-lowcode/.skills/`
**用途**: AI 辅助开发的自定义功能

## 🚀 开发工作流

### 项目结构优势

**统一管理** - 所有项目在一个主目录下：
```
yuncode-lowcode/
├── yuncode-lowcode-boot/      # 后端
├── yuncode-lowcode-admin/     # 原前端（参考）
├── yuncode-pure-admin/        # 新前端（主开发）
├── .skills/                   # AI Skills
├── docs/                      # 文档
└── docker/                    # Docker 配置
```

### 日常开发

1. **启动后端**
   ```bash
   cd yuncode-lowcode/yuncode-lowcode-boot
   # 启动 Spring Boot 应用
   ```

2. **启动前端**
   ```bash
   cd yuncode-lowcode/yuncode-pure-admin
   pnpm dev
   ```
   访问: http://localhost:8848

3. **参考原实现**
   ```bash
   # 查看原项目的实现
   cat yuncode-lowcode/yuncode-lowcode-admin/src/api/auth.ts
   ```

### 项目访问

- **前端**: http://localhost:8848/
- **后端**: http://localhost:8080/
- **API 代理**: `/api` -> `http://localhost:8080`

## 📊 迁移状态对比

| 功能模块 | 原项目位置 | 新项目位置 | 迁移状态 |
|---------|-----------|-----------|---------|
| API 接口 | `yuncode-lowcode-admin/src/api/` | `yuncode-pure-admin/src/api/` | ✅ 已完成 |
| 状态管理 | `yuncode-lowcode-admin/src/stores/` | `yuncode-pure-admin/src/store/modules/` | ✅ 已完成 |
| 路由守卫 | `yuncode-lowcode-admin/src/router/` | `yuncode-pure-admin/src/router/` | ✅ 已整合 |
| Composables | `yuncode-lowcode-admin/src/composables/` | `yuncode-pure-admin/src/composables/` | ✅ 已完成 |
| 业务页面 | `yuncode-lowcode-admin/src/views/` | `yuncode-pure-admin/src/views/` | ⏳ 按需迁移 |
| 配置文件 | `yuncode-lowcode-admin/.env.*` | `yuncode-pure-admin/.env.*` | ✅ 已完成 |

## 💡 开发建议

### 快速导航

```bash
# 进入主项目目录
cd yuncode-lowcode

# 进入后端项目
cd yuncode-lowcode/yuncode-lowcode-boot

# 进入新前端项目（主开发）
cd yuncode-lowcode/yuncode-pure-admin

# 进入原前端项目（参考）
cd yuncode-lowcode/yuncode-lowcode-admin

# 查看 AI Skills
cd yuncode-lowcode/.skills
```

### 查看原实现

当需要了解某个功能的原始实现时：
```bash
# 查看原 API 实现
cat yuncode-lowcode/yuncode-lowcode-admin/src/api/auth.ts

# 查看原状态管理
cat yuncode-lowcode/yuncode-lowcode-admin/src/stores/user.ts

# 对比差异
diff yuncode-lowcode/yuncode-lowcode-admin/src/api/auth.ts \
     yuncode-lowcode/yuncode-pure-admin/src/api/auth.ts
```

## 🎯 下一步

1. ✅ **项目结构已优化** - 统一在 yuncode-lowcode 目录下管理
2. ✅ **核心迁移已完成** - 可开始功能测试
3. ⏳ **按需迁移页面** - 根据实际需要迁移自定义页面
4. ⏳ **功能测试** - 测试登录、SSE 通知等功能

---

**最后更新**: 2026-01-20
**迁移完成度**: 核心功能 100%，自定义页面按需迁移
**项目状态**: ✅ 已统一到 yuncode-lowcode 目录下
