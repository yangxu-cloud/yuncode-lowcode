<template>
  <div class="basic-settings">
    <div class="page-header">
      <h3>基本设置</h3>
      <p class="description">配置系统的基本信息和显示设置</p>
    </div>

    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="150px"
      class="settings-form"
    >
      <el-divider content-position="left">系统信息</el-divider>

      <el-form-item label="系统名称" prop="systemName">
        <el-input
          v-model="formData.systemName"
          placeholder="请输入系统名称"
          maxlength="50"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="系统 Logo" prop="systemLogo">
        <el-upload
          class="logo-uploader"
          :action="uploadUrl"
          :show-file-list="false"
          :on-success="handleLogoSuccess"
          :before-upload="beforeLogoUpload"
        >
          <img v-if="formData.systemLogo" :src="formData.systemLogo" class="logo" />
          <el-icon v-else class="logo-uploader-icon"><Plus /></el-icon>
        </el-upload>
        <div class="upload-tip">建议尺寸：200x50 像素，支持 PNG、JPG 格式</div>
      </el-form-item>

      <el-form-item label="系统网址" prop="systemUrl">
        <el-input
          v-model="formData.systemUrl"
          placeholder="https://example.com"
        />
      </el-form-item>

      <el-form-item label="系统描述" prop="systemDescription">
        <el-input
          v-model="formData.systemDescription"
          type="textarea"
          :rows="3"
          placeholder="请输入系统描述"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>

      <el-divider content-position="left">版权信息</el-divider>

      <el-form-item label="版权信息" prop="copyright">
        <el-input
          v-model="formData.copyright"
          placeholder="© 2025 Your Company"
        />
      </el-form-item>

      <el-form-item label="备案号" prop="icp">
        <el-input
          v-model="formData.icp"
          placeholder="京ICP备xxxxxxxx号"
        />
      </el-form-item>

      <el-divider content-position="left">区域与语言</el-divider>

      <el-form-item label="系统语言" prop="language">
        <el-select v-model="formData.language" placeholder="请选择语言">
          <el-option label="简体中文" value="zh-CN" />
          <el-option label="繁体中文" value="zh-TW" />
          <el-option label="English" value="en-US" />
          <el-option label="日本語" value="ja-JP" />
        </el-select>
      </el-form-item>

      <el-form-item label="时区" prop="timezone">
        <el-select v-model="formData.timezone" placeholder="请选择时区" filterable>
          <el-option label="UTC-12 (贝克岛)" value="Etc/GMT+12" />
          <el-option label="UTC-8 (太平洋时间)" value="Etc/GMT+8" />
          <el-option label="UTC-5 (东部时间)" value="Etc/GMT+5" />
          <el-option label="UTC+0 (伦敦)" value="Etc/GMT" />
          <el-option label="UTC+8 (北京时间)" value="Asia/Shanghai" />
          <el-option label="UTC+9 (东京)" value="Asia/Tokyo" />
        </el-select>
      </el-form-item>

      <el-divider content-position="left">日期时间格式</el-divider>

      <el-form-item label="日期格式" prop="dateFormat">
        <el-select v-model="formData.dateFormat" placeholder="请选择日期格式">
          <el-option label="YYYY-MM-DD" value="YYYY-MM-DD" />
          <el-option label="YYYY/MM/DD" value="YYYY/MM/DD" />
          <el-option label="MM/DD/YYYY" value="MM/DD/YYYY" />
          <el-option label="DD/MM/YYYY" value="DD/MM/YYYY" />
        </el-select>
      </el-form-item>

      <el-form-item label="时间格式" prop="timeFormat">
        <el-radio-group v-model="formData.timeFormat">
          <el-radio label="24h">24 小时制</el-radio>
          <el-radio label="12h">12 小时制</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="handleSave" :loading="loading">
          保存设置
        </el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, type FormInstance, type FormRules, type UploadProps } from "element-plus";
import { Plus } from "@element-plus/icons-vue";
import {
  getBasicSettings,
  saveBasicSettings,
  type BasicSettings
} from "@/api/settings";

const formRef = ref<FormInstance>();
const loading = ref(false);
const uploadUrl = "/api/file/upload";

const formData = reactive<BasicSettings>({
  systemName: "",
  systemLogo: "",
  systemUrl: "",
  systemDescription: "",
  copyright: "",
  icp: "",
  language: "zh-CN",
  timezone: "Asia/Shanghai",
  dateFormat: "YYYY-MM-DD",
  timeFormat: "24h"
});

const rules: FormRules = {
  systemName: [
    { required: true, message: "请输入系统名称", trigger: "blur" },
    { min: 2, max: 50, message: "长度在 2 到 50 个字符", trigger: "blur" }
  ],
  systemUrl: [
    { type: "url", message: "请输入正确的网址", trigger: "blur" }
  ],
  language: [
    { required: true, message: "请选择语言", trigger: "change" }
  ],
  timezone: [
    { required: true, message: "请选择时区", trigger: "change" }
  ]
};

// 加载设置
const loadSettings = async () => {
  try {
    const data = await getBasicSettings();
    Object.assign(formData, data);
  } catch (error) {
    console.error("加载设置失败:", error);
  }
};

// 保存设置
const handleSave = async () => {
  try {
    await formRef.value?.validate();
    loading.value = true;

    await saveBasicSettings(formData);
    ElMessage.success("保存成功");
  } catch (error: any) {
    console.error("保存失败:", error);
    if (error !== false) {
      ElMessage.error(error.message || "保存失败");
    }
  } finally {
    loading.value = false;
  }
};

// 重置
const handleReset = () => {
  loadSettings();
  ElMessage.info("已重置");
};

// Logo 上传成功
const handleLogoSuccess: UploadProps["onSuccess"] = (response) => {
  formData.systemLogo = response.url;
  ElMessage.success("上传成功");
};

// 上传前校验
const beforeLogoUpload: UploadProps["beforeUpload"] = (rawFile) => {
  const isImage = rawFile.type.startsWith("image/");
  const isLt2M = rawFile.size / 1024 / 1024 < 2;

  if (!isImage) {
    ElMessage.error("只能上传图片文件!");
    return false;
  }
  if (!isLt2M) {
    ElMessage.error("图片大小不能超过 2MB!");
    return false;
  }
  return true;
};

onMounted(() => {
  loadSettings();
});
</script>

<style scoped lang="scss">
.basic-settings {
  .page-header {
    margin-bottom: 30px;

    h3 {
      margin: 0 0 10px 0;
      font-size: 20px;
      font-weight: 500;
      color: #303133;
    }

    .description {
      margin: 0;
      font-size: 14px;
      color: #909399;
    }
  }

  .settings-form {
    max-width: 800px;

    :deep(.el-divider__text) {
      font-weight: 500;
      color: #606266;
    }
  }

  .logo-uploader {
    :deep(.el-upload) {
      border: 1px dashed #d9d9d9;
      border-radius: 6px;
      cursor: pointer;
      position: relative;
      overflow: hidden;
      transition: all 0.3s;
      width: 200px;
      height: 50px;
      display: flex;
      align-items: center;
      justify-content: center;

      &:hover {
        border-color: #409eff;
      }
    }
  }

  .logo {
    width: 200px;
    height: 50px;
    object-fit: contain;
  }

  .logo-uploader-icon {
    font-size: 28px;
    color: #8c939d;
  }

  .upload-tip {
    margin-top: 8px;
    font-size: 12px;
    color: #909399;
  }

  :deep(.el-select) {
    width: 100%;
  }
}
</style>
