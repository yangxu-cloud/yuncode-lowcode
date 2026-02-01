<template>
  <div class="component-selector">
    <el-dialog
      v-model="dialogVisible"
      title="选择组件"
      width="800px"
      :close-on-click-modal="false"
      @close="handleClose"
    >
      <!-- 搜索框 -->
      <div class="search-area">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索组件路径..."
          prefix-icon="Search"
          clearable
          size="default"
        />
      </div>

      <!-- 标签页 -->
      <el-tabs v-model="activeTab" class="component-tabs">
        <el-tab-pane
          v-for="category in categories"
          :key="category.key"
          :label="category.label"
          :name="category.key"
        >
          <div class="component-list">
            <div
              v-for="component in filteredComponentsByCategory(category.key)"
              :key="component.path"
              class="component-item"
              :class="{ 'is-selected': selectedComponent === component.path }"
              @click="handleSelectComponent(component.path, component.label)"
            >
              <el-icon :size="20" color="#409eff">
                <component :is="component.icon" />
              </el-icon>
              <div class="component-info">
                <div class="component-label">{{ component.label }}</div>
                <div class="component-path">{{ component.path }}</div>
                <div v-if="component.description" class="component-description">
                  {{ component.description }}
                </div>
              </div>
              <el-icon v-if="selectedComponent === component.path" class="check-icon" color="#67c23a">
                <CircleCheck />
              </el-icon>
            </div>
          </div>
          <el-empty
            v-if="filteredComponentsByCategory(category.key).length === 0"
            description="未找到匹配的组件"
            :image-size="60"
          />
        </el-tab-pane>

        <el-tab-pane label="自定义" name="custom">
          <div class="custom-input-area">
            <el-alert
              title="自定义组件路径"
              type="info"
              :closable="false"
              show-icon
              style="margin-bottom: 16px"
            >
              请输入完整的组件路径，例如: /views/system/user/index.vue
            </el-alert>
            <el-input
              v-model="customComponentPath"
              placeholder="/views/your-module/your-page/index.vue"
              clearable
              size="default"
            >
              <template #prepend>路径:</template>
            </el-input>
            <div class="path-examples">
              <div class="example-title">常用示例:</div>
              <div
                v-for="example in commonExamples"
                :key="example.path"
                class="example-item"
                @click="customComponentPath = example.path"
              >
                {{ example.label }}
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button @click="handleClear">清空</el-button>
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleConfirm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { CircleCheck } from "@element-plus/icons-vue";
import {
  componentList,
  categoryLabels,
  type ComponentItem
} from "@/config/components";

/**
 * 组件选择器组件
 * 从配置文件动态加载真实组件列表
 * 支持自定义组件路径输入
 */

interface Props {
  modelValue?: string;
}

interface Emits {
  (e: "update:modelValue", value: string | undefined): void;
  (e: "change", value: string | undefined): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

// 对话框显示状态
const dialogVisible = ref(false);

// 选中的组件路径
const selectedComponent = ref<string>(props.modelValue || "");

// 选中的组件标签
const selectedLabel = ref<string>("");

// 搜索关键词
const searchKeyword = ref("");

// 自定义组件路径
const customComponentPath = ref("");

// 当前标签页
const activeTab = ref("system");

// 所有分类
const categories = computed(() => {
  return Object.entries(categoryLabels).map(([key, label]) => ({ key, label }));
});

// 常用路径示例
const commonExamples = [
  { path: "/views/facilities/navigation/index.vue", label: "导航管理" },
  { path: "/views/facilities/org/index.vue", label: "组织管理" },
  { path: "/views/permission/page/index.vue", label: "页面权限" },
  { path: "/views/settings/index.vue", label: "系统设置" }
];

/**
 * 根据分类过滤组件
 */
const filteredComponentsByCategory = (category: string) => {
  const components = componentList.filter(comp => comp.category === category);

  if (!searchKeyword.value) {
    return components;
  }

  const keyword = searchKeyword.value.toLowerCase();
  return components.filter(
    comp =>
      comp.label.toLowerCase().includes(keyword) ||
      comp.path.toLowerCase().includes(keyword) ||
      comp.description?.toLowerCase().includes(keyword)
  );
};

/**
 * 打开对话框
 */
const open = () => {
  dialogVisible.value = true;
  selectedComponent.value = props.modelValue || "";
  customComponentPath.value = props.modelValue || "";
  searchKeyword.value = "";
};

/**
 * 选择组件
 */
const handleSelectComponent = (path: string, label: string) => {
  selectedComponent.value = path;
  selectedLabel.value = label;
};

/**
 * 清空选择
 */
const handleClear = () => {
  selectedComponent.value = "";
  selectedLabel.value = "";
  customComponentPath.value = "";
};

/**
 * 确定
 */
const handleConfirm = () => {
  let finalPath = selectedComponent.value;

  // 如果是自定义标签页，使用自定义输入的路径
  if (activeTab.value === "custom" && customComponentPath.value) {
    finalPath = customComponentPath.value;
  }

  emit("update:modelValue", finalPath || undefined);
  emit("change", finalPath || undefined);
  handleClose();
};

/**
 * 关闭对话框
 */
const handleClose = () => {
  dialogVisible.value = false;
  searchKeyword.value = "";
  customComponentPath.value = "";
};

// 暴露方法
defineExpose({
  open
});
</script>

<style scoped lang="scss">
.component-selector {
  .search-area {
    margin-bottom: 16px;
  }

  .component-tabs {
    :deep(.el-tabs__content) {
      height: 400px;
      overflow-y: auto;
    }
  }

  .component-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding: 12px 0;
  }

  .component-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 16px;
    border: 1px solid #e4e7ed;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.2s;
    position: relative;
    background-color: #fff;

    &:hover {
      border-color: #409eff;
      background-color: #ecf5ff;
    }

    &.is-selected {
      border-color: #67c23a;
      background-color: #f0f9ff;
    }

    .component-info {
      flex: 1;
      min-width: 0;

      .component- {
        &label {
          font-size: 14px;
          font-weight: 500;
          color: #303133;
          margin-bottom: 2px;
        }

        &path {
          font-size: 12px;
          color: #909399;
          word-break: break-all;
        }

        &description {
          font-size: 11px;
          color: #a8abb2;
          margin-top: 2px;
        }
      }
    }

    .check-icon {
      flex-shrink: 0;
      font-size: 18px;
    }
  }

  .custom-input-area {
    padding: 20px 0;

    .path-examples {
      margin-top: 20px;
      padding: 16px;
      background-color: #f5f7fa;
      border-radius: 4px;

      .example-title {
        font-size: 13px;
        font-weight: 500;
        color: #606266;
        margin-bottom: 12px;
      }

      .example-item {
        font-size: 12px;
        color: #409eff;
        cursor: pointer;
        padding: 4px 0;
        transition: all 0.2s;

        &:hover {
          color: #66b1ff;
          text-decoration: underline;
        }
      }
    }
  }

  :deep(.el-empty) {
    padding: 40px 0;
  }
}
</style>
