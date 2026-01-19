<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="card-header">
          <h2>Yuncode LowCode</h2>
          <p class="subtitle">低代码开发平台</p>
        </div>
      </template>

      <!-- 登录类型切换 Tab -->
      <el-tabs v-model="activeTab" class="login-tabs" @tab-change="handleTabChange">
        <!-- 普通用户登录 -->
        <el-tab-pane label="用户登录" name="user">
          <el-form
            ref="userFormRef"
            :model="userForm"
            :rules="userRules"
            label-width="80px"
            class="login-form"
          >
            <el-form-item label="租户编码" prop="tenantCode">
              <el-input
                v-model="userForm.tenantCode"
                placeholder="请输入租户编码"
                clearable
                @keyup.enter="handleUserLogin"
              />
            </el-form-item>

            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="userForm.username"
                placeholder="请输入用户名"
                clearable
                @keyup.enter="handleUserLogin"
              />
            </el-form-item>

            <el-form-item label="密码" prop="password">
              <el-input
                v-model="userForm.password"
                type="password"
                placeholder="请输入密码"
                show-password
                clearable
                @keyup.enter="handleUserLogin"
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                @click="handleUserLogin"
                :loading="loading"
                style="width: 100%"
              >
                登录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 租户登录 -->
        <el-tab-pane label="租户登录" name="tenant">
          <el-form
            ref="tenantFormRef"
            :model="tenantForm"
            :rules="tenantRules"
            label-width="80px"
            class="login-form"
          >
            <el-form-item label="租户编码" prop="tenantCode">
              <el-input
                v-model="tenantForm.tenantCode"
                placeholder="请输入租户编码"
                clearable
                @keyup.enter="handleTenantLogin"
              />
            </el-form-item>

            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="tenantForm.username"
                placeholder="请输入用户名"
                clearable
                @keyup.enter="handleTenantLogin"
              />
            </el-form-item>

            <el-form-item label="密码" prop="password">
              <el-input
                v-model="tenantForm.password"
                type="password"
                placeholder="请输入密码"
                show-password
                clearable
                @keyup.enter="handleTenantLogin"
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                @click="handleTenantLogin"
                :loading="loading"
                style="width: 100%"
              >
                登录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <div class="login-footer">
        <el-divider></el-divider>
        <p class="tip">系统运维？请前往 <el-link type="primary" href="/console/login" target="_blank">管理员登录</el-link></p>
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
const activeTab = ref("user"); // 默认用户登录

// 表单引用
const tenantFormRef = ref<FormInstance>();
const userFormRef = ref<FormInstance>();

// 租户登录表单
const tenantForm = reactive({
  tenantCode: "default",
  username: "testuser",
  password: "admin123"
});

// 普通用户登录表单
const userForm = reactive({
  tenantCode: "default",
  username: "testuser",
  password: "admin123"
});

// 表单验证规则
const tenantRules: FormRules = {
  tenantCode: [
    { required: true, message: "请输入租户编码", trigger: "blur" },
    { pattern: /^[a-zA-Z0-9_-]+$/, message: "租户编码只能包含字母、数字、下划线和连字符" }
  ],
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { min: 3, max: 20, message: "用户名长度为 3-20 个字符", trigger: "blur" }
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, max: 20, message: "密码长度为 6-20 个字符", trigger: "blur" }
  ]
};

const userRules: FormRules = {
  tenantCode: [
    { required: true, message: "请输入租户编码", trigger: "blur" },
    { pattern: /^[a-zA-Z0-9_-]+$/, message: "租户编码只能包含字母、数字、下划线和连字符" }
  ],
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { min: 3, max: 20, message: "用户名长度为 3-20 个字符", trigger: "blur" }
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, max: 20, message: "密码长度为 6-20 个字符", trigger: "blur" }
  ]
};

// Tab 切换事件
const handleTabChange = (tabName: string) => {
  console.log("切换到登录类型:", tabName);
};

// 租户登录
const handleTenantLogin = async () => {
  try {
    await tenantFormRef.value?.validate();
    loading.value = true;

    await userStore.tenantLogin({
      tenantCode: tenantForm.tenantCode,
      username: tenantForm.username,
      password: tenantForm.password
    });

    ElMessage.success("租户登录成功");
    router.push("/layout");
  } catch (error: any) {
    console.error("租户登录失败:", error);
  } finally {
    loading.value = false;
  }
};

// 普通用户登录
const handleUserLogin = async () => {
  try {
    await userFormRef.value?.validate();
    loading.value = true;

    await userStore.userLogin({
      tenantCode: userForm.tenantCode,
      username: userForm.username,
      password: userForm.password
    });

    ElMessage.success("用户登录成功");
    router.push("/layout");
  } catch (error: any) {
    console.error("用户登录失败:", error);
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped lang="scss">
.login-container {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 480px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
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

.login-tabs {
  margin-top: 10px;

  :deep(.el-tabs__header) {
    margin-bottom: 20px;
  }

  :deep(.el-tabs__item) {
    font-size: 15px;
    font-weight: 500;
  }
}

.login-form {
  padding: 10px 0;
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
