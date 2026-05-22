import { http } from "@/utils/http";

/**
 * 后端统一返回格式的 data 部分
 */
type AsyncRouteResult = Array<any>;

/**
 * 获取动态路由（菜单管理 → 前端路由树）
 */
export const getAsyncRoutes = () => {
  return http.request<{ code: number; message: string; data: AsyncRouteResult }>(
    "get",
    "/get-async-routes"
  );
};
