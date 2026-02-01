/**
 * 组织 API - 适配器版本
 * 支持 Mock 数据源切换
 */

import { adapterGet, adapterPost, adapterPut, adapterDelete } from '@/utils/request';
import { mockGetOrgTree } from '@mock/org';

// 从原始 org.ts 导入类型
export type { OrgTreeNode, OrgVO, OrgQueryDTO, TenantConfig, OrgForm, UserOrgVO } from './org';

const API_BASE_PREFIX = '/api/org';

/**
 * 获取组织树
 */
export const getOrgTreeAdapter = () => {
  return adapterGet<{ code: number; message: string; data: any[] }>(
    `${API_BASE_PREFIX}/tree`,
    mockGetOrgTree
  );
};

/**
 * 添加组织
 */
export const addOrgAdapter = (data: any) => {
  return adapterPost(
    `${API_BASE_PREFIX}`,
    data,
    async () => {
      // Mock: 模拟添加成功
      return { code: 200, message: '添加成功' };
    }
  );
};

/**
 * 更新组织
 */
export const updateOrgAdapter = (data: any) => {
  return adapterPut(
    `${API_BASE_PREFIX}`,
    data,
    async () => {
      // Mock: 模拟更新成功
      return { code: 200, message: '更新成功' };
    }
  );
};

/**
 * 删除组织
 */
export const deleteOrgAdapter = (id: number) => {
  return adapterDelete(
    `${API_BASE_PREFIX}/${id}`,
    async () => {
      // Mock: 模拟删除成功
      return { code: 200, message: '删除成功' };
    }
  );
};

/**
 * 添加菜单权限
 */
export const addMenuPermissionsAdapter = (menuId: number, permissions: any) => {
  return adapterPost(
    `${API_BASE_PREFIX}/menu/permissions`,
    { menuId, permissions },
    async () => {
      return { code: 200, message: '添加成功' };
    }
  );
};

// ============================================
// 别名导出 - 保持与原 API 命名一致
// ============================================
// getOrgTree 需要提取 data 字段
export const getOrgTree = async () => {
  const response = await getOrgTreeAdapter();

  // 处理可能的多种格式
  if (Array.isArray(response)) {
    return response;
  }

  if (response && typeof response === 'object') {
    // 标准格式：{ code, message, data }
    if ('code' in response && 'data' in response) {
      if (response.code === 200 && Array.isArray(response.data)) {
        return response.data;
      }
    }

    // 简化格式：{ data }（可能缺少 code/message）
    if ('data' in response && Array.isArray(response.data)) {
      return response.data;
    }
  }

  throw new Error("返回的数据格式不正确，期望数组格式");
};
export const addOrg = addOrgAdapter;
export const updateOrg = updateOrgAdapter;
export const deleteOrg = deleteOrgAdapter;
export const addMenuPermissions = addMenuPermissionsAdapter;

/**
 * 获取组织列表
 */
export const getOrgList = async (data: any) => {
  const response = await adapterPost(
    `${API_BASE_PREFIX}/list`,
    data,
    async () => {
      return { code: 200, message: '获取成功', data: { records: [], total: 0 } };
    }
  );
  // 提取 data 字段
  if (response && typeof response === 'object' && 'data' in response) {
    return response.data;
  }
  return response;
};

/**
 * 获取组织详情
 */
export const getOrgById = async (id: number) => {
  const response = await adapterGet(
    `${API_BASE_PREFIX}/${id}`,
    async () => {
      return { code: 200, message: '获取成功', data: {} };
    }
  );
  // 提取 data 字段
  if (response && typeof response === 'object' && 'data' in response) {
    return response.data;
  }
  return response;
};

/**
 * 搜索组织
 */
export const searchOrgs = async (keyword: string) => {
  const response = await adapterGet(
    `${API_BASE_PREFIX}/search?keyword=${keyword}`,
    async () => {
      return { code: 200, message: '搜索成功', data: [] };
    }
  );
  // 提取 data 字段
  if (response && typeof response === 'object' && 'data' in response) {
    return response.data;
  }
  return response;
};

/**
 * 检查组织编码是否存在
 */
export const checkOrgCodeExists = async (orgCode: string, excludeId?: number) => {
  const response = await adapterGet(
    `${API_BASE_PREFIX}/check-code?orgCode=${orgCode}&excludeId=${excludeId || ''}`,
    async () => {
      return { code: 200, message: '检查成功', data: false };
    }
  );
  // 提取 data 字段
  if (response && typeof response === 'object' && 'data' in response) {
    return response.data;
  }
  return response;
};

/**
 * 添加用户到组织
 */
export const addUserToOrg = (data: { orgId: number; userId: number; isLeader?: number }) => {
  return adapterPost(
    `${API_BASE_PREFIX}/add-user`,
    data,
    async () => {
      return { code: 200, message: '添加成功' };
    }
  );
};

/**
 * 从组织移除用户
 */
export const removeUserFromOrg = (orgId: number, userId: number) => {
  return adapterDelete(
    `${API_BASE_PREFIX}/${orgId}/users/${userId}`,
    async () => {
      return { code: 200, message: '移除成功' };
    }
  );
};

/**
 * 设置用户为组织负责人
 */
export const setUserAsLeader = (orgId: number, userId: number, isLeader: number) => {
  return adapterPut(
    `${API_BASE_PREFIX}/${orgId}/users/${userId}/leader`,
    { isLeader },
    async () => {
      return { code: 200, message: '设置成功' };
    }
  );
};

/**
 * 获取用户的组织列表
 */
export const getUserOrgs = async (userId: number) => {
  const response = await adapterGet(
    `${API_BASE_PREFIX}/user-orgs/${userId}`,
    async () => {
      return { code: 200, message: '获取成功', data: [] };
    }
  );
  // 提取 data 字段
  if (response && typeof response === 'object' && 'data' in response) {
    return response.data;
  }
  // 如果直接返回数组，直接返回
  return response;
};
