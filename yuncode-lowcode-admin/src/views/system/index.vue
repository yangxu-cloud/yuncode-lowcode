<template>
  <div class="system-container">
    <el-card class="system-card">
      <el-container>
        <el-aside width="200px" class="system-aside">
          <el-menu
            :default-active="activeMenu"
            router
            class="system-menu"
          >
            <el-menu-item index="/system/logs">
              <el-icon><Document /></el-icon>
              <span>{{ t('system.logManage') }}</span>
            </el-menu-item>
            <el-menu-item index="/system/online-users">
              <el-icon><User /></el-icon>
              <span>{{ t('system.onlineUsers') }}</span>
            </el-menu-item>
            <el-sub-menu index="commons">
              <template #title>
                <el-icon><OfficeBuilding /></el-icon>
                <span>{{ t('menu.commons') }}</span>
              </template>
              <el-menu-item index="/system/commons/org">
                <span>{{ t('menu.org') }}</span>
              </el-menu-item>
            </el-sub-menu>
          </el-menu>
        </el-aside>
        <el-main class="system-main">
          <router-view />
        </el-main>
      </el-container>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import { Document, User, OfficeBuilding } from "@element-plus/icons-vue";

const route = useRoute();
const { t } = useI18n();

const activeMenu = computed(() => route.path);
</script>

<style scoped lang="scss">
.system-container {
  padding: 20px;

  .system-card {
    min-height: calc(100vh - 120px);

    :deep(.el-card__body) {
      padding: 0;
    }

    .el-container {
      height: 100%;
    }

    .system-aside {
      background-color: #f5f7fa;
      border-right: 1px solid #e4e7ed;

      .system-menu {
        border-right: none;
        background-color: transparent;
      }
    }

    .system-main {
      padding: 30px;
      background-color: #fff;
    }
  }
}
</style>
