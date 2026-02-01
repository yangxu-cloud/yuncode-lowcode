<template>
  <div class="online-users">
    <div class="page-header">
      <h3>在线用户</h3>
      <div class="stats">
        <el-tag type="success">在线总数: {{ stats.total }}</el-tag>
        <el-tag type="primary">活跃: {{ stats.active }}</el-tag>
        <el-tag type="info">空闲: {{ stats.idle }}</el-tag>
      </div>
    </div>

    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="用户名">
          <el-input
            v-model="searchForm.username"
            placeholder="请输入用户名"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="tenantName" label="租户" width="150" />
        <el-table-column label="IP地址" width="150">
          <template #default="{ row }">
            {{ row.ip }}
            <el-tag size="small" class="ml-2">{{ row.location }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'info'">
              {{ row.status === 'active' ? '活跃' : '空闲' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="登录时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.loginTime) }}
          </template>
        </el-table-column>
        <el-table-column label="最后访问时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.lastAccessTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="120">
          <template #default="{ row }">
            <el-button
              type="danger"
              size="small"
              @click="handleKickOut(row)"
            >
              踢出
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="searchForm.page"
          v-model:page-size="searchForm.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSearch"
          @current-change="handleSearch"
        />
      </div>
    </el-card>

    <div class="batch-actions" v-if="selectedUsers.length > 0">
      <el-button type="danger" @click="handleBatchKickOut">
        批量踢出 ({{ selectedUsers.length }})
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  getOnlineUsers,
  kickOutUser,
  batchKickOutUsers,
  getOnlineUserStats,
  type OnlineUser,
  type OnlineUserStats
} from "@/api/system";
import { getUserInfo } from "@/utils/auth";
import dayjs from "dayjs";

defineOptions({
  name: "OnlineUsers"
});

const loading = ref(false);
const tableData = ref<OnlineUser[]>([]);
const selectedUsers = ref<OnlineUser[]>([]);
const total = ref(0);

const stats = reactive<OnlineUserStats>({
  total: 0,
  active: 0,
  idle: 0
});

const searchForm = reactive({
  page: 1,
  size: 20,
  username: ""
});

let refreshTimer: number | null = null;

// 格式化日期时间
const formatDateTime = (dateTime: string) => {
  return dayjs(dateTime).format("YYYY-MM-DD HH:mm:ss");
};

// 加载数据
const loadData = async () => {
  try {
    loading.value = true;
    const data = await getOnlineUsers(searchForm);
    if (data && data.records) {
      tableData.value = data.records;
      total.value = data.total;
    } else {
      tableData.value = [];
      total.value = 0;
    }
  } catch (error) {
    console.error("加载在线用户失败:", error);
    tableData.value = [];
    total.value = 0;
    ElMessage.error("加载在线用户失败");
  } finally {
    loading.value = false;
  }
};

// 加载统计
const loadStats = async () => {
  try {
    const data = await getOnlineUserStats();
    Object.assign(stats, data);
  } catch (error) {
    console.error("加载统计数据失败:", error);
  }
};

// 搜索
const handleSearch = () => {
  searchForm.page = 1;
  loadData();
};

// 重置
const handleReset = () => {
  searchForm.username = "";
  searchForm.page = 1;
  loadData();
};

// 选择变化
const handleSelectionChange = (users: OnlineUser[]) => {
  selectedUsers.value = users;
};

// 获取当前登录用户的信息
const getCurrentUserInfo = () => {
  return getUserInfo();
};

// 踢出单个用户
const handleKickOut = async (user: OnlineUser) => {
  try {
    // 检查是否要踢出自己
    // 通过比较用户名来判断，而不是 token
    // 因为现在不同登录类型共享同一个 Cookie token key
    const currentUser = getCurrentUserInfo();
    if (currentUser && user.username === currentUser.username) {
      ElMessage.warning("不能踢出当前登录的用户");
      return;
    }

    await ElMessageBox.confirm(
      `确定要踢出用户 ${user.username} 吗？`,
      "警告",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }
    );

    loading.value = true;
    await kickOutUser(user.sessionId);
    ElMessage.success("踢出成功，5秒后自动刷新列表");

    // 5秒后刷新列表（等待后端异步踢出完成）
    setTimeout(() => {
      loadData();
      loadStats();
      loading.value = false;
    }, 5000);
  } catch (error: any) {
    loading.value = false;
    if (error !== "cancel") {
      console.error("踢出用户失败:", error);
      ElMessage.error("踢出用户失败");
    }
  }
};

// 批量踢出
const handleBatchKickOut = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要批量踢出这 ${selectedUsers.value.length} 个用户吗？`,
      "警告",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }
    );

    loading.value = true;
    const sessionIds = selectedUsers.value.map(u => u.sessionId);
    await batchKickOutUsers(sessionIds);
    ElMessage.success("批量踢出成功，5秒后自动刷新列表");

    // 5秒后刷新列表
    setTimeout(() => {
      selectedUsers.value = [];
      loadData();
      loadStats();
      loading.value = false;
    }, 5000);
  } catch (error: any) {
    loading.value = false;
    if (error !== "cancel") {
      console.error("批量踢出失败:", error);
      ElMessage.error("批量踢出失败");
    }
  }
};

// 初始化
onMounted(() => {
  loadData();
  loadStats();

  // 每30秒自动刷新一次数据
  refreshTimer = window.setInterval(() => {
    loadData();
    loadStats();
  }, 30000);
});

// 清理
onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer);
  }
});
</script>

<style lang="scss" scoped>
.online-users {
  padding: 20px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h3 {
      margin: 0;
      font-size: 20px;
      font-weight: 600;
    }

    .stats {
      display: flex;
      gap: 10px;
    }
  }

  .search-card {
    margin-bottom: 20px;
  }

  .table-card {
    .pagination {
      margin-top: 20px;
      display: flex;
      justify-content: flex-end;
    }
  }

  .batch-actions {
    position: fixed;
    bottom: 20px;
    right: 20px;
    z-index: 999;
  }

  .ml-2 {
    margin-left: 8px;
  }
}
</style>
