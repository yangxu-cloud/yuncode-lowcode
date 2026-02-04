<template>
  <el-drawer v-model="visible" title="选择用户" size="700px" @close="handleClose">
    <!-- 搜索区域 -->
    <div class="search-area">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索用户名或姓名..."
        clearable
        @input="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <!-- 用户列表 -->
    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="filteredUsers"
      @selection-change="handleSelectionChange"
      height="calc(100vh - 300px)"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="userName" label="用户名" width="150" />
      <el-table-column prop="realName" label="姓名" width="150" />
      <el-table-column prop="deptName" label="部门" />
    </el-table>

    <!-- 底部操作按钮 -->
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleConfirm" :disabled="selectedUsers.length === 0">
        确定（已选 {{ selectedUsers.length }} 人）
      </el-button>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { ElMessage } from "element-plus";
import { Search } from "@element-plus/icons-vue";
import { getUserList } from "@/api/user";

/**
 * 用户选择组件
 */

interface User {
  userId: number;
  userName: string;
  realName: string;
  deptName?: string;
}

interface Props {
  modelValue?: number[];
}

interface Emits {
  (e: "update:modelValue", value: number[] | undefined): void;
  (e: "confirm", users: User[]): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

// 对话框显示状态
const visible = ref(false);

// 加载状态
const loading = ref(false);

// 搜索关键词
const searchKeyword = ref("");

// 用户列表
const userList = ref<User[]>([]);

// 已选择的用户
const selectedUsers = ref<User[]>([]);

// 过滤后的用户列表
const filteredUsers = computed(() => {
  if (!searchKeyword.value) {
    return userList.value;
  }
  const keyword = searchKeyword.value.toLowerCase();
  return userList.value.filter(
    user =>
      user.userName.toLowerCase().includes(keyword) ||
      user.realName.toLowerCase().includes(keyword)
  );
});

/**
 * 打开对话框
 */
const open = async (excludeIds?: number[]) => {
  visible.value = true;
  searchKeyword.value = "";
  selectedUsers.value = [];

  // 加载用户列表
  await loadUsers(excludeIds);
};

/**
 * 加载用户列表
 */
const loadUsers = async (excludeIds?: number[]) => {
  try {
    loading.value = true;
    // TODO: 调用实际的 API
    // const data = await getUserList({});

    // 模拟数据
    const mockData: User[] = [
      { userId: 1, userName: "admin", realName: "系统管理员", deptName: "技术部" },
      { userId: 2, userName: "zhangsan", realName: "张三", deptName: "产品部" },
      { userId: 3, userName: "lisi", realName: "李四", deptName: "研发部" },
      { userId: 4, userName: "wangwu", realName: "王五", deptName: "测试部" },
      { userId: 5, userName: "zhaoliu", realName: "赵六", deptName: "运营部" },
      { userId: 6, userName: "sunqi", realName: "孙七", deptName: "市场部" }
    ];

    // 排除已选择的用户
    if (excludeIds && excludeIds.length > 0) {
      userList.value = mockData.filter(user => !excludeIds.includes(user.userId));
    } else {
      userList.value = mockData;
    }
  } catch (error) {
    console.error("加载用户列表失败:", error);
    ElMessage.error("加载用户列表失败");
  } finally {
    loading.value = false;
  }
};

/**
 * 搜索处理
 */
const handleSearch = () => {
  // 搜索逻辑在 computed 中处理
};

/**
 * 选择变化
 */
const handleSelectionChange = (selection: User[]) => {
  selectedUsers.value = selection;
};

/**
 * 确认
 */
const handleConfirm = () => {
  if (selectedUsers.value.length === 0) {
    ElMessage.warning("请至少选择一个用户");
    return;
  }

  const userIds = selectedUsers.value.map(u => u.userId);
  emit("update:modelValue", userIds);
  emit("confirm", selectedUsers.value);
  handleClose();
};

/**
 * 关闭
 */
const handleClose = () => {
  visible.value = false;
  searchKeyword.value = "";
  selectedUsers.value = [];
};

// 暴露方法
defineExpose({
  open
});
</script>

<style scoped lang="scss">
.search-area {
  margin-bottom: 16px;
}

:deep(.el-table) {
  border: 1px solid #e4e7ed;
}
</style>
