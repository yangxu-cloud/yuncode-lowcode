<template>
  <div class="system-info">
    <div class="page-header">
      <h3>系统信息</h3>
      <p class="description">查看系统运行状态和基本信息</p>
    </div>

    <el-card v-loading="loading" class="info-card">
      <el-descriptions :column="2" border>
        <el-descriptions-item>
          <template #label>
            <div class="label-item">
              <el-icon><Monitor /></el-icon>
              系统名称
            </div>
          </template>
          {{ systemInfo.name }}
        </el-descriptions-item>

        <el-descriptions-item>
          <template #label>
            <div class="label-item">
              <el-icon><PriceTag /></el-icon>
              系统版本
            </div>
          </template>
          <el-tag type="success">{{ systemInfo.version }}</el-tag>
        </el-descriptions-item>

        <el-descriptions-item>
          <template #label>
            <div class="label-item">
              <el-icon><Platform /></el-icon>
              运行环境
            </div>
          </template>
          <el-tag :type="systemInfo.env === 'prod' ? 'danger' : 'warning'">
            {{ systemInfo.env === 'prod' ? '生产环境' : systemInfo.env === 'dev' ? '开发环境' : '测试环境' }}
          </el-tag>
        </el-descriptions-item>

        <el-descriptions-item>
          <template #label>
            <div class="label-item">
              <el-icon><Files /></el-icon>
              框架版本
            </div>
          </template>
          {{ systemInfo.framework }}
        </el-descriptions-item>

        <el-descriptions-item>
          <template #label>
            <div class="label-item">
              <el-icon><Coffee /></el-icon>
              Java 版本
            </div>
          </template>
          {{ systemInfo.javaVersion }}
        </el-descriptions-item>

        <el-descriptions-item>
          <template #label>
            <div class="label-item">
              <el-icon><Location /></el-icon>
              服务器 IP
            </div>
          </template>
          {{ systemInfo.serverIp }}
        </el-descriptions-item>

        <el-descriptions-item>
          <template #label>
            <div class="label-item">
              <el-icon><Operation /></el-icon>
              操作系统
            </div>
          </template>
          {{ systemInfo.os }}
        </el-descriptions-item>

        <el-descriptions-item>
          <template #label>
            <div class="label-item">
              <el-icon><Cpu /></el-icon>
              系统架构
            </div>
          </template>
          {{ systemInfo.arch }}
        </el-descriptions-item>

        <el-descriptions-item :span="2">
          <template #label>
            <div class="label-item">
              <el-icon><Clock /></el-icon>
              启动时间
            </div>
          </template>
          {{ systemInfo.startTime }}
        </el-descriptions-item>

        <el-descriptions-item :span="2">
          <template #label>
            <div class="label-item">
              <el-icon><Timer /></el-icon>
              运行时长
            </div>
          </template>
          <el-text type="primary" tag="b">{{ systemInfo.uptime }}</el-text>
        </el-descriptions-item>
      </el-descriptions>

      <div class="action-bar">
        <el-button type="primary" @click="handleRefresh" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新信息
        </el-button>
        <el-popconfirm
          title="确定要重启系统吗？重启后系统将短暂不可用"
          confirm-button-text="确定"
          cancel-button-text="取消"
          @confirm="handleRestart"
        >
          <template #reference>
            <el-button type="danger" :loading="restartLoading">
              <el-icon><VideoPlay /></el-icon>
              重启系统
            </el-button>
          </template>
        </el-popconfirm>
      </div>
    </el-card>

    <!-- 运行状态 -->
    <el-card class="status-card" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>运行状态</span>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="6">
          <div class="status-item">
            <el-icon class="icon" color="#67c23a"><SuccessFilled /></el-icon>
            <div class="info">
              <div class="label">系统状态</div>
              <div class="value">运行中</div>
            </div>
          </div>
        </el-col>

        <el-col :span="6">
          <div class="status-item">
            <el-icon class="icon" color="#409eff"><Connection /></el-icon>
            <div class="info">
              <div class="label">数据库</div>
              <div class="value">正常</div>
            </div>
          </div>
        </el-col>

        <el-col :span="6">
          <div class="status-item">
            <el-icon class="icon" color="#409eff"><FolderOpened /></el-icon>
            <div class="info">
              <div class="label">存储服务</div>
              <div class="value">正常</div>
            </div>
          </div>
        </el-col>

        <el-col :span="6">
          <div class="status-item">
            <el-icon class="icon" color="#409eff"><Message /></el-icon>
            <div class="info">
              <div class="label">邮件服务</div>
              <div class="value">正常</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
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
  VideoPlay,
  SuccessFilled,
  Connection,
  FolderOpened,
  Message
} from "@element-plus/icons-vue";
import { getSystemInfo, restartSystem, type SystemInfo } from "@/api/settings";

const loading = ref(false);
const restartLoading = ref(false);

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

// 加载系统信息
const loadSystemInfo = async () => {
  try {
    loading.value = true;
    const data = await getSystemInfo();
    Object.assign(systemInfo, data);
  } catch (error) {
    console.error("加载系统信息失败:", error);
  } finally {
    loading.value = false;
  }
};

// 刷新
const handleRefresh = () => {
  loadSystemInfo();
  ElMessage.success("已刷新");
};

// 重启系统
const handleRestart = async () => {
  try {
    restartLoading.value = true;
    await restartSystem();
    ElMessage.success("系统正在重启，请稍候...");
  } catch (error: any) {
    console.error("重启失败:", error);
    ElMessage.error(error.message || "重启失败");
  } finally {
    restartLoading.value = false;
  }
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
