package com.yuncode.system.adapter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据库列定义（适配器层通用模型，与数据库类型无关）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnDef {
    private String fieldName;
    private String fieldType;      // 逻辑类型: varchar / int / bigint / decimal / datetime / text
    private Integer fieldLength;
    private Integer decimalDigits; // decimal 小数位数，默认 2
    private boolean primaryKey;
    private boolean notNull;
    private String defaultValue;
    private String comment;
}
