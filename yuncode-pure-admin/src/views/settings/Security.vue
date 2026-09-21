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
      label-width="160px"
      class="settings-form"
    >
      <!-- 密码策略 -->
      <div class="form-section">
        <div class="section-title">
          <el-icon><Lock /></el-icon>
          <span>{{ $t('settings.passwordPolicy') }}</span>
        </div>

        <el-form-item :label="$t('settings.minLength')" prop="passwordMinLength">
          <el-input-number
            v-model="formData.passwordMinLength"
            :min="6"
            :max="20"
          />
          <span class="unit">{{ $t('settings.characters') }}</span>
        </el-form-item>

        <el-form-item :label="$t('settings.requireUppercase')" prop="passwordRequireUppercase">
          <el-switch v-model="formData.passwordRequireUppercase" />
          <span class="tip">{{ $t('settings.requireUppercaseTip') }}</span>
        </el-form-item>

        <el-form-item :label="$t('settings.requireLowercase')" prop="passwordRequireLowercase">
          <el-switch v-model="formData.passwordRequireLowercase" />
          <span class="tip">{{ $t('settings.requireLowercaseTip') }}</span>
        </el-form-item>

        <el-form-item :label="$t('settings.requireNumber')" prop="passwordRequireNumber">
          <el-switch v-model="formData.passwordRequireNumber" />
          <span class="tip">{{ $t('settings.requireNumberTip') }}</span>
        </el-form-item>

        <el-form-item :label="$t('settings.requireSpecial')" prop="passwordRequireSpecial">
          <el-switch v-model="formData.passwordRequireSpecial" />
          <span class="tip">{{ $t('settings.requireSpecialTip') }}</span>
        </el-form-item>

        <el-form-item :label="$t('settings.passwordExpireDays')" prop="passwordExpireDays">
          <el-input-number
            v-model="formData.passwordExpireDays"
            :min="0"
            :max="365"
          />
          <span class="unit">{{ $t('settings.expireDaysZero') }}</span>
        </el-form-item>
      </div>

      <!-- 登录策略 -->
      <div class="form-section">
        <div class="section-title">
          <el-icon><User /></el-icon>
          <span>{{ $t('settings.loginPolicy') }}</span>
        </div>

        <el-form-item :label="$t('settings.maxAttempts')" prop="loginMaxAttempts">
          <el-input-number
            v-model="formData.loginMaxAttempts"
            :min="3"
            :max="10"
          />
          <span class="unit">{{ $t('settings.maxAttemptsTip') }}</span>
        </el-form-item>

        <el-form-item :label="$t('settings.lockDuration')" prop="loginLockDuration">
          <el-input-number
            v-model="formData.loginLockDuration"
            :min="5"
            :max="1440"
          />
          <span class="unit">{{ $t('settings.lockDurationUnit') }}</span>
        </el-form-item>

        <el-form-item :label="$t('settings.sessionTimeout')" prop="loginSessionTimeout">
          <el-input-number
            v-model="formData.loginSessionTimeout"
            :min="30"
            :max="10080"
          />
          <span class="unit">{{ $t('settings.sessionTimeoutUnit') }}</span>
        </el-form-item>

        <el-form-item :label="$t('settings.enableCaptcha')" prop="loginEnableCaptcha">
          <el-switch v-model="formData.loginEnableCaptcha" />
          <span class="tip">{{ $t('settings.enableCaptchaTip') }}</span>
        </el-form-item>
      </div>

      <!-- 操作按钮 -->
      <div class="form-actions">
        <el-button type="primary" @click="handleSave" :loading="loading" size="large">
          <el-icon><Check /></el-icon>
          {{ $t('settings.saveSettings') }}
        </el-button>
        <el-button @click="handleReset" size="large">
          <el-icon><RefreshRight /></el-icon>
          {{ $t('settings.resetSettings') }}
        </el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import { Lock, User, Check, RefreshRight } from "@element-plus/icons-vue";
import {
  getSecuritySettings,
  saveSecuritySettings
} from "@/api/settings";

const { t } = useI18n();
const formRef = ref<FormInstance>();
const loading = ref(false);

// 扁平结构，匹配后端 SettingsVO
const formData = reactive({
  passwordMinLength: 8,
  passwordRequireUppercase: true,
  passwordRequireLowercase: true,
  passwordRequireNumber: true,
  passwordRequireSpecial: false,
  passwordExpireDays: 90,
  loginMaxAttempts: 5,
  loginLockDuration: 30,
  loginSessionTimeout: 120,
  loginEnableCaptcha: true
});

const rules: FormRules = {
  passwordMinLength: [
    { required: true, message: t('settings.minLengthRequired'), trigger: "blur" }
  ],
  passwordExpireDays: [
    { required: true, message: t('settings.expireDaysRequired'), trigger: "blur" }
  ],
  loginMaxAttempts: [
    { required: true, message: t('settings.maxAttemptsRequired'), trigger: "blur" }
  ],
  loginLockDuration: [
    { required: true, message: t('settings.lockDurationRequired'), trigger: "blur" }
  ],
  loginSessionTimeout: [
    { required: true, message: t('settings.sessionTimeoutRequired'), trigger: "blur" }
  ]
};

const loadSettings = async () => {
  try {
    const data = await getSecuritySettings();
    if (data) {
      Object.assign(formData, data);
    }
  } catch (error) {
    console.error(t('settings.loadSettingsFailed'), error);
  }
};

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
  max-width: 900px;

  .page-header {
    margin-bottom: 32px;
    padding-bottom: 20px;
    border-bottom: 1px solid #f0f0f0;

    h3 {
      margin: 0 0 8px 0;
      font-size: 20px;
      font-weight: 600;
      color: #1d2129;
    }

    .description {
      margin: 0;
      font-size: 14px;
      color: #909399;
    }
  }

  .form-section {
    margin-bottom: 28px;
    padding: 20px;
    background: #fafafa;
    border-radius: 12px;

    .section-title {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 20px;
      font-size: 15px;
      font-weight: 600;
      color: #1d2129;

      .el-icon {
        color: #409eff;
      }
    }
  }

  .settings-form {
    :deep(.el-form-item) {
      margin-bottom: 20px;
    }

    .unit {
      margin-left: 12px;
      color: #909399;
      font-size: 13px;
    }

    .tip {
      margin-left: 12px;
      color: #909399;
      font-size: 13px;
    }
  }

  .form-actions {
    display: flex;
    gap: 12px;
    padding-top: 24px;
    border-top: 1px solid #f0f0f0;
  }
}
</style>
