<template>
  <el-dialog
    v-model="dialogVisible"
    title="系统通知"
    width="450px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    center
    destroy-on-close
  >
    <div class="kick-out-content">
      <!-- 警告图标 -->
      <div class="warning-icon">
        <el-icon :size="70" color="#f56c6c">
          <Warning />
        </el-icon>
      </div>

      <!-- 消息标题 -->
      <h2 class="kick-out-title">{{ kickOutData.message }}</h2>

      <!-- 踢出原因 -->
      <p class="kick-out-reason">原因：{{ kickOutData.reason }}</p>

      <!-- 倒计时 -->
      <div class="countdown-box">
        <div class="countdown-number" :class="{ urgent: countdown <= 3 }">
          {{ countdown }}
        </div>
        <div class="countdown-text">秒后将退出</div>
      </div>
    </div>

    <template #footer>
      <el-button type="danger" size="large" @click="handleLogoutNow" style="width: 100%">
        立即退出
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Warning } from '@element-plus/icons-vue'

interface KickOutData {
  message: string
  reason: string
  countdown: number
}

interface Props {
  visible: boolean
  kickOutData: KickOutData
  countdown: number
}

interface Emits {
  (e: 'logout-now'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const dialogVisible = computed({
  get: () => props.visible,
  set: () => {} // 禁止直接关闭
})

const handleLogoutNow = () => {
  emit('logout-now')
}
</script>

<style scoped lang="scss">
.kick-out-content {
  text-align: center;
  padding: 20px 0;
}

.warning-icon {
  margin-bottom: 25px;
  animation: shake 0.6s ease-in-out infinite;
}

@keyframes shake {
  0%, 100% {
    transform: rotate(0deg) scale(1);
  }
  25% {
    transform: rotate(-10deg) scale(1.05);
  }
  75% {
    transform: rotate(10deg) scale(1.05);
  }
}

.kick-out-title {
  color: #f56c6c;
  font-size: 26px;
  font-weight: bold;
  margin: 0 0 15px 0;
}

.kick-out-reason {
  color: #666;
  font-size: 15px;
  margin: 0 0 30px 0;
}

.countdown-box {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 30px;
  border-radius: 12px;
  margin-bottom: 20px;
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.3);
}

.countdown-number {
  color: white;
  font-size: 64px;
  font-weight: bold;
  line-height: 1;
  margin-bottom: 8px;
  transition: all 0.3s ease;

  &.urgent {
    color: #ffeb3b;
    transform: scale(1.2);
    text-shadow: 0 0 20px rgba(255, 235, 59, 0.8);
  }
}

.countdown-text {
  color: rgba(255, 255, 255, 0.95);
  font-size: 18px;
  font-weight: 500;
}
</style>
