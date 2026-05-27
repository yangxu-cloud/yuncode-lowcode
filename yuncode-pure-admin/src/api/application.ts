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
  status: number; // 0-未运行, 1-运行中, 2-已停止, 3-异常, 4-已卸载
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
/**
 * 重启应用
 */
export const restartApplication = async (id: number) => {
  const response = await http.request<{ code: number; message: string; data: void }>(
    "post",
    `/system/application/restart/${id}`
  );
  return response.data;
};

/**
 * 还原应用（从已卸载状态恢复）
 */
export const restoreApplication = async (id: number) => {
  const response = await http.request<{ code: number; message: string; data: void }>(
    "post",
    `/system/application/restore/${id}`
  );
  return response.data;
};

export const getApplicationList = async (params: {
  current: number;
  size: number;
  appName?: string;
  status?: number;
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
  return response.data;
};

/**
 * 获取应用资源统计
 */
export const getApplicationStats = async (appId: string, category?: string) => {
  const response = await http.request<{ code: number; message: string; data: Record<string, number> }>(
    "get",
    `/system/application/stats/${appId}`,
    { params: category ? { category } : {} }
  );
  return response.data;
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
 * 删除应用（永久删除）
 */
export const deleteApplication = async (id: number) => {
  const response = await http.request<{ code: number; message: string; data: void }>(
    "delete",
    `/system/application/delete/${id}`
  );
  if (response.code !== 200) {
    throw new Error(response.message || "删除失败");
  }
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

/**
 * 分发结果
 */
export interface DistributeResult {
  id: number;
  appId: string;
  appName: string;
  oldVersion: string;
  newVersion: string;
  fileName: string;
  fileSize: number;
}

/**
 * 分发应用（打包为 .sap）
 */
export const distributeApplication = async (id: number, includeData = false) => {
  const response = await http.request<{ code: number; message: string; data: DistributeResult }>(
    "post",
    `/system/application/distribute/${id}`,
    { params: { includeData } }
  );
  return response.data;
};

/**
 * 下载分发文件（带认证，返回 blob）
 */
export const downloadDistributeFile = async (appId: string, fileName: string) => {
  const response = await http.request<Blob>(
    "get",
    `/system/application/distribute/download`,
    {
      params: { appId, fileName },
      responseType: "blob"
    }
  );
  return response;
};

/**
 * 暂存包信息
 */
export interface StagedPackage {
  appId: string;
  appName: string;
  version: string;
  fileSize: string;
}

/**
 * 部署结果
 */
export interface DeployResult {
  id: number;
  appId: string;
  appName: string;
  version: string;
}

/**
 * 上传 .sap 文件到暂存区
 */
export const uploadDeployPackage = async (file: File) => {
  const formData = new FormData();
  formData.append("file", file);
  const response = await http.request<{ code: number; message: string; data: StagedPackage }>(
    "post",
    `/system/application/deploy/upload`,
    {
      data: formData,
      headers: { "Content-Type": null }
    } as any
  );
  if (response.code !== 200) {
    throw new Error(response.message || "上传失败");
  }
  return response.data;
};

/**
 * 获取暂存区包列表
 */
export const getStagedPackages = async () => {
  const response = await http.request<{ code: number; message: string; data: StagedPackage[] }>(
    "get",
    `/system/application/deploy/packages`
  );
  if (response.code !== 200) {
    throw new Error(response.message || "获取暂存列表失败");
  }
  return response.data;
};

/**
 * 部署暂存区应用包
 */
export const deployStagedPackage = async (appId: string) => {
  const response = await http.request<{ code: number; message: string; data: DeployResult }>(
    "post",
    `/system/application/deploy/install/${appId}`
  );
  if (response.code !== 200) {
    throw new Error(response.message || "部署失败");
  }
  return response.data;
};

/**
 * 删除暂存区应用包
 */
export const deleteStagedPackage = async (appId: string) => {
  const response = await http.request<{ code: number; message: string; data: void }>(
    "delete",
    `/system/application/deploy/packages/${appId}`
  );
  return response.data;
};

// ==================== 分类管理 ====================

export interface CategoryNode {
  id: number;
  name: string;
  appId: string;
  parentId: number | null;
  sort: number;
  children?: CategoryNode[];
}

/**
 * 获取应用的分类树
 */
export const getCategoryTree = async (appId: string) => {
  const response = await http.request<{ code: number; message: string; data: CategoryNode[] }>(
    "get",
    `/system/application/${appId}/categories`
  );
  return response.data;
};

/**
 * 创建分类
 */
export const createCategory = async (appId: string, name: string, parentId?: number | null) => {
  const response = await http.request<{ code: number; message: string; data: CategoryNode }>(
    "post",
    `/system/application/${appId}/categories`,
    { data: { name, parentId: parentId ?? null } }
  );
  return response.data;
};

/**
 * 重命名分类
 */
export const renameCategory = async (appId: string, id: number, name: string) => {
  const response = await http.request<{ code: number; message: string; data: void }>(
    "put",
    `/system/application/${appId}/categories/${id}`,
    { data: { name } }
  );
  return response.data;
};

/**
 * 删除分类
 */
export const deleteCategory = async (appId: string, id: number) => {
  const response = await http.request<{ code: number; message: string; data: void }>(
    "delete",
    `/system/application/${appId}/categories/${id}`
  );
  return response.data;
};
