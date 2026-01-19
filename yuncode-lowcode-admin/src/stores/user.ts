import { defineStore } from "pinia";
import { ref, computed } from "vue";
import {
  login as loginApi,
  adminLogin as adminLoginApi,
  userLogin as userLoginApi,
  tenantLogin as tenantLoginApi,
  logout as logoutApi,
  getCurrentUserInfo,
  type LoginResponse
} from "@/api/auth";
import { ElMessage } from "element-plus";
import { logLogin, logLogout } from "@/utils/log";

/**
 * 用户信息接口
 */
export interface UserInfo {
  token: string;
  tokenName: string;
  userId: number;
  username: string;
  nickname: string;
  avatar: string;
  tenantId: number;
  tenantName: string;
  loginType?: string;  // 登录类型：admin, user, tenant
  roles?: string[];
  permissions?: string[];
}

/**
 * 用户登录参数
 */
export interface LoginParams {
  tenantCode?: string;
  username: string;
  password: string;
}

/**
 * 用户状态管理 Store
 */
export const useUserStore = defineStore("user", () => {
  // 状态定义
  const token = ref<string>("");
  const userInfo = ref<UserInfo | null>(null);

  // 计算属性
  const isLogin = computed(() => !!token.value);
  const username = computed(() => userInfo.value?.username || "");
  const nickname = computed(() => userInfo.value?.nickname || "");
  const avatar = computed(() => userInfo.value?.avatar || "");
  const tenantName = computed(() => userInfo.value?.tenantName || "");
  const loginType = computed(() => userInfo.value?.loginType || "");
  const roles = computed(() => userInfo.value?.roles || []);
  const permissions = computed(() => userInfo.value?.permissions || []);

  /**
   * 根据登录类型获取localStorage key
   */
  const getStorageKeys = (type: string = "") => {
    // 如果没有指定类型，尝试从当前用户信息获取
    if (!type && userInfo.value?.loginType) {
      type = userInfo.value.loginType;
    }

    // 如果还是没有类型，使用默认值
    if (!type) {
      type = "user";
    }

    return {
      token: `token_${type}`,
      userInfo: `userInfo_${type}`,
      loginType: `loginType_${type}`
    };
  };

  /**
   * 初始化用户信息（从本地存储恢复）
   * 根据指定的登录类型恢复，如果未指定则按优先级恢复
   */
  const initUserInfo = (preferredType: string = "") => {
    // 如果指定了类型，优先恢复该类型
    if (preferredType) {
      const keys = getStorageKeys(preferredType);
      const savedToken = localStorage.getItem(keys.token);
      const savedUserInfo = localStorage.getItem(keys.userInfo);

      if (savedToken && savedUserInfo) {
        try {
          token.value = savedToken;
          userInfo.value = JSON.parse(savedUserInfo);
          // ⚠️ 关键：保存到 sessionStorage，确保 request.ts 能正确获取用户类型
          sessionStorage.setItem("activeLoginType", preferredType);
          console.log(`[User Store] 从${preferredType}恢复用户信息:`, userInfo.value?.username);
          return;
        } catch (error) {
          console.error(`恢复${preferredType}用户信息失败:`, error);
          localStorage.removeItem(keys.token);
          localStorage.removeItem(keys.userInfo);
        }
      }
    }

    // 如果没有指定类型或指定类型恢复失败，按优先级尝试
    const types = ["admin", "tenant", "user"];

    for (const type of types) {
      // 如果已经指定了类型，就跳过
      if (preferredType && type !== preferredType) {
        continue;
      }

      const keys = getStorageKeys(type);
      const savedToken = localStorage.getItem(keys.token);
      const savedUserInfo = localStorage.getItem(keys.userInfo);

      if (savedToken && savedUserInfo) {
        try {
          token.value = savedToken;
          userInfo.value = JSON.parse(savedUserInfo);
          // ⚠️ 关键：保存到 sessionStorage，确保 request.ts 能正确获取用户类型
          sessionStorage.setItem("activeLoginType", type);
          console.log(`[User Store] 从${type}恢复用户信息:`, userInfo.value?.username);
          return; // 找到有效的登录信息后直接返回
        } catch (error) {
          console.error(`恢复${type}用户信息失败:`, error);
          // 清除损坏的数据
          localStorage.removeItem(keys.token);
          localStorage.removeItem(keys.userInfo);
        }
      }
    }
  };

  /**
   * 保存用户信息到本地存储
   */
  const saveUserInfo = (info: UserInfo) => {
    token.value = info.token;
    userInfo.value = info;

    // 根据登录类型使用不同的localStorage key
    const type = info.loginType || "user";
    const keys = getStorageKeys(type);

    localStorage.setItem(keys.token, info.token);
    localStorage.setItem(keys.userInfo, JSON.stringify(info));

    // 保存登录类型到 sessionStorage，实现标签页隔离
    if (info.loginType) {
      sessionStorage.setItem("loginType", info.loginType);
      sessionStorage.setItem("activeLoginType", info.loginType);
    }

    // 调试日志
    console.log('[User Store] 用户信息已保存:', {
      type: type,
      token: info.token.substring(0, 20) + '...',
      userId: info.userId,
      username: info.username,
      storageKeys: keys
    });
  };

  /**
   * 清除用户信息
   */
  const clearUserInfo = () => {
    // ⚠️ 关键修复：通过当前 token.value 匹配 localStorage 中的 token
    // 这样可以确保清除的是当前登录用户的正确数据
    let currentLoginType = "";

    // 方法1：通过 token.value 匹配（最可靠）
    if (token.value) {
      const types = ["admin", "tenant", "user"];
      for (const type of types) {
        const storedToken = localStorage.getItem(`token_${type}`);
        if (storedToken === token.value) {
          currentLoginType = type;
          console.log('[User Store] 通过 token 匹配到用户类型:', type);
          break;
        }
      }
    }

    // 方法2：如果 token 匹配失败，使用 sessionStorage 中的 loginType
    if (!currentLoginType) {
      currentLoginType = sessionStorage.getItem("loginType") || "";
      if (currentLoginType) {
        console.log('[User Store] 通过 sessionStorage 获取用户类型:', currentLoginType);
      }
    }

    // 方法3：最后回退到 userInfo.value
    if (!currentLoginType) {
      currentLoginType = userInfo.value?.loginType || "";
      if (currentLoginType) {
        console.log('[User Store] 通过 userInfo 获取用户类型:', currentLoginType);
      }
    }

    // 清除对应类型的 localStorage 数据
    if (currentLoginType) {
      const keys = getStorageKeys(currentLoginType);
      localStorage.removeItem(keys.token);
      localStorage.removeItem(keys.userInfo);
      // 注意：loginType 已经改用 sessionStorage 存储，不需要清除 localStorage 中的 keys.loginType

      console.log('[User Store] 清除用户信息:', {
        loginType: currentLoginType,
        token: token.value?.substring(0, 20) + '...'
      });
    }

    // ⚠️ 关键：先保存 currentLoginType 到 sessionStorage，供路由守卫使用
    // 必须在清除 loginType 之前保存
    if (currentLoginType) {
      sessionStorage.setItem("lastLoginType", currentLoginType);
      console.log('[User Store] 保存最后登录类型到 sessionStorage:', currentLoginType);
    }

    // 清除 sessionStorage 中的登录类型信息
    sessionStorage.removeItem("loginType");
    sessionStorage.removeItem("activeLoginType");

    // ⚠️ 重要：设置标记，表示当前标签页已经主动退出
    // 防止路由守卫再恢复其他用户类型
    sessionStorage.setItem("logout", "true");

    token.value = "";
    userInfo.value = null;
  };

  /**
   * 清除所有登录类型的信息
   */
  const clearAllUserInfo = () => {
    // 清除所有类型的登录信息
    const types = ["admin", "tenant", "user"];
    types.forEach(type => {
      const keys = getStorageKeys(type);
      localStorage.removeItem(keys.token);
      localStorage.removeItem(keys.userInfo);
      // 注意：loginType 已改用 sessionStorage，不需要清除 keys.loginType
    });

    // 同时清除旧版本的key（兼容性）
    localStorage.removeItem("token");
    localStorage.removeItem("userInfo");
    localStorage.removeItem("loginType");

    // 清除 sessionStorage
    sessionStorage.removeItem("loginType");
    sessionStorage.removeItem("activeLoginType");

    token.value = "";
    userInfo.value = null;

    console.log('[User Store] 已清除所有登录信息');
  };

  /**
   * 管理员登录
   * 平台超级管理员登录，不需要租户编码
   */
  const adminLogin = async (params: Omit<LoginParams, 'tenantCode'>): Promise<void> => {
    try {
      const response = await adminLoginApi(params);
      response.loginType = "admin";  // 设置登录类型
      saveUserInfo(response);
    } catch (error: any) {
      console.error("管理员登录失败:", error);
      throw error;
    }
  };

  /**
   * 普通用户登录
   * 需要租户编码
   */
  const userLogin = async (params: LoginParams): Promise<void> => {
    try {
      const response = await userLoginApi(params);
      response.loginType = "user";  // 设置登录类型
      saveUserInfo(response);
    } catch (error: any) {
      console.error("用户登录失败:", error);
      throw error;
    }
  };

  /**
   * 租户登录
   * 租户管理员登录，需要租户编码
   */
  const tenantLogin = async (params: LoginParams): Promise<void> => {
    try {
      const response = await tenantLoginApi(params);
      response.loginType = "tenant";  // 设置登录类型
      saveUserInfo(response);
    } catch (error: any) {
      console.error("租户登录失败:", error);
      throw error;
    }
  };

  /**
   * 用户登录（兼容旧版本）
   * @deprecated 请使用 tenantLogin、adminLogin 或 userLogin
   */
  const login = async (params: LoginParams): Promise<void> => {
    try {
      const response = await loginApi(params);
      saveUserInfo(response);
    } catch (error: any) {
      console.error("登录失败:", error);
      throw error;
    }
  };

  /**
   * 用户登出
   */
  const logout = async (): Promise<void> => {
    try {
      await logoutApi();
    } catch (error) {
      console.error("登出API调用失败:", error);
    } finally {
      clearUserInfo();
      ElMessage.success("退出成功");
    }
  };

  /**
   * 获取当前用户信息
   */
  const fetchUserInfo = async (): Promise<void> => {
    try {
      const response = await getCurrentUserInfo();
      userInfo.value = {
        ...response,
        token: token.value
      };
      // 根据当前登录类型保存到对应的localStorage key
      if (userInfo.value?.loginType) {
        const keys = getStorageKeys(userInfo.value.loginType);
        localStorage.setItem(keys.userInfo, JSON.stringify(userInfo.value));
      }
    } catch (error) {
      console.error("获取用户信息失败:", error);
      throw error;
    }
  };

  /**
   * 检查是否有指定权限
   */
  const hasPermission = (permission: string): boolean => {
    return permissions.value.includes(permission);
  };

  /**
   * 检查是否有指定角色
   */
  const hasRole = (role: string): boolean => {
    return roles.value.includes(role);
  };

  /**
   * 检查是否是管理员
   */
  const isAdmin = computed(() => {
    return roles.value.includes("admin") || roles.value.includes("super_admin");
  });

  return {
    // 状态
    token,
    userInfo,
    // 计算属性
    isLogin,
    username,
    nickname,
    avatar,
    tenantName,
    loginType,
    roles,
    permissions,
    isAdmin,
    // 方法
    initUserInfo,
    login,
    adminLogin,
    userLogin,
    tenantLogin,
    logout,
    fetchUserInfo,
    hasPermission,
    hasRole,
    clearAllUserInfo
  };
});
