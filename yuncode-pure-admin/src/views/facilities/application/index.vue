<template>
  <div class="application-management">
    <!-- 标签页导航 -->
    <div class="tabs-header">
      <el-tabs v-model="activeTab" class="main-tabs">
        <el-tab-pane label="应用管理" name="management" />
        <el-tab-pane label="应用安装" name="install" />
        <el-tab-pane label="应用升级" name="upgrade" />
        <el-tab-pane label="应用卸载" name="uninstall" />
        <el-tab-pane label="应用分发" name="distribute" />
        <el-tab-pane label="应用日志" name="logs" />
        <el-tab-pane label="开发小组" name="team" />
      </el-tabs>

      <!-- 右上角帮助按钮 -->
      <div class="header-right">
        <el-button :icon="QuestionFilled" circle size="small" />
        <el-button :icon="Close" circle size="small" />
      </div>
    </div>

    <!-- 标签页内容 -->
    <div class="tabs-content">
      <!-- 应用管理Tab -->
      <div v-if="activeTab === 'management'" class="tab-panel">
        <!-- 头部操作区 -->
        <div class="toolbar">
          <div class="toolbar-left">
            <el-button :icon="Refresh" circle @click="loadApplicationList" />
            <el-button type="primary" :icon="Plus" @click="handleCreate">
              新建应用
            </el-button>
            <span class="installed-count">已安装{{ pagination.total }}个</span>
          </div>
          <div class="toolbar-right">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索本地已安装应用"
              clearable
              style="width: 280px"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>
        </div>

        <!-- 应用列表 -->
        <div class="table-container">
          <el-table
            v-loading="loading"
            :data="applicationList"
            style="width: 100%"
            :header-cell-style="{ background: '#f5f7fa', color: '#606266', fontWeight: '500' }"
          >
            <!-- 应用列 -->
            <el-table-column label="应用" min-width="300">
              <template #default="{ row }">
                <div class="app-cell">
                  <div class="status-dot" :class="getStatusClass(row.status)" />
                  <div class="app-icon-wrapper">
                    <el-icon :size="32">
                      <component :is="getIconComponent(getIconName(row.appIcon))" />
                    </el-icon>
                  </div>
                  <div class="app-info">
                    <div class="app-title">{{ row.appName }}</div>
                    <div class="app-id">{{ row.appId }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>

            <!-- 信息列 -->
            <el-table-column label="信息" min-width="150">
              <template #default="{ row }">
                <div class="info-cell">
                  <span v-if="row.status === 1" class="info-number">{{ getRunningNumber(row) }}</span>
                  <span v-else>正常</span>
                </div>
              </template>
            </el-table-column>

            <!-- 运行列 -->
            <el-table-column label="运行" min-width="200">
              <template #default="{ row }">
                <div class="runtime-cell">
                  {{ getRuntimeText(row) }}
                </div>
              </template>
            </el-table-column>

            <!-- 状态列 -->
            <el-table-column label="状态" width="120" align="center">
              <template #default="{ row }">
                <span class="status-text" :class="getStatusTextClass(row.status)">
                  {{ getStatusText(row.status) }}
                </span>
              </template>
            </el-table-column>

            <!-- 操作列 -->
            <el-table-column label="操作" width="180" align="center" fixed="right">
              <template #default="{ row }">
                <div class="table-actions">
                  <el-button
                    type="primary"
                    size="small"
                    link
                    @click="handleStart(row)"
                    :disabled="row.status === 1"
                  >
                    启动
                  </el-button>
                  <el-button
                    size="small"
                    link
                    @click="handleStop(row)"
                    :disabled="row.status !== 1"
                  >
                    停止
                  </el-button>
                  <el-dropdown @command="(cmd) => handleCommand(cmd, row)">
                    <el-button size="small" link>
                      更多<el-icon class="el-icon--right"><arrow-down /></el-icon>
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="edit">编辑</el-dropdown-item>
                        <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 分页 -->
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="pagination.current"
            v-model:page-size="pagination.size"
            :total="pagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="sizes, prev, pager, next, jumper, total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>

      <!-- 其他Tab的占位内容 -->
      <div v-else class="tab-panel">
        <el-empty description="功能开发中..." />
      </div>
    </div>

    <!-- 应用表单对话框 -->
    <AppFormDialog
      ref="appFormDialogRef"
      @success="handleFormSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  Search,
  Refresh,
  Plus,
  QuestionFilled,
  Close,
  ArrowDown,
  Monitor
} from "@element-plus/icons-vue";
import * as ElementPlusIcons from "@element-plus/icons-vue";
import {
  getApplicationList,
  deleteApplication,
  startApplication,
  stopApplication
} from "@/api/application";
import AppFormDialog from "./components/AppFormDialog.vue";
import dayjs from "dayjs";

/**
 * 应用管理页面（优化版）
 */

// 加载状态
const loading = ref(false);

// Tab激活状态
const activeTab = ref<string>("management");

// 搜索关键词
const searchKeyword = ref("");

// 应用列表
const applicationList = ref<any[]>([]);

// 分页参数
const pagination = reactive({
  current: 1,
  size: 20,
  total: 0
});

// 对话框引用
const appFormDialogRef = ref();

/**
 * 加载应用列表
 */
const loadApplicationList = async () => {
  loading.value = true;
  try {
    const response = await getApplicationList({
      current: pagination.current,
      size: pagination.size,
      appName: searchKeyword.value || undefined
    });
    console.log("API返回完整响应:", response);
    console.log("response.data:", response.data);
    console.log("response.data.records:", response.data?.records);
    applicationList.value = response.data.records || [];
    pagination.total = response.data.total || 0;
    console.log("applicationList.value:", applicationList.value);
    console.log("pagination.total:", pagination.total);
  } catch (error: any) {
    console.error("加载应用列表失败:", error);
    ElMessage.error(error.message || "加载应用列表失败");
  } finally {
    loading.value = false;
  }
};

/**
 * 搜索
 */
const handleSearch = () => {
  pagination.current = 1;
  loadApplicationList();
};

/**
 * 分页大小变化
 */
const handleSizeChange = (size: number) => {
  pagination.size = size;
  pagination.current = 1;
  loadApplicationList();
};

/**
 * 当前页变化
 */
const handleCurrentChange = (current: number) => {
  pagination.current = current;
  loadApplicationList();
};

/**
 * 新建应用
 */
const handleCreate = () => {
  appFormDialogRef.value?.open();
};

/**
 * 启动应用
 */
const handleStart = async (row: any) => {
  try {
    await startApplication(row.id);
    ElMessage.success("启动成功");
    loadApplicationList();
  } catch (error: any) {
    ElMessage.error(error.message || "启动失败");
  }
};

/**
 * 停止应用
 */
const handleStop = async (row: any) => {
  try {
    await stopApplication(row.id);
    ElMessage.success("停止成功");
    loadApplicationList();
  } catch (error: any) {
    ElMessage.error(error.message || "停止失败");
  }
};

/**
 * 处理下拉菜单命令
 */
const handleCommand = async (command: string, row: any) => {
  if (command === "edit") {
    appFormDialogRef.value?.open(row);
  } else if (command === "delete") {
    try {
      await ElMessageBox.confirm(
        `确定要删除应用"${row.appName}"吗？`,
        "提示",
        {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning"
        }
      );

      await deleteApplication(row.id);
      ElMessage.success("删除成功");
      loadApplicationList();
    } catch (error: any) {
      if (error !== "cancel") {
        ElMessage.error(error.message || "删除失败");
      }
    }
  }
};

/**
 * 表单成功
 */
const handleFormSuccess = () => {
  loadApplicationList();
};

/**
 * 获取状态样式类
 */
const getStatusClass = (status: number) => {
  switch (status) {
    case 1: return "active";
    case 2: return "stopped";
    case 3: return "error";
    default: return "inactive";
  }
};

/**
 * 获取状态文字
 */
const getStatusText = (status: number) => {
  switch (status) {
    case 0: return "未运行";
    case 1: return "已启动";
    case 2: return "已停止";
    case 3: return "异常";
    default: return "未知";
  }
};

/**
 * 获取状态文字样式类
 */
const getStatusTextClass = (status: number) => {
  switch (status) {
    case 1: return "status-running";
    case 2: return "status-stopped";
    case 3: return "status-error";
    default: return "status-inactive";
  }
};

/**
 * 获取运行数字（模拟）
 */
const getRunningNumber = (row: any) => {
  // 这里可以根据实际业务逻辑返回数字
  const hash = row.appId.split("").reduce((acc, char) => acc + char.charCodeAt(0), 0);
  return (hash % 999) + 1;
};

/**
 * 获取运行时间文本
 */
const getRuntimeText = (row: any) => {
  if (row.startTime && row.status === 1) {
    const start = dayjs(row.startTime);
    const now = dayjs();
    const diff = now.diff(start);

    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    const seconds = Math.floor((diff % (1000 * 60)) / 1000);

    let text = "";
    if (days > 0) text += `${days}日`;
    if (hours > 0) text += `${hours}时`;
    if (minutes > 0) text += `${minutes}分`;
    if (seconds > 0 && days === 0 && hours === 0) text += `${seconds}秒`;

    return text || "刚刚启动";
  }
  return "-";
};

/**
 * 获取图标名称（支持字符串和对象格式）
 */
const getIconName = (icon: any): string => {
  if (!icon) return "Monitor";
  if (typeof icon === "string") return icon;
  if (typeof icon === "object" && icon.icon) return icon.icon;
  return "Monitor";
};

/**
 * 获取图标组件
 */
const getIconComponent = (iconName: string) => {
  return (ElementPlusIcons as any)[iconName] || Monitor;
};

// 初始化
onMounted(() => {
  loadApplicationList();
});
</script>

<style scoped lang="scss">
.application-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;

  // 标签页头部
  .tabs-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 20px;
    border-bottom: 1px solid #e4e7ed;

    .main-tabs {
      flex: 1;

      :deep(.el-tabs__header) {
        margin: 0;
      }

      :deep(.el-tabs__item) {
        padding: 0 20px;
        height: 56px;
        line-height: 56px;
        font-size: 14px;
      }

      :deep(.el-tabs__nav-wrap::after) {
        display: none;
      }
    }

    .header-right {
      display: flex;
      gap: 8px;
    }
  }

  // 标签页内容
  .tabs-content {
    flex: 1;
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }

  // Tab面板
  .tab-panel {
    flex: 1;
    display: flex;
    flex-direction: column;
    padding: 20px;
    overflow: hidden;
  }

  // 工具栏
  .toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    .toolbar-left {
      display: flex;
      align-items: center;
      gap: 12px;

      .installed-count {
        font-size: 14px;
        color: #909399;
        margin-left: 8px;
      }
    }

    .toolbar-right {
      display: flex;
      gap: 12px;
    }
  }

  // 表格容器
  .table-container {
    flex: 1;
    overflow: auto;

    :deep(.el-table) {
      font-size: 14px;
    }

    :deep(.el-table tr:hover > td) {
      background-color: #f5f7fa;
    }

    // 应用单元格
    .app-cell {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 8px 0;

      .status-dot {
        width: 10px;
        height: 10px;
        border-radius: 50%;
        flex-shrink: 0;
        background: #909399;

        &.active {
          background: #67c23a;
          box-shadow: 0 0 0 2px rgba(103, 194, 58, 0.3);
        }

        &.inactive {
          background: #909399;
        }

        &.stopped {
          background: #e6a23c;
        }

        &.error {
          background: #f56c6c;
        }
      }

      .app-icon-wrapper {
        width: 48px;
        height: 48px;
        display: flex;
        align-items: center;
        justify-content: center;
        border: 1px solid #e4e7ed;
        border-radius: 4px;
        background: #f5f7fa;
        color: #409eff;
        flex-shrink: 0;
      }

      .app-info {
        flex: 1;
        min-width: 0;

        .app-title {
          font-size: 14px;
          color: #303133;
          font-weight: 500;
          margin-bottom: 4px;
        }

        .app-id {
          font-size: 12px;
          color: #909399;
          font-family: "Courier New", monospace;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }
    }

    // 信息单元格
    .info-cell {
      .info-number {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        min-width: 48px;
        height: 28px;
        padding: 0 12px;
        background: linear-gradient(135deg, #ff9800, #f57c00);
        color: #fff;
        border-radius: 14px;
        font-size: 14px;
        font-weight: 600;
      }
    }

    // 运行时间单元格
    .runtime-cell {
      color: #909399;
      font-size: 14px;
    }

    // 状态文字
    .status-text {
      font-size: 14px;

      &.status-running {
        color: #67c23a;
      }

      &.status-stopped {
        color: #e6a23c;
      }

      &.status-error {
        color: #f56c6c;
      }

      &.status-inactive {
        color: #909399;
      }
    }

    // 表格操作
    .table-actions {
      display: flex;
      justify-content: center;
      gap: 4px;
    }
  }

  // 分页容器
  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    padding-top: 20px;
  }
}
</style>
