<template>
  <el-drawer
    v-model="dialogVisible"
    :title="isEdit ? '编辑菜单' : '添加菜单'"
    direction="rtl"
    size="600px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <MenuForm
      ref="menuFormRef"
      v-model="formData"
      :menu-tree-data="menuTreeData"
    />

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">
        确定
      </el-button>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from "vue";
import { ElMessage } from "element-plus";
import { getMenuTree, addMenu, updateMenu } from "@/api/menu-adapter";
import MenuForm from "./MenuForm.vue";

interface MenuData {
  id?: number;
  parentId: number;
  parentName?: string;
  menuName: string;
  icon?: string;
  menuType: number;
  path?: string;
  component?: string;
  permission?: string;
  sortOrder?: number;
  visible?: number;
  status?: number;
}

/**
 * 菜单表单抽屉组件
 */

const emit = defineEmits<{
  success: [];
}>();

// 抽屉显示状态
const dialogVisible = ref(false);

// 提交中状态
const submitting = ref(false);

// 菜单表单ref
const menuFormRef = ref();

// 菜单树数据
const menuTreeData = ref<any[]>([]);

// 表单数据
const formData = reactive<MenuData>({
  id: undefined,
  parentId: 0,
  menuName: "",
  icon: "",
  menuType: 0,
  path: "",
  component: "",
  permission: "",
  sortOrder: 0,
  visible: 0,
  status: 0
});

// 是否为编辑模式（定义在 formData 之后）
const isEdit = computed(() => !!formData.id);

/**
 * 默认表单数据
 */
const getDefaultData = (): MenuData => ({
  id: undefined,
  parentId: 0,
  menuName: "",
  icon: "",
  menuType: 0,
  path: "",
  component: "",
  permission: "",
  sortOrder: 0,
  visible: 0,
  status: 0
});

/**
 * 打开抽屉
 */
const open = async (data: any) => {
  // 先重置为默认值，清除上次的数据
  const defaultData = getDefaultData();
  Object.assign(formData, defaultData);

  // 加载菜单树（用于选择上级菜单）
  await loadMenuTree();

  // 使用临时对象收集数据，然后一次性赋值
  const newData: any = getDefaultData();

  if (data.id) {
    // 编辑模式 - 深拷贝数据避免引用问题
    newData.id = data.id;
    newData.parentId = data.parentId || 0;
    newData.parentName = data.parentName;
    newData.menuName = data.menuName || "";
    newData.icon = data.icon || "";
    newData.menuType = data.menuType ?? 0;
    newData.path = data.path || "";
    newData.component = data.component || "";
    newData.permission = data.permission || "";
    newData.sortOrder = data.sortOrder ?? 0;
    newData.visible = data.visible ?? 0;
    newData.status = data.status ?? 0;
    newData.tenantId = data.tenantId;
    newData.tenantCode = data.tenantCode;
  } else {
    // 新增模式
    newData.parentId = data.parentId || 0;
    newData.parentName = data.parentName;
  }

  // 使用 Object.assign 将新数据赋值到 formData
  Object.assign(formData, newData);

  // 打开抽屉
  dialogVisible.value = true;
};

/**
 * 加载菜单树
 */
const loadMenuTree = async () => {
  try {
    const { data } = await getMenuTree();
    menuTreeData.value = data || [];
  } catch (error: any) {
    ElMessage.error(error.message || "加载菜单树失败");
  }
};

/**
 * 提交表单
 */
const handleSubmit = async () => {
  if (!menuFormRef.value) return;

  try {
    await menuFormRef.value.validate();

    submitting.value = true;

    if (isEdit.value) {
      await updateMenu(formData);
      ElMessage.success("更新成功");
    } else {
      await addMenu(formData);
      ElMessage.success("添加成功");
    }

    emit("success");
    handleClose();
  } catch (error: any) {
    if (error !== false) {
      ElMessage.error(error.message || "操作失败");
    }
  } finally {
    submitting.value = false;
  }
};

/**
 * 关闭抽屉
 */
const handleClose = () => {
  dialogVisible.value = false;
  menuFormRef.value?.resetFields();
  Object.assign(formData, {
    id: undefined,
    parentId: 0,
    menuName: "",
    icon: "",
    menuType: 0,
    path: "",
    component: "",
    permission: "",
    sortOrder: 0,
    visible: 0,
    status: 0
  });
};

// 暴露方法
defineExpose({
  open
});
</script>
