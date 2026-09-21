package com.yuncode.system.app.dto;

import lombok.Data;

/**
 * 创建业务对象 DTO
 */
@Data
public class BoTableCreateDTO {
    private String titleName;
    private String suffix;
    private String storageType;
    private Long categoryId;
}
