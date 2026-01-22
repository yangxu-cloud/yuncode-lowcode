<template>
  <div class="hamburger-container" @click="toggleClick">
    <el-tooltip :content="tooltipContent" placement="bottom" effect="light">
      <div class="hamburger-icon">
        <el-icon :size="18">
          <component :is="isActive ? Expand : Fold" />
        </el-icon>
      </div>
    </el-tooltip>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { Expand, Fold } from "@element-plus/icons-vue";

const { t } = useI18n();

interface Props {
  isActive: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  isActive: false
});

const emit = defineEmits<{
  toggleClick: [];
}>();

const tooltipContent = computed(() => {
  return props.isActive ? t('menus.expandSidebar') : t('menus.collapseSidebar');
});

const toggleClick = () => {
  emit("toggleClick");
};
</script>

<style scoped lang="scss">
.hamburger-container {
  .hamburger-icon {
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
