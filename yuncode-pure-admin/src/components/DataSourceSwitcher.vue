<template>
  <div v-if="allowSwitch" class="data-source-switcher">
    <el-tooltip :content="`当前: ${currentLabel}`" placement="left">
      <div class="switcher-btn" @click="handleToggle">
        <el-icon :size="20">
          <component :is="icon" />
        </el-icon>
        <span class="label">{{ label }}</span>
      </div>
    </el-tooltip>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Connection, Switch } from '@element-plus/icons-vue';
import { appConfig, setDataSource, getDataSource, type DataSource } from '@/config/app';

const router = useRouter();

// 开发模式调试
onMounted(() => {
  console.log('[DataSourceSwitcher] 组件已挂载');
  console.log('[DataSourceSwitcher] 是否开发模式:', import.meta.env.DEV);
  console.log('[DataSourceSwitcher] 允许切换:', appConfig.allowRuntimeSwitch);
  console.log('[DataSourceSwitcher] 当前数据源:', getDataSource());
  console.log('[DataSourceSwitcher] localStorage:', localStorage.getItem('data-source'));
});

/**
 * 是否允许切换（仅开发模式）
 */
const allowSwitch = computed(() => {
  const result = appConfig.allowRuntimeSwitch;
  console.log('[DataSourceSwitcher] allowSwitch 计算值:', result);
  return result;
});

/**
 * 当前数据源
 */
const currentSource = computed(() => getDataSource());

/**
 * 当前数据源标签
 */
const currentLabel = computed(() => {
  return currentSource.value === 'mock' ? 'Mock 数据' : '真实 API';
});

/**
 * 按钮显示标签
 */
const label = computed(() => {
  return currentSource.value === 'mock' ? 'Mock' : 'API';
});

/**
 * 按钮图标
 */
const icon = computed(() => {
  return currentSource.value === 'mock' ? Switch : Connection;
});

/**
 * 切换数据源
 */
const handleToggle = async () => {
  const newSource: DataSource = currentSource.value === 'mock' ? 'api' : 'mock';
  const newLabel = newSource === 'mock' ? 'Mock 数据' : '真实 API';

  console.log('[DataSourceSwitcher] 准备切换到:', newSource);

  try {
    await ElMessageBox.confirm(
      `切换到 ${newLabel} 将刷新页面，是否继续？`,
      '切换数据源',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    );

    console.log('[DataSourceSwitcher] 用户确认切换');
    setDataSource(newSource);
  } catch {
    console.log('[DataSourceSwitcher] 用户取消切换');
  }
};
</script>

<style scoped lang="scss">
.data-source-switcher {
  position: fixed;
  top: 50%;
  right: 0;
  z-index: 9999;
  transform: translateY(-50%);

  .switcher-btn {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 4px;
    width: 44px;
    height: 80px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 8px 0 0 8px;
    cursor: pointer;
    box-shadow: -2px 0 8px rgba(0, 0, 0, 0.15);
    transition: all 0.3s;
    color: #fff;
    user-select: none;

    &:hover {
      width: 48px;
      box-shadow: -4px 0 12px rgba(0, 0, 0, 0.25);
    }

    &:active {
      transform: scale(0.95);
    }

    .label {
      font-size: 12px;
      font-weight: 600;
      writing-mode: vertical-rl;
      text-orientation: mixed;
      letter-spacing: 2px;
    }
  }
}

// API 模式下的样式
.data-source-switcher .switcher-btn {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}
</style>
