<template>
  <el-config-provider :locale="currentLocale">
    <router-view />
    <ReDialog />
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, watch } from "vue";
import { ElConfigProvider } from "element-plus";
import { ReDialog } from "@/components/ReDialog";
import { useI18n } from "vue-i18n";
import { useRoute } from "vue-router";
import zhCn from "element-plus/es/locale/lang/zh-cn";
import en from "element-plus/es/locale/lang/en";
import { useKickOutNotification } from "@/composables/useKickOutNotification";
import Cookies from "js-cookie";
import { getToken, getSessionKey } from "@/utils/auth";

// 国际化
const { locale } = useI18n();
const route = useRoute();

// Element Plus 语言映射
const elementPlusLocales = {
  "zh-CN": zhCn,
  "en-US": en
};

// 当前 Element Plus 语言
const currentLocale = computed(() => {
  return elementPlusLocales[locale.value] || zhCn;
});

// SSE 踢出通知
const { connectSSE, disconnect } = useKickOutNotification();

// 检查是否有 token 和 sessionId 并建立连接
const checkAndConnect = () => {
  const tokenData = getToken();
  if (tokenData && tokenData.accessToken && tokenData.sessionId) {
    const token = tokenData.accessToken;
    const sessionId = tokenData.sessionId;
    const sessionKey = getSessionKey(sessionId);
    const hasCookie = Cookies.get(sessionKey);

    if (hasCookie) {
      console.log('[App] 用户已登录，初始化 SSE 连接');
      console.log('[App] sessionId:', sessionId);
      console.log('[App] token (前32位):', token.substring(0, 32));
      connectSSE();
    } else {
      console.log('[App] Cookie 中未找到 session，跳过 SSE 初始化');
      disconnect();
    }
  } else {
    console.log('[App] 用户未登录或缺少 sessionId，跳过 SSE 初始化');
    disconnect();
  }
};

// 组件挂载时初始化 SSE 连接
onMounted(() => {
  checkAndConnect();
});

// 监听路由变化，当从登录页跳转到其他页面时建立连接
watch(
  () => route.path,
  (newPath, oldPath) => {
    console.log('[App] 路由变化:', oldPath, '->', newPath);

    // 如果从登录页跳转离开，建立 SSE 连接
    if (oldPath === '/login' || oldPath === '/console/login') {
      if (newPath !== '/login' && newPath !== '/console/login') {
        console.log('[App] 从登录页跳转，建立 SSE 连接');
        // 延迟一点，确保登录流程完成
        setTimeout(() => {
          checkAndConnect();
        }, 500);
      }
    }
  }
);

// 监听语言变化
watch(locale, (newLocale) => {
  console.log("[App] 语言已切换为:", newLocale);
});

// 组件卸载时断开连接
onUnmounted(() => {
  console.log('[App] App 卸载，断开 SSE 连接');
  disconnect();
});
</script>
