import { type RouteRecordRaw } from "vue-router";
import Layout from "@/layout/index.vue";

const appDevRoutes: RouteRecordRaw = {
  path: "/appDev",
  name: "AppDev",
  component: Layout,
  redirect: "/appDev/application",
  meta: {
    icon: "ep/monitor",
    title: "routes.appDev",
    i18nKey: "routes.appDev",
    rank: 4,
    showLink: true
  },
  children: [
    {
      path: "/appDev/application",
      name: "ApplicationDev",
      component: () => import("@/views/facilities/application/index.vue"),
      meta: {
        icon: "ep/monitor",
        title: "routes.application",
        i18nKey: "routes.application",
        showLink: true
      }
    },
    {
      path: "/appDev/modeling",
      name: "Modeling",
      component: () => import("@/views/app-dev/modeling/index.vue"),
      meta: {
        icon: "ep/operation",
        title: "routes.modeling",
        i18nKey: "routes.modeling",
        showLink: true
      }
    },
    {
      path: "/appDev/schedule",
      name: "Schedule",
      component: () => import("@/views/app-dev/schedule/index.vue"),
      meta: {
        icon: "ep/timer",
        title: "routes.schedule",
        i18nKey: "routes.schedule",
        showLink: true
      }
    },
    {
      path: "/appDev/system-service",
      name: "SystemService",
      component: () => import("@/views/app-dev/system-service/index.vue"),
      meta: {
        icon: "ep/tools",
        title: "routes.systemService",
        i18nKey: "routes.systemService",
        showLink: true
      }
    }
  ]
} satisfies RouteConfigsTable;

export default appDevRoutes;