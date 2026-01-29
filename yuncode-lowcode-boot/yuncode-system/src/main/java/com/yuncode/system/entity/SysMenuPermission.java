package com.yuncode.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 菜单权限关联实体类
 *
 * @author Yuncode
 * @since 2025-01-30
 */
@Data
@TableName("sys_menu_permission")
public class SysMenuPermission implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关联ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 菜单ID
     */
    private Long menuId;

    /**
     * 目标类型:0=角色,1=用户,2=部门
     */
    private Integer targetType;

    /**
     * 目标ID(角色ID/用户ID/部门ID)
     */
    private Long targetId;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 删除标记:0=未删除,1=已删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
