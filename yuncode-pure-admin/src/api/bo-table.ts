import { http } from "@/utils/http";

// ==================== 类型定义 ====================

export interface BoTable {
  id: number;
  appId: string;
  categoryId: number;
  categoryName: string;
  titleName: string;
  storageName: string;
  storageType: string;
  bizCode: string;
  indexes?: string;
  designVersion?: number;
  tenantId: number;
  createTime: string;
  updateTime: string;
}

export interface BoField {
  id?: number;
  tableId?: number;
  fieldName: string;
  fieldTitle: string;
  fieldType: string;
  fieldLength: number;
  component: string;
  defaultValue: string;
  required: number;
  visible: number;
  readonly: number;
  copyable: number;
  sort?: number;
  /** internal: unique key for Vue rendering */
  _key?: string;
  /** internal: checkbox selection state */
  _checked?: boolean;
}

export interface BoTableDetail {
  table: BoTable;
  fields: BoField[];
}

// ==================== API ====================

/**
 * 创建业务对象
 */
export const createBoTable = async (
  appId: string,
  data: { titleName: string; suffix: string; storageType: string; categoryId: number }
) => {
  const response = await http.request<{ code: number; message: string; data: BoTable }>(
    "post",
    `/system/application/${appId}/bo-tables`,
    { data }
  );
  if (response.code !== 200) {
    throw new Error(response.message || "创建失败");
  }
  return response.data;
};

/**
 * 获取业务对象列表
 */
export const getBoTableList = async (appId: string, categoryId?: number) => {
  const params: Record<string, string> = {};
  if (categoryId !== undefined && categoryId !== null) {
    params.categoryId = String(categoryId);
  }
  const response = await http.request<{ code: number; message: string; data: BoTable[] }>(
    "get",
    `/system/application/${appId}/bo-tables`,
    { params }
  );
  return response.data;
};

/**
 * 获取业务对象详情（含字段列表）
 */
export const getBoTableDetail = async (id: number) => {
  const response = await http.request<{ code: number; message: string; data: BoTableDetail }>(
    "get",
    `/system/application/bo-tables/${id}`
  );
  return response.data;
};

/**
 * 更新业务对象
 */
export const updateBoTable = async (id: number, data: { titleName?: string; storageType?: string }) => {
  const response = await http.request<{ code: number; message: string; data: void }>(
    "put",
    `/system/application/bo-tables/${id}`,
    { data }
  );
  return response.data;
};

/**
 * 删除业务对象
 */
export const deleteBoTable = async (id: number) => {
  const response = await http.request<{ code: number; message: string; data: void }>(
    "delete",
    `/system/application/bo-tables/${id}`
  );
  return response.data;
};

/**
 * 批量保存字段
 */
export const batchSaveFields = async (tableId: number, fields: BoField[]) => {
  const response = await http.request<{ code: number; message: string; data: void }>(
    "put",
    `/system/application/bo-tables/${tableId}/fields`,
    { data: fields }
  );
  return response.data;
};

/**
 * 生成DDL文件
 */
export const generateDdl = async (tableId: number) => {
  const response = await http.request<{ code: number; message: string; data: string }>(
    "post",
    `/system/application/bo-tables/${tableId}/generate-ddl`
  );
  return response.data;
};

// ==================== 新增 API ====================

/**
 * 保存完整设计（字段 + 索引 + XML 导出）
 */
export const saveBoDesign = async (tableId: number, data: { fields: BoField[]; indexes: string }) => {
  const response = await http.request<{ code: number; message: string; data: void }>(
    "post",
    `/system/application/bo-tables/${tableId}/design`,
    { data }
  );
  if (response.code !== 200) {
    throw new Error(response.message || "保存失败");
  }
  return response.data;
};

/**
 * 获取 DDL 变更预览
 */
export const getDdlDiff = async (tableId: number) => {
  const response = await http.request<{ code: number; message: string; data: any }>(
    "get",
    `/system/application/bo-tables/${tableId}/ddl-diff`
  );
  return response.data;
};

/**
 * 执行 DDL
 */
export const executeDdl = async (tableId: number, confirmed?: string[]) => {
  const response = await http.request<{ code: number; message: string; data: any }>(
    "post",
    `/system/application/bo-tables/${tableId}/ddl-execute`,
    { data: { confirmed: confirmed || [] } }
  );
  return response.data;
};

/**
 * 部署同步
 */
export const deploySync = async (tableId: number) => {
  const response = await http.request<{ code: number; message: string; data: void }>(
    "post",
    `/system/application/bo-tables/${tableId}/deploy-sync`
  );
  return response.data;
};

/**
 * 回滚设计
 */
export const rollbackDesign = async (tableId: number, version: number) => {
  const response = await http.request<{ code: number; message: string; data: void }>(
    "post",
    `/system/application/bo-tables/${tableId}/rollback`,
    { data: { version } }
  );
  return response.data;
};
