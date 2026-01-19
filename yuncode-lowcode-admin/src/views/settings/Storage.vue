<template>
  <div class="storage-settings">
    <div class="page-header">
      <h3>存储设置</h3>
      <p class="description">配置文件存储方式（本地存储或云存储）</p>
    </div>

    <el-form
      ref="formRef"
      :model="formData"
      label-width="150px"
      class="settings-form"
    >
      <el-tabs v-model="activeStorage" @tab-change="handleTabChange">
        <!-- 本地存储 -->
        <el-tab-pane label="本地存储" name="local">
          <el-form-item label="存储路径">
            <el-input v-model="formData.local.path" placeholder="/data/uploads" />
            <div class="tip">文件存储在服务器的绝对路径</div>
          </el-form-item>

          <el-form-item label="最大文件大小">
            <el-input-number v-model="formData.local.maxSize" :min="1" :max="1024" />
            <span class="unit">MB</span>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleSave" :loading="loading">保存设置</el-button>
          </el-form-item>
        </el-tab-pane>

        <!-- 阿里云 OSS -->
        <el-tab-pane label="阿里云 OSS" name="oss">
          <el-form-item label="Region">
            <el-select v-model="formData.oss.region" placeholder="请选择区域">
              <el-option label="华东1（杭州）" value="oss-cn-hangzhou" />
              <el-option label="华东2（上海）" value="oss-cn-shanghai" />
              <el-option label="华北1（青岛）" value="oss-cn-qingdao" />
              <el-option label="华北2（北京）" value="oss-cn-beijing" />
              <el-option label="华北3（张家口）" value="oss-cn-zhangjiakou" />
              <el-option label="华南1（深圳）" value="oss-cn-shenzhen" />
              <el-option label="华南2（河源）" value="oss-cn-guangzhou" />
              <el-option label="西南1（成都）" value="oss-cn-chengdu" />
            </el-select>
          </el-form-item>

          <el-form-item label="Bucket">
            <el-input v-model="formData.oss.bucket" placeholder="your-bucket-name" />
          </el-form-item>

          <el-form-item label="AccessKey ID">
            <el-input v-model="formData.oss.accessKeyId" placeholder="LTAI5txxxxx" />
          </el-form-item>

          <el-form-item label="AccessKey Secret">
            <el-input
              v-model="formData.oss.accessKeySecret"
              type="password"
              show-password
              placeholder="********************"
            />
          </el-form-item>

          <el-form-item label="自定义域名">
            <el-input v-model="formData.oss.domain" placeholder="https://cdn.example.com" />
            <div class="tip">可选，用于 CDN 加速</div>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleSave" :loading="loading">保存设置</el-button>
            <el-button @click="handleTestConnection('oss')">测试连接</el-button>
          </el-form-item>
        </el-tab-pane>

        <!-- AWS S3 -->
        <el-tab-pane label="AWS S3" name="s3">
          <el-form-item label="Region">
            <el-select v-model="formData.s3.region" placeholder="请选择区域">
              <el-option label="us-east-1 (弗吉尼亚)" value="us-east-1" />
              <el-option label="us-west-1 (加利福尼亚)" value="us-west-1" />
              <el-option label="us-west-2 (俄勒冈)" value="us-west-2" />
              <el-option label="eu-west-1 (爱尔兰)" value="eu-west-1" />
              <el-option label="eu-central-1 (法兰克福)" value="eu-central-1" />
              <el-option label="ap-southeast-1 (新加坡)" value="ap-southeast-1" />
              <el-option label="ap-northeast-1 (东京)" value="ap-northeast-1" />
              <el-option label="ap-east-1 (香港)" value="ap-east-1" />
            </el-select>
          </el-form-item>

          <el-form-item label="Bucket">
            <el-input v-model="formData.s3.bucket" placeholder="your-bucket-name" />
          </el-form-item>

          <el-form-item label="Access Key ID">
            <el-input v-model="formData.s3.accessKeyId" placeholder="AKIAIOSFODNN7EXAMPLE" />
          </el-form-item>

          <el-form-item label="Secret Access Key">
            <el-input
              v-model="formData.s3.secretAccessKey"
              type="password"
              show-password
              placeholder="********************"
            />
          </el-form-item>

          <el-form-item label="Endpoint">
            <el-input v-model="formData.s3.endpoint" placeholder="可选，用于兼容 S3 的存储" />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleSave" :loading="loading">保存设置</el-button>
            <el-button @click="handleTestConnection('s3')">测试连接</el-button>
          </el-form-item>
        </el-tab-pane>

        <!-- 腾讯云 COS -->
        <el-tab-pane label="腾讯云 COS" name="cos">
          <el-form-item label="Region">
            <el-select v-model="formData.cos.region" placeholder="请选择区域">
              <el-option label="北京" value="ap-beijing" />
              <el-option label="上海" value="ap-shanghai" />
              <el-option label="广州" value="ap-guangzhou" />
              <el-option label="成都" value="ap-chengdu" />
              <el-option label="重庆" value="ap-chongqing" />
              <el-option label="深圳" value="ap-shenzhen" />
              <el-option label="香港" value="ap-hongkong" />
              <el-option label="新加坡" value="ap-singapore" />
              <el-option label="东京" value="ap-tokyo" />
            </el-select>
          </el-form-item>

          <el-form-item label="Bucket">
            <el-input v-model="formData.cos.bucket" placeholder="your-bucket-name" />
          </el-form-item>

          <el-form-item label="Secret ID">
            <el-input v-model="formData.cos.secretId" placeholder="AKIDxxxxxxxx" />
          </el-form-item>

          <el-form-item label="Secret Key">
            <el-input
              v-model="formData.cos.secretKey"
              type="password"
              show-password
              placeholder="********************"
            />
          </el-form-item>

          <el-form-item label="自定义域名">
            <el-input v-model="formData.cos.domain" placeholder="https://cdn.example.com" />
            <div class="tip">可选，用于 CDN 加速</div>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleSave" :loading="loading">保存设置</el-button>
            <el-button @click="handleTestConnection('cos')">测试连接</el-button>
          </el-form-item>
        </el-tab-pane>
      </el-tabs>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage } from "element-plus";
import {
  getStorageSettings,
  saveStorageSettings,
  testStorageConnection,
  type StorageSettings
} from "@/api/settings";

const formRef = ref();
const loading = ref(false);
const activeStorage = ref("local");

const formData = reactive<StorageSettings>({
  type: "local",
  local: {
    path: "/data/uploads",
    maxSize: 100
  },
  oss: {
    region: "",
    bucket: "",
    accessKeyId: "",
    accessKeySecret: "",
    domain: ""
  },
  s3: {
    region: "",
    bucket: "",
    accessKeyId: "",
    secretAccessKey: "",
    endpoint: ""
  },
  cos: {
    region: "",
    bucket: "",
    secretId: "",
    secretKey: "",
    domain: ""
  }
});

// 加载设置
const loadSettings = async () => {
  try {
    const data = await getStorageSettings();
    Object.assign(formData, data);
    activeStorage.value = data.type;
  } catch (error) {
    console.error("加载设置失败:", error);
  }
};

// 保存设置
const handleSave = async () => {
  try {
    loading.value = true;
    formData.type = activeStorage.value as any;

    await saveStorageSettings(formData);
    ElMessage.success("保存成功");
  } catch (error: any) {
    console.error("保存失败:", error);
    ElMessage.error(error.message || "保存失败");
  } finally {
    loading.value = false;
  }
};

// 测试连接
const handleTestConnection = async (type: string) => {
  try {
    loading.value = true;
    const config = formData[type as keyof StorageSettings];

    await testStorageConnection(type, config);
    ElMessage.success("连接测试成功");
  } catch (error: any) {
    console.error("连接测试失败:", error);
    ElMessage.error(error.message || "连接测试失败");
  } finally {
    loading.value = false;
  }
};

// 切换标签
const handleTabChange = (name: string) => {
  console.log("切换到:", name);
};

onMounted(() => {
  loadSettings();
});
</script>

<style scoped lang="scss">
.storage-settings {
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

    .tip {
      margin-top: 8px;
      font-size: 12px;
      color: #909399;
    }

    .unit {
      margin-left: 10px;
      color: #909399;
      font-size: 14px;
    }

    :deep(.el-select) {
      width: 100%;
    }
  }
}
</style>
