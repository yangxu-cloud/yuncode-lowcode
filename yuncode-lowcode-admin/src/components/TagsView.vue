<template>
  <div class="tags-view-container">
    <el-scrollbar class="tags-view-wrapper">
      <div class="tags-view-list">
        <div
          v-for="tag in visitedViews"
          :key="tag.path"
          :class="['tags-view-item', { active: isActive(tag) }]"
          @click="clickTag(tag)"
          @contextmenu.prevent="openContextMenu($event, tag)"
        >
          <span class="tag-title">{{ tag.title }}</span>
          <el-icon
            v-if="!isAffix(tag)"
            class="tag-close"
            @click.stop="closeSelectedTag(tag)"
          >
            <Close />
          </el-icon>
        </div>
      </div>
    </el-scrollbar>

    <!-- 右键菜单 -->
    <ul
      v-show="contextMenuVisible"
      :style="{ left: left + 'px', top: top + 'px' }"
      class="contextmenu"
    >
      <li @click="refreshSelectedTag(selectedTag)">
        <el-icon><Refresh /></el-icon>
        <span>{{ t('tagsView.refresh') }}</span>
      </li>
      <li v-if="!isAffix(selectedTag)" @click="closeSelectedTag(selectedTag)">
        <el-icon><Close /></el-icon>
        <span>{{ t('tagsView.close') }}</span>
      </li>
      <li @click="closeOthersTags">
        <el-icon><CircleClose /></el-icon>
        <span>{{ t('tagsView.closeOthers') }}</span>
      </li>
      <li @click="closeAllTags(selectedTag)">
        <el-icon><FolderDelete /></el-icon>
        <span>{{ t('tagsView.closeAll') }}</span>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from "vue";
import { useRoute, useRouter, RouteLocationNormalized } from "vue-router";
import { useI18n } from "vue-i18n";
import {
  Close,
  Refresh,
  CircleClose,
  FolderDelete
} from "@element-plus/icons-vue";

const { t } = useI18n();
const route = useRoute();
const router = useRouter();

interface TagView {
  path: string;
  title: string;
  name?: string;
  affix?: boolean;
}

// 固定的标签（首页）
const affixTags = ref<TagView[]>([
  {
    path: "/home",
    title: t('menu.home'),
    affix: true
  }
]);

// 访问过的视图
const visitedViews = ref<TagView[]>([...affixTags.value]);

// 右键菜单相关
const contextMenuVisible = ref(false);
const selectedTag = ref<TagView>({} as TagView);
const left = ref(0);
const top = ref(0);

// 判断是否是当前激活的标签
const isActive = (tag: TagView) => {
  return tag.path === route.path;
};

// 判断是否是固定标签
const isAffix = (tag: TagView) => {
  return tag.affix;
};

// 添加标签
const addView = (view: TagView) => {
  // 检查是否已存在
  if (visitedViews.value.some(v => v.path === view.path)) {
    return;
  }
  visitedViews.value.push(view);
  // 持久化到 localStorage
  saveTags();
};

// 点击标签
const clickTag = (tag: TagView) => {
  if (tag.path === route.path) {
    return;
  }
  router.push(tag.path);
};

// 关闭选中的标签
const closeSelectedTag = (view: TagView) => {
  const index = visitedViews.value.findIndex(v => v.path === view.path);
  if (index !== -1) {
    visitedViews.value.splice(index, 1);
    saveTags();

    // 如果关闭的是当前激活的标签，跳转到最后一个标签
    if (isActive(view)) {
      const lastView = visitedViews.value[visitedViews.value.length - 1];
      if (lastView) {
        router.push(lastView.path);
      }
    }
  }
};

// 刷新选中的标签
const refreshSelectedTag = (view: TagView) => {
  // 通过移除再添加来实现刷新
  router.replace({ path: "/redirect" + view.path });
};

// 关闭其他标签
const closeOthersTags = () => {
  const currentView = selectedTag.value;
  visitedViews.value = visitedViews.value.filter(tag => {
    return tag.affix || tag.path === currentView.path;
  });
  saveTags();
  contextMenuVisible.value = false;
};

// 关闭所有标签
const closeAllTags = (view: TagView) => {
  visitedViews.value = visitedViews.value.filter(tag => tag.affix);
  saveTags();
  if (isActive(view)) {
    router.push("/home");
  }
  contextMenuVisible.value = false;
};

// 打开右键菜单
const openContextMenu = (e: MouseEvent, tag: TagView) => {
  const minTop = 60; // 最小top值，防止菜单超出屏幕
  const maxTop = window.innerHeight - 100; // 最大top值

  left.value = e.clientX;
  top.value = e.clientY < minTop ? minTop : e.clientX > maxTop ? maxTop : e.clientY;

  selectedTag.value = tag;
  contextMenuVisible.value = true;
};

// 保存标签到 localStorage
const saveTags = () => {
  localStorage.setItem("tagsView", JSON.stringify(visitedViews.value));
};

// 从 localStorage 恢复标签
const restoreTags = () => {
  const savedTags = localStorage.getItem("tagsView");
  if (savedTags) {
    try {
      const tags = JSON.parse(savedTags);
      // 确保固定的标签始终存在
      const affixPaths = affixTags.value.map(t => t.path);
      const filteredTags = tags.filter((tag: TagView) => {
        // 如果是固定标签，只保留一个
        if (affixPaths.includes(tag.path)) {
          return affixTags.value.some(t => t.path === tag.path);
        }
        return true;
      });

      // 合并固定标签和恢复的标签
      visitedViews.value = [
        ...affixTags.value,
        ...filteredTags.filter((tag: TagView) => !affixPaths.includes(tag.path))
      ];
    } catch (e) {
      console.error("Failed to restore tags:", e);
      visitedViews.value = [...affixTags.value];
    }
  }
};

// 监听路由变化
watch(
  () => route.path,
  () => {
    // 添加当前路由到标签
    const currentRoute: TagView = {
      path: route.path,
      title: (route.meta?.title as string) || route.path,
      name: route.name as string
    };
    addView(currentRoute);
  },
  { immediate: true }
);

// 点击其他地方关闭右键菜单
const closeContextMenu = () => {
  contextMenuVisible.value = false;
};

onMounted(() => {
  restoreTags();
  document.addEventListener("click", closeContextMenu);
});
</script>

<style scoped lang="scss">
.tags-view-container {
  height: 40px;
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);

  .tags-view-wrapper {
    height: 100%;
    padding: 0 16px;

    .tags-view-list {
      display: flex;
      align-items: center;
      height: 100%;
      white-space: nowrap;

      .tags-view-item {
        position: relative;
        display: inline-flex;
        align-items: center;
        height: 28px;
        padding: 0 12px;
        margin-right: 6px;
        font-size: 12px;
        color: #666;
        cursor: pointer;
        background-color: #f0f2f5;
        border: 1px solid #e6e6e6;
        border-radius: 3px;
        transition: all 0.2s;

        &:hover {
          .tag-close {
            opacity: 1;
          }
        }

        .tag-title {
          max-width: 120px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .tag-close {
          flex-shrink: 0;
          width: 14px;
          height: 14px;
          margin-left: 6px;
          font-size: 14px;
          opacity: 0;
          transition: opacity 0.2s;

          &:hover {
            color: #f56c6c;
          }
        }
      }
    }
  }
}

.contextmenu {
  position: fixed;
  z-index: 9999;
  padding: 5px 0;
  font-size: 12px;
  color: #333;
  background-color: #fff;
  border: 1px solid #e6e6e6;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  list-style: none;

  li {
    display: flex;
    align-items: center;
    padding: 8px 16px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      background-color: #f0f2f5;
    }

    .el-icon {
      margin-right: 8px;
      font-size: 14px;
    }
  }
}

// 暗色主题
:deep(.dark) {
  .contextmenu {
    background-color: #1d1e1f;
    border-color: #2c2e2f;

    li {
      color: #e5e5e5;

      &:hover {
        background-color: #2c2e2f;
      }
    }
  }
}
</style>
