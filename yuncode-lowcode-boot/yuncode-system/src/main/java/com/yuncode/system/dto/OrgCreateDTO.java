package com.yuncode.system.dto;

import com.yuncode.system.entity.SysOrg;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 创建组织 DTO
 * 包含组织信息和租户配置（当 orgType=1 时使用）
 *
 * @author Yuncode
 * @since 2025-01-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "创建组织请求")
public class OrgCreateDTO extends SysOrg {

    /**
     * 租户配置（当 orgType=1，即创建公司时使用）
     */
    private TenantConfigDTO tenantConfig;
}
