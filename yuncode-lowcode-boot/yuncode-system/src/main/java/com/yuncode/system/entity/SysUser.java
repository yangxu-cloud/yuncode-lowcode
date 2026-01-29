package com.yuncode.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户实体
 */
@Data
@TableName("sys_user")
public class SysUser {

    /**
     * 用户ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /**
     * 所属公司ID
     */
    private Long companyId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    @TableField(insertStrategy = FieldStrategy.ALWAYS)
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 性别（0男 1女 2未知）
     */
    private Integer gender;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 是否主部门领导：0-否 1-是
     */
    private Integer isLeader;

    /**
     * 角色ID列表（逗号分隔）
     */
    private String roleIds;

    /**
     * 角色编码：PLATFORM_ADMIN(平台管理员)/TENANT_ADMIN(租户管理员)/NORMAL(普通用户)
     */
    private String roleCode;

    /**
     * 状态（0正常 1禁用）
     */
    private Integer status;

    /**
     * 删除标志（0正常 1删除）
     */
    @TableLogic
    private Integer deleted;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建者
     */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 更新者
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
}
