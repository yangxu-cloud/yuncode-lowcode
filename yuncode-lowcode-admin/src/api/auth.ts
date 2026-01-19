import request from "@/utils/request";

/**
 * 管理员登录 API
 * 平台超级管理员登录，不需要租户编码
 */
export const adminLogin = (data: {
  username: string;
  password: string;
}) => {
  return request.post<LoginResponse>("/auth/admin/login", {
    ...data,
    loginType: "admin"
  });
};

/**
 * 用户登录 API
 * 普通用户登录，需要租户编码
 */
export const userLogin = (data: {
  tenantCode: string;
  username: string;
  password: string;
}) => {
  return request.post<LoginResponse>("/auth/user/login", {
    ...data,
    loginType: "user"
  });
};

/**
 * 租户登录 API
 * 租户管理员登录，需要租户编码
 */
export const tenantLogin = (data: {
  tenantCode: string;
  username: string;
  password: string;
}) => {
  return request.post<LoginResponse>("/auth/tenant/login", {
    ...data,
    loginType: "tenant"
  });
};

/**
 * 登录 API（兼容旧版本）
 * @deprecated 请使用 tenantLogin、adminLogin 或 userLogin
 */
export const login = (data: {
  tenantCode: string;
  username: string;
  password: string;
}) => {
  return request.post<LoginResponse>("/auth/login", data);
};

/**
 * 登出 API
 */
export const logout = () => {
  return request.post("/auth/logout");
};

/**
 * 获取当前用户信息 API
 */
export const getCurrentUserInfo = () => {
  return request.get("/auth/info");
};

/**
 * 检查登录状态 API
 */
export const checkLogin = () => {
  return request.get("/auth/checkLogin");
};

/**
 * 登录响应接口
 */
export interface LoginResponse {
  token: string;
  tokenName: string;
  userId: number;
  username: string;
  nickname: string;
  avatar: string;
  tenantId: number;
  tenantName: string;
}
