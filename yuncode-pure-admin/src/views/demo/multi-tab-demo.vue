<template>
  <div class="multi-tab-demo">
    <!-- 标签栏 -->
    <div class="tab-bar">
      <div class="tab-list">
        <div
          v-for="tab in tabs"
          :key="tab.id"
          class="tab-item"
          :class="{ active: activeTab === tab.id }"
          @click="switchTab(tab.id)"
        >
          <el-icon :size="14"><component :is="tab.icon" /></el-icon>
          <span class="tab-title">{{ tab.title }}</span>
          <el-icon v-if="tabs.length > 1" class="tab-close" @click.stop="closeTab(tab.id)"><Close /></el-icon>
        </div>
      </div>
      <div class="tab-actions">
        <el-button size="small" circle @click="addTab" title="新建标签页">
          <el-icon><Plus /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="tab-content">
      <keep-alive>
        <component :is="currentComponent" :key="activeTab" />
      </keep-alive>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, shallowRef, markRaw } from "vue";
import { Close, Plus, HomeFilled, Setting, DataLine, Monitor, Document, Box } from "@element-plus/icons-vue";

interface Tab {
  id: string;
  title: string;
  icon: any;
  component: any;
}

const tabs = ref<Tab[]>([
  {
    id: "home",
    title: "首页概览",
    icon: markRaw(HomeFilled),
    component: markRaw({
      template: `
        <div style="padding:24px;">
          <h2 style="margin:0 0 16px;color:#1d2129;">首页概览</h2>
          <el-row :gutter="16">
            <el-col :span="6" v-for="i in 4" :key="i">
              <div style="background:#fff;border-radius:12px;padding:20px;box-shadow:0 1px 3px rgba(0,0,0,0.04);">
                <div style="font-size:24px;font-weight:700;color:#1d2129;">{{ [128, 12, 56, 89][i-1] }}</div>
                <div style="font-size:13px;color:#909399;margin-top:6px;">{{ ['在线用户','应用数','BO模型','表单数'][i-1] }}</div>
              </div>
            </el-col>
          </el-row>
        </div>
      `
    })
  },
  {
    id: "modeling",
    title: "业务建模",
    icon: markRaw(DataLine),
    component: markRaw({
      template: `
        <div style="padding:24px;">
          <h2 style="margin:0 0 16px;color:#1d2129;">业务建模</h2>
          <el-card>
            <div style="text-align:center;padding:40px;color:#909399;">BO 模型管理界面</div>
          </el-card>
        </div>
      `
    })
  },
  {
    id: "app-dev",
    title: "应用管理",
    icon: markRaw(Monitor),
    component: markRaw({
      template: `
        <div style="padding:24px;">
          <h2 style="margin:0 0 16px;color:#1d2129;">应用管理</h2>
          <el-card>
            <div style="text-align:center;padding:40px;color:#909399;">应用列表与部署管理</div>
          </el-card>
        </div>
      `
    })
  },
  {
    id: "settings",
    title: "系统设置",
    icon: markRaw(Setting),
    component: markRaw({
      template: `
        <div style="padding:24px;">
          <h2 style="margin:0 0 16px;color:#1d2129;">系统设置</h2>
          <el-card>
            <div style="text-align:center;padding:40px;color:#909399;">基础配置、安全策略、通知设置</div>
          </el-card>
        </div>
      `
    })
  }
]);
const activeTab = ref("home");

const currentComponent = computed(() => {
  const tab = tabs.value.find(t => t.id === activeTab.value);
  return tab?.component;
});

function switchTab(id: string) {
  activeTab.value = id;
}

function closeTab(id: string) {
  if (tabs.value.length <= 1) return;
  const idx = tabs.value.findIndex(t => t.id === id);
  tabs.value.splice(idx, 1);
  if (activeTab.value === id) {
    activeTab.value = tabs.value[Math.min(idx, tabs.value.length - 1)].id;
  }
}

let tabCount = 4;
function addTab() {
  tabCount++;
  const id = `custom_${Date.now()}`;
  tabs.value.push({
    id,
    title: `自定义页面 ${tabCount}`,
    icon: markRaw(Document),
    component: markRaw({
      template: `<div style="padding:24px;"><h2 style="margin:0 0 16px;color:#1d2129;">自定义页面 ${tabCount}</h2><el-card><div style="text-align:center;padding:40px;color:#909399;">这是通过标签栏新建的页面</div></el-card></div>`
    })
  });
  activeTab.value = id;
}
</script>

<style scoped>
.multi-tab-demo {
  height: calc(100vh - 110px);
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.tab-bar {
  display: flex;
  align-items: center;
  padding: 0 12px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  height: 40px;
  gap: 4px;
}

.tab-list {
  display: flex;
  align-items: center;
  gap: 2px;
  flex: 1;
  overflow-x: auto;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  color: #606266;
  transition: all 0.15s;
  user-select: none;
  white-space: nowrap;
}

.tab-item:hover {
  background: #f5f7fa;
}

.tab-item.active {
  background: #ecf5ff;
  color: #409eff;
  font-weight: 500;
}

.tab-title {
  font-size: 13px;
}

.tab-close {
  font-size: 12px;
  color: #909399;
  border-radius: 50%;
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.15s;
}

.tab-item:hover .tab-close {
  opacity: 1;
}

.tab-close:hover {
  background: #f56c6c;
  color: #fff;
}

.tab-actions {
  flex-shrink: 0;
  padding-left: 8px;
}

.tab-content {
  flex: 1;
  overflow: auto;
  padding: 0;
}
</style>
