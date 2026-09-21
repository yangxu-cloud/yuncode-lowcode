<script setup lang="ts">
import type { Props } from "../types";
import { useResizeObserver } from "@pureadmin/utils";
import { useEpThemeStoreHook } from "@/store/modules/epTheme";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";
import { ref, computed, getCurrentInstance, onMounted } from "vue";
import EnterOutlined from "@/assets/svg/enter_outlined.svg?component";

interface Emits {
  (e: "update:value", val: string): void;
  (e: "enter"): void;
}

const resultRef = ref();
const innerHeight = ref();
const emit = defineEmits<Emits>();
const instance = getCurrentInstance()!;
const props = withDefaults(defineProps<Props>(), {});

const itemStyle = computed(() => {
  return item => {
    return {
      background:
        item?.path === active.value ? "rgba(22, 93, 255, 0.08)" : "",
      color: item.path === active.value ? "#165dff" : ""
    };
  };
});

const active = computed({
  get() {
    return props.value;
  },
  set(val: string) {
    emit("update:value", val);
  }
});

/** 鼠标移入 */
async function handleMouse(item) {
  active.value = item.path;
}

function handleTo() {
  emit("enter");
}

function resizeResult() {
  // el-scrollbar max-height="calc(90vh - 140px)"
  innerHeight.value = window.innerHeight - window.innerHeight / 10 - 140;
}

useResizeObserver(resultRef, resizeResult);

function handleScroll(index: number) {
  const curInstance = instance?.proxy?.$refs[`resultItemRef${index}`];
  if (!curInstance) return 0;
  const curRef = curInstance[0] as ElRef;
  const scrollTop = curRef.offsetTop + 128; // 128 两个result-item（56px+56px=112px）高度加上下margin（8px+8px=16px）
  return scrollTop > innerHeight.value ? scrollTop - innerHeight.value : 0;
}

onMounted(() => {
  resizeResult();
});

defineExpose({ handleScroll });
</script>

<template>
  <div ref="resultRef" class="result">
    <div
      v-for="(item, index) in options"
      :key="item.path"
      :ref="'resultItemRef' + index"
      class="result-item dark:bg-[#1d1d1d]"
      :style="itemStyle(item)"
      @click="handleTo"
      @mouseenter="handleMouse(item)"
    >
      <component :is="useRenderIcon(item.meta?.icon)" class="result-item-icon" />
      <span class="result-item-title">
        {{ item.meta?.title }}
      </span>
      <EnterOutlined class="result-item-enter" />
    </div>
  </div>
</template>

<style lang="scss" scoped>
.result {
  padding: 4px 0;

  &-item {
    display: flex;
    align-items: center;
    height: 42px;
    padding: 0 10px;
    margin: 2px 0;
    cursor: pointer;
    border-radius: 8px;
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

    &:hover {
      background: #f7f8fa;
    }

    &-icon {
      flex-shrink: 0;
    }

    &-title {
      display: flex;
      flex: 1;
      margin-left: 10px;
      font-size: 14px;
      color: #1d2129;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    &-enter {
      flex-shrink: 0;
      opacity: 0;
      transition: opacity 0.15s ease;
    }

    &:hover &-enter {
      opacity: 0.5;
    }
  }
}
</style>
