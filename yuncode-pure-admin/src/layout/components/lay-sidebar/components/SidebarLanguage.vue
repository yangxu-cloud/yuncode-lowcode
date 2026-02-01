<script setup lang="ts">
import { computed, onMounted, watch } from "vue";
import { useI18n } from "vue-i18n";
import Translation from "~icons/ri/translate-2";

const { locale } = useI18n();

// 支持的语言列表
const languages = [
  { label: "简体中文", value: "zh-CN" },
  { label: "English", value: "en-US" }
];

// 当前语言（使用 locale 的值）
const currentLanguage = computed(() => locale.value);

// 初始化语言
onMounted(() => {
  const savedLocale = localStorage.getItem("locale");
  if (savedLocale && (savedLocale === "zh-CN" || savedLocale === "en-US")) {
    locale.value = savedLocale;
  } else {
    locale.value = "zh-CN";
  }
});

// 切换语言
const changeLanguage = (lang: string) => {
  locale.value = lang;
};

// 监听 locale 变化，同步到 localStorage
watch(locale, (newLocale) => {
  localStorage.setItem("locale", newLocale);
});
</script>

<template>
  <el-dropdown trigger="click" @command="changeLanguage">
    <span class="language-dropdown navbar-bg-hover select-none">
      <IconifyIconOffline :icon="Translation" />
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="lang in languages"
          :key="lang.value"
          :command="lang.value"
          :class="{ 'is-active': currentLanguage === lang.value }"
        >
          {{ lang.label }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<style lang="scss" scoped>
.language-dropdown {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 10px;
  cursor: pointer;
  font-size: 18px;
}

:deep(.el-dropdown-menu__item.is-active) {
  color: var(--el-color-primary);
  background-color: var(--el-fill-color-light);
}
</style>
