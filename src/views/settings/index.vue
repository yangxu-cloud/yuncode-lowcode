<template>
  <div class="settings-container">
    <el-card class="settings-card">
      <el-container>
        <el-aside width="200px" class="settings-aside">
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
        </el-aside>
        <el-main class="settings-main">
          <Basic v-if="activeTab === 'basic'" />
          <Security v-else-if="activeTab === 'security'" />
          <System v-else-if="activeTab === 'system'" />
        </el-main>
      </el-container>
    </el-card>
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

// 处理菜单选择
const handleMenuSelect = (key: string) => {
  activeTab.value = key;
};
</script>

<style scoped lang="scss">
.settings-container {
  padding: 20px;

  .settings-card {
    min-height: calc(100vh - 120px);

    :deep(.el-card__body) {
      padding: 0;
    }

    .el-container {
      height: 100%;
    }

    .settings-aside {
      background-color: #f5f7fa;
      border-right: 1px solid #e4e7ed;

      .settings-menu {
        border-right: none;
        background-color: transparent;
      }
    }

    .settings-main {
      padding: 30px;
      background-color: #fff;
    }
  }
}
</style>
