<template>
  <el-dialog v-model="dialogVisible" title="" width="600px" :close-on-click-modal="false" destroy-on-close class="bo-create-dialog">
    <template #header>
      <div class="dialog-header">
        <div class="dialog-navbar">
          <div class="back-btn" @click="handleCancel">
            <span class="back-arrow">←</span>
            <span>返回</span>
          </div>
          <div class="dialog-title">新建业务对象</div>
        </div>
      </div>
    </template>
    <div class="dialog-body">
      <div class="form-card">
        <!-- 1. 标题名称 -->
        <div class="form-group">
          <div class="form-label">
            <div class="label-left">
              <span class="required-star">*</span>
              <span class="label-text">标题名称</span>
            </div>
            <span class="label-hint">如：客户，采购明细</span>
          </div>
          <el-input
            v-model="form.titleName"
            placeholder="请输入业务对象名称，例如：客户"
            maxlength="20"
            :class="{ 'is-error': errors.titleName }"
            @input="updateCharCount"
          />
          <div class="helper-text">
            <span>建议使用清晰易懂的业务术语</span>
            <span :class="['char-count', charCountClass]">{{ charCount }}/20</span>
          </div>
          <div v-if="errors.titleName" class="error-message show">标题名称不能为空</div>
        </div>

        <div class="divider" />

        <!-- 2. 存储名称 -->
        <div class="form-group">
          <div class="form-label">
            <div class="label-left">
              <span class="label-text">存储名称</span>
            </div>
            <span class="label-hint">系统自动生成前缀</span>
          </div>
          <div class="input-group">
            <span class="input-prefix">{{ tablePrefix }}</span>
            <el-input v-model="form.suffix" placeholder="自定义后缀，如：customer" @input="updatePreview" />
          </div>
          <div class="helper-text">
            <span>最终存储名称：<strong class="storage-preview">BO_EU_{{ form.suffix }}</strong></span>
            <span>建议英文+下划线</span>
          </div>
        </div>

        <div class="divider" />

        <!-- 3. 存储类型 -->
        <div class="form-group">
          <div class="form-label">
            <div class="label-left">
              <span class="label-text">存储类型</span>
            </div>
          </div>
          <el-select v-model="form.storageType" class="storage-select" @change="updatePrefix">
            <el-option label="表(Table)" value="Table" />
            <el-option label="视图(View)" value="View" />
            <el-option label="结构(Structure)" value="Structure" />
          </el-select>
          <div class="helper-text">
            表：持久化存储数据 | 视图：虚拟查询 | 结构：仅定义数据结构
          </div>
        </div>
      </div>
    </div>
    <template #footer>
      <div class="dialog-footer">
        <el-button round @click="handleCancel">取消</el-button>
        <el-button type="primary" round :loading="submitting" @click="handleSubmit">确定</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { createBoTable } from "@/api/bo-table";

const props = defineProps<{ modelValue: boolean; appId: string; categoryId: number }>();
const emit = defineEmits<{
  "update:modelValue": [val: boolean];
  success: [data: any];
}>();

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (val) => emit("update:modelValue", val),
});

const submitting = ref(false);

const form = reactive({
  titleName: "",
  suffix: "",
  storageType: "Table",
});

const errors = reactive({ titleName: false });
const tablePrefix = ref("BO_CU_");

const charCount = computed(() => form.titleName.length);
const charCountClass = computed(() => {
  if (charCount.value >= 18) return "danger";
  if (charCount.value >= 14) return "warning";
  return "";
});

function updateCharCount() {
  // reactive handles it via computed
}

function updatePrefix() {
  switch (form.storageType) {
    case "View": tablePrefix.value = "VO_CU_"; break;
    case "Structure": tablePrefix.value = "SO_CU_"; break;
    default: tablePrefix.value = "BO_CU_";
  }
}

function updatePreview() {
  // handled by template interpolation
}

function validateForm() {
  errors.titleName = !form.titleName.trim();
  return !errors.titleName;
}

function handleCancel() {
  if (form.titleName || form.suffix) {
    ElMessageBox.confirm("取消后内容将丢失，确定取消吗？", "提示", {
      confirmButtonText: "确定",
      cancelButtonText: "再想想",
      type: "warning",
    }).then(() => {
      dialogVisible.value = false;
    }).catch(() => {});
  } else {
    dialogVisible.value = false;
  }
}

async function handleSubmit() {
  if (!validateForm()) return;

  submitting.value = true;
  try {
    const result = await createBoTable(props.appId, {
      titleName: form.titleName.trim(),
      suffix: form.suffix.trim(),
      storageType: form.storageType,
      categoryId: props.categoryId,
    });
    ElMessage.success(`业务对象「${result.titleName}」创建成功`);
    emit("success", result);
    dialogVisible.value = false;
  } catch (e: any) {
    ElMessage.error(e?.message || "创建失败");
  } finally {
    submitting.value = false;
  }
}

// Reset form when dialog opens
watch(() => props.modelValue, (val) => {
  if (val) {
    form.titleName = "";
    form.suffix = "";
    form.storageType = "Table";
    errors.titleName = false;
    updatePrefix();
  }
});
</script>

<style scoped lang="scss">
.dialog-header {
  padding: 0;
}

.dialog-navbar {
  display: flex;
  align-items: center;
  height: 52px;
  padding: 0 24px;
  border-bottom: 1px solid #e2e8f0;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #475569;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  padding: 6px 10px;
  border-radius: 8px;
  transition: all 0.2s;

  &:hover {
    background: #f1f5f9;
    color: #3b82f6;
  }
}

.back-arrow {
  font-size: 18px;
}

.dialog-title {
  font-size: 16px;
  font-weight: 600;
  margin-left: 12px;
  color: #0f172a;
  border-left: 1px solid #e2e8f0;
  padding-left: 12px;
}

.dialog-body {
  padding: 0;
  background: #f1f5f9;
}

.form-card {
  background: #fff;
  margin: 20px;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  padding: 20px 24px;
}

.form-group {
  margin-bottom: 16px;

  &:last-of-type {
    margin-bottom: 0;
  }
}

.divider {
  height: 1px;
  background: #eef2f6;
  margin: 16px 0;
}

.form-label {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  flex-wrap: wrap;
  margin-bottom: 6px;
}

.label-left {
  display: flex;
  align-items: center;
  gap: 4px;
}

.required-star {
  color: #ef4444;
  font-size: 12px;
  font-weight: 500;
}

.label-text {
  font-weight: 500;
  font-size: 13px;
  color: #1e293b;
}

.label-hint {
  font-size: 11px;
  color: #64748b;
  background: #f8fafc;
  padding: 2px 8px;
  border-radius: 12px;
}

.helper-text {
  font-size: 11px;
  color: #64748b;
  margin-top: 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.char-count {
  color: #94a3b8;
  font-size: 10px;

  &.warning {
    color: #f59e0b;
  }

  &.danger {
    color: #ef4444;
  }
}

.storage-preview {
  font-family: monospace;
  font-weight: 500;
  color: #3b82f6;
}

.error-message {
  color: #ef4444;
  font-size: 11px;
  margin-top: 4px;
  display: none;

  &.show {
    display: block;
  }
}

.input-group {
  display: flex;
  align-items: stretch;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  transition: all 0.2s;
  background: #fff;

  &:focus-within {
    border-color: #3b82f6;
    box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
  }
}

.input-prefix {
  background: #f8fafc;
  padding: 0 12px;
  font-size: 12px;
  font-weight: 500;
  color: #1e293b;
  border-right: 1px solid #e2e8f0;
  border-radius: 9px 0 0 9px;
  height: 34px;
  line-height: 34px;
  white-space: nowrap;
  font-family: monospace;
}

.input-group :deep(.el-input__wrapper) {
  border: none;
  border-radius: 0 9px 9px 0;
  flex: 1;
  box-shadow: none !important;
  padding: 0 12px;
}

.input-group :deep(.el-input__inner) {
  height: 34px;
}

.storage-select {
  width: 100%;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 12px 24px 20px;
  border-top: 1px solid #eef2f6;
}
</style>

<style lang="scss">
.bo-create-dialog {
  .el-dialog__header {
    padding: 0 !important;
    margin: 0 !important;
  }
  .el-dialog__body {
    padding: 0 !important;
  }
  .el-dialog__footer {
    padding: 0 !important;
  }
  .el-dialog__headerbtn {
    display: none;
  }
}
</style>
