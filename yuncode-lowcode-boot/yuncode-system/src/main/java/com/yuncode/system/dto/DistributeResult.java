package com.yuncode.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 应用分发结果
 *
 * @author Yuncode
 * @since 2025-06-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributeResult {

    /** 应用ID */
    private Long id;

    /** 应用标识 */
    private String appId;

    /** 应用名称 */
    private String appName;

    /** 旧版本号 */
    private String oldVersion;

    /** 新版本号 */
    private String newVersion;

    /** 文件名（xxx.sap） */
    private String fileName;

    /** 文件大小（字节） */
    private long fileSize;
}
