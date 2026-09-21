/**
 * 系统监控/运维 API
 */
import { http } from "@/utils/http";
import axios from "axios";

/** Gateway 指标快照 */
export interface MetricsSnapshot {
  _total: {
    requests: number;
    success: number;
    fail: number;
  };
  [path: string]: {
    requests: number;
    success: number;
    fail: number;
    avgDuration: number;
    maxDuration: number;
    minDuration: number;
  } | any;
}

/** App 部署状态 */
export interface AppStatus {
  enabled: boolean;
  loadedCount: number;
  stoppedCount: number;
  totalCount: number;
  appList?: AppStatusItem[];
}

export interface AppStatusItem {
  appId: string;
  status: "running" | "stopped";
  beanCount: number;
}

/**
 * 获取 Gateway 请求指标
 * 注意：Gateway 运行在 9000 端口，不走 /api 前缀
 */
const gatewayHttp = axios.create({ baseURL: "" });

export async function getGatewayMetrics(): Promise<MetricsSnapshot> {
  const response = await gatewayHttp.get<{ success: boolean; data: MetricsSnapshot }>(
    "/gateway/metrics"
  );
  return response.data.data;
}

/**
 * 获取 HotAppDeployer 状态
 */
export async function getAppDeployStatus(): Promise<AppStatus> {
  const response = await http.request<{ code: number; data: AppStatus }>(
    "get",
    "/system/monitor/apps"
  );
  return response.data;
}
