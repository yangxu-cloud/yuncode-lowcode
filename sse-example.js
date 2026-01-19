/**
 * SSE 前端监听示例代码
 * 用于接收系统实时通知，包括踢出通知等
 */

// ==================== 方式一：Vue 3 Composition API ====================

import { onMounted, onUnmounted } from 'vue'

export function useSSENotification() {
  let eventSource = null
  let countdownTimer = null

  // 建立 SSE 连接
  const connectSSE = () => {
    const token = localStorage.getItem('satoken') // 从 localStorage 获取 token

    if (!token) {
      console.warn('未找到 token，无法建立 SSE 连接')
      return
    }

    // 构建 SSE URL（需要带上 token 进行认证）
    const url = `/api/user/notifications?token=${token}`

    eventSource = new EventSource(url)

    // 连接成功
    eventSource.addEventListener('connected', (event) => {
      console.log('SSE 连接已建立:', event.data)
    })

    // 收到踢出通知
    eventSource.addEventListener('kick_out', (event) => {
      const data = JSON.parse(event.data)
      console.log('收到踢出通知:', data)

      // 显示通知弹窗
      showKickOutDialog(data)
    })

    // 收到倒计时更新
    eventSource.addEventListener('kick_out_update', (event) => {
      const data = JSON.parse(event.data)
      console.log('倒计时更新:', data.countdown)

      // 更新弹窗中的倒计时显示
      updateCountdown(data.countdown)
    })

    // 倒计时结束
    eventSource.addEventListener('kick_out_final', (event) => {
      console.log('倒计时结束，即将退出')

      // 关闭弹窗
      closeKickOutDialog()

      // 清除 token 并跳转到登录页
      logout()
    })

    // 收到普通消息
    eventSource.addEventListener('message', (event) => {
      console.log('收到消息:', event.data)
      // 这里可以使用你的 UI 框架的消息提示组件
      // 例如：ElMessage.success(event.data)
    })

    // 连接错误
    eventSource.onerror = (error) => {
      console.error('SSE 连接错误:', error)

      // 如果连接断开，3秒后尝试重连
      if (eventSource.readyState === EventSource.CLOSED) {
        setTimeout(() => {
          console.log('尝试重新建立 SSE 连接')
          connectSSE()
        }, 3000)
      }
    }
  }

  // 显示踢出通知弹窗
  const showKickOutDialog = (data) => {
    // 这里使用你的 UI 框架的弹窗组件
    // 以下是 Element Plus 的示例：

    ElMessageBox.alert(
      `<div style="text-align: center;">
        <p style="font-size: 16px; margin-bottom: 10px;">${data.message}</p>
        <p style="color: #666;">原因：${data.reason}</p>
        <p style="font-size: 24px; color: #f56c6c; font-weight: bold; margin-top: 20px;">
          <span id="countdown">${data.countdown}</span> 秒后将退出
        </p>
      </div>`,
      '系统通知',
      {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '立即退出',
        showCancelButton: false,
        closeOnClickModal: false,
        closeOnPressEscape: false,
        showClose: false,
        beforeClose: () => {
          logout()
        }
      }
    )
  }

  // 更新倒计时显示
  const updateCountdown = (countdown) => {
    const countdownEl = document.getElementById('countdown')
    if (countdownEl) {
      countdownEl.textContent = countdown
    }
  }

  // 关闭弹窗
  const closeKickOutDialog = () => {
    // 使用你的 UI 框架关闭弹窗的方法
    // 例如：ElMessageBox.close()
  }

  // 退出登录
  const logout = () => {
    // 清除 token
    localStorage.removeItem('satoken')

    // 关闭 SSE 连接
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }

    // 跳转到登录页
    window.location.href = '/login'
  }

  // 组件挂载时建立连接
  onMounted(() => {
    connectSSE()
  })

  // 组件卸载时关闭连接
  onUnmounted(() => {
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
    if (countdownTimer) {
      clearInterval(countdownTimer)
    }
  })
}

// ==================== 方式二：原生 JavaScript ====================

class SSENotificationManager {
  constructor() {
    this.eventSource = null
  }

  // 建立 SSE 连接
  connect() {
    const token = localStorage.getItem('satoken')

    if (!token) {
      console.warn('未找到 token，无法建立 SSE 连接')
      return
    }

    const url = `/api/user/notifications?token=${token}`
    this.eventSource = new EventSource(url)

    // 连接成功
    this.eventSource.addEventListener('connected', (event) => {
      console.log('SSE 连接已建立:', event.data)
    })

    // 收到踢出通知
    this.eventSource.addEventListener('kick_out', (event) => {
      const data = JSON.parse(event.data)
      this.handleKickOut(data)
    })

    // 收到倒计时更新
    this.eventSource.addEventListener('kick_out_update', (event) => {
      const data = JSON.parse(event.data)
      this.updateCountdown(data.countdown)
    })

    // 倒计时结束
    this.eventSource.addEventListener('kick_out_final', (event) => {
      console.log('倒计时结束，即将退出')
      this.logout()
    })

    // 连接错误
    this.eventSource.onerror = (error) => {
      console.error('SSE 连接错误:', error)
    }
  }

  // 处理踢出通知
  handleKickOut(data) {
    // 创建通知弹窗
    const dialog = document.createElement('div')
    dialog.id = 'kickout-dialog'
    dialog.innerHTML = `
      <div style="position: fixed; top: 0; left: 0; right: 0; bottom: 0;
                  background: rgba(0,0,0,0.5); display: flex; align-items: center;
                  justify-content: center; z-index: 9999;">
        <div style="background: white; padding: 30px; border-radius: 8px;
                    max-width: 400px; text-align: center; box-shadow: 0 4px 20px rgba(0,0,0,0.2);">
          <h2 style="color: #f56c6c; margin-bottom: 20px;">⚠️ 系统通知</h2>
          <p style="font-size: 16px; margin-bottom: 10px;">${data.message}</p>
          <p style="color: #666; margin-bottom: 20px;">原因：${data.reason}</p>
          <p style="font-size: 24px; color: #f56c6c; font-weight: bold;">
            <span id="countdown">${data.countdown}</span> 秒后将退出
          </p>
          <button onclick="window.sseManager.logout()"
                  style="background: #f56c6c; color: white; border: none;
                         padding: 10px 30px; border-radius: 4px; cursor: pointer;
                         font-size: 16px; margin-top: 20px;">
            立即退出
          </button>
        </div>
      </div>
    `
    document.body.appendChild(dialog)
  }

  // 更新倒计时
  updateCountdown(countdown) {
    const countdownEl = document.getElementById('countdown')
    if (countdownEl) {
      countdownEl.textContent = countdown
    }
  }

  // 退出登录
  logout() {
    // 清除 token
    localStorage.removeItem('satoken')

    // 关闭 SSE 连接
    if (this.eventSource) {
      this.eventSource.close()
      this.eventSource = null
    }

    // 移除弹窗
    const dialog = document.getElementById('kickout-dialog')
    if (dialog) {
      dialog.remove()
    }

    // 跳转到登录页
    window.location.href = '/login'
  }

  // 断开连接
  disconnect() {
    if (this.eventSource) {
      this.eventSource.close()
      this.eventSource = null
    }
  }
}

// 使用示例：
// 在页面加载时建立连接
// const sseManager = new SSENotificationManager()
// sseManager.connect()
// window.sseManager = sseManager // 供全局访问

// ==================== 方式三：React Hooks ====================

import { useEffect, useState, useCallback } from 'react'

export function useSSE() {
  const [isConnected, setIsConnected] = useState(false)
  const [countdown, setCountdown] = useState(null)

  useEffect(() => {
    const token = localStorage.getItem('satoken')
    if (!token) return

    const url = `/api/user/notifications?token=${token}`
    const eventSource = new EventSource(url)

    eventSource.addEventListener('connected', () => {
      setIsConnected(true)
    })

    eventSource.addEventListener('kick_out', (event) => {
      const data = JSON.parse(event.data)
      setCountdown(data.countdown)
      // 显示你的通知组件
    })

    eventSource.addEventListener('kick_out_update', (event) => {
      const data = JSON.parse(event.data)
      setCountdown(data.countdown)
    })

    eventSource.addEventListener('kick_out_final', () => {
      // 退出登录
      localStorage.removeItem('satoken')
      window.location.href = '/login'
    })

    eventSource.onerror = () => {
      setIsConnected(false)
    }

    return () => {
      eventSource.close()
    }
  }, [])

  return { isConnected, countdown }
}

// ==================== 使用说明 ====================

/*
1. 确保在用户登录成功后再建立 SSE 连接
2. 需要在 HTTP 请求头中携带 token（通过 URL 参数或 Header）
3. 后端会在以下情况推送通知：
   - 用户被管理员踢出（kick_out 事件）
   - 倒计时更新（kick_out_update 事件）
   - 倒计时结束（kick_out_final 事件）
   - 其他系统消息（message 事件）

4. 注意事项：
   - SSE 是单向通信（服务器 -> 客户端）
   - 一个页面只能建立一个 SSE 连接到同一个 URL
   - 连接断开会自动重连（需要在前端处理）
   - 跨域问题需要后端配置 CORS

5. 推荐在应用的根组件或主布局组件中建立 SSE 连接
*/
