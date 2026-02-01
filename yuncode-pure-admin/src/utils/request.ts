/**
 * HTTP 请求适配器
 * 根据 appConfig 配置自动切换 Mock 数据或真实 API
 */

import { isMockMode } from '@/config/app';
import { http } from '@/utils/http';

/**
 * 适配后的 GET 请求
 */
export async function adapterGet<T = any>(
  url: string,
  mockFn?: () => Promise<T>,
  config?: any
): Promise<T> {
  if (isMockMode() && mockFn) {
    return mockFn();
  }
  return http.get(url, config);
}

/**
 * 适配后的 POST 请求
 */
export async function adapterPost<T = any>(
  url: string,
  data?: any,
  mockFn?: () => Promise<T>,
  config?: any
): Promise<T> {
  if (isMockMode() && mockFn) {
    console.log(`[Mock] POST ${url}`, data);
    return mockFn();
  }
  return http.post(url, data, config);
}

/**
 * 适配后的 PUT 请求
 */
export async function adapterPut<T = any>(
  url: string,
  data?: any,
  mockFn?: () => Promise<T>,
  config?: any
): Promise<T> {
  if (isMockMode() && mockFn) {
    console.log(`[Mock] PUT ${url}`, data);
    return mockFn();
  }
  return http.request<T>("put", url, data, config);
}

/**
 * 适配后的 DELETE 请求
 */
export async function adapterDelete<T = any>(
  url: string,
  mockFn?: () => Promise<T>,
  config?: any
): Promise<T> {
  if (isMockMode() && mockFn) {
    console.log(`[Mock] DELETE ${url}`);
    return mockFn();
  }
  return http.request<T>("delete", url, config);
}
