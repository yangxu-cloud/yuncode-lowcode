<template>
  <div class="icon-tabs-wrapper">
    <div class="icon-tabs">
      <div
        v-for="tab in tabs"
        :key="tab.path"
        class="icon-tab"
        :class="{ active: isActive(tab.path) }"
        :title="tab.title"
        @click="switchTab(tab)"
        @mouseenter="activeTab = tab"
      >
        <el-icon v-if="getIcon(tab)" :size="16"><component :is="getIcon(tab)" /></el-icon>
        <span v-else class="icon-tab-text">{{ tab.title?.charAt(0) }}</span>
      </div>
    </div>

    <!-- 操作按钮组 -->
    <Transition name="fade">
      <div v-if="activeTab" class="tab-actions">
        <span class="tab-action-btn" title="刷新页面" @click="handleRefresh">
          <el-icon :size="14"><Refresh /></el-icon>
        </span>
        <span
          class="tab-action-btn"
          :class="{ disabled: isFixed(activeTab) }"
          title="关闭当前"
          @click="handleCloseCurrent"
        >
          <el-icon :size="14"><Close /></el-icon>
        </span>
        <span class="tab-action-btn" title="关闭其他" @click="handleCloseOthers">
          <el-icon :size="14"><FolderRemove /></el-icon>
        </span>
        <span class="tab-action-btn" title="关闭全部" @click="handleCloseAll">
          <el-icon :size="14"><FolderDelete /></el-icon>
        </span>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import { useMultiTagsStoreHook } from "@/store/modules/multiTags";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";
import { ElMessage } from "element-plus";
import {
  Refresh,
  Close,
  FolderRemove,
  FolderDelete
} from "@element-plus/icons-vue";

const router = useRouter();
const route = useRoute();
const { t } = useI18n();
const multiTags = computed(() => useMultiTagsStoreHook().multiTags);

const tabs = computed(() => {
  return multiTags.value.map(tag => ({
    path: tag.path,
    title: t(tag?.meta?.i18nKey || tag?.meta?.title || ""),
    icon: tag?.meta?.icon,
    query: tag?.query,
    params: tag?.params,
    fixedTag: tag?.meta?.fixedTag
  }));
});
console.log("tabs", tabs.value);
const activeTab = ref<any>(null);

function isActive(path: string) {
  return route.path === path;
}

function isFixed(tab: any) {
  return tab?.fixedTag;
}

function getIcon(tab: any) {
  const icon = tab?.icon || tab?.meta?.icon;
  if (icon) {
    return useRenderIcon(icon);
  }
  // 首页兜底图标
  if (tab?.path === "/welcome" || tab?.path === "/") {
    return useRenderIcon("ep/home-filled");
  }
  return null;
}

function switchTab(tab: any) {
  activeTab.value = tab;
  router.push(tab.path);
}

function handleRefresh() {
  if (!activeTab.value) return;
  const { path, query, params } = activeTab.value;
  router.replace({
    path: "/redirect" + path,
    query,
    params
  });
}

function handleCloseCurrent() {
  if (!activeTab.value) return;
  const { path, fixedTag } = activeTab.value;
  if (fixedTag) return;
  useMultiTagsStoreHook().handleTags("splice", path);
  if (route.path === path || route.path.startsWith(path + "/")) {
    const remaining = useMultiTagsStoreHook().multiTags;
    if (remaining.length > 0) {
      router.push(remaining[remaining.length - 1].path);
    } else {
      router.push("/");
    }
  }
  activeTab.value = null;
}

function handleCloseOthers() {
  if (!activeTab.value) return;
  const { path } = activeTab.value;
  const store = useMultiTagsStoreHook();
  const removePaths = store.multiTags
    .filter(t => !t?.meta?.fixedTag && t.path !== path)
    .map(t => t.path);
  removePaths.forEach(p => store.handleTags("splice", p));
}

function handleCloseAll() {
  const store = useMultiTagsStoreHook();
  const removePaths = store.multiTags
    .filter(t => !t?.meta?.fixedTag)
    .map(t => t.path);
  removePaths.forEach(p => store.handleTags("splice", p));
  const remaining = store.multiTags;
  if (remaining.length > 0) {
    router.push(remaining[remaining.length - 1].path);
  } else {
    router.push("/");
  }
  activeTab.value = null;
}
</script>

<style scoped>
.icon-tabs-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-tabs {
  display: flex;
  align-items: center;
  gap: 2px;
}

.icon-tab {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  cursor: pointer;
  color: #909399;
  transition: all 0.15s;
}

.icon-tab:hover {
  background: #f0f2f5;
  color: #606266;
}

.icon-tab.active {
  background: #ecf5ff;
  color: #409eff;
}

/* 操作按钮组 */
.tab-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  padding-left: 8px;
  border-left: 1px solid #e5e6eb;
}

.tab-action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  cursor: pointer;
  color: #909399;
  transition: all 0.15s;
}

.tab-action-btn:hover {
  background: #f0f2f5;
  color: #409eff;
}

.tab-action-btn.disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.tab-action-btn.disabled:hover {
  background: transparent;
  color: #909399;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateX(-8px);
}
</style>
