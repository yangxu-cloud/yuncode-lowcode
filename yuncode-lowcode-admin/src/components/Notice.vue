<template>
  <div class="notice-container">
    <el-dropdown trigger="click" placement="bottom-end" @command="handleCommand">
      <el-badge :value="noticeCount" :hidden="noticeCount === 0" :max="99">
        <el-tooltip :content="t('menus.notice')" placement="bottom" effect="light">
          <div class="notice-icon" @click="showNotice">
            <el-icon :size="18">
              <Bell />
            </el-icon>
          </div>
        </el-tooltip>
      </el-badge>
      <template #dropdown>
        <el-dropdown-menu class="notice-dropdown">
          <!-- 通知选项卡 -->
          <el-tabs v-model="activeTab" class="notice-tabs">
            <!-- 通知 -->
            <el-tab-pane label="通知" name="notice">
              <el-scrollbar max-height="400px">
                <div class="notice-list">
                  <div
                    v-for="item in noticeList"
                    :key="item.id"
                    class="notice-item"
                    :class="{ 'unread': !item.isRead }"
                    @click="handleNoticeClick(item)"
                  >
                    <el-icon class="notice-icon" :color="item.color">
                      <component :is="item.icon" />
                    </el-icon>
                    <div class="notice-content">
                      <div class="notice-title">{{ item.title }}</div>
                      <div class="notice-desc">{{ item.description }}</div>
                      <div class="notice-time">{{ item.time }}</div>
                    </div>
                    <el-icon v-if="!item.isRead" class="unread-dot" color="#f56c6c">
                      <CircleFilled />
                    </el-icon>
                  </div>
                  <el-empty v-if="noticeList.length === 0" description="暂无通知" :image-size="100" />
                </div>
              </el-scrollbar>
            </el-tab-pane>

            <!-- 消息 -->
            <el-tab-pane label="消息" name="message">
              <el-scrollbar max-height="400px">
                <div class="notice-list">
                  <div
                    v-for="item in messageList"
                    :key="item.id"
                    class="notice-item"
                    @click="handleMessageClick(item)"
                  >
                    <el-avatar :size="40" :src="item.avatar" />
                    <div class="message-content">
                      <div class="message-title">{{ item.title }}</div>
                      <div class="message-desc">{{ item.description }}</div>
                      <div class="message-time">{{ item.time }}</div>
                    </div>
                  </div>
                  <el-empty v-if="messageList.length === 0" description="暂无消息" :image-size="100" />
                </div>
              </el-scrollbar>
            </el-tab-pane>

            <!-- 待办 -->
            <el-tab-pane label="待办" name="todo">
              <el-scrollbar max-height="400px">
                <div class="todo-list">
                  <div
                    v-for="item in todoList"
                    :key="item.id"
                    class="todo-item"
                    @click="handleTodoClick(item)"
                  >
                    <el-checkbox v-model="item.completed" @change="handleTodoChange(item)">
                      <span class="todo-title">{{ item.title }}</span>
                    </el-checkbox>
                    <div class="todo-time">{{ item.time }}</div>
                  </div>
                  <el-empty v-if="todoList.length === 0" description="暂无待办" :image-size="100" />
                </div>
              </el-scrollbar>
            </el-tab-pane>
          </el-tabs>

          <!-- 底部操作 -->
          <div class="notice-footer">
            <el-button link @click="markAllRead">全部标为已读</el-button>
            <el-button link @click="clearAll">清空所有</el-button>
          </div>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage } from "element-plus";

const { t } = useI18n();

const activeTab = ref("notice");

// 模拟数据（实际应从后端获取）
const noticeList = ref([
  {
    id: 1,
    title: "系统更新通知",
    description: "系统将于今晚 22:00 进行维护升级，预计耗时30分钟",
    icon: "Bell",
    color: "#409eff",
    time: "5分钟前",
    isRead: false
  },
  {
    id: 2,
    title: "安全警告",
    description: "您的密码已超过90天未修改，建议及时修改",
    icon: "Warning",
    color: "#e6a23c",
    time: "1小时前",
    isRead: false
  },
  {
    id: 3,
    title: "欢迎使用系统",
    description: "欢迎使用 Yuncode 低代码平台",
    icon: "CircleCheck",
    color: "#67c23a",
    time: "昨天",
    isRead: true
  }
]);

const messageList = ref([
  {
    id: 1,
    title: "张三",
    description: "您好，关于项目进度的问题...",
    avatar: "https://cube.elemecdn.com/0/88/76b84f4c431c4e4db3e0.png",
    time: "10:30"
  }
]);

const todoList = ref([
  {
    id: 1,
    title: "完成项目报告",
    time: "今天",
    completed: false
  },
  {
    id: 2,
    title: "审核代码提交",
    time: "明天",
    completed: false
  }
]);

// 未读数量
const noticeCount = computed(() => {
  return noticeList.value.filter(item => !item.isRead).length +
         messageList.value.length +
         todoList.value.filter(item => !item.completed).length;
});

// 点击通知
const handleNoticeClick = (item: any) => {
  item.isRead = true;
  console.log("点击通知:", item);
  // TODO: 处理通知点击逻辑
};

// 点击消息
const handleMessageClick = (item: any) => {
  console.log("点击消息:", item);
  // TODO: 跳转到对应聊天或消息页面
};

// 点击待办
const handleTodoClick = (item: any) => {
  console.log("点击待办:", item);
  // TODO: 跳转到待办详情
};

// 待办状态改变
const handleTodoChange = (item: any) => {
  console.log("待办状态改变:", item);
  // TODO: 更新后端待办状态
};

// 全部标为已读
const markAllRead = () => {
  noticeList.value.forEach(item => item.isRead = true);
  messageList.value = [];
  todoList.value.forEach(item => item.completed = true);
  ElMessage.success("已全部标为已读");
};

// 清空所有
const clearAll = () => {
  noticeList.value = [];
  messageList.value = [];
  todoList.value = [];
  ElMessage.success("已清空所有消息");
};
</script>

<style scoped lang="scss">
.notice-container {
  .notice-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    cursor: pointer;
    border-radius: 4px;
    transition: all 0.3s;

    &:hover {
      background-color: #f0f2f5;
    }
  }
}

:deep(.notice-dropdown) {
  .notice-tabs {
    .el-tabs__nav {
      padding: 0 16px;

      .el-tabs__item {
        padding: 0 16px;
      }
    }

    .el-tabs__content {
      padding: 0;
    }
  }

  .notice-list {
    min-width: 350px;
    max-height: 400px;

    .notice-item,
    .message-item,
    .todo-item {
      position: relative;
      display: flex;
      gap: 12px;
      padding: 12px 16px;
      cursor: pointer;
      border-radius: 6px;
      transition: all 0.2s;

      &:hover {
        background-color: #f0f2f5;
      }

      &.unread {
        background-color: #ecf5ff;
      }
    }

    .notice-icon {
      font-size: 20px;
      margin-top: 2px;
    }

    .unread-dot {
      position: absolute;
      top: 12px;
      left: 6px;
      font-size: 12px;
    }

    .notice-content,
    .message-content {
      flex: 1;
      min-width: 0;

      .notice-title,
      .message-title {
        font-size: 14px;
        font-weight: 500;
        color: #333;
        margin-bottom: 4px;
      }

      .notice-desc,
      .message-desc {
        font-size: 12px;
        color: #666;
        margin-bottom: 4px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .notice-time,
      .message-time,
      .todo-time {
        font-size: 12px;
        color: #909399;
      }
    }

    .todo-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 10px 16px;

      .todo-title {
        font-size: 14px;
        color: #333;
        flex: 1;
      }
    }
  }

  .notice-footer {
    display: flex;
    justify-content: space-between;
    padding: 12px 16px;
    border-top: 1px solid #e6e6e6;
  }
}
</style>
