package com.yuncode.system.service;

import com.yuncode.system.vo.UserOrgVO;

import java.util.List;

/**
 * 组织人员关系服务接口
 * 处理人员与组织的关联关系
 */
public interface OrgUserService {

    /**
     * 添加人员到组织
     */
    void addUserToOrg(Long orgId, Long userId, Integer isLeader, Integer isMainDept);

    /**
     * 从组织移除人员
     */
    void removeUserFromOrg(Long orgId, Long userId);

    /**
     * 设置用户为负责人
     */
    void setUserAsLeader(Long orgId, Long userId, Integer isLeader);

    /**
     * 获取用户的所有组织关系
     */
    List<UserOrgVO> getUserOrgs(Long userId);
}
