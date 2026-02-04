package com.yuncode.system.vo;

import lombok.Data;

import java.util.List;

/**
 * 角色树节点VO
 */
@Data
public class RoleNodeVO {

    /**
     * 角色ID
     */
    private Long id;

    /**
     * 父级ID
     */
    private Long parentId;

    /**
     * 角色名称
     */
    private String label;

    /**
     * 角色名称（保留原始字段名，便于前端使用）
     */
    private String roleName;

    /**
     * 角色类型（1-分类，2-具体角色）
     */
    private Integer roleType;

    /**
     * 角色编码
     */
    private String roleCode;

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
     * 子节点
     */
    private List<RoleNodeVO> children;
}
