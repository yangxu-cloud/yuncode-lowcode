<template>
  <div class="mail-settings">
    <div class="page-header">
      <h3>邮件设置</h3>
      <p class="description">配置 SMTP 邮件服务器用于发送系统通知邮件</p>
    </div>

    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="150px"
      class="settings-form"
    >
      <el-form-item label="启用邮件" prop="enabled">
        <el-switch v-model="formData.enabled" />
        <span class="tip">开启后系统将使用邮件发送通知</span>
      </el-form-item>

      <el-divider content-position="left">SMTP 服务器配置</el-divider>

      <el-form-item label="SMTP 服务器" prop="host">
        <el-input v-model="formData.host" placeholder="smtp.example.com" />
        <div class="tip">
          常用邮件服务器：
          <el-tag size="small" @click="setSmtp('smtp.qq.com')">QQ邮箱</el-tag>
          <el-tag size="small" @click="setSmtp('smtp.163.com')">163邮箱</el-tag>
          <el-tag size="small" @click="setSmtp('smtp.gmail.com')">Gmail</el-tag>
        </div>
      </el-form-item>

      <el-form-item label="端口" prop="port">
        <el-input-number v-model="formData.port" :min="1" :max="65535" />
        <span class="unit">常用端口：25（非SSL）, 465（SSL）, 587（TLS）</span>
      </el-form-item>

      <el-form-item label="发件人邮箱" prop="from">
        <el-input v-model="formData.from" placeholder="noreply@example.com" />
      </el-form-item>

      <el-form-item label="用户名" prop="user">
        <el-input v-model="formData.user" placeholder="your-email@example.com" />
      </el-form-item>

      <el-form-item label="密码" prop="pass">
        <el-input
          v-model="formData.pass"
          type="password"
          show-password
          placeholder="邮箱密码或授权码"
        />
        <div class="tip">对于 QQ、163 等邮箱，请使用授权码而非登录密码</div>
      </el-form-item>

      <el-form-item label="使用 SSL/TLS" prop="secure">
        <el-switch v-model="formData.secure" />
        <span class="tip">端口 465 和 587 通常需要启用 SSL</span>
      </el-form-item>

      <el-divider content-position="left">测试邮件</el-divider>

      <el-form-item label="测试邮箱">
        <el-input v-model="testEmail" placeholder="recipient@example.com" style="width: 300px" />
        <el-button @click="handleTestMail" :loading="testLoading" style="margin-left: 10px">
          发送测试邮件
        </el-button>
        <div class="tip">发送一封测试邮件以验证配置是否正确</div>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="handleSave" :loading="loading">保存设置</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import {
  getMailSettings,
  saveMailSettings,
  testMail,
  type MailSettings
} from "@/api/settings";

const formRef = ref<FormInstance>();
const loading = ref(false);
const testLoading = ref(false);
const testEmail = ref("");

const formData = reactive<MailSettings>({
  enabled: false,
  host: "",
  port: 465,
  from: "",
  user: "",
  pass: "",
  secure: true
});

const rules: FormRules = {
  host: [
    { required: true, message: "请输入 SMTP 服务器地址", trigger: "blur" }
  ],
  port: [
    { required: true, message: "请输入端口号", trigger: "blur" }
  ],
  from: [
    { required: true, message: "请输入发件人邮箱", trigger: "blur" },
    { type: "email", message: "请输入正确的邮箱格式", trigger: "blur" }
  ],
  user: [
    { required: true, message: "请输入用户名", trigger: "blur" }
  ],
  pass: [
    { required: true, message: "请输入密码", trigger: "blur" }
  ]
};

// 加载设置
const loadSettings = async () => {
  try {
    const data = await getMailSettings();
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

    await saveMailSettings(formData);
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

// 设置常用 SMTP
const setSmtp = (host: string) => {
  formData.host = host;

  // 根据不同 SMTP 设置常用端口
  const portMap: Record<string, number> = {
    "smtp.qq.com": 465,
    "smtp.163.com": 465,
    "smtp.gmail.com": 587
  };

  formData.port = portMap[host] || 465;
  formData.secure = true;
};

// 发送测试邮件
const handleTestMail = async () => {
  if (!testEmail.value) {
    ElMessage.warning("请输入测试邮箱地址");
    return;
  }

  try {
    testLoading.value = true;
    await testMail(testEmail.value);
    ElMessage.success("测试邮件已发送，请查收");
  } catch (error: any) {
    console.error("发送测试邮件失败:", error);
    ElMessage.error(error.message || "发送失败，请检查配置");
  } finally {
    testLoading.value = false;
  }
};

onMounted(() => {
  loadSettings();
});
</script>

<style scoped lang="scss">
.mail-settings {
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

    .tip {
      margin-top: 8px;
      font-size: 12px;
      color: #909399;

      .el-tag {
        margin-right: 8px;
        cursor: pointer;

        &:hover {
          background-color: #409eff;
          color: #fff;
        }
      }
    }

    .unit {
      margin-left: 10px;
      color: #909399;
      font-size: 14px;
    }
  }
}
</style>
