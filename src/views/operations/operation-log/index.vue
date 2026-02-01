<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">操作日志</span>
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
        <el-form-item label="操作模块">
          <el-input
            v-model="queryParams.module"
            placeholder="请输入操作模块"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="queryParams.status"
            placeholder="请选择状态"
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
        <el-table-column prop="username" label="操作用户" width="120" />
        <el-table-column prop="module" label="操作模块" width="150" />
        <el-table-column prop="operation" label="操作描述" min-width="150" show-overflow-tooltip />
        <el-table-column prop="method" label="请求方法" min-width="250" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP地址" width="140" />
        <el-table-column prop="userAgent" label="浏览器/系统" min-width="150" show-overflow-tooltip />
        <el-table-column prop="executeTime" label="耗时(ms)" width="100" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success">
              成功
            </el-tag>
            <el-tag v-else type="danger">
              失败
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="操作时间" width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleViewDetail(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
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

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="操作日志详情"
      width="900px"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="操作用户">
          {{ detailData.username }}
        </el-descriptions-item>
        <el-descriptions-item label="操作模块">
          {{ detailData.module }}
        </el-descriptions-item>
        <el-descriptions-item label="操作描述" :span="2">
          {{ detailData.operation }}
        </el-descriptions-item>
        <el-descriptions-item label="请求方法" :span="2">
          {{ detailData.method }}
        </el-descriptions-item>
        <el-descriptions-item label="IP地址">
          {{ detailData.ip }}
        </el-descriptions-item>
        <el-descriptions-item label="浏览器/系统">
          {{ detailData.userAgent }}
        </el-descriptions-item>
        <el-descriptions-item label="耗时">
          {{ detailData.executeTime }} ms
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag v-if="detailData.status === 1" type="success">
            成功
          </el-tag>
          <el-tag v-else type="danger">
            失败
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作时间">
          {{ detailData.createdAt }}
        </el-descriptions-item>
        <el-descriptions-item v-if="detailData.params" label="请求参数" :span="2">
          <pre class="code-block">{{ formatJson(detailData.params) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="detailData.errorMsg" label="错误信息" :span="2">
          <pre class="error-block">{{ detailData.errorMsg }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { Search, Refresh } from "@element-plus/icons-vue";
import { getOperationLogList } from "@/api/operations";
import { ElMessage } from "element-plus";

// 查询参数
const queryParams = ref({
  username: "",
  module: "",
  status: "",
  page: 1,
  size: 10
});

const dateRange = ref<[string, string]>([]);

// 数据状态
const loading = ref(false);
const logList = ref([]);
const total = ref(0);

// 详情对话框
const detailDialogVisible = ref(false);
const detailData = ref({
  username: "",
  module: "",
  operation: "",
  method: "",
  ip: "",
  userAgent: "",
  executeTime: 0,
  status: 0,
  createdAt: "",
  params: "",
  errorMsg: ""
});

// 查询日志列表
const handleQuery = () => {
  loading.value = true;

  const params = {
    username: queryParams.value.username || undefined,
    module: queryParams.value.module || undefined,
    status: queryParams.value.status || undefined,
    page: queryParams.value.page,
    size: queryParams.value.size
  };

  getOperationLogList(params).then((res: any) => {
    if (res.code === 200) {
      logList.value = res.data.records || [];
      total.value = res.data.total || 0;
    } else {
      ElMessage.error(res.message || "获取操作日志失败");
    }
  }).catch((error: any) => {
    console.error("获取操作日志失败:", error);
    ElMessage.error("获取操作日志失败");
  }).finally(() => {
    loading.value = false;
  });
};

// 重置查询
const handleReset = () => {
  queryParams.value = {
    username: "",
    module: "",
    status: "",
    page: 1,
    size: 10
  };
  dateRange.value = [];
  handleQuery();
};

// 查看详情
const handleViewDetail = (row: any) => {
  detailData.value = { ...row };
  detailDialogVisible.value = true;
};

// 格式化 JSON
const formatJson = (jsonStr: string) => {
  try {
    return JSON.stringify(JSON.parse(jsonStr), null, 2);
  } catch (e) {
    return jsonStr;
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

.code-block {
  white-space: pre-wrap;
  word-wrap: break-word;
  background: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
  max-height: 200px;
  overflow-y: auto;
  font-size: 12px;
  font-family: "Courier New", monospace;
}

.error-block {
  white-space: pre-wrap;
  word-wrap: break-word;
  background: #fef0f0;
  padding: 10px;
  border-radius: 4px;
  max-height: 200px;
  overflow-y: auto;
  color: #f56c6c;
  font-size: 12px;
}
</style>
