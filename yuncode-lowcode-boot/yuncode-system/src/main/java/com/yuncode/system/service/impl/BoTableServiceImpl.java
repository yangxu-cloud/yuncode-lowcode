package com.yuncode.system.service.impl;

import cn.hutool.core.io.FileUtil;
import com.yuncode.common.utils.SecurityUtil;
import com.yuncode.system.adapter.ColumnDef;
import com.yuncode.system.adapter.DatabaseAdapter;
import com.yuncode.system.adapter.DatabaseAdapterFactory;
import com.yuncode.system.adapter.IndexDef;
import com.yuncode.system.entity.SysAppCategory;
import com.yuncode.system.entity.SysBoField;
import com.yuncode.system.entity.SysBoTable;
import com.yuncode.system.mapper.SysAppCategoryMapper;
import com.yuncode.system.mapper.SysBoFieldMapper;
import com.yuncode.system.mapper.SysBoTableMapper;
import com.yuncode.system.service.ApplicationDirectoryService;
import com.yuncode.system.service.BoTableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoTableServiceImpl implements BoTableService {

    private final SysBoTableMapper boTableMapper;
    private final SysBoFieldMapper boFieldMapper;
    private final SysAppCategoryMapper categoryMapper;
    private final ApplicationDirectoryService applicationDirectoryService;
    private final BoTableXmlService boTableXmlService;
    private final JdbcTemplate jdbcTemplate;
    private final DatabaseAdapterFactory databaseAdapterFactory;

    /** 系统默认 15 个字段名（不显示在设计器用户字段列表中） */
    private static final Set<String> SYSTEM_FIELD_NAMES = Set.of(
            // 工作流字段
            "ID", "PROCESSINSTID", "ORGID", "CREATEDATE", "CREATEUSER",
            "UPDATEDATE", "UPDATEUSER", "PROCESSDEFID", "ISEND",
            "TASKINST_HANDLEUSER", "TASKINST_NODENAME",
            // 审计字段
            "DELETE_BY", "DELETE_FLAG", "DELETE_TIME", "TENANT_ID"
    );

    /** 15 个默认字段定义（创建 BO 时自动写入） */
    private static final List<Map<String, Object>> DEFAULT_BO_FIELDS = List.of(
            // 工作流字段
            fieldDef("ID", "主键ID", "varchar", 64, "单行文本", "", 0, 0, 1, 0),
            fieldDef("PROCESSINSTID", "流程实例ID", "varchar", 64, "单行文本", "", 0, 0, 1, 0),
            fieldDef("ORGID", "组织ID", "varchar", 64, "单行文本", "", 0, 0, 1, 0),
            fieldDef("CREATEDATE", "创建日期", "datetime", 0, "日期时间", "", 0, 0, 1, 0),
            fieldDef("CREATEUSER", "创建用户", "varchar", 64, "单行文本", "", 0, 0, 1, 0),
            fieldDef("UPDATEDATE", "更新日期", "datetime", 0, "日期时间", "", 0, 0, 1, 0),
            fieldDef("UPDATEUSER", "更新用户", "varchar", 64, "单行文本", "", 0, 0, 1, 0),
            fieldDef("PROCESSDEFID", "流程定义ID", "varchar", 64, "单行文本", "", 0, 0, 1, 0),
            fieldDef("ISEND", "是否结束", "varchar", 10, "单行文本", "N", 0, 0, 1, 0),
            fieldDef("TASKINST_HANDLEUSER", "处理人", "varchar", 500, "多行文本", "", 0, 0, 1, 0),
            fieldDef("TASKINST_NODENAME", "节点名称", "varchar", 200, "单行文本", "", 0, 0, 1, 0),
            // 审计字段
            fieldDef("DELETE_BY", "删除人", "varchar", 64, "单行文本", "", 0, 0, 1, 0),
            fieldDef("DELETE_FLAG", "删除标记", "int", 1, "隐藏", "0", 0, 0, 1, 0),
            fieldDef("DELETE_TIME", "删除时间", "datetime", 0, "日期时间", "", 0, 0, 1, 0),
            fieldDef("TENANT_ID", "租户ID", "bigint", 0, "隐藏", "", 0, 0, 1, 0)
    );

    private static Map<String, Object> fieldDef(String name, String title, String type, int length,
                                                 String component, String defaultValue,
                                                 int required, int visible, int readonly, int copyable) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("fieldName", name);
        m.put("fieldTitle", title);
        m.put("fieldType", type);
        m.put("fieldLength", length);
        m.put("component", component);
        m.put("defaultValue", defaultValue);
        m.put("required", required);
        m.put("visible", visible);
        m.put("readonly", readonly);
        m.put("copyable", copyable);
        return m;
    }

    @Override
    @Transactional
    public SysBoTable createBoTable(String appId, String titleName, String suffix, String storageType, Long categoryId) {
        Long tenantId = SecurityUtil.getTenantId();

        // 1. 计算存储名称
        String prefix = switch (storageType != null ? storageType : "Table") {
            case "View" -> "VO_CU_";
            case "Structure" -> "SO_CU_";
            default -> "BO_CU_";
        };
        String storageName = prefix + suffix;

        // 校验存储名称唯一性
        long existsCount = boTableMapper.selectCountByStorageName(storageName);
        if (existsCount > 0) {
            throw new RuntimeException("存储名称已存在: " + storageName);
        }

        // 2. 生成业务编码
        String bizCode = suffix.toUpperCase();

        // 3. 查询分类名称
        String categoryName = null;
        if (categoryId != null) {
            SysAppCategory cat = categoryMapper.selectById(categoryId);
            if (cat != null) {
                categoryName = cat.getName();
            }
        }

        // 4. 创建 BO 表元数据
        SysBoTable table = new SysBoTable();
        table.setAppId(appId);
        table.setCategoryId(categoryId);
        table.setCategoryName(categoryName);
        table.setTitleName(titleName);
        table.setStorageName(storageName);
        table.setStorageType(storageType);
        table.setBizCode(bizCode);
        table.setDesignVersion(1);
        table.setIndexes("[{\"id\":\"" + UUID.randomUUID() + "\",\"name\":\"idx_id\",\"type\":\"UNIQUE\",\"boItems\":\"ID\",\"comment\":\"\"}]");
        table.setTenantId(tenantId);
        boTableMapper.insert(table);

        // 5. 插入 11 个默认字段
        int sort = 0;
        for (Map<String, Object> def : DEFAULT_BO_FIELDS) {
            SysBoField field = new SysBoField();
            field.setTableId(table.getId());
            field.setFieldName((String) def.get("fieldName"));
            field.setFieldTitle((String) def.get("fieldTitle"));
            field.setFieldType((String) def.get("fieldType"));
            field.setFieldLength((Integer) def.get("fieldLength"));
            field.setComponent((String) def.get("component"));
            field.setDefaultValue((String) def.get("defaultValue"));
            field.setRequired((Integer) def.get("required"));
            field.setVisible((Integer) def.get("visible"));
            field.setReadonly((Integer) def.get("readonly"));
            field.setCopyable((Integer) def.get("copyable"));
            field.setSort(sort++);
            field.setTenantId(tenantId);
            boFieldMapper.insert(field);
        }

        // 6. 导出 XML
        try {
            boTableXmlService.exportToXml(table.getId());
        } catch (Exception e) {
            log.warn("XML 导出失败，但不影响创建: {}", e.getMessage());
        }

        // 7. 创建物理数据库表
        try {
            List<SysBoField> allFields = boFieldMapper.selectByTableId(table.getId());
            String createSql = buildCreateTableSql(table, allFields);
            jdbcTemplate.execute(createSql);
            log.info("物理表创建成功: {}", storageName);
        } catch (Exception e) {
            log.warn("物理表创建失败（可能已存在或权限不足）: {}", e.getMessage());
        }

        log.info("业务对象创建成功: appId={}, titleName={}, storageName={}, tableId={}",
                appId, titleName, storageName, table.getId());
        return table;
    }

    @Override
    public List<SysBoTable> getBoTableList(String appId, Long categoryId) {
        if (categoryId != null) {
            return boTableMapper.selectByCategory(appId, categoryId);
        }
        return boTableMapper.selectByAppId(appId);
    }

    @Override
    public Map<String, Object> getBoTableDetail(Long id) {
        SysBoTable table = boTableMapper.selectById(id);
        if (table == null) {
            throw new RuntimeException("业务对象不存在");
        }
        List<SysBoField> fields = boFieldMapper.selectByTableId(id);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("table", table);
        result.put("fields", fields);
        return result;
    }

    @Override
    @Transactional
    public void deleteBoTable(Long id) {
        SysBoTable table = boTableMapper.selectById(id);
        if (table == null) {
            throw new RuntimeException("业务对象不存在");
        }

        // 1. 删除物理数据库表
        try {
            String dropSql = getAdapter().buildDropTableSql(table.getStorageName());
            jdbcTemplate.execute(dropSql);
            log.info("物理表已删除: {}", table.getStorageName());
        } catch (Exception e) {
            log.warn("物理表删除失败（可能不存在），继续清理: {}", e.getMessage());
        }

        // 2. 删除 XML 文件及备份
        try {
            String xmlPath = boTableXmlService.resolveXmlFilePath(table.getAppId(), table.getStorageName());
            File xmlDir = new File(xmlPath).getParentFile();
            if (xmlDir.exists()) {
                FileUtil.del(xmlDir);
                log.info("XML 目录已删除: {}", xmlDir.getAbsolutePath());
            }
        } catch (Exception e) {
            log.warn("XML 目录删除失败: {}", e.getMessage());
        }

        // 3. 删除字段元数据
        List<SysBoField> fields = boFieldMapper.selectByTableId(id);
        for (SysBoField field : fields) {
            boFieldMapper.deleteById(field.getId());
        }

        // 4. 删除表元数据
        boTableMapper.deleteById(id);

        log.info("业务对象已完全删除: tableId={}, storageName={}", id, table.getStorageName());
    }

    @Override
    @Transactional
    public void updateBoTable(Long id, String titleName, String storageType) {
        SysBoTable table = boTableMapper.selectById(id);
        if (table == null) {
            throw new RuntimeException("业务对象不存在");
        }
        if (titleName != null) table.setTitleName(titleName);
        if (storageType != null) table.setStorageType(storageType);
        boTableMapper.updateById(table);
    }

    @Override
    @Transactional
    public void batchSaveFields(Long tableId, List<SysBoField> fields) {
        SysBoTable table = boTableMapper.selectById(tableId);
        if (table == null) {
            throw new RuntimeException("业务对象不存在");
        }
        Long tenantId = SecurityUtil.getTenantId();

        // 删除旧字段（物理删除）
        boFieldMapper.deleteByTableId(tableId);

        // 插入新字段
        int sort = 0;
        for (SysBoField field : fields) {
            field.setId(null);
            field.setTableId(tableId);
            field.setSort(sort++);
            field.setTenantId(tenantId);
            boFieldMapper.insert(field);
        }
    }

    @Override
    public String generateDdlFile(Long tableId) {
        Map<String, Object> detail = getBoTableDetail(tableId);
        SysBoTable table = (SysBoTable) detail.get("table");
        @SuppressWarnings("unchecked")
        List<SysBoField> fields = (List<SysBoField>) detail.get("fields");

        // 使用适配器构建 CREATE TABLE SQL
        String ddl = buildCreateTableSql(table, fields);

        String appDir = applicationDirectoryService.getApplicationDirectory(table.getAppId());
        if (appDir == null) {
            throw new RuntimeException("应用目录不存在: " + table.getAppId());
        }
        String dirPath = appDir + File.separator + "repository" + File.separator + "tables"
                + File.separator + table.getStorageName();
        FileUtil.mkdir(dirPath);
        File ddlFile = FileUtil.file(dirPath, table.getStorageName() + ".sql");
        FileUtil.writeUtf8String(ddl, ddlFile);

        log.info("DDL 文件生成成功: {}", ddlFile.getAbsolutePath());
        return ddlFile.getAbsolutePath();
    }

    // ============ 新增：保存完整设计 ============

    @Override
    @Transactional
    public void saveBoDesign(Long tableId, List<SysBoField> fields, String indexes) {
        SysBoTable table = boTableMapper.selectById(tableId);
        if (table == null) {
            throw new RuntimeException("业务对象不存在");
        }
        Long tenantId = SecurityUtil.getTenantId();

        // 1. 保存 indexes
        table.setIndexes(indexes);
        table.setDesignVersion(table.getDesignVersion() != null ? table.getDesignVersion() + 1 : 1);

        // 2. 保存字段
        boFieldMapper.deleteByTableId(tableId);
        int sort = 0;
        for (SysBoField field : fields) {
            field.setId(null);
            field.setTableId(tableId);
            field.setSort(sort++);
            field.setTenantId(tenantId);
            boFieldMapper.insert(field);
        }

        // 3. 更新表元数据
        boTableMapper.updateById(table);

        // 4. 导出 XML
        try {
            boTableXmlService.exportToXml(tableId);
        } catch (Exception e) {
            log.warn("XML 导出失败: {}", e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getBoTableDetailWithIndexes(Long id) {
        SysBoTable table = boTableMapper.selectById(id);
        if (table == null) {
            throw new RuntimeException("业务对象不存在");
        }
        List<SysBoField> fields = boFieldMapper.selectByTableId(id);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("table", table);
        result.put("fields", fields);
        return result;
    }

    // ============ DDL 引擎 ============

    @Override
    public Map<String, Object> generateSafeDdl(Long tableId) {
        SysBoTable table = boTableMapper.selectById(tableId);
        if (table == null) {
            throw new RuntimeException("业务对象不存在");
        }

        // 获取当前表结构（适配器层，兼容多数据库）
        List<Map<String, Object>> existingColumns;
        try {
            existingColumns = jdbcTemplate.queryForList(
                    getAdapter().getColumnInfoSql(table.getStorageName())
            );
        } catch (Exception e) {
            // 表不存在
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("tableExists", false);
            result.put("message", "数据库表不存在，需要 CREATE TABLE");
            result.put("ddlItems", List.of(Map.of(
                    "type", "CREATE",
                    "risk", "SAFE",
                    "sql", buildCreateTableSql(table, boFieldMapper.selectByTableId(tableId))
            )));
            return result;
        }

        // 表存在，构建 diff
        Set<String> existingColNames = new HashSet<>();
        Map<String, Map<String, Object>> colMeta = new LinkedHashMap<>();
        for (Map<String, Object> col : existingColumns) {
            String colName = ((String) col.get("COLUMN_NAME")).toUpperCase();
            existingColNames.add(colName);
            colMeta.put(colName, col);
        }

        List<SysBoField> designedFields = boFieldMapper.selectByTableId(tableId);
        List<Map<String, Object>> ddlItems = new ArrayList<>();
        List<Map<String, Object>> riskyItems = new ArrayList<>();

        for (SysBoField f : designedFields) {
            String name = f.getFieldName().toUpperCase();

            if (!existingColNames.contains(name)) {
                // 新增字段 — 安全
                ColumnDef colDef = toColumnDef(f);
                String alterSql = getAdapter().buildAddColumnSql(table.getStorageName(), colDef);
                ddlItems.add(Map.of("type", "ADD", "field", f.getFieldName(), "risk", "SAFE", "sql", alterSql));
            } else {
                // 字段已存在，检查类型和长度变化
                Map<String, Object> existing = colMeta.get(name);
                String existingType = (String) existing.get("DATA_TYPE");
                Object existingLen = existing.get("CHARACTER_MAXIMUM_LENGTH");
                String existingColType = (String) existing.get("COLUMN_TYPE");

                String designedBaseType = f.getFieldType() != null ? f.getFieldType().toLowerCase() : "varchar";

                // 类型变更检测
                if (!isTypeCompatible(designedBaseType, existingType)) {
                    ColumnDef colDef = toColumnDef(f);
                    riskyItems.add(Map.of(
                            "type", "MODIFY_TYPE",
                            "field", f.getFieldName(),
                            "risk", "DANGEROUS",
                            "message", "字段类型不兼容变更: " + existingType + " → " + designedBaseType,
                            "sql", getAdapter().buildModifyColumnSql(table.getStorageName(), colDef)
                    ));
                }

                // 长度变更检测
                if (designedBaseType.equals("varchar") && existingLen != null) {
                    int newLen = f.getFieldLength() != null && f.getFieldLength() > 0 ? f.getFieldLength() : 255;
                    int oldLen = Integer.parseInt(existingLen.toString());
                    if (newLen > oldLen) {
                        ColumnDef colDef = toColumnDef(f);
                        String alterSql = getAdapter().buildModifyColumnSql(table.getStorageName(), colDef);
                        ddlItems.add(Map.of("type", "MODIFY_LENGTH", "field", f.getFieldName(),
                                "risk", "SAFE", "sql", alterSql, "message", oldLen + " → " + newLen));
                    } else if (newLen < oldLen) {
                        ColumnDef colDef = toColumnDef(f);
                        riskyItems.add(Map.of(
                                "type", "SHORTEN_LENGTH",
                                "field", f.getFieldName(),
                                "risk", "WARNING",
                                "message", "缩短字段长度可能导致数据截断: " + oldLen + " → " + newLen,
                                "sql", getAdapter().buildModifyColumnSql(table.getStorageName(), colDef)
                        ));
                    }
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tableExists", true);
        result.put("tableName", table.getStorageName());
        result.put("safeChanges", ddlItems);
        result.put("riskyChanges", riskyItems);
        result.put("hasRiskyChanges", !riskyItems.isEmpty());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> executeSafeDdl(Long tableId, List<String> confirmed) {
        Map<String, Object> diff = generateSafeDdl(tableId);
        List<Map<String, Object>> executed = new ArrayList<>();
        List<Map<String, Object>> failed = new ArrayList<>();

        // 执行安全变更
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> safeChanges = (List<Map<String, Object>>) diff.getOrDefault("safeChanges", Collections.emptyList());
        for (Map<String, Object> item : safeChanges) {
            String sql = (String) item.get("sql");
            try {
                jdbcTemplate.execute(sql);
                executed.add(Map.of("sql", sql, "status", "OK"));
            } catch (Exception e) {
                failed.add(Map.of("sql", sql, "status", "FAILED", "error", e.getMessage()));
            }
        }

        // 执行已确认的危险变更
        Set<String> confirmedSet = new HashSet<>();
        if (confirmed != null) confirmedSet.addAll(confirmed);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> riskyChanges = (List<Map<String, Object>>) diff.getOrDefault("riskyChanges", Collections.emptyList());
        for (Map<String, Object> item : riskyChanges) {
            String sql = (String) item.get("sql");
            if (confirmedSet.contains(sql)) {
                try {
                    jdbcTemplate.execute(sql);
                    executed.add(Map.of("sql", sql, "status", "OK (confirmed)"));
                } catch (Exception e) {
                    failed.add(Map.of("sql", sql, "status", "FAILED", "error", e.getMessage()));
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("executed", executed);
        result.put("failed", failed);
        result.put("success", failed.isEmpty());
        return result;
    }

    // ============ 部署同步 ============

    @Override
    @Transactional
    public void deploySync(Long tableId) {
        SysBoTable table = boTableMapper.selectById(tableId);
        if (table == null) {
            throw new RuntimeException("业务对象不存在");
        }

        String appId = table.getAppId();
        String xmlPath = boTableXmlService.resolveXmlFilePath(appId, table.getStorageName());

        // 1. 备份当前 XML
        File xmlFile = new File(xmlPath);
        if (xmlFile.exists()) {
            int currentVersion = table.getDesignVersion() != null ? table.getDesignVersion() : 1;
            String backupName = table.getStorageName() + "_back_v" + currentVersion + ".xml";
            String backupPath = xmlFile.getParent() + File.separator + backupName;
            FileUtil.copy(xmlPath, backupPath, true);
            log.info("备份 XML: {} → {}", xmlPath, backupPath);
        }

        // 2. 写新 XML（重新导出当前 DB 元数据）
        boTableXmlService.exportToXml(tableId);

        // 3. 同步元数据（已经通过 exportToXml 完成）

        // 4. 执行 DDL（仅安全变更）
        Map<String, Object> ddlResult = executeSafeDdl(tableId, new ArrayList<>());
        log.info("DDL 执行完成: executed={}, failed={}",
                ((List<?>) ddlResult.getOrDefault("executed", Collections.emptyList())).size(),
                ((List<?>) ddlResult.getOrDefault("failed", Collections.emptyList())).size());

        // 5. 更新版本号
        table.setDesignVersion(table.getDesignVersion() != null ? table.getDesignVersion() + 1 : 1);
        boTableMapper.updateById(table);

        log.info("部署同步完成: tableId={}", tableId);
    }

    @Override
    @Transactional
    public void rollback(Long tableId, Integer targetVersion) {
        SysBoTable table = boTableMapper.selectById(tableId);
        if (table == null) {
            throw new RuntimeException("业务对象不存在");
        }

        String xmlPath = boTableXmlService.resolveXmlFilePath(table.getAppId(), table.getStorageName());
        String backupName = table.getStorageName() + "_back_v" + targetVersion + ".xml";
        String backupPath = new File(xmlPath).getParent() + File.separator + backupName;
        File backupFile = new File(backupPath);

        if (!backupFile.exists()) {
            throw new RuntimeException("备份文件不存在: " + backupPath);
        }

        // 1. 覆盖当前 XML
        FileUtil.copy(backupPath, xmlPath, true);
        log.info("回滚: 还原 XML 到版本 {}", targetVersion);

        // 2. 从 XML 恢复元数据到 DB
        var xmlModel = boTableXmlService.importFromXml(tableId);

        // 3. 恢复 sys_bo_table
        table.setTitleName(xmlModel.getTitleName());
        table.setStorageName(xmlModel.getStorageName());
        table.setStorageType(xmlModel.getStorageType());
        table.setDesignVersion(targetVersion);
        table.setSignature(xmlModel.getSignature());
        // 从 XML 反序列化后重建 indexes JSON
        try {
            com.fasterxml.jackson.databind.ObjectMapper jsonMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, Object>> idxList = new ArrayList<>();
            for (var idx : xmlModel.getBoIndexs()) {
                Map<String, Object> idxMap = new LinkedHashMap<>();
                idxMap.put("id", idx.getId());
                idxMap.put("name", idx.getName());
                idxMap.put("type", idx.getType());
                idxMap.put("boItems", idx.getBoItems());
                idxMap.put("comment", idx.getComment());
                idxList.add(idxMap);
            }
            table.setIndexes(jsonMapper.writeValueAsString(idxList));
        } catch (Exception e) {
            log.warn("重建 indexes JSON 失败", e);
        }
        boTableMapper.updateById(table);

        // 4. 还原 sys_bo_field
        boFieldMapper.deleteByTableId(tableId);
        Long tenantId = SecurityUtil.getTenantId();
        int sort = 0;
        for (var item : xmlModel.getBoItems()) {
            SysBoField field = new SysBoField();
            field.setTableId(tableId);
            field.setFieldName(item.getName());
            field.setFieldTitle(item.getTitle());
            field.setFieldType(mapUiTypeToDb(item.getColumnType()));
            field.setFieldLength(item.getLength());
            field.setComponent(mapComponentFromId(item.getComponentId()));
            field.setDefaultValue(item.getDefaultValue());
            field.setRequired(Boolean.FALSE.equals(item.getNullable()) ? 1 : 0);
            field.setVisible(Boolean.TRUE.equals(item.getDisplay()) ? 1 : 0);
            field.setReadonly(Boolean.FALSE.equals(item.getModify()) ? 1 : 0);
            field.setCopyable(Boolean.TRUE.equals(item.getCopy()) ? 1 : 0);
            field.setColumnWidth(item.getColumnWidth());
            field.setComponentSetting(item.getComponentSetting());
            field.setSort(sort++);
            field.setTenantId(tenantId);
            boFieldMapper.insert(field);
        }

        // 5. 执行 DDL
        executeSafeDdl(tableId, new ArrayList<>());

        log.info("回滚完成: tableId={}, targetVersion={}", tableId, targetVersion);
    }

    // ============ 内部辅助 ============

    private DatabaseAdapter getAdapter() {
        return databaseAdapterFactory.getAdapter();
    }

    private String mapFieldType(String fieldType, Integer length) {
        return getAdapter().mapToColumnType(fieldType, length);
    }

    private boolean isTypeCompatible(String designed, String existing) {
        if (designed.equals(existing)) return true;
        return switch (designed) {
            case "varchar" -> existing.equals("varchar") || existing.equals("char");
            case "int" -> existing.equals("int") || existing.equals("integer") || existing.equals("tinyint");
            case "bigint" -> existing.equals("bigint") || existing.equals("int");
            case "decimal" -> existing.equals("decimal") || existing.equals("float") || existing.equals("double");
            case "datetime" -> existing.equals("datetime") || existing.equals("timestamp");
            case "text" -> existing.equals("text") || existing.equals("longtext") || existing.equals("mediumtext");
            default -> false;
        };
    }

    private String buildCreateTableSql(SysBoTable table, List<SysBoField> fields) {
        List<ColumnDef> columns = fields.stream().map(f -> {
            boolean isId = f.getFieldName().equals("ID");
            return ColumnDef.builder()
                    .fieldName(f.getFieldName())
                    .fieldType(f.getFieldType())
                    .fieldLength(f.getFieldLength())
                    .primaryKey(isId)
                    .notNull(isId)
                    .defaultValue(f.getDefaultValue())
                    .build();
        }).toList();

        List<IndexDef> indexes = List.of(IndexDef.builder()
                .indexName("idx_id")
                .indexType("UNIQUE")
                .columnName("ID")
                .build());

        return getAdapter().buildCreateTableSql(table.getStorageName(), columns, indexes, table.getTitleName());
    }

    private String mapUiTypeToDb(String columnType) {
        if (columnType == null) return "varchar";
        String lower = columnType.toLowerCase();
        return switch (lower) {
            case "int", "integer" -> "int";
            case "bigint" -> "bigint";
            case "decimal" -> "decimal";
            case "datetime", "date", "timestamp" -> "datetime";
            case "text", "longtext" -> "text";
            default -> "varchar";
        };
    }

    /** SysBoField → ColumnDef */
    private ColumnDef toColumnDef(SysBoField f) {
        boolean isId = "ID".equalsIgnoreCase(f.getFieldName());
        return ColumnDef.builder()
                .fieldName(f.getFieldName())
                .fieldType(f.getFieldType())
                .fieldLength(f.getFieldLength())
                .primaryKey(isId)
                .notNull(isId || (f.getRequired() != null && f.getRequired() == 1))
                .defaultValue(f.getDefaultValue())
                .build();
    }

    private String mapComponentFromId(String componentId) {
        if (componentId == null) return "单行文本";
        return switch (componentId) {
            case "AWSUI.Text" -> "单行文本";
            case "AWSUI.Dropdown" -> "下拉选择";
            case "AWSUI.Hidden" -> "隐藏";
            case "AWSUI.Number" -> "数字输入";
            case "AWSUI.Slider" -> "滑块";
            case "AWSUI.DatePicker" -> "日期选择";
            case "AWSUI.DateTimePicker" -> "日期时间";
            case "AWSUI.TextArea" -> "多行文本";
            case "AWSUI.RichText" -> "富文本";
            default -> "单行文本";
        };
    }
}
