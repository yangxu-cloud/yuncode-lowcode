import { type RouteRecordRaw } from "vue-router";
import Layout from "@/layout/index.vue";

const operationsRoutes: RouteRecordRaw = {
  path: "/operations",
  name: "Operations",
  component: Layout,
  redirect: "/operations/online-users",
  meta: {
    icon: "ep/setting",
    title: "routes.systemOperations",
    i18nKey: "routes.systemOperations",
    rank: 2
  },
  children: [
    {
      path: "/operations/online-users",
      name: "OnlineUsers",
      component: () => import("@/views/system/online-users/index.vue"),
      meta: {
        icon: "ep/user-filled",
        title: "routes.onlineUsers",
        i18nKey: "routes.onlineUsers",
        showLink: true
      }
    },
    {
      path: "/operations/user-log",
      name: "UserLog",
      component: () => import("@/views/operations/personnel-log/index.vue"),
      meta: {
        icon: "ep/user",
        title: "routes.userLog",
        i18nKey: "routes.userLog",
        showLink: true
      }
    },
    {
      path: "/operations/system-log",
      name: "SystemLog",
      component: () => import("@/views/operations/system-log/index.vue"),
      meta: {
        icon: "ep/document",
        title: "routes.systemLog",
        i18nKey: "routes.systemLog",
        showLink: true
      }
    },
    {
      path: "/operations/operation-log",
      name: "OperationLog",
      component: () => import("@/views/operations/operation-log/index.vue"),
      meta: {
        icon: "ep/edit",
        title: "routes.operationLog",
        i18nKey: "routes.operationLog",
        showLink: true
      }
    },
    {
      path: "/operations/settings",
      name: "Settings",
      component: () => import("@/views/settings/index.vue"),
      meta: {
        icon: "ep/tools",
        title: "routes.settings",
        i18nKey: "routes.settings",
        showLink: true
      }
    }
  ]
} satisfies RouteConfigsTable;

export default operationsRoutes;
