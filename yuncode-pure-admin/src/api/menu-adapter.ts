/**
 * 菜单 API - 适配器版本
 * 支持 Mock 数据源切换
 */

import { adapterGet, adapterPost, adapterPut, adapterDelete } from '@/utils/request';
import { mockGetMenuTree, mockGetMenuPermissions, type MenuTreeItem } from '@mock/menu';
import type { MenuPermissionVO } from './menu';

const API_BASE_PREFIX = '/menu';

/**
 * 获取菜单树
 */
export const getMenuTreeAdapter = () => {
  return adapterGet<{ code: number; message: string; data: MenuTreeItem[] }>(
    `${API_BASE_PREFIX}/tree`,
    mockGetMenuTree
  );
};

/**
 * 获取菜单权限列表
 */
export const getMenuPermissionsAdapter = (menuId: number) => {
  return adapterGet<{ code: number; message: string; data: MenuPermissionVO[] }>(
    `${API_BASE_PREFIX}/permissions/${menuId}`,
    () => mockGetMenuPermissions(menuId)
  );
};

/**
 * 添加菜单
 */
export const addMenuAdapter = (data: any) => {
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
 * 更新菜单
 */
export const updateMenuAdapter = (data: any) => {
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
 * 删除菜单
 */
export const deleteMenuAdapter = (id: number) => {
  return adapterDelete(
    `${API_BASE_PREFIX}/${id}`,
    async () => {
      // Mock: 模拟删除成功
      return { code: 200, message: '删除成功' };
    }
  );
};

// ============================================
// 别名导出 - 保持与原 API 命名一致
// ============================================
export const getMenuTree = getMenuTreeAdapter;
export const addMenu = addMenuAdapter;
export const updateMenu = updateMenuAdapter;
export const deleteMenu = deleteMenuAdapter;
export const getMenuPermissions = getMenuPermissionsAdapter;

/**
 * 菜单上移
 */
export const moveUpMenu = (id: number) => {
  return adapterPut(
    `${API_BASE_PREFIX}/move-up/${id}`,
    {},
    async () => {
      return { code: 200, message: '上移成功' };
    }
  );
};

/**
 * 菜单下移
 */
export const moveDownMenu = (id: number) => {
  return adapterPut(
    `${API_BASE_PREFIX}/move-down/${id}`,
    {},
    async () => {
      return { code: 200, message: '下移成功' };
    }
  );
};

/**
 * 设置菜单可见性
 */
export const setMenuVisible = (id: number, visible: number) => {
  return adapterPut(
    `${API_BASE_PREFIX}/visible`,
    { menuId: id, visible },
    async () => {
      return { code: 200, message: visible ? '显示成功' : '隐藏成功' };
    }
  );
};

/**
 * 移除菜单权限
 */
export const removeMenuPermission = (menuId: number, targetType: number, targetId: number) => {
  return adapterDelete(
    `${API_BASE_PREFIX}/permissions`,
    async () => {
      return { code: 200, message: '移除成功' };
    },
    { params: { menuId, targetType, targetId } }
  );
};

/**
 * 复制权限到子菜单
 */
export const copyPermissionsToChildren = (menuId: number) => {
  return adapterPost(
    `${API_BASE_PREFIX}/permissions/copy-to-children/${menuId}`,
    {},
    async () => {
      return { code: 200, message: '复制成功' };
    }
  );
};

/**
 * 添加菜单权限
 */
export const addMenuPermissionsAdapter = (menuId: number, targetType: number, targetIds: number[]) => {
  return adapterPost(
    `${API_BASE_PREFIX}/permissions`,
    { menuId, targetType, targetIds },
    async () => {
      return { code: 200, message: '添加成功' };
    }
  );
};
