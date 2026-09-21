<template>
  <div class="monitor-page">
    <div class="section-title">系统监控</div>

    <!-- Gateway 请求指标 -->
    <div class="card">
      <div class="card-header">
        <el-icon :size="18"><Monitor /></el-icon>
        <span>Gateway 请求统计</span>
        <el-tag size="small" type="info" v-if="lastUpdated">更新于 {{ lastUpdated }}</el-tag>
        <div style="margin-left:auto;display:flex;align-items:center;gap:6px;font-size:13px;font-weight:400;">
          <span>异常阈值：</span>
          <el-input-number v-model="failThreshold" :min="1" :max="100" size="small" style="width:72px;" />
          <span>%</span>
        </div>
      </div>
      <div class="card-body">
        <el-row :gutter="20" v-if="metrics">
          <el-col :span="6">
            <div class="stat-card">
              <div class="stat-value" style="color:#409eff;">{{ metrics._total?.requests || 0 }}</div>
              <div class="stat-label">总请求数</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-card">
              <div class="stat-value" style="color:#67c23a;">{{ metrics._total?.success || 0 }}</div>
              <div class="stat-label">成功</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-card">
              <div class="stat-value" style="color:#f56c6c;">{{ metrics._total?.fail || 0 }}</div>
              <div class="stat-label">失败</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-card">
              <div class="stat-value" style="color:#e6a23c;">{{ metrics._total ? Math.round(metrics._total.requests > 0 ? (metrics._total.fail / metrics._total.requests * 100) : 0) : 0 }}%</div>
              <div class="stat-label">失败率</div>
            </div>
          </el-col>
        </el-row>

        <el-table v-if="pathStats.length > 0" :data="pathStats" stripe style="width: 100%; margin-top: 16px;" :header-cell-style="{ background: '#fafafa', color: '#606266', fontWeight: 500 }">
          <el-table-column prop="path" label="路径" />
          <el-table-column prop="requests" label="请求数" width="100" />
          <el-table-column prop="success" label="成功" width="100" />
          <el-table-column prop="fail" label="失败" width="80" />
          <el-table-column prop="avgDuration" label="平均耗时(ms)" width="130" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.failRate > failThreshold ? 'danger' : 'success'" size="small">
                {{ row.failRate > failThreshold ? '异常' : '正常' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else-if="gatewayError" description="Gateway 服务未启动（端口 9000）" :image-size="80">
          <template #description>
            <p>Gateway 服务未启动</p>
            <p style="font-size:12px;color:#909399;margin-top:4px;">启动命令: mvn spring-boot:run -pl yuncode-gateway -o</p>
          </template>
        </el-empty>
        <el-empty v-else description="暂无请求数据" :image-size="80" />
      </div>
    </div>

    <!-- Prometheus 端点 -->
    <div class="card">
      <div class="card-header">
        <el-icon :size="18"><DataLine /></el-icon>
        <span>Prometheus 端点</span>
      </div>
      <div class="card-body">
        <el-alert title="Prometheus 指标端点" type="info" :closable="false" show-icon>
          <template #default>
            <p>此端点为 Prometheus 采集提供格式化的指标数据，用于搭建监控大盘（Grafana）。</p>
            <div style="margin-top: 8px; padding: 12px; background: #f5f7fa; border-radius: 8px; font-size: 13px;">
              <p style="font-weight:600;margin-bottom:4px;">1. 确认 Gateway 已启动（端口 9000）</p>
              <p style="font-weight:600;margin-bottom:4px;">2. 验证端点：</p>
              <code style="display:block;padding:8px;background:#fff;border-radius:4px;margin:4px 0;">
                curl http://localhost:9000/gateway/actuator/prometheus
              </code>
              <p style="font-weight:600;margin-bottom:4px;margin-top:8px;">3. 在 prometheus.yml 中添加 scrape_config：</p>
              <code style="display:block;padding:8px;background:#fff;border-radius:4px;margin:4px 0;white-space:pre;">
- job_name: 'yuncode-gateway'
  scrape_interval: 15s
  metrics_path: /gateway/actuator/prometheus
  static_configs:
    - targets: ['localhost:9000']
              </code>
            </div>
          </template>
        </el-alert>
      </div>
    </div>

    <!-- HotAppDeployer 状态 -->
    <div class="section-title" style="margin-top: 24px;">应用部署状态</div>
    <div class="card">
      <div class="card-header">
        <el-icon :size="18"><Box /></el-icon>
        <span>HotAppDeployer</span>
      </div>
      <div class="card-body">
        <el-row :gutter="20" v-if="appStatus">
          <el-col :span="8">
            <div class="stat-card">
              <div class="stat-value" style="color:#67c23a;">{{ appStatus.loadedCount }}</div>
              <div class="stat-label">运行中</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="stat-card">
              <div class="stat-value" style="color:#909399;">{{ appStatus.stoppedCount ?? 0 }}</div>
              <div class="stat-label">已停用</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="stat-card">
              <div class="stat-value" style="color:#303133;">{{ (appStatus.loadedCount || 0) + (appStatus.stoppedCount ?? 0) }}</div>
              <div class="stat-label">总计</div>
            </div>
          </el-col>
        </el-row>

        <el-table v-if="appStatus?.appList && appStatus.appList.length > 0" :data="appStatus.appList" stripe style="width: 100%; margin-top: 16px;" :header-cell-style="{ background: '#fafafa', color: '#606266', fontWeight: 500 }">
          <el-table-column prop="appId" label="App ID" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="row.status === 'running' ? 'success' : 'info'" size="small">
                {{ row.status === 'running' ? '运行中' : '已停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="beanCount" label="Bean 数" width="100" />
        </el-table>
        <el-empty v-else-if="appError" description="后端服务未启动" :image-size="80">
          <template #description>
            <p>后端服务未启动</p>
            <p style="font-size:12px;color:#909399;margin-top:4px;">启动命令: mvn spring-boot:run -pl yuncode-admin -o</p>
          </template>
        </el-empty>
        <el-empty v-else description="暂无已部署的应用" :image-size="80" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from "vue";
import { Monitor, DataLine, Box } from "@element-plus/icons-vue";
import { getGatewayMetrics, getAppDeployStatus, type MetricsSnapshot, type AppStatus } from "@/api/monitor";

const metrics = ref<MetricsSnapshot | null>(null);
const appStatus = ref<AppStatus | null>(null);
const lastUpdated = ref("");
const gatewayError = ref(false);
const appError = ref(false);
const failThreshold = ref(10); // 失败率阈值（百分比），超过标记为异常

const pathStats = computed(() => {
  if (!metrics.value) return [];
  return Object.entries(metrics.value)
    .filter(([key]) => key !== "_total")
    .map(([path, data]: [string, any]) => ({
      path,
      requests: data.requests,
      success: data.success,
      fail: data.fail,
      avgDuration: data.avgDuration,
      failRate: data.requests > 0 ? Math.round(data.fail / data.requests * 100) : 0
    }));
});

async function loadData() {
  const [m, a] = await Promise.allSettled([
    getGatewayMetrics().then(d => { gatewayError.value = false; return d; }).catch(e => { gatewayError.value = true; throw e; }),
    getAppDeployStatus().then(d => { appError.value = false; return d; }).catch(e => { appError.value = true; throw e; })
  ]);
  if (m.status === "fulfilled") metrics.value = m.value;
  if (a.status === "fulfilled") appStatus.value = a.value;
  lastUpdated.value = new Date().toLocaleTimeString();
}

let timer: any;
onMounted(() => {
  loadData();
  timer = setInterval(loadData, 15000);
});
onBeforeUnmount(() => clearInterval(timer));
</script>

<style scoped lang="scss">
.monitor-page {
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 110px);
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d2129;
  margin-bottom: 16px;
  padding-left: 10px;
  border-left: 3px solid #409eff;
}

.card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  font-size: 15px;
  font-weight: 600;
  color: #1d2129;
}

.card-body {
  padding: 20px;
}

.stat-card {
  text-align: center;
  padding: 20px;
  background: #fafafa;
  border-radius: 10px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 6px;
}
</style>
