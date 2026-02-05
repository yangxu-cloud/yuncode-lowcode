<template>
  <el-drawer
    v-model="visible"
    :title="drawerTitle"
    size="500px"
    @closed="handleClosed"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      label-position="right"
    >
      <!-- 角色名称 -->
      <el-form-item label="角色名称" prop="roleName">
        <el-input
          v-model="formData.roleName"
          placeholder="请输入角色名称"
          clearable
        />
      </el-form-item>

      <!-- 角色编码 -->
      <el-form-item label="角色编码" prop="roleCode">
        <el-input
          v-model="formData.roleCode"
          placeholder="请输入角色编码（英文）"
          clearable
          :disabled="isEdit"
        />
        <div class="form-tip">
          <el-icon><InfoFilled /></el-icon>
          <span>角色编码创建后不可修改，建议使用英文</span>
        </div>
      </el-form-item>

      <!-- 所属分类（仅在创建角色时显示） -->
      <el-form-item
        v-if="!isEdit && formData.roleType === 2"
        label="所属分类"
        prop="categoryName"
      >
        <el-autocomplete
          v-model="formData.categoryName"
          :fetch-suggestions="queryCategorySuggestions"
          placeholder="请选择或输入分类名称"
          clearable
          :disabled="isCategoryPreSelected"
          style="width: 100%"
          :trigger-on-focus="true"
          select-when-unmatched
        >
          <template #default="{ item }">
            <div class="category-suggestion">
              <span>{{ item.value }}</span>
              <el-tag v-if="item.isNew" size="small" type="success" style="margin-left: 8px">新建</el-tag>
            </div>
          </template>
        </el-autocomplete>
        <div class="form-tip">
          <el-icon><InfoFilled /></el-icon>
          <span>{{ isCategoryPreSelected ? "已从分类节点添加，分类不可更改" : "可从列表选择，也可输入新分类名（将自动创建）" }}</span>
        </div>
      </el-form-item>

      <!-- 描述 -->
      <el-form-item label="描述">
        <el-input
          v-model="formData.description"
          type="textarea"
          :rows="3"
          placeholder="请输入角色描述"
          clearable
        />
      </el-form-item>

      <!-- 排序 -->
      <el-form-item label="排序">
        <el-input-number
          v-model="formData.sortOrder"
          :min="0"
          :max="9999"
          controls-position="right"
          style="width: 100%"
        />
      </el-form-item>

      <!-- 状态（仅具体角色显示） -->
      <el-form-item v-if="formData.roleType === 2" label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio :label="0">正常</el-radio>
          <el-radio :label="1">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
        {{ isEdit ? "保存" : "创建" }}
      </el-button>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from "vue";
import { ElMessage } from "element-plus";
import { InfoFilled } from "@element-plus/icons-vue";
import type { FormInstance, FormRules } from "element-plus";
import { createRole, updateRole, type RoleForm } from "@/api/role";

/**
 * 角色表单对话框
 */

interface Props {
  modelValue?: boolean;
  role?: any;
  categories?: any[];
}

interface Emits {
  (e: "update:modelValue", value: boolean): void;
  (e: "success"): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

// 表单引用
const formRef = ref<FormInstance>();

// 对话框显示状态
const visible = computed({
  get: () => props.modelValue ?? false,
  set: (val) => emit("update:modelValue", val)
});

// 是否为编辑模式
const isEdit = computed(() => !!props.role?.id);

// 是否为创建分类模式（根据 roleType 判断）
const isCreatingCategory = computed(() => !isEdit.value && formData.roleType === 1);

// 是否分类已预选（从分类节点添加角色时，分类字段只读）
const isCategoryPreSelected = computed(() => {
  // 新增角色模式，且有预选的 parentId
  return !isEdit.value && formData.roleType === 2 && formData.parentId !== undefined && formData.parentId !== 0;
});

// 对话框标题
const drawerTitle = computed(() => {
  if (isEdit.value) {
    // 编辑模式：根据传入的角色类型判断
    return props.role?.roleType === 1 ? "编辑分类" : "编辑角色";
  } else {
    // 新增模式：根据 roleType 判断
    return formData.roleType === 1 ? "新增分类" : "新增角色";
  }
});

// 提交加载状态
const submitLoading = ref(false);

// 表单数据
const formData = reactive<RoleForm & { categoryName?: string }>({
  parentId: undefined,
  categoryName: "",
  roleName: "",
  roleCode: "",
  roleType: 2,
  description: "",
  sortOrder: 0,
  status: 0
});

// 表单验证规则
const formRules: FormRules = {
  roleName: [
    { required: true, message: "请输入角色名称", trigger: "blur" },
    { min: 2, max: 50, message: "角色名称长度在 2 到 50 个字符", trigger: "blur" }
  ],
  roleCode: [
    { required: true, message: "请输入角色编码", trigger: "blur" },
    {
      pattern: /^[a-zA-Z0-9_]+$/,
      message: "角色编码只能包含字母、数字和下划线",
      trigger: "blur"
    },
    { min: 2, max: 50, message: "角色编码长度在 2 到 50 个字符", trigger: "blur" }
  ],
  categoryName: [
    {
      required: true,
      message: "请选择或输入所属分类",
      trigger: "change",
      validator: (_rule, value, callback) => {
        // 只在创建角色时（roleType === 2）才需要验证 categoryName
        if (formData.roleType === 2 && !value) {
          callback(new Error("请选择或输入所属分类"));
        } else {
          callback();
        }
      }
    }
  ]
};

/**
 * 重置表单
 */
const resetForm = () => {
  Object.assign(formData, {
    parentId: undefined,
    categoryName: "",
    roleName: "",
    roleCode: "",
    roleType: 2,
    description: "",
    sortOrder: 0,
    status: 0
  });
};

/**
 * 监听角色数据变化
 */
watch(
  () => props.role,
  (newRole) => {
    if (newRole) {
      // 编辑模式：填充表单数据
      // 如果是具体角色且有 parentId，查找分类名称
      let categoryName = "";
      if (newRole.roleType === 2 && newRole.parentId) {
        const category = props.categories?.find((c: any) => c.id === newRole.parentId);
        categoryName = category?.roleName || "";
      }

      Object.assign(formData, {
        id: newRole.id,
        parentId: newRole.parentId,
        categoryName: categoryName,
        roleName: newRole.roleName || newRole.label,
        roleCode: newRole.roleCode,
        roleType: newRole.roleType || 2,
        description: newRole.description,
        sortOrder: newRole.sortOrder || 0,
        status: newRole.status ?? 0
      });
    } else {
      // 新增模式：重置表单
      resetForm();
    }
  },
  { immediate: true }
);

/**
 * 查询分类建议
 */
interface CategorySuggestion {
  value: string;
  isNew?: boolean;
  categoryId?: number;
}

const queryCategorySuggestions = (queryString: string, cb: (suggestions: CategorySuggestion[]) => void) => {
  const categoryList = props.categories || [];

  if (!queryString) {
    // 如果没有输入，返回所有分类
    const suggestions: CategorySuggestion[] = categoryList.map((cat: any) => ({
      value: cat.roleName || "",
      categoryId: cat.id
    }));
    cb(suggestions);
    return;
  }

  // 查找匹配的分类
  const matchedCategories = categoryList.filter((cat: any) =>
    cat.roleName?.toLowerCase().includes(queryString.toLowerCase())
  );

  const suggestions: CategorySuggestion[] = matchedCategories.map((cat: any) => ({
    value: cat.roleName || "",
    categoryId: cat.id
  }));

  // 如果没有精确匹配，添加输入的值作为新分类选项
  const exactMatch = matchedCategories.some((cat: any) =>
    cat.roleName?.toLowerCase() === queryString.toLowerCase()
  );

  if (!exactMatch && queryString.trim()) {
    suggestions.push({
      value: queryString,
      isNew: true
    } as CategorySuggestion);
  }

  cb(suggestions);
};

/**
 * 提交表单
 */
const handleSubmit = async () => {
  try {
    // 验证表单
    await formRef.value?.validate();

    submitLoading.value = true;

    if (isEdit.value) {
      // 编辑模式 - 直接更新（保持原有的 roleType 和 parentId）
      await updateRole(props.role.id, formData);
      ElMessage.success("编辑成功");
    } else {
      // 新增模式 - 根据 roleType 判断是创建分类还是角色
      if (formData.roleType === 1) {
        // 创建分类（parentId 为 0）
        const submitData = {
          ...formData,
          roleType: 1,  // 分类类型
          parentId: 0
        };
        await createRole(submitData);
        ElMessage.success("创建分类成功");
      } else {
        // 创建角色 - 需要处理分类
        let finalParentId = formData.parentId;
        const categoryList = props.categories || [];

        if (formData.categoryName) {
          // 查找分类是否存在
          const existingCategory = categoryList.find(
            (cat: any) => cat.roleName?.toLowerCase() === formData.categoryName?.toLowerCase()
          );

          if (existingCategory) {
            // 分类已存在，使用现有分类
            finalParentId = existingCategory.id;
          } else {
            // 分类不存在，先创建分类
            const categoryData: RoleForm = {
              parentId: 0,  // 顶级分类
              roleName: formData.categoryName,
              roleCode: formData.categoryName!.toLowerCase().replace(/\s+/g, '_') + '_category',
              roleType: 1,  // 分类类型
              sortOrder: 0,
              status: 0
            };

            // 创建分类并获取返回的 ID
            const categoryResponse = await createRole(categoryData);
            finalParentId = categoryResponse.data;  // 使用返回的 ID
          }
        }

        // 设置最终的数据并创建角色
        const submitData = {
          ...formData,
          roleType: 2,  // 角色类型
          parentId: finalParentId
        };
        await createRole(submitData);
        ElMessage.success("创建角色成功");
      }
    }

    // 通知父组件刷新
    emit("success");

    // 关闭对话框
    visible.value = false;
  } catch (error: any) {
    console.error("提交失败:", error);
    if (error.errors) {
      // 表单验证错误
      return;
    }
    ElMessage.error(error.message || "提交失败");
  } finally {
    submitLoading.value = false;
  }
};

/**
 * Drawer 关闭后重置表单
 */
const handleClosed = () => {
  resetForm();
};

/**
 * 关闭对话框
 */
const handleClose = () => {
  visible.value = false;
};
</script>

<style scoped lang="scss">
.form-tip {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

:deep(.el-input-number) {
  width: 100%;
}

.category-suggestion {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

</style>
