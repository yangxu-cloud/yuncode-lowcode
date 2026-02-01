<template>
  <div class="org-container">
    <el-card class="org-card">
      <!-- 页面头部 -->
      <template #header>
        <div class="card-header">
          <span>{{ $t('routes.org') }}</span>
        </div>
      </template>

      <!-- 主体内容：左右布局 -->
      <div class="org-content">
        <!-- 左侧：组织树 -->
        <div class="org-tree-panel">
          <!-- 搜索框 -->
          <div class="tree-search">
            <el-input
              v-model="searchKeyword"
              :placeholder="$t('org.searchPlaceholder')"
              clearable
              @clear="handleSearchClear"
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>
          <el-tree
            ref="treeRef"
            v-loading="treeLoading"
            :data="orgTreeData"
            :props="treeProps"
            :expand-on-click-node="false"
            :highlight-current="true"
            node-key="id"
            :default-expanded-keys="defaultExpandedKeys"
            draggable
            :allow-drop="allowDrop"
            @node-click="handleNodeClick"
            @node-drop="handleNodeDrop"
          >
            <template #default="{ node, data }">
              <div class="custom-tree-node">
                <div class="node-content">
                  <!-- 根节点使用文件夹图标 -->
                  <el-icon
                    v-if="data.nodeType === 'org' && data.orgType === 0"
                    class="node-icon"
                  >
                    <Folder />
                  </el-icon>
                  <!-- 公司节点使用地球图标 -->
                  <el-icon
                    v-else-if="data.nodeType === 'org' && data.orgType === 1"
                    class="node-icon"
                  >
                    <Location />
                  </el-icon>
                  <!-- 部门节点使用办公楼图标 -->
                  <el-icon
                    v-else-if="data.nodeType === 'org'"
                    class="node-icon"
                  >
                    <OfficeBuilding />
                  </el-icon>
                  <!-- 用户节点使用用户图标 -->
                  <el-icon v-else class="node-icon">
                    <User />
                  </el-icon>
                  <span class="node-label">{{ node.label }}</span>
                </div>

                <!-- 操作按钮 -->
                <div class="node-actions">
                  <!-- 组织节点的操作 -->
                  <template v-if="data.nodeType === 'org'">
                    <el-dropdown @command="(command) => handleAction(command, data)">
                      <el-icon class="action-icon">
                        <MoreFilled />
                      </el-icon>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item command="addSubOrg">
                            <el-icon><Plus /></el-icon>
                            {{ $t('org.addSubOrg') }}
                          </el-dropdown-item>
                          <!-- 根节点不能添加人员 -->
                          <el-dropdown-item
                            v-if="data.orgType !== 0"
                            command="addUser"
                          >
                            <el-icon><User /></el-icon>
                            {{ $t('org.addUser') }}
                          </el-dropdown-item>
                          <!-- 根节点不能编辑和删除 -->
                          <el-dropdown-item
                            v-if="data.orgType !== 0"
                            command="edit"
                            divided
                          >
                            <el-icon><Edit /></el-icon>
                            {{ $t('common.edit') }}
                          </el-dropdown-item>
                          <el-dropdown-item
                            v-if="data.orgType !== 0"
                            command="delete"
                          >
                            <el-icon><Delete /></el-icon>
                            {{ $t('common.delete') }}
                          </el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </template>

                  <!-- 用户节点的操作 -->
                  <template v-else>
                    <el-dropdown @command="(command) => handleAction(command, data)">
                      <el-icon class="action-icon">
                        <MoreFilled />
                      </el-icon>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item command="viewUser">
                            <el-icon><View /></el-icon>
                            {{ $t('org.viewUser') }}
                          </el-dropdown-item>
                          <el-dropdown-item command="removeUser">
                            <el-icon><Remove /></el-icon>
                            {{ $t('org.removeUser') }}
                          </el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </template>
                </div>
              </div>
            </template>
          </el-tree>
        </div>

        <!-- 右侧：详情面板 -->
        <div class="org-detail-panel">
          <el-empty v-if="!selectedNode" :description="$t('org.selectNode')" />
          <div v-else class="detail-content">
            <!-- 组织节点的Tab -->
            <el-tabs v-if="selectedNode.nodeType === 'org'" v-model="activeOrgTab" class="detail-tabs">
              <!-- Tab1: 基础信息（直接子级） -->
              <el-tab-pane :label="$t('org.basicInfo')" name="basic">
                <div class="tab-header">
                  <el-dropdown
                    split-button
                    type="primary"
                    size="small"
                    @click="handleAddFromTab"
                    @command="handleAddFromTabDropdown"
                  >
                    {{ $t('common.add') }}
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="addSubOrg">
                          <el-icon><OfficeBuilding /></el-icon>
                          {{ $t('org.addSubOrg') }}
                        </el-dropdown-item>
                        <el-dropdown-item command="addUser">
                          <el-icon><User /></el-icon>
                          {{ $t('org.addUser') }}
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
                <el-table :data="directChildren" stripe style="width: 100%">
                  <el-table-column prop="label" :label="$t('org.name')" min-width="200">
                    <template #default="{ row }">
                      <div style="display: flex; align-items: center; gap: 6px">
                        <el-icon v-if="row.nodeType === 'org' && row.orgType === 0" color="#409eff">
                          <Folder />
                        </el-icon>
                        <el-icon v-else-if="row.nodeType === 'org' && row.orgType === 1" color="#67c23a">
                          <Location />
                        </el-icon>
                        <el-icon v-else-if="row.nodeType === 'org'" color="#909399">
                          <OfficeBuilding />
                        </el-icon>
                        <el-icon v-else color="#e6a23c">
                          <User />
                        </el-icon>
                        <span>{{ row.label }}</span>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column
                    v-if="selectedNode.orgType === 0"
                    prop="orgCode"
                    :label="$t('org.orgCode')"
                    width="140"
                  >
                    <template #default="{ row }">
                      {{ row.nodeType === 'org' ? row.orgCode : '-' }}
                    </template>
                  </el-table-column>
                  <el-table-column
                    v-if="selectedNode.orgType === 0"
                    :label="$t('org.type')"
                    width="110"
                  >
                    <template #default="{ row }">
                      <el-tag v-if="row.nodeType === 'org' && row.orgType === 0" size="small" type="info">
                        {{ $t('org.root') }}
                      </el-tag>
                      <el-tag v-else-if="row.nodeType === 'org' && row.orgType === 1" size="small" type="success">
                        {{ $t('org.company') }}
                      </el-tag>
                      <el-tag v-else-if="row.nodeType === 'org'" size="small">
                        {{ $t('org.department') }}
                      </el-tag>
                      <el-tag v-else size="small" type="warning">
                        {{ $t('org.person') }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column
                    v-if="selectedNode.orgType === 0"
                    prop="sortOrder"
                    :label="$t('org.sortOrder')"
                    width="90"
                    align="center"
                  />
                  <el-table-column :label="$t('common.operation')" width="180" align="center">
                    <template #default="{ row }">
                      <el-button link type="primary" size="small" @click="handleEditFromTab(row)">
                        {{ $t('common.edit') }}
                      </el-button>
                      <el-button link type="danger" size="small" @click="handleDeleteFromTab(row)">
                        {{ $t('common.delete') }}
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>

              <!-- Tab2: 所有人员信息 -->
              <el-tab-pane :label="$t('org.allPersonnel')" name="personnel">
                <el-table :data="allPersonnel" stripe style="width: 100%">
                  <el-table-column prop="label" :label="$t('org.name')" width="180">
                    <template #default="{ row }">
                      <div style="display: flex; align-items: center; gap: 6px">
                        <el-icon color="#e6a23c">
                          <User />
                        </el-icon>
                        <span>{{ row.label }}</span>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column prop="username" :label="$t('org.username')" width="130" />
                  <el-table-column prop="nickname" :label="$t('org.nickname')" width="130" />
                  <el-table-column :label="$t('org.isLeader')" width="100" align="center">
                    <template #default="{ row }">
                      <el-tag v-if="row.isLeader === 1" size="small" type="success">
                        {{ $t('org.leader') }}
                      </el-tag>
                      <el-tag v-else size="small">{{ $t('org.member') }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="path" :label="$t('org.belongPath')" min-width="200" />
                </el-table>
              </el-tab-pane>
            </el-tabs>

            <!-- 用户节点的Tab -->
            <el-tabs v-else v-model="activeUserTab" class="detail-tabs">
              <!-- Tab1: 人员基础信息 -->
              <el-tab-pane :label="$t('org.basicInfo')" name="basic">
                <div class="user-info-actions">
                  <el-button type="primary" size="small" @click="handleEditUser">
                    <el-icon><Edit /></el-icon>
                    {{ $t('common.edit') }}
                  </el-button>
                  <el-button
                    :type="selectedUserFrozen ? 'success' : 'warning'"
                    size="small"
                    @click="handleToggleFreeze"
                  >
                    <el-icon><Lock /></el-icon>
                    {{ selectedUserFrozen ? $t('org.unfreeze') : $t('org.freeze') }}
                  </el-button>
                  <el-button type="danger" size="small" @click="handleDeleteUser">
                    <el-icon><Delete /></el-icon>
                    {{ $t('common.delete') }}
                  </el-button>
                </div>
                <el-descriptions :column="2" border>
                  <el-descriptions-item label="用户名">
                    {{ selectedNode.username || "-" }}
                  </el-descriptions-item>
                  <el-descriptions-item label="真实姓名">
                    {{ selectedNode.realName || "-" }}
                  </el-descriptions-item>
                  <el-descriptions-item label="昵称">
                    {{ selectedNode.nickname || "-" }}
                  </el-descriptions-item>
                  <el-descriptions-item label="租户编码">
                    @{{ selectedNode.tenantCode || "-" }}
                  </el-descriptions-item>
                  <el-descriptions-item label="邮箱">
                    {{ selectedNode.email || "-" }}
                  </el-descriptions-item>
                  <el-descriptions-item label="手机号">
                    {{ selectedNode.phone || "-" }}
                  </el-descriptions-item>
                  <el-descriptions-item label="是否领导">
                    <el-tag v-if="selectedNode.isLeader === 1" type="success">
                      负责人
                    </el-tag>
                    <el-tag v-else type="info">成员</el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="状态">
                    <el-tag :type="selectedUserFrozen ? 'danger' : 'success'">
                      {{ selectedUserFrozen ? "已冻结" : "正常" }}
                    </el-tag>
                  </el-descriptions-item>
                </el-descriptions>
              </el-tab-pane>

              <!-- Tab2: 兼职部门信息 -->
              <el-tab-pane :label="$t('org.partTimeJobs')" name="parttime">
                <div class="parttime-header">
                  <el-button
                    type="primary"
                    size="small"
                    :icon="Plus"
                    @click="handleAddPartTimeJob"
                  >
                    添加兼职部门
                  </el-button>
                </div>
                <el-table
                  :data="userOrgList"
                  stripe
                  style="width: 100%; margin-top: 12px"
                >
                  <el-table-column prop="orgName" label="部门名称" width="180" />
                  <el-table-column prop="orgPath" label="部门路径" min-width="200" />
                  <el-table-column label="类型" width="100" align="center">
                    <template #default="{ row }">
                      <el-tag v-if="row.isMainDept === 1" type="success" size="small">
                        主部门
                      </el-tag>
                      <el-tag v-else type="info" size="small">兼职部门</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="负责人" width="100" align="center">
                    <template #default="{ row }">
                      <el-tag v-if="row.isLeader === 1" type="warning" size="small">
                        负责人
                      </el-tag>
                      <el-tag v-else type="info" size="small">成员</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="200" align="center">
                    <template #default="{ row }">
                      <el-button
                        v-if="row.isLeader !== 1"
                        link
                        type="primary"
                        size="small"
                        @click="handleSetLeader(row)"
                      >
                        设为负责人
                      </el-button>
                      <el-button
                        v-if="row.isLeader === 1"
                        link
                        type="warning"
                        size="small"
                        @click="handleCancelLeader(row)"
                      >
                        取消负责人
                      </el-button>
                      <el-button
                        v-if="row.isMainDept !== 1"
                        link
                        type="danger"
                        size="small"
                        @click="handleRemovePartTimeJob(row)"
                      >
                        移除
                      </el-button>
                      <span v-else style="color: #909399; font-size: 12px">主部门不可移除</span>
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>

              <!-- Tab3: 菜单权限 -->
              <el-tab-pane :label="$t('org.menuPermissions')" name="permissions">
                <el-empty :description="$t('org.noPermissions')" />
              </el-tab-pane>
            </el-tabs>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 添加/编辑组织抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="drawerTitle"
      size="500px"
      @close="handleDrawerClose"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
      >
        <el-form-item :label="$t('org.orgName')" prop="orgName">
          <el-input
            v-model="formData.orgName"
            :placeholder="$t('org.inputOrgName')"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <el-form-item :label="$t('org.orgCode')" prop="orgCode">
          <el-input
            v-model="formData.orgCode"
            :placeholder="$t('org.inputOrgCode')"
            maxlength="50"
            show-word-limit
            :disabled="isOrgEditMode"
          >
            <template v-if="!isOrgEditMode" #append>
              <el-button :icon="Refresh" @click="generateOrgCode" />
            </template>
          </el-input>
          <div
            v-if="isOrgEditMode"
            style="color: #909399; font-size: 12px; margin-top: 4px"
          >
            组织编码不可编辑
          </div>
        </el-form-item>

        <!-- 租户编码（仅公司节点显示） -->
        <el-form-item
          v-if="formData.orgType === 1"
          label="租户编码"
          prop="tenantCode"
        >
          <el-input
            v-model="formData.tenantCode"
            placeholder="自动生成或手动输入，如：tencent"
            maxlength="50"
            show-word-limit
            :disabled="isOrgEditMode || !isTopLevelCompany"
            @input="
              isOrgEditMode || !isTopLevelCompany
                ? null
                : handleTenantCodeInput($event)
            "
          >
            <template #prepend>@</template>
            <template v-if="!isOrgEditMode && isTopLevelCompany" #append>
              <el-button :icon="Refresh" @click="generateTenantCode" />
            </template>
          </el-input>
          <div style="color: #909399; font-size: 12px; margin-top: 4px">
            {{
              isTopLevelCompany
                ? "租户编码全局唯一，用于用户登录时识别租户（只能包含小写字母、数字、下划线）"
                : "继承父公司的租户编码"
            }}
          </div>
        </el-form-item>

        <el-form-item :label="$t('org.parentOrg')" prop="parentId">
          <el-cascader
            v-model="parentOrgPath"
            :options="orgCascadeOptions"
            :props="cascaderProps"
            :placeholder="$t('org.selectParentOrg')"
            clearable
            change-on-select
            @change="handleParentChange"
          />
        </el-form-item>

        <el-form-item :label="$t('org.orgType')" prop="orgType">
          <el-radio-group v-model="formData.orgType" :disabled="isParentDepartment">
            <el-radio :label="1">{{ $t('org.company') }}</el-radio>
            <el-radio :label="2">{{ $t('org.department') }}</el-radio>
          </el-radio-group>
          <el-text
            v-if="isParentDepartment"
            type="info"
            size="small"
            style="margin-left: 12px"
          >
            部门下只能添加子部门
          </el-text>
        </el-form-item>

        <!-- 租户配置（仅公司节点显示） -->
        <template v-if="formData.orgType === 1">
          <el-divider content-position="left">租户配置</el-divider>

          <el-form-item label="租户类型">
            <el-radio-group v-model="formData.tenantConfig.tenantType">
              <el-radio :value="0">试用版</el-radio>
              <el-radio :value="1">标准版</el-radio>
              <el-radio :value="2">高级版</el-radio>
              <el-radio :value="3">企业版</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="用户数限制">
            <el-input-number
              v-model="formData.tenantConfig.userLimit"
              :min="1"
              :max="10000"
              :step="10"
            />
            <span style="margin-left: 8px; color: #909399">人</span>
          </el-form-item>

          <el-form-item label="存储空间限制">
            <el-input-number
              v-model="formData.tenantConfig.storageLimit"
              :min="1024"
              :max="102400"
              :step="1024"
            />
            <span style="margin-left: 8px; color: #909399">MB ({{ Math.floor((formData.tenantConfig.storageLimit || 10240) / 1024) }}GB)</span>
          </el-form-item>

          <el-form-item label="过期时间">
            <el-date-picker
              v-model="formData.tenantConfig.expireTime"
              type="date"
              placeholder="选择过期日期"
              style="width: 100%"
              :disabled-date="(time) => time.getTime() < Date.now() - 24 * 60 * 60 * 1000"
            />
          </el-form-item>
        </template>

        <el-form-item :label="$t('org.status')" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :label="1">{{ $t('org.enabled') }}</el-radio>
            <el-radio :label="0">{{ $t('org.disabled') }}</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item :label="$t('org.remark')" prop="remark">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            :placeholder="$t('org.inputRemark')"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="drawerVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
          {{ $t('common.save') }}
        </el-button>
      </template>
    </el-drawer>

    <!-- 添加人员抽屉 -->
    <el-drawer
      v-model="addUserDialogVisible"
      :title="isOrgEditMode ? '编辑人员' : '添加人员'"
      direction="rtl"
      size="600px"
      @close="handleAddUserDialogClose"
    >
      <el-form
        ref="addUserFormRef"
        :model="addUserForm"
        :rules="addUserFormRules"
        label-width="120px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="addUserForm.username"
            placeholder="请输入用户名"
            maxlength="50"
            show-word-limit
            :disabled="isOrgEditMode"
          />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input
            v-model="addUserForm.realName"
            placeholder="请输入真实姓名"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input
            v-model="addUserForm.nickname"
            placeholder="请输入昵称（可选）"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input
            v-model="addUserForm.email"
            placeholder="请输入邮箱"
            maxlength="100"
          />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input
            v-model="addUserForm.phone"
            placeholder="请输入手机号"
            maxlength="20"
          />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="addUserForm.gender">
            <el-radio :value="0">男</el-radio>
            <el-radio :value="1">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="设为负责人">
          <el-switch
            v-model="addUserForm.isLeader"
            :active-value="1"
            :inactive-value="0"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div style="flex: auto">
          <el-button @click="addUserDialogVisible = false">取消</el-button>
          <el-button
            type="primary"
            :loading="addUserLoading"
            @click="handleAddUserSubmit"
          >
            确定
          </el-button>
        </div>
      </template>
    </el-drawer>

    <!-- 添加兼职部门选择器 -->
    <DeptSelector
      ref="deptSelectorRef"
      v-model="selectedPartTimeDepts"
      :multiple="false"
      :tenant-id="selectedUserTenantId"
      :exclude-ids="selectedUserOrgIds"
      title="添加兼职部门"
      @change="handleDeptSelected"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import {
  Search,
  OfficeBuilding,
  User,
  Plus,
  Edit,
  Delete,
  MoreFilled,
  View,
  Remove,
  Folder,
  Location,
  Lock,
  Refresh
} from "@element-plus/icons-vue";
import DeptSelector from "@/components/DeptSelector.vue";
import {
  getOrgTree,
  addOrg,
  updateOrg,
  deleteOrg,
  addUserToOrg,
  removeUserFromOrg,
  setUserAsLeader,
  getUserOrgs,
  checkOrgCodeExists,
  type OrgTreeNode,
  type OrgForm,
  type UserOrgVO
} from "@/api/org-adapter";
import { createUser, updateUser, updateUserStatus } from "@/api/user";

const { t } = useI18n();

// 树形数据
const treeRef = ref();
const treeLoading = ref(false);
const orgTreeData = ref<OrgTreeNode[]>([]);
const treeProps = {
  children: "children",
  label: "label"
};
// 默认展开的节点（动态设置）
const defaultExpandedKeys = ref<number[]>([]);

// 搜索
const searchKeyword = ref("");

// 选中的节点
const selectedNode = ref<OrgTreeNode | null>(null);

// Tab激活状态
const activeOrgTab = ref("basic");
const activeUserTab = ref("basic");

// 直接子级数据（部门+人员）
const directChildren = computed(() => {
  if (!selectedNode.value || !selectedNode.value.children) {
    return [];
  }
  return selectedNode.value.children;
});

// 所有人员数据（包括子孙节点）
const allPersonnel = computed(() => {
  if (!selectedNode.value) {
    return [];
  }
  const personnel: OrgTreeNode[] = [];
  const collectPersonnel = (nodes: OrgTreeNode[], path: string = "") => {
    nodes.forEach(node => {
      const currentPath = path ? `${path} / ${node.label}` : node.label;
      if (node.nodeType === "user") {
        personnel.push({
          ...node,
          path: currentPath
        });
      }
      if (node.children) {
        collectPersonnel(node.children, currentPath);
      }
    });
  };

  if (selectedNode.value.children) {
    collectPersonnel(selectedNode.value.children, selectedNode.value.label);
  }

  return personnel;
});

// 用户冻结状态（根据用户状态计算：status=1 表示冻结）
const selectedUserFrozen = computed(() => {
  return selectedNode.value?.nodeType === "user" && selectedNode.value?.status === 1;
});

// 用户组织关系列表（兼职Tab使用）
const userOrgList = ref<any[]>([]);
const userOrgListLoading = ref(false);

// 部门选择器引用
const deptSelectorRef = ref();

// 选中的兼职部门ID
const selectedPartTimeDepts = ref<number>(0);

// 当前用户的租户ID和已加入的组织ID（用于过滤）
const selectedUserTenantId = computed(() => {
  return selectedNode.value?.tenantId || null;
});

const selectedUserOrgIds = computed(() => {
  return userOrgList.value.map(item => item.orgId);
});

// 当前父节点是否为部门（用于判断是否禁用组织类型选择）
const isParentDepartment = computed(() => {
  if (!parentOrgPath.value || parentOrgPath.value.length === 0) {
    return false;
  }
  // 获取父节点ID（路径的最后一个元素）
  const parentId = parentOrgPath.value[parentOrgPath.value.length - 1];
  const parentNode = findNodeById(orgTreeData.value, parentId);
  // 如果父节点是部门（orgType === 2），则返回true
  return parentNode?.orgType === 2;
});

// 是否是顶级公司（父节点是根节点）
const isTopLevelCompany = computed(() => {
  if (!parentOrgPath.value || parentOrgPath.value.length === 0) {
    return false;
  }
  const parentId = parentOrgPath.value[parentOrgPath.value.length - 1];
  const parentNode = findNodeById(orgTreeData.value, parentId);
  // 如果父节点是根节点（orgType === 0 或 id === 1），则返回true
  return parentNode?.orgType === 0 || parentNode?.id === 1;
});

// 抽屉
const drawerVisible = ref(false);
const isOrgEditMode = ref(false); // 是否为编辑模式
const drawerTitle = computed(() => {
  return isOrgEditMode.value ? t("org.editOrg") : t("org.addOrg");
});

// 表单
const formRef = ref<FormInstance>();
const submitLoading = ref(false);
const formData = reactive<OrgForm>({
  id: undefined,
  orgName: "",
  orgCode: "",
  tenantId: undefined, // 租户ID（不显示在界面，部门继承父节点，公司由后端生成）
  tenantCode: "", // 租户编码
  parentId: 0,
  orgType: 1,
  isCompany: 1,
  sortOrder: 0,
  status: 1,
  remark: "",
  tenantConfig: {
    // 租户配置
    tenantType: 1,
    userLimit: 100,
    storageLimit: 10240,
    expireTime: undefined,
    contactName: "",
    contactPhone: "",
    contactEmail: "",
    address: ""
  }
});

// 父组织选择
const parentOrgPath = ref<number[]>([]);
const orgCascadeOptions = ref<any[]>([]);
const cascaderProps = {
  value: "id",
  label: "label",
  children: "children",
  checkStrictly: true,
  emitPath: true // 返回完整路径
};

// 添加人员对话框
const addUserDialogVisible = ref(false);
const addUserFormRef = ref<FormInstance>();
const addUserLoading = ref(false);
const isUserEditMode = ref(false); // 用户编辑模式标志
const addUserForm = reactive({
  userId: 0, // 用户ID（编辑模式使用）
  orgId: 0,
  tenantId: 0, // 添加租户ID字段
  username: "",
  nickname: "",
  realName: "",
  email: "",
  phone: "",
  gender: 0,
  isLeader: 0
});

const addUserFormRules: FormRules = {
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { min: 2, max: 50, message: "用户名长度为2-50个字符", trigger: "blur" }
  ],
  realName: [
    { required: true, message: "请输入真实姓名", trigger: "blur" },
    { min: 2, max: 50, message: "真实姓名长度为2-50个字符", trigger: "blur" }
  ]
};

// 表单验证规则
const validateOrgCode = async (rule: any, value: string, callback: any) => {
  if (!value) {
    return callback();
  }

  try {
    // 检查组织编码是否已存在（编辑时排除当前组织）
    const exists = await checkOrgCodeExists(value, formData.id);
    if (exists) {
      callback(new Error("组织编码已存在，请使用其他编码"));
    } else {
      callback();
    }
  } catch (error) {
    console.error("验证组织编码失败:", error);
    // 验证失败时不阻塞用户，只记录错误
    callback();
  }
};

const formRules: FormRules = {
  orgName: [
    { required: true, message: t("org.inputOrgName"), trigger: "blur" },
    { min: 2, max: 100, message: t("org.orgNameLengthLimit"), trigger: "blur" }
  ],
  orgCode: [
    { required: true, message: t("org.inputOrgCode"), trigger: "blur" },
    { min: 2, max: 50, message: t("org.orgCodeLengthLimit"), trigger: "blur" },
    { validator: validateOrgCode, trigger: "blur" }
  ],
  orgType: [
    { required: true, message: t("org.selectOrgType"), trigger: "change" }
  ]
};

// 加载组织树
const loadOrgTree = async () => {
  try {
    treeLoading.value = true;
    const data = await getOrgTree();

    // 确保返回的是数组格式
    if (!Array.isArray(data)) {
      throw new Error("返回的数据格式不正确，期望数组格式");
    }

    orgTreeData.value = data;
    // 构建级联选择器选项
    orgCascadeOptions.value = buildCascadeOptions(data);
  } catch (error: any) {
    console.error("加载组织树失败:", error);
    ElMessage.error(error.message || "加载组织树失败，请检查后端服务");
    throw error;
  } finally {
    treeLoading.value = false;
    // 设置默认展开的节点
    if (orgTreeData.value.length > 0) {
      setTimeout(() => {
        setDefaultExpandedKeys();
      }, 100);
    }
  }
};

// 构建级联选择器选项
const buildCascadeOptions = (nodes: OrgTreeNode[]): any[] => {
  if (!Array.isArray(nodes)) {
    console.warn("buildCascadeOptions: nodes 不是数组", nodes);
    return [];
  }

  return nodes
    .filter(node => node.nodeType === "org")
    .map(node => ({
      id: node.id,
      label: node.label,
      children: node.children ? buildCascadeOptions(node.children) : []
    }));
};

// 节点点击
const handleNodeClick = (data: OrgTreeNode) => {
  selectedNode.value = data;
  // 重置Tab状态
  activeOrgTab.value = "basic";
  activeUserTab.value = "basic";

  // 如果是用户节点，加载用户的组织关系
  if (data.nodeType === "user") {
    loadUserOrgs(data.userId || data.id);
  }
};

// Tab中的添加按钮处理
const handleAddFromTab = () => {
  if (!selectedNode.value) return;
  handleAddOrg(selectedNode.value);
};

// Tab中的下拉菜单添加处理
const handleAddFromTabDropdown = (command: string) => {
  if (!selectedNode.value) return;
  handleAction(command, selectedNode.value);
};

// Tab中的编辑按钮处理
const handleEditFromTab = (row: OrgTreeNode) => {
  if (row.nodeType === "org") {
    handleEditOrg(row);
  } else {
    handleViewUser(row);
  }
};

// Tab中的删除按钮处理
const handleDeleteFromTab = async (row: OrgTreeNode) => {
  if (row.nodeType === "org") {
    await handleDeleteOrg(row);
  } else {
    await handleRemoveUser(row);
  }
};

// 用户操作处理
const handleEditUser = () => {
  if (!selectedNode.value || selectedNode.value.nodeType !== "user") {
    return;
  }

  // 设置为编辑模式
  isUserEditMode.value = true;

  // 填充用户数据到表单
  const userNode = selectedNode.value;
  addUserForm.userId = userNode.userId || userNode.id;
  addUserForm.orgId = userNode.parentId;
  addUserForm.tenantId = userNode.tenantId || 0;
  addUserForm.username = userNode.username || "";
  addUserForm.nickname = userNode.nickname || "";
  addUserForm.realName = userNode.realName || "";
  addUserForm.email = userNode.email || "";
  addUserForm.phone = userNode.phone || "";
  addUserForm.gender = userNode.gender || 0;
  addUserForm.isLeader = userNode.isLeader || 0;

  // 打开对话框
  addUserDialogVisible.value = true;
};

const handleToggleFreeze = async () => {
  if (!selectedNode.value || selectedNode.value.nodeType !== "user") {
    return;
  }

  const userId = selectedNode.value.userId || selectedNode.value.id;
  const currentStatus = selectedNode.value.status || 0;
  const newStatus = currentStatus === 0 ? 1 : 0; // 0=正常, 1=冻结

  try {
    await updateUserStatus(userId, newStatus);
    ElMessage.success(newStatus === 1 ? "用户已冻结" : "用户已解冻");

    // 更新本地状态
    if (selectedNode.value) {
      selectedNode.value.status = newStatus;
    }

    // 重新加载组织树
    await loadOrgTree();
  } catch (error: any) {
    console.error("更新用户状态失败:", error);
    ElMessage.error(error.message || "更新用户状态失败");
  }
};

const handleDeleteUser = () => {
  ElMessage.info("删除用户功能开发中...");
};

// ========================================
// 兼职部门相关方法
// ========================================

/**
 * 加载用户的组织关系列表
 */
const loadUserOrgs = async (userId: number) => {
  try {
    userOrgListLoading.value = true;
    const data = await getUserOrgs(userId);
    userOrgList.value = data;
  } catch (error: any) {
    console.error("加载用户组织关系失败:", error);
    ElMessage.error(error.message || "加载用户组织关系失败");
  } finally {
    userOrgListLoading.value = false;
  }
};

/**
 * 打开添加兼职部门选择器
 */
const handleAddPartTimeJob = () => {
  if (!selectedNode.value || selectedNode.value.nodeType !== "user") {
    return;
  }

  // 重置选中
  selectedPartTimeDepts.value = 0;

  // 打开部门选择器
  deptSelectorRef.value?.open();
};

/**
 * 部门选择完成
 */
const handleDeptSelected = async (orgId: number, deptItems: any[]) => {
  if (!orgId || deptItems.length === 0) {
    return;
  }

  const dept = deptItems[0];
  const userId = selectedNode.value?.userId || selectedNode.value?.id;

  try {
    // 添加用户到组织（兼职部门，isMainDept=0，不设为负责人）
    await addUserToOrg({
      orgId: orgId,
      userId: userId,
      isLeader: 0,
      isMainDept: 0 // 兼职部门
    });

    ElMessage.success("添加兼职部门成功");

    // 重新加载用户组织关系
    await loadUserOrgs(userId);

    // 重新加载组织树
    await loadOrgTree();
  } catch (error: any) {
    console.error("添加兼职部门失败:", error);
    ElMessage.error(error.message || "添加兼职部门失败");
  }
};

/**
 * 移除兼职部门
 */
const handleRemovePartTimeJob = async (row: any) => {
  if (row.isMainDept === 1) {
    ElMessage.warning("主部门不能移除");
    return;
  }

  try {
    await ElMessageBox.confirm("确定要将该用户从该部门移除吗？", "提示", {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning"
    });

    const userId = selectedNode.value?.userId || selectedNode.value?.id;

    await removeUserFromOrg(row.orgId, userId);

    ElMessage.success("移除成功");

    // 重新加载用户组织关系
    await loadUserOrgs(userId);

    // 重新加载组织树
    await loadOrgTree();
  } catch (error: any) {
    if (error !== "cancel") {
      console.error("移除兼职部门失败:", error);
      ElMessage.error(error.message || "移除兼职部门失败");
    }
  }
};

/**
 * 设置为负责人
 */
const handleSetLeader = async (row: any) => {
  try {
    const userId = selectedNode.value?.userId || selectedNode.value?.id;

    await setUserAsLeader(row.orgId, userId, 1);

    ElMessage.success("设置成功");

    // 重新加载用户组织关系
    await loadUserOrgs(userId);
  } catch (error: any) {
    console.error("设置负责人失败:", error);
    ElMessage.error(error.message || "设置负责人失败");
  }
};

/**
 * 取消负责人
 */
const handleCancelLeader = async (row: any) => {
  try {
    const userId = selectedNode.value?.userId || selectedNode.value?.id;

    await setUserAsLeader(row.orgId, userId, 0);

    ElMessage.success("取消成功");

    // 重新加载用户组织关系
    await loadUserOrgs(userId);
  } catch (error: any) {
    console.error("取消负责人失败:", error);
    ElMessage.error(error.message || "取消负责人失败");
  }
};

// 搜索
const handleSearch = () => {
  if (!searchKeyword.value.trim()) {
    // 如果搜索框为空，显示完整树
    loadOrgTree();
    return;
  }

  // 过滤树节点
  const keyword = searchKeyword.value.toLowerCase().trim();
  const filteredTree = filterTree(orgTreeData.value, keyword);

  if (filteredTree.length === 0) {
    ElMessage.warning("未找到匹配的组织或人员");
  } else {
    orgTreeData.value = filteredTree;
    // 等待DOM更新后展开所有节点以便查看搜索结果
    setTimeout(() => {
      expandAllNodes();
    }, 100);
  }
};

// 递归过滤树节点
const filterTree = (nodes: OrgTreeNode[], keyword: string): OrgTreeNode[] => {
  const result: OrgTreeNode[] = [];

  for (const node of nodes) {
    // 检查当前节点是否匹配
    const isMatch = node.label.toLowerCase().includes(keyword);

    // 递归检查子节点
    const filteredChildren = node.children
      ? filterTree(node.children, keyword)
      : [];

    // 如果当前节点匹配，或者有子节点匹配，则保留该节点
    if (isMatch || filteredChildren.length > 0) {
      result.push({
        ...node,
        children: filteredChildren.length > 0 ? filteredChildren : node.children
      });
    }
  }

  return result;
};

// 展开所有节点
const expandAllNodes = () => {
  if (treeRef.value) {
    const allNodes = treeRef.value.store.nodesMap;
    Object.keys(allNodes).forEach(key => {
      allNodes[key].expanded = true;
    });
  }
};

// 设置默认展开的节点（只展开第一个根节点）
const setDefaultExpandedKeys = () => {
  if (orgTreeData.value.length > 0) {
    // 只展开第一个根节点
    const firstRootId = orgTreeData.value[0]?.id;
    if (firstRootId) {
      defaultExpandedKeys.value = [firstRootId];
      // 同步到树组件的 store
      if (treeRef.value?.store) {
        treeRef.value.store.defaultExpandedKeys = [firstRootId];
      }
    }
  }
};

const handleSearchClear = () => {
  searchKeyword.value = "";
  loadOrgTree();
  // 等待DOM更新后设置默认展开状态
  setTimeout(() => {
    setDefaultExpandedKeys();
  }, 100);
};

// 操作处理
const handleAction = async (command: string, data: OrgTreeNode) => {
  switch (command) {
    case "addSubOrg":
      handleAddOrg(data);
      break;
    case "addUser":
      handleAddUser(data);
      break;
    case "edit":
      handleEditOrg(data);
      break;
    case "delete":
      handleDeleteOrg(data);
      break;
    case "viewUser":
      handleViewUser(data);
      break;
    case "removeUser":
      handleRemoveUser(data);
      break;
  }
};

// 添加人员
const handleAddUser = (parentNode: OrgTreeNode) => {
  // 设置为添加模式
  isUserEditMode.value = false;

  // 重置表单
  addUserForm.userId = 0;
  addUserForm.orgId = parentNode.id;
  addUserForm.tenantId = parentNode.tenantId; // 添加租户ID
  addUserForm.username = "";
  addUserForm.nickname = "";
  addUserForm.realName = "";
  addUserForm.email = "";
  addUserForm.phone = "";
  addUserForm.gender = 0;
  addUserForm.isLeader = 0;

  // 打开对话框
  addUserDialogVisible.value = true;
};

// 提交添加人员
const handleAddUserSubmit = async () => {
  try {
    // 验证表单
    await addUserFormRef.value?.validate();

    addUserLoading.value = true;

    if (isUserEditMode.value) {
      // 编辑模式：更新用户信息
      await updateUser({
        id: addUserForm.userId,
        nickname: addUserForm.nickname,
        realName: addUserForm.realName,
        email: addUserForm.email,
        phone: addUserForm.phone,
        gender: addUserForm.gender
      });

      // 更新负责人状态
      await setUserAsLeader({
        orgId: addUserForm.orgId,
        userId: addUserForm.userId,
        isLeader: addUserForm.isLeader
      });

      ElMessage.success("更新人员信息成功");
    } else {
      // 添加模式：创建新用户
      // 1. 创建用户（不传密码，使用后端默认密码）
      const userId = await createUser({
        username: addUserForm.username,
        nickname: addUserForm.nickname,
        realName: addUserForm.realName,
        email: addUserForm.email,
        phone: addUserForm.phone,
        gender: addUserForm.gender,
        status: 0, // 0-正常
        tenantId: addUserForm.tenantId // 从组织节点获取租户ID
      });

      console.log("用户创建成功，userId:", userId);

      // 2. 将用户添加到组织（默认为主部门）
      await addUserToOrg({
        orgId: addUserForm.orgId,
        userId: userId,
        isLeader: addUserForm.isLeader,
        isMainDept: 1 // 默认主部门
      });

      ElMessage.success("添加人员成功，默认密码为：123456");
    }

    // 关闭对话框
    addUserDialogVisible.value = false;

    // 重新加载组织树
    await loadOrgTree();
  } catch (error: any) {
    console.error(
      isUserEditMode.value ? "更新人员失败:" : "添加人员失败:",
      error
    );
    if (error !== false) {
      ElMessage.error(
        error.message ||
          (isUserEditMode.value ? "更新人员失败" : "添加人员失败")
      );
    }
  } finally {
    addUserLoading.value = false;
  }
};

// 关闭添加人员对话框
const handleAddUserDialogClose = () => {
  // 重置编辑模式
  isUserEditMode.value = false;

  // 重置表单
  addUserForm.userId = 0;
  addUserForm.orgId = 0;
  addUserForm.tenantId = 0; // 重置租户ID
  addUserForm.username = "";
  addUserForm.nickname = "";
  addUserForm.realName = "";
  addUserForm.email = "";
  addUserForm.phone = "";
  addUserForm.gender = 0;
  addUserForm.isLeader = 0;
  addUserFormRef.value?.clearValidate();
};

// 查看用户详情
const handleViewUser = (userNode: OrgTreeNode) => {
  console.log("查看用户详情:", userNode);
  // TODO: 实现用户详情对话框
  ElMessage.info(`查看用户: ${userNode.label}`);
};

// 移除人员
const handleRemoveUser = async (userNode: OrgTreeNode) => {
  try {
    await ElMessageBox.confirm(
      `确定要将 ${userNode.label} 从组织中移除吗？`,
      "移除人员",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }
    );

    // 调用后端API移除人员
    await removeUserFromOrg(userNode.parentId, userNode.userId!);
    ElMessage.success("移除人员成功");

    // 重新加载组织树
    await loadOrgTree();

    // 如果移除的是当前选中节点，清空选中状态
    if (selectedNode.value?.id === userNode.id) {
      selectedNode.value = null;
    }
  } catch (error: any) {
    // 用户取消或出错
    if (error !== "cancel") {
      console.error("移除人员失败:", error);
      ElMessage.error(error.message || "移除人员失败");
    }
  }
};

// 自动计算排序号
const calculateSortOrder = (parentId: number): number => {
  // 找到父节点
  const parentNode = findNodeById(orgTreeData.value, parentId);
  if (!parentNode || !parentNode.children) {
    return 0;
  }

  // 筛选出组织节点（排除用户节点）
  const orgChildren = parentNode.children.filter(child => child.nodeType === 'org');

  // 如果没有子组织，返回0
  if (orgChildren.length === 0) {
    return 0;
  }

  // 返回当前子节点数量（作为新的排序号）
  return orgChildren.length;
};

// 自动生成组织编码
const generateOrgCode = () => {
  // 根据组织类型生成前缀
  const prefix = formData.orgType === 1 ? 'COM' : 'DEPT';

  // 生成时间戳（YYMMDDHHmmss）
  const now = new Date();
  const year = now.getFullYear().toString().slice(-2); // 取后两位
  const month = (now.getMonth() + 1).toString().padStart(2, '0');
  const day = now.getDate().toString().padStart(2, '0');
  const hour = now.getHours().toString().padStart(2, '0');
  const minute = now.getMinutes().toString().padStart(2, '0');
  const second = now.getSeconds().toString().padStart(2, '0');

  const timestamp = `${year}${month}${day}${hour}${minute}${second}`;

  // 组合编码：前缀 + 时间戳
  formData.orgCode = `${prefix}${timestamp}`;
};

// 自动生成租户编码（使用时间戳，与组织编码类似的逻辑）
const generateTenantCode = () => {
  // 生成时间戳（YYMMDDHHmmss）
  const now = new Date();
  const year = now.getFullYear().toString().slice(-2); // 取后两位
  const month = (now.getMonth() + 1).toString().padStart(2, "0");
  const day = now.getDate().toString().padStart(2, "0");
  const hour = now.getHours().toString().padStart(2, "0");
  const minute = now.getMinutes().toString().padStart(2, "0");
  const second = now.getSeconds().toString().padStart(2, "0");

  const timestamp = `${year}${month}${day}${hour}${minute}${second}`;

  // 组合编码：ten_ + 时间戳（小写字母、数字和下划线）
  formData.tenantCode = `ten_${timestamp}`;
};

// 获取节点的租户ID
// 对于公司节点（orgType=1），返回它自己的tenantId
// 对于部门节点（orgType=2），递归向上查找父公司的tenantId
// 对于根节点（orgType=0），返回undefined
const getNodeTenantId = (
  node: OrgTreeNode,
  treeData: OrgTreeNode[]
): number | undefined => {
  if (!node) return undefined;

  // 如果是公司节点，直接返回它的tenantId
  if (node.orgType === 1) {
    return node.tenantId;
  }

  // 如果是部门节点或根节点，需要向上查找父公司
  if (node.parentId) {
    const parentNode = findNodeById(treeData, node.parentId);
    if (parentNode) {
      return getNodeTenantId(parentNode, treeData);
    }
  }

  return undefined;
};

// 获取父公司节点（用于继承租户编码）
const getParentCompanyNode = (
  node: OrgTreeNode,
  treeData: OrgTreeNode[]
): OrgTreeNode | undefined => {
  if (!node) return undefined;

  // 如果是公司节点，直接返回
  if (node.orgType === 1) {
    return node;
  }

  // 如果是部门节点或根节点，向上查找父公司
  if (node.parentId) {
    const parentNode = findNodeById(treeData, node.parentId);
    if (parentNode) {
      return getParentCompanyNode(parentNode, treeData);
    }
  }

  return undefined;
};

// 处理租户编码输入（格式化）
const handleTenantCodeInput = (value: string) => {
  // 转小写
  let formatted = value.toLowerCase();

  // 只允许小写字母、数字、下划线
  formatted = formatted.replace(/[^a-z0-9_]/g, "_");

  // 移除连续的下划线
  formatted = formatted.replace(/_+/g, "_");

  // 更新值
  if (formatted !== value) {
    formData.tenantCode = formatted;
  }
};

// 拖拽控制：判断节点是否可以放置
const allowDrop = (draggingNode: any, dropNode: any, type: string) => {
  // 不能拖拽根节点（orgType === 0）
  if (draggingNode.data.orgType === 0) {
    return false;
  }

  // 不能拖拽用户节点
  if (draggingNode.data.nodeType === 'user') {
    return false;
  }

  // 不能放置到用户节点上或用户节点内
  if (dropNode.data.nodeType === 'user') {
    return false;
  }

  // 部门只能拖到部门或公司下，不能拖到根节点下
  if (type === 'inner' && draggingNode.data.orgType === 2 && dropNode.data.orgType === 0) {
    return false;
  }

  // 允许其他情况
  return true;
};

// 处理节点拖拽
const handleNodeDrop = async (draggingNode: any, dropNode: any, dropType: string) => {
  try {
    const draggingData = draggingNode.data;
    const dropData = dropNode.data;

    // 确定新的父节点ID
    let newParentId = 0;
    if (dropType === 'inner') {
      // 放置到节点内部
      newParentId = dropData.id;
    } else {
      // 放置到节点之前或之后，使用相同的父节点
      newParentId = dropData.parentId || 0;
    }

    // 从DOM中获取新父节点下的所有子节点（拖拽后的最新顺序）
    let siblings: any[] = [];
    if (dropType === 'inner') {
      // 放到节点内部，从dropNode的children中获取
      siblings = dropNode.childNodes?.map((node: any) => node.data).filter((d: any) => d.nodeType === 'org') || [];
    } else {
      // 放到节点之前或之后，从dropNode的父节点获取
      const parent = dropNode.parent;
      siblings = parent?.childNodes?.map((node: any) => node.data).filter((d: any) => d.nodeType === 'org') || [];
    }

    // 重新计算所有兄弟节点的排序号（按照当前顺序，每次加1）
    for (let i = 0; i < siblings.length; i++) {
      const sibling = siblings[i];
      const newSortOrder = i; // 0, 1, 2, 3...

      // 更新每个节点的排序号
      await updateOrg({
        id: sibling.id,
        orgName: sibling.label,
        orgCode: sibling.orgCode,
        parentId: newParentId,
        orgType: sibling.orgType,
        isCompany: sibling.orgType === 1 ? 1 : 0,
        sortOrder: newSortOrder,
        status: 1
      });
    }

    // 清空选中节点，避免状态不一致
    selectedNode.value = null;

    // 重新加载组织树以获取最新数据
    await loadOrgTree();

    ElMessage.success("组织移动成功");
  } catch (error: any) {
    console.error("节点拖拽失败:", error);
    ElMessage.error(error.message || "节点拖拽失败");
    // 发生错误时重新加载树，恢复原状
    await loadOrgTree();
  }
};

// 添加组织
const handleAddOrg = (parentNode: OrgTreeNode) => {
  resetForm();
  formData.parentId = parentNode.id;
  // 使用 getNodePath 获取从根到父节点的完整路径
  parentOrgPath.value = getNodePath(orgTreeData.value, parentNode.id);

  // 自动计算排序号
  formData.sortOrder = calculateSortOrder(parentNode.id);

  // 如果父节点是部门，强制子组织类型为部门
  if (parentNode.orgType === 2) {
    formData.orgType = 2;
  }

  // 自动生成组织编码
  generateOrgCode();

  // 根据父节点类型处理租户信息
  if (parentNode.orgType === 0 || parentNode.id === 1) {
    // 父节点是根节点（组织架构），创建新租户
    generateTenantCode();
    formData.tenantId = undefined;
    formData.tenantCode = formData.tenantCode; // 使用生成的编码
    formData.orgType = 1; // 强制为公司类型
  } else {
    // 父节点不是根节点，继承父节点的租户ID和租户编码
    const parentTenantId = getNodeTenantId(parentNode, orgTreeData.value);
    formData.tenantId = parentTenantId;

    // 获取父公司的租户编码
    const parentCompanyNode = getParentCompanyNode(parentNode, orgTreeData.value);
    formData.tenantCode = parentCompanyNode?.tenantCode || "";

    // orgType 保持用户选择或默认值
  }

  drawerVisible.value = true;
};

// 编辑组织
const handleEditOrg = (node: OrgTreeNode) => {
  resetForm();
  // TODO: 从后端获取完整组织信息
  formData.id = node.id;
  formData.orgName = node.label;
  formData.orgCode = node.orgCode || "";
  formData.tenantId = node.tenantId; // 设置租户ID
  formData.tenantCode = node.tenantCode || ""; // 设置租户编码
  formData.parentId = node.parentId;
  formData.orgType = node.orgType || 1;
  formData.isCompany = node.isCompany || 1;
  formData.sortOrder = node.sortOrder || 0;

  // 使用 getNodePath 获取从根到父节点的完整路径
  if (node.parentId) {
    parentOrgPath.value = getNodePath(orgTreeData.value, node.parentId);
  }

  // 如果父节点是部门，强制组织类型为部门
  if (node.parentId) {
    const parentNode = findNodeById(orgTreeData.value, node.parentId);
    if (parentNode?.orgType === 2) {
      formData.orgType = 2;
    }
  }

  // 标记为编辑模式
  isOrgEditMode.value = true;

  drawerVisible.value = true;
};

// 删除组织
const handleDeleteOrg = async (node: OrgTreeNode) => {
  try {
    await ElMessageBox.confirm(
      t("org.deleteConfirm"),
      t("common.warning"),
      {
        confirmButtonText: t("common.confirm"),
        cancelButtonText: t("common.cancel"),
        type: "warning"
      }
    );

    await deleteOrg(node.id);
    ElMessage.success(t("org.deleteSuccess"));

    // 重新加载组织树
    await loadOrgTree();

    // 如果删除的是当前选中节点，清空选中状态
    if (selectedNode.value?.id === node.id) {
      selectedNode.value = null;
    }
  } catch (error: any) {
    if (error !== "cancel") {
      console.error("删除组织失败:", error);
      ElMessage.error(error.message || t("org.deleteFailed"));
    }
  }
};

// 提交表单
const handleSubmit = async () => {
  try {
    // 先进行表单验证
    await formRef.value?.validate();

    submitLoading.value = true;

    // 如果是公司节点，验证租户编码
    if (formData.orgType === 1) {
      if (!formData.tenantCode || formData.tenantCode.trim() === "") {
        ElMessage.error("请输入租户编码");
        submitLoading.value = false;
        return;
      }
      // 租户编码格式验证（只能包含小写字母、数字、下划线）
      const tenantCodeRegex = /^[a-z0-9_]+$/;
      if (!tenantCodeRegex.test(formData.tenantCode)) {
        ElMessage.error("租户编码只能包含小写字母、数字和下划线");
        submitLoading.value = false;
        return;
      }
    }

    // 再次检查组织编码是否已存在（双重验证）
    const codeExists = await checkOrgCodeExists(formData.orgCode, formData.id);
    if (codeExists) {
      ElMessage.error("组织编码已存在，请使用其他编码");
      submitLoading.value = false;
      return;
    }

    // 根据组织类型自动设置 isCompany 字段
    // orgType = 1 (公司) → isCompany = 1
    // orgType = 2 (部门) → isCompany = 0
    const submitData = {
      ...formData,
      isCompany: formData.orgType === 1 ? 1 : 0
    };

    if (formData.id) {
      // 编辑模式
      await updateOrg(submitData);
      ElMessage.success(t("org.updateSuccess"));
    } else {
      // 新增模式
      await addOrg(submitData);
      ElMessage.success(t("org.addSuccess"));
    }

    drawerVisible.value = false;
    await loadOrgTree();
  } catch (error: any) {
    console.error("提交失败:", error);
    if (error !== false) {
      ElMessage.error(error.message || t("org.submitFailed"));
    }
  } finally {
    submitLoading.value = false;
  }
};

// 获取从根节点到指定节点的完整路径
const getNodePath = (nodes: OrgTreeNode[], targetId: number): number[] => {
  const path: number[] = [];

  const findPath = (nodeList: OrgTreeNode[]): boolean => {
    for (const node of nodeList) {
      if (node.id === targetId) {
        path.push(node.id);
        return true;
      }
      if (node.children && findPath(node.children)) {
        path.unshift(node.id);
        return true;
      }
    }
    return false;
  };

  findPath(nodes);
  return path;
};

// 根据ID查找节点
const findNodeById = (nodes: OrgTreeNode[], id: number): OrgTreeNode | null => {
  for (const node of nodes) {
    if (node.id === id) {
      return node;
    }
    if (node.children) {
      const found = findNodeById(node.children, id);
      if (found) {
        return found;
      }
    }
  }
  return null;
};

// 父组织变化
const handleParentChange = (value: number[]) => {
  // value 是完整路径数组，最后一个元素是直接父节点ID
  if (Array.isArray(value) && value.length > 0) {
    formData.parentId = value[value.length - 1];
  } else {
    formData.parentId = 0;
  }
};

// 关闭抽屉
const handleDrawerClose = () => {
  resetForm();
};

// 重置表单
const resetForm = () => {
  isOrgEditMode.value = false; // 重置编辑模式
  formData.id = undefined;
  formData.orgName = "";
  formData.orgCode = "";
  formData.tenantId = undefined;
  formData.tenantCode = "";
  formData.parentId = 0;
  formData.orgType = 1;
  formData.isCompany = 1;
  formData.sortOrder = 0;
  formData.status = 1;
  formData.remark = "";
  formData.tenantConfig = {
    tenantType: 1,
    userLimit: 100,
    storageLimit: 10240,
    expireTime: undefined,
    contactName: "",
    contactPhone: "",
    contactEmail: "",
    address: ""
  };
  parentOrgPath.value = [];
  formRef.value?.clearValidate();
};

// 初始化
onMounted(() => {
  loadOrgTree();
});
</script>

<style scoped lang="scss">
.org-container {
  padding: 20px;

  .org-card {
    .card-header {
      display: flex;
      align-items: center;
      gap: 12px;

      span {
        font-size: 16px;
        font-weight: 500;
        flex: 1;
      }
    }

    .org-content {
      display: flex;
      height: calc(100vh - 240px);
      gap: 20px;

      .org-tree-panel {
        flex: 0 0 300px;
        max-width: 300px;
        border-right: 1px solid #e4e7ed;
        padding-right: 20px;
        overflow-y: auto;
        display: flex;
        flex-direction: column;

        .tree-search {
          margin-bottom: 16px;
          padding: 12px;
          background-color: #f5f7fa;
          border-radius: 4px;
        }

        .custom-tree-node {
          display: flex;
          align-items: center;
          justify-content: space-between;
          flex: 1;
          padding-right: 8px;

          .node-content {
            display: flex;
            align-items: center;
            gap: 6px;

            .node-icon {
              color: #409eff;
            }

            .node-label {
              font-size: 14px;
            }
          }

          .node-actions {
            .action-icon {
              font-size: 16px;
              color: #909399;
              cursor: pointer;
              padding: 4px;

              &:hover {
                color: #409eff;
              }
            }
          }
        }
      }

      .org-detail-panel {
        flex: 1;
        padding-left: 20px;
        overflow-y: auto;

        .detail-content {
          .detail-tabs {
            .tab-header {
              margin-bottom: 16px;
              display: flex;
              align-items: center;
              gap: 8px;
            }

            .user-info-actions {
              margin-bottom: 16px;
              display: flex;
              gap: 8px;
            }
          }

          .org-detail,
          .user-detail {
            padding: 20px;
          }
        }
      }
    }
  }
}
</style>
