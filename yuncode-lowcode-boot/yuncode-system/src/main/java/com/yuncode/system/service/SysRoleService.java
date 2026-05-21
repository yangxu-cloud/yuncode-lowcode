package com.yuncode.system.service;

import com.yuncode.system.entity.SysRole;
import com.yuncode.system.vo.RoleDetailVO;
import com.yuncode.system.vo.RoleNodeVO;

import java.util.List;

/**
 * 角色服务接口
 */
public interface SysRoleService {

    /**
     * 获取角色树
     *
     * @return 角色树
     */
    List<RoleNodeVO> getRoleTree();

    /**
     * 获取角色详情
     *
     * @param id 角色ID
     * @return 角色详情
     */
    RoleDetailVO getRoleDetail(Long id);

    /**
     * 新增角色
     *
     * @param role 角色信息
     */
    void createRole(SysRole role);

    /**
     * 编辑角色
     *
     * @param id   角色ID
     * @param role 角色信息
     */
    void updateRole(Long id, SysRole role);

    /**
     * 删除角色
     *
     * @param id 角色ID
     */
    void deleteRole(Long id);
}
