package com.yuncode.system.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * BO 元数据 XML 模型 — 用于 XML 导入/导出
 */
@Data
@JacksonXmlRootElement(localName = "boModel")
public class BoTableXmlModel {

    private String id;
    private String titleName;
    private String storageName;
    private String storageType;
    private String categoryName;
    private Integer schemaVersion = 1;
    private Integer designVersion = 1;
    private String signature;
    private String createUser;
    private String createTime;
    private String updateUser;
    private String updateTime;
    private Boolean deleted = false;

    @JacksonXmlElementWrapper(localName = "boItems")
    @JacksonXmlProperty(localName = "boItem")
    private List<BoItemXml> boItems = new ArrayList<>();

    @JacksonXmlElementWrapper(localName = "boIndexs")
    @JacksonXmlProperty(localName = "boIndex")
    private List<BoIndexXml> boIndexs = new ArrayList<>();

    @JacksonXmlProperty(localName = "boRelations")
    private String boRelations = "";

    @Data
    public static class BoItemXml {
        private String id;
        private String name;
        private String title;
        private String columnType;
        private Integer length;
        private Boolean nullable = true;
        private String defaultValue = "";
        private Integer columnWidth;
        private String componentId;
        private String componentSetting;
        private Boolean display = true;
        private Boolean deleted = false;
        private Boolean modify = true;
        private Boolean copy = false;
        private Integer sort = 0;
        private String persistenceType = "ENTITY";
    }

    @Data
    public static class BoIndexXml {
        private String id;
        private String name;
        private String type;
        private String boItems;
        private String comment;
    }
}
