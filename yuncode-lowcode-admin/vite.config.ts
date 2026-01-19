import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";
import { resolve } from "path";

export default defineConfig(({ mode }) => {
  // 加载环境变量
  const env = loadEnv(mode, process.cwd());

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        "@": resolve(__dirname, "src")
      }
    },
    server: {
      port: 3000,
      proxy: {
        "/api": {
          // 从环境变量读取后端地址，如果没设置则使用默认值
          target: env.VITE_API_PROXY_TARGET || "http://localhost:8080",
          changeOrigin: true,
          // 注意：后端已经配置了 context-path: /api，所以不需要移除 /api 前缀
          // rewrite: (path) => path.replace(/^\/api/, '')
        }
      }
    }
  };
});
