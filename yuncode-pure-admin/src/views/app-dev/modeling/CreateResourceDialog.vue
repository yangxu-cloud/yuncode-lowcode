<template>
  <el-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)" title="" width="75vw" :show-close="false" class="create-dialog" destroy-on-close>
    <template #header>
      <div class="create-dialog__header">
        <div class="create-dialog__icon">📘</div>
        <div>
          <h3 class="create-dialog__title">操作指导</h3>
          <p class="create-dialog__sub">选择您需要创建的资源类型，快速开启业务搭建</p>
        </div>
      </div>
    </template>
    <div class="create-dialog__cards">
      <div
        v-for="item in resourceTypes"
        :key="item.key"
        class="create-card"
        @click="handleClick(item.key)"
      >
        <div class="create-card__icon">
          <span class="create-card__emoji">{{ item.emoji }}</span>
        </div>
        <div class="create-card__title">{{ item.title }}</div>
        <div class="create-card__desc">{{ item.desc }}</div>
        <button class="create-card__btn">立即新建 →</button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
defineProps<{ modelValue: boolean }>();
const emit = defineEmits<{ "update:modelValue": [val: boolean]; select: [key: string] }>();

const resourceTypes = [
  { key: "database", title: "新建业务数据库", desc: "创建数据表，定义字段类型与索引，支撑业务持久化存储。", emoji: "🗄️" },
  { key: "form", title: "新建业务表单", desc: "设计可视化表单，配置校验规则与布局，快速收集数据。", emoji: "📋" },
  { key: "workflow", title: "新建业务流程", desc: "定义审批节点，设置流转条件，实现自动化业务流转。", emoji: "🔄" },
  { key: "ledger", title: "新建台账管理", desc: "建立业务台账，跟踪数据变更，汇总关键指标。", emoji: "📊" },
  { key: "dict", title: "新建关联字典", desc: "维护代码映射，统一数据标准，提升系统一致性。", emoji: "📖" },
];

function handleClick(key: string) {
  emit("select", key);
  emit("update:modelValue", false);
}
</script>

<style scoped lang="scss">
.create-dialog__header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 32px 36px 24px;
  border-bottom: 1px solid #edf2f7;
}

.create-dialog__icon {
  font-size: 32px;
  background: linear-gradient(145deg, #3b82f6, #6366f1);
  border-radius: 20px;
  padding: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 6px 12px -6px rgba(59, 130, 246, 0.3);
  line-height: 1;
}

.create-dialog__title {
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, #1e293b 0%, #2d3a4f 100%);
  background-clip: text;
  -webkit-background-clip: text;
  color: transparent;
  margin: 0 0 4px;
  letter-spacing: -0.3px;
}

.create-dialog__sub {
  font-size: 15px;
  color: #5b6e8c;
  margin: 0;
  letter-spacing: 0.2px;
}

.create-dialog__cards {
  display: flex;
  gap: 24px;
  padding: 32px 36px;
  justify-content: center;
}

.create-card {
  flex: 1 1 0;
  min-width: 0;
  background: #fff;
  border-radius: 28px;
  border: 1px solid #e9edf2;
  padding: 28px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.2, 0, 0, 1);
  box-shadow: 0 6px 12px -8px rgba(0, 0, 0, 0.05);

  &:hover {
    transform: translateY(-6px);
    border-color: #3b82f6;
    box-shadow: 0 24px 36px -14px rgba(59, 130, 246, 0.25);
  }

  &__icon {
    width: 64px;
    height: 64px;
    background: #eff6ff;
    border-radius: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 18px;
    transition: all 0.2s;
  }

  &__emoji {
    font-size: 32px;
    line-height: 1;
    transition: filter 0.2s;
  }

  &:hover &__icon {
    background: #3b82f6;
    transform: scale(1.02);
  }

  &:hover &__emoji {
    filter: brightness(0) invert(1);
  }

  &__title {
    font-size: 18px;
    font-weight: 600;
    color: #0f172a;
    margin-bottom: 12px;
    letter-spacing: -0.2px;
  }

  &__desc {
    font-size: 13px;
    line-height: 1.5;
    color: #5c6f87;
    margin-bottom: 24px;
    min-height: 42px;
  }

  &__btn {
    display: block;
    width: 100%;
    padding: 10px 0;
    background: transparent;
    border: 1.5px solid #cbd5e1;
    border-radius: 40px;
    font-size: 14px;
    font-weight: 500;
    color: #1f3a6b;
    cursor: pointer;
    transition: all 0.25s;
    text-align: center;

    &:hover {
      background: #3b82f6;
      border-color: #3b82f6;
      color: white;
      box-shadow: 0 4px 10px -4px #3b82f6;
    }
  }
}
</style>

<!-- Dialog teleport 到 body，需要非 scoped 样式覆盖 -->
<style lang="scss">
.el-dialog.create-dialog.el-dialog {
  border-radius: 32px !important;
  overflow: hidden;
}
.el-dialog.create-dialog .el-dialog__header {
  padding: 0;
  margin: 0;
}
.el-dialog.create-dialog .el-dialog__body {
  padding: 0 32px 32px;
}
.el-dialog.create-dialog .el-dialog__headerbtn {
  display: none;
}
</style>
