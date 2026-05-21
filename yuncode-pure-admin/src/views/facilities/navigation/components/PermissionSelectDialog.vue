<template>
  <el-dialog
    v-model="dialogVisible"
    title="添加权限"
    width="700px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <!-- 目标类型选择 -->
    <el-radio-group v-model="targetType" size="default" style="margin-bottom: 16px" @change="handleTypeChange">
      <el-radio-button :value="0">
        <el-icon><Stamp /></el-icon>
        <span style="margin-left: 4px">角色</span>
      </el-radio-button>
      <el-radio-button :value="1">
        <el-icon><User /></el-icon>
        <span style="margin-left: 4px">人员</span>
      </el-radio-button>
      <el-radio-button :value="2">
        <el-icon><OfficeBuilding /></el-icon>
        <span style="margin-left: 4px">部门</span>
      </el-radio-button>
    </el-radio-group>

    <!-- 已选择的权限 -->
    <div v-if="selectedPermissions.length > 0" class="selected-area">
      <div class="selected-header">
        <span>已选 ({{ selectedPermissions.length }})</span>
        <el-button link type="danger" size="small" @click="handleClearAll">
          清空
        </el-button>
      </div>
      <div class="selected-list">
        <el-tag
          v-for="item in selectedPermissions"
          :key="item.id"
          closable
          @close="handleRemove(item)"
          style="margin: 4px"
        >
          {{ item.label }}
        </el-tag>
      </div>
    </div>

    <!-- 选择区域 -->
    <div v-loading="listLoading" class="select-area">
      <!-- 角色列表 -->
      <div v-if="targetType === 0" class="item-list">
        <el-table
          ref="roleTableRef"
          :data="roleList"
          row-key="id"
          @selection-change="handleSelectionChange"
          height="300"
        >
          <el-table-column type="selection" width="50" reserve-selection />
          <el-table-column prop="roleName" label="角色名称" />
          <el-table-column prop="roleCode" label="角色编码" />
          <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        </el-table>
      </div>

      <!-- 用户列表 -->
      <div v-else-if="targetType === 1" class="item-list">
        <div class="search-box">
          <el-input
            v-model="userSearchKeyword"
            placeholder="搜索用户名、姓名..."
            clearable
            size="small"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
        <el-table
          ref="userTableRef"
          :data="filteredUserList"
          row-key="id"
          @selection-change="handleSelectionChange"
          height="270"
        >
          <el-table-column type="selection" width="50" reserve-selection />
          <el-table-column label="头像" width="60">
            <template #default="{ row }">
              <el-avatar :src="row.avatar" :size="32">
                {{ row.username?.charAt(0)?.toUpperCase() }}
              </el-avatar>
            </template>
          </el-table-column>
          <el-table-column prop="username" label="用户名" />
          <el-table-column prop="realName" label="姓名" />
          <el-table-column prop="nickname" label="昵称" show-overflow-tooltip />
        </el-table>
      </div>

      <!-- 部门列表 -->
      <div v-else-if="targetType === 2" class="item-list">
        <div class="search-box">
          <el-input
            v-model="deptSearchKeyword"
            placeholder="搜索部门..."
            clearable
            size="small"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
        <el-table
          ref="deptTableRef"
          :data="filteredDeptList"
          row-key="id"
          @selection-change="handleSelectionChange"
          height="270"
        >
          <el-table-column type="selection" width="50" reserve-selection />
          <el-table-column label="名称" width="200">
            <template #default="{ row }">
              <div style="display: flex; align-items: center; gap: 6px">
                <el-icon v-if="row.orgType === 1" color="#409eff">
                  <OfficeBuilding />
                </el-icon>
                <el-icon v-else color="#67c23a">
                  <Folder />
                </el-icon>
                <span>{{ row.label }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="orgCode" label="编码" />
          <el-table-column label="类型" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.orgType === 1" type="primary" size="small">
                公司
              </el-tag>
              <el-tag v-else type="success" size="small">部门</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleConfirm" :loading="submitting">
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from "vue";
import { ElMessage } from "element-plus";
import { User, Stamp, OfficeBuilding, Search } from "@element-plus/icons-vue";
import { addMenuPermissionsAdapter } from "@/api/menu-adapter";
import { getRoleTree } from "@/api/role";
import { getOrgTree } from "@/api/org-adapter";

/**
 * 权限选择对话框组件
 * 支持选择角色、用户、部门
 * 角色数据来源于角色管理，人员和组织来源于组织管理
 */

const emit = defineEmits<{
  confirm: [];
}>();

// 对话框显示状态
const dialogVisible = ref(false);

// 当前菜单ID
const currentMenuId = ref<number>();

// 目标类型（0=角色, 1=用户, 2=部门）
const targetType = ref<number>(0);

// 提交中状态
const submitting = ref(false);

// 列表加载状态
const listLoading = ref(false);

// 搜索关键词
const userSearchKeyword = ref("");
const deptSearchKeyword = ref("");

// 已选择的权限
const selectedPermissions = ref<any[]>([]);

// 原始数据列表
const roleList = ref<any[]>([]);
const userList = ref<any[]>([]);
const deptList = ref<any[]>([]);

// 表格引用
const roleTableRef = ref();
const userTableRef = ref();
const deptTableRef = ref();

// 过滤后的用户列表
const filteredUserList = computed(() => {
  if (!userSearchKeyword.value) {
    return userList.value;
  }
  const keyword = userSearchKeyword.value.toLowerCase();
  return userList.value.filter(
    user =>
      user.username?.toLowerCase().includes(keyword) ||
      user.realName?.toLowerCase().includes(keyword) ||
      user.nickname?.toLowerCase().includes(keyword)
  );
});

// 过滤后的部门列表
const filteredDeptList = computed(() => {
  if (!deptSearchKeyword.value) {
    return deptList.value;
  }
  const keyword = deptSearchKeyword.value.toLowerCase();
  return deptList.value.filter(
    dept =>
      dept.label?.toLowerCase().includes(keyword) ||
      dept.orgCode?.toLowerCase().includes(keyword)
  );
});

/**
 * 打开对话框
 */
const open = async (menuId: number) => {
  dialogVisible.value = true;
  currentMenuId.value = menuId;
  targetType.value = 0;
  selectedPermissions.value = [];
  userSearchKeyword.value = "";
  deptSearchKeyword.value = "";

  // 同时加载角色、用户、部门数据
  await Promise.all([loadRoles(), loadOrgData()]);
};

/**
 * 加载角色列表（从角色管理）
 */
const loadRoles = async () => {
  try {
    const response = await getRoleTree();
    const treeData = response.data || [];
    // 从角色树中提取所有 roleType === 2 的具体角色
    roleList.value = extractRolesFromTree(treeData);
  } catch (error: any) {
    ElMessage.error(error.message || "加载角色列表失败");
  }
};

/**
 * 从角色树中提取具体角色节点
 */
const extractRolesFromTree = (nodes: any[]): any[] => {
  const result: any[] = [];
  const walk = (list: any[]) => {
    for (const node of list) {
      if (node.roleType === 2) {
        result.push({
          id: node.id,
          roleName: node.roleName || node.label,
          roleCode: node.roleCode,
          remark: node.description || "-"
        });
      }
      if (node.children && node.children.length > 0) {
        walk(node.children);
      }
    }
  };
  walk(nodes);
  return result;
};

/**
 * 加载组织数据（从组织管理）
 */
const loadOrgData = async () => {
  try {
    listLoading.value = true;
    const treeData = await getOrgTree();
    // 从组织树中提取用户和部门
    userList.value = extractUsersFromTree(treeData);
    deptList.value = extractDeptsFromTree(treeData);
  } catch (error: any) {
    ElMessage.error(error.message || "加载组织数据失败");
  } finally {
    listLoading.value = false;
  }
};

/**
 * 从组织树中提取用户节点
 */
const extractUsersFromTree = (nodes: any[]): any[] => {
  const result: any[] = [];
  const walk = (list: any[]) => {
    for (const node of list) {
      if (node.nodeType === "user") {
        result.push({
          id: node.id,
          username: node.username,
          realName: node.realName,
          nickname: node.nickname,
          avatar: node.avatar
        });
      }
      if (node.children && node.children.length > 0) {
        walk(node.children);
      }
    }
  };
  walk(nodes);
  return result;
};

/**
 * 从组织树中提取部门/组织节点（排除根节点）
 */
const extractDeptsFromTree = (nodes: any[]): any[] => {
  const result: any[] = [];
  const walk = (list: any[]) => {
    for (const node of list) {
      if (node.nodeType === "org" && node.orgType !== 0) {
        result.push({
          id: node.id,
          label: node.label,
          orgCode: node.orgCode,
          orgType: node.orgType
        });
      }
      if (node.children && node.children.length > 0) {
        walk(node.children);
      }
    }
  };
  walk(nodes);
  return result;
};

/**
 * 目标类型变化
 */
const handleTypeChange = () => {
  selectedPermissions.value = [];
  // 清空所有表格的选中状态
  nextTick(() => {
    roleTableRef.value?.clearSelection();
    userTableRef.value?.clearSelection();
    deptTableRef.value?.clearSelection();
  });
};

/**
 * 选择变化
 */
const handleSelectionChange = (selection: any[]) => {
  selectedPermissions.value = selection.map(item => ({
    id: item.id,
    label: item.roleName || item.realName || item.username || item.label,
    data: item
  }));
};

/**
 * 移除已选项
 */
const handleRemove = (item: any) => {
  const index = selectedPermissions.value.findIndex(p => p.id === item.id);
  if (index > -1) {
    selectedPermissions.value.splice(index, 1);
    // 同步取消表格中的选中状态
    const row = findRowById(item.id);
    if (row) {
      const tableRef = getCurrentTableRef();
      tableRef?.toggleRowSelection(row, false);
    }
  }
};

/**
 * 根据ID查找行数据
 */
const findRowById = (id: number) => {
  const list =
    targetType.value === 0
      ? roleList.value
      : targetType.value === 1
        ? userList.value
        : deptList.value;
  return list.find(item => item.id === id);
};

/**
 * 获取当前类型的表格引用
 */
const getCurrentTableRef = () => {
  if (targetType.value === 0) return roleTableRef.value;
  if (targetType.value === 1) return userTableRef.value;
  return deptTableRef.value;
};

/**
 * 清空所有
 */
const handleClearAll = () => {
  selectedPermissions.value = [];
  roleTableRef.value?.clearSelection();
  userTableRef.value?.clearSelection();
  deptTableRef.value?.clearSelection();
};

/**
 * 确定
 */
const handleConfirm = async () => {
  if (!currentMenuId.value) {
    ElMessage.warning("菜单ID不存在");
    return;
  }

  if (selectedPermissions.value.length === 0) {
    ElMessage.warning("请选择要添加的权限");
    return;
  }

  try {
    submitting.value = true;

    const targetIds = selectedPermissions.value.map(p => p.id);
    await addMenuPermissionsAdapter(currentMenuId.value, targetType.value, targetIds);

    ElMessage.success("添加成功");
    emit("confirm");
    handleClose();
  } catch (error: any) {
    ElMessage.error(error.message || "添加失败");
  } finally {
    submitting.value = false;
  }
};

/**
 * 关闭对话框
 */
const handleClose = () => {
  dialogVisible.value = false;
  currentMenuId.value = undefined;
  selectedPermissions.value = [];
  userSearchKeyword.value = "";
  deptSearchKeyword.value = "";
  roleList.value = [];
  userList.value = [];
  deptList.value = [];
};

// 暴露方法
defineExpose({
  open
});
</script>

<style scoped lang="scss">
.select-area {
  .search-box {
    margin-bottom: 12px;
  }

  .item-list {
    border: 1px solid #e4e7ed;
    border-radius: 4px;
    overflow: hidden;
  }
}

.selected-area {
  margin-bottom: 16px;
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 4px;

  .selected-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
    font-size: 13px;
    font-weight: 500;
    color: #606266;
  }

  .selected-list {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }
}
</style>
