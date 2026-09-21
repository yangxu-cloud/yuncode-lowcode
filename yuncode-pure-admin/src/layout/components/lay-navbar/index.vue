<script setup lang="ts">
import { useNav } from "@/layout/hooks/useNav";
import { useLayout } from "@/layout/hooks/useLayout";
import LaySearch from "../lay-search/index.vue";
import LayNotice from "../lay-notice/index.vue";
import LayNavMix from "../lay-sidebar/NavMix.vue";
import LaySidebarFullScreen from "../lay-sidebar/components/SidebarFullScreen.vue";
import LaySidebarLanguage from "../lay-sidebar/components/SidebarLanguage.vue";
import LaySidebarBreadCrumb from "../lay-sidebar/components/SidebarBreadCrumb.vue";
import LaySidebarTopCollapse from "../lay-sidebar/components/SidebarTopCollapse.vue";
import LaySidebarLogo from "../lay-sidebar/components/SidebarLogo.vue";
import IconTabs from "../IconTabs.vue";

import LogoutCircleRLine from "~icons/ri/logout-circle-r-line";
import Setting from "~icons/ri/settings-3-line";

const { layout } = useLayout();
const {
  logout,
  onPanel,
  username,
  userAvatar,
  avatarsStyle
} = useNav();
</script>

<template>
  <div class="navbar">
    <LayNavMix v-if="layout === 'mix'" />

    <div v-if="layout === 'simple'" class="simple-logo-wrap">
      <LaySidebarLogo :collapse="false" />
      <IconTabs />
    </div>

    <LaySidebarBreadCrumb v-if="layout === 'vertical'" id="breadcrumb" class="breadcrumb-container" />

    <div v-if="layout === 'vertical' || layout === 'simple'" class="vertical-header-right">
      <!-- 菜单搜索 -->
      <LaySearch id="header-search" />
      <!-- 全屏 -->
      <LaySidebarFullScreen id="full-screen" />
      <!-- 语言切换 -->
      <LaySidebarLanguage id="language-switch" />
      <!-- 消息通知 -->
      <LayNotice id="header-notice" />
      <!-- 用户信息 -->
      <div class="user-info">
        <el-dropdown trigger="click">
          <span class="el-dropdown-link select-none">
            <img :src="userAvatar" :style="avatarsStyle" />
            <p v-if="username">{{ username }}</p>
          </span>
          <template #dropdown>
            <el-dropdown-menu class="logout">
              <el-dropdown-item @click="logout">
                <IconifyIconOffline
                  :icon="LogoutCircleRLine"
                  style="margin: 5px"
                />
                退出系统
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
      <!-- 设置 -->
      <span
        class="tool-btn"
        title="打开系统配置"
        @click="onPanel"
      >
        <IconifyIconOffline :icon="Setting" />
      </span>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 48px;
  padding: 0 8px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  box-sizing: border-box;

  .vertical-header-right {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    margin-left: auto;
    height: 48px;
    gap: 2px;
  }

  .breadcrumb-container {
    float: left;
    margin-left: 16px;
  }

  .simple-logo-wrap {
    width: auto;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    gap: 12px;
    padding-left: 4px;

    :deep(.sidebar-logo-container) {
      width: auto;
    }

    :deep(.hamburger-btn) {
      border-right: 1px solid #e5e6eb;
      border-radius: 0;
      padding-right: 12px;
      height: 32px;
    }
  }
}

.user-info {
  .el-dropdown-link {
    display: flex;
    align-items: center;
    gap: 8px;
    height: 36px;
    padding: 0 10px;
    border-radius: 8px;
    cursor: pointer;
    transition: background 0.2s ease;

    &:hover {
      background: #f7f8fa;
    }

    p {
      font-size: 13px;
      color: #1d2129;
      font-weight: 500;
    }

    img {
      width: 26px;
      height: 26px;
      border-radius: 50%;
      object-fit: cover;
    }
  }
}

.tool-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  cursor: pointer;
  color: #4e5969;
  transition: all 0.2s ease;

  &:hover {
    background: #f7f8fa;
    color: #1d2129;
  }
}

.logout {
  width: 120px;

  ::v-deep(.el-dropdown-menu__item) {
    display: inline-flex;
    flex-wrap: wrap;
    min-width: 100%;
  }
}
</style>
