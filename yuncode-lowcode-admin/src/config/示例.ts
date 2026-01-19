/**
 * 环境判断使用示例
 */

import { isDev, isProd, isPreview, getEnvMode, EnvMode, printEnvInfo } from '@/config';

// ============================================
// 示例 1: 基础环境判断
// ============================================

if (isDev) {
  console.log('当前是开发环境');
  // 开发环境特有的逻辑
  // 例如：启用调试工具、显示详细日志
}

if (isProd) {
  console.log('当前是生产环境');
  // 生产环境特有的逻辑
  // 例如：启用性能监控、错误上报
}

if (isPreview) {
  console.log('当前是预览环境');
  // 预览环境特有的逻辑
}

// ============================================
// 示例 2: 使用枚举判断
// ============================================

const mode = getEnvMode();

switch (mode) {
  case EnvMode.DEVELOPMENT:
    console.log('开发环境模式');
    break;
  case EnvMode.PRODUCTION:
    console.log('生产环境模式');
    break;
  case EnvMode.PREVIEW:
    console.log('预览环境模式');
    break;
}

// ============================================
// 示例 3: 条件渲染
// ============================================

// 在 Vue 组件中
/*
<template>
  <div>
    <div v-if="isDev">开发环境专属内容</div>
    <div v-if="isProd">生产环境专属内容</div>
  </div>
</template>

<script setup lang="ts">
import { isDev, isProd } from '@/config';
</script>
*/

// ============================================
// 示例 4: 根据环境使用不同的 API 配置
// ============================================

const getApiConfig = () => {
  if (isDev) {
    return {
      baseURL: 'http://localhost:8080',
      timeout: 15000,
    };
  } else {
    return {
      baseURL: '/api',
      timeout: 10000,
    };
  }
};

// ============================================
// 示例 5: 打印环境信息（调试用）
// ============================================

// 在应用启动时调用
printEnvInfo();

// 输出示例：
// ========== 环境信息 ==========
// 环境模式: development
// MODE: development
// DEV: true
// PROD: false
// BASE_URL: /
// API_BASE_URL: http://localhost:8080
// 配置: { baseURL: 'http://localhost:8080', timeout: 15000, ... }
// =============================

// ============================================
// 示例 6: 日志级别控制
// ============================================

const log = {
  debug: (...args: any[]) => {
    if (isDev) {
      console.log('[DEBUG]', ...args);
    }
  },
  info: (...args: any[]) => {
    console.log('[INFO]', ...args);
  },
  warn: (...args: any[]) => {
    console.warn('[WARN]', ...args);
  },
  error: (...args: any[]) => {
    console.error('[ERROR]', ...args);
  },
};

// 使用
log.debug('开发环境才能看到这条日志');
log.info('所有环境都能看到');

// ============================================
// 示例 7: 特征开关（Feature Flags）
// ============================================

const features = {
  // 开发环境启用调试面板
  debugPanel: isDev,

  // 生产环境启用错误监控
  errorTracking: isProd,

  // 开发环境启用性能监控
  performanceMonitoring: isDev,

  // 所有环境都启用
  analytics: true,
};

// 使用
if (features.debugPanel) {
  // 加载调试面板
}

// ============================================
// 示例 8: API 请求拦截器
// ============================================

/*
import axios from 'axios';
import { isDev } from '@/config';

const request = axios.create({
  baseURL: config.baseURL,
  timeout: config.timeout,
});

// 请求拦截器
request.interceptors.request.use((config) => {
  // 开发环境打印请求信息
  if (isDev) {
    console.log('[Request]', config.method?.toUpperCase(), config.url, config.data);
  }

  return config;
});

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    // 开发环境打印响应信息
    if (isDev) {
      console.log('[Response]', response.config.url, response.data);
    }

    return response;
  },
  (error) => {
    // 开发环境打印错误详情
    if (isDev) {
      console.error('[Response Error]', error.config?.url, error.response?.data);
    }

    return Promise.reject(error);
  }
);
*/

// ============================================
// 示例 9: 路由配置
// ============================================

/*
const routes = [
  {
    path: '/debug',
    component: DebugPanel,
    meta: {
      // 只在开发环境显示
      visible: isDev,
    },
  },
];

// 在路由守卫中过滤
router.beforeEach((to, from, next) => {
  if (to.meta.visible === false && isProd) {
    next({ path: '/404' });
  } else {
    next();
  }
});
*/

export {};
