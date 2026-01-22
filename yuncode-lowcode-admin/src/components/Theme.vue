<template>
  <div class="theme-container">
    <el-tooltip :content="t('menus.theme')" placement="bottom" effect="light">
      <div class="theme-icon" @click="toggleTheme">
        <el-icon :size="18">
          <Sunny v-if="isDark" />
          <Moon v-else />
        </el-icon>
      </div>
    </el-tooltip>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage } from "element-plus";

const { t } = useI18n();
const isDark = ref(false);

const toggleTheme = () => {
  isDark.value = !isDark.value;

  if (isDark.value) {
    document.documentElement.classList.add("dark");
    document.documentElement.setAttribute("data-theme", "dark");
    ElMessage.success("已切换到暗色模式");
  } else {
    document.documentElement.classList.remove("dark");
    document.documentElement.setAttribute("data-theme", "light");
    ElMessage.success("已切换到亮色模式");
  }

  // 保存主题设置
  localStorage.setItem("theme", isDark.value ? "dark" : "light");
};

onMounted(() => {
  // 初始化主题
  const savedTheme = localStorage.getItem("theme");
  if (savedTheme === "dark") {
    isDark.value = true;
    document.documentElement.classList.add("dark");
    document.documentElement.setAttribute("data-theme", "dark");
  }
});
</script>

<style scoped lang="scss">
.theme-container {
  .theme-icon {
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
</style>
