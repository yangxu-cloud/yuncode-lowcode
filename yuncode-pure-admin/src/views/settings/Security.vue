<template>
  <div class="security-settings">
    <div class="page-header">
      <h3>{{ $t('settings.securityConfig') }}</h3>
      <p class="description">{{ $t('settings.securityDesc') }}</p>
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
        {{ $t('settings.passwordPolicy') }}
      </el-divider>

      <el-form-item :label="$t('settings.minLength')" prop="passwordPolicy.minLength">
        <el-input-number
          v-model="formData.passwordPolicy.minLength"
          :min="6"
          :max="20"
        />
        <span class="unit">{{ $t('settings.characters') }}</span>
      </el-form-item>

      <el-form-item :label="$t('settings.requireUppercase')" prop="passwordPolicy.requireUppercase">
        <el-switch v-model="formData.passwordPolicy.requireUppercase" />
        <span class="tip">{{ $t('settings.requireUppercaseTip') }}</span>
      </el-form-item>

      <el-form-item :label="$t('settings.requireLowercase')" prop="passwordPolicy.requireLowercase">
        <el-switch v-model="formData.passwordPolicy.requireLowercase" />
        <span class="tip">{{ $t('settings.requireLowercaseTip') }}</span>
      </el-form-item>

      <el-form-item :label="$t('settings.requireNumber')" prop="passwordPolicy.requireNumber">
        <el-switch v-model="formData.passwordPolicy.requireNumber" />
        <span class="tip">{{ $t('settings.requireNumberTip') }}</span>
      </el-form-item>

      <el-form-item :label="$t('settings.requireSpecial')" prop="passwordPolicy.requireSpecial">
        <el-switch v-model="formData.passwordPolicy.requireSpecial" />
        <span class="tip">{{ $t('settings.requireSpecialTip') }}</span>
      </el-form-item>

      <el-form-item :label="$t('settings.passwordExpireDays')" prop="passwordPolicy.expireDays">
        <el-input-number
          v-model="formData.passwordPolicy.expireDays"
          :min="0"
          :max="365"
        />
        <span class="unit">{{ $t('settings.expireDaysZero') }}</span>
      </el-form-item>

      <el-divider content-position="left">
        <el-icon><User /></el-icon>
        {{ $t('settings.loginPolicy') }}
      </el-divider>

      <el-form-item :label="$t('settings.maxAttempts')" prop="loginPolicy.maxAttempts">
        <el-input-number
          v-model="formData.loginPolicy.maxAttempts"
          :min="3"
          :max="10"
        />
        <span class="unit">{{ $t('settings.maxAttemptsTip') }}</span>
      </el-form-item>

      <el-form-item :label="$t('settings.lockDuration')" prop="loginPolicy.lockDuration">
        <el-input-number
          v-model="formData.loginPolicy.lockDuration"
          :min="5"
          :max="1440"
        />
        <span class="unit">{{ $t('settings.lockDurationUnit') }}</span>
      </el-form-item>

      <el-form-item :label="$t('settings.sessionTimeout')" prop="loginPolicy.sessionTimeout">
        <el-input-number
          v-model="formData.loginPolicy.sessionTimeout"
          :min="30"
          :max="10080"
        />
        <span class="unit">{{ $t('settings.sessionTimeoutUnit') }}</span>
      </el-form-item>

      <el-form-item :label="$t('settings.enableCaptcha')" prop="loginPolicy.enableCaptcha">
        <el-switch v-model="formData.loginPolicy.enableCaptcha" />
        <span class="tip">{{ $t('settings.enableCaptchaTip') }}</span>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="handleSave" :loading="loading">
          {{ $t('settings.saveSettings') }}
        </el-button>
        <el-button @click="handleReset">{{ $t('settings.resetSettings') }}</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import { Lock, User } from "@element-plus/icons-vue";
import {
  getSecuritySettings,
  saveSecuritySettings,
  type SecuritySettings
} from "@/api/settings";

const { t } = useI18n();
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
    { required: true, message: t('settings.minLengthRequired'), trigger: "blur" }
  ],
  "passwordPolicy.expireDays": [
    { required: true, message: t('settings.expireDaysRequired'), trigger: "blur" }
  ],
  "loginPolicy.maxAttempts": [
    { required: true, message: t('settings.maxAttemptsRequired'), trigger: "blur" }
  ],
  "loginPolicy.lockDuration": [
    { required: true, message: t('settings.lockDurationRequired'), trigger: "blur" }
  ],
  "loginPolicy.sessionTimeout": [
    { required: true, message: t('settings.sessionTimeoutRequired'), trigger: "blur" }
  ]
};

// 加载设置
const loadSettings = async () => {
  try {
    const data = await getSecuritySettings();
    Object.assign(formData, data);
  } catch (error) {
    console.error(t('settings.loadSettingsFailed'), error);
  }
};

// 保存设置
const handleSave = async () => {
  try {
    await formRef.value?.validate();
    loading.value = true;

    await saveSecuritySettings(formData);
    ElMessage.success(t('settings.saved'));
  } catch (error: any) {
    console.error(t('settings.saveFailed'), error);
    if (error !== false) {
      ElMessage.error(error.message || t('settings.saveFailed'));
    }
  } finally {
    loading.value = false;
  }
};

// 重置
const handleReset = () => {
  loadSettings();
  ElMessage.info(t('settings.resetted'));
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
