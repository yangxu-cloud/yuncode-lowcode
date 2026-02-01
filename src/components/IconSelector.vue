<template>
  <div class="icon-selector">
    <el-dialog
      v-model="dialogVisible"
      title="选择图标"
      width="800px"
      :close-on-click-modal="false"
      @close="handleClose"
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

      <!-- 标签页 -->
      <el-tabs v-model="activeTab" class="icon-tabs">
        <el-tab-pane label="Element Plus 图标" name="element">
          <div v-loading="!elementIconNames.length" element-loading-text="图标加载中...">
            <div class="icon-grid">
              <div
                v-for="icon in filteredElementIcons"
                :key="icon"
                class="icon-item"
                :class="{ 'is-selected': selectedIcon === icon }"
                @click="handleSelectIcon(icon)"
              >
                <el-icon :size="24">
                  <component :is="getIconComponent(icon)" />
                </el-icon>
                <div class="icon-name">{{ icon }}</div>
                <el-icon v-if="selectedIcon === icon" class="check-icon" color="#67c23a">
                  <CircleCheck />
                </el-icon>
              </div>
            </div>
            <el-empty
              v-if="filteredElementIcons.length === 0"
              description="未找到匹配的图标"
              :image-size="60"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="常用图标" name="common">
          <div class="icon-grid">
            <div
              v-for="icon in filteredCommonIcons"
              :key="icon.name"
              class="icon-item"
              :class="{ 'is-selected': selectedIcon === icon.name }"
              @click="handleSelectIcon(icon.name)"
            >
              <el-icon :size="24">
                <component :is="icon.component" />
              </el-icon>
              <div class="icon-name">{{ icon.label }}</div>
              <el-icon v-if="selectedIcon === icon.name" class="check-icon" color="#67c23a">
                <CircleCheck />
              </el-icon>
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
import * as ElementPlusIcons from "@element-plus/icons-vue";
import { CircleCheck } from "@element-plus/icons-vue";

/**
 * 图标选择器组件
 * 支持Element Plus图标库选择
 * 支持搜索过滤
 * 支持常用图标快捷选择
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

// 当前标签页
const activeTab = ref("element");

// Element Plus 所有图标名称
const elementIconNames = Object.keys(ElementPlusIcons).filter(
  name => name !== "default" && !name.startsWith("Icon")
);

/**
 * 获取图标组件
 */
const getIconComponent = (iconName: string) => {
  return (ElementPlusIcons as any)[iconName];
};

// 常用图标列表
const commonIcons = [
  { name: "HomeFilled", label: "首页", component: ElementPlusIcons.HomeFilled },
  { name: "Menu", label: "菜单", component: ElementPlusIcons.Menu },
  { name: "Setting", label: "设置", component: ElementPlusIcons.Setting },
  { name: "User", label: "用户", component: ElementPlusIcons.User },
  { name: "UserFilled", label: "用户(填充)", component: ElementPlusIcons.UserFilled },
  { name: "OfficeBuilding", label: "组织", component: ElementPlusIcons.OfficeBuilding },
  { name: "Folder", label: "文件夹", component: ElementPlusIcons.Folder },
  { name: "FolderOpened", label: "打开的文件夹", component: ElementPlusIcons.FolderOpened },
  { name: "Document", label: "文档", component: ElementPlusIcons.Document },
  { name: "DocumentCopy", label: "文档复制", component: ElementPlusIcons.DocumentCopy },
  { name: "Files", label: "文件", component: ElementPlusIcons.Files },
  { name: "Message", label: "消息", component: ElementPlusIcons.Message },
  { name: "ChatDotRound", label: "聊天", component: ElementPlusIcons.ChatDotRound },
  { name: "Phone", label: "电话", component: ElementPlusIcons.Phone },
  { name: "Location", label: "位置", component: ElementPlusIcons.Location },
  { name: "Clock", label: "时钟", component: ElementPlusIcons.Clock },
  { name: "Calendar", label: "日历", component: ElementPlusIcons.Calendar },
  { name: "Search", label: "搜索", component: ElementPlusIcons.Search },
  { name: "Refresh", label: "刷新", component: ElementPlusIcons.Refresh },
  { name: "Delete", label: "删除", component: ElementPlusIcons.Delete },
  { name: "Edit", label: "编辑", component: ElementPlusIcons.Edit },
  { name: "Plus", label: "添加", component: ElementPlusIcons.Plus },
  { name: "Check", label: "对勾", component: ElementPlusIcons.Check },
  { name: "Close", label: "关闭", component: ElementPlusIcons.Close },
  { name: "View", label: "查看", component: ElementPlusIcons.View },
  { name: "Hide", label: "隐藏", component: ElementPlusIcons.Hide },
  { name: "Lock", label: "锁定", component: ElementPlusIcons.Lock },
  { name: "Unlock", label: "解锁", component: ElementPlusIcons.Unlock },
  { name: "Share", label: "分享", component: ElementPlusIcons.Share },
  { name: "Download", label: "下载", component: ElementPlusIcons.Download },
  { name: "Upload", label: "上传", component: ElementPlusIcons.Upload },
  { name: "Link", label: "链接", component: ElementPlusIcons.Link },
  { name: "Star", label: "收藏", component: ElementPlusIcons.Star },
  { name: "Operation", label: "操作", component: ElementPlusIcons.Operation },
  { name: "Tools", label: "工具", component: ElementPlusIcons.Tools },
  { name: "Histogram", label: "图表", component: ElementPlusIcons.Histogram },
  { name: "PieChart", label: "饼图", component: ElementPlusIcons.PieChart },
  { name: "DataLine", label: "数据线", component: ElementPlusIcons.DataLine },
  { name: "Notification", label: "通知", component: ElementPlusIcons.Notification },
  { name: "Warning", label: "警告", component: ElementPlusIcons.Warning },
  { name: "InfoFilled", label: "信息", component: ElementPlusIcons.InfoFilled },
  { name: "SuccessFilled", label: "成功", component: ElementPlusIcons.SuccessFilled },
  { name: "CircleCheck", label: "成功圆", component: ElementPlusIcons.CircleCheck },
  { name: "CircleClose", label: "错误圆", component: ElementPlusIcons.CircleClose },
  { name: "QuestionFilled", label: "疑问", component: ElementPlusIcons.QuestionFilled },
  { name: "WarningFilled", label: "警告填充", component: ElementPlusIcons.WarningFilled },
  { name: "ArrowUp", label: "上箭头", component: ElementPlusIcons.ArrowUp },
  { name: "ArrowDown", label: "下箭头", component: ElementPlusIcons.ArrowDown },
  { name: "ArrowLeft", label: "左箭头", component: ElementPlusIcons.ArrowLeft },
  { name: "ArrowRight", label: "右箭头", component: ElementPlusIcons.ArrowRight },
  { name: "Back", label: "返回", component: ElementPlusIcons.Back },
  { name: "Switch", label: "切换", component: ElementPlusIcons.Switch },
  { name: "More", label: "更多", component: ElementPlusIcons.More },
  { name: "MoreFilled", label: "更多填充", component: ElementPlusIcons.MoreFilled },
  { name: "Grid", label: "网格", component: ElementPlusIcons.Grid },
  { name: "List", label: "列表", component: ElementPlusIcons.List },
  { name: "Sort", label: "排序", component: ElementPlusIcons.Sort },
  { name: "Filter", label: "过滤", component: ElementPlusIcons.Filter },
  { name: "Platform", label: "平台", component: ElementPlusIcons.Platform },
  { name: "Monitor", label: "显示器", component: ElementPlusIcons.Monitor },
  { name: "Mouse", label: "鼠标", component: ElementPlusIcons.Mouse },
  { name: "Cpu", label: "CPU", component: ElementPlusIcons.Cpu }
];

// 过滤后的Element Plus图标
const filteredElementIcons = computed(() => {
  if (!searchKeyword.value) {
    return elementIconNames;
  }
  const keyword = searchKeyword.value.toLowerCase();
  return elementIconNames.filter(name =>
    name.toLowerCase().includes(keyword)
  );
});

// 过滤后的常用图标
const filteredCommonIcons = computed(() => {
  if (!searchKeyword.value) {
    return commonIcons;
  }
  const keyword = searchKeyword.value.toLowerCase();
  return commonIcons.filter(icon =>
    icon.name.toLowerCase().includes(keyword) ||
    icon.label.toLowerCase().includes(keyword)
  );
});

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
.icon-selector {
  .search-area {
    margin-bottom: 16px;
  }

  :deep(.el-dialog__body) {
    max-height: calc(100vh - 200px);
    overflow-y: auto;
    display: flex;
    flex-direction: column;
  }

  .icon-tabs {
    display: flex;
    flex-direction: column;
    flex: 1;
    overflow: hidden;

    :deep(.el-tabs__content) {
      flex: 1;
      overflow-y: auto;
      max-height: calc(100vh - 320px);
    }
  }

  .icon-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
    gap: 12px;
    padding: 12px 0;
  }

  .icon-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 16px 8px;
    border: 1px solid #e4e7ed;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.2s;
    position: relative;
    background-color: #fff;

    &:hover {
      border-color: #409eff;
      background-color: #ecf5ff;
      transform: translateY(-2px);
      box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
    }

    &.is-selected {
      border-color: #67c23a;
      background-color: #f0f9ff;
    }

    .icon-name {
      font-size: 12px;
      color: #606266;
      text-align: center;
      word-break: break-all;
      line-height: 1.4;
      max-width: 100%;
      overflow: hidden;
      text-overflow: ellipsis;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
    }

    .check-icon {
      position: absolute;
      top: 4px;
      right: 4px;
      font-size: 16px;
    }
  }

  :deep(.el-empty) {
    padding: 40px 0;
  }
}
</style>
