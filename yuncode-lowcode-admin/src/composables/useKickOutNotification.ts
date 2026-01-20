import { onUnmounted, ref } from 'vue'
import { ElMessageBox, ElNotification } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import config from '@/config'

/**
 * SSE 踢出通知管理
 * 用于接收和显示被管理员踢出下线的通知
 */
export function useKickOutNotification() {
  const router = useRouter()
  const userStore = useUserStore()
  let eventSource: EventSource | null = null
  let countdownTimer: number | null = null

  // 倒计时
  const countdown = ref(5)
  const showKickOutDialog = ref(false)
  const kickOutData = ref({
    message: '',
    reason: '',
    countdown: 5
  })

  /**
   * 获取当前登录用户的 token
   */
  const getCurrentToken = (): string | null => {
    // 尝试从 sessionStorage 获取登录类型
    const loginType = sessionStorage.getItem('activeLoginType') || sessionStorage.getItem('loginType')

    if (loginType) {
      const token = localStorage.getItem(`token_${loginType}`)
      if (token) {
        return token
      }
    }

    // 回退方案：按优先级查找
    const types = ['admin', 'tenant', 'user']
    for (const type of types) {
      const token = localStorage.getItem(`token_${type}`)
      if (token) {
        return token
      }
    }

    return null
  }

  /**
   * 建立 SSE 连接
   */
  const connectSSE = () => {
    const token = getCurrentToken()

    if (!token) {
      console.warn('[SSE] 未找到 token，无法建立连接')
      return
    }

    console.log('[SSE] 正在建立连接...')

    // 关闭旧连接（如果存在）
    if (eventSource) {
      console.log('[SSE] 关闭旧连接')
      eventSource.close()
      eventSource = null
    }

    try {
      // 通过 URL 参数传递 token
      // token 参数名从配置文件读取（与后端 application.yml 中的 sa-token.token-name 一致）
      // 需要 /api 前缀让 Vite 代理拦截并转发到后端
      const url = `/api/user/notifications?${config.tokenName}=${encodeURIComponent(token)}`
      eventSource = new EventSource(url)

      // 连接成功
      eventSource.addEventListener('connected', (event) => {
        console.log('[SSE] ✅ 连接已建立', event.data)
      })

      // 收到踢出通知
      eventSource.addEventListener('kick_out', (event) => {
        console.log('[SSE] ⚠️ 收到踢出通知')
        try {
          const data = JSON.parse(event.data)
          console.log('[SSE] 通知数据:', data)
          handleKickOut(data)
        } catch (e) {
          console.error('[SSE] 解析通知数据失败:', e)
        }
      })

      // 收到倒计时更新
      eventSource.addEventListener('kick_out_update', (event) => {
        try {
          const data = JSON.parse(event.data)
          console.log(`[SSE] ⏱️ 倒计时: ${data.countdown} 秒`)
          updateCountdown(data.countdown)
        } catch (e) {
          console.error('[SSE] 解析倒计时数据失败:', e)
        }
      })

      // 倒计时结束
      eventSource.addEventListener('kick_out_final', (event) => {
        console.log('[SSE] 👋 倒计时结束')
        handleLogout()
      })

      // 连接错误
      eventSource.onerror = (error) => {
        console.error('[SSE] ❌ 连接错误', error)

        // 检查是否是认证错误（401）
        if (eventSource && eventSource.readyState === EventSource.CLOSED) {
          console.warn('[SSE] 连接已关闭，可能是 token 失效，停止重连')
          // 关闭连接并清理
          eventSource.close()
          eventSource = null
          // 不再自动重连，避免无限重试失效的 token
        }
      }
    } catch (error) {
      console.error('[SSE] 创建连接失败:', error)
    }
  }

  /**
   * 处理踢出通知
   */
  const handleKickOut = (data: any) => {
    kickOutData.value = {
      message: data.message || '您已被管理员强制下线',
      reason: data.reason || '被管理员踢出',
      countdown: data.countdown || 5
    }
    countdown.value = data.countdown || 5
    showKickOutDialog.value = true

    // 显示 Element Plus 通知
    ElNotification({
      title: '系统通知',
      message: data.message || '您已被管理员强制下线',
      type: 'warning',
      duration: 5000,
      showClose: false
    })
  }

  /**
   * 更新倒计时
   */
  const updateCountdown = (count: number) => {
    countdown.value = count

    // 最后3秒显示紧迫提示
    if (count <= 3) {
      ElNotification({
        title: '即将退出',
        message: `${count} 秒后将强制退出！`,
        type: 'error',
        duration: 1000,
        showClose: false
      })
    }
  }

  /**
   * 退出登录
   */
  const handleLogout = async () => {
    console.log('[SSE] 开始退出登录...')

    // 关闭对话框
    showKickOutDialog.value = false

    // 清理定时器
    if (countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }

    // ⚠️ 先关闭 SSE 连接，防止重连
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }

    // ⚠️ 立即清除本地 token，防止后续重试使用旧 token
    const loginType = sessionStorage.getItem('activeLoginType') || sessionStorage.getItem('loginType')
    if (loginType) {
      localStorage.removeItem(`token_${loginType}`)
      localStorage.removeItem(`userInfo_${loginType}`)
      console.log('[SSE] 已清除本地 token 和用户信息, type=', loginType)
    }
    sessionStorage.removeItem('activeLoginType')
    sessionStorage.removeItem('loginType')

    // 调用退出登录 API（异步，不阻塞）
    try {
      await userStore.logout()
    } catch (error) {
      console.error('[SSE] 退出登录失败:', error)
    }

    // 跳转到登录页
    console.log('[SSE] 跳转到登录页')
    await router.push('/login')
  }

  /**
   * 立即退出（用户主动点击）
   */
  const logoutNow = () => {
    handleLogout()
  }

  /**
   * 断开连接
   */
  const disconnect = () => {
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
  }

  // 组件卸载时清理
  onUnmounted(() => {
    disconnect()
    if (countdownTimer) {
      clearInterval(countdownTimer)
    }
  })

  return {
    // 状态
    countdown,
    showKickOutDialog,
    kickOutData,

    // 方法
    connectSSE,
    disconnect,
    logoutNow,
    handleLogout,
    handleLogoutNow: logoutNow
  }
}
