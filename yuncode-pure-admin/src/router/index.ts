// import "@/utils/sso";
import Cookies from "js-cookie";
import { getConfig } from "@/config";
import NProgress from "@/utils/progress";
import { buildHierarchyTree } from "@/utils/tree";
import remainingRouter from "./modules/remaining";
import { useMultiTagsStoreHook } from "@/store/modules/multiTags";
import { usePermissionStoreHook } from "@/store/modules/permission";
import { isUrl, openLink, cloneDeep, isAllEmpty } from "@pureadmin/utils";
import {
  ascending,
  getTopMenu,
  initRouter,
  getHistoryMode,
  findRouteByPath,
  handleAliveRoute,
  formatTwoStageRoutes,
  formatFlatteningRoutes
} from "./utils";
import {
  type Router,
  type RouteRecordRaw,
  type RouteComponent,
  createRouter
} from "vue-router";
import {
  removeToken,
  MULTIPLE_TABS_KEY,
  getUserInfo
} from "@/utils/auth";
import { useNavigationGuard } from "@/composables/useNavigationGuard";

/** 自动导入全部静态路由，无需再手动引入！匹配 src/router/modules 目录（任何嵌套级别）中具有 .ts 扩展名的所有文件，除了 remaining.ts 文件
 * 如何匹配所有文件请看：https://github.com/mrmlnc/fast-glob#basic-syntax
 * 如何排除文件请看：https://cn.vitejs.dev/guide/features.html#negative-patterns
 */
const modules: Record<string, any> = import.meta.glob(
  ["./modules/**/*.ts", "!./modules/**/remaining.ts"],
  {
    eager: true
  }
);

/** 原始静态路由（未做任何处理） */
const routes = [];

Object.keys(modules).forEach(key => {
  routes.push(modules[key].default);
});

// 分离登录路由（不需要嵌套在其他路由下）和业务路由
const loginRoutes = routes
  .flat(Infinity)
  .filter((route: RouteRecordRaw) => route.path === "/login" || route.path === "/console/login");

const businessRoutes = routes
  .flat(Infinity)
  .filter((route: RouteRecordRaw) => route.path !== "/login" && route.path !== "/console/login");

/** 导出处理后的静态路由（三级及以上的路由全部拍成二级） */
const processedBusinessRoutes = formatTwoStageRoutes(
  formatFlatteningRoutes(buildHierarchyTree(ascending(businessRoutes)))
);

export const constantRoutes: Array<RouteRecordRaw> = [...loginRoutes, ...processedBusinessRoutes];

/** 初始的静态路由，用于退出登录时重置路由 */
const initConstantRoutes: Array<RouteRecordRaw> = cloneDeep(constantRoutes);

/** 用于渲染菜单，保持原始层级 */
export const constantMenus: Array<RouteComponent> = ascending(
  routes.flat(Infinity)
).concat(...remainingRouter);

/** 不参与菜单的路由 */
export const remainingPaths = Object.keys(remainingRouter).map(v => {
  return remainingRouter[v].path;
});

/** 创建路由实例 */
export const router: Router = createRouter({
  history: getHistoryMode(import.meta.env.VITE_ROUTER_HISTORY),
  routes: constantRoutes.concat(...(remainingRouter as any)),
  strict: true,
  scrollBehavior(to, from, savedPosition) {
    return new Promise(resolve => {
      if (savedPosition) {
        return savedPosition;
      } else {
        if (from.meta.saveSrollTop) {
          const top: number =
            document.documentElement.scrollTop || document.body.scrollTop;
          resolve({ left: 0, top });
        }
      }
    });
  }
});

/** 记录已经加载的页面路径 */
const loadedPaths = new Set<string>();

/** 重置已加载页面记录 */
export function resetLoadedPaths() {
  loadedPaths.clear();
}

/** 重置路由 */
export function resetRouter() {
  router.clearRoutes();
  for (const route of initConstantRoutes.concat(...(remainingRouter as any))) {
    router.addRoute(route);
  }
  router.options.routes = formatTwoStageRoutes(
    formatFlatteningRoutes(buildHierarchyTree(ascending(routes.flat(Infinity))))
  );
  usePermissionStoreHook().clearAllCachePage();
  resetLoadedPaths();
}

/** 路由白名单 */
const whiteList = ["/login", "/console/login"];

const { VITE_HIDE_HOME } = import.meta.env;

router.beforeEach(async (to: ToRouteType, _from, next) => {
  to.meta.loaded = loadedPaths.has(to.path);

  if (!to.meta.loaded) {
    NProgress.start();
  }

  if (to.meta?.keepAlive) {
    handleAliveRoute(to, "add");
    if (_from.name === undefined || _from.name === "Redirect") {
      handleAliveRoute(to);
    }
  }

  const { guard } = useNavigationGuard(router);
  await guard(to, _from, next);
});

router.afterEach(to => {
  loadedPaths.add(to.path);
  NProgress.done();
});

export default router;
