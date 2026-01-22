<template>
  <div class="search-container" @click="openSearch">
    <el-tooltip :content="t('menus.search')" placement="bottom" effect="light">
      <div class="search-icon">
        <el-icon :size="18">
          <SearchIcon />
        </el-icon>
      </div>
    </el-tooltip>

    <!-- 搜索对话框 -->
    <el-dialog
      v-model="searchVisible"
      title="全局搜索"
      :width="600"
      :modal="true"
      :close-on-click-modal="true"
      :show-close="true"
      @close="handleClose"
      class="search-dialog"
    >
      <el-input
        ref="searchInputRef"
        v-model="searchKeyword"
        placeholder="搜索菜单、页面、功能..."
        :prefix-icon="SearchIcon"
        clearable
        @input="handleSearch"
        @keyup.enter="handleEnter"
        autofocus
      >
        <template #append>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
        </template>
      </el-input>

      <!-- 搜索结果 -->
      <div class="search-results" v-if="searchResults.length > 0">
        <div class="result-section" v-for="(section, index) in searchResults" :key="index">
          <div class="section-title">{{ section.title }}</div>
          <div
            class="result-item"
            v-for="item in section.items"
            :key="item.path"
            @click="goToPath(item.path)"
          >
            <el-icon class="item-icon">
              <component :is="item.icon" />
            </el-icon>
            <div class="item-info">
              <div class="item-title">{{ item.title }}</div>
              <div class="item-path">{{ item.path }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="search-empty" v-else-if="searchKeyword && searchResults.length === 0">
        <el-empty description="未找到相关结果" :image-size="100" />
      </div>

      <!-- 搜索提示 -->
      <div class="search-tips" v-else>
        <div class="tip-title">搜索提示</div>
        <div class="tip-content">
          <p>• 输入关键字搜索菜单、页面</p>
          <p>• 使用 ↑↓ 方向键选择</p>
          <p>• 按 Enter 键快速跳转</p>
          <p>• 按 ESC 键关闭</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from "vue";
import { useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { Search as SearchIcon } from "@element-plus/icons-vue";

const router = useRouter();
const { t } = useI18n();

const searchVisible = ref(false);
const searchKeyword = ref("");
const searchInputRef = ref();
const searchResults = ref<any[]>([]);

// 菜单配置（可从路由或后端获取）
const menuList = [
  {
    title: "首页",
    path: "/home",
    icon: "House"
  },
  {
    title: "系统管理",
    path: "/system",
    icon: "Management",
    children: [
      { title: "用户管理", path: "/system/user", icon: "User" },
      { title: "角色管理", path: "/system/role", icon: "UserFilled" },
      { title: "菜单管理", path: "/system/menu", icon: "Menu" }
    ]
  },
  {
    title: "日志管理",
    path: "/logs",
    icon: "Document",
    children: [
      { title: "操作日志", path: "/logs/operation", icon: "Document" },
      { title: "系统日志", path: "/logs/system", icon: "Notebook" },
      { title: "用户日志", path: "/logs/user", icon: "User" }
    ]
  },
  {
    title: "系统设置",
    path: "/settings",
    icon: "Setting"
  }
];

// 打开搜索
const openSearch = () => {
  searchVisible.value = true;
  nextTick(() => {
    searchInputRef.value?.focus();
  });
};

// 关闭搜索
const handleClose = () => {
  searchVisible.value = false;
  searchKeyword.value = "";
  searchResults.value = [];
};

// 执行搜索
const handleSearch = () => {
  if (!searchKeyword.value.trim()) {
    searchResults.value = [];
    return;
  }

  const keyword = searchKeyword.value.toLowerCase().trim();
  const results: any[] = [];

  // 搜索菜单
  menuList.forEach(menu => {
    // 搜索父菜单
    if (menu.title.toLowerCase().includes(keyword)) {
      results.push({
        title: menu.title,
        path: menu.path,
        icon: menu.icon
      });
    }

    // 搜索子菜单
    if (menu.children) {
      menu.children.forEach(child => {
        if (child.title.toLowerCase().includes(keyword)) {
          results.push({
            title: child.title,
            path: child.path,
            icon: child.icon
          });
        }
      });
    }
  });

  // 分组显示结果
  if (results.length > 0) {
    searchResults.value = [
      {
        title: "菜单",
        items: results
      }
    ];
  } else {
    searchResults.value = [];
  }
};

// 回车跳转第一个结果
const handleEnter = () => {
  if (searchResults.value.length > 0 && searchResults.value[0].items.length > 0) {
    goToPath(searchResults.value[0].items[0].path);
  }
};

// 跳转路径
const goToPath = (path: string) => {
  router.push(path);
  handleClose();
};

// 快捷键支持 Ctrl+K
const handleKeydown = (e: KeyboardEvent) => {
  // Ctrl+K 或 Cmd+K
  if ((e.ctrlKey || e.metaKey) && e.key === "k") {
    e.preventDefault();
    openSearch();
  }
  // ESC 关闭
  if (e.key === "Escape" && searchVisible.value) {
    e.preventDefault();
    handleClose();
  }
};

onMounted(() => {
  document.addEventListener("keydown", handleKeydown);
});

onUnmounted(() => {
  document.removeEventListener("keydown", handleKeydown);
});
</script>

<style scoped lang="scss">
.search-container {
  .search-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    cursor: pointer;
    border-radius: 4px;
    transition: all 0.3s;

    &:hover {
      background-color: #f0f2f5;
    }
  }
}

:deep(.search-dialog) {
  .el-dialog__header {
    margin-right: 0;
    padding: 20px 20px 10px;
  }

  .el-dialog__body {
    padding: 0 20px 20px;
  }

  .el-input__wrapper {
    padding: 8px 12px;
  }
}

.search-results {
  max-height: 500px;
  overflow-y: auto;

  .result-section {
    margin-bottom: 20px;

    &:last-child {
      margin-bottom: 0;
    }

    .section-title {
      font-size: 14px;
      font-weight: 600;
      color: #909399;
      margin-bottom: 12px;
      padding: 0 4px;
    }

    .result-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 10px 12px;
      cursor: pointer;
      border-radius: 6px;
      transition: all 0.2s;

      &:hover {
        background-color: #f0f2f5;
      }

      .item-icon {
        font-size: 18px;
        color: #909399;
      }

      .item-info {
        flex: 1;

        .item-title {
          font-size: 14px;
          color: #333;
          margin-bottom: 4px;
        }

        .item-path {
          font-size: 12px;
          color: #909399;
        }
      }
    }
  }
}

.search-empty,
.search-tips {
  padding: 40px 20px;

  .tip-title {
    font-size: 16px;
    font-weight: 600;
    color: #333;
    margin-bottom: 16px;
  }

  .tip-content {
    font-size: 14px;
    color: #666;
    line-height: 2;

    p {
      margin: 0;
    }
  }
}
</style>
