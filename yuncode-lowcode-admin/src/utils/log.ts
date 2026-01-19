import { recordLoginLog, recordLogoutLog, type LoginLog } from "@/api/log";
import config from "@/config";

/**
 * 获取客户端 IP 地址（通过第三方服务）
 * 注意：生产环境建议使用后端获取 IP
 */
export const getClientIP = async (): Promise<string> => {
  try {
    const response = await fetch(config.ipQueryApi);
    const data = await response.json();
    return data.ip || "未知";
  } catch {
    return "未知";
  }
};

/**
 * 获取浏览器 User-Agent
 */
export const getUserAgent = (): string => {
  return navigator.userAgent;
};

/**
 * 获取地理位置（基于 IP）
 */
export const getLocation = async (): Promise<string> => {
  try {
    const response = await fetch(config.ipLocationApi);
    const data = await response.json();
    return `${data.country || ""} ${data.region || ""} ${data.city || ""}`.trim() || "未知";
  } catch {
    return "未知";
  }
};

/**
 * 记录登录日志
 */
export const logLogin = async (userInfo: {
  userId: number;
  username: string;
  tenantId: number;
  tenantName: string;
}, status: "success" | "failed" = "success", failReason?: string) => {
  try {
    const ip = await getClientIP();
    const location = await getLocation();
    const userAgent = getUserAgent();

    const logData: Omit<LoginLog, "id"> = {
      userId: userInfo.userId,
      username: userInfo.username,
      tenantId: userInfo.tenantId,
      tenantName: userInfo.tenantName,
      loginTime: new Date().toISOString(),
      ip,
      location,
      userAgent,
      status,
      failReason
    };

    await recordLoginLog(logData);
  } catch (error) {
    console.error("记录登录日志失败:", error);
    // 日志记录失败不影响登录流程
  }
};

/**
 * 记录登出日志
 */
export const logLogout = async (userInfo: {
  userId: number;
  username: string;
}) => {
  try {
    const logData = {
      userId: userInfo.userId,
      username: userInfo.username,
      logoutTime: new Date().toISOString()
    };

    await recordLogoutLog(logData);
  } catch (error) {
    console.error("记录登出日志失败:", error);
    // 日志记录失败不影响登出流程
  }
};
