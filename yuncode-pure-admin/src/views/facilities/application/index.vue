<template>
  <div class="application-management">
    <div class="page-header">
      <div class="page-title-row">
        <span class="page-title">应用开发</span>
        <span class="page-subtitle">管理应用的安装、运行、分发与维护</span>
      </div>
      <el-tabs v-model="activeTab" class="main-tabs">
        <el-tab-pane label="应用管理" name="management" />
        <el-tab-pane label="应用部署" name="deploy" />
        <el-tab-pane label="应用卸载" name="uninstall" />
        <el-tab-pane label="应用分发" name="distribute" />
        <el-tab-pane label="应用日志" name="logs" />
        <el-tab-pane label="开发小组" name="team" />
      </el-tabs>
    </div>

    <div class="page-body">
      <!-- 应用管理 -->
      <template v-if="activeTab === 'management'">
        <div class="content-shell">
          <div class="toolbar">
            <div class="toolbar-left">
              <el-input
                v-model="searchKeyword"
                placeholder="搜索应用名称..."
                clearable
                class="search-input"
                @keyup.enter="handleSearch"
                @clear="handleSearch"
              >
                <template #prefix><el-icon :size="16"><Search /></el-icon></template>
              </el-input>
              <span class="stat-badge">共 {{ pagination.total }} 个应用</span>
            </div>
            <div class="toolbar-right">
              <div class="view-toggle">
                <el-button
                  :type="viewMode === 'card' ? 'primary' : ''"
                  :icon="Grid"
                  size="small"
                  @click="viewMode = 'card'"
                >卡片</el-button>
                <el-button
                  :type="viewMode === 'table' ? 'primary' : ''"
                  :icon="List"
                  size="small"
                  @click="viewMode = 'table'"
                >列表</el-button>
              </div>
              <el-button :icon="Refresh" circle @click="loadApplicationList()" />
              <el-button type="primary" :icon="Plus" @click="handleCreate">新建应用</el-button>
            </div>
          </div>

          <div v-if="viewMode === 'card'" v-loading="loading" class="app-grid">
            <div v-if="applicationList.length === 0 && !loading" class="empty-state">
              <el-empty description="暂无应用" />
            </div>
            <div
              v-for="app in applicationList"
              :key="app.id"
              class="app-card"
              :class="`app-card--${getStatusClass(app.status)}`"
            >
              <div class="app-card__bar" />
              <div class="app-card__top">
                <div class="app-card__avatar" :style="{ background: getAvatarColor(app.appId) }">
                  {{ getAppInitial(app.appName) }}
                </div>
                <div class="app-card__info">
                  <div class="app-card__name">{{ app.appName }}</div>
                  <div class="app-card__id">{{ app.appId }}</div>
                </div>
                <el-tag :type="getStatusTagType(app.status)" size="small" effect="light">
                  {{ getStatusText(app.status) }}
                </el-tag>
              </div>
              <div class="app-card__meta">
                <span class="meta-version">v{{ app.version || "1.0.0" }}</span>
                <span v-if="app.status === 1" class="meta-runtime">
                  <i class="runtime-dot" />{{ getRuntimeText(app) }}
                </span>
                <span v-else class="meta-idle">-</span>
              </div>
              <div class="app-card__actions">
                <el-button size="small" :type="app.status === 1 ? 'default' : 'primary'" :disabled="app.status === 1" @click="handleStart(app)">启动</el-button>
                <el-button size="small" :disabled="app.status !== 1" @click="handleStop(app)">停止</el-button>
                <el-button size="small" :disabled="app.status !== 1" @click="handleRestart(app)">重启</el-button>
                <el-button size="small" plain class="action-last" @click="appFormDialogRef?.open(app)">编辑</el-button>
                <el-button size="small" plain type="danger" @click="handleUninstall(app)">卸载</el-button>
              </div>
            </div>
          </div>

          <div v-if="viewMode === 'table'" v-loading="loading" class="app-table-wrap">
          <el-table :data="applicationList" row-key="id" size="default" stripe
            :header-cell-style="{ background: '#fafafa', color: '#606266', fontWeight: 600, fontSize: '13px' }"
          >
            <el-table-column label="应用" min-width="220">
              <template #default="{ row }">
                <div class="table-app-cell">
                  <div class="table-app-avatar" :style="{ background: getAvatarColor(row.appId) }">
                    {{ getAppInitial(row.appName) }}
                  </div>
                  <div class="table-app-info">
                    <span class="table-app-name">{{ row.appName }}</span>
                    <span class="table-app-id">{{ row.appId }}</span>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusTagType(row.status)" size="small" effect="light">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="版本" width="100">
              <template #default="{ row }">
                <span class="table-version">v{{ row.version || "1.0.0" }}</span>
              </template>
            </el-table-column>
            <el-table-column label="运行时长" width="120">
              <template #default="{ row }">
                <span v-if="row.status === 1" class="table-runtime">{{ getRuntimeText(row) }}</span>
                <span v-else class="table-runtime--idle">-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <div class="table-actions">
                  <el-button
                    size="small"
                    :type="row.status === 1 ? 'warning' : 'primary'"
                    @click="row.status === 1 ? handleStop(row) : handleStart(row)"
                  >
                    {{ row.status === 1 ? '停止' : '启动' }}
                  </el-button>
                  <el-dropdown @command="(cmd: string) => handleTableCommand(cmd, row)">
                    <el-button size="small">
                      更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="restart" :disabled="row.status !== 1">重启</el-dropdown-item>
                        <el-dropdown-item command="edit">编辑</el-dropdown-item>
                        <el-dropdown-item command="uninstall" divided>卸载</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="pagination.current"
            v-model:page-size="pagination.size"
            :total="pagination.total"
            :page-sizes="[12, 24, 48, 96]"
            layout="sizes, prev, pager, next, jumper, total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
        </div>
      </template>

      <!-- 应用卸载 -->
      <template v-if="activeTab === 'uninstall'">
        <div class="content-shell">
          <div class="toolbar">
            <div class="toolbar-left">
              <el-input
                v-model="searchKeyword"
                placeholder="搜索已卸载应用..."
                clearable
                class="search-input"
                @keyup.enter="handleSearchUninstalled"
                @clear="handleSearchUninstalled"
              >
                <template #prefix><el-icon :size="16"><Search /></el-icon></template>
              </el-input>
              <span class="stat-badge">已卸载 {{ pagination.total }} 个</span>
            </div>
            <div class="toolbar-right">
              <div class="view-toggle">
                <el-button
                  :type="viewMode === 'card' ? 'primary' : ''"
                  :icon="Grid"
                  size="small"
                  @click="viewMode = 'card'"
                >卡片</el-button>
                <el-button
                  :type="viewMode === 'table' ? 'primary' : ''"
                  :icon="List"
                  size="small"
                  @click="viewMode = 'table'"
                >列表</el-button>
              </div>
              <el-button :icon="Refresh" circle @click="loadUninstalledList()" />
            </div>
          </div>

          <div v-if="viewMode === 'card'" v-loading="loading" class="app-grid">
            <div v-if="applicationList.length === 0 && !loading" class="empty-state">
              <el-empty description="暂无已卸载应用" />
            </div>
            <div
              v-for="app in applicationList"
              :key="app.id"
              class="app-card"
              :class="`app-card--${getStatusClass(app.status)}`"
            >
              <div class="app-card__bar" />
              <div class="app-card__top">
                <div class="app-card__avatar" :style="{ background: getAvatarColor(app.appId) }">
                  {{ getAppInitial(app.appName) }}
                </div>
                <div class="app-card__info">
                  <div class="app-card__name">{{ app.appName }}</div>
                  <div class="app-card__id">{{ app.appId }}</div>
                </div>
                <el-tag :type="getStatusTagType(app.status)" size="small" effect="light">
                  {{ getStatusText(app.status) }}
                </el-tag>
              </div>
              <div class="app-card__meta">
                <span class="meta-version">v{{ app.version || "1.0.0" }}</span>
                <span class="meta-idle">-</span>
              </div>
              <div class="app-card__actions">
                <el-button size="small" type="primary" @click="handleRestore(app)">还原</el-button>
                <el-button size="small" type="danger" plain @click="handlePermanentDelete(app)">永久删除</el-button>
              </div>
            </div>
          </div>

          <div v-if="viewMode === 'table'" v-loading="loading" class="app-table-wrap">
          <el-table :data="applicationList" row-key="id" size="default" stripe
            :header-cell-style="{ background: '#fafafa', color: '#606266', fontWeight: 600, fontSize: '13px' }"
          >
            <el-table-column label="应用" min-width="200">
              <template #default="{ row }">
                <div class="table-app-cell">
                  <div class="table-app-avatar" style="background: #c0c4cc">
                    {{ getAppInitial(row.appName) }}
                  </div>
                  <div class="table-app-info">
                    <span class="table-app-name">{{ row.appName }}</span>
                    <span class="table-app-id">{{ row.appId }}</span>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="版本" width="100">
              <template #default="{ row }">
                <span class="table-version">v{{ row.version || "1.0.0" }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <div class="table-actions">
                  <el-button size="small" type="primary" @click="handleRestore(row)">还原</el-button>
                  <el-button size="small" type="danger" plain @click="handlePermanentDelete(row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="pagination.current"
            v-model:page-size="pagination.size"
            :total="pagination.total"
            :page-sizes="[12, 24, 48, 96]"
            layout="sizes, prev, pager, next, jumper, total"
            @size-change="handleSizeChangeUninstalled"
            @current-change="handleCurrentChangeUninstalled"
          />
        </div>
        </div>
      </template>

      <!-- 其他 Tab -->
      <template v-if="activeTab === 'distribute'">
        <div class="deploy-tab" style="overflow:hidden;">
          <!-- 左侧：说明区 -->
          <div class="deploy-left">
            <div class="deploy-upload-panel" style="justify-content: center; gap: 16px;">
              <div class="upload-panel-icon">
                <svg viewBox="0 0 120 120" fill="none" xmlns="http://www.w3.org/2000/svg" style="width:72px;height:72px;">
                  <rect x="18" y="42" width="84" height="62" rx="8" fill="#e8f4fd" stroke="#409eff" stroke-width="2.5"/>
                  <rect x="30" y="52" width="60" height="6" rx="3" fill="#409eff" opacity="0.5"/>
                  <rect x="30" y="64" width="44" height="4" rx="2" fill="#409eff" opacity="0.3"/>
                  <rect x="30" y="72" width="52" height="4" rx="2" fill="#409eff" opacity="0.3"/>
                  <path d="M46 20l28 22H18L46 20z" fill="#ecf5ff" stroke="#409eff" stroke-width="2.5" stroke-linejoin="round"/>
                  <circle cx="64" cy="31" r="4" fill="#409eff"/>
                  <path d="M36 42V28" stroke="#409eff" stroke-width="2.5" stroke-linecap="round"/>
                  <path d="M84 42V28" stroke="#409eff" stroke-width="2.5" stroke-linecap="round"/>
                  <path d="M56 96l-8-8m0 0l-8 8m8-8v16" stroke="#67c23a" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>
              <h3 class="upload-panel-title">应用分发</h3>
              <p class="upload-panel-desc" style="text-align:center;max-width:260px;">
                选择一个应用，打包为 <code style="background:#e8f4fd;padding:1px 6px;border-radius:4px;color:#409eff;font-weight:600;">.sap</code> 分发文件进行部署
              </p>
            </div>
          </div>
          <!-- 右侧：操作区 -->
          <div class="deploy-right">
            <div class="deploy-list-header">
              <span class="deploy-list-title">打包</span>
              <div class="deploy-list-actions">
                <el-button size="small" :icon="Refresh" circle @click="loadDistributeList" />
              </div>
            </div>
            <div style="flex:1;padding:20px 20px 10px;display:flex;flex-direction:column;gap:20px;overflow-y:auto;">
              <!-- 选择 + 打包 -->
              <div style="display:flex;align-items:center;gap:12px;">
                <el-select
                  v-model="selectedAppId"
                  filterable
                  placeholder="搜索并选择应用..."
                  value-key="id"
                  class="distribute-select"
                  style="flex:1;"
                  :disabled="distributing"
                >
                  <template #prefix>
                    <el-icon :size="16" color="#909399"><Search /></el-icon>
                  </template>
                  <el-option
                    v-for="app in appOptions"
                    :key="app.id"
                    :label="`${app.appName} (${app.appId})`"
                    :value="app.id"
                  >
                    <div class="select-option">
                      <div class="select-option-main">
                        <span class="select-option-name">{{ app.appName }}</span>
                        <el-tag :type="getStatusTagType(app.status)" size="small" effect="light" class="select-option-tag">{{ getStatusText(app.status) }}</el-tag>
                      </div>
                      <div class="select-option-sub">
                        <span class="select-option-id">{{ app.appId }}</span>
                        <span class="select-option-version">v{{ app.version || "1.0.0" }}</span>
                      </div>
                    </div>
                  </el-option>
                </el-select>
                <el-button
                  type="primary"
                  :loading="distributing"
                  :disabled="!selectedAppId"
                  :icon="FolderOpened"
                  @click="handlePackDownload"
                >
                  {{ distributing ? '正在打包...' : '打包下载' }}
                </el-button>
              </div>

              <!-- 选项 + 重打 -->
              <div v-if="distributeResult" style="display:flex;align-items:center;justify-content:space-between;padding-top:16px;border-top:1px dashed #e5e6eb;">
                <el-checkbox v-model="includeData" class="include-data-check">
                  <span style="font-size:13px;">包含应用数据</span>
                </el-checkbox>
                <el-button size="small" plain @click="handleRepack">重新打包</el-button>
              </div>

              <!-- 打包结果 -->
              <Transition name="distribute-result">
                <div v-if="distributeResult" class="distribute-result-card" style="margin:0;box-shadow:0 2px 12px rgba(0,0,0,0.06),0 0 0 1px rgba(0,0,0,0.04);">
                  <div class="result-icon-wrap">
                    <div class="result-icon-inner">
                      <el-icon :size="28" color="#fff"><CircleCheck /></el-icon>
                    </div>
                  </div>
                  <div class="result-content">
                    <div class="result-title">打包完毕</div>
                    <div class="result-meta">
                      <span class="result-app-id">{{ distributeResult.appId }}</span>
                      <span class="result-version-badge">{{ distributeResult.newVersion }}</span>
                      <span class="result-size">{{ formatFileSize(distributeResult.fileSize) }}</span>
                    </div>
                    <el-button type="success" size="large" class="download-btn" @click="handleDownload">
                      <el-icon style="margin-right:6px;"><FolderOpened /></el-icon>
                      下载到本地
                    </el-button>
                  </div>
                </div>
              </Transition>
            </div>
          </div>
        </div>
      </template>

      <!-- 应用部署 -->
      <template v-if="activeTab === 'deploy'">
        <div class="deploy-tab">
          <!-- 左侧：上传区 -->
          <div class="deploy-left">
            <div class="deploy-upload-panel">
              <div class="upload-panel-icon">
                <el-icon :size="48" color="#409eff"><UploadFilled /></el-icon>
              </div>
              <h3 class="upload-panel-title">上传应用包</h3>
              <p class="upload-panel-desc">选择 .sap 文件上传到暂存区</p>
              <div
                class="upload-drop-zone"
                :class="{ 'is-dragover': isDragOver }"
                @dragover.prevent="isDragOver = true"
                @dragleave.prevent="isDragOver = false"
                @drop.prevent="handleDrop"
                @click="triggerUpload"
              >
                <el-icon :size="32" color="#c0c4cc"><UploadFilled /></el-icon>
                <p>拖拽文件到此处</p>
                <span class="upload-hint">或点击选择文件（支持多选）</span>
              </div>
              <el-button
                type="primary"
                :loading="deploying"
                :disabled="pendingFiles.length === 0"
                class="upload-submit-btn"
                @click="handleUploadSubmit"
              >
                {{ deploying ? "上传中..." : "上传到暂存区" }}
              </el-button>
              <div v-if="pendingFiles.length > 0" class="upload-file-list">
                <div
                  v-for="pf in pendingFiles"
                  :key="pf.id"
                  class="upload-file-item"
                >
                  <el-icon :size="16" color="#409eff"><Document /></el-icon>
                  <span class="upload-file-name">{{ pf.file.name }}</span>
                  <el-button
                    size="small"
                    circle
                    text
                    :icon="Close"
                    class="file-remove-btn"
                    @click="removePendingFile(pf.id)"
                  />
                </div>
              </div>
            </div>
          </div>
          <!-- 右侧：暂存列表 -->
          <div class="deploy-right">
            <div class="deploy-list-header">
              <span class="deploy-list-title">暂存区</span>
              <el-tag size="small" type="info" effect="plain">{{ deployPackages.length }} 个</el-tag>
              <div class="deploy-list-actions">
                <el-button size="small" :icon="Refresh" circle @click="loadDeployPackages" />
              </div>
            </div>
            <div v-if="deployPackages.length === 0" class="deploy-list-empty">
              <el-icon :size="48" color="#d1d5db"><Document /></el-icon>
              <p>暂无暂存包</p>
            </div>
            <div v-else class="deploy-list-scroll">
              <div v-for="pkg in deployPackages" :key="pkg.appId" class="deploy-card">
                <div class="deploy-card-avatar" :style="{ background: getAvatarColor(pkg.appId) }">
                  {{ getAppInitial(pkg.appName) }}
                </div>
                <div class="deploy-card-body">
                  <div class="deploy-card-top">
                    <span class="deploy-card-name">{{ pkg.appName }}</span>
                    <span class="deploy-card-version">v{{ pkg.version || "1.0.0" }}</span>
                  </div>
                  <div class="deploy-card-meta">
                    <span class="deploy-card-id">{{ pkg.appId }}</span>
                    <span class="deploy-card-size">{{ formatFileSize(pkg.fileSize) }}</span>
                  </div>
                </div>
                <div class="deploy-card-actions">
                  <el-button size="small" type="primary" @click="handleDeploySingle(pkg)">部署</el-button>
                  <el-button size="small" type="danger" plain @click="handleDeleteStaged(pkg)">删除</el-button>
                </div>
              </div>
            </div>
          </div>
          <input ref="fileInputRef" type="file" accept=".sap" multiple style="display:none" @change="handleFileChange" />
        </div>
      </template>

      <div v-if="activeTab !== 'management' && activeTab !== 'uninstall' && activeTab !== 'distribute' && activeTab !== 'deploy'" class="placeholder-tab">
        <el-empty :description="`「${getTabLabel(activeTab)}」功能开发中...`" />
      </div>
    </div>

    <AppFormDialog ref="appFormDialogRef" @success="handleFormSuccess" />

  </div>
</template>

<script setup lang="ts">
defineOptions({ name: "ApplicationDev" });

import { ref, reactive, watch, computed, onActivated } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  Search,
  Refresh,
  Plus,
  EditPen,
  Close,
  ArrowDown,
  Monitor,
  CircleCheck,
  FolderOpened,
  Grid,
  List,
  UploadFilled,
  Document
} from "@element-plus/icons-vue";
import * as ElementPlusIcons from "@element-plus/icons-vue";
import {
  getApplicationList,
  deleteApplication,
  startApplication,
  stopApplication,
  restartApplication,
  uninstallApplication,
  restoreApplication,
  distributeApplication,
  downloadDistributeFile,
  uploadDeployPackage,
  getStagedPackages,
  deployStagedPackage,
  deleteStagedPackage
} from "@/api/application";
import type { DistributeResult } from "@/api/application";
import AppFormDialog from "./components/AppFormDialog.vue";
import dayjs from "dayjs";

const loading = ref(false);
const activeTab = ref("management");
const viewMode = ref<"card" | "table">("card");
const searchKeyword = ref("");
const applicationList = ref<any[]>([]);
const pagination = reactive({ current: 1, size: 24, total: 0 });
const appFormDialogRef = ref();

// 分发相关
const selectedAppId = ref<number | null>(null);
const appOptions = ref<any[]>([]);
const includeData = ref(false);
const distributing = ref(false);
const distributeResult = ref<DistributeResult | null>(null);

// 部署相关
const deployPackages = ref<any[]>([]);
const isDragOver = ref(false);
const fileInputRef = ref<HTMLInputElement>();
const deploying = ref(false);
const pendingFiles = ref<{ file: File; id: number }[]>([]);
let fileIdCounter = 0;

const loadApplicationList = async (status?: number) => {
  loading.value = true;
  try {
    const r = await getApplicationList({
      current: pagination.current,
      size: pagination.size,
      appName: searchKeyword.value || undefined,
      status
    });
    const records = (r?.data?.records || []).slice();
    // 管理 tab 排除已卸载的应用
    if ((status === undefined || status === null) && records.length) {
      applicationList.value = records.filter((app: any) => app.status !== 4);
    } else {
      applicationList.value = records;
    }
    pagination.total = r?.data?.total || 0;
  } catch (e: any) {
    ElMessage.error(e.message || "加载失败");
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => { pagination.current = 1; loadApplicationList(); };
const handleSizeChange = (s: number) => { pagination.size = s; pagination.current = 1; loadApplicationList(); };
const handleCurrentChange = (c: number) => { pagination.current = c; loadApplicationList(); };
const handleCreate = () => { appFormDialogRef.value?.open(); };

const handleStart = async (row: any) => {
  try { await startApplication(row.id); ElMessage.success("启动成功"); loadApplicationList(); }
  catch (e: any) { ElMessage.error(e.message || "启动失败"); }
};
const handleStop = async (row: any) => {
  try { await stopApplication(row.id); ElMessage.success("已停止"); loadApplicationList(); }
  catch (e: any) { ElMessage.error(e.message || "停止失败"); }
};
const handleRestart = async (row: any) => {
  try { await restartApplication(row.id); ElMessage.success("重启成功"); loadApplicationList(); }
  catch (e: any) { ElMessage.error(e.message || "重启失败"); }
};

const handleUninstall = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定卸载"${row.appName}"？`, "提示", { type: "warning" });
    await uninstallApplication(row.id);
    ElMessage.success("卸载成功");
    loadApplicationList();
  } catch (e: any) {
    if (e !== "cancel") ElMessage.error(e.message || "卸载失败");
  }
};

const handleTableCommand = async (cmd: string, row: any) => {
  switch (cmd) {
    case "restart":
      await handleRestart(row);
      break;
    case "edit":
      appFormDialogRef.value?.open(row);
      break;
    case "uninstall":
      await handleUninstall(row);
      break;
  }
};

const handleRestore = async (row: any) => {
  try { await restoreApplication(row.id); ElMessage.success("还原成功"); loadUninstalledList(); }
  catch (e: any) { ElMessage.error(e.message || "还原失败"); }
};

const handlePermanentDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(`永久删除"${row.appName}"？不可恢复。`, "警告", { type: "warning" });
    await deleteApplication(row.id);
    ElMessage.success("已删除");
    await loadUninstalledList();
  } catch (e: any) {
    if (e !== "cancel") ElMessage.error(e.message || "删除失败");
  }
};

const loadUninstalledList = async () => { pagination.current = 1; await loadApplicationList(4); };
const handleSearchUninstalled = () => { pagination.current = 1; loadApplicationList(4); };
const handleSizeChangeUninstalled = (s: number) => { pagination.size = s; pagination.current = 1; loadApplicationList(4); };
const handleCurrentChangeUninstalled = (c: number) => { pagination.current = c; loadApplicationList(4); };
const handleFormSuccess = () => { loadApplicationList(); };

// 部署功能（暂存区）
const loadDeployPackages = async () => {
  try {
    const data = await getStagedPackages();
    deployPackages.value = data || [];
  } catch (e: any) {
    console.error("加载暂存包列表失败", e);
  }
};

const triggerUpload = () => { fileInputRef.value?.click(); };

const handleFileChange = (e: Event) => {
  const input = e.target as HTMLInputElement;
  const files = input.files;
  if (!files || files.length === 0) return;

  const valid: File[] = [];
  for (let i = 0; i < files.length; i++) {
    const file = files[i];
    if (!file.name.endsWith(".sap")) {
      ElMessage.warning(`"${file.name}" 不是 .sap 格式，已跳过`);
      continue;
    }
    // 避免重复添加同名文件
    if (pendingFiles.value.some(p => p.file.name === file.name && p.file.size === file.size)) {
      continue;
    }
    valid.push(file);
  }

  for (const f of valid) {
    pendingFiles.value.push({ file: f, id: ++fileIdCounter });
  }

  input.value = "";
};

const removePendingFile = (id: number) => {
  pendingFiles.value = pendingFiles.value.filter(p => p.id !== id);
};

const handleUploadSubmit = async () => {
  if (pendingFiles.value.length === 0) return;
  deploying.value = true;
  let success = 0;
  let fail = 0;

  for (const { file } of pendingFiles.value) {
    try {
      await uploadDeployPackage(file);
      success++;
      ElMessage.success(`"${file.name}" 上传成功`);
    } catch (e: any) {
      fail++;
      ElMessage.error(`"${file.name}" 上传失败：${e.message || "未知错误"}`);
    }
  }

  pendingFiles.value = [];
  await loadDeployPackages();
  deploying.value = false;
};

const handleDrop = (e: DragEvent) => {
  isDragOver.value = false;
  const files = e.dataTransfer?.files;
  if (!files || files.length === 0) return;

  const valid: File[] = [];
  for (let i = 0; i < files.length; i++) {
    const file = files[i];
    if (!file.name.endsWith(".sap")) {
      ElMessage.warning(`"${file.name}" 不是 .sap 格式，已跳过`);
      continue;
    }
    if (pendingFiles.value.some(p => p.file.name === file.name && p.file.size === file.size)) {
      continue;
    }
    valid.push(file);
  }

  for (const f of valid) {
    pendingFiles.value.push({ file: f, id: ++fileIdCounter });
  }
};

const handleDeploySingle = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定部署"${row.appName}" (v${row.version})？`, "确认部署", { type: "info" });
    await deployStagedPackage(row.appId);
    ElMessage.success(`"${row.appName}" 部署成功`);
    await loadDeployPackages();
    await loadApplicationList();
  } catch (e: any) {
    if (e !== "cancel") ElMessage.error(e.message || "部署失败");
  }
};

const handleDeleteStaged = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定删除暂存包"${row.appName}"？`, "确认删除", { type: "warning" });
    await deleteStagedPackage(row.appId);
    ElMessage.success("已删除");
    await loadDeployPackages();
  } catch (e: any) {
    if (e !== "cancel") ElMessage.error(e.message || "删除失败");
  }
};

const formatFileSize = (bytes: string | number): string => {
  const n = typeof bytes === "string" ? parseInt(bytes) : bytes;
  if (!n || n < 1024) return (n || 0) + " B";
  if (n < 1024 * 1024) return (n / 1024).toFixed(1) + " KB";
  return (n / (1024 * 1024)).toFixed(2) + " MB";
};

// 分发功能
const loadDistributeList = async () => {
  try {
    const r = await getApplicationList({ current: 1, size: 200 });
    const records = (r?.data?.records || []).filter((app: any) => app.status !== 4);
    appOptions.value = records;
  } catch (e: any) {
    // silent
  }
};

const selectedApp = computed(() => {
  return appOptions.value.find((a: any) => a.id === selectedAppId.value) || null;
});

const handlePackDownload = async () => {
  if (!selectedAppId.value) return;
  distributing.value = true;
  distributeResult.value = null;

  try {
    const result = await distributeApplication(selectedAppId.value, includeData.value);
    distributeResult.value = result;
    ElMessage.success('打包完成');
  } catch (e: any) {
    ElMessage.error(e.message || '打包失败');
  } finally {
    distributing.value = false;
  }
};

const handleRepack = async () => {
  distributeResult.value = null;
  await handlePackDownload();
};

const handleDownload = async () => {
  if (!distributeResult.value) return;
  try {
    const blob = await downloadDistributeFile(
      distributeResult.value.appId,
      distributeResult.value.fileName
    );
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = distributeResult.value.fileName;
    a.click();
    URL.revokeObjectURL(url);
    ElMessage.success("下载完成");
  } catch (e: any) {
    ElMessage.error(e.message || "下载失败");
  }
};

// 切换 Tab 时自动刷新数据
watch(activeTab, (tab) => {
  searchKeyword.value = "";
  pagination.current = 1;
  if (tab === "uninstall") {
    loadUninstalledList();
  } else if (tab === "management") {
    loadApplicationList();
  } else if (tab === "distribute") {
    distributeResult.value = null;
    selectedAppId.value = null;
    loadDistributeList();
  } else if (tab === "deploy") {
    loadDeployPackages();
  }
});

const getStatusClass = (s: number) => {
  switch (s) { case 1: return "active"; case 2: return "stopped"; case 3: return "error"; case 4: return "uninstalled"; default: return "inactive"; }
};
const getStatusText = (s: number) => {
  switch (s) { case 0: return "未运行"; case 1: return "运行中"; case 2: return "已停止"; case 3: return "异常"; case 4: return "已卸载"; default: return "未知"; }
};
const getStatusTagType = (s: number) => {
  switch (s) { case 1: return "success"; case 2: return "warning"; case 3: return "danger"; case 4: return "info"; default: return "info"; }
};
const getRuntimeText = (row: any) => {
  if (row.startTime && row.status === 1) {
    const diff = dayjs().diff(dayjs(row.startTime));
    const d = Math.floor(diff / 86400000);
    const h = Math.floor((diff % 86400000) / 3600000);
    const m = Math.floor((diff % 3600000) / 60000);
    if (d > 0) return `${d}天 ${h}小时`;
    if (h > 0) return `${h}小时 ${m}分`;
    if (m > 0) return `${m}分钟`;
    return "刚刚启动";
  }
  return "-";
};
const getIconName = (icon: any): string => {
  if (!icon) return "Monitor";
  if (typeof icon === "string") return icon;
  if (typeof icon === "object" && icon.icon) return icon.icon;
  return "Monitor";
};
const getIconComponent = (name: string) => (ElementPlusIcons as any)[name] || Monitor;

const getTabLabel = (tab: string): string => {
  const map: Record<string, string> = {
    deploy: "应用部署", logs: "应用日志", team: "开发小组"
  };
  return map[tab] || tab;
};

const avatarColors = ["#409eff", "#67c23a", "#e6a23c", "#6366f1", "#14b8a6", "#f97316", "#8b5cf6", "#ec4899"];
const getAvatarColor = (appId: string): string => {
  let hash = 0;
  for (let i = 0; i < appId.length; i++) {
    hash = appId.charCodeAt(i) + ((hash << 5) - hash);
  }
  return avatarColors[Math.abs(hash) % avatarColors.length];
};
const getAppInitial = (name: string): string => {
  return (name || "?").charAt(0).toUpperCase();
};

onActivated(() => loadApplicationList());
</script>

<style scoped lang="scss">
.application-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.page-header {
  background: #fff;
  padding: 12px 28px 0;
  border-bottom: 1px solid #ebeef5;
  flex-shrink: 0;

  .page-title-row {
    display: flex;
    align-items: baseline;
    gap: 16px;
    padding-bottom: 8px;

    .page-title {
      font-size: 18px;
      font-weight: 700;
      color: #1d2129;
      letter-spacing: 0.5px;
    }

    .page-subtitle {
      font-size: 13px;
      color: #86909c;
    }
  }

  .main-tabs {
    :deep(.el-tabs__header) { margin: 0 0 -1px; }
    :deep(.el-tabs__item) {
      height: 48px; line-height: 48px; font-size: 14px; padding: 0 20px;
      transition: color 0.2s;

      &:hover { color: #409eff; }
      &.is-active { font-weight: 600; color: #1d2129; }
    }
    :deep(.el-tabs__active-bar) { height: 3px; border-radius: 3px 3px 0 0; }
    :deep(.el-tabs__nav-wrap::after) { display: none; }
  }
}

.page-body {
  flex: 1;
  padding: 24px 28px;
  overflow: auto;
  display: flex;
  flex-direction: column;
  background: #f7f8fa;
}

// Content shell
.content-shell {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

// Toolbar
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-shrink: 0;

  .toolbar-left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .toolbar-right {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .stat-badge {
    font-size: 13px;
    color: #86909c;
    white-space: nowrap;
  }

  .search-input {
    width: 280px;
    :deep(.el-input__wrapper) {
      border-radius: 8px;
      box-shadow: 0 0 0 1px #dcdfe6 inset;
      transition: all 0.2s;
      &:hover, &:focus-within { box-shadow: 0 0 0 1px #409eff inset; }
    }
  }

  .view-toggle {
    display: flex;
    border: 1px solid #dcdfe6;
    border-radius: 6px;
    overflow: hidden;
    flex-shrink: 0;

    .el-button {
      border: none;
      border-radius: 0;
      padding: 5px 12px;
      font-size: 13px;
      &:first-child { border-right: 1px solid #dcdfe6; }

      &:not(.el-button--primary) {
        color: #606266;
        &:hover { color: #409eff; background: #ecf5ff; }
      }
    }
  }
}

// App grid (card layout)
.app-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
  gap: 16px;
  align-content: start;
  min-height: 200px;
  padding: 2px;
}

// Empty state
.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

// Placeholder tab
.placeholder-tab {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

// App card
.app-card {
  background: #fff;
  border-radius: 12px;
  padding: 0;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  position: relative;
  overflow: hidden;

  &__bar {
    height: 3px;
    background: #c0c4cc;
    transition: background 0.3s;
  }

  &--active &__bar { background: #67c23a; }
  &--stopped &__bar { background: #e6a23c; }
  &--error &__bar { background: #f56c6c; }
  &--inactive &__bar { background: #909399; }
  &--uninstalled &__bar { background: #b0b8c4; }
  &:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
    transform: translateY(-2px);
  }

  &__top {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 18px 20px 0;
  }

  &__avatar {
    width: 40px;
    height: 40px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 18px;
    font-weight: 700;
    flex-shrink: 0;
  }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__name {
    font-size: 15px;
    font-weight: 600;
    color: #1d2129;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    margin-bottom: 2px;
  }

  &__id {
    font-size: 12px;
    color: #86909c;
    font-family: "SF Mono", "Cascadia Code", monospace;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__meta {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px 20px;

    .meta-version {
      font-size: 12px;
      color: #606266;
      font-family: "SF Mono", "Cascadia Code", monospace;
    }

    .meta-runtime {
      font-size: 13px;
      color: #606266;
      display: inline-flex;
      align-items: center;
      gap: 5px;
    }

    .runtime-dot {
      width: 6px; height: 6px;
      border-radius: 50%;
      background: #67c23a;
      display: inline-block;
      animation: pulse 2s infinite;
    }

    .meta-idle {
      font-size: 13px;
      color: #c0c4cc;
    }
  }

  @keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.4; }
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 0 20px 18px;
    margin-top: auto;

    .action-last {
      margin-left: auto;
    }
  }
}

// Pagination
.pagination-bar {
  display: flex;
  justify-content: flex-end;
  padding-top: 20px;
  flex-shrink: 0;
  margin-top: auto;
}

// Table view
.app-table-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 200px;

  :deep(.el-table) {
    border-radius: 10px;
    overflow: hidden;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);

    th { border-bottom: 2px solid #ebeef5; }
    td { border-bottom: 1px solid #f2f3f5; }

    .el-table__body tr:hover > td {
      background: #ecf5ff !important;
    }
  }
}

.table-app-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.table-app-avatar {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  flex-shrink: 0;
}

.table-app-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.table-app-name {
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
}

.table-app-id {
  font-size: 12px;
  color: #86909c;
  font-family: "SF Mono", "Cascadia Code", monospace;
}

.table-version {
  font-size: 12px;
  color: #409eff;
  background: #ecf5ff;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
  font-family: "SF Mono", "Cascadia Code", monospace;
}

.table-runtime {
  font-size: 13px;
  color: #606266;
  display: inline-flex;
  align-items: center;
  gap: 5px;

  &::before {
    content: "";
    width: 5px;
    height: 5px;
    border-radius: 50%;
    background: #67c23a;
    animation: runtimePulse 2s infinite;
  }

  &--idle {
    font-size: 13px;
    color: #c0c4cc;
  }
}

@keyframes runtimePulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}

// Distribute flow layout
// Distribute select style override
.distribute-select {
  :deep(.el-select__wrapper) {
    border-radius: 10px;
    padding: 6px 12px;
    transition: all 0.2s;
    &:hover { box-shadow: 0 0 0 1px #409eff inset; }
  }
}

// Select option content
.select-option {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;

  .select-option-main {
    display: flex;
    align-items: center;
    gap: 8px;

    .select-option-name {
      font-size: 14px;
      font-weight: 600;
      color: #1d2129;
    }

    .select-option-tag {
      flex-shrink: 0;
    }
  }

  .select-option-sub {
    display: flex;
    align-items: center;
    gap: 16px;

    .select-option-id {
      font-size: 12px;
      color: #86909c;
      font-family: "SF Mono", "Cascadia Code", monospace;
    }

    .select-option-version {
      font-size: 11px;
      color: #409eff;
      background: #ecf5ff;
      padding: 0 6px;
      border-radius: 3px;
      font-weight: 500;
    }
  }
}

// Options row (post-pack)
.distribute-options-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px dashed #e5e6eb;

  .include-data-check {
    :deep(.el-checkbox__label) { color: #606266; }
  }
}

// Result card with transition
.distribute-result-card {
  background: #fff;
  border-radius: 16px;
  padding: 36px 40px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06), 0 0 0 1px rgba(0, 0, 0, 0.04);
  display: flex;
  align-items: center;
  gap: 24px;
  position: relative;
  overflow: hidden;

  &::before {
    content: "";
    position: absolute;
    top: 0; left: 0; right: 0;
    height: 3px;
    background: linear-gradient(90deg, #67c23a 0%, #85ce61 100%);
    border-radius: 16px 16px 0 0;
  }

  .result-icon-wrap {
    flex-shrink: 0;

    .result-icon-inner {
      width: 56px;
      height: 56px;
      border-radius: 50%;
      background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 4px 16px rgba(103, 194, 58, 0.35);
    }
  }

  .result-content {
    flex: 1;
    min-width: 0;

    .result-title {
      font-size: 18px;
      font-weight: 700;
      color: #1d2129;
      margin-bottom: 8px;
    }

    .result-meta {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 20px;
      flex-wrap: wrap;

      .result-app-id {
        font-family: "SF Mono", "Cascadia Code", monospace;
        font-size: 13px;
        color: #4e5969;
        font-weight: 500;
      }

      .result-version-badge {
        font-size: 12px;
        font-weight: 600;
        color: #409eff;
        background: #ecf5ff;
        padding: 2px 10px;
        border-radius: 20px;
      }

      .result-size {
        font-size: 12px;
        color: #86909c;
        font-weight: 500;
      }
    }

    .download-btn {
      border-radius: 10px;
      font-size: 15px;
      font-weight: 600;
      padding: 0 28px;
      height: 44px;
    }
  }
}

// Transition
.distribute-result-enter-active {
  transition: all 0.45s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.distribute-result-leave-active {
  transition: all 0.2s ease;
}

.distribute-result-enter-from {
  opacity: 0;
  transform: translateY(20px) scale(0.96);
}

.distribute-result-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

// ── 应用部署 (Deploy) Tab: 左上传 / 右列表 ──
.deploy-tab {
  flex: 1;
  display: flex;
  gap: 24px;
  min-height: 0;
}

// 左侧上传面板
.deploy-left {
  width: 340px;
  flex-shrink: 0;
  display: flex;
}

.deploy-upload-panel {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  padding: 28px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  border: 1px solid #ebeef5;
}

.upload-panel-icon {
  margin-bottom: 16px;
}

.upload-panel-title {
  margin: 0 0 4px;
  font-size: 17px;
  font-weight: 600;
  color: #1d2129;
}

.upload-panel-desc {
  margin: 0 0 20px;
  font-size: 13px;
  color: #86909c;
}

.upload-drop-zone {
  width: 100%;
  padding: 28px 0;
  border: 2px dashed #dcdfe6;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.25s;
  background: #fafafa;

  p {
    margin: 0;
    font-size: 14px;
    color: #4e5969;
  }

  .upload-hint {
    font-size: 12px;
    color: #c9cdd4;
  }

  &:hover, &.is-dragover {
    border-color: #409eff;
    background: #ecf5ff;

    p, .upload-hint { color: #409eff; }
  }
}

.upload-file-list {
  width: 100%;
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.upload-file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 6px;
  background: #f7f8fa;
  transition: background 0.15s;

  &:hover {
    background: #f0f2f5;
  }

  .upload-file-name {
    flex: 1;
    font-size: 12px;
    color: #4e5969;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .file-remove-btn {
    flex-shrink: 0;
    --el-button-size: 20px;
    font-size: 12px;
    color: #c9cdd4;
    &:hover { color: #f56c6c; }
  }
}

.upload-submit-btn {
  width: 100%;
  margin-top: 16px;
  height: 40px;
  font-size: 14px;
}

// 右侧暂存列表
.deploy-right {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #ebeef5;
  overflow: hidden;
}

.deploy-list-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px;
  border-bottom: 1px solid #f2f3f5;

  .deploy-list-title {
    font-size: 15px;
    font-weight: 600;
    color: #1d2129;
  }

  .deploy-list-actions {
    margin-left: auto;
  }
}

.deploy-list-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #c9cdd4;

  p {
    margin: 0;
    font-size: 14px;
    color: #86909c;
  }
}

.deploy-list-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

// 暂存卡片
.deploy-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 10px;
  border: 1px solid #f2f3f5;
  transition: all 0.2s;

  &:hover {
    border-color: #e5e6eb;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  }
}

.deploy-card-avatar {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  flex-shrink: 0;
}

.deploy-card-body {
  flex: 1;
  min-width: 0;
}

.deploy-card-top {
  display: flex;
  align-items: baseline;
  gap: 8px;

  .deploy-card-name {
    font-size: 14px;
    font-weight: 500;
    color: #1d2129;
  }

  .deploy-card-version {
    font-size: 12px;
    color: #409eff;
    font-family: "SF Mono", "Cascadia Code", monospace;
    flex-shrink: 0;
  }
}

.deploy-card-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 2px;

  .deploy-card-id {
    font-size: 12px;
    color: #86909c;
    font-family: "SF Mono", "Cascadia Code", monospace;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .deploy-card-size {
    font-size: 11px;
    color: #c9cdd4;
    flex-shrink: 0;
  }
}

.deploy-card-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}
</style>
