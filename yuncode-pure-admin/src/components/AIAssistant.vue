<template>
  <div class="ai-assistant">
    <el-tooltip content="AI 助手" placement="left">
      <div class="assistant-btn" @click="handleToggle">
        <el-icon :size="20">
          <ChatDotRound />
        </el-icon>
        <span class="label">AI</span>
      </div>
    </el-tooltip>

    <!-- 浮动面板 -->
    <Teleport to="body">
      <Transition name="panel">
        <div v-if="visible" class="ai-overlay" @click.self="handleClose">
          <div class="ai-panel" @click.stop>
            <!-- 头部 -->
            <div class="panel-header">
              <div class="header-left">
                <div class="ai-avatar">
                  <el-icon :size="18"><MagicStick /></el-icon>
                </div>
                <div class="header-info">
                  <span class="header-title">AI 助手</span>
                  <span class="header-status">
                    <span class="status-dot" />
                    在线
                  </span>
                </div>
              </div>
              <div class="header-actions">
                <el-tooltip content="清空对话" placement="top">
                  <el-button text circle size="small" @click="handleClear">
                    <el-icon :size="16"><Delete /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-button text circle size="small" @click="handleClose">
                  <el-icon :size="16"><Close /></el-icon>
                </el-button>
              </div>
            </div>

            <!-- 聊天内容 -->
            <div class="chat-wrapper">
              <div ref="chatContainerRef" class="chat-container">
                <template v-if="messages.length > 0">
                  <div
                    v-for="(msg, index) in messages"
                    :key="index"
                    class="message-row"
                    :class="[msg.role]"
                  >
                    <div v-if="msg.role === 'assistant'" class="msg-avatar">
                      <el-icon :size="14"><MagicStick /></el-icon>
                    </div>
                    <div class="msg-bubble">
                      <div class="msg-content" v-html="msg.content" />
                      <div class="msg-time">{{ msg.time }}</div>
                    </div>
                    <div v-if="msg.role === 'user'" class="msg-avatar user-avatar">
                      <el-icon :size="14"><User /></el-icon>
                    </div>
                  </div>
                </template>

                <!-- 空状态 -->
                <div v-else class="empty-state">
                  <div class="empty-icon">
                    <el-icon :size="40"><ChatDotRound /></el-icon>
                  </div>
                  <p class="empty-title">你好，我是 AI 助手</p>
                  <p class="empty-desc">有什么可以帮你的？试试下面的问题</p>
                  <div class="quick-actions">
                    <div
                      v-for="(q, i) in quickQuestions"
                      :key="i"
                      class="quick-item"
                      @click="handleQuickAsk(q)"
                    >
                      <span class="quick-text">{{ q }}</span>
                      <el-icon :size="12"><Right /></el-icon>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 输入区域 -->
              <div class="input-area">
                <div class="input-wrapper">
                  <el-input
                    v-model="inputText"
                    type="textarea"
                    :autosize="{ minRows: 1, maxRows: 4 }"
                    placeholder="输入你的问题..."
                    resize="none"
                    @keydown.enter.exact.prevent="handleSend"
                  />
                  <div class="input-actions">
                    <span class="char-count" :class="{ warn: inputText.length > 500 }">
                      {{ inputText.length > 0 ? `${inputText.length}` : '' }}
                    </span>
                    <el-button
                      type="primary"
                      circle
                      size="small"
                      :disabled="!inputText.trim() || sending"
                      :loading="sending"
                      @click="handleSend"
                    >
                      <el-icon v-if="!sending" :size="14"><Promotion /></el-icon>
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, watch } from 'vue';
import {
  ChatDotRound,
  MagicStick,
  User,
  Close,
  Delete,
  Promotion,
  Right
} from '@element-plus/icons-vue';

interface Message {
  role: 'user' | 'assistant';
  content: string;
  time: string;
}

const visible = ref(false);
const inputText = ref('');
const sending = ref(false);
const messages = ref<Message[]>([]);
const chatContainerRef = ref<HTMLDivElement>();

const quickQuestions = [
  '如何创建一个新表单？',
  '帮我设计数据库表结构',
  '如何配置审批流程？',
  '解释一下这个错误'
];

const now = () => {
  const d = new Date();
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
};

const scrollToBottom = () => {
  nextTick(() => {
    if (chatContainerRef.value) {
      chatContainerRef.value.scrollTop = chatContainerRef.value.scrollHeight;
    }
  });
};

watch(messages, () => scrollToBottom(), { deep: true });

const handleToggle = () => {
  visible.value = !visible.value;
};

const handleOpen = () => {
  visible.value = true;
};

const handleClose = () => {
  visible.value = false;
};

const handleClear = () => {
  messages.value = [];
};

const addMessage = (role: 'user' | 'assistant', content: string) => {
  messages.value.push({ role, content, time: now() });
};

const handleQuickAsk = (question: string) => {
  inputText.value = question;
  handleSend();
};

const handleSend = async () => {
  const text = inputText.value.trim();
  if (!text || sending.value) return;

  addMessage('user', text);
  inputText.value = '';
  sending.value = true;

  // Simulate AI response
  await new Promise(r => setTimeout(r, 800 + Math.random() * 1200));

  const responses: Record<string, string> = {
    '如何创建一个新表单？': '要创建新表单，请进入 <b>应用开发 → 业务建模</b>，点击"新建业务表单"。你可以在表单设计器中拖拽添加字段、配置校验规则和布局。',
    '帮我设计数据库表结构': '我可以帮你设计表结构。请告诉我：<br/>1. 表的用途是什么？<br/>2. 需要哪些字段？<br/>3. 字段之间有什么关联关系？',
    '如何配置审批流程？': '配置审批流程的步骤：<br/>1. 进入 <b>应用开发 → 业务建模</b><br/>2. 选择"新建业务流程"<br/>3. 在流程设计器中添加审批节点<br/>4. 设置流转条件和审批人',
    '解释一下这个错误': '请把具体的错误信息发给我，我来帮你分析原因并提供解决方案。'
  };

  const reply = responses[text] || `收到你的问题："${text}"。<br/><br/>这个问题我需要更多信息才能准确回答，你可以提供以下内容：<br/>• 具体的报错信息<br/>• 操作步骤<br/>• 相关截图`;

  addMessage('assistant', reply);
  sending.value = false;
};
</script>

<style scoped lang="scss">
.ai-assistant {
  position: fixed;
  top: 50%;
  right: 0;
  z-index: 2000;
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
}

/* 遮罩层 */
.ai-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(0, 0, 0, 0.12);
  display: flex;
  justify-content: flex-end;
  align-items: stretch;
}

/* 浮动面板 */
.ai-panel {
  width: 420px;
  height: 100%;
  background: #fff;
  display: flex;
  flex-direction: column;
  box-shadow: -4px 0 24px rgba(0, 0, 0, 0.1);
}

/* 进出场动画 */
.panel-enter-active {
  transition: opacity 0.2s ease;
  .ai-panel {
    transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  }
}

.panel-leave-active {
  transition: opacity 0.18s ease;
  .ai-panel {
    transition: transform 0.18s cubic-bezier(0.4, 0, 0.6, 1);
  }
}

.panel-enter-from {
  opacity: 0;
  .ai-panel {
    transform: translateX(100%);
  }
}

.panel-leave-to {
  opacity: 0;
  .ai-panel {
    transform: translateX(100%);
  }
}

/* 头部 */
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .ai-avatar {
    width: 36px;
    height: 36px;
    border-radius: 12px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
  }

  .header-info {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .header-title {
    font-size: 15px;
    font-weight: 600;
    color: #1a1a2e;
  }

  .header-status {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: #8b8fa3;
  }

  .status-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #52c41a;
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 4px;
  }
}

/* 聊天主体 */
.chat-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.chat-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  scroll-behavior: smooth;
  min-height: 0;

  &::-webkit-scrollbar {
    width: 4px;
  }
  &::-webkit-scrollbar-thumb {
    background: #d9d9d9;
    border-radius: 4px;
  }
}

/* 消息行 */
.message-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 16px;
  animation: msgIn 0.25s ease;

  &.user {
    flex-direction: row-reverse;
  }

  .msg-avatar {
    flex-shrink: 0;
    width: 28px;
    height: 28px;
    border-radius: 10px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;

    &.user-avatar {
      background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
    }
  }

  .msg-bubble {
    max-width: 78%;

    .msg-content {
      padding: 10px 14px;
      border-radius: 14px;
      font-size: 13.5px;
      line-height: 1.6;
      word-break: break-word;
      color: #333;

      :deep(b) {
        font-weight: 600;
        color: #1a1a2e;
      }
    }

    .msg-time {
      font-size: 11px;
      color: #bbb;
      margin-top: 4px;
    }
  }

  &.assistant .msg-bubble .msg-content {
    background: #f4f5f7;
    border-top-left-radius: 4px;
  }

  &.user .msg-bubble {
    text-align: right;

    .msg-content {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: #fff;
      border-top-right-radius: 4px;
    }

    .msg-time {
      text-align: right;
    }
  }
}

@keyframes msgIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;

  .empty-icon {
    width: 72px;
    height: 72px;
    border-radius: 20px;
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    color: #667eea;
    margin-bottom: 20px;
  }

  .empty-title {
    font-size: 17px;
    font-weight: 600;
    color: #1a1a2e;
    margin-bottom: 6px;
  }

  .empty-desc {
    font-size: 13px;
    color: #8b8fa3;
    margin-bottom: 24px;
  }

  .quick-actions {
    width: 100%;
    max-width: 320px;
  }

  .quick-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 10px 14px;
    background: #f8f9fb;
    border: 1px solid #eef0f3;
    border-radius: 10px;
    margin-bottom: 8px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      background: #eff1f5;
      border-color: #667eea;
    }

    .quick-text {
      font-size: 13px;
      color: #4a4a5a;
    }

    .el-icon {
      color: #667eea;
      opacity: 0;
      transition: opacity 0.2s;
    }

    &:hover .el-icon {
      opacity: 1;
    }
  }
}

/* 输入区域 */
.input-area {
  padding: 12px 16px 16px;
  border-top: 1px solid #f0f0f0;
  background: #fff;
  flex-shrink: 0;

  .input-wrapper {
    display: flex;
    align-items: flex-end;
    gap: 8px;
    padding: 8px 12px;
    border: 1.5px solid #e8e8e8;
    border-radius: 14px;
    transition: border-color 0.2s;

    &:focus-within {
      border-color: #667eea;
      box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.08);
    }

    :deep(.el-textarea__inner) {
      border: none !important;
      box-shadow: none !important;
      padding: 2px 0;
      background: transparent;
      font-size: 13.5px;
      line-height: 1.5;
    }

    .input-actions {
      display: flex;
      align-items: center;
      gap: 6px;
      flex-shrink: 0;
      padding-bottom: 1px;
    }

    .char-count {
      font-size: 11px;
      color: #bbb;
      min-width: 16px;
      text-align: right;

      &.warn {
        color: #f56c6c;
      }
    }
  }
}
</style>
