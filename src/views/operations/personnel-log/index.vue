<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">用户日志</span>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="用户名">
          <el-input
            v-model="queryParams.username"
            placeholder="请输入用户名"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="登录状态">
          <el-select
            v-model="queryParams.status"
            placeholder="请选择登录状态"
            clearable
          >
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 数据表格 -->
      <el-table
        v-loading="loading"
        :data="logList"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="status" label="登录状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success">
              成功
            </el-tag>
            <el-tag v-else type="danger">
              失败
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="loginTime" label="登录时间" width="180" />
        <el-table-column prop="loginLocation" label="登录地点" width="200" />
        <el-table-column prop="logoutTime" label="退出时间" width="180">
          <template #default="{ row }">
            <span>{{ row.logoutTime || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="ipaddr" label="IP地址" width="140" />
        <el-table-column prop="browser" label="浏览器" width="120" />
        <el-table-column prop="os" label="操作系统" width="120" />
        <el-table-column label="在线时长" width="120">
          <template #default="{ row }">
            <span v-if="row.costTime">{{ formatDuration(row.costTime) }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="msg" label="提示消息" min-width="200" show-overflow-tooltip />
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleQuery"
        @current-change="handleQuery"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { Search, Refresh } from "@element-plus/icons-vue";
import { getUserLogList } from "@/api/operations";
import { ElMessage } from "element-plus";

// 查询参数
const queryParams = ref({
  username: "",
  status: undefined,
  page: 1,
  size: 10
});

const dateRange = ref<[string, string]>([]);

// 数据状态
const loading = ref(false);
const logList = ref([]);
const total = ref(0);

// 查询日志列表
const handleQuery = () => {
  loading.value = true;
  const params = {
    username: queryParams.value.username || undefined,
    status: queryParams.value.status,
    page: queryParams.value.page,
    size: queryParams.value.size,
    startTime: dateRange.value?.[0] || undefined,
    endTime: dateRange.value?.[1] || undefined
  };

  getUserLogList(params).then((res: any) => {
    if (res.code === 200) {
      logList.value = res.data.records || [];
      total.value = res.data.total || 0;
    } else {
      ElMessage.error(res.message || "获取用户日志失败");
    }
  }).catch((error: any) => {
    console.error("获取用户日志失败:", error);
    ElMessage.error("获取用户日志失败");
  }).finally(() => {
    loading.value = false;
  });
};

// 重置查询
const handleReset = () => {
  queryParams.value = {
    username: "",
    status: undefined,
    page: 1,
    size: 10
  };
  dateRange.value = [];
  handleQuery();
};

// 格式化在线时长（毫秒转为可读格式）
const formatDuration = (milliseconds: number): string => {
  if (!milliseconds) return '-';

  const seconds = Math.floor(milliseconds / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);

  if (days > 0) {
    return `${days}天${hours % 24}小时`;
  } else if (hours > 0) {
    return `${hours}小时${minutes % 60}分钟`;
  } else if (minutes > 0) {
    return `${minutes}分钟${seconds % 60}秒`;
  } else {
    return `${seconds}秒`;
  }
};

onMounted(() => {
  handleQuery();
});
</script>

<style scoped lang="scss">
.app-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.search-form {
  margin-bottom: 20px;
}

.el-pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
