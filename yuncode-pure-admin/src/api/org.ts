import { http } from "@/utils/http";

/**
 * 组织树节点
 */
export interface OrgTreeNode {
  id: number;
  nodeType: "org" | "user";
  label: string;
  orgCode?: string;
  parentId: number;
  orgType?: number;
  isCompany?: number;
  tenantId?: number; // 租户ID
  tenantCode?: string; // 租户编码
  userId?: number;
  username?: string;
  realName?: string; // 真实姓名
  nickname?: string;
  avatar?: string;
  email?: string; // 邮箱
  phone?: string; // 手机号
  gender?: number; // 性别
  status?: number; // 用户状态：0=正常，1=冻结（仅用户节点）
  isLeader?: number;
  sortOrder?: number;
  children?: OrgTreeNode[];
  path?: string;
}

/**
 * 组织视图对象
 */
export interface OrgVO {
  id: number;
  orgName: string;
  orgCode: string;
  parentId: number;
  orgType: number;
  isCompany: number;
  sortOrder: number;
  status: number;
  tenantId: number;
  remark?: string;
  createTime?: string;
  updateTime?: string;
  createBy?: string;
  updateBy?: string;
  userCount?: number;
  orgTypeName?: string;
  isCompanyName?: string;
  statusName?: string;
}

/**
 * 组织查询对象
 */
export interface OrgQueryDTO {
  orgName?: string;
  orgCode?: string;
  parentId?: number;
  orgType?: number;
  isCompany?: number;
  status?: number;
  keyword?: string;
}

/**
 * 租户配置
 */
export interface TenantConfig {
  tenantType?: number; // 租户类型：0试用 1标准 2高级 3企业
  userLimit?: number; // 用户数量限制
  storageLimit?: number; // 存储空间限制（MB）
  expireTime?: string; // 过期时间
  contactName?: string; // 联系人
  contactPhone?: string; // 联系电话
  contactEmail?: string; // 联系邮箱
  address?: string; // 企业地址
}

/**
 * 组织表单对象
 */
export interface OrgForm {
  id?: number;
  orgName: string;
  orgCode: string;
  tenantId?: number; // 租户ID（公司节点：由后端生成；部门节点：继承父节点）
  tenantCode?: string; // 租户编码（仅公司节点需要）
  parentId: number;
  orgType: number;
  isCompany: number;
  sortOrder: number;
  status: number;
  remark?: string;
  tenantConfig?: TenantConfig; // 租户配置（仅公司节点需要）
}

/**
 * 获取组织树（包含人员）
 */
export const getOrgTree = async () => {
  const response = await http.request<{ code: number; message: string; data: OrgTreeNode[] }>("get", "/org/tree");

  // 检查响应格式
  if (response && typeof response === 'object' && 'code' in response) {
    // 后端返回标准格式 {code, message, data}
    if (response.code === 200 && Array.isArray(response.data)) {
      return response.data;
    }
    throw new Error(response.message || "获取组织树失败");
  }

  // 如果直接返回数组，直接返回
  if (Array.isArray(response)) {
    return response;
  }

  throw new Error("返回数据格式不正确");
};

/**
 * 获取组织列表
 */
export const getOrgList = async (data: OrgQueryDTO) => {
  const response = await http.request<{ code: number; message: string; data: OrgVO[] }>("post", "/org/list", { data });
  return response.data;
};

/**
 * 获取组织详情
 */
export const getOrgById = async (id: number) => {
  const response = await http.request<{ code: number; message: string; data: OrgVO }>("get", `/org/${id}`);
  return response.data;
};

/**
 * 添加组织
 */
export const addOrg = async (data: OrgForm) => {
  const response = await http.request<{ code: number; message: string; data: void }>("post", "/org", { data });
  return response.data;
};

/**
 * 更新组织
 */
export const updateOrg = async (data: OrgForm) => {
  const response = await http.request<{ code: number; message: string; data: void }>("put", "/org", { data });
  return response.data;
};

/**
 * 删除组织
 */
export const deleteOrg = async (id: number) => {
  const response = await http.request<{ code: number; message: string; data: void }>("delete", `/org/${id}`);
  return response.data;
};

/**
 * 搜索组织
 */
export const searchOrgs = async (keyword: string) => {
  const response = await http.request<{ code: number; message: string; data: OrgVO[] }>("get", "/org/search", { params: { keyword } });
  return response.data;
};

/**
 * 检查组织编码是否存在
 */
export const checkOrgCodeExists = async (orgCode: string, excludeId?: number) => {
  const response = await http.request<{ code: number; message: string; data: boolean }>("get", "/org/check-code", {
    params: { orgCode, excludeId }
  });
  return response.data;
};

/**
 * 添加人员到组织
 */
export const addUserToOrg = async (data: {
  orgId: number;
  userId: number;
  isLeader?: number;
  isMainDept?: number;
}) => {
  const response = await http.request<{ code: number; message: string; data: void }>("post", "/org/add-user", {
    params: data
  });
  return response.data;
};

/**
 * 从组织移除人员
 */
export const removeUserFromOrg = async (orgId: number, userId: number) => {
  const response = await http.request<{ code: number; message: string; data: void }>("delete", "/org/remove-user", {
    params: { orgId, userId }
  });
  return response.data;
};

/**
 * 设置用户为主部门负责人
 */
export const setUserAsLeader = async (orgId: number, userId: number, isLeader: number) => {
  const response = await http.request<{ code: number; message: string; data: void }>("put", "/org/set-leader", {
    params: { orgId, userId, isLeader }
  });
  return response.data;
};

/**
 * 用户组织关系
 */
export interface UserOrgVO {
  id: number;
  userId: number;
  username?: string;
  nickname?: string;
  realName?: string;
  orgId: number;
  orgName: string;
  orgCode: string;
  orgType: number;
  isMainDept: number; // 0=兼职部门，1=主部门
  isLeader: number; // 0=否，1=是
  orgPath: string;
  createTime?: string;
}

/**
 * 获取用户的所有组织关系（包括主部门和兼职部门）
 */
export const getUserOrgs = async (userId: number) => {
  const response = await http.request<{ code: number; message: string; data: UserOrgVO[] }>("get", `/org/user-orgs/${userId}`);
  return response.data;
};
