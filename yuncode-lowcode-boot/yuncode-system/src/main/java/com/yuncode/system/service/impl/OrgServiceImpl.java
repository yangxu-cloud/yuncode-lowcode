package com.yuncode.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuncode.common.util.security.SecurityUtil;
import com.yuncode.system.dto.OrgQueryDTO;
import com.yuncode.system.dto.TenantConfigDTO;
import com.yuncode.system.entity.SysOrg;
import com.yuncode.system.entity.SysUser;
import com.yuncode.system.entity.SysUserOrg;
import com.yuncode.system.mapper.SysOrgMapper;
import com.yuncode.system.mapper.SysUserMapper;
import com.yuncode.system.mapper.SysUserOrgMapper;
import com.yuncode.system.service.OrgService;
import com.yuncode.system.vo.OrgTreeNode;
import com.yuncode.system.vo.OrgVO;
import com.yuncode.system.vo.UserOrgVO;
import com.yuncode.tenant.entity.SysTenant;
import com.yuncode.tenant.mapper.SysTenantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final SysTenantMapper tenantMapper;

    @Override
    public List<OrgTreeNode> getOrgTree() {
        // 查询所有组织
        List<SysOrg> allOrgs;
        try {
            // 判断是否是平台管理员
            if (SecurityUtil.isPlatformAdmin()) {
                // 平台管理员查看所有租户的组织，不受租户限制
                log.info("平台管理员查询所有组织（忽略租户限制）");
                allOrgs = orgMapper.selectAllForPlatformAdmin();
            } else {
                // 普通用户只能查看自己租户的组织（多租户插件会自动添加 tenant_id 条件）
                log.info("普通用户查询组织（受租户限制，tenantId={}）", SecurityUtil.getTenantId());
                allOrgs = orgMapper.selectList(new LambdaQueryWrapper<>());
            }
        } catch (Exception e) {
            // 如果获取用户信息失败（如未登录），使用默认查询（多租户插件会自动添加 tenant_id 条件）
            log.warn("获取用户信息失败，使用默认租户限制查询", e);
            allOrgs = orgMapper.selectList(new LambdaQueryWrapper<>());
        }

        // 构建组织树节点
        List<OrgTreeNode> treeNodes = new ArrayList<>();
        for (SysOrg org : allOrgs) {
            OrgTreeNode node = convertToOrgTreeNode(org);
            treeNodes.add(node);
        }

        // 构建树形结构（包含人员）
        return buildTreeWithUsers(treeNodes, 0L);
    }

    @Override
    public List<OrgVO> getOrgList(OrgQueryDTO queryDTO) {
        // 多租户插件会自动添加 tenant_id 条件
        LambdaQueryWrapper<SysOrg> wrapper = new LambdaQueryWrapper<>();

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
    public OrgVO getOrgById(Long id) {
        // 多租户插件会自动添加 tenant_id 条件，无需手动检查
        SysOrg org = orgMapper.selectById(id);
        if (org == null) {
            return null;
        }
        return convertToOrgVO(org);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addOrg(SysOrg org) {
        return addOrg(org, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addOrg(SysOrg org, TenantConfigDTO tenantConfig) {
        // 检查组织编码是否已存在（多租户插件会自动添加 tenant_id 条件）
        if (checkOrgCodeExists(org.getOrgCode(), null)) {
            throw new RuntimeException("组织编码已存在");
        }

        // 设置默认值
        if (org.getSortOrder() == null) {
            org.setSortOrder(0);
        }
        if (org.getStatus() == null) {
            org.setStatus(1);
        }
        if (org.getDeleted() == null) {
            org.setDeleted(0); // 0-未删除
        }

        // 如果是公司节点（orgType=1），创建租户
        if (org.getOrgType() != null && org.getOrgType() == 1) {
            // 处理 company_id 逻辑：
            // 1. 如果是顶级公司（parent_id=0），company_id = null
            // 2. 如果是子公司（父级是公司），company_id = 父组织ID
            if (org.getParentId() != null && org.getParentId() > 0) {
                // 有父级，是子公司
                // 使用 selectByIdIgnoreTenant 忽略租户限制查询父组织
                SysOrg parentOrg = orgMapper.selectByIdIgnoreTenant(org.getParentId());
                if (parentOrg != null && parentOrg.getIsCompany() != null && parentOrg.getIsCompany() == 1) {
                    // 父级是公司，当前公司继承父公司的ID作为 company_id
                    org.setCompanyId(parentOrg.getId());
                    log.info("子公司继承父公司ID: companyId={}, parentCompanyId={}",
                        org.getCompanyId(), parentOrg.getId());
                } else if (parentOrg == null) {
                    log.warn("父组织不存在: parentId={}", org.getParentId());
                } else {
                    log.info("父级不是公司，不设置company_id: parentId={}, parentIsCompany={}",
                        org.getParentId(), parentOrg.getIsCompany());
                }
            } else {
                // 顶级公司，company_id 保持为 null
                log.info("创建顶级公司，company_id=null");
            }

            // 判断是否需要创建新租户
            // 如果前端已经传了 tenantId（说明是子公司，继承父公司的租户），则不创建新租户
            if (org.getTenantId() != null) {
                // 子公司：使用父公司的租户ID和租户编码
                log.info("子公司使用父公司租户: tenantId={}", org.getTenantId());

                // 如果前端没有传 tenantCode，从父组织查询并继承
                if (org.getTenantCode() == null || org.getTenantCode().isEmpty()) {
                    if (org.getParentId() != null && org.getParentId() > 0) {
                        SysOrg parentOrg = orgMapper.selectByIdIgnoreTenant(org.getParentId());
                        if (parentOrg != null && parentOrg.getTenantCode() != null) {
                            org.setTenantCode(parentOrg.getTenantCode());
                            log.info("子公司继承父公司租户编码: tenantCode={}", parentOrg.getTenantCode());
                        }
                    }
                }

                int result = orgMapper.insert(org);
                log.info("创建子公司成功: orgId={}, tenantId={}, tenantCode={}, companyId={}",
                        org.getId(), org.getTenantId(), org.getTenantCode(), org.getCompanyId());
                return result > 0;
            }

            // 顶级公司：创建新租户
            // 创建租户记录
            if (tenantConfig == null) {
                tenantConfig = new TenantConfigDTO();
            }

            SysTenant tenant = new SysTenant();
            tenant.setTenantName(org.getOrgName());
            tenant.setTenantCode(org.getTenantCode());
            tenant.setTenantType(tenantConfig.getTenantType() != null ? tenantConfig.getTenantType() : 1);
            tenant.setUserLimit(tenantConfig.getUserLimit() != null ? tenantConfig.getUserLimit() : 100);
            tenant.setStorageLimit(tenantConfig.getStorageLimit() != null ? tenantConfig.getStorageLimit() : 10240);
            tenant.setExpireTime(tenantConfig.getExpireTime());
            tenant.setContactName(tenantConfig.getContactName());
            tenant.setContactPhone(tenantConfig.getContactPhone());
            tenant.setContactEmail(tenantConfig.getContactEmail());
            tenant.setAddress(tenantConfig.getAddress());
            tenant.setRemark(tenantConfig.getRemark());
            tenant.setStatus(0); // 0-正常

            // 保存租户
            tenantMapper.insert(tenant);
            log.info("创建租户成功: tenantId={}, tenantCode={}, tenantName={}",
                    tenant.getId(), tenant.getTenantCode(), tenant.getTenantName());

            // 将租户ID和租户编码关联到组织
            org.setTenantId(tenant.getId());
            org.setTenantCode(tenant.getTenantCode());

            // 保存组织
            int result = orgMapper.insert(org);

            // 更新租户的 org_id 关联
            tenant.setOrgId(org.getId());
            tenantMapper.updateById(tenant);

            log.info("创建公司（租户）成功: orgId={}, tenantId={}, tenantCode={}, companyId={}",
                    org.getId(), tenant.getId(), tenant.getTenantCode(), org.getCompanyId());

            return result > 0;
        } else {
            // 如果是部门节点，继承父节点的租户信息和公司信息
            if (org.getParentId() != null && org.getParentId() > 0) {
                // 如果前端没有传 tenantId，才从父组织查询
                if (org.getTenantId() == null) {
                    // 使用 selectByIdIgnoreTenant 忽略租户限制查询父组织
                    SysOrg parentOrg = orgMapper.selectByIdIgnoreTenant(org.getParentId());
                    if (parentOrg != null) {
                        org.setTenantId(parentOrg.getTenantId());
                        org.setTenantCode(parentOrg.getTenantCode());

                        log.info("父组织信息: parentId={}, parentName={}, isCompany={}, parentCompanyId={}",
                            parentOrg.getId(), parentOrg.getOrgName(), parentOrg.getIsCompany(), parentOrg.getCompanyId());
                    } else {
                        log.warn("父组织不存在: parentId={}", org.getParentId());
                    }
                }

                // 查询父组织以获取 company_id（无论 tenantId 是否已设置）
                SysOrg parentOrg = orgMapper.selectByIdIgnoreTenant(org.getParentId());
                if (parentOrg != null) {

                    // 继承 company_id 逻辑：
                    // 1. 如果父级是公司（is_company=1），则 company_id = 父组织ID
                    // 2. 如果父级是部门，则 company_id = 父部门的 company_id
                    if (parentOrg.getIsCompany() != null && parentOrg.getIsCompany() == 1) {
                        // 父级是公司，当前部门的 company_id = 父组织ID
                        org.setCompanyId(parentOrg.getId());
                        log.info("部门继承公司ID: deptId={}, companyId={}", org.getId(), parentOrg.getId());
                    } else {
                        // 父级是部门或其他，继承父级的 company_id
                        org.setCompanyId(parentOrg.getCompanyId());
                        log.info("部门继承父级的company_id: deptId={}, companyId={}", org.getId(), parentOrg.getCompanyId());
                    }
                } else {
                    log.warn("父组织不存在: parentId={}", org.getParentId());
                }
            }

            log.info("准备插入部门: orgId={}, parentId={}, companyId={}", org.getId(), org.getParentId(), org.getCompanyId());
            return orgMapper.insert(org) > 0;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateOrg(SysOrg org) {
        // 检查组织编码是否已存在（多租户插件会自动添加 tenant_id 条件）
        if (checkOrgCodeExists(org.getOrgCode(), org.getId())) {
            throw new RuntimeException("组织编码已存在");
        }

        // 检查是否有子组织（多租户插件会自动添加 tenant_id 条件）
        if (org.getIsCompany() != null && org.getIsCompany() == 0) {
            // 如果改为非公司，检查是否有子组织
            LambdaQueryWrapper<SysOrg> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysOrg::getParentId, org.getId())
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
    public boolean deleteOrg(Long id) {
        log.info("开始删除组织: id={}", id);

        // 1. 一次性查询所有组织
        List<SysOrg> allOrgs = orgMapper.selectAllForPlatformAdmin();
        log.info("查询到所有组织: count={}", allOrgs.size());

        // 2. 在内存中构建组织ID映射（用于快速查找子组织）
        Map<Long, List<SysOrg>> parentChildrenMap = allOrgs.stream()
                .filter(org -> org.getParentId() != null && org.getDeleted() != null && org.getDeleted() == 0)
                .collect(Collectors.groupingBy(SysOrg::getParentId));

        // 3. 收集所有要删除的组织ID（包括所有子孙组织）
        List<Long> orgIds = new ArrayList<>();
        collectOrgIdsOptimized(id, parentChildrenMap, orgIds);
        log.info("收集到要删除的组织ID: orgIds={}, 总数={}", orgIds, orgIds.size());

        // 4. 收集要删除的租户ID（一级公司 orgType=1 或 isCompany=1）- 必须在删除组织之前执行
        Set<Long> tenantIdsToDelete = new HashSet<>();
        for (Long orgId : orgIds) {
            SysOrg org = orgMapper.selectByIdIgnoreTenant(orgId);
            log.info("检查组织: orgId={}, orgName={}, orgType={}, isCompany={}, tenantId={}",
                    org != null ? org.getId() : null,
                    org != null ? org.getOrgName() : null,
                    org != null ? org.getOrgType() : null,
                    org != null ? org.getIsCompany() : null,
                    org != null ? org.getTenantId() : null);

            if (org != null && org.getTenantId() != null) {
                // 检查是否是一级公司（orgType=1 或 isCompany=1，且没有父公司ID）
                boolean isTopLevelCompany = (org.getOrgType() != null && org.getOrgType() == 1)
                        || (org.getIsCompany() != null && org.getIsCompany() == 1);
                if (isTopLevelCompany) {
                    tenantIdsToDelete.add(org.getTenantId());
                    log.info("收集到要删除的租户ID: tenantId={}, orgId={}, orgName={}, orgType={}, isCompany={}",
                            org.getTenantId(), org.getId(), org.getOrgName(), org.getOrgType(), org.getIsCompany());
                }
            }
        }
        log.info("收集到要删除的租户ID: tenantIds={}, 总数={}", tenantIdsToDelete, tenantIdsToDelete.size());

        // 5. 收集所有要删除的用户ID
        Set<Long> userIds = new HashSet<>();
        for (Long orgId : orgIds) {
            List<SysUserOrg> userOrgs = userOrgMapper.selectByOrgIdForPlatformAdmin(orgId);
            for (SysUserOrg userOrg : userOrgs) {
                userIds.add(userOrg.getUserId());
            }
        }
        log.info("收集到要删除的用户ID: userIds={}, 总数={}", userIds, userIds.size());

        // ========================================
        // 删除策略说明：
        // 1. sys_user_org（关联关系表）→ 物理删除（没有 @TableLogic）
        // 2. sys_user（人员表）→ 逻辑删除（有 @TableLogic，deleted=1）
        // 3. sys_org（组织表）→ 逻辑删除（有 @TableLogic，deleted=1）
        // 4. sys_tenant（租户表）→ 逻辑删除（有 @TableLogic，deleted=1）
        // ========================================

        // 6. 批量删除用户组织关系（物理删除 - 关联关系表不需要保留）
        // 注意：sys_user_org 没有 @TableLogic 注解，使用物理删除
        int deleteRelCount = userOrgMapper.deletePhysicalByOrgIds(orgIds);
        log.info("批量物理删除用户组织关系成功: count={}", deleteRelCount);

        // 7. 批量删除用户（逻辑删除 - 使用 IgnoreTenant 方法绕过多租户限制）
        if (!userIds.isEmpty()) {
            for (Long userId : userIds) {
                // 使用 deleteByIdIgnoreTenant 绕过多租户插件的 tenant_id 限制
                // @TableLogic 会自动将删除转换为 UPDATE deleted=1
                userMapper.deleteByIdIgnoreTenant(userId);
            }
            log.info("批量逻辑删除用户成功: 总数={}", userIds.size());
        }

        // 8. 批量删除组织（逻辑删除 - 使用 IgnoreTenant 方法绕过多租户限制）
        for (Long orgId : orgIds) {
            // 使用 deleteByIdIgnoreTenant 绕过多租户插件的 tenant_id 限制
            // @TableLogic 会自动将删除转换为 UPDATE deleted=1
            orgMapper.deleteByIdIgnoreTenant(orgId);
        }
        log.info("批量逻辑删除组织成功: 总数={}", orgIds.size());

        // 9. 删除对应的租户（逻辑删除 - 使用 IgnoreTenant 方法绕过多租户限制）
        if (!tenantIdsToDelete.isEmpty()) {
            for (Long tenantId : tenantIdsToDelete) {
                // 使用 deleteByIdIgnoreTenant 绕过多租户插件的 tenant_id 限制
                // @TableLogic 会自动将删除转换为 UPDATE deleted=1
                tenantMapper.deleteByIdIgnoreTenant(tenantId);
                log.info("逻辑删除租户成功: tenantId={}", tenantId);
            }
            log.info("批量逻辑删除租户完成: 总数={}", tenantIdsToDelete.size());
        }

        // ========================================
        // 如果需要切换为物理删除，可以使用以下保留的代码：
        // ========================================
        // // 7. 批量物理删除用户（取消注释即可使用）
        // if (!userIds.isEmpty()) {
        //     List<Long> userIdList = new ArrayList<>(userIds);
        //     int deleteUserCount = userMapper.deletePhysicalByIds(userIdList);
        //     log.info("批量物理删除用户成功: count={}", deleteUserCount);
        // }
        //
        // // 8. 批量物理删除组织（取消注释即可使用）
        // int deleteOrgCount = orgMapper.deletePhysicalByIds(orgIds);
        // log.info("批量物理删除组织成功: count={}", deleteOrgCount);
        //
        // // 9. 批量物理删除租户（取消注释即可使用）
        // if (!tenantIdsToDelete.isEmpty()) {
        //     for (Long tenantId : tenantIdsToDelete) {
        //         int deleteResult = tenantMapper.deletePhysicalById(tenantId);
        //         log.info("物理删除租户{}: tenantId={}, result={}",
        //             deleteResult > 0 ? "成功" : "失败", tenantId, deleteResult);
        //     }
        //     log.info("批量物理删除租户完成: 总数={}", tenantIdsToDelete.size());
        // }
        // ========================================

        return true;
    }

    /**
     * 递归收集所有组织ID（优化版：使用预构建的父子关系映射）
     *
     * @param orgId 组织ID
     * @param parentChildrenMap 父子关系映射
     * @param orgIds 收集结果
     */
    private void collectOrgIdsOptimized(Long orgId, Map<Long, List<SysOrg>> parentChildrenMap, List<Long> orgIds) {
        orgIds.add(orgId);
        log.info("添加组织ID: orgId={}", orgId);

        List<SysOrg> children = parentChildrenMap.get(orgId);
        if (children != null && !children.isEmpty()) {
            for (SysOrg child : children) {
                collectOrgIdsOptimized(child.getId(), parentChildrenMap, orgIds);
            }
        }
    }

    @Override
    public List<OrgVO> searchOrgs(String keyword) {
        // 多租户插件会自动添加 tenant_id 条件
        LambdaQueryWrapper<SysOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.like(SysOrg::getOrgName, keyword)
                .or()
                .like(SysOrg::getOrgCode, keyword));

        List<SysOrg> orgs = orgMapper.selectList(wrapper);
        return orgs.stream()
                .map(this::convertToOrgVO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean checkOrgCodeExists(String orgCode, Long excludeId) {
        // 多租户插件会自动添加 tenant_id 条件
        LambdaQueryWrapper<SysOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysOrg::getOrgCode, orgCode);
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
     * @return 树形结构
     */
    private List<OrgTreeNode> buildTreeWithUsers(List<OrgTreeNode> nodes, Long parentId) {
        List<OrgTreeNode> result = new ArrayList<>();

        for (OrgTreeNode node : nodes) {
            if (node.getParentId().equals(parentId)) {
                // 递归获取子节点
                List<OrgTreeNode> children = buildTreeWithUsers(nodes, node.getId());

                // 获取该组织下的人员（多租户插件会自动添加 tenant_id 条件）
                List<OrgTreeNode> userNodes = getUsersByOrgId(node.getId());
                if (!userNodes.isEmpty()) {
                    if (children == null) {
                        children = new ArrayList<>();
                    }
                    children.addAll(userNodes);
                }

                // 按照排序号排序子节点
                if (children != null && !children.isEmpty()) {
                    children.sort((a, b) -> {
                        Integer sortA = a.getSortOrder() != null ? a.getSortOrder() : 999;
                        Integer sortB = b.getSortOrder() != null ? b.getSortOrder() : 999;
                        return sortA.compareTo(sortB);
                    });
                }

                node.setChildren(children);
                result.add(node);
            }
        }

        // 对当前层级的节点也按照排序号排序
        result.sort((a, b) -> {
            Integer sortA = a.getSortOrder() != null ? a.getSortOrder() : 999;
            Integer sortB = b.getSortOrder() != null ? b.getSortOrder() : 999;
            return sortA.compareTo(sortB);
        });

        return result;
    }

    /**
     * 获取组织下的人员
     *
     * @param orgId 组织ID
     * @return 用户节点列表
     */
    private List<OrgTreeNode> getUsersByOrgId(Long orgId) {
        log.info("查询组织 {} 下的人员", orgId);

        List<SysUserOrg> userOrgs;
        try {
            // 判断是否是平台管理员
            if (SecurityUtil.isPlatformAdmin()) {
                // 平台管理员查看所有组织的用户，不受租户限制
                log.info("平台管理员查询组织 {} 的用户（忽略租户限制）", orgId);
                userOrgs = userOrgMapper.selectByOrgIdForPlatformAdmin(orgId);
            } else {
                // 普通用户只能查看自己租户的组织用户（多租户插件会自动添加 tenant_id 条件）
                log.info("普通用户查询组织 {} 的用户（受租户限制）", orgId);
                LambdaQueryWrapper<SysUserOrg> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(SysUserOrg::getOrgId, orgId);
                userOrgs = userOrgMapper.selectList(wrapper);
            }
        } catch (Exception e) {
            // 如果获取用户信息失败（如未登录），使用默认查询（多租户插件会自动添加 tenant_id 条件）
            log.warn("获取用户信息失败，使用默认租户限制查询", e);
            LambdaQueryWrapper<SysUserOrg> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUserOrg::getOrgId, orgId);
            userOrgs = userOrgMapper.selectList(wrapper);
        }
        log.info("查询到 {} 条用户组织关系", userOrgs.size());
        List<OrgTreeNode> userNodes = new ArrayList<>();

        for (SysUserOrg userOrg : userOrgs) {
            log.info("处理用户组织关系: userId={}, orgId={}, isLeader={}, isMainDept={}",
                userOrg.getUserId(), userOrg.getOrgId(), userOrg.getIsLeader(), userOrg.getIsMainDept());
            // 使用 selectByIdIgnoreTenant 忽略租户限制
            // 注意：用户可能是在其他租户下创建的，需要忽略租户限制
            SysUser user = userMapper.selectByIdIgnoreTenant(userOrg.getUserId());
            if (user != null) {
                // 获取组织的租户编码
                SysOrg org = orgMapper.selectByIdIgnoreTenant(orgId);
                String tenantCode = (org != null) ? org.getTenantCode() : null;

                OrgTreeNode userNode = new OrgTreeNode();
                userNode.setId(user.getId());
                userNode.setNodeType("user");
                // 优先使用真实姓名，其次昵称，最后用户名
                String displayName = user.getRealName() != null && !user.getRealName().isEmpty()
                    ? user.getRealName()
                    : (user.getNickname() != null && !user.getNickname().isEmpty()
                        ? user.getNickname()
                        : user.getUsername());

                // 检查当前用户在当前组织中的角色（主部门 vs 兼职部门）
                // 只有当当前组织不是用户的主部门时，才显示"(兼)"标识
                boolean isMainDeptInCurrentOrg = (userOrg.getIsMainDept() != null && userOrg.getIsMainDept() == 1);

                log.info("用户 {} 在当前组织 {} 的角色: isMainDept={}, 是否主部门={}",
                    user.getUsername(), orgId, userOrg.getIsMainDept(), isMainDeptInCurrentOrg);

                // 只有在非主部门（兼职部门）中才显示"(兼)"标识
                if (!isMainDeptInCurrentOrg) {
                    displayName += "(兼)";
                    log.info("用户 {} 在当前组织 {} 中显示(兼)标识", displayName, orgId);
                }

                userNode.setLabel(displayName);
                userNode.setParentId(orgId);
                userNode.setUserId(user.getId());
                userNode.setUsername(user.getUsername());
                userNode.setRealName(user.getRealName());
                userNode.setNickname(user.getNickname());
                userNode.setAvatar(user.getAvatar());
                userNode.setEmail(user.getEmail());
                userNode.setPhone(user.getPhone());
                userNode.setGender(user.getGender());
                userNode.setStatus(user.getStatus());
                userNode.setIsLeader(userOrg.getIsLeader());
                userNode.setIsMainDept(userOrg.getIsMainDept());
                userNode.setTenantId(user.getTenantId()); // 添加租户ID
                userNode.setTenantCode(tenantCode); // 从组织获取租户编码
                userNode.setSortOrder(999); // 用户排在最后
                userNodes.add(userNode);
                log.info("添加用户节点: id={}, label={}, username={}, tenantId={}, tenantCode={}, isMainDept={}",
                    userNode.getId(), userNode.getLabel(), userNode.getUsername(), userNode.getTenantId(), userNode.getTenantCode(), userOrg.getIsMainDept());
            } else {
                log.warn("用户不存在: userId={}", userOrg.getUserId());
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
        node.setTenantId(org.getTenantId());
        node.setTenantCode(org.getTenantCode());
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

        // 获取用户数量（多租户插件会自动添加 tenant_id 条件）
        LambdaQueryWrapper<SysUserOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserOrg::getOrgId, org.getId());
        int userCount = Math.toIntExact(userOrgMapper.selectCount(wrapper));
        vo.setUserCount(userCount);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addUserToOrg(Long orgId, Long userId, Integer isLeader, Integer isMainDept) {
        log.info("添加用户到组织: orgId={}, userId={}", orgId, userId);

        // 检查组织是否存在（使用 selectByIdIgnoreTenant 忽略租户限制）
        // 注意：根据组织ID（主键）查询，不需要租户限制
        SysOrg org = orgMapper.selectByIdIgnoreTenant(orgId);
        if (org == null) {
            throw new RuntimeException("组织不存在");
        }
        log.info("组织存在: orgId={}, orgName={}, orgType={}, isCompany={}, companyId={}, tenantId={}",
            org.getId(), org.getOrgName(), org.getOrgType(), org.getIsCompany(), org.getCompanyId(), org.getTenantId());

        // 检查用户是否存在（使用 selectByIdIgnoreTenant 忽略租户限制）
        // 注意：用户可能是在其他租户下创建的，需要忽略租户限制
        SysUser user = userMapper.selectByIdIgnoreTenant(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        log.info("用户存在: userId={}, username={}, tenantId={}", user.getId(), user.getUsername(), user.getTenantId());

        // 检查用户是否已在该组织（多租户插件会自动添加 tenant_id 条件）
        LambdaQueryWrapper<SysUserOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserOrg::getOrgId, orgId)
                .eq(SysUserOrg::getUserId, userId);
        SysUserOrg existing = userOrgMapper.selectOne(wrapper);
        if (existing != null) {
            throw new RuntimeException("用户已在该组织中");
        }

        // 创建用户组织关系
        // 注意：需要手动设置 tenantId 为组织的 tenantId，而不是使用 session 的 tenantId
        SysUserOrg userOrg = new SysUserOrg();
        userOrg.setOrgId(orgId);
        userOrg.setUserId(userId);
        userOrg.setIsLeader(isLeader != null ? isLeader : 0);
        userOrg.setIsMainDept(isMainDept != null ? isMainDept : 0);
        userOrg.setTenantId(org.getTenantId()); // 使用组织的租户ID
        userOrg.setDeleted(0); // 显式设置未删除

        int result = userOrgMapper.insert(userOrg);
        log.info("用户组织关系插入结果: result={}, userOrg.id={}, tenantId={}",
            result, userOrg.getId(), userOrg.getTenantId());

        if (result <= 0 || userOrg.getId() == null) {
            throw new RuntimeException("添加用户到组织失败");
        }

        // 更新用户的部门ID和公司ID
        // 逻辑：
        // 1. 如果添加到部门（orgType=2），则 dept_id = 部门ID，company_id = 部门的company_id
        // 2. 如果添加到公司（is_company=1），则 company_id = 公司ID（公司本身作为用户的company_id）
        SysUser updateUser = new SysUser();
        updateUser.setId(userId);

        if (org.getOrgType() != null && org.getOrgType() == 2) {
            // 添加到部门
            // dept_id = 当前部门ID
            updateUser.setDeptId(orgId);
            // company_id = 部门的 company_id（部门已经从父级继承好的）
            updateUser.setCompanyId(org.getCompanyId());
            log.info("用户添加到部门: userId={}, deptId={}, companyId={}",
                userId, orgId, org.getCompanyId());
        } else if (org.getIsCompany() != null && org.getIsCompany() == 1) {
            // 添加到公司
            // company_id = 公司本身ID（用户的所属公司就是该公司）
            updateUser.setCompanyId(orgId);
            log.info("用户添加到公司: userId={}, companyId={}", userId, orgId);
        } else if (org.getOrgType() != null && org.getOrgType() == 0) {
            // 添加到根节点，不设置 company_id 和 dept_id
            log.info("用户添加到根节点: userId={}, orgId={}", userId, orgId);
        }

        // 如果有需要更新的字段，执行更新
        if (updateUser.getCompanyId() != null || updateUser.getDeptId() != null) {
            // 使用 updateByIdIgnoreTenant 忽略租户限制
            // 注意：用户可能是在其他租户下创建的，需要忽略租户限制
            int updateResult = userMapper.updateByIdIgnoreTenant(updateUser);
            log.info("用户信息更新结果: {}, deptId={}, companyId={}",
                updateResult, updateUser.getDeptId(), updateUser.getCompanyId());
        } else {
            log.warn("用户信息未更新: deptId={}, companyId={}",
                updateUser.getDeptId(), updateUser.getCompanyId());
        }

        log.info("用户组织关系创建成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeUserFromOrg(Long orgId, Long userId) {
        log.info("从组织移除用户: orgId={}, userId={}", orgId, userId);

        // 检查关系是否存在（多租户插件会自动添加 tenant_id 条件）
        LambdaQueryWrapper<SysUserOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserOrg::getOrgId, orgId)
                .eq(SysUserOrg::getUserId, userId);
        SysUserOrg userOrg = userOrgMapper.selectOne(wrapper);
        if (userOrg == null) {
            throw new RuntimeException("用户不在该组织中");
        }

        // 检查是否为主部门
        if (userOrg.getIsMainDept() == 1) {
            throw new RuntimeException("不能移除用户的主部门");
        }

        // 物理删除关系记录
        userOrgMapper.deletePhysicalById(userOrg.getId());
        log.info("删除用户组织关系成功: id={}, userId={}, orgId={}", userOrg.getId(), userId, orgId);

        // 注意：不删除用户记录，用户可能属于多个部门
        // 只删除用户与该部门的关系
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setUserAsLeader(Long orgId, Long userId, Integer isLeader) {
        // 检查关系是否存在（多租户插件会自动添加 tenant_id 条件）
        LambdaQueryWrapper<SysUserOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserOrg::getOrgId, orgId)
                .eq(SysUserOrg::getUserId, userId);
        SysUserOrg userOrg = userOrgMapper.selectOne(wrapper);
        if (userOrg == null) {
            throw new RuntimeException("用户不在该组织中");
        }

        // 更新负责人状态
        userOrg.setIsLeader(isLeader);
        userOrgMapper.updateById(userOrg);
    }

    @Override
    public List<UserOrgVO> getUserOrgs(Long userId) {
        log.info("获取用户组织关系, userId={}", userId);

        // 查询用户信息（使用 IgnoreTenant 绕过多租户限制）
        SysUser user = userMapper.selectByIdIgnoreTenant(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 查询用户的所有组织关系
        // 注意：sys_user_org 在 ignoreTable 列表中，不会被多租户插件过滤
        LambdaQueryWrapper<SysUserOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserOrg::getUserId, userId)
                .eq(SysUserOrg::getDeleted, 0)
                .orderByAsc(SysUserOrg::getIsMainDept) // 主部门排在前面
                .orderByAsc(SysUserOrg::getCreateTime);
        List<SysUserOrg> userOrgs = userOrgMapper.selectList(wrapper);

        if (userOrgs.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取所有组织ID
        List<Long> orgIds = userOrgs.stream()
                .map(SysUserOrg::getOrgId)
                .collect(Collectors.toList());

        // 批量查询组织信息（使用 IgnoreTenant 绕过多租户限制）
        List<SysOrg> orgs = new ArrayList<>();
        for (Long orgId : orgIds) {
            SysOrg org = orgMapper.selectByIdIgnoreTenant(orgId);
            if (org != null) {
                orgs.add(org);
            }
        }
        Map<Long, SysOrg> orgMap = orgs.stream()
                .collect(Collectors.toMap(SysOrg::getId, org -> org));

        // 构建返回结果
        List<UserOrgVO> result = new ArrayList<>();
        for (SysUserOrg userOrg : userOrgs) {
            SysOrg org = orgMap.get(userOrg.getOrgId());
            if (org == null) {
                continue;
            }

            UserOrgVO vo = new UserOrgVO();
            vo.setId(userOrg.getId());
            vo.setUserId(userId);
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setRealName(user.getRealName());
            vo.setOrgId(org.getId());
            vo.setOrgName(org.getOrgName());
            vo.setOrgCode(org.getOrgCode());
            vo.setOrgType(org.getOrgType());
            vo.setIsMainDept(userOrg.getIsMainDept());
            vo.setIsLeader(userOrg.getIsLeader());
            vo.setCreateTime(userOrg.getCreateTime());

            // 构建组织路径
            String orgPath = buildOrgPath(org);
            vo.setOrgPath(orgPath);

            result.add(vo);
        }

        log.info("获取用户组织关系成功, userId={}, count={}", userId, result.size());
        return result;
    }

    /**
     * 构建组织路径（从根到当前组织的完整路径）
     */
    private String buildOrgPath(SysOrg org) {
        if (org == null) {
            return "";
        }

        List<String> pathNames = new ArrayList<>();
        SysOrg current = org;

        // 向上遍历到根节点
        while (current != null && current.getParentId() != null && current.getParentId() != 0) {
            pathNames.add(0, current.getOrgName()); // 添加到列表头部

            // 查询父组织
            current = orgMapper.selectById(current.getParentId());

            // 防止循环引用
            if (pathNames.size() > 20) {
                break;
            }
        }

        // 添加根组织名称
        if (current != null) {
            pathNames.add(0, current.getOrgName());
        }

        return String.join(" / ", pathNames);
    }
}
