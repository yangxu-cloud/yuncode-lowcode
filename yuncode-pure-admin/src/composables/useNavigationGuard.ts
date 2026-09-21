import Cookies from "js-cookie";
import { getConfig } from "@/config";
import NProgress from "@/utils/progress";
import remainingRouter from "@/router/modules/remaining";
import { useMultiTagsStoreHook } from "@/store/modules/multiTags";
import { usePermissionStoreHook } from "@/store/modules/permission";
import { isUrl, openLink } from "@pureadmin/utils";

function isOneOfArray(value: any, arr: any[]): boolean {
  if (!arr) return false;
  return arr.includes(value);
}
import {
  initRouter,
  handleAliveRoute,
  findRouteByPath
} from "@/router/utils";
import { removeToken, MULTIPLE_TABS_KEY, getUserInfo } from "@/utils/auth";
import type { Router, RouteLocationNormalized } from "vue-router";

const whiteList = ["/login", "/console/login"];

/**
 * 路由守卫 composable
 * 将 82 行的 beforeEach 逻辑拆分为独立模块
 */
export function useNavigationGuard(router: Router) {
  const { VITE_HIDE_HOME } = import.meta.env;

  function setDocumentTitle(to: RouteLocationNormalized) {
    to.matched.some(item => {
      if (!item.meta.title) return "";
      const Title = getConfig().Title;
      if (Title) document.title = `${item.meta.title} | ${Title}`;
      else document.title = item.meta.title as string;
    });
  }

  function toCorrectRoute(to: RouteLocationNormalized, next: Function, _from: any) {
    whiteList.includes(to.fullPath) ? next(_from.fullPath) : next();
  }

  async function handleDynamicRoutes(to: RouteLocationNormalized, next: Function) {
    const permissionStore = usePermissionStoreHook();
    if (permissionStore.wholeMenus.length === 0) {
      await initRouter();
      next({ ...to, replace: true });
      return true; // 表示已处理
    }
    return false;
  }

  function handlePermission(to: RouteLocationNormalized, userInfo: any, next: Function) {
    if (to.meta?.roles && !isOneOfArray(to.meta?.roles, userInfo?.roles)) {
      next({ path: "/error/403" });
      return true;
    }
    return false;
  }

  function handleHideHome(to: RouteLocationNormalized, next: Function) {
    if (VITE_HIDE_HOME === "true" && to.fullPath === "/welcome") {
      next({ path: "/error/404" });
      return true;
    }
    return false;
  }

  return {
    async guard(to: RouteLocationNormalized, _from: any, next: Function) {
      const externalLink = isUrl(to?.name as string);

      // 设置文档标题
      if (!externalLink) setDocumentTitle(to);

      const userInfo = getUserInfo();

      if (Cookies.get(MULTIPLE_TABS_KEY) && userInfo) {
        // 动态路由初始化
        if (to.path !== "/login" && to.path !== "/console/login") {
          const handled = await handleDynamicRoutes(to, next);
          // 添加标签页
          useMultiTagsStoreHook().handleTags("push", to);
          if (handled) return;
        }

        // 权限检查
        if (handlePermission(to, userInfo, next)) return;

        // 隐藏首页检查
        if (handleHideHome(to, next)) return;

        // 外部链接处理
        if (_from?.name) {
          if (externalLink) {
            openLink(to?.name as string);
            NProgress.done();
          } else {
            toCorrectRoute(to, next, _from);
          }
        } else {
          toCorrectRoute(to, next, _from);
        }
      } else {
        if (to.path !== "/login" && to.path !== "/console/login") {
          if (whiteList.indexOf(to.path) !== -1) {
            next();
          } else {
            removeToken();
            const loginType = userInfo?.loginType || "admin";
            if (loginType === "admin") {
              next({ path: "/console/login" });
            } else {
              next({ path: "/login" });
            }
          }
        } else {
          next();
        }
      }
    }
  };
}
