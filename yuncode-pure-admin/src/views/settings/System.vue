<template>
  <div class="system-info">
    <div class="page-header">
      <h3>{{ $t('settings.systemInfoPage') }}</h3>
      <p class="description">{{ $t('settings.systemInfoDesc') }}</p>
    </div>

    <!-- 系统信息卡片 -->
    <div v-loading="loading" class="info-grid">
      <div class="info-item">
        <div class="info-icon" style="background: #ecf5ff; color: #409eff;">
          <el-icon :size="24"><Monitor /></el-icon>
        </div>
        <div class="info-content">
          <div class="info-label">{{ $t('settings.systemNameLabel') }}</div>
          <div class="info-value">{{ systemInfo.name || '-' }}</div>
        </div>
      </div>

      <div class="info-item">
        <div class="info-icon" style="background: #f0f9eb; color: #67c23a;">
          <el-icon :size="24"><PriceTag /></el-icon>
        </div>
        <div class="info-content">
          <div class="info-label">{{ $t('settings.systemVersion') }}</div>
          <div class="info-value">
            <el-tag type="success" effect="dark" round>{{ systemInfo.version || '-' }}</el-tag>
          </div>
        </div>
      </div>

      <div class="info-item">
        <div class="info-icon" :style="{ background: systemInfo.env === 'prod' ? '#fef0f0' : '#fdf6ec', color: systemInfo.env === 'prod' ? '#f56c6c' : '#e6a23c' }">
          <el-icon :size="24"><Platform /></el-icon>
        </div>
        <div class="info-content">
          <div class="info-label">{{ $t('settings.runtimeEnv') }}</div>
          <div class="info-value">
            <el-tag :type="systemInfo.env === 'prod' ? 'danger' : 'warning'" effect="dark" round>
              {{ getEnvText(systemInfo.env) }}
            </el-tag>
          </div>
        </div>
      </div>

      <div class="info-item">
        <div class="info-icon" style="background: #f4f4f5; color: #909399;">
          <el-icon :size="24"><Files /></el-icon>
        </div>
        <div class="info-content">
          <div class="info-label">{{ $t('settings.frameworkVersion') }}</div>
          <div class="info-value">{{ systemInfo.framework || '-' }}</div>
        </div>
      </div>

      <div class="info-item">
        <div class="info-icon" style="background: #ecf5ff; color: #409eff;">
          <el-icon :size="24"><Coffee /></el-icon>
        </div>
        <div class="info-content">
          <div class="info-label">{{ $t('settings.javaVersion') }}</div>
          <div class="info-value">{{ systemInfo.javaVersion || '-' }}</div>
        </div>
      </div>

      <div class="info-item">
        <div class="info-icon" style="background: #f0f9eb; color: #67c23a;">
          <el-icon :size="24"><Location /></el-icon>
        </div>
        <div class="info-content">
          <div class="info-label">{{ $t('settings.serverIp') }}</div>
          <div class="info-value">{{ systemInfo.serverIp || '-' }}</div>
        </div>
      </div>

      <div class="info-item">
        <div class="info-icon" style="background: #fdf6ec; color: #e6a23c;">
          <el-icon :size="24"><Operation /></el-icon>
        </div>
        <div class="info-content">
          <div class="info-label">{{ $t('settings.os') }}</div>
          <div class="info-value">{{ systemInfo.os || '-' }}</div>
        </div>
      </div>

      <div class="info-item">
        <div class="info-icon" style="background: #f4f4f5; color: #909399;">
          <el-icon :size="24"><Cpu /></el-icon>
        </div>
        <div class="info-content">
          <div class="info-label">{{ $t('settings.arch') }}</div>
          <div class="info-value">{{ systemInfo.arch || '-' }}</div>
        </div>
      </div>
    </div>

    <!-- 运行时间 -->
    <div class="uptime-card">
      <div class="uptime-item">
        <el-icon :size="20" color="#409eff"><Clock /></el-icon>
        <div class="uptime-info">
          <span class="uptime-label">{{ $t('settings.startTime') }}</span>
          <span class="uptime-value">{{ systemInfo.startTime || '-' }}</span>
        </div>
      </div>
      <div class="uptime-item">
        <el-icon :size="20" color="#67c23a"><Timer /></el-icon>
        <div class="uptime-info">
          <span class="uptime-label">{{ $t('settings.uptime') }}</span>
          <span class="uptime-value highlight">{{ systemInfo.uptime || '-' }}</span>
        </div>
      </div>
    </div>

    <!-- 运行状态 -->
    <div class="status-section">
      <div class="section-title">{{ $t('settings.systemStatus') }}</div>
      <div class="status-grid">
        <div class="status-item">
          <div class="status-icon success">
            <el-icon :size="20"><SuccessFilled /></el-icon>
          </div>
          <div class="status-info">
            <div class="status-label">{{ $t('settings.systemStatus') }}</div>
            <div class="status-value">{{ $t('settings.statusRunning') }}</div>
          </div>
        </div>

        <div class="status-item">
          <div class="status-icon primary">
            <el-icon :size="20"><Connection /></el-icon>
          </div>
          <div class="status-info">
            <div class="status-label">{{ $t('settings.dbStatus') }}</div>
            <div class="status-value">{{ $t('settings.statusNormal') }}</div>
          </div>
        </div>

        <div class="status-item">
          <div class="status-icon primary">
            <el-icon :size="20"><FolderOpened /></el-icon>
          </div>
          <div class="status-info">
            <div class="status-label">{{ $t('settings.storageStatus') }}</div>
            <div class="status-value">{{ $t('settings.statusNormal') }}</div>
          </div>
        </div>

        <div class="status-item">
          <div class="status-icon primary">
            <el-icon :size="20"><Message /></el-icon>
          </div>
          <div class="status-info">
            <div class="status-label">{{ $t('settings.mailStatus') }}</div>
            <div class="status-value">{{ $t('settings.statusNormal') }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 刷新按钮 -->
    <div class="action-bar">
      <el-button type="primary" @click="handleRefresh" :loading="loading" size="large">
        <el-icon><Refresh /></el-icon>
        {{ $t('settings.refreshInfo') }}
      </el-button>
    </div>
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
  Message,
  Operation
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

const getEnvText = (env: string) => {
  const envMap: Record<string, string> = {
    'prod': t('settings.envProd'),
    'dev': t('settings.envDev'),
    'test': t('settings.envTest')
  };
  return envMap[env] || env;
};

const loadSystemInfo = async () => {
  try {
    loading.value = true;
    const data = await getSystemInfo();
    if (data) {
      Object.assign(systemInfo, data);
    }
  } catch (error) {
    console.error(t('settings.loadFailed'), error);
  } finally {
    loading.value = false;
  }
};

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
  max-width: 900px;

  .page-header {
    margin-bottom: 32px;
    padding-bottom: 20px;
    border-bottom: 1px solid #f0f0f0;

    h3 {
      margin: 0 0 8px 0;
      font-size: 20px;
      font-weight: 600;
      color: #1d2129;
    }

    .description {
      margin: 0;
      font-size: 14px;
      color: #909399;
    }
  }

  .info-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
    margin-bottom: 24px;
  }

  .info-item {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 20px;
    background: #fafafa;
    border-radius: 12px;
    transition: all 0.3s ease;

    &:hover {
      background: #f5f7fa;
      transform: translateY(-1px);
    }
  }

  .info-icon {
    width: 52px;
    height: 52px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .info-content {
    flex: 1;
    min-width: 0;
  }

  .info-label {
    font-size: 13px;
    color: #909399;
    margin-bottom: 6px;
  }

  .info-value {
    font-size: 15px;
    font-weight: 500;
    color: #1d2129;
  }

  .uptime-card {
    display: flex;
    gap: 24px;
    padding: 20px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 12px;
    margin-bottom: 24px;
  }

  .uptime-item {
    display: flex;
    align-items: center;
    gap: 12px;
    flex: 1;

    .el-icon {
      color: #fff;
    }
  }

  .uptime-info {
    display: flex;
    flex-direction: column;
  }

  .uptime-label {
    font-size: 12px;
    color: rgba(255, 255, 255, 0.8);
    margin-bottom: 4px;
  }

  .uptime-value {
    font-size: 16px;
    font-weight: 600;
    color: #fff;

    &.highlight {
      font-size: 20px;
    }
  }

  .status-section {
    margin-bottom: 24px;
  }

  .section-title {
    font-size: 15px;
    font-weight: 600;
    color: #1d2129;
    margin-bottom: 16px;
  }

  .status-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
  }

  .status-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px;
    background: #fafafa;
    border-radius: 10px;
  }

  .status-icon {
    width: 40px;
    height: 40px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;

    &.success {
      background: #f0f9eb;
      color: #67c23a;
    }

    &.primary {
      background: #ecf5ff;
      color: #409eff;
    }
  }

  .status-info {
    flex: 1;
  }

  .status-label {
    font-size: 12px;
    color: #909399;
    margin-bottom: 4px;
  }

  .status-value {
    font-size: 14px;
    font-weight: 500;
    color: #1d2129;
  }

  .action-bar {
    padding-top: 24px;
    border-top: 1px solid #f0f0f0;
  }
}
</style>
