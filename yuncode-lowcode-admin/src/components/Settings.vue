<template>
  <div class="settings-container">
    <el-tooltip :content="t('menus.settings')" placement="bottom" effect="light">
      <div class="settings-icon" @click="openSettings">
        <el-icon :size="18">
          <Setting />
        </el-icon>
      </div>
    </el-tooltip>

    <el-drawer
      v-model="settingsVisible"
      title="项目配置"
      direction="rtl"
      :size="300"
      :before-close="handleClose"
    >
      <el-scrollbar height="100%">
        <el-form :model="settings" label-position="left" class="settings-form">
          <!-- 主题色 -->
          <div class="settings-section">
            <div class="section-title">
              <el-icon><Sunny /></el-icon>
              <span>{{ t('settings.primaryColor') }}</span>
            </div>
            <div class="section-content">
              <div class="color-picker-wrapper">
                <el-color-picker
                  v-model="settings.primaryColor"
                  @change="handlePrimaryColorChange"
                  show-alpha
                  :predefine="predefineColors"
                />
              </div>
            </div>
          </div>

          <!-- 主题模式 -->
          <div class="settings-section">
            <div class="section-title">
              <el-icon><Moon /></el-icon>
              <span>{{ t('settings.themeMode') }}</span>
            </div>
            <div class="section-content">
              <el-segmented v-model="settings.themeMode" @change="handleThemeModeChange" :options="themeModeOptions" />
            </div>
          </div>

          <!-- 界面显示 -->
          <div class="settings-section">
            <div class="section-title">
              <el-icon><Monitor /></el-icon>
              <span>{{ t('settings.interfaceDisplay') }}</span>
            </div>
            <div class="section-content">
              <el-form-item :label="t('settings.greyMode')">
                <el-switch
                  v-model="settings.isGrey"
                  @change="handleGreyChange"
                />
              </el-form-item>
              <el-form-item :label="t('settings.weakMode')">
                <el-switch
                  v-model="settings.isWeak"
                  @change="handleWeakChange"
                />
              </el-form-item>
              <el-form-item :label="t('settings.hideTags')">
                <el-switch
                  v-model="settings.hideTags"
                  @change="handleHideTagsChange"
                />
              </el-form-item>
              <el-form-item :label="t('settings.hideFooter')">
                <el-switch
                  v-model="settings.hideFooter"
                />
              </el-form-item>
              <el-form-item :label="t('settings.hideLogo')">
                <el-switch
                  v-model="settings.hideLogo"
                  @change="handleHideLogoChange"
                />
              </el-form-item>
            </div>
          </div>

          <!-- 菜单布局 -->
          <div class="settings-section">
            <div class="section-title">
              <el-icon><Grid /></el-icon>
              <span>{{ t('settings.menuLayout') }}</span>
            </div>
            <div class="section-content">
              <el-form-item :label="t('settings.menuWidth')">
                <el-slider
                  v-model="settings.menuWidth"
                  :min="180"
                  :max="280"
                  :step="10"
                  @change="handleMenuWidthChange"
                  show-input
                />
              </el-form-item>
              <el-form-item :label="t('settings.menuCollapse')">
                <el-switch
                  v-model="settings.menuCollapse"
                  @change="handleMenuCollapseChange"
                />
              </el-form-item>
            </div>
          </div>

          <!-- 页签风格 -->
          <div class="settings-section">
            <div class="section-title">
              <el-icon><Tickets /></el-icon>
              <span>{{ t('settings.tagsStyle') }}</span>
            </div>
            <div class="section-content">
              <el-form-item :label="t('settings.tagsStyle')">
                <el-radio-group v-model="settings.tagsStyle" @change="handleTagsStyleChange">
                  <el-radio value="card">卡片</el-radio>
                  <el-radio value="smart">灵动</el-radio>
                  <el-radio value="smooth">平滑</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item :label="t('settings.tagsPersist')">
                <el-switch
                  v-model="settings.tagsPersist"
                  @change="handleTagsPersistChange"
                />
              </el-form-item>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="settings-actions">
            <el-button type="primary" @click="saveSettings">{{ t('settings.save') }}</el-button>
            <el-button @click="resetSettings">{{ t('settings.reset') }}</el-button>
          </div>
        </el-form>
      </el-scrollbar>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage } from "element-plus";
import { Setting, Sunny, Monitor, Grid, Tickets, Moon } from "@element-plus/icons-vue";

const { t } = useI18n();

const emit = defineEmits<{
  menuWidthChange: [width: number];
  menuCollapseChange: [collapse: boolean];
  hideLogoChange: [hide: boolean];
  hideTagsChange: [hide: boolean];
  tagsStyleChange: [style: string];
}>();

const settingsVisible = ref(false);

const defaultSettings = {
  primaryColor: "#409eff",
  themeMode: "light",
  isGrey: false,
  isWeak: false,
  hideTags: false,
  hideFooter: false,
  hideLogo: false,
  menuWidth: 200,
  menuCollapse: false,
  tagsStyle: "card",
  tagsPersist: true
};

const settings = ref({ ...defaultSettings });

// 预设颜色
const predefineColors = ref([
  '#409eff',
  '#3d8afe',
  '#2d7af9',
  '#0056d6',
  '#1890ff',
  '#096dd9',
  '#0050b3',
  '#003a8c',
  '#f5222d',
  '#fa541c',
  '#fa8c16',
  '#faad14',
  '#fadb14',
  '#a0d911',
  '#52c41a',
  '#13c2c2',
  '#1890ff',
  '#2f54eb',
  '#722ed1',
  '#eb2f96'
]);

// 主题模式选项
const themeModeOptions = computed(() => [
  {
    label: '浅色',
    value: 'light',
    icon: Sunny
  },
  {
    label: '深色',
    value: 'dark',
    icon: Moon
  },
  {
    label: '自动',
    value: 'auto',
    icon: Monitor
  }
]);

// 从 localStorage 加载配置
onMounted(() => {
  loadSettings();
});

function loadSettings() {
  const savedSettings = localStorage.getItem("layoutSettings");
  if (savedSettings) {
    try {
      settings.value = { ...defaultSettings, ...JSON.parse(savedSettings) };
      applySettings();
    } catch (e) {
      console.error("Failed to parse settings:", e);
    }
  }
}

function openSettings() {
  settingsVisible.value = true;
}

function handleClose() {
  settingsVisible.value = false;
}

// 主题色切换
function handlePrimaryColorChange(color: string) {
  document.documentElement.style.setProperty("--el-color-primary", color);
}

// 主题模式切换
function handleThemeModeChange(mode: string) {
  const isDark = mode === 'dark' || (mode === 'auto' && window.matchMedia('(prefers-color-scheme: dark)').matches);

  if (isDark) {
    document.documentElement.classList.add("dark");
    document.documentElement.setAttribute("data-theme", "dark");
  } else {
    document.documentElement.classList.remove("dark");
    document.documentElement.setAttribute("data-theme", "light");
  }

  // 如果是自动模式，监听系统主题变化
  if (mode === 'auto') {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    const handleSystemThemeChange = (e: MediaQueryListEvent) => {
      if (e.matches) {
        document.documentElement.classList.add("dark");
        document.documentElement.setAttribute("data-theme", "dark");
      } else {
        document.documentElement.classList.remove("dark");
        document.documentElement.setAttribute("data-theme", "light");
      }
    };

    mediaQuery.addEventListener('change', handleSystemThemeChange);

    // 清理监听器
    return () => {
      mediaQuery.removeEventListener('change', handleSystemThemeChange);
    };
  }
}

// 灰色模式
function handleGreyChange(value: boolean) {
  if (value) {
    document.documentElement.classList.add("html-grey");
  } else {
    document.documentElement.classList.remove("html-grey");
  }
}

// 色弱模式
function handleWeakChange(value: boolean) {
  if (value) {
    document.documentElement.classList.add("html-weakness");
  } else {
    document.documentElement.classList.remove("html-weakness");
  }
}

// 隐藏标签页
function handleHideTagsChange(value: boolean) {
  emit("hideTagsChange", value);
  if (value) {
    document.documentElement.classList.add("hide-tags");
  } else {
    document.documentElement.classList.remove("hide-tags");
  }
}

// 隐藏 Logo
function handleHideLogoChange(value: boolean) {
  emit("hideLogoChange", value);
  if (value) {
    document.documentElement.classList.add("hide-logo");
  } else {
    document.documentElement.classList.remove("hide-logo");
  }
}

// 菜单宽度
function handleMenuWidthChange(value: number) {
  emit("menuWidthChange", value);
}

// 菜单折叠
function handleMenuCollapseChange(value: boolean) {
  emit("menuCollapseChange", value);
}

// 标签风格
function handleTagsStyleChange(style: string) {
  emit("tagsStyleChange", style);
  document.documentElement.setAttribute("data-tags-style", style);
}

// 标签持久化
function handleTagsPersistChange(value: boolean) {
  if (!value) {
    localStorage.removeItem("tagsView");
  }
}

// 保存配置
function saveSettings() {
  localStorage.setItem("layoutSettings", JSON.stringify(settings.value));
  ElMessage.success(t('settings.saveSuccess'));
}

// 重置配置
function resetSettings() {
  settings.value = { ...defaultSettings };
  applySettings();
  localStorage.removeItem("layoutSettings");
  ElMessage.success(t('settings.resetSuccess'));
}

// 应用配置
function applySettings() {
  handlePrimaryColorChange(settings.value.primaryColor);
  handleThemeModeChange(settings.value.themeMode);
  handleGreyChange(settings.value.isGrey);
  handleWeakChange(settings.value.isWeak);
  handleHideTagsChange(settings.value.hideTags);
  handleHideLogoChange(settings.value.hideLogo);
  handleTagsStyleChange(settings.value.tagsStyle);
}

// 监听设置变化，实时应用
watch(settings, (newSettings) => {
  // 实时保存到 localStorage
  localStorage.setItem("layoutSettings", JSON.stringify(newSettings));
}, { deep: true });
</script>

<style scoped lang="scss">
.settings-container {
  .settings-icon {
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

.settings-form {
  padding: 16px;

  .settings-section {
    margin-bottom: 24px;

    .section-title {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 16px;
      font-size: 14px;
      font-weight: 600;
      color: #333;
    }

    .section-content {
      padding-left: 28px;

      :deep(.el-form-item) {
        margin-bottom: 16px;

        &:last-child {
          margin-bottom: 0;
        }
      }

      .el-radio-group {
        display: flex;
        flex-direction: column;
        gap: 8px;

        .el-radio {
          margin-right: 0;
        }
      }

      .color-picker-wrapper {
        display: flex;
        align-items: center;
        gap: 12px;

        :deep(.el-color-picker) {
          .el-color-picker__trigger {
            width: 100px;
            height: 36px;
          }
        }
      }

      :deep(.el-segmented) {
        width: 100%;

        .el-segmented__item {
          padding: 8px 16px;
        }
      }
    }
  }

  .settings-actions {
    margin-top: 32px;
    padding-top: 16px;
    border-top: 1px solid #e6e6e6;
    display: flex;
    gap: 12px;

    .el-button {
      flex: 1;
    }
  }
}

// 灰色模式
:deep(.html-grey) {
  filter: grayscale(100%);
}

// 色弱模式
:deep(.html-weakness) {
  filter: invert(0.8);
}

// 隐藏标签页
:deep(.hide-tags) {
  .tags-view-container {
    display: none !important;
  }
}

// 隐藏 Logo
:deep(.hide-logo) {
  .el-aside .logo {
    display: none !important;
  }
}

// 标签风格 - 卡片
:deep([data-tags-style="card"]) {
  .tags-view-item {
    background-color: #f0f2f5;
    border: 1px solid #e6e6e6;
    border-radius: 3px;

    &.active {
      background-color: #ecf5ff;
      border-color: #409eff;
    }
  }
}

// 标签风格 - 灵动
:deep([data-tags-style="smart"]) {
  .tags-view-item {
    background-color: transparent;
    border: none;
    border-bottom: 2px solid transparent;
    border-radius: 0;

    &.active {
      background-color: transparent;
      border-bottom-color: #409eff;
    }
  }
}

// 标签风格 - 平滑
:deep([data-tags-style="smooth"]) {
  .tags-view-item {
    background-color: #fff;
    border: 1px solid transparent;
    border-radius: 16px;
    padding: 0 16px;

    &.active {
      background-color: #409eff;
      color: #fff;
      border-color: #409eff;
    }
  }
}
</style>
