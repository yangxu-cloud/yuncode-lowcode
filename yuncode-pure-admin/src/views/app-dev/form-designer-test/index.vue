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
import { ref, onMounted } from "vue";
import FcDesigner from "@form-create/designer";

const showDesigner = ref(false);
const designerConfig = ref({
  showAi: false,
  showLanguage: false,
  nameReadonly: false,
});

onMounted(() => {
  showDesigner.value = true;
});
</script>
