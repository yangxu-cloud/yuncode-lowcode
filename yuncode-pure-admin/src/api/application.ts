import { http } from "@/utils/http";

/**
 * 应用实体
 */
export interface Application {
  id?: number;
  appId: string;
  appName: string;
  appIcon?: string;
  appDescription?: string;
  status: number; // 0-未运行, 1-运行中, 2-已停止, 3-异常
  version?: string;
  startTime?: string;
  stopTime?: string;
  createTime: string;
  updateTime: string;
}

/**
 * 应用图标（支持颜色）
 */
export interface AppIcon {
  icon: string;
  color?: string;
}

/**
 * 应用表单
 */
export interface ApplicationForm {
  id?: number;
  appId: string;
  appName: string;
  appIcon?: string | AppIcon; // 兼容字符串或对象格式
  appDescription?: string;
  version?: string;
}

/**
 * 应用日志
 */
export interface ApplicationLog {
  id: number;
  appId: number;
  operationType: number; // 0-安装, 1-启动, 2-停止, 3-卸载, 4-升级
  operationContent?: string;
  status: number; // 0-成功, 1-失败
  errorMessage?: string;
  createTime: string;
}

/**
 * 分页结果
 */
export interface PageResult<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
}

/**
 * 分页查询应用列表
 */
export const getApplicationList = async (params: {
  current: number;
  size: number;
  appName?: string;
}) => {
  const response = await http.request<{ code: number; message: string; data: PageResult<Application> }>(
    "get",
    `/system/application/list`,
    { params }
  );
  return response;
};

/**
 * 获取应用详情
 */
export const getApplicationDetail = async (id: number) => {
  const response = await http.request<{ code: number; message: string; data: Application }>(
    "get",
    `/system/application/${id}`
  );
  return response;
};

/**
 * 创建应用
 */
export const createApplication = async (data: ApplicationForm) => {
  const response = await http.request<{ code: number; message: string; data: void }>(
    "post",
    `/system/application/create`,
    { data }
  );
  return response.data;
};

/**
 * 更新应用
 */
export const updateApplication = async (data: ApplicationForm) => {
  const response = await http.request<{ code: number; message: string; data: void }>(
    "put",
    `/system/application/update`,
    { data }
  );
  return response.data;
};

/**
 * 删除应用
 */
export const deleteApplication = async (id: number) => {
  const response = await http.request<{ code: number; message: string; data: void }>(
    "delete",
    `/system/application/delete/${id}`
  );
  return response.data;
};

/**
 * 启动应用
 */
export const startApplication = async (id: number) => {
  const response = await http.request<{ code: number; message: string; data: void }>(
    "post",
    `/system/application/start/${id}`
  );
  return response.data;
};

/**
 * 停止应用
 */
export const stopApplication = async (id: number) => {
  const response = await http.request<{ code: number; message: string; data: void }>(
    "post",
    `/system/application/stop/${id}`
  );
  return response.data;
};

/**
 * 安装应用
 */
export const installApplication = async (id: number) => {
  const response = await http.request<{ code: number; message: string; data: void }>(
    "post",
    `/system/application/install/${id}`
  );
  return response.data;
};

/**
 * 卸载应用
 */
export const uninstallApplication = async (id: number) => {
  const response = await http.request<{ code: number; message: string; data: void }>(
    "post",
    `/system/application/uninstall/${id}`
  );
  return response.data;
};

/**
 * 升级应用
 */
export const upgradeApplication = async (id: number) => {
  const response = await http.request<{ code: number; message: string; data: void }>(
    "post",
    `/system/application/upgrade/${id}`
  );
  return response.data;
};

/**
 * 分页查询应用日志
 */
export const getApplicationLogs = async (params: {
  current: number;
  size: number;
  appId?: number;
  operationType?: number;
}) => {
  const response = await http.request<{ code: number; message: string; data: PageResult<ApplicationLog> }>(
    "get",
    `/system/application/logs`,
    { params }
  );
  return response;
};
