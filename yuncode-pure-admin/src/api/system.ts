import { http } from "@/utils/http";

export interface OnlineUser {
  sessionId: string;
  username: string;
  nickname: string;
  tenantName: string;
  ip: string;
  location: string;
  status: "active" | "idle";
  loginTime: string;
  lastAccessTime: string;
}

export interface OnlineUserStats {
  total: number;
  active: number;
  idle: number;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
}

/** 获取在线用户列表 */
export const getOnlineUsers = async (params?: any) => {
  const response = await http.request<{ code: number; message: string; data: PageResult<OnlineUser> }>(
    "get",
    `/system/online-users`,
    { params }
  );
  return response.data;
};

/** 获取在线用户统计 */
export const getOnlineUserStats = async () => {
  const response = await http.request<{ code: number; message: string; data: OnlineUserStats }>(
    "get",
    `/system/online-users/stats`
  );
  return response.data;
};

/** 踢出用户 */
export const kickOutUser = (sessionId: string) => {
  return http.request<void>(
    "post",
    `/system/online-users/${sessionId}/kick`
  );
};

/** 批量踢出用户 */
export const batchKickOutUsers = (sessionIds: string[]) => {
  return http.request<void>(
    "post",
    `/system/online-users/batch-kick`,
    { data: sessionIds }
  );
};
