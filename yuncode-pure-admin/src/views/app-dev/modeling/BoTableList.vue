<template>
  <div class="bo-list">
    <div class="list-toolbar">
      <span class="list-count">共 {{ tables.length }} 个业务对象</span>
      <el-button size="small" type="primary" round :icon="Plus" @click="$emit('create')">新建业务对象</el-button>
    </div>

    <div v-if="tables.length === 0" class="list-empty">
      <el-empty description="暂无业务对象，请在分类下新建" />
    </div>

    <div v-else class="table-list">
      <div v-for="t in tables" :key="t.id" class="table-item">
        <div class="table-item__left">
          <div class="table-item__icon">
            <el-icon :size="20"><Coin /></el-icon>
          </div>
          <div class="table-item__info">
            <div class="table-item__title">
              <template v-if="editingId === t.id">
                <el-input
                  :ref="(el: any) => { if (el) currentInput = el }"
                  v-model="editValue"
                  size="small"
                  class="inline-edit-input"
                  @blur="handleSaveRename(t)"
                  @keyup.enter="handleSaveRename(t)"
                  @keyup.esc="handleCancelRename"
                />
              </template>
              <template v-else>
                <span class="title-text">{{ t.titleName }}</span>
                <el-icon :size="13" class="edit-icon" @click.stop="handleStartRename(t)"><EditPen /></el-icon>
              </template>
            </div>
            <span class="table-item__meta">
              <code>{{ t.storageName }}</code>
              <span class="meta-divider">|</span>
              <span>{{ t.storageType }}</span>
              <span class="meta-divider">|</span>
              <span>{{ t.createTime?.substring(0, 10) || '-' }}</span>
              <span class="meta-divider">|</span>
              <span>v{{ t.designVersion ?? 1 }}</span>
            </span>
          </div>
        </div>
        <div class="table-item__actions">
          <el-button size="small" round @click="$emit('design', t)">设计</el-button>
          <el-button size="small" round plain @click="handleSync(t)">同步</el-button>
          <el-button size="small" round type="danger" plain @click="handleDelete(t)">删除</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Coin, EditPen, Plus } from "@element-plus/icons-vue";
import { getBoTableList, deleteBoTable, deploySync, updateBoTable, type BoTable } from "@/api/bo-table";

const props = defineProps<{ appId: string; categoryId?: number | null }>();
const emit = defineEmits<{ design: [table: BoTable]; changed: []; create: [] }>();

const tables = ref<BoTable[]>([]);
const loading = ref(false);
const editingId = ref<number | null>(null);
const editValue = ref("");
let currentInput: any = null;

async function load() {
  if (!props.appId) return;
  loading.value = true;
  try {
    tables.value = (await getBoTableList(props.appId, props.categoryId ?? undefined)) || [];
  } catch {
    tables.value = [];
  } finally {
    loading.value = false;
  }
}

function handleDelete(t: BoTable) {
  ElMessageBox.confirm(`确定删除业务对象「${t.titleName}」吗？`, "删除确认", {
    confirmButtonText: "删除",
    cancelButtonText: "取消",
    type: "warning",
  }).then(async () => {
    await deleteBoTable(t.id);
    ElMessage.success("已删除");
    emit("changed");
    load();
  }).catch(() => {});
}

async function handleSync(t: BoTable) {
  ElMessageBox.confirm(`同步「${t.titleName}」的设计到实体表？`, "部署同步", {
    confirmButtonText: "同步",
    cancelButtonText: "取消",
    type: "info",
  }).then(async () => {
    await deploySync(t.id);
    ElMessage.success("同步完成");
    load();
  }).catch(() => {});
}

function handleStartRename(t: BoTable) {
  editingId.value = t.id;
  editValue.value = t.titleName;
  nextTick(() => {
    currentInput?.focus();
  });
}

async function handleSaveRename(t: BoTable) {
  const val = editValue.value?.trim();
  if (!val) return;
  try {
    await updateBoTable(t.id, { titleName: val });
    ElMessage.success("名称已更新");
    emit("changed");
    load();
  } catch {
    // ignore
  } finally {
    editingId.value = null;
  }
}

function handleCancelRename() {
  editingId.value = null;
}

watch(() => [props.appId, props.categoryId], () => load(), { immediate: true });

defineExpose({ load });
</script>

<style scoped lang="scss">
.bo-list {
  background: #fff;
  border-radius: 14px;
  border: 1px solid #f1f5f9;
  overflow: hidden;
}

.list-toolbar {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f2f5;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.list-count {
  font-size: 13px;
  color: #64748b;
}

.list-empty {
  padding: 48px 0;
}

.table-list {
  display: flex;
  flex-direction: column;
}

.table-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid #f0f2f5;
  transition: background 0.15s;

  &:hover {
    background: #f8fafc;
  }

  &:last-child {
    border-bottom: none;
  }

  &__left {
    display: flex;
    align-items: center;
    gap: 14px;
    min-width: 0;
    flex: 1;
  }

  &__icon {
    width: 40px;
    height: 40px;
    background: #eef2ff;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #6366f1;
    flex-shrink: 0;
  }

  &__info {
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-width: 0;
  }

  &__title {
    font-size: 14px;
    font-weight: 600;
    color: #1e293b;
    display: inline-flex;
    align-items: center;
    gap: 6px;
    min-height: 28px;
  }

  .inline-edit-input {
    width: 200px;
  }

  .title-text {
    cursor: default;
  }

  &__meta {
    font-size: 12px;
    color: #94a3b8;
    display: flex;
    align-items: center;
    gap: 6px;

    code {
      background: #f1f5f9;
      padding: 1px 6px;
      border-radius: 4px;
      font-family: monospace;
      font-size: 11px;
      color: #6366f1;
    }
  }

  &__actions {
    display: flex;
    gap: 6px;
    flex-shrink: 0;
  }
}

.meta-divider {
  color: #e2e8f0;
}

.edit-icon {
  color: #94a3b8;
  cursor: pointer;
  opacity: 0;
  transition: all 0.2s;

  &:hover {
    color: #3b82f6;
  }
}

.table-item:hover .edit-icon {
  opacity: 1;
}
</style>
