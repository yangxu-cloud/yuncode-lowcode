<template>
  <div class="settings-page">
    <div class="settings-sidebar">
      <div class="sidebar-header">
        <el-icon :size="20"><Setting /></el-icon>
        <span>系统设置</span>
      </div>
      <el-menu
        :default-active="activeTab"
        @select="handleMenuSelect"
        class="settings-menu"
      >
        <el-menu-item index="basic">
          <el-icon><Setting /></el-icon>
          <span>{{ $t('routes.basicSettings') }}</span>
        </el-menu-item>
        <el-menu-item index="security">
          <el-icon><Lock /></el-icon>
          <span>{{ $t('routes.securitySettings') }}</span>
        </el-menu-item>
        <el-menu-item index="system">
          <el-icon><Monitor /></el-icon>
          <span>{{ $t('routes.systemInfo') }}</span>
        </el-menu-item>
      </el-menu>
    </div>
    <div class="settings-content">
      <Basic v-if="activeTab === 'basic'" />
      <Security v-else-if="activeTab === 'security'" />
      <System v-else-if="activeTab === 'system'" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useI18n } from "vue-i18n";
import { Setting, Lock, Monitor } from "@element-plus/icons-vue";
import Basic from "./Basic.vue";
import Security from "./Security.vue";
import System from "./System.vue";

const { t } = useI18n();
const activeTab = ref("basic");

const handleMenuSelect = (key: string) => {
  activeTab.value = key;
};
</script>

<style scoped lang="scss">
.settings-page {
  display: flex;
  gap: 20px;
  padding: 20px;
  min-height: calc(100vh - 110px);
  background: #f5f7fa;
}

.settings-sidebar {
  width: 220px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  overflow: hidden;

  .sidebar-header {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 20px;
    font-size: 16px;
    font-weight: 600;
    color: #1d2129;
    border-bottom: 1px solid #f0f0f0;
  }

  .settings-menu {
    border-right: none;
    padding: 8px;

    :deep(.el-menu-item) {
      height: 44px;
      line-height: 44px;
      margin: 4px 0;
      border-radius: 8px;
      font-size: 14px;
      color: #606266;

      &:hover {
        background: #f5f7fa;
      }

      &.is-active {
        background: #ecf5ff;
        color: #409eff;
      }

      .el-icon {
        font-size: 18px;
      }
    }
  }
}

.settings-content {
  flex: 1;
  min-width: 0;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  padding: 24px;
}
</style>
