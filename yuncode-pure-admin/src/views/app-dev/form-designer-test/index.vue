<template>
  <div class="form-designer-test" style="height: 100vh;">
    <FcDesigner
      v-if="showDesigner"
      :height="700"
      :config="designerConfig"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from "vue";
import FcDesigner from "@form-create/designer";

const showDesigner = ref(false);

const designerConfig = ref({
  showAi: false,
  showLanguage: false,
  nameReadonly: false,
  autoActive: false,
});

onMounted(() => {
  showDesigner.value = true;
});
</script>

<style>
/* 设计区边框：默认不显示，hover 时才显示 */
._fd-drag-tool {
  outline: none !important;
  border: none !important;
}
._fd-drag-tool.active {
  outline: 2px solid #2E73FF !important;
  outline-offset: 1px;
}
._fd-drag-tool:hover {
  outline: 1px solid rgba(46, 115, 255, 0.4) !important;
  outline-offset: 1px;
}
._fd-drag-tool:hover > div > ._fd-drag-btn {
  display: flex !important;
}
._fd-drag-tool.active > div > ._fd-drag-btn {
  display: flex !important;
}
._fd-drag-btn {
  display: none;
}
</style>
