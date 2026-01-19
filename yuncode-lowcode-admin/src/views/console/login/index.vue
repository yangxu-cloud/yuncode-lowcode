<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="card-header">
          <h2>Yuncode LowCode</h2>
          <p class="subtitle">系统运维管理平台</p>
        </div>
      </template>

      <el-form
        ref="adminFormRef"
        :model="adminForm"
        :rules="adminRules"
        label-width="80px"
        class="login-form"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="adminForm.username"
            placeholder="请输入管理员用户名"
            clearable
            @keyup.enter="handleAdminLogin"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="adminForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
            clearable
            @keyup.enter="handleAdminLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            @click="handleAdminLogin"
            :loading="loading"
            style="width: 100%"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <el-divider></el-divider>
        <p class="tip">管理员登录用于系统运维和管理</p>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import type { FormInstance, FormRules } from "element-plus";
import { useUserStore } from "@/stores/user";
import { ElMessage } from "element-plus";

const router = useRouter();
const userStore = useUserStore();
const loading = ref(false);

// 表单引用
const adminFormRef = ref<FormInstance>();

// 管理员登录表单
const adminForm = reactive({
  username: "admin",
  password: "admin123"
});

// 表单验证规则
const adminRules: FormRules = {
  username: [
    { required: true, message: "请输入管理员用户名", trigger: "blur" },
    { min: 3, max: 20, message: "用户名长度为 3-20 个字符", trigger: "blur" }
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, max: 20, message: "密码长度为 6-20 个字符", trigger: "blur" }
  ]
};

// 管理员登录
const handleAdminLogin = async () => {
  try {
    await adminFormRef.value?.validate();
    loading.value = true;

    await userStore.adminLogin({
      username: adminForm.username,
      password: adminForm.password
    });

    ElMessage.success("管理员登录成功");
    // 跳转到管理后台首页
    router.push("/layout");
  } catch (error: any) {
    console.error("管理员登录失败:", error);
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped lang="scss">
.login-container {
  width: 100%;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
}

.login-card {
  width: 420px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  border-radius: 12px;
}

.card-header {
  text-align: center;

  h2 {
    margin: 0;
    color: #409eff;
    font-size: 24px;
    padding: 10px 0;
  }

  .subtitle {
    margin: 0;
    color: #909399;
    font-size: 14px;
    padding-bottom: 10px;
  }
}

.login-form {
  padding: 20px 0;
}

.login-footer {
  margin-top: 20px;

  .tip {
    text-align: center;
    color: #909399;
    font-size: 13px;
    margin: 10px 0 0 0;
  }
}
</style>
