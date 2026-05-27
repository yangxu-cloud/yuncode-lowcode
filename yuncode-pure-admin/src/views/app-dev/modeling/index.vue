<template>
  <div class="modeling-page">
    <!-- 左侧：微应用导航 -->
    <aside class="modeling-sidebar">
      <!-- 品牌区 -->
      <div class="sidebar-brand">
        <div class="brand-logo">
          <el-icon :size="20"><Cpu /></el-icon>
        </div>
        <div class="brand-text">
          <span class="brand-title">业务建模</span>
          <span class="brand-sub">Business Modeling</span>
        </div>
      </div>

      <!-- 搜索区 -->
      <div class="sidebar-header">
        <div class="sidebar-search">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索应用..."
            clearable
            size="small"
            class="sidebar-search-input"
          >
            <template #prefix><el-icon :size="14"><Search /></el-icon></template>
          </el-input>
          <el-button size="small" circle :icon="Refresh" class="sidebar-refresh-btn" />
        </div>
      </div>

      <div class="sidebar-body">
        <!-- 微应用列表 -->
        <div class="sidebar-section">
          <div class="section-title">
            <span>微应用</span>
            <span class="section-count">{{ filteredApps.length }}</span>
          </div>

          <div class="app-tree">
            <template v-for="app in filteredApps" :key="app.id">
              <!-- 应用节点 -->
              <div
                class="app-node"
                :class="{ 'is-active': selectedKey === `app:${app.id}` }"
                @click="handleSelectApp(app)"
              >
                <div class="app-node__left">
                  <div class="app-node__icon" :style="{ background: getAvatarColor(app.appId) }">
                    {{ getAppInitial(app.appName) }}
                  </div>
                  <div class="app-node__info">
                    <span class="app-node__name">{{ app.appName }}</span>
                  </div>
                </div>
                <div class="app-node__actions">
                  <span class="node-action" title="添加一级分类" @click.stop="handleAddCategory(app)">
                    <el-icon :size="12"><Plus /></el-icon>
                  </span>
                </div>
              </div>

              <!-- 分类节点（两级） -->
              <div v-if="app.categories && app.categories.length > 0" class="category-list">
                <template v-for="cat in app.categories.filter(c => !c.parentId)" :key="cat.id">
                  <!-- 一级分类 -->
                  <div
                    class="category-node"
                    :class="{ 'is-active': selectedKey === `cat:${cat.id}` }"
                    @click="handleSelectCategory(app, cat)"
                  >
                    <el-icon :size="12" class="category-node__icon"><Folder /></el-icon>
                    <template v-if="editingCatId === cat.id">
                      <el-input
                        :ref="(el: any) => { if (el) currentCatInput = el }"
                        v-model="editCatValue"
                        size="small"
                        class="cat-inline-edit"
                        @click.stop
                        @blur="handleSaveCategory(cat)"
                        @keyup.enter="handleSaveCategory(cat)"
                        @keyup.esc="handleCancelCategory"
                      />
                    </template>
                    <span v-else class="category-node__name">{{ cat.name }}</span>
                    <div class="category-node__actions">
                      <el-icon :size="12" class="node-action" title="添加子分类" @click.stop="handleAddSubCategory(app, cat)"><Plus /></el-icon>
                      <el-icon :size="12" class="node-action" title="编辑" @click.stop="handleStartEditCategory(cat)"><EditPen /></el-icon>
                      <el-icon :size="12" class="node-action node-action--danger" title="删除" @click.stop="handleDeleteCategory(cat)"><Delete /></el-icon>
                    </div>
                  </div>
                  <!-- 二级分类 -->
                  <div
                    v-for="sub in app.categories.filter(c => c.parentId === cat.id)"
                    :key="sub.id"
                    class="category-node category-node--sub"
                    :class="{ 'is-active': selectedKey === `cat:${sub.id}` }"
                    @click="handleSelectCategory(app, sub)"
                  >
                    <el-icon :size="11" class="category-node__icon"><Document /></el-icon>
                    <template v-if="editingCatId === sub.id">
                      <el-input
                        :ref="(el: any) => { if (el) currentCatInput = el }"
                        v-model="editCatValue"
                        size="small"
                        class="cat-inline-edit"
                        @click.stop
                        @blur="handleSaveCategory(sub)"
                        @keyup.enter="handleSaveCategory(sub)"
                        @keyup.esc="handleCancelCategory"
                      />
                    </template>
                    <span v-else class="category-node__name">{{ sub.name }}</span>
                    <div class="category-node__actions">
                      <el-icon :size="11" class="node-action" title="编辑" @click.stop="handleStartEditCategory(sub)"><EditPen /></el-icon>
                      <el-icon :size="11" class="node-action node-action--danger" title="删除" @click.stop="handleDeleteCategory(sub)"><Delete /></el-icon>
                    </div>
                  </div>
                </template>
              </div>
            </template>

            <div v-if="filteredApps.length === 0" class="sidebar-empty">
              <el-icon :size="36" color="#9ca3af"><FolderOpened /></el-icon>
              <span>暂无应用</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部折叠指示器 -->
      <div class="sidebar-footer">
        <div class="sidebar-footer__line" />
      </div>
    </aside>

    <!-- 右侧：主内容区 -->
    <main class="modeling-main">
      <!-- 顶部导航栏 -->
      <header class="modeling-header">
        <div class="header-breadcrumb">
          <span
            class="breadcrumb-item"
            :class="{ active: activeTab === 'stats' }"
            @click="activeTab = 'stats'"
          >
            <el-icon :size="14"><DataAnalysis /></el-icon>
            概述
          </span>
          <template v-if="selectedCategory">
            <template v-if="stats.tables > 0">
              <span class="breadcrumb-sep" />
              <span class="breadcrumb-item" :class="{ active: activeTab === 'storage' }" @click="activeTab = 'storage'">
                <el-icon :size="14"><Coin /></el-icon>
                存储管理
              </span>
            </template>
            <template v-if="stats.forms > 0">
              <span class="breadcrumb-sep" />
              <span class="breadcrumb-item" :class="{ active: activeTab === 'form' }" @click="activeTab = 'form'">
                <el-icon :size="14"><Document /></el-icon>
                表单管理
              </span>
            </template>
            <template v-if="stats.workflows > 0">
              <span class="breadcrumb-sep" />
              <span class="breadcrumb-item" :class="{ active: activeTab === 'workflow' }" @click="activeTab = 'workflow'">
                <el-icon :size="14"><Share /></el-icon>
                流程管理
              </span>
            </template>
            <template v-if="stats.views > 0">
              <span class="breadcrumb-sep" />
              <span class="breadcrumb-item" :class="{ active: activeTab === 'view' }" @click="activeTab = 'view'">
                <el-icon :size="14"><DataLine /></el-icon>
                视图管理
              </span>
            </template>
            <template v-if="stats.charts > 0">
              <span class="breadcrumb-sep" />
              <span class="breadcrumb-item" :class="{ active: activeTab === 'dict' }" @click="activeTab = 'dict'">
                <el-icon :size="14"><Collection /></el-icon>
                字典管理
              </span>
            </template>
          </template>
        </div>
      </header>

      <!-- 内容滚动区 -->
      <div class="modeling-content">
        <!-- 统计 tab -->
        <template v-if="activeTab === 'stats'">
          <!-- 标题 + 操作按钮 -->
          <div class="content-toolbar">
            <div class="toolbar-title">
              <h1>{{ selectedName || '请选择应用' }}</h1>
              <span v-if="selectedKey" class="toolbar-subtitle">
                {{ selectedCategory ? '分类概览' : '应用概览' }}
              </span>
            </div>
            <div class="toolbar-actions">
              <el-button type="primary" :icon="Plus" v-if="showCreateButton" round @click="showCreateDialog = true">新建</el-button>
              <el-button :icon="Refresh" :disabled="!selectedKey" circle @click="handleRefresh" />
            </div>
          </div>

          <!-- 统计卡片 -->
          <div class="stats-grid">
            <div class="stat-card stat-card--indigo">
              <div class="stat-card__bg" />
              <div class="stat-card__header">
                <div class="stat-card__icon">
                  <el-icon :size="22"><Coin /></el-icon>
                </div>
                <span class="stat-card__badge stat-card__badge--green">+12%</span>
              </div>
              <p class="stat-card__label">数据库表</p>
              <p class="stat-card__value">{{ stats.tables }}</p>
              <div class="stat-card__bar">
                <div class="stat-card__bar-fill" style="width: 72%;" />
              </div>
            </div>
            <div class="stat-card stat-card--blue">
              <div class="stat-card__bg" />
              <div class="stat-card__header">
                <div class="stat-card__icon">
                  <el-icon :size="22"><Document /></el-icon>
                </div>
                <span class="stat-card__badge stat-card__badge--blue">稳定</span>
              </div>
              <p class="stat-card__label">表单</p>
              <p class="stat-card__value">{{ stats.forms }}</p>
              <div class="stat-card__bar">
                <div class="stat-card__bar-fill" style="width: 50%;" />
              </div>
            </div>
            <div class="stat-card stat-card--amber">
              <div class="stat-card__bg" />
              <div class="stat-card__header">
                <div class="stat-card__icon">
                  <el-icon :size="22"><Share /></el-icon>
                </div>
                <span class="stat-card__badge stat-card__badge--amber">活跃</span>
              </div>
              <p class="stat-card__label">流程</p>
              <p class="stat-card__value">{{ stats.workflows }}</p>
              <div class="stat-card__bar">
                <div class="stat-card__bar-fill" style="width: 35%;" />
              </div>
            </div>
            <div class="stat-card stat-card--purple">
              <div class="stat-card__bg" />
              <div class="stat-card__header">
                <div class="stat-card__icon">
                  <el-icon :size="22"><DataLine /></el-icon>
                </div>
                <span class="stat-card__badge stat-card__badge--purple">视图</span>
              </div>
              <p class="stat-card__label">视图台账</p>
              <p class="stat-card__value">{{ stats.views }}</p>
              <div class="stat-card__bar">
                <div class="stat-card__bar-fill" style="width: 25%;" />
              </div>
            </div>
          </div>

          <!-- 底部信息 -->
          <div class="bottom-grid">
            <!-- 最近操作日志 -->
            <div class="bottom-card">
              <h3 class="bottom-card__title">
                <el-icon :size="18" color="#6366f1"><Clock /></el-icon>
                最近操作日志
              </h3>
              <div class="log-list">
                <div v-for="(log, i) in recentLogs" :key="i" class="log-item">
                  <span class="log-dot" :style="{ background: log.color }" />
                  <div class="log-content">
                    <p class="log-text">{{ log.text }}</p>
                    <p class="log-meta">{{ log.user }} · {{ log.time }}</p>
                  </div>
                </div>
              </div>
            </div>

            <!-- 应用信息 -->
            <div class="bottom-card bottom-card--info" v-if="selectedApp">
              <div class="info-card__bg" />
              <h3 class="bottom-card__title info-title">
                <el-icon :size="18"><InfoFilled /></el-icon>
                应用信息
              </h3>
              <div class="info-grid">
                <div class="info-item">
                  <span class="info-label">应用ID</span>
                  <span class="info-value">{{ selectedApp.appId }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">版本</span>
                  <span class="info-value">v{{ selectedApp.version || '1.0.0' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">状态</span>
                  <span class="info-value">
                    <span class="info-status" :class="`info-status--${selectedApp.status}`">
                      {{ getStatusText(selectedApp.status) }}
                    </span>
                  </span>
                </div>
                <div class="info-item">
                  <span class="info-label">创建时间</span>
                  <span class="info-value">{{ selectedApp.createdAt || '-' }}</span>
                </div>
              </div>
            </div>
            <div class="bottom-card bottom-card--info" v-else>
              <div class="info-card__bg" />
              <h3 class="bottom-card__title info-title">
                <el-icon :size="18"><InfoFilled /></el-icon>
                应用信息
              </h3>
              <div class="info-empty">
                <el-icon :size="32" color="rgba(255,255,255,0.15)"><Monitor /></el-icon>
                <p>请在左侧选择一个应用查看详情</p>
              </div>
            </div>
          </div>
        </template>

        <!-- 存储管理 tab -->
        <template v-else-if="activeTab === 'storage'">
          <BoTableList
            :key="boListKey"
            :app-id="selectedApp?.appId || ''"
            :category-id="selectedCategory?.id"
            @design="handleDesignTable"
            @changed="handleStorageChanged"
            @create="showCreateBusinessObject = true"
          />
        </template>

        <!-- 其他 tab（占位） -->
        <template v-else>
          <div class="tab-placeholder">
            <el-empty :description="`「${getTabLabel(activeTab)}」功能开发中...`" />
          </div>
        </template>
      </div>
    </main>

    <!-- 新建资源弹窗 -->
    <CreateResourceDialog v-model="showCreateDialog" @select="handleCreateResource" />

    <!-- 新建业务对象弹窗 -->
    <CreateBusinessObject
      v-if="selectedApp"
      v-model="showCreateBusinessObject"
      :app-id="selectedApp.appId"
      :category-id="selectedCategory?.id || 0"
      @success="handleBoCreated"
    />

    <!-- BO 设计器 -->
    <BODesigner v-model="showDesigner" :table-id="designerTableId" @saved="handleDesignerSaved" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  Search, Refresh, Plus, EditPen, Delete, ArrowDown, FolderOpened, Folder,
  Coin, Document, Share, DataLine,
  Clock, InfoFilled, Cpu, DataAnalysis, Collection, Monitor
} from "@element-plus/icons-vue";
import CreateResourceDialog from "./CreateResourceDialog.vue";
import CreateBusinessObject from "./CreateBusinessObject.vue";
import BoTableList from "./BoTableList.vue";
import BODesigner from "./BODesigner.vue";
import { getApplicationList, getApplicationStats, getCategoryTree, createCategory, renameCategory, deleteCategory } from "@/api/application";

defineOptions({ name: "Modeling" });

// ===== 类型 =====
interface AppCategory {
  id: number;
  name: string;
  appId: string;
  parentId?: number | null; // null=一级分类，否则为二级分类
}

interface AppItem {
  id: number;
  appId: string;
  appName: string;
  version?: string;
  status: number;
  createdAt?: string;
  categories?: AppCategory[];
}

// ===== 状态 =====
const searchKeyword = ref("");
const selectedKey = ref("");
const selectedApp = ref<AppItem | null>(null);
const selectedCategory = ref<AppCategory | null>(null);
const activeTab = ref("stats");
const appList = ref<AppItem[]>([]);
const showCreateDialog = ref(false);
const showCreateBusinessObject = ref(false);
const showDesigner = ref(false);
const designerTableId = ref<number | null>(null);
const boListKey = ref(0);

// ===== 分类行内编辑状态 =====
const editingCatId = ref<number | null>(null);
const editCatValue = ref("");
let currentCatInput: any = null;

// ===== 统计数据 =====
const stats = ref({ tables: 0, forms: 0, workflows: 0, views: 0, charts: 0 });

async function loadStats(appId: string, category?: string) {
  try {
    const data = await getApplicationStats(appId, category);
    if (data) {
      stats.value = {
        tables: data.tables ?? 0,
        forms: data.forms ?? 0,
        workflows: data.workflows ?? 0,
        views: data.views ?? 0,
        charts: data.charts ?? 0,
      };
    }
  } catch {
    stats.value = { tables: 0, forms: 0, workflows: 0, views: 0, charts: 0 };
  }
}

// ===== 最近操作日志 =====
const recentLogs = ref([
  { text: "修改了 BO_EU_QMS_API_ASSO", user: "管理员 (admin)", time: "10分钟前", color: "#6366f1" },
  { text: "成功同步 WMS 库存数据", user: "系统自动", time: "45分钟前", color: "#22c55e" },
  { text: "新建存储模型 1 项", user: "张三 (zhangsan)", time: "2小时前", color: "#94a3b8" },
]);

// ===== 计算属性 =====
const filteredApps = computed(() => {
  if (!searchKeyword.value) return appList.value;
  const kw = searchKeyword.value.toLowerCase();
  return appList.value.filter(a => a.appName.toLowerCase().includes(kw) || a.appId.toLowerCase().includes(kw));
});

const selectedName = computed(() => {
  if (selectedCategory.value) return selectedCategory.value.name;
  if (selectedApp.value) return selectedApp.value.appName;
  return "";
});

// 只有叶子分类（无子分类）才显示"新建"按钮
const showCreateButton = computed(() => {
  if (!selectedCategory.value) return false;
  const app = appList.value.find(a => a.appId === selectedCategory.value?.appId);
  if (!app?.categories) return true;
  return !app.categories.some(c => c.parentId === selectedCategory.value?.id);
});

// ===== 方法 =====
function getAvatarColor(appId: string): string {
  const colors = [
    "linear-gradient(135deg, #6366f1, #818cf8)",
    "linear-gradient(135deg, #3b82f6, #60a5fa)",
    "linear-gradient(135deg, #22c55e, #4ade80)",
    "linear-gradient(135deg, #eab308, #facc15)",
    "linear-gradient(135deg, #ef4444, #f87171)",
    "linear-gradient(135deg, #ec4899, #f472b6)",
    "linear-gradient(135deg, #8b5cf6, #a78bfa)",
  ];
  let hash = 0;
  for (const ch of appId) hash = ((hash << 5) - hash + ch.charCodeAt(0)) | 0;
  return colors[Math.abs(hash) % colors.length];
}

function getAppInitial(name: string): string {
  return name?.charAt(0) || "A";
}

function getStatusText(status: number): string {
  const map: Record<number, string> = { 0: "未运行", 1: "运行中", 2: "已停止", 3: "异常", 4: "已卸载" };
  return map[status] || "未知";
}

function getTabLabel(tab: string): string {
  const map: Record<string, string> = { stats: "概述", storage: "存储管理", form: "表单管理", workflow: "流程管理", view: "视图管理", dict: "字典管理" };
  return map[tab] || tab;
}

function handleSelectApp(app: AppItem) {
  selectedKey.value = `app:${app.id}`;
  selectedApp.value = app;
  selectedCategory.value = null;
  activeTab.value = "stats";
  loadStats(app.appId);
}

function handleSelectCategory(app: AppItem, cat: AppCategory) {
  selectedKey.value = `cat:${cat.id}`;
  selectedApp.value = app;
  selectedCategory.value = cat;
  activeTab.value = "stats";
  loadStats(app.appId, cat.name);
}

function handleAddCategory(app: AppItem) {
  ElMessageBox.prompt("请输入分类名称", "添加一级分类", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    inputPattern: /^.{1,20}$/,
    inputErrorMessage: "分类名称长度 1-20 个字符",
    inputPlaceholder: "分类名称",
  }).then(async ({ value }) => {
    await createCategory(app.appId, value.trim());
    await loadCategories(app);
    ElMessage.success(`分类「${value.trim()}」已添加`);
  }).catch(() => {});
}

function handleAddSubCategory(app: AppItem, parentCat: AppCategory) {
  ElMessageBox.prompt("请输入子分类名称", `在「${parentCat.name}」下添加`, {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    inputPattern: /^.{1,20}$/,
    inputErrorMessage: "分类名称长度 1-20 个字符",
    inputPlaceholder: "子分类名称",
  }).then(async ({ value }) => {
    await createCategory(app.appId, value.trim(), parentCat.id);
    await loadCategories(app);
    ElMessage.success(`子分类「${value.trim()}」已添加`);
  }).catch(() => {});
}




function handleStartEditCategory(cat: AppCategory) {
  editingCatId.value = cat.id;
  editCatValue.value = cat.name;
  nextTick(() => {
    currentCatInput?.focus();
  });
}

async function handleSaveCategory(cat: AppCategory) {
  const val = editCatValue.value?.trim();
  if (!val || val.length > 20) return;
  try {
    const app = appList.value.find(a => a.appId === cat.appId);
    if (!app) return;
    await renameCategory(cat.appId, cat.id, val);
    await loadCategories(app);
  } catch {
    // ignore
  } finally {
    editingCatId.value = null;
  }
}

function handleCancelCategory() {
  editingCatId.value = null;
}

function handleDeleteCategory(cat: AppCategory) {
  const app = appList.value.find(a => a.appId === cat.appId);
  if (!app) return;

  const cats = app.categories || [];
  const childCount = cats.filter(c => c.parentId === cat.id).length;
  const msg = childCount > 0
    ? `确定要删除分类「${cat.name}」吗？其下 ${childCount} 个子分类将一并删除。`
    : `确定要删除分类「${cat.name}」吗？`;

  ElMessageBox.confirm(msg, "删除分类", {
    confirmButtonText: "删除",
    cancelButtonText: "取消",
    type: "warning",
  }).then(async () => {
    await deleteCategory(cat.appId, cat.id);
    await loadCategories(app);
    // 如果当前选中的是被删分类，清除选中
    if (selectedCategory.value?.id === cat.id) {
      selectedCategory.value = null;
      selectedKey.value = `app:${app.id}`;
    }
    ElMessage.success("分类已删除");
  }).catch(() => {});
}

function handleRefresh() {
  if (selectedApp.value) {
    loadStats(selectedApp.value.appId, selectedCategory.value?.name);
  }
}

function handleCreateResource(type: string) {
  if (type === "database") {
    showCreateBusinessObject.value = true;
    return;
  }
  const labels: Record<string, string> = { database: "业务数据库", form: "业务表单", workflow: "业务流程", ledger: "台账管理", dict: "关联字典" };
  ElMessage.info(`即将创建: 新建${labels[type] || type}`);
}

function handleBoCreated() {
  // 刷新 stats，使存储管理 tab 出现
  if (selectedApp.value) {
    loadStats(selectedApp.value.appId, selectedCategory.value?.name);
  }
  activeTab.value = "storage";
  boListKey.value++;
}

function handleDesignerSaved() {
  // BO 设计器保存后刷新
  if (selectedApp.value) {
    loadStats(selectedApp.value.appId, selectedCategory.value?.name);
  }
}

async function handleDesignTable(table: any) {
  designerTableId.value = table.id;
  showDesigner.value = true;
}

async function handleStorageChanged() {
  if (selectedApp.value) {
    await loadStats(selectedApp.value.appId, selectedCategory.value?.name);
    // 如果存储管理页签开着但已经没有表了，自动切回概述
    if (activeTab.value === 'storage' && stats.value.tables === 0) {
      activeTab.value = 'stats';
    }
  }
}

// ===== 分类加载 =====
async function loadCategories(app: AppItem) {
  try {
    const tree = await getCategoryTree(app.appId);
    if (tree && tree.length > 0) {
      const flat: AppCategory[] = [];
      const flatten = (nodes: any[]) => {
        for (const node of nodes) {
          flat.push({ id: node.id, name: node.name, appId: node.appId, parentId: node.parentId });
          if (node.children?.length) flatten(node.children);
        }
      };
      flatten(tree);
      // 通过 splice 确保 Vue 响应式检测到变化
      const idx = appList.value.findIndex(a => a.appId === app.appId);
      if (idx > -1) {
        appList.value[idx].categories = flat;
      }
    }
  } catch (e) {
    console.error(`加载分类失败: ${app.appId}`, e);
  }
}

// ===== 初始化 =====
async function loadApps() {
  try {
    const res = await getApplicationList({ current: 1, size: 100 });
    const records = res?.data?.records || [];
    if (records.length > 0) {
      const apps = records
        .filter((a: any) => a.status === 1)
        .map((a: any) => ({
          id: a.id,
          appId: a.appId,
          appName: a.appName,
          version: a.version,
          status: a.status,
          createdAt: a.createTime,
          categories: [] as AppCategory[],
        }));
      appList.value = apps;
      // 并行加载所有应用的分类
      await Promise.all(apps.map(a => loadCategories(a)));
    } else {
      console.warn("应用列表为空", res);
    }
  } catch (e) {
    console.error("加载应用列表失败", e);
  }
}

onMounted(() => {
  loadApps();
});
</script>

<style scoped lang="scss">
.modeling-page {
  display: flex;
  height: calc(100vh - 105px);
  overflow: hidden;
  background: #f1f5f9;
}

/* ==================== 左侧边栏 ==================== */
.modeling-sidebar {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #f8fafc;
  border-right: 1px solid #e5e7eb;
  position: relative;
}

/* 品牌区 */
.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 20px 16px;
  border-bottom: 1px solid #e5e7eb;
}

.brand-logo {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.25);
}

.brand-text {
  display: flex;
  flex-direction: column;
}

.brand-title {
  font-size: 15px;
  font-weight: 700;
  color: #111827;
  letter-spacing: 0.02em;
}

.brand-sub {
  font-size: 10px;
  color: #6b7280;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  margin-top: 1px;
}

.sidebar-header {
  padding: 14px 16px 10px;
}

.sidebar-search {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sidebar-search-input {
  flex: 1;

  :deep(.el-input__wrapper) {
    background: #fff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    box-shadow: none;
    transition: all 0.2s;

    &:hover {
      border-color: #c7d2fe;
    }
    &.is-focus {
      border-color: #6366f1;
      box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.1);
    }
  }

  :deep(.el-input__inner) {
    color: #374151;
    font-size: 13px;

    &::placeholder {
      color: #9ca3af;
    }
  }

  :deep(.el-input__prefix) {
    color: #9ca3af;
  }

  :deep(.el-input__clear) {
    color: #d1d5db;
  }
}

.sidebar-refresh-btn {
  background: transparent;
  border: 1px solid #d1d5db;
  color: #6b7280;
  transition: all 0.2s;

  &:hover {
    color: #6366f1;
    border-color: #a5b4fc;
    background: #eef2ff;
  }
}

.sidebar-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px 10px 16px;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: #d1d5db; border-radius: 4px; }
}

.sidebar-section {
  margin-bottom: 8px;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px 10px;
  font-size: 11px;
  font-weight: 600;
  color: #4b5563;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.section-count {
  font-size: 10px;
  background: #eef2ff;
  color: #6366f1;
  padding: 1px 7px;
  border-radius: 10px;
  font-weight: 600;
}

/* 应用节点 */
.app-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px 8px 8px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 2px;
  position: relative;
  gap: 6px;

  &:hover {
    background: #eef2ff;
  }

  &.is-active {
    background: #e0e7ff;

    .app-node__name { color: #4338ca; }

    &::before {
      content: "";
      position: absolute;
      left: 0;
      top: 8px;
      bottom: 8px;
      width: 3px;
      background: #6366f1;
      border-radius: 0 3px 3px 0;
    }
  }

  &__left {
    display: flex;
    align-items: center;
    min-width: 0;
    flex: 1;
    gap: 10px;
  }

  &__icon {
    width: 30px;
    height: 30px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 700;
    color: #fff;
    flex-shrink: 0;
  }

  &__info {
    min-width: 0;
    display: flex;
    flex-direction: column;
  }

  &__name {
    font-size: 13px;
    font-weight: 500;
    color: #374151;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    line-height: 1.4;
    transition: color 0.15s;
  }

  &__actions {
    display: flex;
    gap: 1px;
    flex-shrink: 0;
    opacity: 0.55;
    transition: opacity 0.15s;
  }

  &:hover &__actions {
    opacity: 1;
  }
}

/* 分类节点 */
.category-list {
  padding-left: 18px;
  border-left: 1px solid #e0e7ff;
  margin-left: 27px;
  margin-bottom: 4px;
}

.category-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px 6px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 1px;
  gap: 6px;

  &:hover {
    background: #f0f4ff;
  }

  &.is-active {
    background: #e0e7ff;
  }

  &--sub {
    padding-left: 20px;
  }

  &__icon {
    color: #6366f1;
    flex-shrink: 0;
  }

  &__name {
    flex: 1;
    font-size: 12px;
    color: #4b5563;
    overflow: hidden;

    .category-node--sub & {
      font-size: 11.5px;
      color: #9ca3af;
    }
  }

  .cat-inline-edit {
    flex: 1;
    min-width: 0;
  }

  &__actions {
    display: flex;
    gap: 1px;
    flex-shrink: 0;
    opacity: 0.5;
    transition: opacity 0.15s;
  }

  &:hover &__actions {
    opacity: 1;
  }
}

.node-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 6px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    color: #6366f1;
    background: #e0e7ff;
  }

  &--danger:hover {
    color: #ef4444;
    background: #fee2e2;
  }
}

.sidebar-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 48px 0;
  color: #4b5563;
  font-size: 13px;
}

.sidebar-footer {
  padding: 12px 20px;

  &__line {
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba(99, 102, 241, 0.25), transparent);
  }
}

/* ==================== 右侧主内容 ==================== */
.modeling-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.modeling-header {
  height: 48px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  padding: 0 28px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
}

.header-breadcrumb {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 13px;
}

.breadcrumb-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: #94a3b8;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 8px;
  transition: all 0.2s;
  font-weight: 500;

  &:hover {
    color: #6366f1;
    background: #f5f3ff;
  }
  &.active {
    color: #6366f1;
    background: #eef2ff;
    font-weight: 600;
  }
}

.breadcrumb-sep {
  width: 1px;
  height: 16px;
  background: #e2e8f0;
  margin: 0 4px;
}

.modeling-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px 28px;

  &::-webkit-scrollbar { width: 6px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: #d1d5db; border-radius: 4px; }
}

/* ==================== 工具栏 ==================== */
.content-toolbar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 24px;

  h1 {
    font-size: 24px;
    font-weight: 700;
    color: #0f172a;
    margin: 0;
    line-height: 1.2;
  }
}

.toolbar-subtitle {
  display: block;
  font-size: 13px;
  color: #94a3b8;
  margin-top: 4px;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

/* ==================== 统计卡片 ==================== */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #fff;
  border-radius: 14px;
  padding: 22px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;

  &:hover {
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08);
    transform: translateY(-3px);
  }

  &__bg {
    position: absolute;
    top: 0;
    right: 0;
    width: 100px;
    height: 100px;
    border-radius: 50%;
    filter: blur(40px);
    opacity: 0.06;
    pointer-events: none;
  }

  &--indigo &__bg { background: #6366f1; }
  &--blue &__bg { background: #3b82f6; }
  &--amber &__bg { background: #f59e0b; }
  &--purple &__bg { background: #a855f7; }

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 14px;
  }

  &__icon {
    width: 42px;
    height: 42px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: transform 0.3s;
  }

  &:hover &__icon {
    transform: scale(1.08);
  }

  &--indigo &__icon { background: #eef2ff; color: #6366f1; }
  &--blue &__icon { background: #eff6ff; color: #3b82f6; }
  &--amber &__icon { background: #fffbeb; color: #d97706; }
  &--purple &__icon { background: #faf5ff; color: #a855f7; }

  &__badge {
    font-size: 11px;
    font-weight: 600;
    padding: 3px 10px;
    border-radius: 20px;

    &--green { background: #f0fdf4; color: #16a34a; }
    &--blue { background: #eff6ff; color: #3b82f6; }
    &--amber { background: #fffbeb; color: #d97706; }
    &--red { background: #fef2f2; color: #dc2626; }
    &--purple { background: #faf5ff; color: #a855f7; }
  }

  &__label {
    font-size: 13px;
    color: #64748b;
    margin: 0 0 6px;
    font-weight: 500;
  }

  &__value {
    font-size: 28px;
    font-weight: 800;
    color: #0f172a;
    margin: 0 0 14px;
    letter-spacing: -0.02em;
  }

  &__bar {
    height: 4px;
    background: #f1f5f9;
    border-radius: 4px;
    overflow: hidden;
  }

  &__bar-fill {
    height: 100%;
    border-radius: 4px;
    transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);
  }

  &--indigo &__bar-fill { background: linear-gradient(90deg, #6366f1, #818cf8); }
  &--blue &__bar-fill { background: linear-gradient(90deg, #3b82f6, #60a5fa); }
  &--amber &__bar-fill { background: linear-gradient(90deg, #f59e0b, #fbbf24); }
  &--purple &__bar-fill { background: linear-gradient(90deg, #a855f7, #c084fc); }
}

/* ==================== 底部信息 ==================== */
.bottom-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

.bottom-card {
  background: #fff;
  border-radius: 14px;
  padding: 22px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);

  &--info {
    background: linear-gradient(135deg, #1e1b4b, #312e81);
    border: none;
    position: relative;
    overflow: hidden;
  }

  &__title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    font-weight: 600;
    color: #0f172a;
    margin: 0 0 18px;
  }
}

.info-card__bg {
  position: absolute;
  right: -40px;
  bottom: -40px;
  width: 160px;
  height: 160px;
  background: rgba(99, 102, 241, 0.1);
  border-radius: 50%;
  filter: blur(50px);
  pointer-events: none;
}

.info-title {
  color: #fff !important;
  position: relative;
  z-index: 1;
}

/* 日志列表 */
.log-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.log-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 10px;
  transition: background 0.15s;

  &:hover {
    background: #f8fafc;
  }
}

.log-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 5px;
  flex-shrink: 0;
}

.log-content {
  flex: 1;
}

.log-text {
  font-size: 13px;
  font-weight: 500;
  color: #1e293b;
  margin: 0 0 3px;
}

.log-meta {
  font-size: 12px;
  color: #94a3b8;
  margin: 0;
}

/* 应用信息 */
.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  position: relative;
  z-index: 1;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 12px 14px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.info-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.35);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  font-weight: 500;
}

.info-value {
  font-size: 14px;
  font-weight: 600;
  color: #e2e8f0;
}

.info-status {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 20px;

  &::before {
    content: "";
    width: 6px;
    height: 6px;
    border-radius: 50%;
  }

  &--0 { background: rgba(148, 163, 184, 0.15); color: #94a3b8; }
  &--0::before { background: #94a3b8; }
  &--1 { background: rgba(34, 197, 94, 0.15); color: #4ade80; }
  &--1::before { background: #4ade80; }
  &--2 { background: rgba(251, 191, 36, 0.15); color: #fbbf24; }
  &--2::before { background: #fbbf24; }
  &--3 { background: rgba(248, 113, 113, 0.15); color: #f87171; }
  &--3::before { background: #f87171; }
  &--4 { background: rgba(148, 163, 184, 0.15); color: #94a3b8; }
  &--4::before { background: #94a3b8; }
}

.info-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 24px 0;
  position: relative;
  z-index: 1;

  p {
    color: rgba(255, 255, 255, 0.3);
    font-size: 13px;
    margin: 0;
  }
}

/* ==================== 占位 ==================== */
.tab-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

</style>
