<template>
  <el-dialog v-model="dialogVisible" title="" width="90vw" top="2vh" :close-on-click-modal="false" destroy-on-close class="bo-designer-dialog">
    <template #header>
      <div class="designer-navbar">
        <div class="back-btn" @click="handleCancel">
          <span class="back-arrow">←</span>
          <span>返回</span>
        </div>
        <div class="designer-title">{{ table?.titleName || '业务对象设计' }}</div>
        <div class="designer-sub" v-if="table">{{ table.bizCode }}</div>
      </div>
    </template>

    <div class="designer-body" v-if="table">
      <!-- 基本信息 -->
      <div class="card">
        <div class="card-header">
          <h3>基本信息</h3>
        </div>
        <div class="card-body">
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">标题名称</span>
              <span class="info-value">
                {{ editTitle }}
                <span class="edit-hint">（从台账编辑）</span>
              </span>
            </div>
            <div class="info-item">
              <span class="info-label">存储名称</span>
              <span class="info-value">
                <code>{{ table.storageName }}</code>
                <button class="copy-btn" @click="copyText(table.storageName)">复制</button>
              </span>
            </div>
            <div class="info-item">
              <span class="info-label">业务编码</span>
              <span class="info-value">
                <code>{{ table.bizCode }}</code>
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 字段列表 -->
      <div class="card">
        <div class="card-header">
          <h3>字段列表</h3>
        </div>
        <div class="card-body">
          <div class="field-toolbar">
            <div class="toolbar-left">
              <div class="search-box">
                <span class="search-icon">🔍</span>
                <input
                  v-model="searchKeyword"
                  type="text"
                  placeholder="搜索字段名称、标题..."
                  class="search-input"
                />
              </div>
              <button class="tool-btn" @click="ElMessage.info('引用其他业务对象字段（开发中）')">引用</button>
              <button class="tool-btn delete-tool-btn" @click="batchDelete">删除</button>
            </div>
            <button class="tool-btn add-btn" @click="addField">+ 添加字段</button>
          </div>

          <!-- 主表字段 -->
          <div class="field-group">
            <div class="field-group__header" @click="mainTableExpanded = !mainTableExpanded">
              <div class="field-group__header-left">
                <el-icon class="field-group__arrow" :class="{ 'is-expanded': mainTableExpanded }">
                  <ArrowRight />
                </el-icon>
                <span class="field-group__tag field-group__tag--main">主</span>
                <span class="field-group__title">{{ table?.titleName || '主表' }}</span>
                <span class="field-group__count">{{ mainFields.length }}</span>
              </div>
            </div>
            <transition name="slide">
              <div v-show="mainTableExpanded" class="field-group__body">
                <div class="field-table-wrapper">
                  <table class="field-table">
                    <thead>
                      <tr>
                        <th style="width:40px" class="checkbox-col">
                          <input type="checkbox" v-model="selectAllMain" @change="handleSelectAllMain" class="row-checkbox" />
                        </th>
                        <th style="width:30px"></th>
                        <th>名称</th>
                        <th>标题</th>
                        <th style="width:100px">类型</th>
                        <th>长度</th>
                        <th>组件</th>
                        <th>默认值</th>
                        <th style="width:52px;white-space:nowrap" class="checkbox-col">必填</th>
                        <th style="width:52px;white-space:nowrap" class="checkbox-col">可见</th>
                        <th style="width:52px;white-space:nowrap" class="checkbox-col">只读</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr
                        v-for="f in mainFields"
                        :key="f._key"
                        :class="{ selected: f._checked }"
                      >
                        <td class="checkbox-col">
                          <input type="checkbox" v-model="f._checked" @change="updateSelectAllMain" class="row-checkbox" />
                        </td>
                        <td class="drag-handle">⋮⋮</td>
                        <td>
                          <span v-if="f.id" class="name-display">{{ f.fieldName }}</span>
                          <input v-else v-model="f.fieldName" type="text" class="name-input" placeholder="字段名称" />
                        </td>
                        <td>
                          <input v-model="f.fieldTitle" type="text" class="inline-input" placeholder="标题" />
                        </td>
                        <td>
                          <select v-model="f.fieldType" class="inline-select type-select" @change="onTypeChange(f)">
                            <option value="文本">文本</option>
                            <option value="数字">数字</option>
                            <option value="日期">日期</option>
                            <option value="大文本">大文本</option>
                          </select>
                        </td>
                        <td>
                          <input v-model.number="f.fieldLength" type="text" class="inline-input" style="width:60px" placeholder="长度" />
                        </td>
                        <td>
                          <select v-model="f.component" class="inline-select">
                            <option v-for="opt in getComponentOptions(f.fieldType)" :key="opt" :value="opt">{{ opt }}</option>
                          </select>
                        </td>
                        <td>
                          <input v-model="f.defaultValue" type="text" class="inline-input" placeholder="默认值" />
                        </td>
                        <td class="checkbox-col">
                          <input type="checkbox" v-model="f.required" :true-value="1" :false-value="0" class="required-checkbox" />
                        </td>
                        <td class="checkbox-col">
                          <input type="checkbox" v-model="f.visible" :true-value="1" :false-value="0" class="visible-checkbox" />
                        </td>
                        <td class="checkbox-col">
                          <input type="checkbox" v-model="f.readonly" :true-value="1" :false-value="0" class="readonly-checkbox" />
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </transition>
          </div>

          <!-- 系统字段 -->
          <div class="field-group">
            <div class="field-group__header" @click="systemFieldsExpanded = !systemFieldsExpanded">
              <div class="field-group__header-left">
                <el-icon class="field-group__arrow" :class="{ 'is-expanded': systemFieldsExpanded }">
                  <ArrowRight />
                </el-icon>
                <span class="field-group__tag field-group__tag--system">系</span>
                <span class="field-group__title">系统字段</span>
                <span class="field-group__count">{{ systemFields.length }}</span>
              </div>
            </div>
            <transition name="slide">
              <div v-show="systemFieldsExpanded" class="field-group__body">
                <div class="field-table-wrapper">
                  <table class="field-table">
                    <thead>
                      <tr>
                        <th style="width:40px" class="checkbox-col">
                          <input type="checkbox" v-model="selectAllSystem" @change="handleSelectAllSystem" class="row-checkbox" />
                        </th>
                        <th style="width:30px"></th>
                        <th>名称</th>
                        <th>标题</th>
                        <th style="width:100px">类型</th>
                        <th>长度</th>
                        <th>组件</th>
                        <th>默认值</th>
                        <th style="width:52px;white-space:nowrap" class="checkbox-col">必填</th>
                        <th style="width:52px;white-space:nowrap" class="checkbox-col">可见</th>
                        <th style="width:52px;white-space:nowrap" class="checkbox-col">只读</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr
                        v-for="f in systemFields"
                        :key="f._key"
                        :class="{ selected: f._checked }"
                      >
                        <td class="checkbox-col">
                          <input type="checkbox" v-model="f._checked" @change="updateSelectAllSystem" class="row-checkbox" />
                        </td>
                        <td class="drag-handle">⋮⋮</td>
                        <td>
                          <span class="name-display">{{ f.fieldName }}</span>
                        </td>
                        <td>
                          <input v-model="f.fieldTitle" type="text" class="inline-input" placeholder="标题" />
                        </td>
                        <td>
                          <select v-model="f.fieldType" class="inline-select type-select" @change="onTypeChange(f)" disabled>
                            <option value="文本">文本</option>
                            <option value="数字">数字</option>
                            <option value="日期">日期</option>
                            <option value="大文本">大文本</option>
                          </select>
                        </td>
                        <td>
                          <input v-model.number="f.fieldLength" type="text" class="inline-input" style="width:60px" placeholder="长度" disabled />
                        </td>
                        <td>
                          <select v-model="f.component" class="inline-select" disabled>
                            <option v-for="opt in getComponentOptions(f.fieldType)" :key="opt" :value="opt">{{ opt }}</option>
                          </select>
                        </td>
                        <td>
                          <input v-model="f.defaultValue" type="text" class="inline-input" placeholder="默认值" />
                        </td>
                        <td class="checkbox-col">
                          <input type="checkbox" v-model="f.required" :true-value="1" :false-value="0" class="required-checkbox" disabled />
                        </td>
                        <td class="checkbox-col">
                          <input type="checkbox" v-model="f.visible" :true-value="1" :false-value="0" class="visible-checkbox" />
                        </td>
                        <td class="checkbox-col">
                          <input type="checkbox" v-model="f.readonly" :true-value="1" :false-value="0" class="readonly-checkbox" disabled />
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </transition>
          </div>
        </div>
      </div>

      <!-- 索引 -->
      <div class="card card-compact">
        <div class="card-body">
          <div class="index-section">
            <span class="index-label">索引：</span>
            <div class="index-tags">
              <span v-for="(tag, i) in indexTags" :key="i" class="index-tag">
                {{ tag === 'ID' ? '🔷' : '🔹' }} {{ tag }}
                <span v-if="tag !== 'ID'" class="remove" @click="indexTags.splice(i, 1)">✕</span>
              </span>
            </div>
            <button class="add-index-btn" @click="addIndex">+ 添加索引</button>
          </div>
        </div>
      </div>

    </div>

  <template #footer>
    <div class="designer-footer">
      <button class="btn btn-default" @click="dialogVisible = false">取消</button>
      <button class="btn btn-primary" :disabled="saving" @click="handleSave">
        {{ saving ? '保存中...' : '保存' }}
      </button>
    </div>
  </template>
  </el-dialog>

  <!-- 模板选择对话框 -->
  <el-dialog v-model="showTemplateDialog" title="从模板组合" width="400px" destroy-on-close>
    <p style="font-size:13px;color:#64748b;margin-bottom:16px;">选择一个模板快速添加字段组合：</p>
    <el-radio-group v-model="selectedTemplate" direction="vertical" style="width:100%">
      <el-radio value="bo_default" border style="margin-bottom:8px;">BO 默认字段（11个）</el-radio>
      <el-radio value="contact" border style="margin-bottom:8px;">联系人（姓名、电话、邮箱、地址）</el-radio>
      <el-radio value="address" border>地址（省、市、区、详细地址）</el-radio>
    </el-radio-group>
    <template #footer>
      <el-button @click="showTemplateDialog = false">取消</el-button>
      <el-button type="primary" @click="applyTemplate">应用</el-button>
    </template>
  </el-dialog>

  <!-- 新建索引弹窗 -->
  <el-dialog v-model="showIndexModal" title="新建索引" width="500px" destroy-on-close>
    <div class="modal-body">
      <div class="form-group">
        <label>索引类型</label>
        <select v-model="indexForm.type" class="form-select">
          <option value="普通索引">普通索引</option>
          <option value="唯一索引">唯一索引</option>
        </select>
      </div>
      <div class="form-group">
        <label>索引字段</label>
        <select v-model="indexForm.field" class="form-select">
          <option value="">请选择</option>
          <option v-for="f in fields.filter(f => INDEX_FIELD_NAMES.includes(f.fieldName))" :key="f._key" :value="f.fieldName">{{ f.fieldName }} ({{ f.fieldTitle || f.fieldName }}) - {{ f.fieldType }}</option>
        </select>
      </div>
      <div class="form-group">
        <label>索引备注</label>
        <input v-model="indexForm.remark" type="text" class="form-input" placeholder="请输入索引备注" />
        <div class="remark-hint">
          <span>*创建日期</span>
          <span>*创建人</span>
          <span>*更新日期</span>
          <span>*更新人</span>
          <span>*组织ID</span>
        </div>
      </div>
    </div>
    <template #footer>
      <button class="modal-btn modal-btn-cancel" @click="showIndexModal = false">取消</button>
      <button class="modal-btn modal-btn-confirm" @click="confirmAddIndex">确定</button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { ArrowRight } from "@element-plus/icons-vue";
import { getBoTableDetail, saveBoDesign, type BoTable, type BoField } from "@/api/bo-table";
import { useBoFieldManager } from "./composables/useBoFieldManager";
import { useBoIndexManager } from "./composables/useBoIndexManager";
import { useBoTemplateManager } from "./composables/useBoTemplateManager";

const props = defineProps<{ modelValue: boolean; tableId?: number | null }>();
const emit = defineEmits<{ "update:modelValue": [val: boolean]; saved: [] }>();

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (val) => emit("update:modelValue", val),
});

// 使用 composables
const { fields, searchKeyword, selectAllMain, selectAllSystem, mainFields, systemFields, DEFAULT_FIELD_NAMES, setFields, addField, toggleSelectAllMain, toggleSelectAllSystem } = useBoFieldManager();
const { indexTags, showIndexModal, indexForm, INDEX_FIELD_NAMES, addIndex, confirmAddIndex, removeIndex } = useBoIndexManager();
const { showTemplateDialog, selectedTemplate, TEMPLATES, openTemplateDialog, applyTemplate } = useBoTemplateManager();

const table = ref<BoTable | null>(null);
const editTitle = ref("");
const saving = ref(false);
const mainTableExpanded = ref(true);
const systemFieldsExpanded = ref(false);
const selectAll = ref(false);
const selectedSet = ref<Set<string>>(new Set());

const COMPONENT_MAP: Record<string, string[]> = {
  "文本": ["单行文本", "下拉选择", "隐藏"],
  "数字": ["数字输入", "滑块", "隐藏"],
  "日期": ["日期选择", "日期时间", "隐藏"],
  "大文本": ["多行文本", "富文本", "隐藏"],
};

// Old DB type → new display type
const DB_TO_UI_TYPE: Record<string, string> = {
  "varchar": "文本", "int": "数字", "bigint": "数字", "decimal": "数字",
  "datetime": "日期", "text": "大文本",
};

// New display type → DB type (reverse, for saving)
const UI_TO_DB_TYPE: Record<string, string> = {
  "文本": "varchar", "数字": "int", "日期": "datetime", "大文本": "text",
};

function migrateField(f: BoField): BoField {
  if (DB_TO_UI_TYPE[f.fieldType]) {
    f.fieldType = DB_TO_UI_TYPE[f.fieldType];
    // ensure component is valid for the new type
    const opts = getComponentOptions(f.fieldType);
    if (!opts.includes(f.component)) f.component = opts[0];
  }
  return f;
}

function getComponentOptions(type: string): string[] {
  return COMPONENT_MAP[type] || COMPONENT_MAP["文本"];
}

const TYPE_DEFAULT_LENGTH: Record<string, number> = {
  "文本": 128, "数字": 10, "日期": 0, "大文本": 2000,
};

function onTypeChange(f: any) {
  const options = getComponentOptions(f.fieldType);
  if (!options.includes(f.component)) {
    f.component = options[0];
  }
  f.fieldLength = TYPE_DEFAULT_LENGTH[f.fieldType] ?? 128;
}

let keyCounter = 0;
function generateKey() {
  return `_field_${++keyCounter}`;
}

const filteredFields = computed(() => {
  // Hide system fields from display
  let list = fields.value.filter(f => !DEFAULT_FIELD_NAMES.includes(f.fieldName));
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase();
    list = list.filter(f =>
      f.fieldName.toLowerCase().includes(kw) || f.fieldTitle?.toLowerCase().includes(kw)
    );
  }
  return list;
});

function handleSelectAllMain() {
  mainFields.value.forEach(f => {
    f._checked = selectAllMain.value;
  });
}

function handleSelectAllSystem() {
  systemFields.value.forEach(f => {
    f._checked = selectAllSystem.value;
  });
}

function updateSelectAllMain() {
  const filtered = mainFields.value;
  const checked = filtered.filter(f => f._checked);
  selectAllMain.value = filtered.length > 0 && checked.length === filtered.length;
}

function updateSelectAllSystem() {
  const filtered = systemFields.value;
  const checked = filtered.filter(f => f._checked);
  selectAllSystem.value = filtered.length > 0 && checked.length === filtered.length;
}

function handleSelectAll() {
  filteredFields.value.forEach(f => {
    f._checked = selectAll.value;
    if (selectAll.value) selectedSet.value.add(f._key!);
    else selectedSet.value.delete(f._key!);
  });
}

function updateSelectAll() {
  const filtered = filteredFields.value;
  const checked = filtered.filter(f => f._checked);
  selectAll.value = filtered.length > 0 && checked.length === filtered.length;
  checked.forEach(f => selectedSet.value.add(f._key!));
  filtered.filter(f => !f._checked).forEach(f => selectedSet.value.delete(f._key!));
}

function batchDelete() {
  const toDelete = fields.value.filter(f => f._checked);
  if (toDelete.length === 0) {
    ElMessage.warning("请先勾选要删除的字段");
    return;
  }
  ElMessageBox.confirm(`确定删除选中的 ${toDelete.length} 个字段吗？`, "确认", {
    confirmButtonText: "删除",
    cancelButtonText: "取消",
    type: "warning",
  }).then(() => {
    const keys = new Set(toDelete.map(f => f._key));
    fields.value = fields.value.filter(f => !keys.has(f._key));
    selectedSet.value.clear();
    selectAll.value = false;
  }).catch(() => {});
}

function copyText(text: string) {
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success("已复制");
  });
}

async function load() {
  if (!props.tableId) return;
  try {
    const detail = await getBoTableDetail(props.tableId);
    if (detail) {
      table.value = detail.table;
      editTitle.value = detail.table.titleName;
      fields.value = (detail.fields || []).map(f => migrateField({ ...f, _key: generateKey(), _checked: false }));
    }
  } catch (e) {
    ElMessage.error("加载失败");
  }
}

async function handleSave() {
  if (!table.value) return;
  // Validate user fields: name, title, length are required
  const userFields = fields.value.filter(f => !DEFAULT_FIELD_NAMES.includes(f.fieldName));
  for (const f of userFields) {
    if (!f.fieldName.trim()) {
      ElMessage.warning("字段名称不能为空"); return;
    }
    if (!f.fieldTitle.trim()) {
      ElMessage.warning(`字段「${f.fieldName}」标题不能为空`); return;
    }
    if (!f.fieldLength) {
      ElMessage.warning(`字段「${f.fieldName}」长度不能为空`); return;
    }
  }
  saving.value = true;
  try {
    // Remove internal props, convert type back to DB format
    const cleanFields = fields.value.map(({ _key, _checked, fieldType, ...rest }) => ({
      ...rest,
      fieldType: UI_TO_DB_TYPE[fieldType] || fieldType,
    }));

    // Build indexes JSON from indexTags
    const indexes = indexTags.value.map(name => ({
      id: crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}_${Math.random().toString(36).slice(2, 9)}`,
      name: name === "ID" ? "idx_id" : `idx_${name.toLowerCase()}`,
      type: name === "ID" ? "UNIQUE" : "INDEX",
      boItems: name,
      comment: "",
    }));

    await saveBoDesign(table.value.id, { fields: cleanFields, indexes: JSON.stringify(indexes) });
    ElMessage.success("保存成功");
    emit("saved");
    dialogVisible.value = false;
  } catch (e: any) {
    ElMessage.error(e?.message || "保存失败");
  } finally {
    saving.value = false;
  }
}

function handleCancel() {
  ElMessageBox.confirm("确定取消吗？未保存的更改将丢失。", "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "再想想",
    type: "warning",
  }).then(() => {
    dialogVisible.value = false;
  }).catch(() => {});
}

watch([() => props.modelValue, () => props.tableId], ([val, id]) => {
  if (val && id) {
    load();
  }
});
</script>

<style scoped lang="scss">
/* ====== 导航栏 ====== */
.designer-navbar {
  display: flex;
  align-items: center;
  height: 64px;
  padding: 0 32px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #475569;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  padding: 8px 14px;
  border-radius: 40px;
  transition: all 0.2s ease;

  &:hover {
    background: #f1f5f9;
    color: #3b82f6;
    transform: translateX(-2px);
  }
}

.back-arrow { font-size: 18px; }

.designer-title {
  font-size: 20px;
  font-weight: 600;
  margin-left: 12px;
  padding-left: 16px;
  border-left: 2px solid #e2e8f0;
  color: #1e293b;
}

.designer-sub {
  margin-left: 12px;
  font-size: 12px;
  font-weight: 500;
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.1);
  padding: 4px 12px;
  border-radius: 40px;
}

/* ====== 内容区 ====== */
.designer-body {
  background: linear-gradient(135deg, #f5f7fa 0%, #eef2f6 100%);
  padding: 28px 32px;
  max-height: 72vh;
  overflow-y: auto;
}

/* ====== 卡片 ====== */
.card {
  background: rgba(255, 255, 255, 0.98);
  border-radius: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.02);
  margin-bottom: 24px;
  overflow: hidden;
  border: 1px solid rgba(226, 232, 240, 0.4);

  &:last-child { margin-bottom: 0; }
}

.card-header {
  padding: 18px 24px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(248, 250, 252, 0.6);

  h3 {
    font-size: 15px;
    font-weight: 600;
    color: #1e293b;
    display: flex;
    align-items: center;
    gap: 10px;
    margin: 0;
  }
}

.card-body {
  padding: 24px;
}

/* ====== 基本信息 ====== */
.info-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 20px 48px;
}

.info-item {
  display: flex;
  align-items: baseline;
  gap: 16px;
  background: #f8fafc;
  padding: 8px 20px 8px 16px;
  border-radius: 16px;
  border: 1px solid #eef2ff;
}

.info-label {
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-value {
  font-size: 14px;
  color: #1e293b;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 10px;

  code {
    background: #fff;
    padding: 4px 12px;
    border-radius: 12px;
    font-family: 'SF Mono', 'Fira Code', monospace;
    font-size: 12px;
    color: #3b82f6;
    border: 1px solid #e2e8f0;
  }
}

.edit-hint {
  font-size: 11px;
  color: #94a3b8;
  margin-left: 4px;
  font-weight: normal;
}

.copy-btn {
  background: none;
  border: none;
  color: #3b82f6;
  cursor: pointer;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 20px;
  transition: all 0.2s;
  font-weight: 500;

  &:hover { background: #eff6ff; }
}

/* ====== 工具栏 ====== */
.field-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar-left {
  display: flex;
  gap: 10px;
  align-items: center;
  flex: 1;
}

.search-box {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 48px;
  padding: 6px 16px;
  border: 1px solid #e2e8f0;
  width: 280px;
  transition: all 0.2s;

  &:focus-within {
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  }
}

.search-icon {
  color: #94a3b8;
  font-size: 14px;
  margin-right: 4px;
}

.search-input {
  border: none;
  background: none;
  padding: 8px 10px;
  font-size: 13px;
  outline: none;
  width: 100%;
  font-family: inherit;
}

.tool-btn {
  padding: 8px 18px;
  border-radius: 40px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  background: #fff;
  border: 1px solid #e2e8f0;
  color: #475569;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: inherit;

  &:hover {
    background: #f8fafc;
    transform: translateY(-1px);
  }
}

.delete-tool-btn {
  color: #ef4444;
  border-color: #fecaca;

  &:hover {
    background: #fef2f2;
    color: #dc2626;
  }
}

.add-btn {
  background: linear-gradient(105deg, #3b82f6 0%, #2563eb 100%);
  color: #fff;
  border: none;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.2);

  &:hover {
    background: linear-gradient(105deg, #2563eb 0%, #1d4ed8 100%);
    color: #fff;
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
  }
}

/* ====== 表格 ====== */
.field-table-wrapper {
  overflow-x: auto;
  border-radius: 20px;
}

/* ====== 字段分组折叠 ====== */
.field-group {
  margin-bottom: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  overflow: hidden;

  &:last-child {
    margin-bottom: 0;
  }
}

.field-group__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;

  &:hover {
    background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
  }
}

.field-group__header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.field-group__arrow {
  font-size: 14px;
  color: #64748b;
  transition: transform 0.3s ease;

  &.is-expanded {
    transform: rotate(90deg);
  }
}

.field-group__tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;

  &--main {
    background: linear-gradient(135deg, #3b82f6, #2563eb);
    color: #fff;
    box-shadow: 0 2px 6px rgba(59, 130, 246, 0.3);
  }

  &--system {
    background: linear-gradient(135deg, #94a3b8, #64748b);
    color: #fff;
    box-shadow: 0 2px 6px rgba(148, 163, 184, 0.3);
  }
}

.field-group__title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.field-group__count {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  background: #e2e8f0;
  padding: 2px 10px;
  border-radius: 20px;
}

.field-group__body {
  border-top: 1px solid #e2e8f0;
}

/* 折叠动画 */
.slide-enter-active,
.slide-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}

.slide-enter-from,
.slide-leave-to {
  opacity: 0;
  max-height: 0;
}

.slide-enter-to,
.slide-leave-from {
  opacity: 1;
  max-height: 2000px;
}

.field-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;

  th {
    text-align: left;
    padding: 14px 12px;
    background: #f8fafc;
    color: #475569;
    font-weight: 600;
    font-size: 12px;
    border-bottom: 1.5px solid #e2e8f0;
  }

  td {
    padding: 14px 12px;
    border-bottom: 1px solid #f1f5f9;
    color: #334155;
    vertical-align: middle;
  }

  tbody tr:hover {
    background: #fafcff;
  }

  tbody tr.selected {
    background: linear-gradient(90deg, #eff6ff 0%, #f0f9ff 100%);
  }
}

.drag-handle {
  cursor: grab;
  color: #cbd5e1;
  text-align: center;
}

.checkbox-col {
  text-align: center;
}

.row-checkbox {
  width: 18px;
  height: 18px;
  cursor: pointer;
  accent-color: #3b82f6;
}

.required-checkbox, .visible-checkbox, .readonly-checkbox {
  width: 18px;
  height: 18px;
  cursor: pointer;
  accent-color: #3b82f6;
}

/* 字段名称输入框 */
.name-input {
  padding: 6px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-size: 12px;
  font-family: 'SF Mono', 'Fira Code', monospace;
  background: #fff;
  width: 100%;
  box-sizing: border-box;
  outline: none;
  transition: all 0.2s;

  &:focus {
    outline: none;
    border-color: #3b82f6;
    box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
  }

  &::placeholder {
    color: #cbd5e1;
  }
}

/* 已保存字段名称展示 */
.name-display {
  font-family: 'SF Mono', 'Fira Code', monospace;
  font-size: 12px;
  color: #1e293b;
  background: #f8fafc;
  padding: 4px 8px;
  border-radius: 8px;
  display: inline-block;
}

/* 内联输入框 */
.inline-input {
  padding: 6px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  font-size: 12px;
  background: #fff;
  transition: all 0.2s;
  outline: none;
  font-family: inherit;
  width: 100%;
  box-sizing: border-box;

  &:focus {
    outline: none;
    border-color: #3b82f6;
  }
}

/* 内联下拉 */
.inline-select {
  padding: 6px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  font-size: 12px;
  background: #fff;
  transition: all 0.2s;
  outline: none;
  font-family: inherit;
  cursor: pointer;
  max-width: 110px;

  &:focus {
    outline: none;
    border-color: #3b82f6;
  }
}

.type-select {
  min-width: 76px;
}

/* ====== 索引 ====== */
.card-compact {
  .card-body {
    padding: 14px 24px;
  }
}

.index-section {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.index-label {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  white-space: nowrap;
}

.index-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  flex: 1;
}

.index-tag {
  background: linear-gradient(135deg, #eff6ff 0%, #eef2ff 100%);
  color: #2563eb;
  padding: 6px 16px;
  border-radius: 40px;
  font-size: 12px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid rgba(59, 130, 246, 0.15);

  .remove {
    cursor: pointer;
    font-size: 16px;
    color: #64748b;
    line-height: 1;

    &:hover { color: #ef4444; }
  }
}

.add-index-btn {
  background: none;
  border: 1px dashed #cbd5e1;
  padding: 6px 18px;
  border-radius: 40px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
  transition: all 0.2s;
  font-family: inherit;

  &:hover {
    border-color: #3b82f6;
    color: #3b82f6;
    background: #eff6ff;
  }
}

/* ====== 底部按钮 ====== */
.designer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  padding: 20px 32px;
  border-top: 1px solid rgba(226, 232, 240, 0.8);
  background: #fff;
}

.btn {
  padding: 12px 36px;
  border-radius: 48px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
  font-family: inherit;
}

.btn-primary {
  background: linear-gradient(105deg, #3b82f6 0%, #2563eb 100%);
  color: #fff;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 16px rgba(37, 99, 235, 0.4);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
    transform: none;
  }
}

.btn-default {
  background: #fff;
  border: 1px solid #e2e8f0;
  color: #475569;

  &:hover { background: #f8fafc; }
}

/* ====== 弹窗表单 ====== */
.modal-body {
  padding: 0;
}

.form-group {
  margin-bottom: 20px;

  label {
    display: block;
    font-size: 13px;
    font-weight: 600;
    color: #1e293b;
    margin-bottom: 8px;
  }
}

.form-input, .form-select {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  font-size: 14px;
  outline: none;
  transition: all 0.2s;
  font-family: inherit;
  box-sizing: border-box;

  &:focus {
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  }
}

.remark-hint {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;

  span {
    background: #f1f5f9;
    padding: 2px 8px;
    border-radius: 16px;
  }
}

.modal-btn {
  padding: 8px 20px;
  border-radius: 40px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  font-family: inherit;
}

.modal-btn-cancel {
  background: #fff;
  border: 1px solid #e2e8f0;
  color: #475569;
}

.modal-btn-confirm {
  background: #3b82f6;
  color: #fff;
}
</style>

<style lang="scss">
.bo-designer-dialog {
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
