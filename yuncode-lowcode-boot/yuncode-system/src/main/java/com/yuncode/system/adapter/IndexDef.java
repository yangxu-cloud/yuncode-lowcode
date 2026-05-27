package com.yuncode.system.adapter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据库索引定义（适配器层通用模型）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexDef {
    private String indexName;
    private String indexType;   // INDEX / UNIQUE
    private String columnName;  // 单字段索引
    private String comment;
}
