<template>
  <router-view />

  <!-- 踢出通知弹窗 -->
  <KickOutDialog
    :visible="showKickOutDialog"
    :kick-out-data="kickOutData"
    :countdown="countdown"
    @logout-now="handleLogoutNow"
  />
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import KickOutDialog from '@/components/KickOutDialog.vue'
import { useKickOutNotification } from '@/composables/useKickOutNotification'

// 使用踢出通知功能
const {
  countdown,
  showKickOutDialog,
  kickOutData,
  connectSSE,
  disconnect,
  handleLogoutNow
} = useKickOutNotification()

const route = useRoute()

// 检查是否有 token 并建立连接
const checkAndConnect = () => {
  const loginType = sessionStorage.getItem('activeLoginType') || sessionStorage.getItem('loginType')
  const hasToken = loginType
    ? localStorage.getItem(`token_${loginType}`)
    : (localStorage.getItem('token_admin') || localStorage.getItem('token_tenant') || localStorage.getItem('token_user'))

  if (hasToken) {
    console.log('[App] 用户已登录，初始化 SSE 连接，登录类型:', loginType || 'auto')
    connectSSE()
  } else {
    console.log('[App] 用户未登录，跳过 SSE 初始化')
    // 如果没有 token，断开连接
    disconnect()
  }
}

// 组件挂载时初始化 SSE 连接
onMounted(() => {
  checkAndConnect()
})

// 监听路由变化，当从登录页跳转到其他页面时建立连接
watch(
  () => route.path,
  (newPath, oldPath) => {
    console.log('[App] 路由变化:', oldPath, '->', newPath)

    // 如果从登录页跳转离开，建立 SSE 连接
    if (oldPath === '/login' || oldPath === '/console/login') {
      if (newPath !== '/login' && newPath !== '/console/login') {
        console.log('[App] 从登录页跳转，建立 SSE 连接')
        // 延迟一点，确保登录流程完成
        setTimeout(() => {
          checkAndConnect()
        }, 500)
      }
    }
  }
)

// 组件卸载时断开连接
onUnmounted(() => {
  console.log('[App] App 卸载，断开 SSE 连接')
  disconnect()
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html,
body,
#app {
  width: 100%;
  height: 100%;
}
</style>
