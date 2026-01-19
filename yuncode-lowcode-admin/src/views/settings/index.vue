<template>
  <div class="settings-container">
    <el-card class="settings-card">
      <el-container>
        <el-aside width="200px" class="settings-aside">
          <el-menu
            :default-active="activeMenu"
            router
            class="settings-menu"
          >
            <el-menu-item index="/settings/basic">
              <el-icon><Setting /></el-icon>
              <span>基本设置</span>
            </el-menu-item>
            <el-menu-item index="/settings/security">
              <el-icon><Lock /></el-icon>
              <span>安全设置</span>
            </el-menu-item>
            <el-menu-item index="/settings/storage">
              <el-icon><FolderOpened /></el-icon>
              <span>存储设置</span>
            </el-menu-item>
            <el-menu-item index="/settings/mail">
              <el-icon><Message /></el-icon>
              <span>邮件设置</span>
            </el-menu-item>
            <el-menu-item index="/settings/system">
              <el-icon><Monitor /></el-icon>
              <span>系统信息</span>
            </el-menu-item>
          </el-menu>
        </el-aside>
        <el-main class="settings-main">
          <router-view />
        </el-main>
      </el-container>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();
const activeMenu = computed(() => route.path);
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
