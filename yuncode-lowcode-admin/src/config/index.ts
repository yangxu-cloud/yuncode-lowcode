/**
 * 后端接口配置文件
 * 统一管理后端服务的地址、端口和其他配置
 */

/**
 * 环境类型
 */
export enum EnvMode {
  DEVELOPMENT = 'development',
  PRODUCTION = 'production',
  PREVIEW = 'preview'
}

/**
 * 判断当前环境
 */
export const isDev = import.meta.env.DEV;
export const isProd = import.meta.env.PROD;
export const isPreview = import.meta.env.MODE === 'preview';

/**
 * 获取当前环境模式
 */
export const getEnvMode = (): EnvMode => {
  if (isDev) return EnvMode.DEVELOPMENT;
  if (isPreview) return EnvMode.PREVIEW;
  return EnvMode.PRODUCTION;
};

/**
 * 第三方服务配置
 */
export const thirdPartyConfig = {
  /**
   * SkyWalking UI 地址（链路追踪）
   * 用于在日志管理页面打开链路追踪详情
   */
  skyWalkingUrl: import.meta.env.VITE_SKYWALKING_URL || "http://localhost:8088",

  /**
   * IP 查询服务 API
   * 用于获取客户端 IP 地址
   */
  ipQueryApi: import.meta.env.VITE_IP_QUERY_API || "https://api.ipify.org?format=json",

  /**
   * IP 地理位置查询 API
   * 用于获取 IP 对应的地理位置
   */
  ipLocationApi: import.meta.env.VITE_IP_LOCATION_API || "https://ipapi.co/json/",
};

/**
 * 后端服务配置
 */
export const apiConfig = {
  /**
   * 后端服务基础地址
   * 可以是完整的 URL (如: http://localhost:8080)
   * 也可以是相对路径 (如: /api) 用于代理模式
   */
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api",

  /**
   * 请求超时时间（毫秒）
   */
  timeout: 10000,

  /**
   * 是否显示请求日志
   */
  showRequestLog: isDev,

  /**
   * 是否显示响应日志
   */
  showResponseLog: isDev,
};

/**
 * 开发环境配置
 */
export const devConfig = {
  /**
   * 开发环境后端地址
   * 注意：已通过环境变量 VITE_API_BASE_URL 配置，这里不需要再设置 baseURL
   */
  // baseURL: "http://localhost:8080",

  /**
   * 开发环境超时时间
   */
  timeout: 15000,

  /**
   * 显示详细日志
   */
  showRequestLog: true,
  showResponseLog: true,
};

/**
 * 生产环境配置
 */
export const prodConfig = {
  /**
   * 生产环境后端地址
   * 通常使用相对路径，通过 Nginx 代理
   */
  baseURL: "/api",

  /**
   * 生产环境超时时间
   */
  timeout: 10000,

  /**
   * 生产环境不显示详细日志
   */
  showRequestLog: false,
  showResponseLog: false,
};

/**
 * 获取当前环境配置
 */
export const getConfig = () => {
  const mode = getEnvMode();

  if (mode === EnvMode.DEVELOPMENT) {
    return {
      ...apiConfig,
      ...devConfig,
      ...thirdPartyConfig,
    };
  } else if (mode === EnvMode.PREVIEW) {
    // 预览环境使用生产配置
    return {
      ...apiConfig,
      ...prodConfig,
      ...thirdPartyConfig,
    };
  } else {
    // 生产环境
    return {
      ...apiConfig,
      ...prodConfig,
      ...thirdPartyConfig,
    };
  }
};

/**
 * 打印环境信息（用于调试）
 */
export const printEnvInfo = () => {
  console.log('========== 环境信息 ==========');
  console.log('环境模式:', getEnvMode());
  console.log('MODE:', import.meta.env.MODE);
  console.log('DEV:', isDev);
  console.log('PROD:', isProd);
  console.log('BASE_URL:', import.meta.env.BASE_URL);
  console.log('API_BASE_URL:', import.meta.env.VITE_API_BASE_URL);
  console.log('SKYWALKING_URL:', import.meta.env.VITE_SKYWALKING_URL);
  console.log('配置:', getConfig());
  console.log('============================');
};

export default getConfig();
