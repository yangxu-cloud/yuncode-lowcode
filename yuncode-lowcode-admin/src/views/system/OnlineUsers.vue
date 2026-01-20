<template>
  <div class="online-users">
    <div class="page-header">
      <h3>{{ t('system.onlineUsers') }}</h3>
      <div class="stats">
        <el-tag type="success">在线总数: {{ stats.total }}</el-tag>
        <el-tag type="primary">活跃: {{ stats.active }}</el-tag>
        <el-tag type="info">空闲: {{ stats.idle }}</el-tag>
      </div>
    </div>

    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item :label="t('system.username')">
          <el-input
            v-model="searchForm.username"
            :placeholder="t('system.inputUsername')"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            {{ t('common.search') }}
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            {{ t('common.reset') }}
          </el-button>
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
        <el-table-column prop="username" :label="t('system.username')" width="120" />
        <el-table-column prop="nickname" :label="t('system.nickname')" width="120" />
        <el-table-column prop="tenantName" :label="t('login.tenantCode')" width="150" />
        <el-table-column :label="t('system.ip')" width="150">
          <template #default="{ row }">
            {{ row.ip }}
            <el-tag size="small" class="ml-2">{{ row.location }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('system.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'info'">
              {{ row.status === 'active' ? t('system.active') : t('system.idle') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('system.loginTime')" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.loginTime) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('system.lastAccessTime')" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.lastAccessTime) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('common.operation')" fixed="right" width="120">
          <template #default="{ row }">
            <el-button
              type="danger"
              size="small"
              @click="handleKickOut(row)"
            >
              {{ t('system.kickOut') }}
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
        <el-icon><Delete /></el-icon>
        {{ t('system.batchKickOut', { count: selectedUsers.length }) }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage, ElMessageBox } from "element-plus";
import { Search, Refresh, Delete } from "@element-plus/icons-vue";
import { getOnlineUsers, kickOutUser, batchKickOutUsers, getOnlineUserStats, type OnlineUser } from "@/api/log-manage";
import dayjs from "dayjs";

const { t } = useI18n();

const loading = ref(false);
const tableData = ref<OnlineUser[]>([]);
const selectedUsers = ref<OnlineUser[]>([]);
const total = ref(0);

const stats = reactive({
  total: 0,
  active: 0,
  idle: 0
});

const searchForm = reactive({
  page: 1,
  size: 20,
  username: ""
});

// 加载数据
const loadData = async () => {
  try {
    loading.value = true;
    const data = await getOnlineUsers(searchForm);
    tableData.value = data.records;
    total.value = data.total;
  } catch (error) {
    console.error("加载在线用户失败:", error);
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

// 踢出单个用户
const handleKickOut = async (user: OnlineUser) => {
  try {
    await ElMessageBox.confirm(
      `${t('system.confirmKickOut')}: ${user.username}?`,
      t('common.warning'),
      {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: "warning"
      }
    );

    loading.value = true; // 开始 loading
    await kickOutUser(user.sessionId);
    ElMessage.success(`${t('system.kickOutSuccess')}，5秒后自动刷新列表`);

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
      ElMessage.error(error.message || t('system.kickOutFailed'));
    }
  }
};

// 批量踢出
const handleBatchKickOut = async () => {
  try {
    await ElMessageBox.confirm(
      `${t('system.confirmBatchKickOut')} (${selectedUsers.value.length})?`,
      t('common.warning'),
      {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: "warning"
      }
    );

    loading.value = true;
    const sessionIds = selectedUsers.value.map(u => u.sessionId);
    await batchKickOutUsers(sessionIds);
    ElMessage.success(`${t('system.kickOutSuccess')}，5秒后自动刷新列表`);
    selectedUsers.value = [];

    // 5秒后刷新列表（等待后端异步踢出完成）
    setTimeout(() => {
      loadData();
      loadStats();
      loading.value = false;
    }, 5000);
  } catch (error: any) {
    loading.value = false;
    if (error !== "cancel") {
      console.error("批量踢出失败:", error);
      ElMessage.error(error.message || t('system.kickOutFailed'));
    }
  }
};

// 格式化日期时间
const formatDateTime = (date: string) => {
  return dayjs(date).format("YYYY-MM-DD HH:mm:ss");
};

onMounted(() => {
  loadData();
  loadStats();

  // 每30秒刷新一次数据
  setInterval(() => {
    loadData();
    loadStats();
  }, 30000);
});
</script>

<style scoped lang="scss">
.online-users {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h3 {
      margin: 0;
      font-size: 20px;
      font-weight: 500;
      color: #303133;
    }

    .stats {
      display: flex;
      gap: 12px;
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
    bottom: 30px;
    left: 50%;
    transform: translateX(-50%);
    z-index: 1000;
    padding: 12px 24px;
    background-color: #fff;
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  }
}
</style>
