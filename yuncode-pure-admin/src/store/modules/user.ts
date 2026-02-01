import { defineStore } from "pinia";
import {
  type userType,
  store,
  router,
  resetRouter,
  routerArrays
} from "../utils";
import {
  type UserResult,
  type RefreshTokenResult,
  getLogin,
  refreshTokenApi
} from "@/api/user";
import { useMultiTagsStoreHook } from "./multiTags";
import {
  type DataInfo,
  setToken,
  removeToken,
  getUserInfo
} from "@/utils/auth";
import { logout as logoutApi } from "@/api/auth";

export const useUserStore = defineStore("pure-user", {
  state: (): userType => {
    // 从 sessionStorage 获取用户信息
    const userInfo = getUserInfo();

    return {
      // 头像
      avatar: userInfo?.avatar ?? "",
      // 用户名
      username: userInfo?.username ?? "",
      // 昵称
      nickname: userInfo?.nickname ?? "",
      // 页面级别权限
      roles: userInfo?.roles ?? [],
      // 按钮级别权限
      permissions: userInfo?.permissions ?? [],
      // 用户ID
      userId: userInfo?.userId ?? 0,
      // 租户ID
      tenantId: userInfo?.tenantId ?? 0,
      // 角色编码
      roleCode: userInfo?.roleCode ?? "NORMAL",
      // 是否勾选了登录页的免登录
      isRemembered: false,
      // 登录页的免登录存储几天，默认7天
      loginDay: 7
    };
  },
  actions: {
    /** 存储头像 */
    SET_AVATAR(avatar: string) {
      this.avatar = avatar;
    },
    /** 存储用户名 */
    SET_USERNAME(username: string) {
      this.username = username;
    },
    /** 存储昵称 */
    SET_NICKNAME(nickname: string) {
      this.nickname = nickname;
    },
    /** 存储角色 */
    SET_ROLES(roles: Array<string>) {
      this.roles = roles;
    },
    /** 存储按钮级别权限 */
    SET_PERMS(permissions: Array<string>) {
      this.permissions = permissions;
    },
    /** 存储用户ID */
    SET_USER_ID(userId: number) {
      this.userId = userId;
    },
    /** 存储租户ID */
    SET_TENANT_ID(tenantId: number) {
      this.tenantId = tenantId;
    },
    /** 存储角色编码 */
    SET_ROLE_CODE(roleCode: string) {
      this.roleCode = roleCode;
    },
    /** 存储是否勾选了登录页的免登录 */
    SET_ISREMEMBERED(bool: boolean) {
      this.isRemembered = bool;
    },
    /** 设置登录页的免登录存储几天 */
    SET_LOGINDAY(value: number) {
      this.loginDay = Number(value);
    },
    /** 登入 */
    async loginByUsername(data) {
      return new Promise<UserResult>((resolve, reject) => {
        // 保存 loginType
        const loginType = data.loginType || "admin";
        console.log("[User Store] 登录参数:", data);

        getLogin(data)
          .then(response => {
            console.log("[User Store] 后端响应:", response);
            if (response?.code === 200) {
              // 后端返回格式：{ code: 200, message: "...", data: { token, userId, tenantId, ... } }
              const adaptedData = {
                ...response,
                data: {
                  ...response.data,
                  // 添加 Pure Admin 需要的字段
                  accessToken: response.data.token,
                  refreshToken: "",
                  userId: response.data.userId,
                  tenantId: response.data.tenantId || 0,
                  roleCode: response.data.roleCode || "NORMAL",
                  roles: ["admin"],
                  permissions: [],
                  loginType: loginType
                }
              };
              console.log("[User Store] 设置 Token，token (前32位):", adaptedData.data.accessToken.substring(0, 32), "userId:", adaptedData.data.userId, "tenantId:", adaptedData.data.tenantId, "roleCode:", adaptedData.data.roleCode, "loginType:", adaptedData.data.loginType);
              setToken(adaptedData.data);
              resolve(adaptedData as UserResult);
            } else {
              resolve(response as UserResult);
            }
          })
          .catch(error => {
            reject(error);
          });
      });
    },
    /** 前端登出（调用后端接口清理 Redis） */
    logOut() {
      const userInfo = getUserInfo();
      const loginType = userInfo?.loginType || "admin";
      console.log("[User Store] 退出登录，loginType:", loginType);

      // 先调用后端登出接口，清理 Redis 中的在线用户记录
      logoutApi().then(() => {
        console.log("[User Store] 后端登出成功，Redis 已清理");
      }).catch((error) => {
        console.warn("[User Store] 后端登出接口调用失败，但继续前端清理:", error);
        // 即使后端接口调用失败，也继续执行前端清理
      }).finally(() => {
        // 无论后端接口是否成功，都执行前端清理
        this.username = "";
        this.roles = [];
        this.permissions = [];
        removeToken();
        useMultiTagsStoreHook().handleTags("equal", [...routerArrays]);
        resetRouter();

        // 根据登录类型跳转到对应的登录页面
        if (loginType === "admin") {
          router.push("/console/login");
        } else {
          router.push("/login");
        }
      });
    },
    /** 刷新`token` */
    async handRefreshToken(data) {
      return new Promise<RefreshTokenResult>((resolve, reject) => {
        refreshTokenApi(data)
          .then(data => {
            if (data) {
              setToken(data.data);
              resolve(data);
            }
          })
          .catch(error => {
            reject(error);
          });
      });
    }
  }
});

export function useUserStoreHook() {
  return useUserStore(store);
}
