<template>
  <div class="basic-settings">
    <div class="page-header">
      <h3>{{ $t('settings.basicConfig') }}</h3>
      <p class="description">{{ $t('settings.basicDesc') }}</p>
    </div>

    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="150px"
      class="settings-form"
    >
      <el-divider content-position="left">{{ $t('settings.systemInfo') }}</el-divider>

      <el-form-item :label="$t('settings.systemName')" prop="systemName">
        <el-input
          v-model="formData.systemName"
          :placeholder="$t('settings.inputSystemName')"
          maxlength="50"
          show-word-limit
        />
      </el-form-item>

      <el-form-item :label="$t('settings.systemLogo')" prop="systemLogo">
        <el-input
          v-model="formData.systemLogo"
          :placeholder="$t('settings.inputSystemLogo')"
        />
      </el-form-item>

      <el-form-item :label="$t('settings.systemUrl')" prop="systemUrl">
        <el-input
          v-model="formData.systemUrl"
          :placeholder="$t('settings.inputSystemUrl')"
        />
      </el-form-item>

      <el-form-item :label="$t('settings.systemDescription')" prop="systemDescription">
        <el-input
          v-model="formData.systemDescription"
          type="textarea"
          :rows="3"
          :placeholder="$t('settings.inputSystemDescription')"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>

      <el-divider content-position="left">{{ $t('settings.copyrightInfo') }}</el-divider>

      <el-form-item :label="$t('settings.copyright')" prop="copyright">
        <el-input
          v-model="formData.copyright"
          :placeholder="$t('settings.inputCopyright')"
        />
      </el-form-item>

      <el-form-item :label="$t('settings.icp')" prop="icp">
        <el-input
          v-model="formData.icp"
          :placeholder="$t('settings.inputIcp')"
        />
      </el-form-item>

      <el-divider content-position="left">{{ $t('settings.regionLanguage') }}</el-divider>

      <el-form-item :label="$t('settings.language')" prop="language">
        <el-select v-model="formData.language" :placeholder="$t('settings.pleaseSelect')">
          <el-option :label="$t('settings.langZhCN')" value="zh-CN" />
          <el-option :label="$t('settings.langZhTW')" value="zh-TW" />
          <el-option :label="$t('settings.langEnUS')" value="en-US" />
          <el-option :label="$t('settings.langJaJP')" value="ja-JP" />
        </el-select>
      </el-form-item>

      <el-form-item :label="$t('settings.timezone')" prop="timezone">
        <el-select v-model="formData.timezone" :placeholder="$t('settings.pleaseSelect')" filterable>
          <el-option :label="$t('settings.utc12')" value="Etc/GMT+12" />
          <el-option :label="$t('settings.utc8')" value="Etc/GMT+8" />
          <el-option :label="$t('settings.utc5')" value="Etc/GMT+5" />
          <el-option :label="$t('settings.utc0')" value="Etc/GMT" />
          <el-option :label="$t('settings.utc8Beijing')" value="Asia/Shanghai" />
          <el-option :label="$t('settings.utc9Tokyo')" value="Asia/Tokyo" />
        </el-select>
      </el-form-item>

      <el-divider content-position="left">{{ $t('settings.dateTimeFormat') }}</el-divider>

      <el-form-item :label="$t('settings.dateFormat')" prop="dateFormat">
        <el-select v-model="formData.dateFormat" :placeholder="$t('settings.pleaseSelect')">
          <el-option label="YYYY-MM-DD" value="YYYY-MM-DD" />
          <el-option label="YYYY/MM/DD" value="YYYY/MM/DD" />
          <el-option label="MM/DD/YYYY" value="MM/DD/YYYY" />
          <el-option label="DD/MM/YYYY" value="DD/MM/YYYY" />
        </el-select>
      </el-form-item>

      <el-form-item :label="$t('settings.timeFormat')" prop="timeFormat">
        <el-radio-group v-model="formData.timeFormat">
          <el-radio label="24h">{{ $t('settings.timeFormat24') }}</el-radio>
          <el-radio label="12h">{{ $t('settings.timeFormat12') }}</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-divider content-position="left">开发者选项</el-divider>

      <el-form-item label="数据源模式" prop="dataSource">
        <el-radio-group v-model="dataSource" @change="handleDataSourceChange">
          <el-radio label="api">真实 API</el-radio>
          <el-radio label="mock">Mock 数据</el-radio>
        </el-radio-group>
        <div class="form-tip">
          <el-icon><InfoFilled /></el-icon>
          <span>切换数据源模式将刷新页面，当前模式：{{ dataSource === 'mock' ? 'Mock 数据' : '真实 API' }}</span>
        </div>
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
import { ref, reactive, onMounted, computed } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import { InfoFilled } from '@element-plus/icons-vue';
import {
  getBasicSettings,
  saveBasicSettings,
  type BasicSettings
} from "@/api/settings";
import { getDataSource, setDataSource, type DataSource } from "@/config/app";

const { t } = useI18n();
const formRef = ref<FormInstance>();
const loading = ref(false);

// 数据源模式
const dataSource = ref<DataSource>(getDataSource());

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
    { required: true, message: t('settings.systemNameRequired'), trigger: "blur" },
    { min: 2, max: 50, message: t('settings.systemNameLength'), trigger: "blur" }
  ],
  systemUrl: [
    { type: "url", message: t('settings.urlError'), trigger: "blur" }
  ],
  language: [
    { required: true, message: t('settings.languageRequired'), trigger: "change" }
  ],
  timezone: [
    { required: true, message: t('settings.timezoneRequired'), trigger: "change" }
  ]
};

// 加载设置
const loadSettings = async () => {
  try {
    const data = await getBasicSettings();
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

    await saveBasicSettings(formData);
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

// 切换数据源
const handleDataSourceChange = async (newSource: DataSource) => {
  const newLabel = newSource === 'mock' ? 'Mock 数据' : '真实 API';

  try {
    await ElMessageBox.confirm(
      `切换到 ${newLabel} 将刷新页面，是否继续？`,
      '切换数据源',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    );

    setDataSource(newSource);
  } catch {
    // 用户取消，恢复原值
    dataSource.value = newSource === 'mock' ? 'api' : 'mock';
  }
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

  :deep(.el-select) {
    width: 100%;
  }

  .form-tip {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-top: 8px;
    padding: 8px 12px;
    background-color: #f0f9ff;
    border-left: 3px solid #409eff;
    border-radius: 4px;
    font-size: 12px;
    color: #606266;

    .el-icon {
      color: #409eff;
    }
  }
}
</style>
