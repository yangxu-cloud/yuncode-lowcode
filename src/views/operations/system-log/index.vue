<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">系统日志</span>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="日志级别">
          <el-select
            v-model="queryParams.level"
            placeholder="请选择日志级别"
            clearable
          >
            <el-option label="DEBUG" value="DEBUG" />
            <el-option label="INFO" value="INFO" />
            <el-option label="WARN" value="WARN" />
            <el-option label="ERROR" value="ERROR" />
          </el-select>
        </el-form-item>
        <el-form-item label="模块">
          <el-input
            v-model="queryParams.module"
            placeholder="请输入模块名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="链路追踪ID">
          <el-input
            v-model="queryParams.traceId"
            placeholder="请输入链路追踪ID"
            clearable
            @keyup.enter="handleQuery"
          />
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
        <el-table-column prop="level" label="日志级别" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.level === 'ERROR'" type="danger">
              ERROR
            </el-tag>
            <el-tag v-else-if="row.level === 'WARN'" type="warning">
              WARN
            </el-tag>
            <el-tag v-else-if="row.level === 'INFO'" type="success">
              INFO
            </el-tag>
            <el-tag v-else type="info">
              DEBUG
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="module" label="模块" width="150" />
        <el-table-column prop="traceId" label="链路追踪ID" width="280">
          <template #default="{ row }">
            <el-button
              v-if="row.traceId"
              type="primary"
              link
              @click="handleViewTrace(row.traceId)"
            >
              {{ row.traceId }}
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="日志信息" min-width="300" show-overflow-tooltip />
        <el-table-column prop="thread" label="线程" width="150" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="时间" width="180" />
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
      title="日志详情"
      width="800px"
    >
      <el-descriptions :column="1" border>
        <el-descriptions-item label="日志级别">
          <el-tag v-if="detailData.level === 'ERROR'" type="danger">
            ERROR
          </el-tag>
          <el-tag v-else-if="detailData.level === 'WARN'" type="warning">
            WARN
          </el-tag>
          <el-tag v-else-if="detailData.level === 'INFO'" type="success">
            INFO
          </el-tag>
          <el-tag v-else type="info">
            DEBUG
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="模块">
          {{ detailData.module }}
        </el-descriptions-item>
        <el-descriptions-item v-if="detailData.traceId" label="链路追踪ID">
          {{ detailData.traceId }}
        </el-descriptions-item>
        <el-descriptions-item label="线程">
          {{ detailData.thread }}
        </el-descriptions-item>
        <el-descriptions-item label="时间">
          {{ detailData.createdAt }}
        </el-descriptions-item>
        <el-descriptions-item label="日志信息">
          <pre>{{ detailData.message }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="detailData.exception" label="异常堆栈">
          <pre class="exception-stack">{{ detailData.exception }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 链路追踪对话框 -->
    <el-dialog
      v-model="traceDialogVisible"
      title="链路追踪"
      width="1200px"
    >
      <el-alert
        title="链路追踪ID"
        :type="'info'"
        :closable="false"
        style="margin-bottom: 20px"
      >
        <template #default>
          <code style="font-size: 14px;">{{ currentTraceId }}</code>
        </template>
      </el-alert>

      <el-table
        v-loading="traceLoading"
        :data="traceLogs"
        border
        stripe
        style="width: 100%"
        :default-sort="{ prop: 'createdAt', order: 'ascending' }"
      >
        <el-table-column prop="level" label="日志级别" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.level === 'ERROR'" type="danger" size="small">
              ERROR
            </el-tag>
            <el-tag v-else-if="row.level === 'WARN'" type="warning" size="small">
              WARN
            </el-tag>
            <el-tag v-else-if="row.level === 'INFO'" type="success" size="small">
              INFO
            </el-tag>
            <el-tag v-else type="info" size="small">
              DEBUG
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="module" label="模块" width="150" />
        <el-table-column prop="spanId" label="Span ID" width="180" />
        <el-table-column prop="message" label="日志信息" min-width="300" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="时间" width="180" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { Search, Refresh } from "@element-plus/icons-vue";
import { getSystemLogList } from "@/api/operations";
import { ElMessage } from "element-plus";

// 查询参数
const queryParams = ref({
  level: "",
  module: "",
  traceId: "",
  page: 1,
  size: 10
});

const dateRange = ref<[string, string] | []>([]);

// 数据状态
const loading = ref(false);
const logList = ref([]);
const total = ref(0);

// 详情对话框
const detailDialogVisible = ref(false);
const detailData = ref({
  level: "",
  module: "",
  traceId: "",
  thread: "",
  createdAt: "",
  message: "",
  exception: ""
});

// 链路追踪对话框
const traceDialogVisible = ref(false);
const traceLoading = ref(false);
const traceLogs = ref([]);
const currentTraceId = ref("");

// 查询日志列表
const handleQuery = () => {
  loading.value = true;

  const params = {
    level: queryParams.value.level,
    module: queryParams.value.module,
    traceId: queryParams.value.traceId || undefined,
    page: queryParams.value.page,
    size: queryParams.value.size,
    startTime: dateRange.value && dateRange.value.length > 0 ? dateRange.value[0] : undefined,
    endTime: dateRange.value && dateRange.value.length > 0 ? dateRange.value[1] : undefined
  };

  getSystemLogList(params).then((res: any) => {
    if (res.code === 200) {
      logList.value = res.data.records || [];
      total.value = res.data.total || 0;
    } else {
      ElMessage.error(res.message || "获取系统日志失败");
    }
  }).catch((error: any) => {
    console.error("获取系统日志失败:", error);
    ElMessage.error("获取系统日志失败");
  }).finally(() => {
    loading.value = false;
  });
};

// 重置查询
const handleReset = () => {
  queryParams.value = {
    level: "",
    module: "",
    traceId: "",
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

// 查看链路追踪
const handleViewTrace = (traceId: string) => {
  currentTraceId.value = traceId;
  traceDialogVisible.value = true;

  // 查询该 traceId 的所有日志
  traceLoading.value = true;
  const params = {
    traceId: traceId,
    page: 1,
    size: 100  // 获取更多日志用于链路追踪
  };

  getSystemLogList(params).then((res: any) => {
    if (res.code === 200) {
      traceLogs.value = res.data.records || [];
    } else {
      ElMessage.error(res.message || "获取链路追踪日志失败");
    }
  }).catch((error: any) => {
    console.error("获取链路追踪日志失败:", error);
    ElMessage.error("获取链路追踪日志失败");
  }).finally(() => {
    traceLoading.value = false;
  });
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

pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  background: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
  max-height: 400px;
  overflow-y: auto;
  font-family: "Courier New", Courier, monospace;
  font-size: 13px;
  line-height: 1.5;
}

.exception-stack {
  color: #f56c6c;
  font-size: 13px;
  max-height: 500px;
  overflow-y: auto;
  background: #fef0f0;
  padding: 15px;
  border-radius: 4px;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: "Courier New", Courier, monospace;
  line-height: 1.6;
}
</style>
