import { type RouteRecordRaw } from "vue-router";

export default [
  {
    path: "/login",
    name: "Login",
    component: () => import("@/views/login/index.vue"),
    meta: {
      title: "登录",
      showLink: false
    }
  },
  {
    path: "/console/login",
    name: "AdminLogin",
    component: () => import("@/views/login/admin.vue"),
    meta: {
      title: "管理员登录",
      showLink: false
    }
  }
] satisfies RouteConfigsTable;
