<template>
  <el-dialog
    v-model="visible"
    title="选择权限"
    width="700px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <!-- 搜索区域 -->
    <div class="search-area">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索权限名称或编码..."
        clearable
        @input="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <!-- 权限树 -->
    <el-tree
      ref="treeRef"
      v-loading="loading"
      :data="filteredPermissions"
      :props="treeProps"
      :default-checked-keys="selectedPermissionIds"
      node-key="id"
      show-checkbox
      :filter-node-method="filterNode"
      height="400px"
    >
      <template #default="{ node, data }">
        <div class="tree-node">
          <el-icon v-if="data.icon" class="node-icon">
            <component :is="getIcon(data.icon)" />
          </el-icon>
          <span class="node-label">{{ node.label }}</span>
          <el-tag v-if="data.type" size="small" class="node-tag">
            {{ getTypeLabel(data.type) }}
          </el-tag>
        </div>
      </template>
    </el-tree>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleConfirm">
        确定（已选 {{ checkedCount }} 项）
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from "vue";
import { ElMessage } from "element-plus";
import { Search } from "@element-plus/icons-vue";
import type { ElTree } from "element-plus";
import { getIcon } from "@/utils/icon";

/**
 * 权限选择对话框
 */

interface PermissionNode {
  id: number;
  label: string;
  type?: string;
  icon?: string;
  children?: PermissionNode[];
}

interface Props {
  modelValue?: boolean;
  selectedIds?: number[];
}

interface Emits {
  (e: "update:modelValue", value: boolean): void;
  (e: "confirm", permissionIds: number[]): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

// 树引用
const treeRef = ref<InstanceType<typeof ElTree>>();

// 对话框显示状态
const visible = computed({
  get: () => props.modelValue ?? false,
  set: (val) => emit("update:modelValue", val)
});

// 加载状态
const loading = ref(false);

// 搜索关键词
const searchKeyword = ref("");

// 权限树数据
const permissionTree = ref<PermissionNode[]>([]);

// 已选择的权限ID
const selectedPermissionIds = computed(() => props.selectedIds || []);

// 过滤后的权限树
const filteredPermissions = computed(() => {
  if (!searchKeyword.value) {
    return permissionTree.value;
  }
  // 搜索逻辑在 filterNode 中处理
  return permissionTree.value;
});

// 选中的数量
const checkedCount = ref(0);

// 树形结构配置
const treeProps = {
  children: "children",
  label: "label"
};

/**
 * 监听对话框打开
 */
watch(visible, (val) => {
  if (val) {
    loadPermissions();
  }
});

/**
 * 加载权限树
 */
const loadPermissions = async () => {
  try {
    loading.value = true;

    // TODO: 调用实际的 API 获取菜单权限树
    // const data = await getMenuTree();

    // 模拟数据
    permissionTree.value = [
      {
        id: 1,
        label: "系统管理",
        type: "目录",
        icon: "Setting",
        children: [
          {
            id: 11,
            label: "用户管理",
            type: "菜单",
            icon: "User",
            children: [
              { id: 111, label: "查询用户", type: "按钮" },
              { id: 112, label: "新增用户", type: "按钮" },
              { id: 113, label: "编辑用户", type: "按钮" },
              { id: 114, label: "删除用户", type: "按钮" }
            ]
          },
          {
            id: 12,
            label: "角色管理",
            type: "菜单",
            icon: "UserFilled",
            children: [
              { id: 121, label: "查询角色", type: "按钮" },
              { id: 122, label: "新增角色", type: "按钮" },
              { id: 123, label: "编辑角色", type: "按钮" },
              { id: 124, label: "删除角色", type: "按钮" }
            ]
          },
          {
            id: 13,
            label: "菜单管理",
            type: "菜单",
            icon: "Menu",
            children: [
              { id: 131, label: "查询菜单", type: "按钮" },
              { id: 132, label: "新增菜单", type: "按钮" },
              { id: 133, label: "编辑菜单", type: "按钮" },
              { id: 134, label: "删除菜单", type: "按钮" }
            ]
          }
        ]
      },
      {
        id: 2,
        label: "业务管理",
        type: "目录",
        icon: "Briefcase",
        children: [
          {
            id: 21,
            label: "订单管理",
            type: "菜单",
            icon: "Document",
            children: [
              { id: 211, label: "查询订单", type: "按钮" },
              { id: 212, label: "新增订单", type: "按钮" },
              { id: 213, label: "编辑订单", type: "按钮" }
            ]
          }
        ]
      }
    ];

    // 更新选中数量
    updateCheckedCount();
  } catch (error) {
    console.error("加载权限树失败:", error);
    ElMessage.error("加载权限树失败");
  } finally {
    loading.value = false;
  }
};

/**
 * 搜索过滤
 */
const filterNode = (value: string, data: PermissionNode) => {
  if (!value) return true;
  return data.label.includes(value);
};

/**
 * 搜索处理
 */
const handleSearch = () => {
  treeRef.value?.filter(searchKeyword.value);
};

/**
 * 获取图标组件
 */
const getIcon = (iconName: string) => {
  return getIcon(iconName);
};

/**
 * 获取类型标签
 */
const getTypeLabel = (type: string) => {
  const typeMap: Record<string, string> = {
    目录: "目录",
    菜单: "菜单",
    按钮: "按钮"
  };
  return typeMap[type] || type;
};

/**
 * 更新选中数量
 */
const updateCheckedCount = () => {
  setTimeout(() => {
    const checkedKeys = treeRef.value?.getCheckedKeys(true) || [];
    checkedCount.value = checkedKeys.length;
  }, 100);
};

/**
 * 确认
 */
const handleConfirm = () => {
  // 获取选中的权限ID（只获取叶子节点）
  const checkedKeys = treeRef.value?.getCheckedKeys(true) || [];

  if (checkedKeys.length === 0) {
    ElMessage.warning("请至少选择一个权限");
    return;
  }

  emit("confirm", checkedKeys as number[]);
  handleClose();
};

/**
 * 关闭
 */
const handleClose = () => {
  visible.value = false;
  searchKeyword.value = "";
  checkedCount.value = 0;
};
</script>

<style scoped lang="scss">
.search-area {
  margin-bottom: 16px;
}

:deep(.el-tree) {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 8px;
  height: 400px;
  overflow-y: auto;

  .tree-node {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 8px;
    padding-right: 8px;

    .node-icon {
      font-size: 16px;
      color: #909399;
    }

    .node-label {
      font-size: 14px;
      color: #303133;
    }

    .node-tag {
      margin-left: auto;
    }
  }
}
</style>
