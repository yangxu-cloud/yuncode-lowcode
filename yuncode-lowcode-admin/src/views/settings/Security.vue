<template>
  <div class="security-settings">
    <div class="page-header">
      <h3>安全设置</h3>
      <p class="description">配置密码策略和登录安全策略</p>
    </div>

    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="180px"
      class="settings-form"
    >
      <el-divider content-position="left">
        <el-icon><Lock /></el-icon>
        密码策略
      </el-divider>

      <el-form-item label="最小密码长度" prop="passwordPolicy.minLength">
        <el-input-number
          v-model="formData.passwordPolicy.minLength"
          :min="6"
          :max="20"
        />
        <span class="unit">个字符</span>
      </el-form-item>

      <el-form-item label="必须包含大写字母" prop="passwordPolicy.requireUppercase">
        <el-switch v-model="formData.passwordPolicy.requireUppercase" />
        <span class="tip">密码中必须包含至少一个大写字母 (A-Z)</span>
      </el-form-item>

      <el-form-item label="必须包含小写字母" prop="passwordPolicy.requireLowercase">
        <el-switch v-model="formData.passwordPolicy.requireLowercase" />
        <span class="tip">密码中必须包含至少一个小写字母 (a-z)</span>
      </el-form-item>

      <el-form-item label="必须包含数字" prop="passwordPolicy.requireNumber">
        <el-switch v-model="formData.passwordPolicy.requireNumber" />
        <span class="tip">密码中必须包含至少一个数字 (0-9)</span>
      </el-form-item>

      <el-form-item label="必须包含特殊字符" prop="passwordPolicy.requireSpecial">
        <el-switch v-model="formData.passwordPolicy.requireSpecial" />
        <span class="tip">密码中必须包含至少一个特殊字符 (!@#$%^&*)</span>
      </el-form-item>

      <el-form-item label="密码过期天数" prop="passwordPolicy.expireDays">
        <el-input-number
          v-model="formData.passwordPolicy.expireDays"
          :min="0"
          :max="365"
        />
        <span class="unit">天（设置为 0 表示永不过期）</span>
      </el-form-item>

      <el-divider content-position="left">
        <el-icon><User /></el-icon>
        登录策略
      </el-divider>

      <el-form-item label="最大失败次数" prop="loginPolicy.maxAttempts">
        <el-input-number
          v-model="formData.loginPolicy.maxAttempts"
          :min="3"
          :max="10"
        />
        <span class="unit">次（超过后账号将被锁定）</span>
      </el-form-item>

      <el-form-item label="锁定时长" prop="loginPolicy.lockDuration">
        <el-input-number
          v-model="formData.loginPolicy.lockDuration"
          :min="5"
          :max="1440"
        />
        <span class="unit">分钟</span>
      </el-form-item>

      <el-form-item label="会话超时" prop="loginPolicy.sessionTimeout">
        <el-input-number
          v-model="formData.loginPolicy.sessionTimeout"
          :min="30"
          :max="10080"
        />
        <span class="unit">分钟（超过后需要重新登录）</span>
      </el-form-item>

      <el-form-item label="启用验证码" prop="loginPolicy.enableCaptcha">
        <el-switch v-model="formData.loginPolicy.enableCaptcha" />
        <span class="tip">登录时需要输入图形验证码</span>
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
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import { Lock, User } from "@element-plus/icons-vue";
import {
  getSecuritySettings,
  saveSecuritySettings,
  type SecuritySettings
} from "@/api/settings";

const formRef = ref<FormInstance>();
const loading = ref(false);

const formData = reactive<SecuritySettings>({
  passwordPolicy: {
    minLength: 8,
    requireUppercase: true,
    requireLowercase: true,
    requireNumber: true,
    requireSpecial: false,
    expireDays: 90
  },
  loginPolicy: {
    maxAttempts: 5,
    lockDuration: 30,
    sessionTimeout: 120,
    enableCaptcha: true
  }
});

const rules: FormRules = {
  "passwordPolicy.minLength": [
    { required: true, message: "请设置最小密码长度", trigger: "blur" }
  ],
  "passwordPolicy.expireDays": [
    { required: true, message: "请设置密码过期天数", trigger: "blur" }
  ],
  "loginPolicy.maxAttempts": [
    { required: true, message: "请设置最大失败次数", trigger: "blur" }
  ],
  "loginPolicy.lockDuration": [
    { required: true, message: "请设置锁定时长", trigger: "blur" }
  ],
  "loginPolicy.sessionTimeout": [
    { required: true, message: "请设置会话超时时间", trigger: "blur" }
  ]
};

// 加载设置
const loadSettings = async () => {
  try {
    const data = await getSecuritySettings();
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

    await saveSecuritySettings(formData);
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

onMounted(() => {
  loadSettings();
});
</script>

<style scoped lang="scss">
.security-settings {
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
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 500;
      color: #606266;
    }

    .unit {
      margin-left: 10px;
      color: #909399;
      font-size: 14px;
    }

    .tip {
      margin-left: 10px;
      color: #909399;
      font-size: 13px;
    }
  }
}
</style>
