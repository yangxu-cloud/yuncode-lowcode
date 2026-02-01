<template>
  <el-dialog
    v-model="dialogVisible"
    title="添加权限"
    width="700px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <!-- 目标类型选择 -->
    <el-radio-group v-model="targetType" size="default" style="margin-bottom: 16px">
      <el-radio-button :label="0">
        <el-icon><User /></el-icon>
        <span style="margin-left: 4px">角色</span>
      </el-radio-button>
      <el-radio-button :label="1">
        <el-icon><Avatar /></el-icon>
        <span style="margin-left: 4px">用户</span>
      </el-radio-button>
      <el-radio-button :label="2">
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
    <div class="select-area">
      <!-- 角色列表 -->
      <div v-if="targetType === 0" class="item-list">
        <el-table
          ref="roleTableRef"
          :data="roleList"
          @selection-change="handleSelectionChange"
          height="300"
        >
          <el-table-column type="selection" width="50" />
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
            prefix-icon="Search"
            clearable
            size="small"
          />
        </div>
        <el-table
          ref="userTableRef"
          :data="filteredUserList"
          @selection-change="handleSelectionChange"
          height="270"
        >
          <el-table-column type="selection" width="50" />
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
            prefix-icon="Search"
            clearable
            size="small"
          />
        </div>
        <el-table
          ref="deptTableRef"
          :data="filteredDeptList"
          @selection-change="handleSelectionChange"
          height="270"
        >
          <el-table-column type="selection" width="50" />
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
import { ref, computed } from "vue";
import { ElMessage } from "element-plus";
import { User, Avatar, OfficeBuilding, Folder } from "@element-plus/icons-vue";
import { addMenuPermissionsAdapter } from "@/api/menu-adapter";
import { getOrgTreeAdapter } from "@/api/org-adapter";

/**
 * 权限选择对话框组件
 * 支持选择角色、用户、部门
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

// 搜索关键词
const userSearchKeyword = ref("");
const deptSearchKeyword = ref("");

// 已选择的权限
const selectedPermissions = ref<any[]>([]);

// 角色列表（模拟数据）
const roleList = ref([
  { id: 1, roleName: "超级管理员", roleCode: "admin", remark: "系统最高权限" },
  { id: 2, roleName: "租户管理员", roleCode: "tenant_admin", remark: "租户管理权限" },
  { id: 3, roleName: "普通用户", roleCode: "user", remark: "普通用户权限" },
  { id: 4, roleName: "访客", roleCode: "guest", remark: "只读权限" }
]);

// 用户列表（模拟数据）
const userList = ref([
  {
    id: 1,
    username: "admin",
    realName: "管理员",
    nickname: "超级管理员",
    avatar: ""
  },
  {
    id: 2,
    username: "user1",
    realName: "张三",
    nickname: "张三",
    avatar: ""
  },
  {
    id: 3,
    username: "user2",
    realName: "李四",
    nickname: "李四",
    avatar: ""
  }
]);

// 部门列表
const deptList = ref<any[]>([]);

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
    return flattenDeptList(deptList.value);
  }
  const keyword = deptSearchKeyword.value.toLowerCase();
  return flattenDeptList(deptList.value).filter(
    dept =>
      dept.label?.toLowerCase().includes(keyword) ||
      dept.orgCode?.toLowerCase().includes(keyword)
  );
});

/**
 * 展平部门列表
 */
const flattenDeptList = (list: any[]): any[] => {
  const result: any[] = [];
  const flatten = (nodes: any[]) => {
    for (const node of nodes) {
      if (node.nodeType === "org") {
        result.push(node);
      }
      if (node.children && node.children.length > 0) {
        flatten(node.children);
      }
    }
  };
  flatten(list);
  return result;
};

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

  // 加载部门树
  await loadDeptTree();
};

/**
 * 加载部门树
 */
const loadDeptTree = async () => {
  try {
    const { data } = await getOrgTreeAdapter();
    deptList.value = data || [];
  } catch (error: any) {
    ElMessage.error(error.message || "加载部门树失败");
  }
};

/**
 * 选择变化
 */
const handleSelectionChange = (selection: any[]) => {
  // 根据目标类型设置标签
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
  }
};

/**
 * 清空所有
 */
const handleClearAll = () => {
  selectedPermissions.value = [];
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
