/**
 * 首页仪表盘 API
 * 统计分析相关接口
 */
import { http } from "@/utils/http";
import { isMockMode } from "@/config/app";

// ==================== 类型定义 ====================

/** 统计卡片数据 */
export interface StatsCard {
  title: string;
  value: number;
  icon: string;
  color: string;
  bgColor: string;
}

/** 运维记录 */
export interface OpsRecord {
  time: string;
  user: string;
  action: string;
  target: string;
  type: string;
}

/** SLA 指标 */
export interface SlaIndicator {
  name: string;
  target: string;
  current: string;
  status: string;
}

/** 在线趋势数据 */
export interface OnlineTrend {
  hours: string[];
  values: number[];
}

/** 应用分布数据 */
export interface AppDistribution {
  name: string;
  value: number;
  color: string;
}

/** 仪表盘完整数据 */
export interface DashboardData {
  stats: StatsCard[];
  onlineTrend: OnlineTrend;
  appDistribution: AppDistribution[];
  recentOps: OpsRecord[];
  slaIndicators: SlaIndicator[];
}

// ==================== Mock 数据 ====================

const mockDashboardData: DashboardData = {
  stats: [
    { title: "在线人数", value: 128, icon: "👥", color: "#409eff", bgColor: "#ecf5ff" },
    { title: "应用数量", value: 12, icon: "📦", color: "#67c23a", bgColor: "#f0f9eb" },
    { title: "BO 模型", value: 56, icon: "🗃️", color: "#e6a23c", bgColor: "#fdf6ec" },
    { title: "表单数量", value: 89, icon: "📝", color: "#f56c6c", bgColor: "#fef0f0" },
    { title: "台账数量", value: 34, icon: "📋", color: "#909399", bgColor: "#f4f4f5" },
    { title: "流程数量", value: 23, icon: "🔄", color: "#b37feb", bgColor: "#f9f0ff" }
  ],
  onlineTrend: {
    hours: Array.from({ length: 24 }, (_, i) => `${i}:00`),
    values: [12, 18, 25, 32, 45, 68, 89, 120, 145, 132, 128, 115, 98, 85, 92, 105, 128, 142, 135, 118, 95, 68, 42, 28]
  },
  appDistribution: [
    { name: "质量管理", value: 5, color: "#409eff" },
    { name: "供应商管理", value: 3, color: "#67c23a" },
    { name: "设备管理", value: 2, color: "#e6a23c" },
    { name: "其他", value: 2, color: "#909399" }
  ],
  recentOps: [
    { time: "2026-05-28 09:30", user: "admin", action: "部署应用", target: "QMS质量管理系统", type: "deploy" },
    { time: "2026-05-28 08:15", user: "yangxu", action: "更新配置", target: "系统参数设置", type: "config" },
    { time: "2026-05-27 17:42", user: "admin", action: "创建用户", target: "zhangsan", type: "user" },
    { time: "2026-05-27 14:20", user: "yangxu", action: "导入数据", target: "供应商主数据", type: "data" },
    { time: "2026-05-27 10:05", user: "admin", action: "备份数据库", target: "yuncode_lowcode", type: "backup" }
  ],
  slaIndicators: [
    { name: "系统可用性", target: "99.9%", current: "99.97%", status: "success" },
    { name: "API 响应时间", target: "< 200ms", current: "156ms", status: "success" },
    { name: "并发用户数", target: ">= 500", current: "428", status: "warning" },
    { name: "数据备份", target: "每日", current: "正常", status: "success" }
  ]
};

// ==================== API 接口 ====================

/**
 * 获取仪表盘完整数据
 */
export async function getDashboardData(): Promise<DashboardData> {
  if (isMockMode()) {
    return Promise.resolve(mockDashboardData);
  }

  try {
    const response = await http.request<{ code: number; data: DashboardData }>(
      "get",
      "/system/dashboard"
    );
    return response.data;
  } catch {
    return mockDashboardData;
  }
}

/**
 * 获取统计卡片数据
 */
export async function getStatsCards(): Promise<StatsCard[]> {
  if (isMockMode()) {
    return Promise.resolve(mockDashboardData.stats);
  }

  try {
    const response = await http.request<{ code: number; data: StatsCard[] }>(
      "get",
      "/system/dashboard/stats"
    );
    return response.data;
  } catch {
    return mockDashboardData.stats;
  }
}

/**
 * 获取在线趋势数据
 */
export async function getOnlineTrend(): Promise<OnlineTrend> {
  if (isMockMode()) {
    return Promise.resolve(mockDashboardData.onlineTrend);
  }

  try {
    const response = await http.request<{ code: number; data: OnlineTrend }>(
      "get",
      "/system/dashboard/online-trend"
    );
    return response.data;
  } catch {
    return mockDashboardData.onlineTrend;
  }
}

/**
 * 获取应用分布数据
 */
export async function getAppDistribution(): Promise<AppDistribution[]> {
  if (isMockMode()) {
    return Promise.resolve(mockDashboardData.appDistribution);
  }

  try {
    const response = await http.request<{ code: number; data: AppDistribution[] }>(
      "get",
      "/system/dashboard/app-distribution"
    );
    return response.data;
  } catch {
    return mockDashboardData.appDistribution;
  }
}

/**
 * 获取最近运维记录
 */
export async function getRecentOps(): Promise<OpsRecord[]> {
  if (isMockMode()) {
    return Promise.resolve(mockDashboardData.recentOps);
  }

  try {
    const response = await http.request<{ code: number; data: OpsRecord[] }>(
      "get",
      "/system/dashboard/recent-ops"
    );
    return response.data;
  } catch {
    return mockDashboardData.recentOps;
  }
}

/**
 * 获取 SLA 指标
 */
export async function getSlaIndicators(): Promise<SlaIndicator[]> {
  if (isMockMode()) {
    return Promise.resolve(mockDashboardData.slaIndicators);
  }

  try {
    const response = await http.request<{ code: number; data: SlaIndicator[] }>(
      "get",
      "/system/dashboard/sla"
    );
    return response.data;
  } catch {
    return mockDashboardData.slaIndicators;
  }
}
