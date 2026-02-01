<template>
  <el-form
    ref="formRef"
    :model="formData"
    :rules="formRules"
    label-width="100px"
    size="default"
  >
    <el-form-item label="菜单类型" prop="menuType">
      <el-radio-group v-model="formData.menuType">
        <el-radio :label="0">目录</el-radio>
        <el-radio :label="1">菜单</el-radio>
        <el-radio :label="2">按钮</el-radio>
      </el-radio-group>
    </el-form-item>

    <el-form-item label="菜单名称" prop="menuName">
      <el-input
        v-model="formData.menuName"
        placeholder="请输入菜单名称"
        clearable
      />
    </el-form-item>

    <el-form-item label="菜单图标">
      <div class="icon-selector-wrapper">
        <el-input
          v-model="formData.icon"
          placeholder="选择菜单图标"
          readonly
          @click="openIconSelector"
        >
          <template #prefix>
            <el-icon v-if="getIcon(formData.icon)" :size="18">
              <component :is="getIcon(formData.icon)" />
            </el-icon>
          </template>
          <template #append>
            <el-button :icon="Picture" @click="openIconSelector">
              选择
            </el-button>
          </template>
        </el-input>
      </div>
    </el-form-item>

    <el-form-item label="组件路径" v-if="formData.menuType === 1">
      <div class="component-selector-wrapper">
        <el-input
          v-model="formData.component"
          placeholder="选择组件路径"
          readonly
          @click="openComponentSelector"
        >
          <template #append>
            <el-button :icon="FolderOpened" @click="openComponentSelector">
              选择
            </el-button>
          </template>
        </el-input>
      </div>
    </el-form-item>

    <el-form-item label="路由路径" v-if="formData.menuType !== 0">
      <el-input
        v-model="formData.path"
        placeholder="例如: /system/user（自动生成）"
        clearable
      >
        <template #append>
          <el-button
            :icon="Refresh"
            @click="generatePathFromComponent"
            title="根据组件路径自动生成"
          >
            自动生成
          </el-button>
        </template>
      </el-input>
      <div class="form-tip">
        <el-icon><InfoFilled /></el-icon>
        <span>选择组件后会自动生成路由路径，也可手动修改</span>
      </div>
    </el-form-item>

    <el-form-item label="权限标识">
      <el-input
        v-model="formData.permission"
        placeholder="例如: system:user:list"
        clearable
      />
    </el-form-item>

    <el-form-item label="排序号">
      <el-input-number
        v-model="formData.sortOrder"
        :min="0"
        :max="9999"
        controls-position="right"
      />
    </el-form-item>

    <el-form-item label="显示状态">
      <el-radio-group v-model="formData.visible">
        <el-radio :label="0">显示</el-radio>
        <el-radio :label="1">隐藏</el-radio>
      </el-radio-group>
    </el-form-item>

    <el-form-item label="菜单状态">
      <el-radio-group v-model="formData.status">
        <el-radio :label="0">正常</el-radio>
        <el-radio :label="1">禁用</el-radio>
      </el-radio-group>
    </el-form-item>
  </el-form>

  <!-- 图标选择器 -->
  <IconSelect ref="iconSelectorRef" @change="handleIconChange" />

  <!-- 组件选择器 -->
  <ComponentSelector ref="componentSelectorRef" @change="handleComponentChange" />
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { Picture, FolderOpened, Refresh, InfoFilled } from "@element-plus/icons-vue";
import type { FormInstance, FormRules } from "element-plus";
import { getMenuTree } from "@/api/menu";
import { getIconComponent } from "@/utils/icon";
import IconSelect from "@/components/IconSelect.vue";
import ComponentSelector from "@/components/ComponentSelector.vue";

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
 * 菜单表单组件
 */

const props = defineProps<{
  modelValue: MenuData;
  menuTreeData: any[];
}>();

const emit = defineEmits<{
  "update:modelValue": [value: MenuData];
}>();

// 表单ref
const formRef = ref<FormInstance>();

// 图标选择器ref
const iconSelectorRef = ref();

// 组件选择器ref
const componentSelectorRef = ref();

// 表单数据 - 使用ref存储内部状态
const formData = ref<MenuData>({ ...props.modelValue });

// 监听外部数据变化
watch(
  () => props.modelValue,
  (newValue) => {
    formData.value = { ...newValue };
  },
  { deep: true, immediate: true }
);

// 监听内部数据变化，同步到外部
watch(
  formData,
  (newValue) => {
    emit("update:modelValue", { ...newValue });
  },
  { deep: true }
);

// 表单验证规则
const formRules: FormRules = {
  menuName: [
    { required: true, message: "请输入菜单名称", trigger: "blur" }
  ],
  menuType: [
    { required: true, message: "请选择菜单类型", trigger: "change" }
  ]
};

/**
 * 获取图标组件
 */
const getIcon = (iconName: string | undefined) => {
  return getIconComponent(iconName);
};

/**
 * 打开图标选择器
 */
const openIconSelector = () => {
  iconSelectorRef.value?.open();
};

/**
 * 图标选择回调
 */
const handleIconChange = (icon: string | undefined) => {
  formData.value.icon = icon || "";
};

/**
 * 打开组件选择器
 */
const openComponentSelector = () => {
  componentSelectorRef.value?.open();
};

/**
 * 组件选择回调
 */
const handleComponentChange = (component: string | undefined) => {
  formData.value.component = component || "";
  // 自动生成路由路径
  if (component) {
    generatePathFromComponent();
  }
};

/**
 * 根据组件路径自动生成路由路径
 */
const generatePathFromComponent = () => {
  const component = formData.value.component;
  if (!component) {
    ElMessage.warning("请先选择组件");
    return;
  }

  // 规则：
  // 1. 去掉 views/ 前缀
  // 2. 去掉 /index.vue 后缀
  // 3. 在前面加 /

  let path = component
    .replace(/^views\//, "")          // 去掉 views/ 前缀
    .replace(/\/index\.vue$/, "")     // 去掉 /index.vue 后缀
    .replace(/\.vue$/, "");           // 去掉 .vue 后缀（自定义组件可能没有 index）

  // 确保以 / 开头
  if (!path.startsWith("/")) {
    path = "/" + path;
  }

  formData.value.path = path;

  ElMessage.success("路由路径已自动生成");
};

/**
 * 验证表单
 */
const validate = () => {
  return formRef.value?.validate();
};

/**
 * 重置表单
 */
const resetFields = () => {
  formRef.value?.resetFields();
};

// 暴露方法
defineExpose({
  validate,
  resetFields
});
</script>

<style scoped lang="scss">
.icon-selector-wrapper,
.component-selector-wrapper {
  width: 100%;

  :deep(.el-input-group__append) {
    padding: 0;
  }

  :deep(.el-button) {
    border: none;
  }
}

.form-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  padding: 8px 12px;
  background-color: #f0f9ff;
  border-left: 3px solid #409eff;
  border-radius: 4px;
  font-size: 12px;
  color: #606266;

  .el-icon {
    color: #409eff;
    flex-shrink: 0;
  }

  span {
    flex: 1;
  }
}
</style>
