import { http } from "@/utils/http";

/**
 * 日志级别
 */
export type LogLevel = "TRACE" | "DEBUG" | "INFO" | "WARN" | "ERROR";

/**
 * 日志类型
 */
export type LogType = "system" | "operation" | "user";

/**
 * 操作日志
 */
export interface OperationLog {
  id: string;
  tenantId: number;
  tenantName: string;
  userId: number;
  username: string;
  module: string;
  operation: string;
  method: string;
  params: string;
  ip: string;
  location: string;
  userAgent: string;
  executeTime: number; // 执行时长(ms)
  status: "success" | "failed";
  errorMsg?: string;
  traceId: string; // 链路追踪ID
  createdAt: string;
}

/**
 * 系统日志
 */
export interface SystemLog {
  id: string;
  tenantId: number;
  level: LogLevel;
  module: string;
  message: string;
  exception?: string;
  stackTrace?: string;
  traceId: string; // 链路追踪ID
  spanId: string; // Span ID
  parentSpanId?: string; // 父 Span ID
  tags?: Record<string, string>; // 自定义标签
  createdAt: string;
}

/**
 * 用户登录日志
 */
export interface UserLog {
  id: string;
  tenantId: number;
  tenantName: string;
  userId: number;
  username: string;
  loginTime: string;
  logoutTime?: string;
  ip: string;
  location: string;
  userAgent: string;
  status: "success" | "failed";
  failReason?: string;
  traceId: string;
  createdAt: string;
}

/**
 * 在线用户
 */
export interface OnlineUser {
  sessionId: string;
  tenantId: number;
  tenantName: string;
  userId: number;
  username: string;
  nickname: string;
  avatar: string;
  ip: string;
  location: string;
  userAgent: string;
  loginTime: string;
  lastAccessTime: string;
  status: "active" | "idle";
}

/**
 * 获取操作日志列表
 */
export const getOperationLogs = (params: {
  page: number;
  size: number;
  tenantId?: number;
  username?: string;
  module?: string;
  operation?: string;
  status?: string;
  startTime?: string;
  endTime?: string;
  traceId?: string;
}) => {
  return http.request<{
    records: OperationLog[];
    total: number;
    current: number;
    size: number;
  }>("get", "/log/operation/list", {
    params
  });
};

/**
 * 获取系统日志列表
 */
export const getSystemLogs = (params: {
  page: number;
  size: number;
  level?: LogLevel;
  module?: string;
  message?: string;
  startTime?: string;
  endTime?: string;
  traceId?: string;
}) => {
  return http.request<{
    records: SystemLog[];
    total: number;
    current: number;
    size: number;
  }>("get", "/log/system/list", {
    params
  });
};

/**
 * 获取用户日志列表
 */
export const getUserLogs = (params: {
  page: number;
  size: number;
  tenantId?: number;
  username?: string;
  status?: string;
  startTime?: string;
  endTime?: string;
}) => {
  return http.request<{
    records: UserLog[];
    total: number;
    current: number;
    size: number;
  }>("get", "/log/user/list", {
    params
  });
};

/**
 * 根据链路追踪ID获取相关日志
 */
export const getLogsByTraceId = (traceId: string) => {
  return http.request<{
    systemLogs: SystemLog[];
    operationLogs: OperationLog[];
    userLogs: UserLog[];
  }>("get", `/log/trace/${traceId}`);
};

/**
 * 获取在线用户列表
 */
export const getOnlineUsers = (params: {
  page: number;
  size: number;
  username?: string;
  tenantId?: number;
}) => {
  return http.request<{
    records: OnlineUser[];
    total: number;
    current: number;
    size: number;
  }>("get", "/system/online-users", {
    params
  });
};

/**
 * 踢出在线用户
 */
export const kickOutUser = (sessionId: string) => {
  return http.request("post", `/system/online-users/${sessionId}/kick`);
};

/**
 * 批量踢出用户
 */
export const batchKickOutUsers = (sessionIds: string[]) => {
  return http.request("post", "/system/online-users/batch-kick", {
    data: { sessionIds }
  });
};

/**
 * 获取在线用户统计
 */
export const getOnlineUserStats = () => {
  return http.request<{
    total: number;
    active: number;
    idle: number;
  }>("get", "/system/online-users/stats");
};

/**
 * 删除操作日志
 */
export const deleteOperationLog = (id: string) => {
  return http.request("delete", `/log/operation/${id}`);
};

/**
 * 批量删除操作日志
 */
export const batchDeleteOperationLogs = (ids: string[]) => {
  return http.request("post", "/log/operation/batch-delete", {
    data: { ids }
  });
};

/**
 * 删除系统日志
 */
export const deleteSystemLog = (id: string) => {
  return http.request("delete", `/log/system/${id}`);
};

/**
 * 批量删除系统日志
 */
export const batchDeleteSystemLogs = (ids: string[]) => {
  return http.request("post", "/log/system/batch-delete", {
    data: { ids }
  });
};

/**
 * 清空过期日志
 */
export const cleanExpiredLogs = (days: number) => {
  return http.request("post", "/log/clean", {
    data: { days }
  });
};
