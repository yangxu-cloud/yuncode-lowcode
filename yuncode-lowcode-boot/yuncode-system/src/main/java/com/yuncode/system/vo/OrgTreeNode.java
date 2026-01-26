package com.yuncode.system.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 组织树节点
 *
 * @author Yuncode
 * @since 2025-01-22
 */
@Data
public class OrgTreeNode implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 节点ID
     */
    private Long id;

    /**
     * 节点类型：org=组织，user=用户
     */
    private String nodeType;

    /**
     * 组织名称
     */
    private String label;

    /**
     * 组织编码
     */
    private String orgCode;

    /**
     * 父节点ID
     */
    private Long parentId;

    /**
     * 组织类型：1=集团/公司，2=部门
     */
    private Integer orgType;

    /**
     * 是否公司：0=否，1=是
     */
    private Integer isCompany;

    /**
     * 用户ID（仅用户节点）
     */
    private Long userId;

    /**
     * 用户名（仅用户节点）
     */
    private String username;

    /**
     * 用户昵称（仅用户节点）
     */
    private String nickname;

    /**
     * 用户头像（仅用户节点）
     */
    private String avatar;

    /**
     * 是否负责人：0=否，1=是（仅用户节点）
     */
    private Integer isLeader;

    /**
     * 是否主部门：0=否，1=是（仅用户节点，与sys_user.dept_id对应）
     */
    private Integer isMainDept;

    /**
     * 子节点列表
     */
    private List<OrgTreeNode> children;

    /**
     * 排序号
     */
    private Integer sortOrder;
}
