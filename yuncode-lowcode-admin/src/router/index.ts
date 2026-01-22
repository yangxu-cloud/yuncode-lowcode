import { createRouter, createWebHistory } from "vue-router";
import { useUserStore } from "@/stores/user";

const routes = [
  {
    path: "/",
    redirect: "/login"
  },
  {
    path: "/redirect/:path(.*)",
    component: () => import("@/views/redirect.vue"),
    meta: {
      requiresAuth: false
    }
  },
  {
    path: "/login",
    name: "Login",
    component: () => import("@/views/login/index.vue"),
    meta: {
      title: "用户登录",
      requiresAuth: false
    }
  },
  {
    path: "/console/login",
    name: "ConsoleLogin",
    component: () => import("@/views/console/login/index.vue"),
    meta: {
      title: "管理员登录",
      requiresAuth: false
    }
  },
  {
    path: "/layout",
    name: "Layout",
    component: () => import("@/views/layout/index.vue"),
    redirect: "/home",
    meta: {
      requiresAuth: true
    },
    children: [
      {
        path: "/home",
        name: "Home",
        component: () => import("@/views/home/index.vue"),
        meta: {
          title: "首页",
          requiresAuth: true
        }
      },
      {
        path: "/system",
        name: "System",
        component: () => import("@/views/system/index.vue"),
        redirect: "/system/logs",
        meta: {
          title: "系统管理",
          requiresAuth: true
        },
        children: [
          {
            path: "/system/logs",
            name: "SystemLogs",
            component: () => import("@/views/system/LogManage.vue"),
            meta: { title: "日志管理", requiresAuth: true }
          },
          {
            path: "/system/online-users",
            name: "OnlineUsers",
            component: () => import("@/views/system/OnlineUsers.vue"),
            meta: { title: "在线用户", requiresAuth: true }
          }
        ]
      },
      {
        path: "/settings",
        name: "Settings",
        component: () => import("@/views/settings/index.vue"),
        redirect: "/settings/basic",
        meta: {
          title: "系统设置",
          requiresAuth: true
        },
        children: [
          {
            path: "/settings/basic",
            name: "SettingsBasic",
            component: () => import("@/views/settings/Basic.vue"),
            meta: { title: "基本设置", requiresAuth: true }
          },
          {
            path: "/settings/security",
            name: "SettingsSecurity",
            component: () => import("@/views/settings/Security.vue"),
            meta: { title: "安全设置", requiresAuth: true }
          },
          {
            path: "/settings/storage",
            name: "SettingsStorage",
            component: () => import("@/views/settings/Storage.vue"),
            meta: { title: "存储设置", requiresAuth: true }
          },
          {
            path: "/settings/mail",
            name: "SettingsMail",
            component: () => import("@/views/settings/Mail.vue"),
            meta: { title: "邮件设置", requiresAuth: true }
          },
          {
            path: "/settings/system",
            name: "SettingsSystem",
            component: () => import("@/views/settings/System.vue"),
            meta: { title: "系统信息", requiresAuth: true }
          }
        ]
      }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

/**
 * 路由守卫：验证登录状态
 */
router.beforeEach(async (to, from, next) => {
  // 初始化用户 Store（从本地存储恢复）
  const userStore = useUserStore();

  // ⚠️ 关键修复：检查当前标签页是否主动退出
  // 如果用户点击了退出，不要恢复其他用户的登录信息
  const hasLogout = sessionStorage.getItem("logout");
  if (hasLogout === "true") {
    console.log('[路由守卫] 检测到用户主动退出，清除标记并跳转登录页');
    sessionStorage.removeItem("logout");

    // ⚠️ 重要：从 sessionStorage 读取 lastLoginType（在 clearUserInfo 中保存）
    // 不能从 userInfo 读取，因为此时 userInfo 已经被清空
    const lastLoginType = sessionStorage.getItem("lastLoginType") || "";
    sessionStorage.removeItem("lastLoginType"); // 使用后清除

    console.log('[路由守卫] 最后登录类型:', lastLoginType);

    // 如果之前的登录类型是 admin，跳转到管理员登录页
    if (lastLoginType === "admin" && to.path !== "/console/login") {
      console.log('[路由守卫] Admin 退出，跳转到管理员登录页');
      next("/console/login");
      return;
    }

    // 不恢复任何用户信息，直接跳转到用户/租户登录页
    if (to.path !== "/login" && to.path !== "/console/login") {
      next("/login");
      return;
    }

    // 如果已经在登录页，清除 logout 标记后放行（必须调用 next()）
    next();
    return;
  }

  // 根据当前路径确定要恢复的登录类型
  let preferredType = "";

  // 只在没有登录信息时才从localStorage恢复
  if (!userStore.isLogin) {
    if (to.path.startsWith("/console")) {
      preferredType = "admin";
    } else if (to.path.startsWith("/login")) {
      preferredType = "user";  // 或 tenant，根据需要调整
    } else {
      // 对于其他页面（如 /layout, /home），优先从 sessionStorage 读取当前 tab 的用户类型
      preferredType = sessionStorage.getItem("activeLoginType") || "";

      // 如果 sessionStorage 中没有，根据 URL 路径推断
      if (!preferredType) {
        if (to.path.startsWith("/console") || to.path.startsWith("/system")) {
          preferredType = "admin";
        } else {
          // 优先级：user > tenant > admin（因为普通用户最常用）
          const types = ["user", "tenant", "admin"];
          for (const type of types) {
            const token = localStorage.getItem(`token_${type}`);
            if (token) {
              preferredType = type;
              // 保存到 sessionStorage，避免下次还要遍历
              sessionStorage.setItem("activeLoginType", type);
              break;
            }
          }
        }
      }
    }

    // 恢复用户信息
    userStore.initUserInfo(preferredType);
  }

  // ⚠️ 关键：即使已登录，也要确保 sessionStorage 中有 activeLoginType
  if (userStore.isLogin && !sessionStorage.getItem("activeLoginType")) {
    // 从 userStore.userInfo 获取登录类型并保存到 sessionStorage
    if (userStore.userInfo?.loginType) {
      sessionStorage.setItem("activeLoginType", userStore.userInfo.loginType);
      console.log('[路由守卫] 从 userInfo 恢复 activeLoginType:', userStore.userInfo.loginType);
    }
  }

  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - Yuncode LowCode`;
  }

  // 开发环境：输出路由守卫调试信息
  if (import.meta.env.DEV) {
    console.log('[路由守卫]', {
      to: to.path,
      preferredType: preferredType,
      requiresAuth: to.meta.requiresAuth,
      isLogin: userStore.isLogin,
      hasToken: !!userStore.token,
      currentUser: userStore.username
    });
  }

  // 检查路由是否需要认证
  const requiresAuth = to.meta.requiresAuth !== false;

  if (requiresAuth) {
    // 需要登录的路由
    if (userStore.isLogin) {
      // 本地有 token，但需要验证 token 是否真正有效
      // 通过调用 API 验证（如果后端返回 false，说明 token 已失效）
      try {
        // 动态导入 API，避免循环依赖
        const { checkLogin } = await import("@/api/auth");
        const isLoggedIn = await checkLogin();

        // ⚠️ 关键：检查 checkLogin 的返回值
        if (isLoggedIn === false) {
          console.warn('[路由守卫] Token 已失效（被踢出或过期），清除本地数据');

          // 获取当前登录类型
          const loginType = userStore.userInfo?.loginType ||
                            sessionStorage.getItem("activeLoginType") ||
                            sessionStorage.getItem("loginType") || "";

          // 清除对应的 localStorage 和 sessionStorage
          if (loginType) {
            localStorage.removeItem(`token_${loginType}`);
            localStorage.removeItem(`userInfo_${loginType}`);
            sessionStorage.removeItem("loginType");
            sessionStorage.removeItem("activeLoginType");
          }

          // 清空 store
          userStore.token = "";
          userStore.userInfo = null;

          // 跳转到对应的登录页
          if (loginType === "admin") {
            next("/console/login");
          } else {
            next("/login");
          }
          return;
        }

        // Token 有效，允许访问
        next();
      } catch (error: any) {
        console.error('[路由守卫] Token 验证失败:', error);
        // Token 已失效（被踢出或过期），清除本地数据并跳转登录页
        userStore.logout();
        next({
          path: "/login",
          query: { redirect: to.fullPath }
        });
      }
    } else {
      // 未登录，跳转到登录页
      console.log('[路由守卫] 未登录，跳转到登录页');
      next({
        path: "/login",
        query: { redirect: to.fullPath } // 保存原始路由用于登录后跳转
      });
    }
  } else {
    // 不需要登录的路由（如登录页）
    const loginType = sessionStorage.getItem("loginType") || "";

    if (userStore.isLogin) {
      // 已登录用户访问登录页的逻辑
      if (to.path === "/login" && loginType === "admin") {
        // 管理员已登录，访问普通用户登录页，允许访问（可能想切换到普通用户登录）
        console.log('[路由守卫] 管理员访问普通用户登录页，允许访问');
        next();
      } else if (to.path === "/console/login" && loginType !== "admin") {
        // 普通用户已登录，访问管理员登录页，允许访问（可能想切换到管理员登录）
        console.log('[路由守卫] 普通用户访问管理员登录页，允许访问');
        next();
      } else if (to.path === "/login" || to.path === "/console/login") {
        // 已登录用户访问对应类型的登录页，跳转到首页
        console.log('[路由守卫] 已登录用户访问登录页，跳转到首页');
        next("/home");
      } else {
        next();
      }
    } else {
      // 未登录用户，允许访问所有不需要认证的页面
      next();
    }
  }
});

export default router;
