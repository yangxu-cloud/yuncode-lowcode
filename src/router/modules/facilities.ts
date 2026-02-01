import { type RouteRecordRaw } from "vue-router";
import Layout from "@/layout/index.vue";

const facilitiesRoutes: RouteRecordRaw = {
  path: "/facilities",
  name: "Facilities",
  component: Layout,
  redirect: "/facilities/navigation",
  meta: {
    icon: "ep:management",
    title: "routes.facilities",
    i18nKey: "routes.facilities",
    rank: 3,
    showLink: true
  },
  children: [
    {
      path: "/facilities/org",
      name: "Org",
      component: () => import("@/views/facilities/org/index.vue"),
      meta: {
        icon: "ep:office-building",
        title: "routes.org",
        i18nKey: "routes.org",
        showParent: true,
        showLink: true
      }
    },
    {
      path: "/facilities/navigation",
      name: "Navigation",
      component: () => import("@/views/facilities/navigation/index.vue"),
      meta: {
        icon: "ep:menu",
        title: "routes.navigation",
        i18nKey: "routes.navigation",
        showParent: true,
        showLink: true
      }
    }
  ]
} satisfies RouteConfigsTable;

export default facilitiesRoutes;
