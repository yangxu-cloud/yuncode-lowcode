<script setup lang="ts">
import { useNav } from "@/layout/hooks/useNav";
import MdiKeyboardEsc from "@/assets/svg/keyboard_esc.svg?component";
import EnterOutlined from "@/assets/svg/enter_outlined.svg?component";
import ArrowUpLine from "~icons/ri/arrow-up-line";
import ArrowDownLine from "~icons/ri/arrow-down-line";

withDefaults(defineProps<{ total?: number }>(), {
  total: 0
});

const { device } = useNav();
</script>

<template>
  <div class="search-footer text-[#333] dark:text-white">
    <span class="search-footer-item">
      <span class="kbd-icon">
        <EnterOutlined class="kbd-svg" />
      </span>
      确认
    </span>
    <span class="search-footer-item">
      <span class="kbd-icon">
        <IconifyIconOffline :icon="ArrowUpLine" class="kbd-svg" />
      </span>
      <span class="kbd-icon">
        <IconifyIconOffline :icon="ArrowDownLine" class="kbd-svg" />
      </span>
      切换
    </span>
    <span class="search-footer-item">
      <span class="kbd-icon kbd-esc">ESC</span>
      关闭
    </span>
    <p v-if="device !== 'mobile' && total > 0" class="search-footer-total">
      {{ `共 ${total} 项` }}
    </p>
  </div>
</template>

<style lang="scss" scoped>
.search-footer {
  display: flex;
  align-items: center;

  .search-footer-item {
    display: flex;
    align-items: center;
    gap: 4px;
    margin-right: 12px;
    font-size: 12px;
    color: #86909c;
  }

  .kbd-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 20px;
    height: 18px;
    padding: 0 4px;
    background: #f2f3f5;
    border: 1px solid #e5e6eb;
    border-radius: 4px;
    font-size: 10px;
    color: #4e5969;
    line-height: 1;
    box-sizing: border-box;

    .kbd-svg {
      width: 12px;
      height: 12px;
    }
  }

  .kbd-esc {
    font-size: 10px;
    font-weight: 500;
    letter-spacing: 0.5px;
  }

  .search-footer-total {
    position: absolute;
    right: 20px;
    font-size: 12px;
    color: #c9cdd4;
  }
}
</style>
