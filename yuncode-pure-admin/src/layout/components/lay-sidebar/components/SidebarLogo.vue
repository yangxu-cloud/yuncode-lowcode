<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import { useNav } from "@/layout/hooks/useNav";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";
import { usePermissionStoreHook } from "@/store/modules/permission";

defineProps({
  collapse: Boolean
});

const { t } = useI18n();
const router = useRouter();
const route = useRoute();
const { title } = useNav();
const isOpen = ref(false);
const rootEl = ref<HTMLElement | null>(null);
const panelWidth = ref(Math.floor(window.innerWidth * 0.5));
let hoverTimer: ReturnType<typeof setTimeout> | null = null;
let leaveTimer: ReturnType<typeof setTimeout> | null = null;

function updatePanelWidth() {
  panelWidth.value = Math.floor(window.innerWidth * 0.5);
}

const categories = computed(() => {
  return usePermissionStoreHook().wholeMenus.filter(
    m => m?.meta?.showLink !== false && m?.children?.length > 0
  );
});

const categoryIconColors = [
  { bg: "#eef4ff", color: "#4479f4" },
  { bg: "#eafaf0", color: "#2eb45a" },
  { bg: "#fff5eb", color: "#f0883e" },
  { bg: "#f5eefc", color: "#8b5cf6" },
  { bg: "#fef3f2", color: "#ef4444" },
  { bg: "#ecfdf5", color: "#10b981" }
];

function getCategoryColor(i: number) {
  return categoryIconColors[i % categoryIconColors.length];
}

function open() {
  if (leaveTimer) { clearTimeout(leaveTimer); leaveTimer = null; }
  hoverTimer = setTimeout(() => { isOpen.value = true; }, 50);
}

function close() {
  if (hoverTimer) { clearTimeout(hoverTimer); hoverTimer = null; }
  leaveTimer = setTimeout(() => { isOpen.value = false; }, 150);
}

function cancelClose() {
  if (leaveTimer) { clearTimeout(leaveTimer); leaveTimer = null; }
}

function navigateTo(child) {
  router.push(child.path);
  isOpen.value = false;
}

function handleClickOutside(e: MouseEvent) {
  if (!isOpen.value) return;
  const target = e.target as Node;
  if (rootEl.value && rootEl.value.contains(target)) return;
  const panel = document.querySelector(".console-panel");
  if (panel && panel.contains(target)) return;
  isOpen.value = false;
}

onMounted(() => {
  document.addEventListener("click", handleClickOutside);
  window.addEventListener("resize", updatePanelWidth);
});

onBeforeUnmount(() => {
  document.removeEventListener("click", handleClickOutside);
  window.removeEventListener("resize", updatePanelWidth);
  if (hoverTimer) clearTimeout(hoverTimer);
  if (leaveTimer) clearTimeout(leaveTimer);
});
</script>

<template>
  <div ref="rootEl" class="sidebar-logo-container" :class="{ collapses: collapse }">
    <!-- 展开状态：汉堡图标 + 控制台 -->
    <div v-if="!collapse" class="logo-expand">
      <div
        class="hamburger-btn"
        :class="{ active: isOpen }"
        @mouseenter="open"
        @mouseleave="close"
        @click="isOpen ? (isOpen = false) : open()"
      >
        <!-- 面板打开时显示 X，否则显示控制台图标 -->
        <svg v-if="!isOpen" width="18" height="18" viewBox="0 0 20 20" fill="none">
          <rect x="2" y="2" width="7" height="7" rx="1.5" fill="currentColor"/>
          <rect x="11" y="2" width="7" height="7" rx="1.5" fill="currentColor"/>
          <rect x="2" y="11" width="7" height="7" rx="1.5" fill="currentColor"/>
          <rect x="11" y="11" width="7" height="7" rx="1.5" fill="currentColor"/>
        </svg>
        <svg v-else width="18" height="18" viewBox="0 0 20 20" fill="none">
          <path d="M5 5L15 15M15 5L5 15" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
        </svg>
      </div>
    </div>

    <!-- 折叠状态：只显示汉堡图标 -->
    <div
      v-else
      class="logo-collapse"
    >
      <div
        class="hamburger-btn"
        :class="{ active: isOpen }"
        @mouseenter="open"
        @mouseleave="close"
        @click="isOpen ? (isOpen = false) : open()"
      >
        <svg v-if="!isOpen" width="18" height="18" viewBox="0 0 20 20" fill="none">
          <rect x="2" y="2" width="7" height="7" rx="1.5" fill="currentColor"/>
          <rect x="11" y="2" width="7" height="7" rx="1.5" fill="currentColor"/>
          <rect x="2" y="11" width="7" height="7" rx="1.5" fill="currentColor"/>
          <rect x="11" y="11" width="7" height="7" rx="1.5" fill="currentColor"/>
        </svg>
        <svg v-else width="18" height="18" viewBox="0 0 20 20" fill="none">
          <path d="M5 5L15 15M15 5L5 15" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
        </svg>
      </div>
    </div>

    <!-- 下拉面板 -->
    <Teleport to="body">
      <Transition name="console-slide">
        <div
          v-if="isOpen"
          class="console-panel"
          @mouseenter="cancelClose"
          @mouseleave="close"
          :style="{
            top: '48px',
            left: '0px',
            width: panelWidth + 'px'
          }"
        >
          <div class="cp-header">
                        <div class="panel-brand">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                <rect x="2" y="2" width="16" height="16" rx="3" fill="#165dff" opacity="0.1"/>
                <rect x="5" y="5" width="10" height="10" rx="2" fill="#165dff"/>
              </svg>
              <span class="panel-title">功能导航</span>
            </div>
          </div>
          <div class="cp-body">
            <div
              v-for="(cat, ci) in categories"
              :key="cat.path"
              class="cp-section"
            >
              <div class="cp-section-head">
                <div
                  class="cp-section-icon"
                  :style="{
                    backgroundColor: getCategoryColor(ci).bg,
                    color: getCategoryColor(ci).color
                  }"
                >
                  <component :is="useRenderIcon(cat.meta?.icon)" v-if="cat.meta?.icon" />
                  <span v-else>📁</span>
                </div>
                <span class="cp-section-name">
                  {{ t(cat.meta?.title || cat.meta?.i18nKey || "") }}
                </span>
              </div>
              <div class="cp-section-list">
                <div
                  v-for="child in cat.children.filter(c => c?.meta?.showLink !== false && !c?.meta?.hideMenu)"
                  :key="child.path"
                  class="cp-item"
                  @click="navigateTo(child)"
                >
                  <div
                    class="cp-item-icon"
                    :style="{
                      backgroundColor: getCategoryColor(ci).bg,
                      color: getCategoryColor(ci).color
                    }"
                  >
                    <component :is="useRenderIcon(child.meta?.icon)" v-if="child.meta?.icon" />
                    <span v-else>📄</span>
                  </div>
                  <span class="cp-item-label">{{ t(child.meta?.title || child.meta?.i18nKey || "") }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style lang="scss" scoped>
.sidebar-logo-container {
  position: relative;
  width: 100%;
  height: 48px;
  overflow: visible;

  .logo-expand {
    display: flex;
    align-items: center;
    height: 100%;
    padding-left: 10px;
    gap: 10px;
  }

  .hamburger-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    border-radius: 6px;
    cursor: pointer;
    color: var(--pure-theme-sub-menu-active-text, #fff);
    transition: all 0.2s;
    flex-shrink: 0;

    &:hover,
    &.active {
      background: rgba(255, 255, 255, 0.12);
    }
  }

  .logo-text {
    font-size: 16px;
    font-weight: 600;
    color: var(--pure-theme-sub-menu-active-text, #fff);
    white-space: nowrap;
  }

  .logo-collapse {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
  }

  .sidebar-logo-link {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;

    img {
      height: 28px;
    }
  }
}
</style>

<style>
/* 控制台面板 - 现代设计 */
.console-panel {
  position: fixed;
  height: 80vh;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border-radius: 0 14px 14px 0;
  box-shadow:
    0 0 0 1px rgba(0, 0, 0, 0.04),
    0 4px 16px rgba(0, 0, 0, 0.08),
    0 16px 48px rgba(0, 0, 0, 0.12);
  z-index: 9999;
  overflow: hidden;
  will-change: transform, opacity;
}

/* Header */
.cp-header {
  padding: 16px 20px 12px;
  font-size: 18px;
  font-weight: 700;
  color: #1d2129;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
}

/* Body */
.cp-body {
  flex: 1;
  min-height: 0;
  padding: 20px 24px;
  max-height: 70vh;
  overflow-y: auto;

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(0, 0, 0, 0.1);
    border-radius: 4px;

    &:hover {
      background: rgba(0, 0, 0, 0.18);
    }
  }
}

/* Section */
.cp-section {
  margin-bottom: 24px;

  &:last-child {
    margin-bottom: 0;
  }
}

.cp-section-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  padding: 0 4px;
}

.cp-section-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  font-size: 13px;
  flex-shrink: 0;
  transition: transform 0.2s ease;

  svg, i {
    font-size: 14px;
  }
}

.cp-section:hover .cp-section-icon {
  transform: scale(1.08);
}

.cp-section-name {
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
  letter-spacing: -0.01em;
}

/* Children grid */
.cp-section-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.cp-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  width: 80px;
  padding: 14px 6px 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 12px;
  color: #4e5969;
  text-align: center;
  background: transparent;

  &:hover {
    background: #f5f7fa;
    color: #1a1d26;
  }

  &:active {
    transform: scale(0.96);
  }
}

.cp-item-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  font-size: 18px;
  flex-shrink: 0;
  transition: transform 0.2s ease;

  svg, i {
    font-size: 20px;
  }
}

.cp-item:hover .cp-item-icon {
  transform: scale(1.06);
}

.cp-item-label {
  line-height: 1.3;
  white-space: nowrap;
}

/* 面板动画 - 从左侧滑入 */
.console-slide-enter-active {
  transition:
    opacity 0.28s cubic-bezier(0.16, 1, 0.3, 1),
    transform 0.35s cubic-bezier(0.16, 1, 0.3, 1);
}

.console-slide-leave-active {
  transition:
    opacity 0.2s cubic-bezier(0.4, 0, 1, 1),
    transform 0.2s cubic-bezier(0.4, 0, 0.7, 0.4);
}

.console-slide-enter-from {
  opacity: 0;
  transform: translateX(-24px);
}

.console-slide-leave-to {
  opacity: 0;
  transform: translateX(-16px);
}
</style>
