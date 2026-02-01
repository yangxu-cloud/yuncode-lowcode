/**
 * 应用配置
 * 控制数据源、环境等全局配置
 */

// 数据源类型
export type DataSource = 'mock' | 'api';

/**
 * 应用配置
 */
export const appConfig = {
  // 数据源配置：mock | api
  // 优先级：localStorage > 环境变量 > 默认值
  dataSource: getInitialDataSource() as DataSource,

  // 是否允许运行时切换（仅开发模式）
  allowRuntimeSwitch: import.meta.env.DEV,
};

/**
 * 获取初始数据源配置
 */
function getInitialDataSource(): string {
  // 1. 优先从 localStorage 读取（用户手动切换的值）
  const stored = localStorage.getItem('data-source');
  if (stored === 'mock' || stored === 'api') {
    return stored;
  }

  // 2. 从环境变量读取
  const envValue = import.meta.env.VITE_DATA_SOURCE;
  if (envValue === 'mock' || envValue === 'api') {
    return envValue;
  }

  // 3. 默认使用 API（生产环境）或 mock（开发演示）
  return import.meta.env.DEV ? 'api' : 'api';
}

/**
 * 设置数据源
 */
export function setDataSource(source: DataSource) {
  localStorage.setItem('data-source', source);
  appConfig.dataSource = source;
  // 刷新页面以应用新配置
  window.location.reload();
}

/**
 * 获取当前数据源
 */
export function getDataSource(): DataSource {
  return appConfig.dataSource;
}

/**
 * 是否使用 Mock 数据
 */
export function isMockMode(): boolean {
  return appConfig.dataSource === 'mock';
}
