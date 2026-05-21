<template>
  <el-dialog
    v-model="dialogVisible"
    :title="isEdit ? '编辑应用' : '新建应用'"
    width="600px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
    >
      <el-form-item label="应用ID" prop="appId">
        <div style="display: flex; gap: 8px; width: 100%">
          <div style="flex-shrink: 0; display: flex; align-items: center; color: #606266; font-weight: 500">
            com.yuncode.user.apps.
          </div>
          <el-input
            v-model="appIdSuffix"
            placeholder="输入后缀或点击生成"
          >
            <template #append>
              <el-button
                v-if="!isEdit"
                :icon="Refresh"
                @click="generateAppId"
              >
                生成
              </el-button>
            </template>
          </el-input>
        </div>
        <div style="color: #909399; font-size: 12px; margin-top: 4px">
          后缀格式建议：yun{年月日}{6位伪随机数}
        </div>
        <div style="color: #909399; font-size: 12px; margin-top: 2px">
          示例：yun20240227232611
        </div>
      </el-form-item>

      <el-form-item label="应用名称" prop="appName">
        <el-input
          v-model="formData.appName"
          placeholder="请输入应用名称"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="应用图标">
        <div class="icon-selector-wrapper">
          <el-input
            :model-value="getIconDisplayValue()"
            placeholder="选择图标或输入图标名称"
            readonly
            @click="openIconSelector"
          >
            <template #prepend>
              <div class="icon-preview" :style="getIconStyle()">
                <el-icon :size="18">
                  <component :is="getIconComponent(getIconName())" />
                </el-icon>
              </div>
            </template>
            <template #append>
              <el-button :icon="hasIcon() ? undefined : 'Plus'" @click.stop="openIconSelector">
                {{ hasIcon() ? '更换' : '选择' }}
              </el-button>
            </template>
          </el-input>
          <el-button
            v-if="hasIcon()"
            type="danger"
            :icon="'Delete'"
            size="small"
            circle
            class="icon-clear-btn"
            @click="clearIcon"
          >
          </el-button>
        </div>
      </el-form-item>

      <el-form-item label="版本号" prop="version">
        <el-input
          v-model="formData.version"
          placeholder="如：1.0.0"
          :disabled="!isEdit"
        />
        <div v-if="!isEdit" style="color: #909399; font-size: 12px; margin-top: 4px">
          版本号由系统自动生成
        </div>
      </el-form-item>

      <el-form-item label="应用描述" prop="appDescription">
        <el-input
          v-model="formData.appDescription"
          type="textarea"
          :rows="3"
          placeholder="请输入应用描述"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">
        确定
      </el-button>
    </template>
  </el-dialog>

  <!-- 图标选择器-->
  <IconSelector
    ref="iconSelectorRef"
    v-model="formData.appIcon"
    @change="handleIconChange"
  />
</template>

<script setup lang="ts">
import { ref, reactive, nextTick, computed } from "vue";
import { ElMessage } from "element-plus";
import { Refresh } from "@element-plus/icons-vue";
import type { FormInstance, FormRules } from "element-plus";
import {
  createApplication,
  updateApplication,
  type ApplicationForm
} from "@/api/application";
import IconSelector from "@/components/IconSelector.vue";
import * as ElementPlusIcons from "@element-plus/icons-vue";

/**
 * 应用表单对话框组件
 */

const emit = defineEmits<{
  success: [];
}>();

// 对话框显示状态
const dialogVisible = ref(false);

// 是否为编辑模式
const isEdit = ref(false);

// 表单引用
const formRef = ref<FormInstance>();

// 图标选择器引用
const iconSelectorRef = ref();

// 提交中状态
const submitting = ref(false);

// 表单数据
const formData = reactive<ApplicationForm>({
  id: undefined,
  appId: "",
  appName: "",
  appIcon: "",
  appDescription: "",
  version: ""
});

// 固定前缀
const APP_ID_PREFIX = "com.yuncode.user.apps.";

// 应用ID后缀（可编辑部分）
const appIdSuffix = computed({
  get: () => {
    if (formData.appId.startsWith(APP_ID_PREFIX)) {
      return formData.appId.substring(APP_ID_PREFIX.length);
    }
    return formData.appId;
  },
  set: (value: string) => {
    formData.appId = APP_ID_PREFIX + value;
  }
});

// 表单验证规则
const formRules: FormRules = {
  appId: [
    { required: true, message: "应用ID不能为空", trigger: "blur" },
    {
      validator: (rule: any, value: any, callback: any) => {
        if (!appIdSuffix.value) {
          callback(new Error("应用ID后缀不能为空"));
        } else if (appIdSuffix.value.length > 50) {
          callback(new Error("应用ID后缀长度不能超过50个字符"));
        } else {
          callback();
        }
      },
      trigger: "blur"
    }
  ],
  appName: [
    { required: true, message: "应用名称不能为空", trigger: "blur" },
    { min: 2, max: 200, message: "应用名称长度为2-200个字符", trigger: "blur" }
  ]
};

/**
 * 打开对话框
 */
const open = (data?: ApplicationForm) => {
  if (data && data.id) {
    // 编辑模式
    isEdit.value = true;
    Object.assign(formData, data);
    // 提取后缀（如果appId包含前缀）
    if (data.appId && data.appId.startsWith(APP_ID_PREFIX)) {
      const suffix = data.appId.substring(APP_ID_PREFIX.length);
      formData.appId = APP_ID_PREFIX + suffix;
    }
  } else {
    // 新建模式
    isEdit.value = false;
    resetForm();
    generateAppId();
  }
  dialogVisible.value = true;

  // 清除验证
  nextTick(() => {
    formRef.value?.clearValidate();
  });
};

/**
 * 生成应用ID后缀
 * 格式：yun{年月日}{6位伪随机数}
 * 示例：yun20240227232611
 */
const generateAppId = () => {
  const now = new Date();
  // 获取年月日：YYYYMMDD
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  const dateStr = `${year}${month}${day}`;

  // 生成6位伪随机数
  const random = String(Math.floor(Math.random() * 1000000)).padStart(6, '0');

  // 生成应用ID后缀
  appIdSuffix.value = `yun${dateStr}${random}`;
};

/**
 * 提交表单
 */
const handleSubmit = async () => {
  try {
    // 验证表单
    await formRef.value?.validate();

    submitting.value = true;

    // 准备提交数据
    const submitData: ApplicationForm = {
      id: formData.id,
      appId: formData.appId,
      appName: formData.appName,
      appIcon: formatIconForSubmit(formData.appIcon),
      appDescription: formData.appDescription,
      version: formData.version
    };

    if (isEdit.value) {
      // 编辑模式
      await updateApplication(submitData);
      ElMessage.success("更新成功");
    } else {
      // 新建模式
      await createApplication(submitData);
      ElMessage.success("创建成功");
    }

    emit("success");
    handleClose();
  } catch (error: any) {
    if (error !== false) {
      ElMessage.error(error.message || (isEdit.value ? "更新失败" : "创建失败"));
    }
  } finally {
    submitting.value = false;
  }
};

/**
 * 关闭对话框
 */
const handleClose = () => {
  dialogVisible.value = false;
  resetForm();
};

/**
 * 重置表单
 */
const resetForm = () => {
  formData.id = undefined;
  formData.appId = APP_ID_PREFIX;
  formData.appName = "";
  formData.appIcon = "Box"; // 默认图标
  formData.appDescription = "";
  formData.version = "1.0.0"; // 默认版本号
  formRef.value?.clearValidate();
};

/**
 * 打开图标选择器
 */
const openIconSelector = () => {
  iconSelectorRef.value?.open();
};

/**
 * 图标变化处理
 */
const handleIconChange = (value: string | { icon: string, color?: string } | undefined) => {
  if (typeof value === "string") {
    // 字符串格式：只设置图标
    formData.appIcon = value;
  } else if (value && value.icon) {
    // 对象格式：设置图标和颜色
    formData.appIcon = {
      icon: value.icon,
      color: value.color || undefined
    };
  } else {
    formData.appIcon = "";
  }
};

/**
 * 清除图标
 */
const clearIcon = () => {
  formData.appIcon = "";
};

/**
 * 获取图标组件
 */
const getIconComponent = (iconName: string) => {
  return (ElementPlusIcons as any)[iconName];
};

/**
 * 获取图标名称（支持对象格式）
 */
const getIconName = (): string => {
  if (typeof formData.appIcon === "object") {
    return formData.appIcon.icon;
  }
  return formData.appIcon;
};

/**
 * 获取图标颜色
 */
const getIconColor = (): string | undefined => {
  if (typeof formData.appIcon === "object") {
    return formData.appIcon.color;
  }
  return undefined;
};

/**
 * 获取图标显示值（用于 IconSelector 组件）
 */
const getIconDisplayValue = (): string | { icon: string, color?: string } => {
  if (typeof formData.appIcon === "object") {
    return formData.appIcon;
  }
  return formData.appIcon;
};

/**
 * 检查是否已选择图标
 */
const hasIcon = (): boolean => {
  return !!formData.appIcon;
};

/**
 * 获取图标样式（用于预览）
 */
const getIconStyle = (): Record<string, string> => {
  const color = getIconColor();
  return color ? { color } : {};
};

/**
 * 格式化图标为提交格式
 * 将对象格式转换为字符串格式，兼容后端存储
 */
const formatIconForSubmit = (icon: string | { icon: string, color?: string } | undefined): string => {
  if (!icon) {
    return "Box";
  }

  if (typeof icon === "string") {
    return icon;
  }

  // 对象格式：只返回图标名称，颜色暂不存储到数据库
  return icon.icon || "Box";
};

// 暴露方法
defineExpose({
  open
});
</script>

<style scoped lang="scss">
.icon-selector-wrapper {
  display: flex;
  gap: 8px;
  align-items: flex-start;

  .icon-preview {
    padding: 4px 12px;
    display: flex;
    align-items: center;
    min-width: 40px;
    justify-content: center;
  }

  .icon-clear-btn {
    flex-shrink: 0;
    margin-top: 4px;
  }
}
</style>
