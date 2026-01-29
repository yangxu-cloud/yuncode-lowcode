package com.yuncode.system.service;

import com.yuncode.system.dto.OrgQueryDTO;
import com.yuncode.system.dto.TenantConfigDTO;
import com.yuncode.system.entity.SysOrg;
import com.yuncode.system.vo.OrgTreeNode;
import com.yuncode.system.vo.OrgVO;
import com.yuncode.system.vo.UserOrgVO;

import java.util.List;

/**
 * 组织服务接口
 *
 * @author Yuncode
 * @since 2025-01-22
 */
public interface OrgService {

    /**
     * 获取组织树（包含人员）
     *
     * @return 组织树节点列表
     */
    List<OrgTreeNode> getOrgTree();

    /**
     * 获取组织列表
     *
     * @param queryDTO 查询条件
     * @return 组织列表
     */
    List<OrgVO> getOrgList(OrgQueryDTO queryDTO);

    /**
     * 获取组织详情
     *
     * @param id 组织ID
     * @return 组织视图对象
     */
    OrgVO getOrgById(Long id);

    /**
     * 添加组织
     *
     * @param org 组织实体
     * @return 是否成功
     */
    boolean addOrg(SysOrg org);

    /**
     * 添加组织（带租户配置）
     * 当 orgType=1（公司）时，同步创建租户
     *
     * @param org 组织实体
     * @param tenantConfig 租户配置（orgType=1 时使用）
     * @return 是否成功
     */
    boolean addOrg(SysOrg org, TenantConfigDTO tenantConfig);

    /**
     * 更新组织
     *
     * @param org 组织实体
     * @return 是否成功
     */
    boolean updateOrg(SysOrg org);

    /**
     * 删除组织
     *
     * @param id 组织ID
     * @return 是否成功
     */
    boolean deleteOrg(Long id);

    /**
     * 搜索组织
     *
     * @param keyword 关键词
     * @return 组织列表
     */
    List<OrgVO> searchOrgs(String keyword);

    /**
     * 检查组织编码是否存在
     *
     * @param orgCode 组织编码
     * @param excludeId 排除的ID（更新时使用）
     * @return 是否存在
     */
    boolean checkOrgCodeExists(String orgCode, Long excludeId);

    /**
     * 添加人员到组织
     *
     * @param orgId 组织ID
     * @param userId 用户ID
     * @param isLeader 是否负责人
     * @param isMainDept 是否主部门
     */
    void addUserToOrg(Long orgId, Long userId, Integer isLeader, Integer isMainDept);

    /**
     * 从组织移除人员
     *
     * @param orgId 组织ID
     * @param userId 用户ID
     */
    void removeUserFromOrg(Long orgId, Long userId);

    /**
     * 设置用户为负责人
     *
     * @param orgId 组织ID
     * @param userId 用户ID
     * @param isLeader 是否负责人
     */
    void setUserAsLeader(Long orgId, Long userId, Integer isLeader);

    /**
     * 获取用户的所有组织关系（包括主部门和兼职部门）
     *
     * @param userId 用户ID
     * @return 用户组织关系列表
     */
    List<UserOrgVO> getUserOrgs(Long userId);
}
