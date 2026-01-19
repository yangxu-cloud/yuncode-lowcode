/**
 * 踢出通知管理器
 * 通用的 SSE 踢出通知处理模块，可以在任何前端框架中使用
 *
 * 使用方法：
 * 1. 在页面加载后初始化：KickOutManager.init()
 * 2. 或在 Vue/React 组件中调用
 */

class KickOutManager {
  constructor() {
    this.eventSource = null
    this.dialogElement = null
    this.countdownElement = null
    this.isConnected = false
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectDelay = 3000
  }

  /**
   * 初始化 SSE 连接
   */
  init() {
    console.log('[KickOutManager] 初始化踢出通知管理器')
    this.createDialog()
    this.connect()

    // 页面卸载时清理
    window.addEventListener('beforeunload', () => {
      this.destroy()
    })
  }

  /**
   * 创建踢出通知弹窗
   */
  createDialog() {
    // 如果已存在，先移除
    const existing = document.getElementById('kickout-notification-dialog')
    if (existing) {
      existing.remove()
    }

    const dialogHTML = `
      <div id="kickout-notification-dialog" style="display: none;">
        <div style="position: fixed; top: 0; left: 0; right: 0; bottom: 0;
                    background: rgba(0, 0, 0, 0.7); display: flex; align-items: center;
                    justify-content: center; z-index: 99999; backdrop-filter: blur(5px);
                    animation: fadeIn 0.3s ease-out;">
          <div style="background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
                      padding: 45px 40px; border-radius: 20px; max-width: 480px; width: 90%;
                      text-align: center; box-shadow: 0 25px 70px rgba(0,0,0,0.3);
                      animation: slideIn 0.4s ease-out; border: 2px solid #f56c6c;">
            <div style="font-size: 70px; margin-bottom: 25px; animation: shake 0.6s ease-in-out infinite;">
              ⚠️
            </div>
            <h2 id="kickout-title" style="color: #f56c6c; margin-bottom: 18px; font-size: 28px; font-weight: bold;">
              您已被强制下线
            </h2>
            <p id="kickout-message" style="color: #333; font-size: 16px; margin-bottom: 12px;">
              您已被管理员强制下线
            </p>
            <p id="kickout-reason" style="color: #666; font-size: 14px; margin-bottom: 30px;">
              原因：被管理员踢出
            </p>
            <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        padding: 30px; border-radius: 16px; margin-bottom: 30px; box-shadow: 0 10px 30px rgba(102, 126, 234, 0.3);">
              <div id="kickout-countdown" style="color: white; font-size: 64px; font-weight: bold;
                          line-height: 1; margin-bottom: 8px; transition: all 0.3s;">
                5
              </div>
              <div style="color: rgba(255, 255, 255, 0.95); font-size: 18px; font-weight: 500;">
                秒后将自动退出
              </div>
            </div>
            <button id="kickout-logout-btn"
                    style="background: #f56c6c; color: white; border: none; padding: 16px 50px;
                           border-radius: 10px; cursor: pointer; font-size: 17px; font-weight: 600;
                           transition: all 0.3s; box-shadow: 0 6px 20px rgba(245, 108, 108, 0.35);
                           font-family: inherit;"
                    onmouseover="this.style.transform='translateY(-2px)'; this.style.boxShadow='0 8px 25px rgba(245, 108, 108, 0.45)';"
                    onmouseout="this.style.transform='translateY(0)'; this.style.boxShadow='0 6px 20px rgba(245, 108, 108, 0.35)';">
              立即退出
            </button>
          </div>
        </div>
        <style>
          @keyframes fadeIn {
            from { opacity: 0; }
            to { opacity: 1; }
          }
          @keyframes slideIn {
            from { transform: scale(0.85); opacity: 0; }
            to { transform: scale(1); opacity: 1; }
          }
          @keyframes shake {
            0%, 100% { transform: rotate(0deg) scale(1); }
            25% { transform: rotate(-8deg) scale(1.05); }
            50% { transform: rotate(0deg) scale(1); }
            75% { transform: rotate(8deg) scale(1.05); }
          }
          @keyframes pulse {
            0%, 100% { transform: scale(1); }
            50% { transform: scale(1.15); }
          }
        </style>
      </div>
    `

    document.body.insertAdjacentHTML('beforeend', dialogHTML)

    this.dialogElement = document.getElementById('kickout-notification-dialog')
    this.countdownElement = document.getElementById('kickout-countdown')

    // 绑定立即退出按钮
    document.getElementById('kickout-logout-btn').addEventListener('click', () => {
      this.logout()
    })
  }

  /**
   * 建立 SSE 连接
   */
  connect() {
    // 检查是否有 token
    const token = localStorage.getItem('satoken')

    if (!token) {
      console.warn('[KickOutManager] 未找到 token，无法建立 SSE 连接')
      return
    }

    // 关闭旧连接
    if (this.eventSource) {
      this.eventSource.close()
    }

    console.log('[KickOutManager] 正在建立 SSE 连接...')

    try {
      this.eventSource = new EventSource('/api/user/notifications')

      // 连接成功
      this.eventSource.addEventListener('connected', (event) => {
        console.log('[KickOutManager] ✅ SSE 连接已建立', event.data)
        this.isConnected = true
        this.reconnectAttempts = 0
      })

      // 收到踢出通知
      this.eventSource.addEventListener('kick_out', (event) => {
        console.log('[KickOutManager] ⚠️ 收到踢出通知')
        try {
          const data = JSON.parse(event.data)
          console.log('[KickOutManager] 通知数据:', data)
          this.showDialog(data)
        } catch (e) {
          console.error('[KickOutManager] 解析通知数据失败:', e)
        }
      })

      // 收到倒计时更新
      this.eventSource.addEventListener('kick_out_update', (event) => {
        try {
          const data = JSON.parse(event.data)
          console.log(`[KickOutManager] ⏱️ 倒计时更新: ${data.countdown} 秒`)
          this.updateCountdown(data.countdown)
        } catch (e) {
          console.error('[KickOutManager] 解析倒计时数据失败:', e)
        }
      })

      // 倒计时结束
      this.eventSource.addEventListener('kick_out_final', (event) => {
        console.log('[KickOutManager] 👋 倒计时结束，即将退出')
        this.hideDialog()
        setTimeout(() => {
          this.logout()
        }, 500)
      })

      // 普通消息
      this.eventSource.addEventListener('message', (event) => {
        console.log('[KickOutManager] 📨 收到消息:', event.data)
      })

      // 连接错误
      this.eventSource.onerror = (error) => {
        console.error('[KickOutManager] ❌ SSE 连接错误')
        this.isConnected = false

        if (this.eventSource.readyState === EventSource.CLOSED) {
          console.log('[KickOutManager] 连接已关闭，尝试重连...')
          this.reconnect()
        } else if (this.eventSource.readyState === EventSource.CONNECTING) {
          console.log('[KickOutManager] 正在尝试重新连接...')
        }
      }

    } catch (error) {
      console.error('[KickOutManager] 创建 SSE 连接失败:', error)
    }
  }

  /**
   * 重连
   */
  reconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('[KickOutManager] 达到最大重连次数，停止重连')
      return
    }

    this.reconnectAttempts++
    console.log(`[KickOutManager] 第 ${this.reconnectAttempts} 次重连尝试...`)

    setTimeout(() => {
      // 检查是否还有 token
      if (localStorage.getItem('satoken')) {
        this.connect()
      } else {
        console.warn('[KickOutManager] Token 已失效，停止重连')
      }
    }, this.reconnectDelay)
  }

  /**
   * 显示踢出弹窗
   */
  showDialog(data) {
    if (!this.dialogElement) {
      this.createDialog()
    }

    // 更新内容
    const titleEl = document.getElementById('kickout-title')
    const messageEl = document.getElementById('kickout-message')
    const reasonEl = document.getElementById('kickout-reason')

    if (titleEl) titleEl.textContent = '⚠️ 您已被强制下线'
    if (messageEl) messageEl.textContent = data.message || '您已被管理员强制下线'
    if (reasonEl) reasonEl.textContent = `原因：${data.reason || '被管理员踢出'}`

    // 重置倒计时样式
    if (this.countdownElement) {
      this.countdownElement.textContent = data.countdown || 5
      this.countdownElement.style.transform = 'scale(1)'
      this.countdownElement.style.color = 'white'
    }

    // 显示弹窗
    this.dialogElement.style.display = 'block'

    // 播放提示音（如果需要）
    this.playNotificationSound()
  }

  /**
   * 更新倒计时
   */
  updateCountdown(countdown) {
    if (!this.countdownElement) return

    this.countdownElement.textContent = countdown

    // 最后3秒添加紧迫感效果
    if (countdown <= 3) {
      this.countdownElement.style.color = '#ffeb3b'
      this.countdownElement.style.transform = 'scale(1.3)'
      this.countdownElement.style.textShadow = '0 0 20px rgba(255, 235, 59, 0.8)'
    }
  }

  /**
   * 隐藏弹窗
   */
  hideDialog() {
    if (this.dialogElement) {
      this.dialogElement.style.display = 'none'
    }
  }

  /**
   * 播放提示音
   */
  playNotificationSound() {
    try {
      // 创建简单的提示音
      const audioContext = new (window.AudioContext || window.webkitAudioContext)()
      const oscillator = audioContext.createOscillator()
      const gainNode = audioContext.createGain()

      oscillator.connect(gainNode)
      gainNode.connect(audioContext.destination)

      oscillator.frequency.value = 800
      oscillator.type = 'sine'

      gainNode.gain.setValueAtTime(0.3, audioContext.currentTime)
      gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.5)

      oscillator.start(audioContext.currentTime)
      oscillator.stop(audioContext.currentTime + 0.5)
    } catch (e) {
      // 忽略音频播放错误
      console.warn('[KickOutManager] 无法播放提示音:', e)
    }
  }

  /**
   * 退出登录
   */
  logout() {
    console.log('[KickOutManager] 开始退出登录...')

    // 关闭 SSE 连接
    if (this.eventSource) {
      this.eventSource.close()
      this.eventSource = null
    }

    // 清除本地存储
    localStorage.removeItem('satoken')
    localStorage.removeItem('userInfo')
    sessionStorage.clear()

    // 显示退出提示
    this.showToast('已退出登录', 'info')

    // 跳转到登录页
    setTimeout(() => {
      window.location.href = '/login'
    }, 1000)
  }

  /**
   * 显示提示消息（可选）
   */
  showToast(message, type = 'info') {
    // 可以集成第三方提示库，如 Toastify、SweetAlert 等
    console.log(`[KickOutManager] Toast: ${message} (${type})`)
  }

  /**
   * 销毁
   */
  destroy() {
    console.log('[KickOutManager] 销毁管理器')
    if (this.eventSource) {
      this.eventSource.close()
      this.eventSource = null
    }
    if (this.dialogElement) {
      this.dialogElement.remove()
      this.dialogElement = null
    }
  }

  /**
   * 手动断开连接
   */
  disconnect() {
    if (this.eventSource) {
      this.eventSource.close()
      this.eventSource = null
      this.isConnected = false
      console.log('[KickOutManager] 已断开 SSE 连接')
    }
  }

  /**
   * 检查连接状态
   */
  isConnectionActive() {
    return this.isConnected && this.eventSource && this.eventSource.readyState === EventSource.OPEN
  }
}

// 创建全局实例
window.KickOutManager = new KickOutManager()

// 自动初始化（如果页面已加载）
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => {
    // 检查是否有 token
    if (localStorage.getItem('satoken')) {
      window.KickOutManager.init()
    }
  })
} else {
  // 页面已加载完成
  if (localStorage.getItem('satoken')) {
    window.KickOutManager.init()
  }
}

// 导出（支持模块化）
if (typeof module !== 'undefined' && module.exports) {
  module.exports = KickOutManager
}
