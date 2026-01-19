import request from "@/utils/request";

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
 * 存储设置配置
 */
export interface StorageSettings {
  type: "local" | "oss" | "s3" | "cos";
  local: {
    path: string;
    maxSize: number;
  };
  oss: {
    region: string;
    bucket: string;
    accessKeyId: string;
    accessKeySecret: string;
    domain?: string;
  };
  s3: {
    region: string;
    bucket: string;
    accessKeyId: string;
    secretAccessKey: string;
    endpoint?: string;
  };
  cos: {
    region: string;
    bucket: string;
    secretId: string;
    secretKey: string;
    domain?: string;
  };
}

/**
 * 邮件设置配置
 */
export interface MailSettings {
  enabled: boolean;
  host: string;
  port: number;
  from: string;
  user: string;
  pass: string;
  secure: boolean;
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
  return request.get<BasicSettings>("/settings/basic");
};

/**
 * 保存基本设置
 */
export const saveBasicSettings = (data: BasicSettings) => {
  return request.post("/settings/basic", data);
};

/**
 * 获取安全设置
 */
export const getSecuritySettings = () => {
  return request.get<SecuritySettings>("/settings/security");
};

/**
 * 保存安全设置
 */
export const saveSecuritySettings = (data: SecuritySettings) => {
  return request.post("/settings/security", data);
};

/**
 * 获取存储设置
 */
export const getStorageSettings = () => {
  return request.get<StorageSettings>("/settings/storage");
};

/**
 * 保存存储设置
 */
export const saveStorageSettings = (data: StorageSettings) => {
  return request.post("/settings/storage", data);
};

/**
 * 测试存储连接
 */
export const testStorageConnection = (type: string, config: any) => {
  return request.post("/settings/storage/test", { type, config });
};

/**
 * 获取邮件设置
 */
export const getMailSettings = () => {
  return request.get<MailSettings>("/settings/mail");
};

/**
 * 保存邮件设置
 */
export const saveMailSettings = (data: MailSettings) => {
  return request.post("/settings/mail", data);
};

/**
 * 测试邮件发送
 */
export const testMail = (to: string) => {
  return request.post("/settings/mail/test", { to });
};

/**
 * 获取系统信息
 */
export const getSystemInfo = () => {
  return request.get<SystemInfo>("/settings/system/info");
};

/**
 * 重启系统
 */
export const restartSystem = () => {
  return request.post("/settings/system/restart");
};
