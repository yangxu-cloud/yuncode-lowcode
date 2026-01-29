package com.yuncode.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户组织关系 VO
 */
@Data
public class UserOrgVO {

    /**
     * 用户组织关系ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户真实姓名
     */
    private String realName;

    /**
     * 组织ID
     */
    private Long orgId;

    /**
     * 组织名称
     */
    private String orgName;

    /**
     * 组织编码
     */
    private String orgCode;

    /**
     * 组织类型：0=根节点，1=公司，2=部门
     */
    private Integer orgType;

    /**
     * 是否主部门：0=兼职部门，1=主部门
     */
    private Integer isMainDept;

    /**
     * 是否负责人：0=否，1=是
     */
    private Integer isLeader;

    /**
     * 组织路径（从根到当前组织的完整路径）
     */
    private String orgPath;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
