<template>
  <div class="app-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>业务建模</span>
        </div>
      </template>
      <el-empty description="业务建模功能开发中..." />
    </el-card>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: "Modeling"
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
