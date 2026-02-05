<template>
  <div class="role-management">
    <el-card class="role-card">
      <!-- 页面头部 -->
      <template #header>
        <div class="card-header">
          <span>角色管理</span>
        </div>
      </template>

      <!-- 主体内容：左右布局 -->
      <div class="role-content">
        <!-- 左侧：角色树 -->
        <div class="role-tree-panel">
          <!-- 搜索框 -->
          <div class="tree-search">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索角色..."
              clearable
              @input="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>

          <!-- 角色树 -->
          <el-tree
            ref="treeRef"
            v-loading="treeLoading"
            :data="roleTreeData"
            :props="treeProps"
            :filter-node-method="filterNode"
            :expand-on-click-node="false"
            :highlight-current="true"
            node-key="id"
            :default-expanded-keys="defaultExpandedKeys"
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <div class="custom-tree-node">
                <div class="node-content">
                  <!-- Layer 1 根节点使用文件夹图标 -->
                  <el-icon v-if="data.roleType === 0" class="node-icon root-icon">
                    <Folder />
                  </el-icon>
                  <!-- Layer 2 分类节点使用集合图标 -->
                  <el-icon v-else-if="data.roleType === 1" class="node-icon category-icon">
                    <Collection />
                  </el-icon>
                  <!-- Layer 3 具体角色节点使用印章图标 -->
                  <el-icon v-else class="node-icon role-icon">
                    <Stamp />
                  </el-icon>
                  <span class="node-label">{{ node.label }}</span>
                </div>

                <!-- 操作按钮 -->
                <div class="node-actions">
                  <!-- 根节点(Layer 1)的操作 -->
                  <template v-if="data.roleType === 0">
                    <el-dropdown @command="(command) => handleAction(command, data)">
                      <el-icon class="action-icon">
                        <MoreFilled />
                      </el-icon>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item command="addRole">
                            <el-icon><Plus /></el-icon>
                            添加角色
                          </el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </template>

                  <!-- 分类节点(Layer 2)的操作 -->
                  <template v-else-if="data.roleType === 1">
                    <el-dropdown @command="(command) => handleAction(command, data)">
                      <el-icon class="action-icon">
                        <MoreFilled />
                      </el-icon>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item command="addRole">
                            <el-icon><Plus /></el-icon>
                            添加角色
                          </el-dropdown-item>
                          <el-dropdown-item command="edit" divided>
                            <el-icon><Edit /></el-icon>
                            编辑
                          </el-dropdown-item>
                          <el-dropdown-item command="delete">
                            <el-icon><Delete /></el-icon>
                            删除
                          </el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </template>

                  <!-- 角色节点(Layer 3)的操作 -->
                  <template v-else>
                    <el-dropdown @command="(command) => handleAction(command, data)">
                      <el-icon class="action-icon">
                        <MoreFilled />
                      </el-icon>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item command="edit">
                            <el-icon><Edit /></el-icon>
                            编辑
                          </el-dropdown-item>
                          <el-dropdown-item command="delete">
                            <el-icon><Delete /></el-icon>
                            删除
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
        <div class="role-detail-panel">
          <el-empty v-if="!selectedNode" description="请选择左侧角色节点" />

          <div v-else class="detail-content">
            <!-- Tab页签 -->
            <el-tabs v-if="selectedNode.roleType === 2" v-model="activeTab" class="detail-tabs">
              <!-- Tab1: 基本信息 -->
              <el-tab-pane label="基本信息" name="basic">
                <div class="tab-content">
                  <div class="tab-header">
                    <div class="role-title">
                      <el-icon class="role-icon" :size="24" style="margin-right: 8px">
                        <Stamp />
                      </el-icon>
                      <span class="title-text">{{ selectedNode.label }}</span>
                    </div>
                    <el-button
                      type="primary"
                      :icon="Edit"
                      size="small"
                      @click="handleEditRole(selectedNode)"
                    >
                      编辑
                    </el-button>
                  </div>
                  <el-descriptions :column="2" border>
                    <el-descriptions-item label="角色名称">
                      {{ selectedNode.label }}
                    </el-descriptions-item>
                    <el-descriptions-item label="角色编码">
                      {{ selectedNode.roleCode }}
                    </el-descriptions-item>
                    <el-descriptions-item label="所属分类">
                      {{ categoryName }}
                    </el-descriptions-item>
                    <el-descriptions-item label="排序">
                      {{ selectedNode.sortOrder }}
                    </el-descriptions-item>
                    <el-descriptions-item label="状态" :span="2">
                      <el-tag :type="selectedNode.status === 0 ? 'success' : 'danger'">
                        {{ selectedNode.status === 0 ? '正常' : '禁用' }}
                      </el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="描述" :span="2">
                      {{ selectedNode.description || '无' }}
                    </el-descriptions-item>
                  </el-descriptions>
                </div>
              </el-tab-pane>

              <!-- Tab2: 人员 -->
              <el-tab-pane label="人员" name="users">
                <div class="tab-header">
                  <el-button type="primary" :icon="Plus" @click="handleAddUser">
                    添加人员
                  </el-button>
                </div>
                <el-table :data="roleUsers" v-loading="usersLoading">
                  <el-table-column prop="userName" label="用户名" />
                  <el-table-column prop="realName" label="姓名" />
                  <el-table-column label="操作" width="100">
                    <template #default="{ row }">
                      <el-button link type="danger" @click="handleRemoveUser(row)">
                        移除
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>

              <!-- Tab3: 部门 -->
              <el-tab-pane label="部门" name="depts">
                <div class="tab-header">
                  <el-button type="primary" :icon="Plus" @click="handleAddDept">
                    添加部门
                  </el-button>
                </div>
                <el-table :data="roleDepts" v-loading="deptsLoading">
                  <el-table-column prop="deptName" label="部门名称" />
                  <el-table-column label="操作" width="100">
                    <template #default="{ row }">
                      <el-button link type="danger" @click="handleRemoveDept(row)">
                        移除
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>

              <!-- Tab4: 权限 -->
              <el-tab-pane label="权限" name="permissions">
                <div class="tab-header">
                  <el-button type="primary" :icon="Plus" @click="handleAddPermission">
                    添加权限
                  </el-button>
                </div>
                <el-table :data="rolePermissions" v-loading="permissionsLoading">
                  <el-table-column prop="permissionName" label="权限名称" />
                  <el-table-column prop="permissionCode" label="权限编码" />
                  <el-table-column label="操作" width="100">
                    <template #default="{ row }">
                      <el-button link type="danger" @click="handleRemovePermission(row)">
                        移除
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>
            </el-tabs>

            <!-- 根节点或分类节点提示 -->
            <el-empty v-else-if="selectedNode.roleType === 0" description="根节点，请展开查看分类" />
            <el-empty v-else description="请选择具体角色查看详情" />
          </div>
        </div>
      </div>
    </el-card>

    <!-- 角色表单对话框 -->
    <RoleFormDialog
      v-model="roleFormVisible"
      :role="currentRole"
      :categories="categories"
      @success="handleRoleFormSuccess"
    />

    <!-- 用户选择器 -->
    <UserSelector
      ref="userSelectorRef"
      v-model="selectedUsers"
      :multiple="true"
      :exclude-ids="roleUsers.map(u => u.userId)"
      title="添加人员"
      @change="handleUserSelectConfirm"
    />

    <!-- 部门选择器 -->
    <DeptSelector
      ref="deptSelectorRef"
      v-model="selectedDepts"
      :multiple="false"
      :exclude-ids="roleDepts.map(d => d.deptId)"
      title="添加部门"
      @change="handleDeptSelected"
    />

    <!-- 权限选择对话框 -->
    <PermissionSelectDialog
      v-model="permissionSelectVisible"
      :selected-ids="selectedPermissionIds"
      @confirm="handlePermissionSelectConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  Search,
  Plus,
  Edit,
  Delete,
  Folder,
  Collection,
  Stamp,
  MoreFilled
} from "@element-plus/icons-vue";
import type { ElTree } from "element-plus";
import {
  getRoleTree,
  getRoleDetail,
  deleteRole,
  addUsersToRole,
  removeUserFromRole,
  addDeptsToRole,
  removeDeptFromRole,
  addPermissionsToRole,
  removePermissionFromRole,
  type RoleNode,
  type RoleUser,
  type RoleDept,
  type RolePermission
} from "@/api/role";
import RoleFormDialog from "./components/RoleFormDialog.vue";
import UserSelector from "@/components/UserSelector.vue";
import PermissionSelectDialog from "./components/PermissionSelectDialog.vue";
import DeptSelector from "@/components/DeptSelector.vue";

// 角色树数据
const roleTreeData = ref<RoleNode[]>([]);
const treeLoading = ref(false);
const treeRef = ref<InstanceType<typeof ElTree>>();

// 搜索关键词
const searchKeyword = ref("");

// 默认展开的节点（根节点）
const defaultExpandedKeys = ref<string[]>(['-1']);

// 当前选中的节点
const selectedNode = ref<RoleNode | null>(null);

// 当前激活的 Tab
const activeTab = ref("basic");

// 角色详情数据
const categoryName = ref("");
const roleUsers = ref<RoleUser[]>([]);
const roleDepts = ref<RoleDept[]>([]);
const rolePermissions = ref<RolePermission[]>([]);

// 加载状态
const usersLoading = ref(false);
const deptsLoading = ref(false);
const permissionsLoading = ref(false);

// 树形结构配置
const treeProps = {
  children: "children",
  label: "label"
};

// ========== 对话框和组件状态 ==========

// 角色表单对话框
const roleFormVisible = ref(false);
const currentRole = ref<RoleNode | null>(null);
const categories = computed(() => {
  // 树结构：根节点(roleType=0) -> 分类(roleType=1) -> 角色(roleType=2)
  // 需要从根节点的 children 中获取分类列表
  const rootNode = roleTreeData.value.find(n => n.roleType === 0);
  return rootNode?.children || [];
});

// 用户选择器
const userSelectorRef = ref<InstanceType<typeof UserSelector>>();
const selectedUsers = ref<number[]>([]);

// 权限选择对话框
const permissionSelectVisible = ref(false);
const selectedPermissionIds = computed(() =>
  rolePermissions.value.map(p => p.permissionId)
);

// 部门选择器
const deptSelectorRef = ref<InstanceType<typeof DeptSelector>>();
const selectedDepts = ref<number | number[]>(0);

/**
 * 加载角色树
 */
const loadRoleTree = async () => {
  try {
    treeLoading.value = true;
    const response = await getRoleTree();
    roleTreeData.value = response.data || [];
  } catch (error) {
    console.error("加载角色树失败:", error);
    ElMessage.error("加载角色树失败");
  } finally {
    treeLoading.value = false;
  }
};

/**
 * 搜索过滤
 */
const filterNode = (value: string, data: RoleNode) => {
  if (!value) return true;
  return data.label.includes(value);
};

/**
 * 搜索输入
 */
const handleSearch = () => {
  treeRef.value?.filter(searchKeyword.value);
};

/**
 * 节点点击
 */
const handleNodeClick = (data: RoleNode) => {
  selectedNode.value = data;

  if (data.roleType === 2) {
    // 具体角色，加载详情
    loadRoleDetail(data.id!);
  }
};

/**
 * 加载角色详情
 */
const loadRoleDetail = async (id: number) => {
  try {
    const response = await getRoleDetail(id);
    const detail = response.data;

    // 更新基本信息
    categoryName.value = detail.categoryName || "";

    // 更新人员、部门、权限
    roleUsers.value = detail.users || [];
    roleDepts.value = detail.depts || [];
    rolePermissions.value = detail.permissions || [];
  } catch (error) {
    console.error("加载角色详情失败:", error);
    ElMessage.error("加载角色详情失败");
  }
};

/**
 * 操作处理
 */
const handleAction = (command: string, data: RoleNode) => {
  switch (command) {
    case "addRole":
      if (data.roleType === 0) {
        // 根节点：添加角色，需要用户选择父级分类
        currentRole.value = {
          id: undefined,
          parentId: undefined,  // 需要用户选择父级分类
          roleName: "",
          roleCode: "",
          roleType: 2,
          description: "",
          sortOrder: 0,
          status: 0
        };
      } else {
        // 分类节点：添加角色，直接使用当前分类作为父级
        currentRole.value = {
          id: undefined,
          parentId: data.id,  // 使用当前分类的ID作为父级
          roleName: "",
          roleCode: "",
          roleType: 2,
          description: "",
          sortOrder: 0,
          status: 0
        };
      }
      roleFormVisible.value = true;
      break;
    case "edit":
      // 打开编辑对话框
      currentRole.value = data;
      roleFormVisible.value = true;
      break;
    case "delete":
      handleDelete(data);
      break;
  }
};

/**
 * 编辑角色
 */
const handleEditRole = (data: RoleNode) => {
  currentRole.value = data;
  roleFormVisible.value = true;
};

/**
 * 删除角色
 */
const handleDelete = async (data: RoleNode) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除角色"${data.label}"吗？`,
      "删除确认",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }
    );

    const response = await deleteRole(data.id!);
    if (response.code === 200) {
      ElMessage.success("删除成功");
      // 重新加载角色树
      await loadRoleTree();
      // 清空右侧
      selectedNode.value = null;
    } else {
      ElMessage.error(response.message || "删除失败");
    }
  } catch (error: any) {
    if (error !== "cancel") {
      console.error("删除失败:", error);
      ElMessage.error(error.message || "删除失败");
    }
  }
};

/**
 * 添加人员
 */
const handleAddUser = () => {
  if (!selectedNode.value) {
    ElMessage.warning("请先选择角色");
    return;
  }

  // 重置选中
  selectedUsers.value = [];

  // 获取已选用户ID列表，用于排除
  const excludeIds = roleUsers.value.map(u => u.userId);

  // 打开用户选择器
  userSelectorRef.value?.open();
};

/**
 * 用户选择完成
 */
const handleUserSelectConfirm = async (userIds: number[], users: any[]) => {
  if (!userIds || userIds.length === 0) {
    return;
  }

  const roleId = selectedNode.value?.id;

  try {
    const response = await addUsersToRole(roleId!, userIds);
    if (response.code === 200) {
      ElMessage.success("添加成功");
      // 重新加载详情
      await loadRoleDetail(roleId!);
    } else {
      ElMessage.error(response.message || "添加失败");
    }
  } catch (error: any) {
    console.error("添加失败:", error);
    ElMessage.error(error.message || "添加失败");
  }
};

/**
 * 移除人员
 */
const handleRemoveUser = async (row: RoleUser) => {
  try {
    await ElMessageBox.confirm(
      `确定要从角色中移除用户"${row.realName}"吗？`,
      "移除确认",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }
    );

    const response = await removeUserFromRole(selectedNode.value!.id!, row.userId);
    if (response.code === 200) {
      ElMessage.success("移除成功");
      // 重新加载详情
      await loadRoleDetail(selectedNode.value!.id!);
    } else {
      ElMessage.error(response.message || "移除失败");
    }
  } catch (error: any) {
    if (error !== "cancel") {
      console.error("移除失败:", error);
      ElMessage.error(error.message || "移除失败");
    }
  }
};

/**
 * 移除部门
 */
const handleRemoveDept = async (row: RoleDept) => {
  try {
    await ElMessageBox.confirm(
      `确定要从角色中移除部门"${row.deptName}"吗？`,
      "移除确认",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }
    );

    const response = await removeDeptFromRole(selectedNode.value!.id!, row.deptId);
    if (response.code === 200) {
      ElMessage.success("移除成功");
      // 重新加载详情
      await loadRoleDetail(selectedNode.value!.id!);
    } else {
      ElMessage.error(response.message || "移除失败");
    }
  } catch (error: any) {
    if (error !== "cancel") {
      console.error("移除失败:", error);
      ElMessage.error(error.message || "移除失败");
    }
  }
};

/**
 * 添加权限
 */
const handleAddPermission = () => {
  permissionSelectVisible.value = true;
};

/**
 * 移除权限
 */
const handleRemovePermission = async (row: RolePermission) => {
  try {
    await ElMessageBox.confirm(
      `确定要从角色中移除权限"${row.permissionName}"吗？`,
      "移除确认",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }
    );

    const response = await removePermissionFromRole(selectedNode.value!.id!, row.permissionId);
    if (response.code === 200) {
      ElMessage.success("移除成功");
      // 重新加载详情
      await loadRoleDetail(selectedNode.value!.id!);
    } else {
      ElMessage.error(response.message || "移除失败");
    }
  } catch (error: any) {
    if (error !== "cancel") {
      console.error("移除失败:", error);
      ElMessage.error(error.message || "移除失败");
    }
  }
};

/**
 * 权限选择确认
 */
const handlePermissionSelectConfirm = async (permissionIds: number[]) => {
  try {
    const response = await addPermissionsToRole(selectedNode.value!.id!, permissionIds);
    if (response.code === 200) {
      ElMessage.success("添加成功");
      // 重新加载详情
      await loadRoleDetail(selectedNode.value!.id!);
    } else {
      ElMessage.error(response.message || "添加失败");
    }
  } catch (error: any) {
    console.error("添加失败:", error);
    ElMessage.error(error.message || "添加失败");
  }
};

/**
 * 角色表单成功回调
 */
const handleRoleFormSuccess = async () => {
  // 重新加载角色树
  await loadRoleTree();

  // 如果是编辑模式，重新加载详情
  if (currentRole.value?.id) {
    await loadRoleDetail(currentRole.value.id);
  }
};

/**
 * 添加部门
 */
const handleAddDept = () => {
  if (!selectedNode.value) {
    ElMessage.warning("请先选择角色");
    return;
  }

  // 重置选中
  selectedDepts.value = 0;

  // 获取已选部门ID列表，用于排除
  const excludeIds = roleDepts.value.map(d => d.deptId);

  // 打开部门选择器
  deptSelectorRef.value?.open();
};

/**
 * 部门选择完成
 */
const handleDeptSelected = async (deptId: number, deptItems: any[]) => {
  if (!deptId || deptItems.length === 0) {
    return;
  }

  const roleId = selectedNode.value?.id;

  try {
    const response = await addDeptsToRole(roleId!, [deptId]);
    if (response.code === 200) {
      ElMessage.success("添加成功");
      // 重新加载详情
      await loadRoleDetail(roleId!);
    } else {
      ElMessage.error(response.message || "添加失败");
    }
  } catch (error: any) {
    console.error("添加失败:", error);
    ElMessage.error(error.message || "添加失败");
  }
};

// 组件挂载时加载角色树
onMounted(() => {
  loadRoleTree();
});
</script>

<style scoped lang="scss">
.role-management {
  height: 100%;
  padding: 0px 0px 30px 0px;
  box-sizing: border-box;

  .role-card {
    height: 100%;
    display: flex;
    flex-direction: column;

    :deep(.el-card__body) {
      flex: 1;
      overflow: hidden;
      padding: 20px;
    }

    .card-header {
      font-size: 16px;
      font-weight: 500;
    }

    .role-content {
      display: flex;
      gap: 20px;
      height: 100%;
    }

    .role-tree-panel {
      width: 300px;
      min-width: 300px;
      max-width: 300px;
      border-right: 1px solid #e4e7ed;
      padding: 16px;
      overflow-y: auto;
      flex-shrink: 0;

      .tree-search {
        margin-bottom: 16px;
      }

      .custom-tree-node {
        flex: 1;
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding-right: 8px;

        .node-content {
          flex: 1;
          display: flex;
          align-items: center;
          gap: 8px;

          .node-icon {
            flex-shrink: 0;
            font-size: 16px;
            color: #909399;

            &.root-icon {
              font-size: 20px;
              color: #67c23a;
            }

            &.category-icon {
              font-size: 17px;
              color: #409eff;
            }

            &.role-icon {
              font-size: 16px;
              color: #e6a23c;
            }
          }

          .node-label {
            font-size: 14px;
            color: #303133;
            font-weight: 500;

            // 根节点(Layer 1)的标签样式
            .custom-tree-node:has(.root-icon) & {
              font-size: 16px;
              font-weight: 600;
              color: #67c23a;
            }

            // 分类节点(Layer 2)的标签样式
            .custom-tree-node:has(.category-icon) & {
              font-size: 15px;
              font-weight: 500;
              color: #409eff;
            }

            // 角色节点(Layer 3)的标签样式
            .custom-tree-node:has(.role-icon) & {
              font-size: 14px;
              font-weight: 400;
              color: #303133;
            }
          }
        }

        .node-actions {
          flex-shrink: 0;

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

      :deep(.el-tree-node__content) {
        height: 36px;
      }
    }

    .role-detail-panel {
      flex: 1;
      padding: 16px;
      overflow-y: auto;
      min-width: 0;

      .detail-tabs {
        display: flex;
        flex-direction: column;

        :deep(.el-tabs__content) {
          flex: 1;
          overflow-y: auto;
        }

        .tab-content {
          .tab-header {
            margin-bottom: 16px;
            padding: 16px;
            background-color: #f5f7fa;
            border-radius: 4px;
            display: flex;
            justify-content: space-between;
            align-items: center;

            .role-title {
              display: flex;
              align-items: center;
              font-size: 16px;
              font-weight: 500;
              color: #303133;

              .role-icon {
                color: #e6a23c;
              }

              .title-text {
                font-size: 18px;
              }
            }
          }
        }

        .tab-header {
          margin-bottom: 16px;
        }

        :deep(.el-descriptions) {
          margin-bottom: 16px;
        }
      }
    }
  }
}

// 响应式布局
@media (max-width: 768px) {
  .role-management {
    .role-card {
      .role-content {
        flex-direction: column;

        .role-tree-panel {
          width: 100%;
          max-width: none;
          border-right: none;
          border-bottom: 1px solid #e4e7ed;
          max-height: 400px;
        }

        .role-detail-panel {
          padding: 12px;
        }
      }
    }
  }
}
</style>
