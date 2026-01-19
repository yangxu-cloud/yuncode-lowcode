<template>
  <el-dropdown @command="handleLanguageChange" trigger="click">
    <span class="language-switcher">
      <el-icon :size="18"><Connection /></el-icon>
      <span class="language-text">{{ currentLanguage }}</span>
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="zh-CN" :class="{ active: locale === 'zh-CN' }">
          🇨🇳 简体中文
        </el-dropdown-item>
        <el-dropdown-item command="en-US" :class="{ active: locale === 'en-US' }">
          🇺🇸 English
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage } from "element-plus";
import { Connection } from "@element-plus/icons-vue";

const { locale } = useI18n();

const currentLanguage = computed(() => {
  return locale.value === "zh-CN" ? "简体中文" : "English";
});

const handleLanguageChange = (lang: string) => {
  locale.value = lang;
  localStorage.setItem("locale", lang);
  ElMessage.success(lang === "zh-CN" ? "已切换到简体中文" : "Switched to English");

  // 刷新页面以应用语言更改
  setTimeout(() => {
    window.location.reload();
  }, 300);
};
</script>

<style scoped lang="scss">
.language-switcher {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.3s;

  &:hover {
    background-color: rgba(0, 0, 0, 0.04);
  }

  .language-text {
    font-size: 14px;
    color: #606266;
  }
}

:deep(.el-dropdown-menu__item) {
  &.active {
    color: #409eff;
    background-color: #ecf5ff;
  }

  &.active::after {
    content: "✓";
    position: absolute;
    right: 12px;
    color: #409eff;
  }
}
</style>
