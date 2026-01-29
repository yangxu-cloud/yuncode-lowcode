package com.yuncode.system.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户配置 DTO
 * 用于添加公司时同步创建租户
 *
 * @author Yuncode
 * @since 2025-01-28
 */
@Data
public class TenantConfigDTO {

    /**
     * 租户类型：0试用 1标准 2高级 3企业
     */
    private Integer tenantType = 1;

    /**
     * 用户数量限制
     */
    private Integer userLimit = 100;

    /**
     * 存储空间限制（MB）
     */
    private Integer storageLimit = 10240; // 默认 10GB

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 联系人
     */
    private String contactName;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 联系邮箱
     */
    private String contactEmail;

    /**
     * 企业地址
     */
    private String address;

    /**
     * 备注
     */
    private String remark;
}
