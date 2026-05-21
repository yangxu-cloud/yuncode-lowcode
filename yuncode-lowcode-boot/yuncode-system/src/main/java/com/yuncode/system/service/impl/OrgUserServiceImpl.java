package com.yuncode.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuncode.system.entity.SysOrg;
import com.yuncode.system.entity.SysUser;
import com.yuncode.system.entity.SysUserOrg;
import com.yuncode.system.mapper.SysOrgMapper;
import com.yuncode.system.mapper.SysUserMapper;
import com.yuncode.system.mapper.SysUserOrgMapper;
import com.yuncode.system.service.OrgUserService;
import com.yuncode.system.vo.UserOrgVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 组织人员关系服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class OrgUserServiceImpl implements OrgUserService {

    private final SysOrgMapper orgMapper;
    private final SysUserOrgMapper userOrgMapper;
    private final SysUserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addUserToOrg(Long orgId, Long userId, Integer isLeader, Integer isMainDept) {
        log.info("添加用户到组织: orgId={}, userId={}", orgId, userId);

        SysOrg org = orgMapper.selectByIdIgnoreTenant(orgId);
        if (org == null) {
            throw new RuntimeException("组织不存在");
        }

        SysUser user = userMapper.selectByIdIgnoreTenant(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        LambdaQueryWrapper<SysUserOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserOrg::getOrgId, orgId)
                .eq(SysUserOrg::getUserId, userId);
        SysUserOrg existing = userOrgMapper.selectOne(wrapper);
        if (existing != null) {
            throw new RuntimeException("用户已在该组织中");
        }

        SysUserOrg userOrg = new SysUserOrg();
        userOrg.setOrgId(orgId);
        userOrg.setUserId(userId);
        userOrg.setIsLeader(isLeader != null ? isLeader : 0);
        userOrg.setIsMainDept(isMainDept != null ? isMainDept : 0);
        userOrg.setTenantId(org.getTenantId());
        userOrg.setDeleted(0);

        int result = userOrgMapper.insert(userOrg);
        if (result <= 0 || userOrg.getId() == null) {
            throw new RuntimeException("添加用户到组织失败");
        }

        SysUser updateUser = new SysUser();
        updateUser.setId(userId);

        if (org.getOrgType() != null && org.getOrgType() == 2) {
            updateUser.setDeptId(orgId);
            updateUser.setCompanyId(org.getCompanyId());
        } else if (org.getIsCompany() != null && org.getIsCompany() == 1) {
            updateUser.setCompanyId(orgId);
        } else if (org.getOrgType() != null && org.getOrgType() == 0) {
            log.info("用户添加到根节点: userId={}, orgId={}", userId, orgId);
        }

        if (updateUser.getCompanyId() != null || updateUser.getDeptId() != null) {
            userMapper.updateByIdIgnoreTenant(updateUser);
        }

        log.info("用户组织关系创建成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeUserFromOrg(Long orgId, Long userId) {
        log.info("从组织移除用户: orgId={}, userId={}", orgId, userId);

        LambdaQueryWrapper<SysUserOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserOrg::getOrgId, orgId)
                .eq(SysUserOrg::getUserId, userId);
        SysUserOrg userOrg = userOrgMapper.selectOne(wrapper);
        if (userOrg == null) {
            throw new RuntimeException("用户不在该组织中");
        }
        if (userOrg.getIsMainDept() == 1) {
            throw new RuntimeException("不能移除用户的主部门");
        }

        userOrgMapper.deletePhysicalById(userOrg.getId());
        log.info("删除用户组织关系成功: id={}, userId={}, orgId={}", userOrg.getId(), userId, orgId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setUserAsLeader(Long orgId, Long userId, Integer isLeader) {
        LambdaQueryWrapper<SysUserOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserOrg::getOrgId, orgId)
                .eq(SysUserOrg::getUserId, userId);
        SysUserOrg userOrg = userOrgMapper.selectOne(wrapper);
        if (userOrg == null) {
            throw new RuntimeException("用户不在该组织中");
        }

        userOrg.setIsLeader(isLeader);
        userOrgMapper.updateById(userOrg);
    }

    @Override
    public List<UserOrgVO> getUserOrgs(Long userId) {
        log.info("获取用户组织关系, userId={}", userId);

        SysUser user = userMapper.selectByIdIgnoreTenant(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        LambdaQueryWrapper<SysUserOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserOrg::getUserId, userId)
                .eq(SysUserOrg::getDeleted, 0)
                .orderByAsc(SysUserOrg::getIsMainDept)
                .orderByAsc(SysUserOrg::getCreateTime);
        List<SysUserOrg> userOrgs = userOrgMapper.selectList(wrapper);

        if (userOrgs.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> orgIds = userOrgs.stream()
                .map(SysUserOrg::getOrgId)
                .collect(Collectors.toList());

        List<SysOrg> orgs = new ArrayList<>();
        for (Long orgId : orgIds) {
            SysOrg org = orgMapper.selectByIdIgnoreTenant(orgId);
            if (org != null) {
                orgs.add(org);
            }
        }
        Map<Long, SysOrg> orgMap = orgs.stream()
                .collect(Collectors.toMap(SysOrg::getId, org -> org));

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
            vo.setOrgPath(buildOrgPath(org));

            result.add(vo);
        }

        log.info("获取用户组织关系成功, userId={}, count={}", userId, result.size());
        return result;
    }

    private String buildOrgPath(SysOrg org) {
        if (org == null) {
            return "";
        }

        List<String> pathNames = new ArrayList<>();
        SysOrg current = org;

        while (current != null && current.getParentId() != null && current.getParentId() != 0) {
            pathNames.add(0, current.getOrgName());
            current = orgMapper.selectById(current.getParentId());
            if (pathNames.size() > 20) {
                break;
            }
        }

        if (current != null) {
            pathNames.add(0, current.getOrgName());
        }

        return String.join(" / ", pathNames);
    }
}
