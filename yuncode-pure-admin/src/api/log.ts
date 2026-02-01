import { http } from "@/utils/http";

/**
 * 登录日志接口
 */
export interface LoginLog {
  id?: number;
  userId: number;
  username: string;
  tenantId: number;
  tenantName: string;
  loginTime: string;
  logoutTime?: string;
  ip: string;
  location?: string;
  userAgent?: string;
  status: "success" | "failed";
  failReason?: string;
}

/**
 * 记录登录日志
 */
export const recordLoginLog = (data: Omit<LoginLog, "id">) => {
  return http.request("post", "/log/login", {
    data
  });
};

/**
 * 获取登录日志列表
 */
export const getLoginLogs = (params: {
  page: number;
  size: number;
  userId?: number;
  username?: string;
  startDate?: string;
  endDate?: string;
}) => {
  return http.request<{
    records: LoginLog[];
    total: number;
    current: number;
    size: number;
  }>("get", "/log/login/list", {
    params
  });
};

/**
 * 记录登出日志
 */
export const recordLogoutLog = (data: {
  userId: number;
  username: string;
  logoutTime: string;
}) => {
  return http.request("post", "/log/logout", {
    data
  });
};
