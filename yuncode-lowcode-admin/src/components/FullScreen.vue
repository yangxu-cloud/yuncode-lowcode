<template>
  <div class="fullscreen-container">
    <el-tooltip :content="t('menus.fullscreen')" placement="bottom" effect="light">
      <div class="fullscreen-icon" @click="toggleFullscreen">
        <el-icon :size="18">
          <FullScreen v-if="!isFullscreen" />
          <Aim v-else />
        </el-icon>
      </div>
    </el-tooltip>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage } from "element-plus";

const { t } = useI18n();
const isFullscreen = ref(false);

const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen();
    isFullscreen.value = true;
    ElMessage.success("已进入全屏模式");
  } else {
    if (document.exitFullscreen) {
      document.exitFullscreen();
      isFullscreen.value = false;
      ElMessage.success("已退出全屏模式");
    }
  }
};

const handleFullscreenChange = () => {
  isFullscreen.value = !!document.fullscreenElement;
};

onMounted(() => {
  document.addEventListener("fullscreenchange", handleFullscreenChange);
});

onUnmounted(() => {
  document.removeEventListener("fullscreenchange", handleFullscreenChange);
});
</script>

<style scoped lang="scss">
.fullscreen-container {
  .fullscreen-icon {
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
