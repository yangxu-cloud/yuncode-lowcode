package com.yuncode.system.vo;

import lombok.Data;

import java.util.List;

/**
 * 角色详情VO
 */
@Data
public class RoleDetailVO {

    /**
     * 角色ID
     */
    private Long id;

    /**
     * 父级ID
     */
    private Long parentId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 角色类型（1-分类，2-具体角色）
     */
    private Integer roleType;

    /**
     * 描述
     */
    private String description;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 用户列表
     */
    private List<RoleUserVO> users;

    /**
     * 部门列表
     */
    private List<RoleDeptVO> depts;

    /**
     * 权限列表
     */
    private List<RolePermissionVO> permissions;

    /**
     * 角色用户VO
     */
    @Data
    public static class RoleUserVO {
        private Long userId;
        private String userName;
        private String realName;
    }

    /**
     * 角色部门VO
     */
    @Data
    public static class RoleDeptVO {
        private Long deptId;
        private String deptName;
    }

    /**
     * 角色权限VO
     */
    @Data
    public static class RolePermissionVO {
        private Long permissionId;
        private String permissionName;
        private String permissionCode;
    }
}
