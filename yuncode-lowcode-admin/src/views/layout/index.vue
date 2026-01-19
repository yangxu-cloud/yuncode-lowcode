<template>
  <el-container class="layout-container">
    <el-aside width="200px">
      <div class="logo">Yuncode</div>
      <el-menu
        :default-active="activeMenu"
        class="el-menu-vertical"
        router
        background-color="#545c64"
        text-color="#fff"
        active-text-color="#ffd04b"
      >
        <el-menu-item index="/home">
          <el-icon><House /></el-icon>
          <span>{{ t('menu.home') }}</span>
        </el-menu-item>
        <el-menu-item index="/system">
          <el-icon><Management /></el-icon>
          <span>{{ t('menu.system') }}</span>
        </el-menu-item>
        <el-menu-item index="/settings">
          <el-icon><Setting /></el-icon>
          <span>{{ t('menu.settings') }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header>
        <div class="header-content">
          <span class="title">Yuncode LowCode Platform</span>
          <div class="header-actions">
            <LanguageSwitcher />
            <el-dropdown @command="handleCommand">
              <span class="el-dropdown-link">
                <el-avatar :size="32" :src="userStore.avatar" v-if="userStore.avatar" />
                <el-icon v-else :size="32"><User /></el-icon>
                <span class="user-info">
                  {{ userStore.nickname || userStore.username }}
                </span>
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item disabled>
                    <span>{{ t('login.tenantCode') }}: {{ userStore.tenantName }}</span>
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">
                    <el-icon><SwitchButton /></el-icon>
                    {{ t('login.logout') }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import { useUserStore } from "@/stores/user";
import LanguageSwitcher from "@/components/LanguageSwitcher.vue";

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const { t } = useI18n();

const activeMenu = computed(() => route.path);

const handleCommand = async (command: string) => {
  if (command === "logout") {
    // 在logout前获取登录类型
    const loginType = localStorage.getItem("loginType") || "";

    await userStore.logout();

    // 根据登录类型跳转到对应的登录页
    if (loginType === "admin") {
      router.push("/console/login");
    } else {
      router.push("/login");
    }
  }
};
</script>

<style scoped lang="scss">
.layout-container {
  height: 100%;
}

.el-aside {
  background-color: #545c64;
  color: #fff;

  .logo {
    height: 60px;
    line-height: 60px;
    text-align: center;
    font-size: 20px;
    font-weight: bold;
    color: #fff;
    border-bottom: 1px solid #434a50;
  }

  .el-menu-vertical {
    border-right: none;
  }
}

.el-header {
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;

  .header-content {
    width: 100%;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .title {
      font-size: 18px;
      font-weight: bold;
    }

    .header-actions {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .el-dropdown-link {
      cursor: pointer;
      display: flex;
      align-items: center;
      gap: 8px;

      .user-info {
        margin-left: 8px;
        font-size: 14px;
      }
    }
  }
}

.el-main {
  background-color: #f0f2f5;
}
</style>
