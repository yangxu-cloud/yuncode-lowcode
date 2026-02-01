import { onUnmounted, ref } from 'vue'
import { ElNotification } from 'element-plus'
import { useUserStoreHook } from '@/store/modules/user'
import { yuncodeConfig } from '@/config'
import Cookies from 'js-cookie'
import { getToken, getSessionKey } from '@/utils/auth'

/**
 * SSE 踢出通知管理
 * 用于接收和显示被管理员踢出下线的通知
 *
 * 基于 sessionId 的会话隔离方案：
 * - Cookie key 使用后端返回的 sessionId（UUID），确保每个会话独立
 * - sessionStorage 存储当前会话的用户信息、token 和 sessionId
 * - SSE 只处理当前标签页的登录用户
 * - 与后端 Sa-Token + Redis 会话机制完美匹配
 */
export function useKickOutNotification() {
  const userStore = useUserStoreHook()
  let eventSource: EventSource | null = null

  // 倒计时
  const countdown = ref(5)
  const showKickOutDialog = ref(false)
  const kickOutData = ref({
    message: '',
    reason: '',
    countdown: 5
  })

  /**
   * 获取当前登录用户的 token 和 sessionId
   * 从 sessionStorage 获取当前会话信息
   */
  const getCurrentAuthInfo = (): { token: string | null; sessionId: string | null } => {
    const tokenData = getToken()
    if (!tokenData || !tokenData.accessToken) {
      console.warn('[SSE] 未找到 token')
      return { token: null, sessionId: null }
    }

    const token = tokenData.accessToken
    const sessionId = tokenData.sessionId

    if (!sessionId) {
      console.warn('[SSE] 未找到 sessionId')
      return { token: null, sessionId: null }
    }

    const sessionKey = getSessionKey(sessionId)
    const cookieData = Cookies.get(sessionKey)

    if (!cookieData) {
      console.warn(`[SSE] Cookie 中未找到 session，key: ${sessionKey}`)
      return { token: null, sessionId: null }
    }

    try {
      console.log(`[SSE] 成功获取认证信息，sessionId: ${sessionId}`)
      console.log(`[SSE] token (前32位): ${token.substring(0, 32)}`)
      return { token, sessionId }
    } catch (e) {
      console.error('[SSE] 解析认证数据失败:', e)
      return { token: null, sessionId: null }
    }
  }

  /**
   * 建立 SSE 连接
   */
  const connectSSE = () => {
    const { token, sessionId } = getCurrentAuthInfo()

    if (!token || !sessionId) {
      console.warn('[SSE] 未找到 token 或 sessionId，无法建立连接')
      return
    }

    console.log(`[SSE] 使用当前会话的 sessionId: ${sessionId}`)
    console.log(`[SSE] 使用当前会话的 token (前32位): ${token.substring(0, 32)}`)
    console.log('[SSE] 正在建立连接...')

    // 关闭旧连接（如果存在）
    if (eventSource) {
      console.log('[SSE] 关闭旧连接')
      eventSource.close()
      eventSource = null
    }

    try {
      // 通过 URL 参数传递 token 和 sessionId
      const url = `/api/user/notifications?${yuncodeConfig.tokenName}=${encodeURIComponent(token)}&sessionId=${encodeURIComponent(sessionId)}`
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
          eventSource.close()
          eventSource = null
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
  const handleLogout = () => {
    console.log('[SSE] 开始退出登录...')

    // 关闭对话框
    showKickOutDialog.value = false

    // ⚠️ 先关闭 SSE 连接，防止重连
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }

    // 调用退出登录
    // userStore.logOut() 会调用 removeToken()
    // removeToken() 会清除 sessionStorage 中的所有登录信息（token、userInfo、loginType）
    // sessionStorage 是标签页隔离的，不会影响其他标签页的登录状态
    userStore.logOut()
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
