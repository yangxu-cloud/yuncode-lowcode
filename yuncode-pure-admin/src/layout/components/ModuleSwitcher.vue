<template>
  <div class="module-switcher">
    <div
      ref="triggerEl"
      class="module-switcher-trigger"
      :class="{ active: isOpen }"
      @click="isOpen = !isOpen"
    >
      <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
        <rect x="2" y="4" width="16" height="1.5" rx="0.75" fill="currentColor"/>
        <rect x="2" y="9.25" width="16" height="1.5" rx="0.75" fill="currentColor"/>
        <rect x="2" y="14.5" width="16" height="1.5" rx="0.75" fill="currentColor"/>
      </svg>
    </div>

    <Teleport to="body">
      <Transition name="dropdown">
        <div
          v-if="isOpen"
          ref="panelEl"
          class="module-dropdown-panel"
        >
          <div class="panel-header">
            <div class="panel-brand">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                <rect x="2" y="2" width="16" height="16" rx="3" fill="#165dff" opacity="0.1"/>
                <rect x="5" y="5" width="10" height="10" rx="2" fill="#165dff"/>
              </svg>
              <span class="panel-title">功能导航1</span>
            </div>
            <button class="panel-close" @click="isOpen = false">
              <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
                <path d="M1 1L11 11M11 1L1 11" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
              </svg>
            </button>
          </div>
          <div class="panel-body">
            <div
              v-for="(cat, catIndex) in categories"
              :key="cat.path"
              class="panel-category"
            >
              <div class="cat-title">{{ t(cat.meta?.title || cat.meta?.i18nKey || "") }}</div>
              <div class="cat-grid">
                <div
                  v-for="(child, childIndex) in cat.children.filter(c => c?.meta?.showLink !== false && !c?.meta?.hideMenu)"
                  :key="child.path"
                  class="cat-grid-item"
                  @click="navigateTo(child)"
                >
                  <div
                    class="cat-grid-icon"
                    :style="{
                      backgroundColor: getColor(catIndex * 10 + childIndex).bg,
                      color: getColor(catIndex * 10 + childIndex).color
                    }"
                  >
                    <component
                      :is="useRenderIcon(child.meta?.icon)"
                      v-if="child.meta?.icon"
                    />
                    <span v-else style="font-size: 18px;">📄</span>
                  </div>
                  <span class="cat-grid-label">
                    {{ t(child.meta?.title || child.meta?.i18nKey || "") }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from "vue";
import { useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";
import { usePermissionStoreHook } from "@/store/modules/permission";

const { t } = useI18n();
const router = useRouter();
const isOpen = ref(false);
const triggerEl = ref<HTMLElement | null>(null);
const panelEl = ref<HTMLElement | null>(null);

const categories = computed(() => {
  return usePermissionStoreHook().wholeMenus.filter(
    m => m?.meta?.showLink !== false && m?.children?.length > 0
  );
});

const iconColors = [
  { bg: "#fff1f0", color: "#ff4d4f" },
  { bg: "#e6f7ff", color: "#1890ff" },
  { bg: "#f6ffed", color: "#52c41a" },
  { bg: "#fff7e6", color: "#fa8c16" },
  { bg: "#f9f0ff", color: "#722ed1" },
  { bg: "#e6fffb", color: "#13c2c2" },
  { bg: "#fff0f6", color: "#eb2f96" },
  { bg: "#f0f5ff", color: "#2f54eb" },
];

function getColor(index: number) {
  return iconColors[index % iconColors.length];
}

function navigateTo(child: any) {
  router.push(child.path);
  isOpen.value = false;
}

function handleClickOutside(e: MouseEvent) {
  if (!isOpen.value) return;
  const trigger = triggerEl.value;
  const panel = panelEl.value;
  const target = e.target as Node;
  if (trigger && trigger.contains(target)) return;
  if (panel && panel.contains(target)) return;
  isOpen.value = false;
}

onMounted(() => {
  document.addEventListener("click", handleClickOutside);
});

onBeforeUnmount(() => {
  document.removeEventListener("click", handleClickOutside);
});
</script>

<style scoped>
.module-switcher {
  position: relative;
  display: flex;
  align-items: center;
  height: 48px;
}

.module-switcher-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  cursor: pointer;
  color: #4e5969;
  transition: all 0.2s;
  border-radius: 6px;
}

.module-switcher-trigger:hover,
.module-switcher-trigger.active {
  background-color: rgba(0, 0, 0, 0.04);
  color: #165dff;
}
</style>

<style>
.module-dropdown-panel {
  position: fixed;
  top: 48px;
  left: 0;
  width: 100%;
  max-width: 960px;
  background: #fff;
  border-radius: 0 0 12px 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  z-index: 9999;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid #f2f3f5;
}

.panel-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d2129;
}

.panel-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  color: #86909c;
  transition: all 0.12s;
}

.panel-close:hover {
  background: #f2f3f5;
  color: #4e5969;
}

.panel-body {
  padding: 20px 24px;
  max-height: 70vh;
  overflow-y: auto;
}

.panel-category {
  margin-bottom: 24px;
}

.panel-category:last-child {
  margin-bottom: 0;
}

.cat-title {
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
  margin-bottom: 12px;
}

.cat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 12px;
}

.cat-grid-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
}

.cat-grid-item:hover {
  background: #f5f7fa;
}

.cat-grid-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  font-size: 20px;
  transition: transform 0.15s;
}

.cat-grid-item:hover .cat-grid-icon {
  transform: scale(1.05);
}

.cat-grid-label {
  font-size: 12px;
  color: #4e5969;
  text-align: center;
  line-height: 1.4;
}

.dropdown-enter-active {
  transition: opacity 0.15s ease, transform 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.dropdown-leave-active {
  transition: opacity 0.1s ease, transform 0.1s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
