<script setup lang="ts">
import { match } from "pinyin-pro";
import { getConfig } from "@/config";
import { useRouter } from "vue-router";
import SearchResult from "./components/SearchResult.vue";
import SearchFooter from "./components/SearchFooter.vue";
import SearchHistory from "./components/SearchHistory.vue";
import type { optionsItem, dragItem } from "./types";
import { ref, computed, shallowRef, nextTick, watch, onBeforeUnmount } from "vue";
import { useDebounceFn } from "@vueuse/core";
import { usePermissionStoreHook } from "@/store/modules/permission";
import { cloneDeep, isAllEmpty, storageLocal } from "@pureadmin/utils";

const router = useRouter();

const HISTORY_TYPE = "history";
const COLLECT_TYPE = "collect";
const LOCALEHISTORYKEY = "menu-search-history";
const LOCALECOLLECTKEY = "menu-search-collect";

const isExpanded = ref(false);
const keyword = ref("");
const resultRef = ref();
const historyRef = ref();
const scrollbarRef = ref();
const activePath = ref("");
const historyPath = ref("");
const resultOptions = shallowRef([]);
const historyOptions = shallowRef([]);
const handleSearch = useDebounceFn(search, 300);
const historyNum = getConfig().MenuSearchHistory;
const inputRef = ref<HTMLInputElement | null>(null);
const wrapRef = ref<HTMLElement | null>(null);
const iconRef = ref<HTMLElement | null>(null);
const dropdownStyle = ref<Record<string, string>>({});
const dropdownReady = ref(false);

const menusData = computed(() => {
  return cloneDeep(usePermissionStoreHook().wholeMenus);
});

const showSearchResult = computed(() => {
  return keyword.value && resultOptions.value.length > 0;
});

const showSearchHistory = computed(() => {
  return !keyword.value && historyOptions.value.length > 0;
});

const showEmpty = computed(() => {
  return (
    (!keyword.value && historyOptions.value.length === 0) ||
    (keyword.value && resultOptions.value.length === 0)
  );
});

const showDropdown = computed(() => {
  return isExpanded.value && (showSearchResult.value || showSearchHistory.value || showEmpty.value);
});

function updateDropdownPos() {
  nextTick(() => {
    setTimeout(() => {
      const el = iconRef.value;
      if (!el) return;
      const rect = el.getBoundingClientRect();
      dropdownStyle.value = {
        position: "fixed",
        top: rect.bottom + 4 + "px",
        left: (rect.left - 224) + "px"
      };
      dropdownReady.value = true;
    }, 320);
  });
}

watch(showDropdown, (val) => {
  if (val) updateDropdownPos();
});

function getStorageItem(key) {
  return storageLocal().getItem<optionsItem[]>(key) || [];
}

function setStorageItem(key, value) {
  storageLocal().setItem(key, value);
}

function flatTree(arr) {
  const res = [];
  function deep(arr) {
    arr.forEach(item => {
      res.push(item);
      item.children && deep(item.children);
    });
  }
  deep(arr);
  return res;
}

function search() {
  const flatMenusData = flatTree(menusData.value);
  resultOptions.value = flatMenusData.filter(menu =>
    keyword.value
      ? menu.meta?.title
          .toLocaleLowerCase()
          .includes(keyword.value.toLocaleLowerCase().trim()) ||
        !isAllEmpty(
          match(
            menu.meta?.title.toLocaleLowerCase(),
            keyword.value.toLocaleLowerCase().trim()
          )
        )
      : false
  );
  activePath.value =
    resultOptions.value?.length > 0 ? resultOptions.value[0].path : "";
}

function scrollTo(index) {
  const ref = resultOptions.value.length ? resultRef.value : historyRef.value;
  const scrollTop = ref.handleScroll(index);
  scrollbarRef.value.setScrollTop(scrollTop);
}

function getCurrentOptionsAndPath() {
  const isResultOptions = resultOptions.value.length > 0;
  const options = isResultOptions ? resultOptions.value : historyOptions.value;
  const currentPath = isResultOptions ? activePath.value : historyPath.value;
  return { options, currentPath, isResultOptions };
}

function updatePathAndScroll(newIndex, isResultOptions) {
  if (isResultOptions) {
    activePath.value = resultOptions.value[newIndex].path;
  } else {
    historyPath.value = historyOptions.value[newIndex].path;
  }
  scrollTo(newIndex);
}

function handleUp() {
  const { options, currentPath, isResultOptions } = getCurrentOptionsAndPath();
  if (options.length === 0) return;
  const index = options.findIndex(item => item.path === currentPath);
  const prevIndex = (index - 1 + options.length) % options.length;
  updatePathAndScroll(prevIndex, isResultOptions);
}

function handleDown() {
  const { options, currentPath, isResultOptions } = getCurrentOptionsAndPath();
  if (options.length === 0) return;
  const index = options.findIndex(item => item.path === currentPath);
  const nextIndex = (index + 1) % options.length;
  updatePathAndScroll(nextIndex, isResultOptions);
}

function handleEnter() {
  const { options, currentPath, isResultOptions } = getCurrentOptionsAndPath();
  if (options.length === 0 || currentPath === "") return;
  const index = options.findIndex(item => item.path === currentPath);
  if (index === -1) return;
  if (isResultOptions) {
    saveHistory();
  } else {
    updateHistory();
  }
  router.push(options[index].path);
  collapse();
}

function handleDelete(item) {
  const key = item.type === HISTORY_TYPE ? LOCALEHISTORYKEY : LOCALECOLLECTKEY;
  let list = getStorageItem(key);
  list = list.filter(listItem => listItem.path !== item.path);
  setStorageItem(key, list);
  getHistory();
}

function handleCollect(item) {
  let searchHistoryList = getStorageItem(LOCALEHISTORYKEY);
  let searchCollectList = getStorageItem(LOCALECOLLECTKEY);
  searchHistoryList = searchHistoryList.filter(
    historyItem => historyItem.path !== item.path
  );
  setStorageItem(LOCALEHISTORYKEY, searchHistoryList);
  if (!searchCollectList.some(collectItem => collectItem.path === item.path)) {
    searchCollectList.unshift({ ...item, type: COLLECT_TYPE });
    setStorageItem(LOCALECOLLECTKEY, searchCollectList);
  }
  getHistory();
}

function saveHistory() {
  const { path, meta } = resultOptions.value.find(
    item => item.path === activePath.value
  );
  const searchHistoryList = getStorageItem(LOCALEHISTORYKEY);
  const searchCollectList = getStorageItem(LOCALECOLLECTKEY);
  const isCollected = searchCollectList.some(item => item.path === path);
  const existingIndex = searchHistoryList.findIndex(item => item.path === path);
  if (!isCollected) {
    if (existingIndex !== -1) searchHistoryList.splice(existingIndex, 1);
    if (searchHistoryList.length >= historyNum) searchHistoryList.pop();
    searchHistoryList.unshift({ path, meta, type: HISTORY_TYPE });
    storageLocal().setItem(LOCALEHISTORYKEY, searchHistoryList);
  }
}

function updateHistory() {
  let searchHistoryList = getStorageItem(LOCALEHISTORYKEY);
  const historyIndex = searchHistoryList.findIndex(
    item => item.path === historyPath.value
  );
  if (historyIndex !== -1) {
    const [historyItem] = searchHistoryList.splice(historyIndex, 1);
    searchHistoryList.unshift(historyItem);
    setStorageItem(LOCALEHISTORYKEY, searchHistoryList);
  }
}

function getHistory() {
  const searchHistoryList = getStorageItem(LOCALEHISTORYKEY);
  const searchCollectList = getStorageItem(LOCALECOLLECTKEY);
  historyOptions.value = [...searchHistoryList, ...searchCollectList];
  historyPath.value = historyOptions.value[0]?.path;
}

function handleDrag(item: dragItem) {
  const searchCollectList = getStorageItem(LOCALECOLLECTKEY);
  const [reorderedItem] = searchCollectList.splice(item.oldIndex, 1);
  searchCollectList.splice(item.newIndex, 0, reorderedItem);
  storageLocal().setItem(LOCALECOLLECTKEY, searchCollectList);
  historyOptions.value = [
    ...getStorageItem(LOCALEHISTORYKEY),
    ...getStorageItem(LOCALECOLLECTKEY)
  ];
  historyPath.value = reorderedItem.path;
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === "Escape") {
    e.preventDefault();
    collapse();
  } else if (e.key === "ArrowUp") {
    e.preventDefault();
    handleUp();
  } else if (e.key === "ArrowDown") {
    e.preventDefault();
    handleDown();
  } else if (e.key === "Enter") {
    e.preventDefault();
    handleEnter();
  }
}

function handleClickOutside(e: MouseEvent) {
  if (!isExpanded.value) return;
  const target = e.target as HTMLElement;
  const dropdown = document.querySelector(".search-dropdown");
  if (dropdown && dropdown.contains(target)) return;
  if (iconRef.value && iconRef.value.contains(target)) return;
  if (inputRef.value && inputRef.value.contains(target)) return;
  collapse();
}

function expand() {
  isExpanded.value = true;
  nextTick(() => {
    inputRef.value?.focus();
    getHistory();
  });
}

function collapse() {
  isExpanded.value = false;
  dropdownReady.value = false;
  keyword.value = "";
  resultOptions.value = [];
  historyOptions.value = [];
  activePath.value = "";
  historyPath.value = "";
}

watch(isExpanded, (val) => {
  if (val) {
    document.addEventListener("mousedown", handleClickOutside);
  } else {
    document.removeEventListener("mousedown", handleClickOutside);
  }
});

onBeforeUnmount(() => {
  document.removeEventListener("mousedown", handleClickOutside);
});
</script>

<template>
  <div ref="wrapRef" class="inline-search" :class="{ expanded: isExpanded }">
    <!-- 搜索图标（始终在右侧） -->
    <div ref="iconRef" class="search-icon" @click="isExpanded ? collapse() : expand()">
      <svg width="16" height="16" viewBox="0 0 20 20" fill="none">
        <circle cx="8.5" cy="8.5" r="6" stroke="currentColor" stroke-width="1.5"/>
        <path d="M13 13L17 17" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
      </svg>
    </div>
    <!-- 左侧展开的 input -->
    <transition name="search-expand">
      <div v-if="isExpanded" class="search-input-wrap">
        <input
          ref="inputRef"
          v-model="keyword"
          type="text"
          placeholder="搜索菜单..."
          class="search-input"
          @input="handleSearch"
          @keydown="onKeydown"
        />
      </div>
    </transition>
    <!-- 下拉结果 -->
    <Teleport to="body">
      <div v-if="showDropdown" class="search-dropdown" :class="{ 'dropdown-visible': dropdownReady }" :style="dropdownStyle" @mousedown.prevent>
        <el-scrollbar ref="scrollbarRef" max-height="320px">
          <el-empty v-if="showEmpty" description="暂无搜索结果" :image-size="60" />
          <SearchHistory
            v-if="showSearchHistory"
            ref="historyRef"
            v-model:value="historyPath"
            :options="historyOptions"
            @click="handleEnter"
            @delete="handleDelete"
            @collect="handleCollect"
            @drag="handleDrag"
          />
          <SearchResult
            v-if="showSearchResult"
            ref="resultRef"
            v-model:value="activePath"
            :options="resultOptions"
            @click="handleEnter"
          />
        </el-scrollbar>
        <div class="search-dropdown-footer">
          <SearchFooter :total="resultOptions.length" />
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.inline-search {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  height: 48px;
}

.search-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  cursor: pointer;
  color: #646a73;
  border-radius: 8px;
  transition: all 0.2s ease;
  flex-shrink: 0;
  order: 1;
}

.search-icon:hover {
  background: rgba(0, 0, 0, 0.04);
  color: #1d2129;
}

.search-input-wrap {
  order: 0;
  width: 220px;
  flex-shrink: 0;
  overflow: hidden;
}

.search-input {
  width: 100%;
  height: 34px;
  padding: 0 12px;
  font-size: 14px;
  color: #1d2129;
  background: #f7f8fa;
  border: 1.5px solid transparent;
  border-radius: 10px;
  outline: none;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  box-sizing: border-box;
}

.search-input:focus {
  border-color: var(--el-color-primary, #165dff);
  background: #fff;
  box-shadow: 0 0 0 3px rgba(22, 93, 255, 0.08);
}

.search-input::placeholder {
  color: #a9aeb8;
  font-weight: 400;
}

/* 展开动画 */
.search-expand-enter-active {
  transition: width 0.3s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.2s ease;
}

.search-expand-leave-active {
  transition: width 0.2s ease, opacity 0.15s ease;
}

.search-expand-enter-from,
.search-expand-leave-to {
  width: 0;
  opacity: 0;
}
</style>

<style>
/* 下拉结果面板 */
.search-dropdown {
  width: 320px;
  background: #fff;
  border-radius: 12px;
  box-shadow:
    0 12px 40px rgba(0, 0, 0, 0.08),
    0 4px 12px rgba(0, 0, 0, 0.04),
    0 0 0 1px rgba(0, 0, 0, 0.03);
  z-index: 10001;
  overflow: hidden;
  padding: 6px;
  box-sizing: border-box;
  opacity: 0;
  transform: translateY(-4px);
  transition: opacity 0.15s ease, transform 0.15s ease;
  pointer-events: none;
}

.search-dropdown.dropdown-visible {
  opacity: 1;
  transform: translateY(0);
  pointer-events: auto;
}

.search-dropdown-footer {
  padding: 8px 10px;
  border-top: 1px solid #f2f3f5;
  margin-top: 2px;
}
</style>
