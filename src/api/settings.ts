import { http } from "@/utils/http";

/**
 * 基本设置配置
 */
export interface BasicSettings {
  systemName: string;
  systemLogo: string;
  systemUrl: string;
  systemDescription: string;
  copyright: string;
  icp: string;
  language: string;
  timezone: string;
  dateFormat: string;
  timeFormat: string;
}

/**
 * 安全设置配置
 */
export interface SecuritySettings {
  passwordPolicy: {
    minLength: number;
    requireUppercase: boolean;
    requireLowercase: boolean;
    requireNumber: boolean;
    requireSpecial: boolean;
    expireDays: number;
  };
  loginPolicy: {
    maxAttempts: number;
    lockDuration: number;
    sessionTimeout: number;
    enableCaptcha: boolean;
  };
}

/**
 * 系统信息
 */
export interface SystemInfo {
  name: string;
  version: string;
  env: string;
  framework: string;
  javaVersion: string;
  startTime: string;
  uptime: string;
  serverIp: string;
  os: string;
  arch: string;
}

/**
 * 获取基本设置
 */
export const getBasicSettings = () => {
  return http.request<BasicSettings>("get", "/settings/basic");
};

/**
 * 保存基本设置
 */
export const saveBasicSettings = (data: BasicSettings) => {
  return http.request("post", "/settings/basic", {
    data
  });
};

/**
 * 获取安全设置
 */
export const getSecuritySettings = () => {
  return http.request<SecuritySettings>("get", "/settings/security");
};

/**
 * 保存安全设置
 */
export const saveSecuritySettings = (data: SecuritySettings) => {
  return http.request("post", "/settings/security", {
    data
  });
};

/**
 * 获取系统信息
 */
export const getSystemInfo = () => {
  return http.request<SystemInfo>("get", "/settings/system/info");
};
