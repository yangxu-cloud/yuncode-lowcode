import axios from "axios";
import { ElMessage } from "element-plus";
import config from "@/config";

const request = axios.create({
  baseURL: config.baseURL,
  timeout: config.timeout
});

// Request interceptor
request.interceptors.request.use(
  config => {
    // 根据当前登录的用户类型选择对应的 token
    let token = null;
    let usedType = "";

    // 从 sessionStorage 获取当前 tab 的用户类型
    const loginType = sessionStorage.getItem("activeLoginType") || "";

    // 根据登录类型选择对应的 token
    if (loginType === "admin") {
      token = localStorage.getItem("token_admin");
      usedType = "admin";
    } else if (loginType === "tenant") {
      token = localStorage.getItem("token_tenant");
      usedType = "tenant";
    } else {
      // 默认使用 user token
      token = localStorage.getItem("token_user");
      usedType = "user";
    }

    // ⚠️ 如果 sessionStorage 中没有登录类型，不要在请求拦截器中设置 sessionStorage
    // 这应该由路由守卫或登录逻辑来管理
    // 如果所有 token 都没有，就不发送 token，让后端返回 401

    // 添加 token 到请求头
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
      console.log(`[Request] 添加 Token (${usedType}):`, token.substring(0, 20) + '...', 'URL:', config.url);
    } else {
      console.log(`[Request] 未找到 Token (${loginType}), URL:`, config.url);
    }

    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

// Response interceptor
request.interceptors.response.use(
  response => {
    console.log('[Response Success]', response.data);
    const { code, message, data } = response.data;
    if (code === 200) {
      return data;
    } else {
      ElMessage.error(message || "请求失败");
      return Promise.reject(new Error(message || "请求失败"));
    }
  },
  error => {
    console.error('[Response Error]', error);
    console.error('[Response Error Details]', error.response);

    // 处理HTTP错误状态码
    if (error.response) {
      const { status, data } = error.response;

      switch (status) {
        case 401:
          ElMessage.error(data?.message || "未登录，请先登录");
          // 可以在这里跳转到登录页
          // router.push('/console/login');
          break;
        case 403:
          ElMessage.error(data?.message || "没有权限访问");
          break;
        case 404:
          ElMessage.error(data?.message || "请求的资源不存在");
          break;
        case 500:
          ElMessage.error(data?.message || "服务器内部错误");
          break;
        default:
          ElMessage.error(data?.message || error.message || "网络错误");
      }

      return Promise.reject(new Error(data?.message || error.message || "网络错误"));
    }

    ElMessage.error(error.message || "网络错误");
    return Promise.reject(error);
  }
);

export default request;
