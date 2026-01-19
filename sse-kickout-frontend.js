/**
 * SSE 踢出通知 - 前端实现示例
 * 用于显示踢出通知和5秒倒计时弹出框
 */

// ==================== Vue 3 + Element Plus 完整实现 ====================

<template>
  <div id="app">
    <!-- 你的应用内容 -->
    <router-view />

    <!-- 踢出通知弹窗 -->
    <el-dialog
      v-model="kickOutDialogVisible"
      title="系统通知"
      width="400px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
      center
    >
      <div class="kick-out-content">
        <el-icon class="warning-icon" :size="60" color="#f56c6c">
          <Warning />
        </el-icon>
        <h2>{{ kickOutData.message }}</h2>
        <p class="reason">原因：{{ kickOutData.reason }}</p>
        <div class="countdown">
          <span class="countdown-number">{{ countdown }}</span>
          <span class="countdown-text">秒后将退出</span>
        </div>
      </div>
      <template #footer>
        <el-button type="danger" @click="handleLogoutNow" size="large">
          立即退出
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Warning } from '@element-plus/icons-vue'

const router = useRouter()

// 踢出通知数据
const kickOutDialogVisible = ref(false)
const kickOutData = ref({
  message: '',
  reason: '',
  countdown: 5
})
const countdown = ref(5)

let eventSource = null
let countdownTimer = null

// 建立 SSE 连接
const connectSSE = () => {
  const token = localStorage.getItem('satoken')

  if (!token) {
    console.warn('未找到 token，无法建立 SSE 连接')
    return
  }

  // 构建 SSE URL，通过请求头传递认证信息
  const url = `/api/user/notifications`

  eventSource = new EventSource(url)

  // 连接成功
  eventSource.addEventListener('connected', (event) => {
    console.log('✅ SSE 连接已建立')
  })

  // 收到踢出通知
  eventSource.addEventListener('kick_out', (event) => {
    const data = JSON.parse(event.data)
    console.log('⚠️ 收到踢出通知:', data)

    // 显示踢出弹窗
    kickOutData.value = data
    countdown.value = data.countdown
    kickOutDialogVisible.value = true

    // 显示提示消息
    ElMessage.warning({
      message: data.message,
      duration: 5000,
      showClose: false
    })
  })

  // 收到倒计时更新
  eventSource.addEventListener('kick_out_update', (event) => {
    const data = JSON.parse(event.data)
    console.log('⏱️ 倒计时更新:', data.countdown)

    // 更新倒计时显示
    countdown.value = data.countdown

    // 最后3秒显示紧迫提示
    if (data.countdown <= 3) {
      ElMessage.error({
        message: `${data.countdown} 秒后将强制退出！`,
        duration: 1000,
        showClose: false
      })
    }
  })

  // 倒计时结束
  eventSource.addEventListener('kick_out_final', (event) => {
    console.log('👋 倒计时结束，即将退出')

    // 关闭弹窗
    kickOutDialogVisible.value = false

    // 清理并退出
    cleanupAndLogout()
  })

  // 收到普通消息
  eventSource.addEventListener('message', (event) => {
    console.log('📨 收到消息:', event.data)
    ElMessage.success(event.data)
  })

  // 连接错误
  eventSource.onerror = (error) => {
    console.error('❌ SSE 连接错误:', error)

    // 如果连接断开，尝试重连
    if (eventSource.readyState === EventSource.CLOSED) {
      console.log('🔄 连接已关闭，3秒后尝试重连...')
      setTimeout(() => {
        connectSSE()
      }, 3000)
    }
  }
}

// 清理资源并退出登录
const cleanupAndLogout = () => {
  console.log('🚪 开始退出登录流程')

  // 停止倒计时
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }

  // 关闭 SSE 连接
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }

  // 清除本地存储的认证信息
  localStorage.removeItem('satoken')
  localStorage.removeItem('userInfo')
  sessionStorage.clear()

  // 显示退出提示
  ElMessage.info('已退出登录')

  // 跳转到登录页
  setTimeout(() => {
    router.push('/login')
  }, 500)
}

// 立即退出（用户主动点击按钮）
const handleLogoutNow = () => {
  console.log('👤 用户选择立即退出')
  cleanupAndLogout()
}

// 组件挂载时建立 SSE 连接
onMounted(() => {
  console.log('🔗 初始化 SSE 连接...')
  connectSSE()
})

// 组件卸载时清理资源
onUnmounted(() => {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
})
</script>

<style scoped>
.kick-out-content {
  text-align: center;
  padding: 20px 0;
}

.warning-icon {
  margin-bottom: 20px;
  animation: shake 0.5s ease-in-out infinite;
}

@keyframes shake {
  0%, 100% { transform: rotate(0deg); }
  25% { transform: rotate(-10deg); }
  75% { transform: rotate(10deg); }
}

.kick-out-content h2 {
  color: #f56c6c;
  font-size: 24px;
  margin: 20px 0;
}

.reason {
  color: #666;
  font-size: 14px;
  margin-bottom: 30px;
}

.countdown {
  margin-top: 30px;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 10px;
  color: white;
}

.countdown-number {
  font-size: 48px;
  font-weight: bold;
  display: block;
  margin-bottom: 10px;
}

.countdown-text {
  font-size: 16px;
  display: block;
}
</style>

// ==================== 原生 JavaScript 实现 ====================

class KickOutNotificationManager {
  constructor() {
    this.eventSource = null
    this.dialogElement = null
    this.countdownElement = null
  }

  // 初始化
  init() {
    this.connectSSE()
    this.createDialogTemplate()
  }

  // 创建弹窗 HTML
  createDialogTemplate() {
    const template = `
      <div id="kickout-dialog" style="display: none; position: fixed; top: 0; left: 0; right: 0; bottom: 0;
                  background: rgba(0,0,0,0.6); display: flex; align-items: center;
                  justify-content: center; z-index: 9999; backdrop-filter: blur(5px);">
        <div style="background: white; padding: 40px; border-radius: 16px;
                    max-width: 450px; width: 90%; text-align: center;
                    box-shadow: 0 20px 60px rgba(0,0,0,0.3);
                    animation: slideIn 0.3s ease-out;">
          <div style="font-size: 60px; margin-bottom: 20px; animation: shake 0.5s ease-in-out infinite;">⚠️</div>
          <h2 style="color: #f56c6c; margin-bottom: 15px; font-size: 26px;" id="dialog-message">
            您已被管理员强制下线
          </h2>
          <p style="color: #666; font-size: 15px; margin-bottom: 25px;" id="dialog-reason">
            原因：被管理员踢出
          </p>
          <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                      padding: 25px; border-radius: 12px; margin-bottom: 25px;">
            <div style="color: white; font-size: 56px; font-weight: bold; line-height: 1;"
                 id="countdown-number">5</div>
            <div style="color: rgba(255,255,255,0.9); font-size: 16px; margin-top: 5px;">秒后将退出</div>
          </div>
          <button onclick="window.kickOutManager.logoutNow()"
                  style="background: #f56c6c; color: white; border: none;
                         padding: 14px 40px; border-radius: 8px; cursor: pointer;
                         font-size: 16px; font-weight: 600; transition: all 0.3s;
                         box-shadow: 0 4px 12px rgba(245, 108, 108, 0.3);"
                  onmouseover="this.style.transform='translateY(-2px)'; this.style.boxShadow='0 6px 16px rgba(245, 108, 108, 0.4)'"
                  onmouseout="this.style.transform='translateY(0)'; this.style.boxShadow='0 4px 12px rgba(245, 108, 108, 0.3)'">
            立即退出
          </button>
        </div>
      </div>
      <style>
        @keyframes slideIn {
          from { transform: scale(0.9); opacity: 0; }
          to { transform: scale(1); opacity: 1; }
        }
        @keyframes shake {
          0%, 100% { transform: rotate(0deg); }
          25% { transform: rotate(-10deg); }
          75% { transform: rotate(10deg); }
        }
      </style>
    `
    document.body.insertAdjacentHTML('beforeend', template)
    this.dialogElement = document.getElementById('kickout-dialog')
    this.countdownElement = document.getElementById('countdown-number')
  }

  // 建立 SSE 连接
  connectSSE() {
    const token = localStorage.getItem('satoken')
    if (!token) {
      console.warn('未找到 token')
      return
    }

    this.eventSource = new EventSource('/api/user/notifications')

    this.eventSource.addEventListener('connected', () => {
      console.log('✅ SSE 连接已建立')
    })

    this.eventSource.addEventListener('kick_out', (event) => {
      const data = JSON.parse(event.data)
      console.log('⚠️ 收到踢出通知:', data)
      this.showDialog(data)
    })

    this.eventSource.addEventListener('kick_out_update', (event) => {
      const data = JSON.parse(event.data)
      console.log('⏱️ 倒计时:', data.countdown)
      this.updateCountdown(data.countdown)
    })

    this.eventSource.addEventListener('kick_out_final', () => {
      console.log('👋 倒计时结束')
      this.hideDialog()
      this.logout()
    })

    this.eventSource.onerror = (error) => {
      console.error('❌ SSE 错误:', error)
      if (this.eventSource.readyState === EventSource.CLOSED) {
        setTimeout(() => this.connectSSE(), 3000)
      }
    }
  }

  // 显示弹窗
  showDialog(data) {
    document.getElementById('dialog-message').textContent = data.message
    document.getElementById('dialog-reason').textContent = `原因：${data.reason}`
    this.countdownElement.textContent = data.countdown
    this.dialogElement.style.display = 'flex'

    // 最后3秒时改变颜色提示紧迫
    if (data.countdown <= 3) {
      this.countdownElement.style.color = '#ff0000'
    }
  }

  // 更新倒计时
  updateCountdown(countdown) {
    this.countdownElement.textContent = countdown

    // 最后3秒显示紧迫感
    if (countdown <= 3) {
      this.countdownElement.style.color = '#ff0000'
      this.countdownElement.style.transform = 'scale(1.2)'
    }
  }

  // 隐藏弹窗
  hideDialog() {
    if (this.dialogElement) {
      this.dialogElement.style.display = 'none'
    }
  }

  // 立即退出
  logoutNow() {
    console.log('👤 用户点击立即退出')
    this.logout()
  }

  // 退出登录
  logout() {
    if (this.eventSource) {
      this.eventSource.close()
    }

    localStorage.removeItem('satoken')
    localStorage.removeItem('userInfo')
    sessionStorage.clear()

    window.location.href = '/login'
  }

  // 销毁
  destroy() {
    if (this.eventSource) {
      this.eventSource.close()
    }
    if (this.dialogElement) {
      this.dialogElement.remove()
    }
  }
}

// 使用示例（原生 JS）：
// 在页面加载完成后初始化
// window.kickOutManager = new KickOutNotificationManager()
// window.kickOutManager.init()

// ==================== React + Ant Design 实现 ====================

import { useEffect, useState } from 'react'
import { Modal, message, Button } from 'antd'
import { WarningOutlined } from '@ant-design/icons'

function useKickOutNotification() {
  const [visible, setVisible] = useState(false)
  const [countdown, setCountdown] = useState(5)
  const [data, setData] = useState({
    message: '',
    reason: ''
  })

  useEffect(() => {
    const token = localStorage.getItem('satoken')
    if (!token) return

    const eventSource = new EventSource('/api/user/notifications')

    eventSource.addEventListener('kick_out', (event) => {
      const notification = JSON.parse(event.data)
      setData(notification)
      setCountdown(notification.countdown)
      setVisible(true)
      message.warning(notification.message)
    })

    eventSource.addEventListener('kick_out_update', (event) => {
      const notification = JSON.parse(event.data)
      setCountdown(notification.countdown)

      if (notification.countdown <= 3) {
        message.error(`${notification.countdown} 秒后将强制退出！`)
      }
    })

    eventSource.addEventListener('kick_out_final', () => {
      setVisible(false)
      handleLogout()
    })

    return () => eventSource.close()
  }, [])

  const handleLogout = () => {
    localStorage.removeItem('satoken')
    window.location.href = '/login'
  }

  const KickOutModal = () => (
    <Modal
      open={visible}
      title="系统通知"
      onCancel={() => {}} // 禁止关闭
      footer={null}
      closable={false}
      centered
      width={400}
    >
      <div style={{ textAlign: 'center', padding: '20px 0' }}>
        <WarningOutlined style={{ fontSize: 60, color: '#f56c6c', marginBottom: 20 }} />
        <h2 style={{ color: '#f56c6c', marginBottom: 15 }}>{data.message}</h2>
        <p style={{ color: '#666', marginBottom: 25 }}>原因：{data.reason}</p>
        <div style={{
          background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
          padding: 25,
          borderRadius: 12,
          marginBottom: 25
        }}>
          <div style={{ color: 'white', fontSize: 56, fontWeight: 'bold', lineHeight: 1 }}>
            {countdown}
          </div>
          <div style={{ color: 'rgba(255,255,255,0.9)', fontSize: 16, marginTop: 5 }}>
            秒后将退出
          </div>
        </div>
        <Button
          type="primary"
          danger
          size="large"
          onClick={handleLogout}
          block
        >
          立即退出
        </Button>
      </div>
    </Modal>
  )

  return { KickOutModal, visible }
}

// 在组件中使用：
// function App() {
//   const { KickOutModal } = useKickOutNotification()
//   return (
//     <>
//       <KickOutModal />
//       {/* 你的应用内容 */}
//     </>
//   )
// }

export { useKickOutNotification }
