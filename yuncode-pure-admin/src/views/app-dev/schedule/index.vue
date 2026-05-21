<template>
  <div class="app-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>定时任务</span>
        </div>
      </template>
      <el-empty description="定时任务功能开发中..." />
    </el-card>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: "Schedule"
});
</script>

<style scoped lang="scss">
.app-container {
  padding: 20px;
}

.card-header {
  font-size: 16px;
  font-weight: 600;
}
</style>
