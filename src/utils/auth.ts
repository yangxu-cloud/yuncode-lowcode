import Cookies from "js-cookie";
import { useUserStoreHook } from "@/store/modules/user";
import { storageLocal, isString, isIncludeAllChildren } from "@pureadmin/utils";

export interface DataInfo<T> {
  /** token (后端 Sa-Token 生成的 JWT Token，用于 API 认证) */
  accessToken: string;
  /** sessionId (业务会话标识，UUID 格式，用于前端 Cookie 和 SSE 连接) */
  sessionId: string;
  /** `accessToken`的过期时间（时间戳） */
  expires: T;
  /** 用于调用刷新accessToken的接口时所需的token */
  refreshToken: string;
  /** 用户ID */
  userId?: number;
  /** 租户ID */
  tenantId?: number;
  /** 角色编码 */
  roleCode?: string;
  /** 头像 */
  avatar?: string;
  /** 用户名 */
  username?: string;
  /** 昵称 */
  nickname?: string;
  /** 当前登录用户的角色 */
  roles?: Array<string>;
  /** 当前登录用户的按钮级别权限 */
  permissions?: Array<string>;
  /** 登录类型（用于区分不同登录方式：admin/user/tenant） */
  loginType?: string;
}

// ============= 基于 sessionId 的会话隔离存储 =============
// Cookie key 使用后端返回的 sessionId，确保每个会话都有独立的存储
// sessionStorage 存储当前会话的用户信息、token 和 sessionId
//
// 设计原理：
// 1. 后端生成 sessionId（UUID），作为业务会话的唯一标识
// 2. sessionId 作为 Redis key：online_user:{sessionId}
// 3. Cookie key 使用 sessionId：session-{sessionId}
// 4. Sa-Token 的 JWT token 用于 API 认证
// 5. 支持同一账号多端登录（不同设备有不同 sessionId）
// 6. 踢出时后端通过 sessionId 精确踢出特定会话
//
// 优点：
// - sessionId 短且唯一（32字符），无特殊字符
// - 完全解耦，每个会话独立
// - 与后端 Redis 会话机制完美匹配
// - 支持多用户、多设备同时登录

/** Storage keys */
export const USER_INFO_KEY = "userInfo";
export const CURRENT_TOKEN_KEY = "current-token";  // 存储 Sa-Token JWT
export const CURRENT_SESSION_KEY = "current-sessionId";  // 存储业务会话ID
export const MULTIPLE_TABS_KEY = "multiple-tabs";

/**
 * 获取 Cookie key（根据后端返回的 sessionId）
 * 使用 sessionId 作为 key，短且唯一
 */
export function getSessionKey(sessionId: string): string {
  return `session-${sessionId}`;
}

/**
 * 设置 token 和用户信息
 */
export function setToken(data: DataInfo<Date>) {
  const token = data.accessToken;
  const sessionId = data.sessionId;
  const userId = data.userId;
  const tenantId = data.tenantId || 0;
  const loginType = data.loginType || "admin";

  console.log("[Auth] 设置 Token，sessionId:", sessionId, "token (前32位):", token.substring(0, 32), "userId:", userId, "tenantId:", tenantId, "loginType:", loginType);

  let expires = 0;
  const { refreshToken } = data;
  const { isRemembered, loginDay } = useUserStoreHook();
  expires = new Date(data.expires).getTime();

  const cookieString = JSON.stringify({
    accessToken: token,
    sessionId: sessionId,  // 添加 sessionId 到 Cookie
    expires,
    refreshToken,
    userId,
    tenantId
  });
  const sessionKey = getSessionKey(sessionId);

  // Token 和 sessionId 存到 Cookie（按 sessionId 区分 key，确保每个会话独立）
  expires > 0
    ? Cookies.set(sessionKey, cookieString, {
        expires: (expires - Date.now()) / 86400000
      })
    : Cookies.set(sessionKey, cookieString);

  // multiple-tabs 用于判断是否有用户登录（跨标签页共享）
  Cookies.set(
    MULTIPLE_TABS_KEY,
    "true",
    isRemembered
      ? {
          expires: loginDay
        }
      : {}
  );

  // 保存当前 token 和 sessionId 到 sessionStorage（用于获取 Cookie key）
  sessionStorage.setItem(CURRENT_TOKEN_KEY, token);
  sessionStorage.setItem(CURRENT_SESSION_KEY, sessionId);

  // 用户信息存到 sessionStorage（标签页隔离）
  const userInfo = {
    userId,
    tenantId,
    roleCode: data?.roleCode ?? "NORMAL",
    refreshToken,
    expires,
    avatar: data?.avatar ?? "",
    username: data?.username ?? "",
    nickname: data?.nickname ?? "",
    roles: data?.roles ?? [],
    permissions: data?.permissions ?? [],
    loginType,
    sessionId
  };

  sessionStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo));

  // 更新 store
  useUserStoreHook().SET_AVATAR(userInfo.avatar);
  useUserStoreHook().SET_USERNAME(userInfo.username);
  useUserStoreHook().SET_NICKNAME(userInfo.nickname);
  useUserStoreHook().SET_ROLES(userInfo.roles);
  useUserStoreHook().SET_PERMS(userInfo.permissions);
  useUserStoreHook().SET_USER_ID(userInfo.userId || 0);
  useUserStoreHook().SET_TENANT_ID(userInfo.tenantId || 0);
  useUserStoreHook().SET_ROLE_CODE(userInfo.roleCode);
}

/**
 * 获取 token
 */
export function getToken(): DataInfo<number> {
  // 从 sessionStorage 获取当前 token
  const token = sessionStorage.getItem(CURRENT_TOKEN_KEY);
  const sessionId = sessionStorage.getItem(CURRENT_SESSION_KEY);
  if (token && sessionId) {
    const sessionKey = getSessionKey(sessionId);

    // 优先从 Cookie 获取
    const cookieData = Cookies.get(sessionKey);
    if (cookieData) {
      return JSON.parse(cookieData);
    }

    // 其次从 sessionStorage 获取用户信息
    const userInfoStr = sessionStorage.getItem(USER_INFO_KEY);
    if (userInfoStr) {
      const userInfo = JSON.parse(userInfoStr);
      // 补充 accessToken
      userInfo.accessToken = token;
      return userInfo;
    }
  }

  return null;
}

/**
 * 获取 sessionId
 */
export function getSessionId(): string | null {
  return sessionStorage.getItem(CURRENT_SESSION_KEY);
}

/**
 * 获取用户信息
 */
export function getUserInfo(): DataInfo<number> {
  const userInfoStr = sessionStorage.getItem(USER_INFO_KEY);
  if (userInfoStr) {
    return JSON.parse(userInfoStr);
  }
  return null;
}

/**
 * 删除 token 和用户信息
 */
export function removeToken() {
  const sessionId = sessionStorage.getItem(CURRENT_SESSION_KEY);
  const token = sessionStorage.getItem(CURRENT_TOKEN_KEY);
  if (sessionId && token) {
    const sessionKey = getSessionKey(sessionId);

    // 只删除当前会话的 Cookie
    Cookies.remove(sessionKey);

    console.log(`[Auth] removeToken() - 清除 sessionId: ${sessionId}, token (前32位): ${token.substring(0, 32)}`);
  }

  // ⚠️ 不要清除 MULTIPLE_TABS_KEY！
  // 原因：MULTIPLE_TABS_KEY 是跨标签页共享的，用于判断"是否有用户登录"
  // 如果清除它，会影响其他标签页的登录状态
  // 例如：admin 和 user 同时在不同标签页登录，user 被踢出时不应该影响 admin

  sessionStorage.removeItem(USER_INFO_KEY);
  sessionStorage.removeItem(CURRENT_TOKEN_KEY);
  sessionStorage.removeItem(CURRENT_SESSION_KEY);
}

/**
 * 格式化token（jwt格式）
 */
export const formatToken = (token: string): string => {
  return "Bearer " + token;
};

/**
 * 是否有按钮级别的权限
 */
export const hasPerms = (value: string | Array<string>): boolean => {
  if (!value) return false;
  const allPerms = "*:*:*";
  const { permissions } = useUserStoreHook();
  if (!permissions) return false;
  if (permissions.length === 1 && permissions[0] === allPerms) return true;
  const isAuths = isString(value)
    ? permissions.includes(value)
    : isIncludeAllChildren(value, permissions);
  return isAuths ? true : false;
};
