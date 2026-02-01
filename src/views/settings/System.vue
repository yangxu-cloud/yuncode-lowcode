<template>
  <div class="system-info">
    <div class="page-header">
      <h3>{{ $t('settings.systemInfoPage') }}</h3>
      <p class="description">{{ $t('settings.systemInfoDesc') }}</p>
    </div>

    <el-card v-loading="loading" class="info-card">
      <el-descriptions :column="2" border>
        <el-descriptions-item>
          <template #label>
            <div class="label-item">
              <el-icon><Monitor /></el-icon>
              {{ $t('settings.systemNameLabel') }}
            </div>
          </template>
          {{ systemInfo.name }}
        </el-descriptions-item>

        <el-descriptions-item>
          <template #label>
            <div class="label-item">
              <el-icon><PriceTag /></el-icon>
              {{ $t('settings.systemVersion') }}
            </div>
          </template>
          <el-tag type="success">{{ systemInfo.version }}</el-tag>
        </el-descriptions-item>

        <el-descriptions-item>
          <template #label>
            <div class="label-item">
              <el-icon><Platform /></el-icon>
              {{ $t('settings.runtimeEnv') }}
            </div>
          </template>
          <el-tag :type="systemInfo.env === 'prod' ? 'danger' : 'warning'">
            {{ getEnvText(systemInfo.env) }}
          </el-tag>
        </el-descriptions-item>

        <el-descriptions-item>
          <template #label>
            <div class="label-item">
              <el-icon><Files /></el-icon>
              {{ $t('settings.frameworkVersion') }}
            </div>
          </template>
          {{ systemInfo.framework }}
        </el-descriptions-item>

        <el-descriptions-item>
          <template #label>
            <div class="label-item">
              <el-icon><Coffee /></el-icon>
              {{ $t('settings.javaVersion') }}
            </div>
          </template>
          {{ systemInfo.javaVersion }}
        </el-descriptions-item>

        <el-descriptions-item>
          <template #label>
            <div class="label-item">
              <el-icon><Location /></el-icon>
              {{ $t('settings.serverIp') }}
            </div>
          </template>
          {{ systemInfo.serverIp }}
        </el-descriptions-item>

        <el-descriptions-item>
          <template #label>
            <div class="label-item">
              <el-icon><Operation /></el-icon>
              {{ $t('settings.os') }}
            </div>
          </template>
          {{ systemInfo.os }}
        </el-descriptions-item>

        <el-descriptions-item>
          <template #label>
            <div class="label-item">
              <el-icon><Cpu /></el-icon>
              {{ $t('settings.arch') }}
            </div>
          </template>
          {{ systemInfo.arch }}
        </el-descriptions-item>

        <el-descriptions-item :span="2">
          <template #label>
            <div class="label-item">
              <el-icon><Clock /></el-icon>
              {{ $t('settings.startTime') }}
            </div>
          </template>
          {{ systemInfo.startTime }}
        </el-descriptions-item>

        <el-descriptions-item :span="2">
          <template #label>
            <div class="label-item">
              <el-icon><Timer /></el-icon>
              {{ $t('settings.uptime') }}
            </div>
          </template>
          <el-text type="primary" tag="b">{{ systemInfo.uptime }}</el-text>
        </el-descriptions-item>
      </el-descriptions>

      <div class="action-bar">
        <el-button type="primary" @click="handleRefresh" :loading="loading">
          <el-icon><Refresh /></el-icon>
          {{ $t('settings.refreshInfo') }}
        </el-button>
      </div>
    </el-card>

    <!-- 运行状态 -->
    <el-card class="status-card" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>{{ $t('settings.systemStatus') }}</span>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="6">
          <div class="status-item">
            <el-icon class="icon" color="#67c23a"><SuccessFilled /></el-icon>
            <div class="info">
              <div class="label">{{ $t('settings.systemStatus') }}</div>
              <div class="value">{{ $t('settings.statusRunning') }}</div>
            </div>
          </div>
        </el-col>

        <el-col :span="6">
          <div class="status-item">
            <el-icon class="icon" color="#409eff"><Connection /></el-icon>
            <div class="info">
              <div class="label">{{ $t('settings.dbStatus') }}</div>
              <div class="value">{{ $t('settings.statusNormal') }}</div>
            </div>
          </div>
        </el-col>

        <el-col :span="6">
          <div class="status-item">
            <el-icon class="icon" color="#409eff"><FolderOpened /></el-icon>
            <div class="info">
              <div class="label">{{ $t('settings.storageStatus') }}</div>
              <div class="value">{{ $t('settings.statusNormal') }}</div>
            </div>
          </div>
        </el-col>

        <el-col :span="6">
          <div class="status-item">
            <el-icon class="icon" color="#409eff"><Message /></el-icon>
            <div class="info">
              <div class="label">{{ $t('settings.mailStatus') }}</div>
              <div class="value">{{ $t('settings.statusNormal') }}</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage } from "element-plus";
import {
  Monitor,
  PriceTag,
  Platform,
  Files,
  Coffee,
  Location,
  Cpu,
  Clock,
  Timer,
  Refresh,
  SuccessFilled,
  Connection,
  FolderOpened,
  Message
} from "@element-plus/icons-vue";
import { getSystemInfo, type SystemInfo } from "@/api/settings";

const { t } = useI18n();
const loading = ref(false);

const systemInfo = reactive<SystemInfo>({
  name: "",
  version: "",
  env: "",
  framework: "",
  javaVersion: "",
  startTime: "",
  uptime: "",
  serverIp: "",
  os: "",
  arch: ""
});

// 获取环境文本
const getEnvText = (env: string) => {
  const envMap: Record<string, string> = {
    'prod': t('settings.envProd'),
    'dev': t('settings.envDev'),
    'test': t('settings.envTest')
  };
  return envMap[env] || env;
};

// 加载系统信息
const loadSystemInfo = async () => {
  try {
    loading.value = true;
    const data = await getSystemInfo();
    Object.assign(systemInfo, data);
  } catch (error) {
    console.error(t('settings.loadFailed'), error);
  } finally {
    loading.value = false;
  }
};

// 刷新
const handleRefresh = () => {
  loadSystemInfo();
  ElMessage.success(t('settings.refreshSuccess'));
};

onMounted(() => {
  loadSystemInfo();
});
</script>

<style scoped lang="scss">
.system-info {
  .page-header {
    margin-bottom: 30px;

    h3 {
      margin: 0 0 10px 0;
      font-size: 20px;
      font-weight: 500;
      color: #303133;
    }

    .description {
      margin: 0;
      font-size: 14px;
      color: #909399;
    }
  }

  .info-card {
    margin-bottom: 20px;

    .label-item {
      display: flex;
      align-items: center;
      gap: 6px;

      .el-icon {
        font-size: 16px;
      }
    }

    .action-bar {
      margin-top: 30px;
      display: flex;
      gap: 10px;
    }
  }

  .status-card {
    .card-header {
      font-weight: 500;
    }

    .status-item {
      display: flex;
      align-items: center;
      padding: 20px;
      background-color: #f5f7fa;
      border-radius: 8px;

      .icon {
        font-size: 32px;
        margin-right: 16px;
      }

      .info {
        flex: 1;

        .label {
          font-size: 14px;
          color: #909399;
          margin-bottom: 8px;
        }

        .value {
          font-size: 18px;
          font-weight: 500;
          color: #303133;
        }
      }
    }
  }
}
</style>
