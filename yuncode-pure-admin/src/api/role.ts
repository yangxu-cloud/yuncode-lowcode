import { http } from "@/utils/http";

/**
 * 角色节点
 */
export interface RoleNode {
  id?: number;
  parentId?: number;
  label: string;
  roleName?: string;  // 角色名称（原始字段）
  roleType?: number;
  roleCode?: string;
  description?: string;
  sortOrder?: number;
  status?: number;
  children?: RoleNode[];
}

/**
 * 角色用户
 */
export interface RoleUser {
  userId: number;
  userName: string;
  realName: string;
}

/**
 * 角色部门
 */
export interface RoleDept {
  deptId: number;
  deptName: string;
}

/**
 * 角色权限
 */
export interface RolePermission {
  permissionId: number;
  permissionName: string;
  permissionCode: string;
}

/**
 * 角色详情
 */
export interface RoleDetail {
  id?: number;
  parentId?: number;
  categoryName?: string;
  roleName: string;
  roleCode: string;
  roleType?: number;
  description?: string;
  sortOrder?: number;
  status?: number;
  users?: RoleUser[];
  depts?: RoleDept[];
  permissions?: RolePermission[];
}

/**
 * 角色表单
 */
export interface RoleForm {
  id?: number;
  parentId?: number;
  roleName: string;
  roleCode: string;
  roleType: number;
  description?: string;
  sortOrder?: number;
  status?: number;
}

/**
 * 获取角色树
 */
export function getRoleTree() {
  return http.request<{ code: number; message: string; data: RoleNode[] }>(
    "get",
    `/system/role/tree`
  );
}

/**
 * 获取角色详情
 */
export function getRoleDetail(id: number) {
  return http.request<{ code: number; message: string; data: RoleDetail }>(
    "get",
    `/system/role/${id}`
  );
}

/**
 * 新增角色
 */
export function createRole(data: RoleForm) {
  return http.request<{ code: number; message: string; data: number }>(
    "post",
    `/system/role`,
    { data }
  );
}

/**
 * 编辑角色
 */
export function updateRole(id: number, data: RoleForm) {
  return http.request<{ code: number; message: string; data: void }>(
    "put",
    `/system/role/${id}`,
    { data }
  );
}

/**
 * 删除角色
 */
export function deleteRole(id: number) {
  return http.request<{ code: number; message: string; data: void }>(
    "delete",
    `/system/role/${id}`
  );
}

/**
 * 添加人员到角色
 */
export function addUsersToRole(roleId: number, userIds: number[]) {
  return http.request<{ code: number; message: string; data: void }>(
    "post",
    `/system/role/${roleId}/users`,
    { data: userIds }
  );
}

/**
 * 从角色移除人员
 */
export function removeUserFromRole(roleId: number, userId: number) {
  return http.request<{ code: number; message: string; data: void }>(
    "delete",
    `/system/role/${roleId}/users/${userId}`
  );
}

/**
 * 添加部门到角色
 */
export function addDeptsToRole(roleId: number, deptIds: number[]) {
  return http.request<{ code: number; message: string; data: void }>(
    "post",
    `/system/role/${roleId}/depts`,
    { data: deptIds }
  );
}

/**
 * 从角色移除部门
 */
export function removeDeptFromRole(roleId: number, deptId: number) {
  return http.request<{ code: number; message: string; data: void }>(
    "delete",
    `/system/role/${roleId}/depts/${deptId}`
  );
}

/**
 * 添加权限到角色
 */
export function addPermissionsToRole(roleId: number, permissionIds: number[]) {
  return http.request<{ code: number; message: string; data: void }>(
    "post",
    `/system/role/${roleId}/permissions`,
    { data: permissionIds }
  );
}

/**
 * 从角色移除权限
 */
export function removePermissionFromRole(roleId: number, permissionId: number) {
  return http.request<{ code: number; message: string; data: void }>(
    "delete",
    `/system/role/${roleId}/permissions/${permissionId}`
  );
}
