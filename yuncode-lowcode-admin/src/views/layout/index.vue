<template>
  <el-container class="layout-container">
    <el-aside :width="sidebarWidth">
      <div class="logo" :class="{ 'logo-collapse': isCollapse }">
        <span v-if="!isCollapse">Yuncode</span>
        <span v-else>Y</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        class="el-menu-vertical"
        router
        background-color="#545c64"
        text-color="#fff"
        active-text-color="#ffd04b"
      >
        <el-menu-item index="/home">
          <el-icon><House /></el-icon>
          <template #title>{{ t('menu.home') }}</template>
        </el-menu-item>
        <el-menu-item index="/system">
          <el-icon><Management /></el-icon>
          <template #title>{{ t('menu.system') }}</template>
        </el-menu-item>
        <el-menu-item index="/settings">
          <el-icon><Setting /></el-icon>
          <template #title>{{ t('menu.settings') }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container class="main-container">
      <el-header>
        <Navbar @sidebar-toggle="handleSidebarToggle" />
      </el-header>
      <TagsView />
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, watch } from "vue";
import { useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import { House, Management, Setting, OfficeBuilding } from "@element-plus/icons-vue";
import Navbar from "@/components/Navbar.vue";
import TagsView from "@/components/TagsView.vue";

const route = useRoute();
const { t } = useI18n();

const activeMenu = computed(() => route.path);
const isDark = ref(false);
const isCollapse = ref(false);

// 侧边栏宽度计算
const sidebarWidth = computed(() => {
  return isCollapse.value ? "64px" : "200px";
});

// 从 localStorage 加载侧边栏状态
onMounted(() => {
  const savedCollapseState = localStorage.getItem("sidebarCollapse");
  if (savedCollapseState) {
    isCollapse.value = savedCollapseState === "true";
  }

  // 监听主题变化
  const observer = new MutationObserver(() => {
    isDark.value = document.documentElement.classList.contains("dark");
  });

  observer.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ["class"]
  });
});

// 监听折叠状态变化并保存
watch(isCollapse, (newValue) => {
  localStorage.setItem("sidebarCollapse", String(newValue));
});

// 处理侧边栏切换
const handleSidebarToggle = (collapse: boolean) => {
  isCollapse.value = collapse;
};
</script>

<style scoped lang="scss">
.layout-container {
  height: 100%;
}

.el-aside {
  background-color: #545c64;
  color: #fff;
  transition: width 0.3s ease, background-color 0.3s;
  overflow: hidden;

  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    font-size: 18px;
    font-weight: bold;
    color: #fff;
    border-bottom: 1px solid #434a50;
    transition: all 0.3s;
    white-space: nowrap;
    overflow: hidden;

    &.logo-collapse {
      font-size: 24px;
      padding: 0;
    }

    .logo-img {
      width: 32px;
      height: 32px;
    }

    .logo-text {
      font-size: 18px;
      font-weight: 600;
    }
  }

  .el-menu-vertical {
    border-right: none;
    transition: all 0.3s;
  }

  // 折叠时不显示 tooltip
  &:not(.el-menu--collapse) {
    width: 200px;
  }
}

.main-container {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.el-header {
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  padding: 0;
  height: 50px;
  flex-shrink: 0;
}

.el-main {
  background-color: #f0f2f5;
  padding: 16px;
  flex: 1;
  overflow: auto;
}

// 暗色主题
:deep(.dark) {
  .el-header {
    background-color: #1d1e1f;
    border-bottom-color: #2c2e2f;
  }

  .el-main {
    background-color: #141414;
  }

  .el-aside {
    background-color: #1d1e1f;

    .logo {
      border-bottom-color: #2c2e2f;
    }
  }
}
</style>
