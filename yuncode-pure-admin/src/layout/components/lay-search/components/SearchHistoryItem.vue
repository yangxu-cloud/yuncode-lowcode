<script setup lang="ts">
import type { optionsItem } from "../types";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";
import StarIcon from "~icons/ep/star";
import CloseIcon from "~icons/ep/close";

interface Props {
  item: optionsItem;
}

interface Emits {
  (e: "collectItem", val: optionsItem): void;
  (e: "deleteItem", val: optionsItem): void;
}

const emit = defineEmits<Emits>();
withDefaults(defineProps<Props>(), {});

function handleCollect(item) {
  emit("collectItem", item);
}

function handleDelete(item) {
  emit("deleteItem", item);
}
</script>

<template>
  <component :is="useRenderIcon(item.meta?.icon)" class="history-item-icon" />
  <span class="history-item-title">
    {{ item.meta?.title }}
  </span>
  <span
    v-show="item.type === 'history'"
    class="history-item-action"
    @mousedown.prevent
    @click.stop="handleCollect(item)"
  >
    <IconifyIconOffline
      :icon="StarIcon"
      class="action-icon"
    />
  </span>
  <span
    class="history-item-action"
    @mousedown.prevent
    @click.stop="handleDelete(item)"
  >
    <IconifyIconOffline
      :icon="CloseIcon"
      class="action-icon"
    />
  </span>
</template>

<style lang="scss" scoped>
.history-item-icon {
  flex-shrink: 0;
}

.history-item-title {
  display: flex;
  flex: 1;
  margin-left: 10px;
  font-size: 14px;
  color: #1d2129;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-item-action {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  margin-left: 4px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;

  .action-icon {
    font-size: 14px;
    color: #86909c;
    transition: color 0.2s ease;
  }

  &:hover {
    background: rgba(0, 0, 0, 0.06);

    .action-icon {
      color: #1d2129;
    }
  }
}
</style>
