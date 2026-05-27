import { http } from "@/utils/http";

export type UserResult = {
  code: number;
  message: string;
  data: {
    /** token */
    token: string;
    /** sessionId */
    sessionId: string;
    /** tokenName */
    tokenName: string;
    /** 用户ID */
    userId: number;
    /** 用户名 */
    username: string;
    /** 昵称 */
    nickname: string;
    /** 头像 */
    avatar: string;
    /** 租户ID */
    tenantId: number;
    /** 租户名称 */
    tenantName: string;
    /** 角色编码 */
    roleCode: string;
  };
};

export type RefreshTokenResult = {
  success: boolean;
  data: {
    /** `token` */
    accessToken: string;
    /** 用于调用刷新`accessToken`的接口时所需的`token` */
    refreshToken: string;
    /** `accessToken`的过期时间（格式'xxxx/xx/xx xx:xx:xx'） */
    expires: Date;
  };
};

/** 管理员登录 */
export const getAdminLogin = (data?: object) => {
  const requestData = {
    ...data,
    loginType: "admin"
  };
  return http.request<UserResult>("post", `/auth/admin/login`, { data: requestData });
};

/** 普通用户登录 */
export const getUserLogin = (data?: object) => {
  const requestData = {
    ...data,
    loginType: "user"
  };
  return http.request<UserResult>("post", `/auth/user/login`, { data: requestData });
};

/** 租户登录 */
export const getTenantLogin = (data?: object) => {
  const requestData = {
    ...data,
    loginType: "tenant"
  };
  return http.request<UserResult>("post", `/auth/tenant/login`, { data: requestData });
};

/** 统一登录接口 - 根据 loginType 调用不同接口 */
export const getLogin = (data?: any) => {
  const { loginType = "admin" } = data || {};

  switch (loginType) {
    case "user":
      return getUserLogin(data);
    case "tenant":
      return getTenantLogin(data);
    case "admin":
    default:
      return getAdminLogin(data);
  }
};

/** 刷新`token` */
export const refreshTokenApi = (data?: object) => {
  return http.request<RefreshTokenResult>("post", "/auth/refresh", { data });
};

/**
 * 创建用户
 */
export const createUser = async (data: any) => {
  const response = await http.request<{ code: number; message: string; data: number }>("post", `/user/create`, { data });
  // 检查响应码
  if (response.code !== 200) {
    throw new Error(response.message || "创建用户失败");
  }
  return response.data;
};

/**
 * 更新用户信息
 */
export const updateUser = async (data: any) => {
  const response = await http.request<{ code: number; message: string; data: void }>("put", `/user/update`, { data });
  // 检查响应码
  if (response.code !== 200) {
    throw new Error(response.message || "更新用户失败");
  }
  return response.data;
};

/**
 * 更新用户状态
 */
export const updateUserStatus = async (userId: number, status: number) => {
  const response = await http.request<{ code: number; message: string; data: void }>("put", `/user/status`, {
    params: { userId, status }
  });
  // 检查响应码
  if (response.code !== 200) {
    throw new Error(response.message || "更新用户状态失败");
  }
  return response.data;
};

/**
 * 获取用户列表
 */
export const getUserList = async (params?: any) => {
  return http.request<any>("get", `/user/list`, { params });
};
