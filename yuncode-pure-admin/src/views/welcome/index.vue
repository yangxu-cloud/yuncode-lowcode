<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, shallowRef, nextTick } from "vue";
import * as echarts from "echarts";
import {
  getDashboardData,
  type StatsCard,
  type OpsRecord,
  type SlaIndicator
} from "@/api/dashboard";

defineOptions({ name: "Welcome" });

const loading = ref(true);
const statsCards = ref<StatsCard[]>([]);
const recentOps = ref<OpsRecord[]>([]);
const slaInfo = ref<SlaIndicator[]>([]);

const onlineChartRef = ref<HTMLElement>();
const appChartRef = ref<HTMLElement>();
const onlineChart = shallowRef<echarts.ECharts>();
const appChart = shallowRef<echarts.ECharts>();

onMounted(async () => {
  await loadData();
  window.addEventListener("resize", handleResize);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize);
  onlineChart.value?.dispose();
  appChart.value?.dispose();
});

function handleResize() {
  onlineChart.value?.resize();
  appChart.value?.resize();
}

async function loadData() {
  loading.value = true;
  try {
    const data = await getDashboardData();
    statsCards.value = data.stats;
    recentOps.value = data.recentOps;
    slaInfo.value = data.slaIndicators;

    await nextTick();
    initOnlineChart(data.onlineTrend.hours, data.onlineTrend.values);
    initAppChart(data.appDistribution);
  } finally {
    loading.value = false;
  }
}

function initOnlineChart(hours: string[], values: number[]) {
  if (!onlineChartRef.value) return;
  onlineChart.value = echarts.init(onlineChartRef.value);
  onlineChart.value.setOption({
    tooltip: { trigger: "axis" },
    grid: { left: "3%", right: "4%", bottom: "3%", top: "10%", containLabel: true },
    xAxis: {
      type: "category",
      data: hours,
      axisLine: { lineStyle: { color: "#e0e0e0" } },
      axisLabel: { color: "#909399", fontSize: 11 }
    },
    yAxis: {
      type: "value",
      axisLine: { show: false },
      splitLine: { lineStyle: { color: "#f5f5f5" } },
      axisLabel: { color: "#909399", fontSize: 11 }
    },
    series: [{
      type: "line",
      data: values,
      smooth: true,
      symbol: "none",
      lineStyle: { color: "#409eff", width: 2 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: "rgba(64,158,255,0.3)" },
          { offset: 1, color: "rgba(64,158,255,0.02)" }
        ])
      }
    }]
  });
}

function initAppChart(distribution: { name: string; value: number; color: string }[]) {
  if (!appChartRef.value) return;
  appChart.value = echarts.init(appChartRef.value);
  appChart.value.setOption({
    tooltip: { trigger: "item" },
    legend: { bottom: "0%", textStyle: { color: "#606266", fontSize: 12 } },
    series: [{
      type: "pie",
      radius: ["45%", "70%"],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: "#fff", borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: "bold" } },
      data: distribution.map(item => ({
        value: item.value,
        name: item.name,
        itemStyle: { color: item.color }
      }))
    }]
  });
}

function getStatusType(status: string) {
  const map: Record<string, string> = { success: "success", warning: "warning", danger: "danger" };
  return map[status] || "info";
}

function getOpsTypeTag(type: string) {
  const map: Record<string, { label: string; color: string }> = {
    deploy: { label: "部署", color: "#409eff" },
    config: { label: "配置", color: "#e6a23c" },
    user: { label: "用户", color: "#67c23a" },
    data: { label: "数据", color: "#909399" },
    backup: { label: "备份", color: "#f56c6c" }
  };
  return map[type] || { label: type, color: "#909399" };
}
</script>

<template>
  <div v-loading="loading" class="welcome-container">
    <!-- 第一区块：统计卡片 -->
    <div class="section-title">数据概览</div>
    <div class="stats-grid">
      <div v-for="item in statsCards" :key="item.title" class="stat-card">
        <div class="stat-icon" :style="{ background: item.bgColor, color: item.color }">
          {{ item.icon }}
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ item.value }}</div>
          <div class="stat-label">{{ item.title }}</div>
        </div>
      </div>
    </div>

    <!-- 图表行 -->
    <div class="charts-row">
      <div class="chart-card">
        <div class="card-header">今日在线趋势</div>
        <div ref="onlineChartRef" class="chart-body" />
      </div>
      <div class="chart-card">
        <div class="card-header">应用分布</div>
        <div ref="appChartRef" class="chart-body" />
      </div>
    </div>

    <!-- 第二区块：最近运维信息 -->
    <div class="section-title">最近运维</div>
    <div class="card">
      <el-table :data="recentOps" stripe style="width: 100%" :header-cell-style="{ background: '#fafafa', color: '#606266', fontWeight: 500 }">
        <el-table-column prop="time" label="时间" width="180" />
        <el-table-column prop="user" label="操作人" width="120" />
        <el-table-column label="操作类型" width="120">
          <template #default="{ row }">
            <el-tag size="small" :color="getOpsTypeTag(row.type).color" style="color: #fff; border: none;">
              {{ getOpsTypeTag(row.type).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="操作" width="140" />
        <el-table-column prop="target" label="目标对象" />
      </el-table>
    </div>

    <!-- 第三区块：SLA 信息 -->
    <div class="section-title">SLA 监控</div>
    <div class="sla-grid">
      <div v-for="item in slaInfo" :key="item.name" class="sla-card">
        <div class="sla-header">
          <span class="sla-name">{{ item.name }}</span>
          <el-tag :type="getStatusType(item.status)" size="small" effect="dark" round>
            {{ item.status === "success" ? "达标" : "关注" }}
          </el-tag>
        </div>
        <div class="sla-body">
          <div class="sla-current">{{ item.current }}</div>
          <div class="sla-target">目标: {{ item.target }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.welcome-container {
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
  line-height: 1.2;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  }
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}

.stat-content {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1d2129;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.charts-row {
  display: grid;
  grid-template-columns: 3fr 2fr;
  gap: 16px;
  margin-bottom: 20px;
}

.chart-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.card-header {
  padding: 16px 20px 0;
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
}

.chart-body {
  height: 260px;
}

.card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  margin-bottom: 20px;
}

.sla-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.sla-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  }
}

.sla-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.sla-name {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.sla-current {
  font-size: 28px;
  font-weight: 700;
  color: #1d2129;
}

.sla-target {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
