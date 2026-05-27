package com.yuncode.system.service.impl;

import cn.hutool.core.io.FileUtil;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.yuncode.system.dto.BoTableXmlModel;
import com.yuncode.system.entity.SysBoField;
import com.yuncode.system.entity.SysBoTable;
import com.yuncode.system.mapper.SysBoFieldMapper;
import com.yuncode.system.mapper.SysBoTableMapper;
import com.yuncode.system.service.ApplicationDirectoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * BO 元数据 XML 导入/导出服务
 * 负责将 BO 元数据导出为 XML 文件，以及从 XML 文件导入恢复
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BoTableXmlService {

    private final SysBoTableMapper boTableMapper;
    private final SysBoFieldMapper boFieldMapper;
    private final ApplicationDirectoryService applicationDirectoryService;

    private static final String BO_REPO_PATH = "repository" + File.separator + "bo";

    private final XmlMapper xmlMapper = createXmlMapper();

    private static XmlMapper createXmlMapper() {
        XmlMapper mapper = new XmlMapper();
        mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        return mapper;
    }

    /**
     * 计算 XML 内容的 SHA-256 签名
     */
    public String computeSignature(String xmlContent) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(xmlContent.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * 校验 XML 内容的签名
     */
    public boolean verifySignature(String xmlContent, String signature) {
        return computeSignature(xmlContent).equals(signature);
    }

    /**
     * 导出 BO 元数据到 XML 文件
     *
     * @param tableId BO 表 ID
     * @return 生成的 XML 文件路径
     */
    public String exportToXml(Long tableId) {
        SysBoTable table = boTableMapper.selectById(tableId);
        if (table == null) {
            throw new RuntimeException("业务对象不存在: " + tableId);
        }
        List<SysBoField> fields = boFieldMapper.selectByTableId(tableId);

        // 构建 XML 模型
        BoTableXmlModel model = buildXmlModel(table, fields);

        // 序列化为 XML 字符串（不含签名）
        String xmlWithoutSignature;
        try {
            xmlWithoutSignature = xmlMapper.writeValueAsString(model);
        } catch (Exception e) {
            throw new RuntimeException("XML 序列化失败", e);
        }

        // 计算签名
        String signature = computeSignature(xmlWithoutSignature);
        model.setSignature(signature);

        // 重新序列化（含签名）
        String xmlContent;
        try {
            xmlContent = xmlMapper.writeValueAsString(model);
        } catch (Exception e) {
            throw new RuntimeException("XML 序列化失败", e);
        }

        // 更新 DB 中的 signature 和 indexes
        String indexJson = serializeIndexes(table, fields);
        table.setIndexes(indexJson);
        table.setSignature(signature);
        boTableMapper.updateById(table);

        // 写入文件（使用 storageName 命名目录和文件）
        String filePath = resolveXmlFilePath(table.getAppId(), table.getStorageName());
        FileUtil.mkParentDirs(filePath);
        FileUtil.writeUtf8String(xmlContent, filePath);

        log.info("BO XML exported: tableId={}, storageName={}, path={}", tableId, table.getStorageName(), filePath);
        return filePath;
    }

    /**
     * 从 XML 文件导入 BO 元数据（覆盖 DB 中现有数据）
     *
     * @param tableId BO 表 ID
     * @return 导入的 XML 模型
     */
    public BoTableXmlModel importFromXml(Long tableId) {
        SysBoTable table = boTableMapper.selectById(tableId);
        if (table == null) {
            throw new RuntimeException("业务对象不存在: " + tableId);
        }

        String filePath = resolveXmlFilePath(table.getAppId(), table.getStorageName());
        File xmlFile = new File(filePath);
        if (!xmlFile.exists()) {
            throw new RuntimeException("XML 文件不存在: " + filePath);
        }

        String xmlContent = FileUtil.readUtf8String(xmlFile);

        // 反序列化
        BoTableXmlModel model;
        try {
            model = xmlMapper.readValue(xmlContent, BoTableXmlModel.class);
        } catch (Exception e) {
            throw new RuntimeException("XML 反序列化失败", e);
        }

        // 校验签名
        if (model.getSignature() != null && !model.getSignature().isEmpty()) {
            String storedSig = model.getSignature();
            model.setSignature(null);
            String contentWithoutSig;
            try {
                contentWithoutSig = xmlMapper.writeValueAsString(model);
            } catch (Exception e) {
                throw new RuntimeException("XML 序列化失败", e);
            }
            if (!verifySignature(contentWithoutSig, storedSig)) {
                throw new RuntimeException("XML 签名校验失败，文件可能已被篡改");
            }
            model.setSignature(storedSig);
        }

        return model;
    }

    /**
     * 获取 XML 文件的存储路径
     * <p>使用 storageName 作为目录名和文件名，确保可读性和跨数据库兼容性</p>
     */
    public String resolveXmlFilePath(String appId, String storageName) {
        String appDir = applicationDirectoryService.getApplicationDirectory(appId);
        if (appDir == null) {
            throw new RuntimeException("应用目录不存在: " + appId);
        }
        return appDir + File.separator + BO_REPO_PATH
                + File.separator + storageName + File.separator + storageName + ".xml";
    }

    /**
     * 检查 XML 文件是否存在
     */
    public boolean xmlExists(String appId, Long tableId) {
        SysBoTable table = boTableMapper.selectById(tableId);
        if (table == null) return false;
        return new File(resolveXmlFilePath(appId, table.getStorageName())).exists();
    }

    // ========== 内部辅助方法 ==========

    private BoTableXmlModel buildXmlModel(SysBoTable table, List<SysBoField> fields) {
        BoTableXmlModel model = new BoTableXmlModel();
        model.setId(String.valueOf(table.getId()));
        model.setTitleName(table.getTitleName());
        model.setStorageName(table.getStorageName());
        model.setStorageType(table.getStorageType());
        model.setCategoryName(table.getCategoryName());
        model.setDesignVersion(table.getDesignVersion() != null ? table.getDesignVersion() : 1);

        // 字段列表
        for (SysBoField f : fields) {
            BoTableXmlModel.BoItemXml item = new BoTableXmlModel.BoItemXml();
            item.setId(String.valueOf(f.getId()));
            item.setName(f.getFieldName());
            item.setTitle(f.getFieldTitle());
            item.setColumnType("bigint".equals(f.getFieldType()) && f.getFieldName().equals("ID")
                    ? "bigint" : f.getFieldType());
            item.setLength(f.getFieldLength() != null && f.getFieldLength() > 0 ? f.getFieldLength() : null);
            item.setNullable(f.getRequired() == null || f.getRequired() == 0);
            item.setDefaultValue(f.getDefaultValue() != null ? f.getDefaultValue() : "");
            item.setColumnWidth(f.getColumnWidth());
            item.setComponentId(mapComponentToId(f.getComponent()));
            item.setComponentSetting(f.getComponentSetting());
            item.setDisplay(f.getVisible() == null || f.getVisible() == 1);
            item.setModify(f.getReadonly() == null || f.getReadonly() == 0);
            item.setCopy(f.getCopyable() != null && f.getCopyable() == 1);
            item.setSort(f.getSort());
            model.getBoItems().add(item);
        }

        // 索引
        if (table.getIndexes() != null && !table.getIndexes().isEmpty()) {
            parseIndexesFromJson(table.getIndexes(), model);
        } else {
            // 默认 ID 索引
            BoTableXmlModel.BoIndexXml defaultIdx = new BoTableXmlModel.BoIndexXml();
            defaultIdx.setId(UUID.randomUUID().toString());
            defaultIdx.setName("idx_id");
            defaultIdx.setType("UNIQUE");
            defaultIdx.setBoItems("ID");
            model.getBoIndexs().add(defaultIdx);
        }

        return model;
    }

    private String serializeIndexes(SysBoTable table, List<SysBoField> fields) {
        // 使用 JSON 存储索引，直接使用已有的 indexes 字段或生成默认
        if (table.getIndexes() != null && !table.getIndexes().isEmpty()) {
            return table.getIndexes();
        }
        return "[{\"id\":\"" + UUID.randomUUID() + "\",\"name\":\"idx_id\",\"type\":\"UNIQUE\",\"boItems\":\"ID\",\"comment\":\"\"}]";
    }

    @SuppressWarnings("unchecked")
    private void parseIndexesFromJson(String json, BoTableXmlModel model) {
        try {
            // 简单解析 JSON 数组 — 用 Jackson 的 ObjectMapper
            com.fasterxml.jackson.databind.ObjectMapper jsonMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<java.util.Map<String, Object>> list = jsonMapper.readValue(json, List.class);
            for (java.util.Map<String, Object> entry : list) {
                BoTableXmlModel.BoIndexXml idx = new BoTableXmlModel.BoIndexXml();
                idx.setId((String) entry.getOrDefault("id", UUID.randomUUID().toString()));
                idx.setName((String) entry.getOrDefault("name", ""));
                idx.setType((String) entry.getOrDefault("type", "INDEX"));
                idx.setBoItems((String) entry.getOrDefault("boItems", ""));
                idx.setComment((String) entry.getOrDefault("comment", ""));
                model.getBoIndexs().add(idx);
            }
        } catch (Exception e) {
            log.warn("解析索引 JSON 失败，使用默认索引: {}", e.getMessage());
            BoTableXmlModel.BoIndexXml defaultIdx = new BoTableXmlModel.BoIndexXml();
            defaultIdx.setId(UUID.randomUUID().toString());
            defaultIdx.setName("idx_id");
            defaultIdx.setType("UNIQUE");
            defaultIdx.setBoItems("ID");
            model.getBoIndexs().add(defaultIdx);
        }
    }

    private String mapComponentToId(String component) {
        if (component == null) return "AWSUI.Text";
        return switch (component) {
            case "单行文本" -> "AWSUI.Text";
            case "下拉选择" -> "AWSUI.Dropdown";
            case "隐藏" -> "AWSUI.Hidden";
            case "数字输入" -> "AWSUI.Number";
            case "滑块" -> "AWSUI.Slider";
            case "日期选择" -> "AWSUI.DatePicker";
            case "日期时间" -> "AWSUI.DateTimePicker";
            case "多行文本" -> "AWSUI.TextArea";
            case "富文本" -> "AWSUI.RichText";
            default -> "AWSUI.Text";
        };
    }
}
