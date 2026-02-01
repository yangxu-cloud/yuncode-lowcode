<template>
  <div class="dept-select">
    <el-input
      :model-value="displayValue"
      :placeholder="placeholder"
      readonly
      :clearable="clearable"
      @click="handleOpen"
      @clear="handleClear"
    >
      <template #suffix>
        <el-icon style="cursor: pointer" @click="handleOpen">
          <ArrowDown />
        </el-icon>
      </template>
    </el-input>

    <!-- 部门选择对话框 -->
    <DeptSelector
      ref="selectorRef"
      v-model="innerValue"
      :multiple="multiple"
      :tenant-id="tenantId"
      :exclude-ids="excludeIds"
      @change="handleChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from "vue";
import { ArrowDown } from "@element-plus/icons-vue";
import DeptSelector from "./DeptSelector.vue";

/**
 * 部门选择输入框组件
 * 类似 el-select 的使用方式
 */

interface Props {
  modelValue?: number | number[];
  multiple?: boolean;
  tenantId?: number | null;
  placeholder?: string;
  clearable?: boolean;
  excludeIds?: number[];
}

interface Emits {
  (e: "update:modelValue", value: number | number[]): void;
  (e: "change", value: number | number[], items: any[]): void;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => 0,
  multiple: false,
  tenantId: null,
  placeholder: "请选择部门",
  clearable: true,
  excludeIds: () => []
});

const emit = defineEmits<Emits>();

const selectorRef = ref();
const innerValue = ref(props.modelValue);
const selectedItems = ref<any[]>([]);

// 显示值
const displayValue = computed(() => {
  if (selectedItems.value.length === 0) {
    return "";
  }
  if (props.multiple) {
    return selectedItems.value.map(item => item.label).join(", ");
  } else {
    return selectedItems.value[0]?.label || "";
  }
});

// 打开选择器
const handleOpen = () => {
  selectorRef.value?.open();
};

// 清空
const handleClear = () => {
  if (props.multiple) {
    innerValue.value = [];
  } else {
    innerValue.value = 0;
  }
  selectedItems.value = [];
  emit("update:modelValue", innerValue.value);
  emit("change", innerValue.value, []);
};

// 选择变化
const handleChange = (value: number | number[], items: any[]) => {
  innerValue.value = value;
  selectedItems.value = items;
  emit("update:modelValue", value);
  emit("change", value, items);
};

// 监听外部值变化
watch(
  () => props.modelValue,
  (val) => {
    innerValue.value = val;
  }
);

defineExpose({
  open: () => selectorRef.value?.open()
});
</script>

<style scoped lang="scss">
.dept-select {
  :deep(.el-input__inner) {
    cursor: pointer;
  }

  :deep(.el-input__suffix) {
    cursor: pointer;
  }
}
</style>
