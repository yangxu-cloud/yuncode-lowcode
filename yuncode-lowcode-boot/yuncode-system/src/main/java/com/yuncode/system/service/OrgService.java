package com.yuncode.system.service;

import com.yuncode.system.dto.OrgQueryDTO;
import com.yuncode.system.entity.SysOrg;
import com.yuncode.system.vo.OrgTreeNode;
import com.yuncode.system.vo.OrgVO;

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
     * @param tenantId 租户ID
     * @return 组织树节点列表
     */
    List<OrgTreeNode> getOrgTree(Long tenantId);

    /**
     * 获取组织列表
     *
     * @param queryDTO 查询条件
     * @param tenantId 租户ID
     * @return 组织列表
     */
    List<OrgVO> getOrgList(OrgQueryDTO queryDTO, Long tenantId);

    /**
     * 获取组织详情
     *
     * @param id 组织ID
     * @param tenantId 租户ID
     * @return 组织视图对象
     */
    OrgVO getOrgById(Long id, Long tenantId);

    /**
     * 添加组织
     *
     * @param org 组织实体
     * @return 是否成功
     */
    boolean addOrg(SysOrg org);

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
     * @param tenantId 租户ID
     * @return 是否成功
     */
    boolean deleteOrg(Long id, Long tenantId);

    /**
     * 搜索组织
     *
     * @param keyword 关键词
     * @param tenantId 租户ID
     * @return 组织列表
     */
    List<OrgVO> searchOrgs(String keyword, Long tenantId);

    /**
     * 检查组织编码是否存在
     *
     * @param orgCode 组织编码
     * @param tenantId 租户ID
     * @param excludeId 排除的ID（更新时使用）
     * @return 是否存在
     */
    boolean checkOrgCodeExists(String orgCode, Long tenantId, Long excludeId);
}
