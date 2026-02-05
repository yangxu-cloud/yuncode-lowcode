<template>
  <div class="navigation-management">
    <el-card class="navigation-card">
      <!-- 页面头部 -->
      <template #header>
        <div class="card-header">
          <span>导航管理</span>
        </div>
      </template>

      <!-- 主体内容：左右布局 -->
      <div class="navigation-content">
        <!-- 左侧：菜单树 -->
        <div class="navigation-tree-panel">
          <!-- 搜索框 -->
          <div class="tree-search">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索菜单..."
              clearable
              @input="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>

          <!-- 新建子系统按钮 -->
          <div class="tree-actions">
            <el-button type="primary" :icon="Plus" @click="handleAddRootMenu">
              新建子系统
            </el-button>
          </div>

          <!-- 菜单树 -->
          <el-tree
            ref="treeRef"
            v-loading="loading"
            :data="menuTreeData"
            :props="treeProps"
            :filter-node-method="filterNode"
            :default-expanded-keys="defaultExpandedKeys"
            node-key="id"
            highlight-current
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <div class="tree-node">
                <el-icon v-if="getIcon(data.icon)" :size="16" style="margin-right: 4px">
                  <component :is="getIcon(data.icon)" />
                </el-icon>
                <span class="node-label">{{ node.label }}</span>
                <!-- 节点操作按钮 -->
                <div class="node-actions" @click.stop>
                  <el-tooltip content="添加子菜单" placement="top">
                    <el-button
                      :icon="Plus"
                      circle
                      size="small"
                      link
                      @click="handleAddMenu(data)"
                    />
                  </el-tooltip>
                  <el-tooltip content="编辑" placement="top">
                    <el-button
                      :icon="Edit"
                      circle
                      size="small"
                      link
                      @click="handleEditMenu(data)"
                    />
                  </el-tooltip>
                  <el-dropdown trigger="click" @command="(cmd) => handleMoreAction(cmd, data)">
                    <el-button :icon="More" circle size="small" link />
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item :command="'visible'">
                          <el-icon><View /></el-icon>
                          {{ data.visible === 0 ? "隐藏" : "显示" }}
                        </el-dropdown-item>
                        <el-dropdown-item :command="'moveUp'" :disabled="!canMoveUp(data)">
                          <el-icon><ArrowUp /></el-icon>
                          上移
                        </el-dropdown-item>
                        <el-dropdown-item :command="'moveDown'" :disabled="!canMoveDown(data)">
                          <el-icon><ArrowDown /></el-icon>
                          下移
                        </el-dropdown-item>
                        <el-dropdown-item :command="'delete'" divided>
                          <el-icon><Delete /></el-icon>
                          删除
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </div>
            </template>
          </el-tree>
      </div>

      <!-- 右侧：菜单详情和权限管理 -->
      <div class="navigation-detail-panel">
        <el-empty v-if="!currentMenu" description="请选择左侧菜单节点" />

        <div v-else class="detail-content">
          <!-- Tab页签 -->
          <el-tabs v-model="activeTab" class="menu-tabs">
            <!-- 菜单信息 -->
            <el-tab-pane label="菜单信息" name="info">
              <div class="tab-content">
                <div class="tab-header">
                  <div class="menu-title">
                    <el-icon v-if="getIcon(currentMenu.icon)" :size="24" style="margin-right: 8px">
                      <component :is="getIcon(currentMenu.icon)" />
                    </el-icon>
                    <span class="title-text">{{ currentMenu.menuName }}</span>
                  </div>
                  <el-button
                    type="primary"
                    :icon="Edit"
                    size="small"
                    @click="handleEditMenu(currentMenu)"
                  >
                    编辑
                  </el-button>
                </div>
                <el-descriptions :column="2" border>
                  <el-descriptions-item label="菜单名称">
                    {{ currentMenu.menuName }}
                  </el-descriptions-item>
                  <el-descriptions-item label="菜单类型">
                    <el-tag v-if="currentMenu.menuType === 0" type="info">目录</el-tag>
                    <el-tag v-else-if="currentMenu.menuType === 1" type="primary">菜单</el-tag>
                    <el-tag v-else type="success">按钮</el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="路由路径">
                    {{ currentMenu.path || "-" }}
                  </el-descriptions-item>
                  <el-descriptions-item label="组件路径">
                    {{ currentMenu.component || "-" }}
                  </el-descriptions-item>
                  <el-descriptions-item label="图标">
                    <el-icon v-if="getIcon(currentMenu.icon)" :size="20">
                      <component :is="getIcon(currentMenu.icon)" />
                    </el-icon>
                    <span v-else>-</span>
                  </el-descriptions-item>
                  <el-descriptions-item label="排序号">
                    {{ currentMenu.sortOrder || 0 }}
                  </el-descriptions-item>
                  <el-descriptions-item label="可见性">
                    <el-tag v-if="currentMenu.visible === 0" type="success">显示</el-tag>
                    <el-tag v-else type="info">隐藏</el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="状态">
                    <el-tag v-if="currentMenu.status === 0" type="success">正常</el-tag>
                    <el-tag v-else type="danger">禁用</el-tag>
                  </el-descriptions-item>
                </el-descriptions>
              </div>
            </el-tab-pane>

            <!-- 权限管理 -->
            <el-tab-pane label="权限管理" name="permission">
              <div class="tab-content">
                <div class="tab-header">
                  <span class="tab-title">权限配置</span>
                  <div class="header-actions">
                    <el-button
                      type="primary"
                      :icon="Plus"
                      size="small"
                      @click="handleAddPermission"
                    >
                      添加权限
                    </el-button>
                    <el-button
                      type="success"
                      :icon="Share"
                      size="small"
                      @click="handleCopyPermissions"
                    >
                      权限追加到下级
                    </el-button>
                  </div>
                </div>

                <el-table
                  v-loading="permissionLoading"
                  :data="permissions"
                  border
                  stripe
                  size="default"
                >
                  <el-table-column label="类型" width="100" align="center">
                    <template #default="{ row }">
                      <el-tag v-if="row.targetType === 0" type="primary">角色</el-tag>
                      <el-tag v-else-if="row.targetType === 1" type="success">用户</el-tag>
                      <el-tag v-else type="warning">部门</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="targetName" label="名称" />
                  <el-table-column prop="targetTypeName" label="描述" width="120" />
                  <el-table-column label="操作" width="80" align="center" fixed="right">
                    <template #default="{ row }">
                      <el-button
                        type="danger"
                        :icon="Delete"
                        size="small"
                        link
                        @click="handleRemovePermission(row)"
                      >
                        删除
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </div>
    </el-card>

    <!-- 菜单编辑对话框 -->
    <MenuFormDialog
      ref="menuFormDialogRef"
      @success="handleMenuSaved"
    />

    <!-- 权限选择对话框 -->
    <PermissionSelectDialog
      ref="permissionSelectDialogRef"
      @confirm="handlePermissionSelected"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  Plus,
  Edit,
  Delete,
  More,
  View,
  ArrowUp,
  ArrowDown,
  Share
} from "@element-plus/icons-vue";
import type { ElTree } from "element-plus";
import {
  getMenuTree,
  addMenu,
  updateMenu,
  deleteMenu,
  moveUpMenu,
  moveDownMenu,
  setMenuVisible,
  getMenuPermissions,
  removeMenuPermission,
  copyPermissionsToChildren
} from "@/api/menu-adapter";
import { getIconComponent } from "@/utils/icon";
import MenuFormDialog from "./components/MenuFormDialog.vue";
import PermissionSelectDialog from "./components/PermissionSelectDialog.vue";

/**
 * 导航管理页面
 * 左侧：菜单树（搜索、新建子系统、节点操作）
 * 右侧：菜单详情、权限管理
 */

// 加载状态
const loading = ref(false);
const permissionLoading = ref(false);

// 搜索关键词
const searchKeyword = ref("");

// 菜单树数据
const menuTreeData = ref<any[]>([]);

// 树形组件ref
const treeRef = ref<InstanceType<typeof ElTree>>();

// 当前选中的菜单
const currentMenu = ref<any>(null);

// 当前激活的tab页签
const activeTab = ref<string>("info");

// 权限列表
const permissions = ref<any[]>([]);

// 对话框ref
const menuFormDialogRef = ref();
const permissionSelectDialogRef = ref();

// 默认展开的节点(只展开"组织管理"节点)
const defaultExpandedKeys = ref<number[]>([]);

// 树形配置
const treeProps = {
  children: "children",
  label: "menuName"
};

/**
 * 获取图标组件
 */
const getIcon = (iconName: string | undefined) => {
  return getIconComponent(iconName);
};

/**
 * 加载菜单树
 */
const loadMenuTree = async () => {
  loading.value = true;
  try {
    const { data } = await getMenuTree();
    menuTreeData.value = data || [];

    // 数据加载完成后，展开第一个根节点
    await nextTick();
    await nextTick();
    await nextTick();

    if (treeRef.value && menuTreeData.value.length > 0) {
      // 直接从数据中获取第一个根节点的ID
      const firstRootId = menuTreeData.value[0]?.id;
      if (firstRootId) {
        // 通过ID找到对应的树节点并展开
        const node = treeRef.value.store.nodesMap[firstRootId];
        if (node) {
          node.expanded = true;
        }
      }
    }
  } catch (error: any) {
    ElMessage.error(error.message || "加载菜单树失败");
  } finally {
    loading.value = false;
  }
};

/**
 * 加载菜单权限
 */
const loadMenuPermissions = async (menuId: number) => {
  permissionLoading.value = true;
  try {
    const { data } = await getMenuPermissions(menuId);
    permissions.value = data || [];
  } catch (error: any) {
    ElMessage.error(error.message || "加载权限失败");
  } finally {
    permissionLoading.value = false;
  }
};

/**
 * 搜索过滤
 */
const filterNode = (value: string, data: any) => {
  if (!value) return true;
  return data.menuName.includes(value);
};

/**
 * 处理搜索
 */
const handleSearch = () => {
  treeRef.value?.filter(searchKeyword.value);
};

/**
 * 节点点击
 */
const handleNodeClick = (data: any) => {
  currentMenu.value = data;
  loadMenuPermissions(data.id);
};

/**
 * 添加根菜单（新建子系统）
 */
const handleAddRootMenu = () => {
  menuFormDialogRef.value?.open({
    parentId: 0,
    menuType: 0
  });
};

/**
 * 添加子菜单
 */
const handleAddMenu = (parentMenu: any) => {
  menuFormDialogRef.value?.open({
    parentId: parentMenu.id,
    parentName: parentMenu.menuName
  });
};

/**
 * 编辑菜单
 */
const handleEditMenu = (menu: any) => {
  menuFormDialogRef.value?.open(menu);
};

/**
 * 更多操作
 */
const handleMoreAction = async (command: string, data: any) => {
  switch (command) {
    case "visible":
      await handleToggleVisible(data);
      break;
    case "moveUp":
      await handleMoveUp(data);
      break;
    case "moveDown":
      await handleMoveDown(data);
      break;
    case "delete":
      await handleDelete(data);
      break;
  }
};

/**
 * 切换可见性
 */
const handleToggleVisible = async (menu: any) => {
  try {
    const newVisible = menu.visible === 0 ? 1 : 0;
    await setMenuVisible(menu.id, newVisible);
    ElMessage.success("设置成功");
    await loadMenuTree();
  } catch (error: any) {
    ElMessage.error(error.message || "设置失败");
  }
};

/**
 * 上移
 */
const handleMoveUp = async (menu: any) => {
  try {
    await moveUpMenu(menu.id);
    ElMessage.success("上移成功");
    await loadMenuTree();
  } catch (error: any) {
    ElMessage.error(error.message || "上移失败");
  }
};

/**
 * 下移
 */
const handleMoveDown = async (menu: any) => {
  try {
    await moveDownMenu(menu.id);
    ElMessage.success("下移成功");
    await loadMenuTree();
  } catch (error: any) {
    ElMessage.error(error.message || "下移失败");
  }
};

/**
 * 删除菜单
 */
const handleDelete = async (menu: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除菜单"${menu.menuName}"吗？`,
      "提示",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }
    );

    await deleteMenu(menu.id);
    ElMessage.success("删除成功");

    // 如果删除的是当前选中的菜单，清空右侧
    if (currentMenu.value?.id === menu.id) {
      currentMenu.value = null;
      permissions.value = [];
    }

    await loadMenuTree();
  } catch (error: any) {
    if (error !== "cancel") {
      ElMessage.error(error.message || "删除失败");
    }
  }
};

/**
 * 判断是否可以上移
 */
const canMoveUp = (menu: any): boolean => {
  // TODO: 实现上移判断逻辑
  return true;
};

/**
 * 判断是否可以下移
 */
const canMoveDown = (menu: any): boolean => {
  // TODO: 实现下移判断逻辑
  return true;
};

/**
 * 菜单保存成功
 */
const handleMenuSaved = async () => {
  await loadMenuTree();

  // 如果当前有选中的菜单，尝试重新选中它（使用新的树数据）
  if (currentMenu.value) {
    // 等待下一个 tick，确保树已经更新
    await nextTick();
    // 可以在这里重新设置 currentMenu，或者清空它
    // currentMenu.value = null;
  }
};

/**
 * 添加权限
 */
const handleAddPermission = () => {
  if (!currentMenu.value) {
    ElMessage.warning("请先选择菜单");
    return;
  }
  permissionSelectDialogRef.value?.open(currentMenu.value.id);
};

/**
 * 移除权限
 */
const handleRemovePermission = async (permission: any) => {
  try {
    await ElMessageBox.confirm(
      "确定要删除该权限吗？",
      "提示",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }
    );

    await removeMenuPermission(
      currentMenu.value.id,
      permission.targetType,
      permission.targetId
    );

    ElMessage.success("删除成功");
    await loadMenuPermissions(currentMenu.value.id);
  } catch (error: any) {
    if (error !== "cancel") {
      ElMessage.error(error.message || "删除失败");
    }
  }
};

/**
 * 权限追加到下级
 */
const handleCopyPermissions = async () => {
  if (!currentMenu.value) {
    ElMessage.warning("请先选择菜单");
    return;
  }

  try {
    await ElMessageBox.confirm(
      "确定要将当前菜单的权限追加到所有子菜单吗？",
      "提示",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "info"
      }
    );

    const count = await copyPermissionsToChildren(currentMenu.value.id);
    ElMessage.success(`已追加 ${count} 条权限到子菜单`);
  } catch (error: any) {
    if (error !== "cancel") {
      ElMessage.error(error.message || "追加失败");
    }
  }
};

/**
 * 权限选择完成
 */
const handlePermissionSelected = () => {
  if (currentMenu.value) {
    loadMenuPermissions(currentMenu.value.id);
  }
};

// 初始化
onMounted(() => {
  loadMenuTree();
});
</script>

<style scoped lang="scss">
.navigation-management {
  height: 100%;
  padding: 0px 0px 30px 0px;
  box-sizing: border-box;

  .navigation-card {
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

    .navigation-content {
      display: flex;
      gap: 20px;
      height: 100%;
    }

    .navigation-tree-panel {
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

      .tree-actions {
        margin-bottom: 16px;
      }

      .tree-node {
        flex: 1;
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding-right: 8px;

        .node-label {
          flex: 1;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .node-actions {
          display: none;
          gap: 4px;
        }

        &:hover .node-actions {
          display: flex;
        }
      }

      :deep(.el-tree-node__content) {
        height: 36px;
      }
    }

    .navigation-detail-panel {
      flex: 1;
      padding: 16px;
      overflow-y: auto;
      min-width: 0;

      .detail-content {
        .menu-tabs {
          display: flex;
          flex-direction: column;

          :deep(.el-tabs__content) {
            flex: 1;
            overflow-y: auto;
          }

          .tab-header {
            margin-bottom: 16px;
            padding: 16px;
            background-color: #f5f7fa;
            border-radius: 4px;
            display: flex;
            justify-content: space-between;
            align-items: center;

            .menu-title {
              display: flex;
              align-items: center;
              font-size: 16px;
              font-weight: 500;
              color: #303133;

              .title-text {
                font-size: 18px;
              }
            }

            .tab-title {
              font-size: 16px;
              font-weight: 500;
              color: #303133;
            }

            .header-actions {
              display: flex;
              gap: 8px;
            }
          }
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
  .navigation-management {
    .navigation-card {
      .navigation-content {
        flex-direction: column;

        .navigation-tree-panel {
          width: 100%;
          max-width: none;
          border-right: none;
          border-bottom: 1px solid #e4e7ed;
          max-height: 400px;
        }

        .navigation-detail-panel {
          padding: 12px;
        }
      }
    }
  }
}
</style>
