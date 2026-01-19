<template>
  <div class="logs-manage">
    <div class="page-header">
      <h3>{{ t('system.logManage') }}</h3>
    </div>

    <el-card>
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 操作日志 -->
        <el-tab-pane :label="t('system.operationLog')" name="operation">
          <el-form :inline="true" :model="operationSearch" class="search-form">
            <el-form-item :label="t('system.username')">
              <el-input v-model="operationSearch.username" clearable />
            </el-form-item>
            <el-form-item :label="t('system.module')">
              <el-input v-model="operationSearch.module" clearable />
            </el-form-item>
            <el-form-item :label="t('system.operation')">
              <el-input v-model="operationSearch.operation" clearable />
            </el-form-item>
            <el-form-item :label="t('system.status')">
              <el-select v-model="operationSearch.status" clearable>
                <el-option label="成功" :value="1" />
                <el-option label="失败" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item :label="t('system.traceId')">
              <el-input v-model="operationSearch.traceId" clearable />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadOperationLogs">
                {{ t('common.search') }}
              </el-button>
              <el-button @click="resetOperationSearch">{{ t('common.reset') }}</el-button>
            </el-form-item>
          </el-form>

          <el-table
            v-loading="operationLoading"
            :data="operationLogs"
            border
            stripe
            max-height="600"
          >
            <el-table-column prop="username" :label="t('system.username')" width="120" />
            <el-table-column prop="module" :label="t('system.module')" width="150" />
            <el-table-column prop="operation" :label="t('system.operation')" width="150" />
            <el-table-column prop="method" :label="t('system.method')" width="200" />
            <el-table-column :label="t('system.ip')" width="150">
              <template #default="{ row }">
                {{ row.ip }}
                <div class="text-xs text-gray">{{ row.location }}</div>
              </template>
            </el-table-column>
            <el-table-column :label="t('system.executeTime')" width="100">
              <template #default="{ row }">
                {{ row.executeTime }}ms
              </template>
            </el-table-column>
            <el-table-column :label="t('system.status')" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                  {{ row.status === 1 ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="traceId" :label="t('system.traceId')" width="250">
              <template #default="{ row }">
                <div class="trace-id-actions">
                  <el-link type="primary" @click="showTraceDetail(row.traceId)">
                    {{ row.traceId }}
                  </el-link>
                  <el-button
                    type="success"
                    size="small"
                    circle
                    @click="openInSkyWalking(row.traceId)"
                    title="在 SkyWalking 中查看"
                  >
                    <el-icon><View /></el-icon>
                  </el-button>
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="t('system.createdAt')" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('common.operation')" fixed="right" width="100">
              <template #default="{ row }">
                <el-button type="primary" size="small" text @click="showDetail(row)">
                  {{ t('common.detail') }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="operationSearch.page"
            v-model:page-size="operationSearch.size"
            :total="operationTotal"
            layout="total, sizes, prev, pager, next"
            @size-change="loadOperationLogs"
            @current-change="loadOperationLogs"
          />
        </el-tab-pane>

        <!-- 系统日志 -->
        <el-tab-pane :label="t('system.systemLog')" name="system">
          <el-form :inline="true" :model="systemSearch" class="search-form">
            <el-form-item :label="t('system.level')">
              <el-select v-model="systemSearch.level" clearable>
                <el-option label="TRACE" value="TRACE" />
                <el-option label="DEBUG" value="DEBUG" />
                <el-option label="INFO" value="INFO" />
                <el-option label="WARN" value="WARN" />
                <el-option label="ERROR" value="ERROR" />
              </el-select>
            </el-form-item>
            <el-form-item :label="t('system.module')">
              <el-input v-model="systemSearch.module" clearable />
            </el-form-item>
            <el-form-item :label="t('system.message')">
              <el-input v-model="systemSearch.message" clearable />
            </el-form-item>
            <el-form-item :label="t('system.traceId')">
              <el-input v-model="systemSearch.traceId" clearable />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadSystemLogs">
                {{ t('common.search') }}
              </el-button>
              <el-button @click="resetSystemSearch">{{ t('common.reset') }}</el-button>
            </el-form-item>
          </el-form>

          <el-table
            v-loading="systemLoading"
            :data="systemLogs"
            border
            stripe
            max-height="600"
          >
            <el-table-column :label="t('system.level')" width="100">
              <template #default="{ row }">
                <el-tag :type="getLevelType(row.level)" size="small">
                  {{ row.level }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="module" :label="t('system.module')" width="150" />
            <el-table-column prop="message" :label="t('system.message')" min-width="300" show-overflow-tooltip />
            <el-table-column prop="traceId" :label="t('system.traceId')" width="200">
              <template #default="{ row }">
                <el-link type="primary" @click="showTraceDetail(row.traceId)">
                  {{ row.traceId }}
                </el-link>
              </template>
            </el-table-column>
            <el-table-column :label="t('system.createdAt')" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('common.operation')" fixed="right" width="100">
              <template #default="{ row }">
                <el-button type="primary" size="small" text @click="showSystemDetail(row)">
                  {{ t('common.detail') }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="systemSearch.page"
            v-model:page-size="systemSearch.size"
            :total="systemTotal"
            layout="total, sizes, prev, pager, next"
            @size-change="loadSystemLogs"
            @current-change="loadSystemLogs"
          />
        </el-tab-pane>

        <!-- 用户日志 -->
        <el-tab-pane :label="t('system.userLog')" name="user">
          <el-form :inline="true" :model="userSearch" class="search-form">
            <el-form-item :label="t('system.username')">
              <el-input v-model="userSearch.username" clearable />
            </el-form-item>
            <el-form-item :label="t('system.status')">
              <el-select v-model="userSearch.status" clearable>
                <el-option label="成功" :value="1" />
                <el-option label="失败" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadUserLogs">
                {{ t('common.search') }}
              </el-button>
              <el-button @click="resetUserSearch">{{ t('common.reset') }}</el-button>
            </el-form-item>
          </el-form>

          <el-table
            v-loading="userLoading"
            :data="userLogs"
            border
            stripe
            max-height="600"
          >
            <el-table-column prop="username" :label="t('system.username')" width="120" />
            <el-table-column prop="tenantName" :label="t('login.tenantCode')" width="150" />
            <el-table-column :label="t('system.ip')" width="150">
              <template #default="{ row }">
                {{ row.ip }}
                <div class="text-xs text-gray">{{ row.location }}</div>
              </template>
            </el-table-column>
            <el-table-column :label="t('system.status')" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                  {{ row.status === 1 ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="failReason" :label="t('system.failReason')" width="200" show-overflow-tooltip />
            <el-table-column :label="t('system.loginTime')" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.loginTime) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('system.logoutTime')" width="180">
              <template #default="{ row }">
                {{ row.logoutTime ? formatDateTime(row.logoutTime) : '-' }}
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="userSearch.page"
            v-model:page-size="userSearch.size"
            :total="userTotal"
            layout="total, sizes, prev, pager, next"
            @size-change="loadUserLogs"
            @current-change="loadUserLogs"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 操作日志详情对话框 -->
    <el-dialog v-model="detailVisible" :title="t('system.logDetail')" width="800px">
      <el-descriptions :column="2" border v-if="currentLog">
        <el-descriptions-item :label="t('system.username')">
          {{ currentLog.username }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('system.module')">
          {{ currentLog.module }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('system.operation')">
          {{ currentLog.operation }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('system.method')">
          {{ currentLog.method }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('system.ip')">
          {{ currentLog.ip }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('system.location')">
          {{ currentLog.location }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('system.executeTime')">
          {{ currentLog.executeTime }}ms
        </el-descriptions-item>
        <el-descriptions-item :label="t('system.status')">
          <el-tag :type="currentLog.status === 1 ? 'success' : 'danger'">
            {{ currentLog.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('system.traceId')" :span="2">
          {{ currentLog.traceId }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('system.params')" :span="2">
          <pre class="json-preview">{{ formatJson(currentLog.params) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item :label="t('system.errorMsg')" :span="2" v-if="currentLog.errorMsg">
          <pre class="error-msg">{{ currentLog.errorMsg }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 系统日志详情对话框 -->
    <el-dialog v-model="systemDetailVisible" :title="t('system.logDetail')" width="800px">
      <el-descriptions :column="2" border v-if="currentSystemLog">
        <el-descriptions-item :label="t('system.level')">
          <el-tag :type="getLevelType(currentSystemLog.level)">
            {{ currentSystemLog.level }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('system.module')">
          {{ currentSystemLog.module }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('system.traceId')" :span="2">
          {{ currentSystemLog.traceId }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('system.spanId')">
          {{ currentSystemLog.spanId }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('system.parentSpanId')" v-if="currentSystemLog.parentSpanId">
          {{ currentSystemLog.parentSpanId }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('system.message')" :span="2">
          {{ currentSystemLog.message }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('system.exception')" :span="2" v-if="currentSystemLog.exception">
          <pre class="error-msg">{{ currentSystemLog.exception }}</pre>
        </el-descriptions-item>
        <el-descriptions-item :label="t('system.stackTrace')" :span="2" v-if="currentSystemLog.stackTrace">
          <pre class="stack-trace">{{ currentSystemLog.stackTrace }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 链路追踪对话框 -->
    <el-dialog v-model="traceVisible" :title="`${t('system.traceDetail')}: ${currentTraceId}`" width="1200px">
      <el-tabs v-model="traceTab">
        <el-tab-pane :label="t('system.operationLog')" name="operation">
          <el-table :data="traceData.operationLogs" border max-height="400">
            <el-table-column prop="module" :label="t('system.module')" width="150" />
            <el-table-column prop="operation" :label="t('system.operation')" width="150" />
            <el-table-column prop="username" :label="t('system.username')" width="120" />
            <el-table-column prop="message" :label="t('system.message')" min-width="200" />
            <el-table-column :label="t('system.createdAt')" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.createdAt) }}
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane :label="t('system.systemLog')" name="system">
          <el-table :data="traceData.systemLogs" border max-height="400">
            <el-table-column :label="t('system.level')" width="100">
              <template #default="{ row }">
                <el-tag :type="getLevelType(row.level)" size="small">
                  {{ row.level }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="module" :label="t('system.module')" width="150" />
            <el-table-column prop="message" :label="t('system.message')" min-width="300" />
            <el-table-column :label="t('system.createdAt')" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.createdAt) }}
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane :label="t('system.userLog')" name="user">
          <el-table :data="traceData.userLogs" border max-height="400">
            <el-table-column prop="username" :label="t('system.username')" width="120" />
            <el-table-column prop="ip" :label="t('system.ip')" width="150" />
            <el-table-column :label="t('system.status')" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 'success' ? 'success' : 'danger'" size="small">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="t('system.loginTime')" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.loginTime) }}
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import {
  getOperationLogs,
  getSystemLogs,
  getUserLogs,
  getLogsByTraceId,
  type OperationLog,
  type SystemLog,
  type UserLog
} from "@/api/log-manage";
import dayjs from "dayjs";
import config from "@/config";

const { t } = useI18n();

const activeTab = ref("operation");
const operationLoading = ref(false);
const systemLoading = ref(false);
const userLoading = ref(false);

const operationLogs = ref<OperationLog[]>([]);
const systemLogs = ref<SystemLog[]>([]);
const userLogs = ref<UserLog[]>([]);

const operationTotal = ref(0);
const systemTotal = ref(0);
const userTotal = ref(0);

const detailVisible = ref(false);
const systemDetailVisible = ref(false);
const traceVisible = ref(false);

const currentLog = ref<OperationLog | null>(null);
const currentSystemLog = ref<SystemLog | null>(null);
const currentTraceId = ref("");
const traceTab = ref("operation");

const traceData = reactive({
  operationLogs: [] as OperationLog[],
  systemLogs: [] as SystemLog[],
  userLogs: [] as UserLog[]
});

const operationSearch = reactive({
  page: 1,
  size: 20,
  username: "",
  module: "",
  operation: "",
  status: "",
  traceId: ""
});

const systemSearch = reactive({
  page: 1,
  size: 20,
  level: "" as any,
  module: "",
  message: "",
  traceId: ""
});

const userSearch = reactive({
  page: 1,
  size: 20,
  username: "",
  status: ""
});

// 加载操作日志
const loadOperationLogs = async () => {
  try {
    operationLoading.value = true;
    const data = await getOperationLogs(operationSearch);
    operationLogs.value = data.records;
    operationTotal.value = data.total;
  } catch (error) {
    console.error("加载操作日志失败:", error);
  } finally {
    operationLoading.value = false;
  }
};

// 加载系统日志
const loadSystemLogs = async () => {
  try {
    systemLoading.value = true;
    const data = await getSystemLogs(systemSearch);
    systemLogs.value = data.records;
    systemTotal.value = data.total;
  } catch (error) {
    console.error("加载系统日志失败:", error);
  } finally {
    systemLoading.value = false;
  }
};

// 加载用户日志
const loadUserLogs = async () => {
  try {
    userLoading.value = true;
    const data = await getUserLogs(userSearch);
    userLogs.value = data.records;
    userTotal.value = data.total;
  } catch (error) {
    console.error("加载用户日志失败:", error);
  } finally {
    userLoading.value = false;
  }
};

// 重置搜索
const resetOperationSearch = () => {
  operationSearch.username = "";
  operationSearch.module = "";
  operationSearch.operation = "";
  operationSearch.status = "";
  operationSearch.traceId = "";
  loadOperationLogs();
};

const resetSystemSearch = () => {
  systemSearch.level = "";
  systemSearch.module = "";
  systemSearch.message = "";
  systemSearch.traceId = "";
  loadSystemLogs();
};

const resetUserSearch = () => {
  userSearch.username = "";
  userSearch.status = "";
  loadUserLogs();
};

// 显示详情
const showDetail = (log: OperationLog) => {
  currentLog.value = log;
  detailVisible.value = true;
};

const showSystemDetail = (log: SystemLog) => {
  currentSystemLog.value = log;
  systemDetailVisible.value = true;
};

// 显示链路追踪
const showTraceDetail = async (traceId: string) => {
  try {
    currentTraceId.value = traceId;
    const data = await getLogsByTraceId(traceId);
    traceData.operationLogs = data.operationLogs;
    traceData.systemLogs = data.systemLogs;
    traceData.userLogs = data.userLogs;
    traceVisible.value = true;
  } catch (error) {
    console.error("加载链路追踪数据失败:", error);
  }
};

// 获取日志级别对应的标签类型
const getLevelType = (level: string) => {
  const typeMap: Record<string, any> = {
    TRACE: "info",
    DEBUG: "info",
    INFO: "",
    WARN: "warning",
    ERROR: "danger"
  };
  return typeMap[level] || "";
};

// 格式化日期时间
const formatDateTime = (date: string) => {
  return dayjs(date).format("YYYY-MM-DD HH:mm:ss");
};

// 格式化JSON
const formatJson = (json: string) => {
  try {
    return JSON.stringify(JSON.parse(json), null, 2);
  } catch {
    return json;
  }
};

// 在 SkyWalking 中打开链路追踪
const openInSkyWalking = (traceId: string) => {
  const skyWalkingUrl = localStorage.getItem("skywalkingUrl") || config.skyWalkingUrl;
  const url = `${skyWalkingUrl}/trace/${traceId}`;
  window.open(url, "_blank");
};

// 切换标签
const handleTabChange = (name: string) => {
  if (name === "operation" && operationLogs.value.length === 0) {
    loadOperationLogs();
  } else if (name === "system" && systemLogs.value.length === 0) {
    loadSystemLogs();
  } else if (name === "user" && userLogs.value.length === 0) {
    loadUserLogs();
  }
};

onMounted(() => {
  loadOperationLogs();
});
</script>

<style scoped lang="scss">
.logs-manage {
  .page-header {
    margin-bottom: 20px;

    h3 {
      margin: 0;
      font-size: 20px;
      font-weight: 500;
      color: #303133;
    }
  }

  .search-form {
    margin-bottom: 20px;
  }

  .json-preview {
    background-color: #f5f7fa;
    padding: 12px;
    border-radius: 4px;
    font-size: 12px;
    max-height: 200px;
    overflow: auto;
  }

  .error-msg,
  .stack-trace {
    background-color: #fef0f0;
    padding: 12px;
    border-radius: 4px;
    font-size: 12px;
    color: #f56c6c;
    max-height: 300px;
    overflow: auto;
    white-space: pre-wrap;
    word-break: break-all;
  }

  .text-xs {
    font-size: 12px;
  }

  .text-gray {
    color: #909399;
  }

  .trace-id-actions {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}
</style>
