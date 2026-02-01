import { http } from "@/utils/http";

/**
 * 用户日志 API
 */
export const getUserLogList = (params: any) => {
  return http.request("get", "/log/user/list", {
    params
  });
};

/**
 * 系统日志 API
 */
export const getSystemLogList = (params: any) => {
  return http.request("get", "/log/system/list", {
    params
  });
};

/**
 * 操作日志 API
 */
export const getOperationLogList = (params: any) => {
  return http.request("get", "/log/operation/list", {
    params
  });
};

/**
 * 获取操作日志详情
 */
export const getOperationLogDetail = (id: number) => {
  return http.request("get", `/log/operation/${id}`);
};

/**
 * 获取系统日志详情
 */
export const getSystemLogDetail = (id: number) => {
  return http.request("get", `/log/system/${id}`);
};
