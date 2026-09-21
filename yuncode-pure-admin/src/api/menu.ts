import { http } from "@/utils/http";


/**
 * 菜单树节点
 */
export interface MenuTreeNode {
  id?: number;
  menuName: string;
  icon?: string;
  parentId: number;
  menuType: number;
  path?: string;
  component?: string;
  sortOrder?: number;
  visible?: number;
  status?: number;
  tenantId?: number;
  tenantCode?: string;
  children?: MenuTreeNode[];
}

/**
 * 菜单表单
 */
export interface MenuForm {
  id?: number;
  menuName: string;
  icon?: string;
  parentId: number;
  menuType: number;
  path?: string;
  component?: string;
  permission?: string;
  sortOrder?: number;
  visible?: number;
  status?: number;
  tenantId?: number;
  tenantCode?: string;
}

/**
 * 菜单权限VO
 */
export interface MenuPermissionVO {
  id?: number;
  menuId?: number;
  targetType: number;
  targetId: number;
  targetName?: string;
  targetTypeName?: string;
}

/**
 * 获取菜单树
 */
export const getMenuTree = () => {
  return http.request<{ code: number; message: string; data: MenuTreeNode[] }>(
    "get",
    `/menu/tree`
  );
};

/**
 * 获取用户菜单树
 */
export const getUserMenuTree = () => {
  return http.request<{ code: number; message: string; data: MenuTreeNode[] }>(
    "get",
    `/menu/user/tree`
  );
};

/**
 * 获取菜单详情
 */
export const getMenuById = (id: number) => {
  return http.request<{ code: number; message: string; data: any }>(
    "get",
    `/menu/${id}`
  );
};

/**
 * 添加菜单
 */
export const addMenu = async (data: MenuForm) => {
  const response = await http.request<{ code: number; message: string; data: void }>(
    "post",
    `/menu`,
    { data }
  );
  
  return response.data;
};

/**
 * 更新菜单
 */
export const updateMenu = async (data: MenuForm) => {
  const response = await http.request<{ code: number; message: string; data: void }>(
    "put",
    `/menu`,
    { data }
  );
  
  return response.data;
};

/**
 * 删除菜单
 */
export const deleteMenu = async (id: number) => {
  const response = await http.request<{ code: number; message: number; data: void }>(
    "delete",
    `/menu/${id}`
  );
  
  return response.data;
};

/**
 * 上移菜单
 */
export const moveUpMenu = async (menuId: number) => {
  const response = await http.request<{ code: number; message: string; data: void }>(
    "put",
    `/menu/move-up/${menuId}`
  );
  
  return response.data;
};

/**
 * 下移菜单
 */
export const moveDownMenu = async (menuId: number) => {
  const response = await http.request<{ code: number; message: string; data: void }>(
    "put",
    `/menu/move-down/${menuId}`
  );
  
  return response.data;
};

/**
 * 设置菜单可见性
 */
export const setMenuVisible = async (menuId: number, visible: number) => {
  const response = await http.request<{ code: number; message: string; data: void }>(
    "put",
    `/menu/visible`,
    {
      params: { menuId, visible }
    }
  );
  
  return response.data;
};

/**
 * 搜索菜单
 */
export const searchMenus = (keyword: string) => {
  return http.request<{ code: number; message: string; data: any[] }>(
    "get",
    `/menu/search`,
    {
      params: { keyword }
    }
  );
};

/**
 * 获取菜单权限列表
 */
export const getMenuPermissions = (menuId: number) => {
  return http.request<{ code: number; message: string; data: MenuPermissionVO[] }>(
    "get",
    `/menu/permissions/${menuId}`
  );
};

/**
 * 添加菜单权限
 */
export const addMenuPermissions = async (menuId: number, targetType: number, targetIds: number[]) => {
  const response = await http.request<{ code: number; message: string; data: void }>(
    "post",
    `/menu/permissions`,
    {
      params: { menuId, targetType, targetIds }
    }
  );
  
  return response.data;
};

/**
 * 移除菜单权限
 */
export const removeMenuPermission = async (menuId: number, targetType: number, targetId: number) => {
  const response = await http.request<{ code: number; message: string; data: void }>(
    "delete",
    `/menu/permissions`,
    {
      params: { menuId, targetType, targetId }
    }
  );
  
  return response.data;
};

/**
 * 权限追加到下级
 */
export const copyPermissionsToChildren = async (menuId: number) => {
  const response = await http.request<{ code: number; message: string; data: number }>(
    "post",
    `/menu/permissions/copy-to-children/${menuId}`
  );
  
  return response.data;
};

/**
 * 初始化默认菜单
 */
export const initDefaultMenus = async () => {
  const response = await http.request<{ code: number; message: string; data: void }>(
    "post",
    `/menu/init`
  );
  
  return response.data;
};
