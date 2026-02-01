<template>
  <div class="icon-select">
    <el-dialog
      v-model="dialogVisible"
      title="选择图标"
      width="700px"
      :close-on-click-modal="false"
      @close="handleClose"
      :style="{ height: dialogHeight }"
    >
      <!-- 搜索框 -->
      <div class="search-area">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索图标名称..."
          prefix-icon="Search"
          clearable
          size="default"
        />
      </div>

      <!-- 图标网格 -->
      <div class="icon-container">
        <div
          v-for="icon in filteredIcons"
          :key="icon"
          class="icon-item"
          :class="{ 'is-selected': selectedIcon === icon }"
          @click="handleSelectIcon(icon)"
        >
          <el-icon :size="16">
            <component :is="getIconComponent(icon)" />
          </el-icon>
          <div class="icon-tooltip">{{ icon }}</div>
          <div v-if="selectedIcon === icon" class="check-mark">
            <el-icon :size="8">
              <Check />
            </el-icon>
          </div>
        </div>
      </div>

      <el-empty
        v-if="filteredIcons.length === 0"
        description="未找到匹配的图标"
        :image-size="60"
      />

      <template #footer>
        <span class="footer-info">{{ filteredIcons.length }} 个图标</span>
        <el-button @click="handleClear">清空</el-button>
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleConfirm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from "vue";
import * as ElementPlusIcons from "@element-plus/icons-vue";
import { Check } from "@element-plus/icons-vue";

/**
 * 图标选择器组件
 * 参考 pureadmin 设计风格
 * Element Plus 图标库
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

// 选中的图标
const selectedIcon = ref<string>(props.modelValue || "");

// 搜索关键词
const searchKeyword = ref("");

// 屏幕高度的一半
const dialogHeight = ref("50vh");

// 计算屏幕高度
const updateDialogHeight = () => {
  dialogHeight.value = `${window.innerHeight / 2}px`;
};

onMounted(() => {
  updateDialogHeight();
  window.addEventListener("resize", updateDialogHeight);
});

onUnmounted(() => {
  window.removeEventListener("resize", updateDialogHeight);
});

// Element Plus 所有图标名称（过滤掉特殊图标）
const elementIconNames = Object.keys(ElementPlusIcons).filter(
  name => name !== "default" && !name.startsWith("Icon")
);

// 过滤后的图标
const filteredIcons = computed(() => {
  if (!searchKeyword.value) {
    return elementIconNames;
  }
  const keyword = searchKeyword.value.toLowerCase();
  return elementIconNames.filter(name =>
    name.toLowerCase().includes(keyword)
  );
});

/**
 * 获取图标组件
 */
const getIconComponent = (iconName: string) => {
  return (ElementPlusIcons as any)[iconName];
};

/**
 * 打开对话框
 */
const open = () => {
  dialogVisible.value = true;
  selectedIcon.value = props.modelValue || "";
  searchKeyword.value = "";
};

/**
 * 选择图标
 */
const handleSelectIcon = (iconName: string) => {
  selectedIcon.value = iconName;
};

/**
 * 清空选择
 */
const handleClear = () => {
  selectedIcon.value = "";
};

/**
 * 确定
 */
const handleConfirm = () => {
  emit("update:modelValue", selectedIcon.value || undefined);
  emit("change", selectedIcon.value || undefined);
  handleClose();
};

/**
 * 关闭对话框
 */
const handleClose = () => {
  dialogVisible.value = false;
  searchKeyword.value = "";
};

// 暴露方法
defineExpose({
  open
});
</script>

<style scoped lang="scss">
.icon-select {
  .search-area {
    margin-bottom: 12px;
  }

  :deep(.el-dialog__body) {
    padding: 12px;
    height: calc(100% - 100px);
    overflow: hidden;
  }

  .icon-container {
    height: 100%;
    overflow-y: auto;
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(40px, 1fr));
    gap: 6px;
    padding: 8px;
    background-color: #fff;
    border-radius: 4px;

    // 自定义滚动条样式
    &::-webkit-scrollbar {
      width: 6px;
    }

    &::-webkit-scrollbar-thumb {
      background-color: #ddd;
      border-radius: 3px;

      &:hover {
        background-color: #ccc;
      }
    }
  }

  .icon-item {
    position: relative;
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #fff;
    border: 1px solid #eee;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
      border-color: #1890ff;
      background-color: #f0f7ff;

      .icon-tooltip {
        opacity: 1;
        visibility: visible;
      }
    }

    &.is-selected {
      border-color: #1890ff;
      border-width: 2px;
      background-color: #f0f7ff;
      box-shadow: 0 2px 4px rgba(24, 144, 255, 0.2);
    }

    .icon-tooltip {
      position: absolute;
      bottom: -30px;
      left: 50%;
      transform: translateX(-50%);
      background-color: #333;
      color: #fff;
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 12px;
      white-space: nowrap;
      opacity: 0;
      visibility: hidden;
      transition: all 0.2s ease;
      pointer-events: none;
      z-index: 10;

      &::before {
        content: "";
        position: absolute;
        top: -4px;
        left: 50%;
        transform: translateX(-50%);
        width: 0;
        height: 0;
        border-left: 4px solid transparent;
        border-right: 4px solid transparent;
        border-bottom: 4px solid #333;
      }
    }

    .check-mark {
      position: absolute;
      top: 4px;
      right: 4px;
      width: 12px;
      height: 12px;
      background-color: #1890ff;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
    }
  }

  :deep(.el-dialog__footer) {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 8px;
    padding: 8px 12px;
  }

  .footer-info {
    margin-right: auto;
    font-size: 12px;
    color: #909399;
  }

  :deep(.el-empty) {
    padding: 40px 0;
  }
}
</style>
