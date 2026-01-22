<template>
  <el-dropdown trigger="click" @command="handleCommand">
    <span class="user-dropdown">
      <el-avatar :size="32" :src="userStore.avatar" v-if="userStore.avatar" />
      <el-icon v-else :size="32" class="user-icon">
        <User />
      </el-icon>
      <span class="username">
        {{ userStore.nickname || userStore.username }}
      </span>
      <el-icon class="dropdown-icon">
        <ArrowDown />
      </el-icon>
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item disabled>
          <div class="user-info-item">
            <el-icon><User /></el-icon>
            <div class="user-detail">
              <div class="user-name">{{ userStore.nickname || userStore.username }}</div>
              <div class="user-tenant">{{ userStore.tenantName || t('login.tenantCode') }}: {{ userStore.tenantCode }}</div>
            </div>
          </div>
        </el-dropdown-item>
        <el-dropdown-item divided command="profile">
          <el-icon><User /></el-icon>
          {{ t('menus.profile') }}
        </el-dropdown-item>
        <el-dropdown-item command="settings">
          <el-icon><Setting /></el-icon>
          {{ t('menus.settings') }}
        </el-dropdown-item>
        <el-dropdown-item divided command="logout">
          <el-icon><SwitchButton /></el-icon>
          {{ t('login.logout') }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { useUserStore } from "@/stores/user";
import { ElMessageBox } from "element-plus";
import { ElMessage } from "element-plus";

const router = useRouter();
const { t } = useI18n();
const userStore = useUserStore();

const handleCommand = async (command: string) => {
  switch (command) {
    case "profile":
      // TODO: 跳转到个人中心
      ElMessage.info("个人中心功能开发中");
      break;
    case "settings":
      router.push("/settings");
      break;
    case "logout":
      try {
        await ElMessageBox.confirm(t('login.logoutConfirm'), t('common.tip'), {
          confirmButtonText: t('common.confirm'),
          cancelButtonText: t('common.cancel'),
          type: "warning"
        });

        // 在logout前获取登录类型
        const loginType = localStorage.getItem("loginType") || "";
        await userStore.logout();

        // 根据登录类型跳转到对应的登录页
        if (loginType === "admin") {
          router.push("/console/login");
        } else {
          router.push("/login");
        }
      } catch (error) {
        // 用户取消退出
      }
      break;
  }
};
</script>

<style scoped lang="scss">
.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 0 12px;
  height: 40px;
  border-radius: 4px;
  transition: all 0.3s;

  &:hover {
    background-color: #f0f2f5;
  }

  .user-icon {
    color: #909399;
  }

  .username {
    font-size: 14px;
    color: #333;
    max-width: 120px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .dropdown-icon {
    font-size: 12px;
    color: #909399;
  }
}

.user-info-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;

  .user-detail {
    flex: 1;

    .user-name {
      font-size: 14px;
      font-weight: 500;
      color: #333;
    }

    .user-tenant {
      font-size: 12px;
      color: #909399;
      margin-top: 4px;
    }
  }
}
</style>
