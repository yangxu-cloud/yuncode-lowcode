package com.yuncode.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuncode.system.dto.OrgQueryDTO;
import com.yuncode.system.entity.SysOrg;
import com.yuncode.system.entity.SysUser;
import com.yuncode.system.entity.SysUserOrg;
import com.yuncode.system.mapper.SysOrgMapper;
import com.yuncode.system.mapper.SysUserMapper;
import com.yuncode.system.mapper.SysUserOrgMapper;
import com.yuncode.system.service.OrgService;
import com.yuncode.system.vo.OrgTreeNode;
import com.yuncode.system.vo.OrgVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 组织服务实现类
 *
 * @author Yuncode
 * @since 2025-01-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgServiceImpl implements OrgService {

    private final SysOrgMapper orgMapper;
    private final SysUserOrgMapper userOrgMapper;
    private final SysUserMapper userMapper;

    @Override
    public List<OrgTreeNode> getOrgTree(Long tenantId) {
        // 查询所有组织
        List<SysOrg> allOrgs = orgMapper.selectOrgTree(tenantId);

        // 构建组织树节点
        List<OrgTreeNode> treeNodes = new ArrayList<>();
        for (SysOrg org : allOrgs) {
            OrgTreeNode node = convertToOrgTreeNode(org);
            treeNodes.add(node);
        }

        // 构建树形结构（包含人员）
        return buildTreeWithUsers(treeNodes, 0L, tenantId);
    }

    @Override
    public List<OrgVO> getOrgList(OrgQueryDTO queryDTO, Long tenantId) {
        LambdaQueryWrapper<SysOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysOrg::getTenantId, tenantId);

        if (queryDTO != null) {
            if (StringUtils.hasText(queryDTO.getOrgName())) {
                wrapper.like(SysOrg::getOrgName, queryDTO.getOrgName());
            }
            if (StringUtils.hasText(queryDTO.getOrgCode())) {
                wrapper.like(SysOrg::getOrgCode, queryDTO.getOrgCode());
            }
            if (queryDTO.getParentId() != null) {
                wrapper.eq(SysOrg::getParentId, queryDTO.getParentId());
            }
            if (queryDTO.getOrgType() != null) {
                wrapper.eq(SysOrg::getOrgType, queryDTO.getOrgType());
            }
            if (queryDTO.getIsCompany() != null) {
                wrapper.eq(SysOrg::getIsCompany, queryDTO.getIsCompany());
            }
            if (queryDTO.getStatus() != null) {
                wrapper.eq(SysOrg::getStatus, queryDTO.getStatus());
            }
        }

        wrapper.orderByAsc(SysOrg::getSortOrder);

        List<SysOrg> orgs = orgMapper.selectList(wrapper);
        return orgs.stream()
                .map(this::convertToOrgVO)
                .collect(Collectors.toList());
    }

    @Override
    public OrgVO getOrgById(Long id, Long tenantId) {
        SysOrg org = orgMapper.selectById(id);
        if (org == null || !org.getTenantId().equals(tenantId)) {
            return null;
        }
        return convertToOrgVO(org);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addOrg(SysOrg org) {
        // 检查组织编码是否已存在
        if (checkOrgCodeExists(org.getOrgCode(), org.getTenantId(), null)) {
            throw new RuntimeException("组织编码已存在");
        }

        // 设置默认值
        if (org.getSortOrder() == null) {
            org.setSortOrder(0);
        }
        if (org.getStatus() == null) {
            org.setStatus(1);
        }

        return orgMapper.insert(org) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateOrg(SysOrg org) {
        // 检查组织编码是否已存在
        if (checkOrgCodeExists(org.getOrgCode(), org.getTenantId(), org.getId())) {
            throw new RuntimeException("组织编码已存在");
        }

        // 检查是否有子组织
        if (org.getIsCompany() != null && org.getIsCompany() == 0) {
            // 如果改为非公司，检查是否有子组织
            LambdaQueryWrapper<SysOrg> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysOrg::getParentId, org.getId())
                    .eq(SysOrg::getTenantId, org.getTenantId())
                    .eq(SysOrg::getIsCompany, 1);
            Long count = orgMapper.selectCount(wrapper);
            if (count > 0) {
                throw new RuntimeException("该组织下有公司，不能改为部门");
            }
        }

        return orgMapper.updateById(org) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteOrg(Long id, Long tenantId) {
        // 检查是否有子组织
        LambdaQueryWrapper<SysOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysOrg::getParentId, id)
                .eq(SysOrg::getTenantId, tenantId);
        Long childCount = orgMapper.selectCount(wrapper);
        if (childCount > 0) {
            throw new RuntimeException("该组织下有子组织，不能删除");
        }

        // 检查是否有关联用户
        int userCount = userOrgMapper.countUsersByOrgId(id, tenantId);
        if (userCount > 0) {
            throw new RuntimeException("该组织下有用户，不能删除");
        }

        return orgMapper.deleteById(id) > 0;
    }

    @Override
    public List<OrgVO> searchOrgs(String keyword, Long tenantId) {
        List<SysOrg> orgs = orgMapper.searchOrgs(keyword, tenantId);
        return orgs.stream()
                .map(this::convertToOrgVO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean checkOrgCodeExists(String orgCode, Long tenantId, Long excludeId) {
        LambdaQueryWrapper<SysOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysOrg::getOrgCode, orgCode)
                .eq(SysOrg::getTenantId, tenantId);
        if (excludeId != null) {
            wrapper.ne(SysOrg::getId, excludeId);
        }
        return orgMapper.selectCount(wrapper) > 0;
    }

    /**
     * 构建树形结构（包含人员）
     *
     * @param nodes 所有节点
     * @param parentId 父节点ID
     * @param tenantId 租户ID
     * @return 树形结构
     */
    private List<OrgTreeNode> buildTreeWithUsers(List<OrgTreeNode> nodes, Long parentId, Long tenantId) {
        List<OrgTreeNode> result = new ArrayList<>();

        for (OrgTreeNode node : nodes) {
            if (node.getParentId().equals(parentId)) {
                // 递归获取子节点
                List<OrgTreeNode> children = buildTreeWithUsers(nodes, node.getId(), tenantId);
                node.setChildren(children);

                // 获取该组织下的人员
                List<OrgTreeNode> userNodes = getUsersByOrgId(node.getId(), tenantId);
                if (!userNodes.isEmpty()) {
                    if (node.getChildren() == null) {
                        node.setChildren(new ArrayList<>());
                    }
                    node.getChildren().addAll(userNodes);
                }

                result.add(node);
            }
        }

        return result;
    }

    /**
     * 获取组织下的人员
     *
     * @param orgId 组织ID
     * @param tenantId 租户ID
     * @return 用户节点列表
     */
    private List<OrgTreeNode> getUsersByOrgId(Long orgId, Long tenantId) {
        List<SysUserOrg> userOrgs = userOrgMapper.selectByOrgId(orgId, tenantId);
        List<OrgTreeNode> userNodes = new ArrayList<>();

        for (SysUserOrg userOrg : userOrgs) {
            SysUser user = userMapper.selectById(userOrg.getUserId());
            if (user != null) {
                OrgTreeNode userNode = new OrgTreeNode();
                userNode.setId(user.getId());
                userNode.setNodeType("user");
                userNode.setLabel(user.getNickname() != null ? user.getNickname() : user.getUsername());
                userNode.setParentId(orgId);
                userNode.setUserId(user.getId());
                userNode.setUsername(user.getUsername());
                userNode.setNickname(user.getNickname());
                userNode.setAvatar(user.getAvatar());
                userNode.setIsLeader(userOrg.getIsLeader());
                userNode.setIsMainDept(userOrg.getIsMainDept());
                userNode.setSortOrder(999); // 用户排在最后
                userNodes.add(userNode);
            }
        }

        return userNodes;
    }

    /**
     * 转换为组织树节点
     *
     * @param org 组织实体
     * @return 组织树节点
     */
    private OrgTreeNode convertToOrgTreeNode(SysOrg org) {
        OrgTreeNode node = new OrgTreeNode();
        node.setId(org.getId());
        node.setNodeType("org");
        node.setLabel(org.getOrgName());
        node.setOrgCode(org.getOrgCode());
        node.setParentId(org.getParentId());
        node.setOrgType(org.getOrgType());
        node.setIsCompany(org.getIsCompany());
        node.setSortOrder(org.getSortOrder());
        return node;
    }

    /**
     * 转换为组织视图对象
     *
     * @param org 组织实体
     * @return 组织视图对象
     */
    private OrgVO convertToOrgVO(SysOrg org) {
        if (org == null) {
            return new OrgVO();
        }
        OrgVO vo = new OrgVO();
        BeanUtils.copyProperties(org, vo);

        // 设置组织类型名称
        if (org.getOrgType() != null) {
            vo.setOrgTypeName(org.getOrgType() == 1 ? "集团/公司" : "部门");
        }

        // 设置是否公司名称
        if (org.getIsCompany() != null) {
            vo.setIsCompanyName(org.getIsCompany() == 1 ? "是" : "否");
        }

        // 设置状态名称
        if (org.getStatus() != null) {
            vo.setStatusName(org.getStatus() == 1 ? "启用" : "禁用");
        }

        // 获取用户数量
        int userCount = userOrgMapper.countUsersByOrgId(org.getId(), org.getTenantId());
        vo.setUserCount(userCount);

        return vo;
    }
}
