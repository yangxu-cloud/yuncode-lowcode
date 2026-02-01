<template>
  <div class="ai-assistant">
    <el-tooltip content="AI 助手" placement="left">
      <div class="assistant-btn" @click="handleOpen">
        <el-icon :size="20">
          <ChatDotRound />
        </el-icon>
        <span class="label">AI</span>
      </div>
    </el-tooltip>

    <!-- AI 助手对话框 -->
    <el-drawer
      v-model="visible"
      title="AI 助手"
      direction="rtl"
      size="400px"
      :before-close="handleClose"
    >
      <div class="assistant-content">
        <el-empty description="AI 助手功能开发中..." />
        <div class="coming-soon">
          <p>即将支持：</p>
          <ul>
            <li>智能问答</li>
            <li>代码生成</li>
            <li>问题诊断</li>
            <li>操作指导</li>
          </ul>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ChatDotRound } from '@element-plus/icons-vue';

const visible = ref(false);

const handleOpen = () => {
  visible.value = true;
};

const handleClose = () => {
  visible.value = false;
};
</script>

<style scoped lang="scss">
.ai-assistant {
  position: fixed;
  top: 50%;
  right: 0;
  z-index: 9999;
  transform: translateY(-50%);

  .assistant-btn {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 4px;
    width: 44px;
    height: 80px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 8px 0 0 8px;
    cursor: pointer;
    box-shadow: -2px 0 8px rgba(0, 0, 0, 0.15);
    transition: all 0.3s;
    color: #fff;
    user-select: none;

    &:hover {
      width: 48px;
      box-shadow: -4px 0 12px rgba(0, 0, 0, 0.25);
    }

    &:active {
      transform: scale(0.95);
    }

    .label {
      font-size: 12px;
      font-weight: 600;
      writing-mode: vertical-rl;
      text-orientation: mixed;
      letter-spacing: 2px;
    }
  }

  .assistant-content {
    .coming-soon {
      margin-top: 30px;
      padding: 20px;
      background-color: #f5f7fa;
      border-radius: 8px;

      p {
        font-weight: 600;
        color: #303133;
        margin-bottom: 12px;
      }

      ul {
        list-style: none;
        padding: 0;

        li {
          padding: 8px 0;
          color: #606266;
          display: flex;
          align-items: center;

          &:before {
            content: "•";
            color: #409eff;
            font-weight: bold;
            margin-right: 8px;
          }
        }
      }
    }
  }
}
</style>
