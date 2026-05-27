# 业务对象（BO）元数据管理方案

## 1. 概述

本方案定义了 Yuncode 低代码平台中业务对象（Business Object）的完整生命周期管理，包括元数据存储、DDL 生成与执行、XML 导入导出、部署同步和回滚策略。

## 2. 存储架构

### 2.1 双存储模型

- **MySQL**（`sys_bo_table` + `sys_bo_field`）— 运行时操作的数据源，支持快速增删改查、搜索和统计
- **XML 文件**（`apps/{appId}/repository/bo/{tableId}/{tableId}.xml`）— 设计的权威记录，用于版本控制、部署同步和跨环境迁移

```
设计态（运行时）                    部署态（打包/同步）
┌──────────────────┐              ┌──────────────────┐
│  sys_bo_table    │   导出XML    │  repository/bo/  │
│  sys_bo_field    │ ──────────→  │  {tableId}/      │
│  (MySQL)         │              │  {tableId}.xml   │
└──────────────────┘              └──────────────────┘
       ↑                                  │
       │ 增删改查                          │ 导入/部署
       │                                  ↓
  BODesigner UI                   ┌──────────────────┐
  (设计器)                        │  目标环境 MySQL   │
                                  └──────────────────┘
```

### 2.2 表结构

#### sys_bo_table

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT (ASSIGN_ID) | 主键，同时作为 XML 目录名和文件名 |
| appId | VARCHAR | 所属应用 |
| categoryId | BIGINT | 分类 ID |
| categoryName | VARCHAR | 分类名称 |
| titleName | VARCHAR(50) | 标题名称（用户输入） |
| storageName | VARCHAR(100) | 存储名称（前缀 + 后缀 + 类型后缀） |
| storageType | VARCHAR(10) | TABLE / VIEW / STRUCTURE |
| bizCode | VARCHAR(100) | 业务编码 |
| indexes | TEXT | 索引定义（JSON 数组） |
| signature | VARCHAR(128) | XML 文件签名，用于完整性校验 |
| designVersion | INT | 设计版本号，每次保存自增 |
| tenantId | BIGINT | 租户 ID |
| createTime | DATETIME | 创建时间 |
| updateTime | DATETIME | 更新时间 |
| deleted | TINYINT | 逻辑删除 (0/1) |

#### sys_bo_field

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT (ASSIGN_ID) | 主键 |
| tableId | BIGINT | 所属 BO 表 |
| fieldName | VARCHAR(64) | 字段名称 |
| fieldTitle | VARCHAR(100) | 字段标题 |
| fieldType | VARCHAR(20) | 数据库类型：varchar / int / bigint / decimal / datetime / text |
| fieldLength | INT | 字段长度 |
| component | VARCHAR(50) | 前端组件标识 |
| componentSetting | TEXT | 组件完整配置（JSON） |
| columnWidth | INT | 列宽 |
| defaultValue | VARCHAR(200) | 默认值 |
| required | TINYINT | 必填 (0/1) |
| visible | TINYINT | 可见 (0/1) |
| readonly | TINYINT | 只读 (0/1) |
| copyable | TINYINT | 可复制 (0/1) |
| sort | INT | 排序 |
| tenantId | BIGINT | 租户 ID |
| createTime | DATETIME | 创建时间 |
| updateTime | DATETIME | 更新时间 |
| deleted | TINYINT | 逻辑删除 (0/1) |

### 2.3 UUID / ID 策略

- 使用 MyBatis-Plus `ASSIGN_ID`（雪花算法）生成 BIGINT 主键
- **tableId 同时作为**：
  - `sys_bo_table.id` 主键
  - XML 文件目录名：`repository/bo/{tableId}/`
  - XML 文件名：`{tableId}.xml`
- 多环境部署时，tableId 在导出 XML 中保持不变，导入目标环境时**使用原始 id**，确保跨环境一致性

---

## 3. 系统字段（15 个默认字段）

创建 BO 表时自动生成，不显示在 BODesigner 用户字段列表中：

### 3.1 工作流字段

BO 表默认包含的工作流字段，用于流程引擎和主子表关联：

| 字段名 | 标题 | 类型 | 长度 | 说明 |
|--------|------|------|------|------|
| ID | 主键ID | varchar | 64 | 主键（默认唯一索引） |
| PROCESSINSTID | 流程实例ID | varchar | 64 | 流程实例标识，主子表关联键 |
| ORGID | 组织ID | varchar | 64 | 所属组织 |
| CREATEDATE | 创建日期 | datetime | — | 创建时间 |
| CREATEUSER | 创建用户 | varchar | 64 | 创建人 |
| UPDATEDATE | 更新日期 | datetime | — | 更新时间 |
| UPDATEUSER | 更新用户 | varchar | 64 | 更新人 |
| PROCESSDEFID | 流程定义ID | varchar | 64 | 流程定义标识 |
| ISEND | 是否结束 | varchar | 10 | 流程是否结束，默认 N |
| TASKINST_HANDLEUSER | 处理人 | varchar | 500 | 当前任务处理人 |
| TASKINST_NODENAME | 节点名称 | varchar | 200 | 当前流程节点名称 |

### 3.2 审计字段

额外的审计和租户隔离字段：

| 字段名 | 标题 | 类型 | 长度 | 说明 |
|--------|------|------|------|------|
| DELETE_BY | 删除人 | varchar | 64 | 逻辑删除人 |
| DELETE_FLAG | 删除标记 | int | 1 | 0=正常, 1=已删除 |
| DELETE_TIME | 删除时间 | datetime | — | 逻辑删除时间 |
| TENANT_ID | 租户ID | bigint | — | 租户隔离 |

---

## 4. DDL 安全规则

### 4.1 创建表

- CREATE TABLE 总是可执行（表不存在时创建）
- 包含所有系统字段 + 用户字段
- 包含索引定义

### 4.2 变更检测与安全矩阵

比较 XML（新设计）与 DB（当前结构）的差异：

| 变更类型 | 检测方式 | 处理策略 |
|----------|----------|----------|
| 新增字段 | 当前表无此列 | **自动执行** ALTER TABLE ADD COLUMN |
| 字段加长 | `新长度 > 旧长度` | **自动执行** ALTER TABLE MODIFY COLUMN |
| 添加索引 | 当前表无此索引 | **自动执行** CREATE INDEX |
| 删除索引 | 当前表有此索引但新 XML 无 | **提示确认**后执行 DROP INDEX |
| 字段缩短 | `新长度 < 旧长度` | **警告并阻止**，需用户确认存在数据截断风险 |
| 删字段 | XML 中字段标记为 deleted | **提示确认**：提示"该字段数据将丢失"，由开发者决定 |
| 改字段类型 | `新类型 ≠ 旧类型` | **警告并阻止**，类型不兼容变更需手动处理 |
| 改字段名 | `新名称 ≠ 旧名称` | **仅更新注释**，不允许改列名（数据迁移风险） |

### 4.3 DDL 执行流程

```
保存设计 → 生成新 DDL → 读取当前表结构
                              ↓
                    比较差异（diff）
                              ↓
              ┌───────────────┼───────────────┐
              ↓               ↓               ↓
          安全变更         需确认变更       危险变更
        (加字段/加长)    (删字段/删索引)   (改类型/缩短)
              ↓               ↓               ↓
         自动执行        弹窗确认后执行     阻止并提示
```

---

## 5. XML 格式规范

### 5.1 文件路径

```
apps/{appId}/repository/bo/{tableId}/{tableId}.xml
```

### 5.2 XML Schema

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<boModel>
    <!-- 基础标识 -->
    <id>7f37dc6f-86ef-4bfe-91fe-d923a058234b</id>
    <titleName>检验报告子表2</titleName>
    <storageName>BO_EU_QMS_CIVILENTER_CD2</storageName>
    <storageType>TABLE</storageType>
    <categoryName>首件检验（民机）</categoryName>

    <!-- 版本与完整性 -->
    <schemaVersion>1</schemaVersion>
    <designVersion>2</designVersion>
    <signature>sha256-hash</signature>

    <!-- 审计 -->
    <createUser>system1</createUser>
    <createTime>2024-04-11 16:27:28</createTime>
    <updateUser>system1</updateUser>
    <updateTime>2026-01-07 18:27:57</updateTime>
    <deleted>false</deleted>

    <!-- 字段定义 -->
    <boItems>
        <boItem>
            <id>39b1bf42-45e9-430d-a4b4-8229c6f3ff35</id>
            <name>NUM</name>
            <title>排序号</title>
            <columnType>varchar</columnType>
            <length>128</length>
            <nullable>true</nullable>
            <defaultValue></defaultValue>
            <columnWidth>100</columnWidth>
            <componentId>AWSUI.Number</componentId>
            <componentSetting><![CDATA[{"clearable":false,...}]]></componentSetting>
            <display>true</display>
            <deleted>false</deleted>
            <modify>true</modify>
            <copy>false</copy>
            <sort>1</sort>
            <tooltip></tooltip>
            <persistenceType>ENTITY</persistenceType>
        </boItem>
    </boItems>

    <!-- 索引定义 -->
    <boIndexs>
        <boIndex>
            <id>43033027-0eba-423a-9887-bf345eb13981</id>
            <name>AWS_IN_97298CCCF11E</name>
            <type>INDEX</type>
            <boItems>BINDID</boItems>
            <comment></comment>
        </boIndex>
    </boIndexs>

    <boRelations/>
</boModel>
```

### 5.3 字段类型映射（DB ↔ XML ↔ UI）

| DB 存储 (fieldType) | XML (columnType) | UI 显示 | 默认长度 |
|---------------------|------------------|---------|----------|
| varchar | varchar | 文本 | 128 |
| int | int | 数字 | 10 |
| bigint | bigint | 数字 | — |
| decimal | decimal | 数字 | — |
| datetime | datetime | 日期 | 0 |
| text | text | 大文本 | 2000 |

### 5.4 signature 计算规则

对 XML 文件去除 `<signature>` 节点后，计算 SHA-256 hash 值。部署导入时重新计算并比对，确保文件未被篡改。

---

## 6. 部署同步流程

### 6.1 同步时机

- **打包部署**：从设计环境导出 XML → 目标环境导入
- **增量同步**：比较 designVersion，仅同步有变化的 BO

### 6.2 同步步骤

```
1. 备份当前 XML
   apps/{appId}/repository/bo/{tableId}/{tableId}.xml
   → apps/{appId}/repository/bo/{tableId}/back_v{oldVersion}.xml

2. 写入新 XML
   → apps/{appId}/repository/bo/{tableId}/{tableId}.xml

3. 同步元数据到 sys_bo_table / sys_bo_field
   （INSERT ON DUPLICATE KEY UPDATE）

4. 执行 DDL（按 4.3 安全规则）
   - 比较新旧 XML 差异
   - 安全变更自动执行
   - 需确认变更等待用户确认
   - 危险变更阻止并报告

5. 验证
   - 校验 signature
   - 校验表结构是否与 XML 一致
```

### 6.3 导入策略

| 目标环境状态 | 处理方式 |
|-------------|----------|
| 表不存在 | 完整 CREATE TABLE |
| 表存在且 designVersion 相同 | 跳过 |
| 表存在且 designVersion 较旧 | 执行增量 DDL 升级 |
| 表存在但 XML 无记录 | 提示：DB 有表但无 XML 记录，询问是否反向生成 XML |

---

## 7. 回滚策略

### 7.1 回滚操作

```
1. 读取备份 XML：back_v{version}.xml
2. 覆盖当前 XML：{tableId}.xml
3. 更新 sys_bo_table / sys_bo_field（还原元数据）
4. 执行回滚 DDL（反向 diff）
5. 表结构回到 v{version} 状态
```

### 7.2 回滚的限制

| 场景 | 处理 |
|------|------|
| 新增字段回滚 | DROP COLUMN，数据丢失，**需二次确认** |
| 删除字段回滚 | 列已删除，数据无法恢复，仅恢复元数据 |
| 类型/长度变更回滚 | 可能失败（已有数据不兼容），报告并提示手动处理 |
| 仅元数据变更 | 直接还原，无 DDL |

### 7.3 备份保留策略

- 每次部署同步前自动备份
- 备份文件命名：`back_v{version}_{timestamp}.xml`
- 保留最近 10 个备份版本

---

## 8. 组件映射表（componentId）

| UI 类型 | UI 组件 | XML componentId | DB component |
|---------|---------|-----------------|--------------|
| 文本 | 单行文本 | AWSUI.Text | 单行文本 |
| 文本 | 下拉选择 | AWSUI.Dropdown | 下拉选择 |
| 文本 | 隐藏 | AWSUI.Hidden | 隐藏 |
| 数字 | 数字输入 | AWSUI.Number | 数字输入 |
| 数字 | 滑块 | AWSUI.Slider | 滑块 |
| 日期 | 日期选择 | AWSUI.DatePicker | 日期选择 |
| 日期 | 日期时间 | AWSUI.DateTimePicker | 日期时间 |
| 大文本 | 多行文本 | AWSUI.TextArea | 多行文本 |
| 大文本 | 富文本 | AWSUI.RichText | 富文本 |

---

## 9. 索引管理

### 9.1 默认索引

创建 BO 表时自动创建：
- **ID** — 唯一索引（不可删除）`UNIQUE KEY idx_id (ID)`

### 9.2 索引存储

`sys_bo_table.indexes` 字段，JSON 格式：

```json
[
  {
    "id": "uuid",
    "name": "idx_fieldname",
    "type": "INDEX",
    "boItems": "FIELD1,FIELD2",
    "comment": ""
  }
]
```

- `type`: `INDEX`（普通索引）或 `UNIQUE`（唯一索引）
- `boItems`: 逗号分隔的字段名

### 9.3 索引同步

- 保存时：`indexes` JSON → 写入 DB + 同步到 XML `<boIndexs>`
- 部署时：XML `<boIndexs>` → CREATE/DROP INDEX
- 默认 ID 唯一索引：自动生成，不可在前端删除

---

## 10. 并发策略

### 10.1 设计态

- **不做乐观锁**：同一时刻只有一个人在 BODesigner 中编辑同一个 BO
- 编辑前检查是否有其他人正在编辑（心跳机制，可选实现）

### 10.2 部署态

- **部署时写 XML**：非运行时频繁写入，部署时一次性生成/更新 XML
- 多用户部署同一应用：应用级别的部署锁（Redis 分布式锁）
- 部署队列：同一应用串行部署

---

## 11. 改造清单

### 11.1 DB 改造

| 表 | 改造内容 |
|----|----------|
| sys_bo_table | 新增 `indexes TEXT`、`signature VARCHAR(128)`、`designVersion INT DEFAULT 1` |
| sys_bo_field | 新增 `componentSetting TEXT`、`columnWidth INT DEFAULT 150` |
| sys_bo_field | `@@ 待定 @@` 是否新增 `persistenceType`、`tooltip`、`calcFormula` |

### 11.2 后端改造

| 模块 | 内容 |
|------|------|
| BoTableServiceImpl.saveBoTable() | 保存时同步写 XML 文件 |
| BoTableServiceImpl.generateDdl() | 比较新旧表结构，生成安全 DDL |
| BoTableServiceImpl.executeDdl() | 执行 DDL，附带安全校验 |
| BoTableServiceImpl.deploySync() | 部署同步完整流程（备份→写入→同步→DDL） |
| BoTableServiceImpl.rollback() | 回滚到指定版本 |
| XML 工具类 | XML 序列化/反序列化、signature 计算 |

### 11.3 前端改造

| 模块 | 内容 |
|------|------|
| BODesigner.vue | 索引管理提交到后端 |
| BODesigner.vue | 完整性校验（名称、标题、长度必填） |
| 部署管理页面（新） | 查看同步状态、diff 预览、确认危险操作 |
