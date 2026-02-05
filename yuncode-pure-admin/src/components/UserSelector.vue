<template>
  <div class="user-selector">
    <el-dialog
      v-model="dialogVisible"
      :title="title"
      width="900px"
      :close-on-click-modal="false"
      @close="handleClose"
    >
      <!-- 上部：已选用户 -->
      <div class="selected-area">
        <div class="selected-header">
          <span class="selected-label">
            已选用户 ({{ selectedUsers.length }})：
          </span>
          <el-button
            v-if="selectedUsers.length > 0"
            link
            type="danger"
            size="small"
            @click="handleClearAll"
          >
            清空
          </el-button>
        </div>
        <div class="selected-content" :class="{ 'has-content': selectedUsers.length > 0 }">
          <transition-group name="list" tag="div" class="selected-tags">
            <el-tag
              v-for="user in selectedUsers"
              :key="user.userId"
              closable
              @close="handleRemove(user)"
              class="selected-tag"
              size="small"
            >
              <el-icon style="margin-right: 4px; font-size: 13px">
                <User />
              </el-icon>
              {{ user.realName }} ({{ user.userName }})
            </el-tag>
          </transition-group>
          <el-empty
            v-if="selectedUsers.length === 0"
            description="请从下方选择用户"
            :image-size="40"
            class="empty-state"
          />
        </div>
      </div>

      <!-- 下部：左右分栏 -->
      <div class="user-selector-content">
        <!-- 左侧：组织树 -->
        <div class="org-tree-panel">
          <div class="panel-header">
            <span>组织架构</span>
            <el-input
              v-model="filterText"
              placeholder="搜索组织"
              prefix-icon="Search"
              clearable
              size="small"
              style="width: 200px"
            />
          </div>
          <div class="tree-wrapper">
            <el-tree
              ref="treeRef"
              :data="orgTreeData"
              :props="treeProps"
              :filter-node-method="filterNode"
              node-key="id"
              highlight-current
              default-expand-all
              @node-click="handleNodeClick"
            >
              <template #default="{ node, data }">
                <div class="tree-node">
                  <!-- 用户节点使用用户图标 -->
                  <el-icon v-if="data.nodeType === 'user'" color="#909399">
                    <User />
                  </el-icon>
                  <!-- 根节点使用文件夹图标 -->
                  <el-icon v-else-if="data.orgType === 0" color="#67c23a">
                    <Folder />
                  </el-icon>
                  <!-- 公司节点使用地球图标 -->
                  <el-icon v-else-if="data.orgType === 1" color="#409eff">
                    <Location />
                  </el-icon>
                  <!-- 部门节点使用办公楼图标 -->
                  <el-icon v-else color="#409eff">
                    <OfficeBuilding />
                  </el-icon>
                  <span class="node-label">{{ node.label }}</span>
                  <el-badge
                    v-if="data.userCount > 0"
                    :value="data.userCount"
                    class="node-badge"
                    type="primary"
                  />
                </div>
              </template>
            </el-tree>
          </div>
        </div>

        <!-- 右侧：用户列表 -->
        <div class="user-list-panel">
          <div class="panel-header">
            <span>{{ currentOrgName || '全部用户' }}</span>
            <el-input
              v-model="userSearchKeyword"
              placeholder="搜索用户"
              prefix-icon="Search"
              clearable
              size="small"
              style="width: 200px"
            />
          </div>
          <div class="user-list-wrapper">
            <el-empty
              v-if="filteredUserList.length === 0"
              description="暂无用户"
              :image-size="60"
            />
            <div v-else class="user-items">
              <div
                v-for="user in filteredUserList"
                :key="user.userId"
                class="user-item"
                :class="{ 'is-selected': isUserSelected(user.userId) }"
                @click="handleSelectUser(user)"
              >
                <div class="item-avatar">
                  <el-avatar :size="32" :src="user.avatar">
                    <el-icon><User /></el-icon>
                  </el-avatar>
                </div>
                <div class="item-content">
                  <div class="item-name">{{ user.realName }}</div>
                  <div class="item-code">{{ user.userName }}</div>
                </div>
                <el-icon v-if="isUserSelected(user.userId)" class="check-icon" color="#67c23a">
                  <CircleCheck />
                </el-icon>
              </div>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleConfirm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from "vue";
import { ElMessage } from "element-plus";
import {
  Search,
  User,
  OfficeBuilding,
  Folder,
  Location,
  CircleCheck
} from "@element-plus/icons-vue";
import { getOrgTree } from "@/api/org";
import { getUserList } from "@/api/user";

/**
 * 用户选择器组件
 * 支持多选模式，支持租户过滤
 * 左侧：组织树
 * 右侧：用户列表
 * 顶部：已选用户显示
 */

interface UserInfo {
  userId: number;
  userName: string;
  realName: string;
  avatar?: string;
  deptName?: string;
}

interface Props {
  modelValue?: number[];
  multiple?: boolean;
  tenantId?: number | null;
  title?: string;
  placeholder?: string;
  excludeIds?: number[]; // 排除的用户ID
}

interface Emits {
  (e: "update:modelValue", value: number[]): void;
  (e: "change", value: number[], items: UserInfo[]): void;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => [],
  multiple: true,
  tenantId: null,
  title: "选择用户",
  placeholder: "请选择用户",
  excludeIds: () => []
});

const emit = defineEmits<Emits>();

// 对话框显示状态
const dialogVisible = ref(false);

// 搜索关键词
const filterText = ref("");
const userSearchKeyword = ref("");

// 组织树数据
const orgTreeData = ref<any[]>([]);

// 用户列表
const userList = ref<UserInfo[]>([]);

// 树形组件ref
const treeRef = ref();

// 当前选中的组织
const currentOrg = ref<any>(null);
const currentOrgName = computed(() => currentOrg.value?.label || "");

// 已选用户列表
const selectedUsers = ref<UserInfo[]>([]);

// 树形配置
const treeProps = {
  children: "children",
  label: "label"
};

/**
 * 打开对话框
 */
const open = () => {
  dialogVisible.value = true;
  loadOrgTree();
  loadUserList();
  initSelectedUsers();
};

/**
 * 加载组织树
 */
const loadOrgTree = async () => {
  try {
    const tree = await getOrgTree();
    orgTreeData.value = tree;
  } catch (error: any) {
    console.error("加载组织树失败:", error);
    ElMessage.error(error.message || "加载组织树失败");
  }
};

/**
 * 从树节点递归提取所有用户节点
 */
const extractUsersFromNode = (node: any): UserInfo[] => {
  const users: UserInfo[] = [];

  // 如果是用户节点，直接添加
  if (node.nodeType === 'user') {
    users.push({
      userId: node.id || node.userId,
      userName: node.userName || node.username || '',
      realName: node.realName || node.label || '',
      avatar: node.avatar || '',
      deptName: node.deptName || ''
    });
    return users;
  }

  // 如果是组织节点，递归遍历子节点
  if (node.children && Array.isArray(node.children)) {
    for (const child of node.children) {
      users.push(...extractUsersFromNode(child));
    }
  }

  return users;
};

/**
 * 加载用户列表
 */
const loadUserList = async () => {
  try {
    let users: UserInfo[] = [];

    if (currentOrg.value) {
      // 如果选中了组织节点，提取该组织下的所有用户
      users = extractUsersFromNode(currentOrg.value);
    } else {
      // 如果没有选中组织，提取整个树中的所有用户
      for (const rootNode of orgTreeData.value) {
        users.push(...extractUsersFromNode(rootNode));
      }
    }

    // 排除已选择的用户
    userList.value = users.filter(user => !props.excludeIds.includes(user.userId));

    console.log('加载用户列表:', {
      currentOrg: currentOrg.value?.label,
      totalUsers: users.length,
      filteredUsers: userList.value.length
    });
  } catch (error: any) {
    console.error("加载用户列表失败:", error);
    ElMessage.error(error.message || "加载用户列表失败");
  }
};

/**
 * 初始化已选用户
 */
const initSelectedUsers = () => {
  const ids = props.modelValue as number[];
  selectedUsers.value = userList.value.filter(user => ids.includes(user.userId));
};

/**
 * 过滤节点
 */
const filterNode = (value: string, data: any) => {
  if (!value) return true;
  return data.label.includes(value);
};

/**
 * 判断用户是否已选
 */
const isUserSelected = (userId: number): boolean => {
  return selectedUsers.value.some(user => user.userId === userId);
};

/**
 * 节点点击
 */
const handleNodeClick = (data: any) => {
  // 如果是用户节点，添加到已选列表
  if (data.nodeType === 'user') {
    const user: UserInfo = {
      userId: data.id || data.userId,
      userName: data.userName || data.username,
      realName: data.realName || data.label,
      avatar: data.avatar
    };

    // 切换选中状态
    const index = selectedUsers.value.findIndex(u => u.userId === user.userId);
    if (index > -1) {
      // 已选中，取消选中
      selectedUsers.value.splice(index, 1);
    } else {
      // 未选中，添加选中
      selectedUsers.value.push(user);
    }
  } else {
    // 如果是组织节点，加载该组织下的用户
    currentOrg.value = data;
    loadUserList();
  }
};

/**
 * 选择用户
 */
const handleSelectUser = (user: UserInfo) => {
  const index = selectedUsers.value.findIndex(u => u.userId === user.userId);
  if (index > -1) {
    // 已选中，取消选中
    selectedUsers.value.splice(index, 1);
  } else {
    // 未选中，添加选中
    selectedUsers.value.push(user);
  }
};

/**
 * 移除已选用户
 */
const handleRemove = (user: UserInfo) => {
  const index = selectedUsers.value.findIndex(u => u.userId === user.userId);
  if (index > -1) {
    selectedUsers.value.splice(index, 1);
  }
};

/**
 * 清空所有
 */
const handleClearAll = () => {
  selectedUsers.value = [];
};

/**
 * 确定
 */
const handleConfirm = () => {
  const ids = selectedUsers.value.map(u => u.userId);
  emit("update:modelValue", ids);
  emit("change", ids, selectedUsers.value);
  handleClose();
};

/**
 * 关闭对话框
 */
const handleClose = () => {
  dialogVisible.value = false;
  filterText.value = "";
  userSearchKeyword.value = "";
  currentOrg.value = null;
};

// 过滤用户列表
const filteredUserList = computed(() => {
  if (!userSearchKeyword.value) {
    return userList.value;
  }
  const keyword = userSearchKeyword.value.toLowerCase();
  return userList.value.filter(
    user =>
      user.userName.toLowerCase().includes(keyword) ||
      user.realName.toLowerCase().includes(keyword)
  );
});

// 监听搜索关键词
watch(filterText, val => {
  if (treeRef.value) {
    treeRef.value.filter(val);
  }
});

// 暴露方法
defineExpose({
  open
});
</script>

<style scoped lang="scss">
.user-selector {
  :deep(.el-dialog__body) {
    padding: 0;
  }

  &-content {
    display: flex;
    gap: 0;
    height: 480px;
    overflow: hidden;
  }

  .panel- {
    &header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 14px 16px;
      background: linear-gradient(135deg, #f5f7fa 0%, #fafbfc 100%);
      border-bottom: 1px solid #e4e7ed;
      font-weight: 500;
      font-size: 14px;
      color: #303133;

      :deep(.el-input) {
        .el-input__wrapper {
          border-radius: 16px;
          box-shadow: 0 2px 4px rgba(0, 0, 0, 0.04);
          transition: all 0.3s;

          &:hover {
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
          }
        }
      }
    }
  }

  .org- {
    &tree- {
      &panel {
        flex: 1;
        display: flex;
        flex-direction: column;
        border-right: 1px solid #e4e7ed;
        overflow: hidden;
        background-color: #fafbfc;
      }
    }
  }

  .user- {
    &list- {
      &panel {
        flex: 1.2;
        display: flex;
        flex-direction: column;
        overflow: hidden;
        background-color: #ffffff;
      }
    }
  }

  .tree-wrapper,
  .user-list-wrapper {
    flex: 1;
    overflow-y: auto;
    padding: 16px;

    &::-webkit-scrollbar {
      width: 6px;
    }

    &::-webkit-scrollbar-thumb {
      background-color: #dcdfe6;
      border-radius: 3px;

      &:hover {
        background-color: #c0c4cc;
      }
    }
  }

  .tree-wrapper {
    background-color: #fafbfc;

    :deep(.el-tree) {
      background-color: transparent;

      .el-tree-node__content {
        border-radius: 6px;
        transition: all 0.3s;
        margin-bottom: 2px;

        &:hover {
          background-color: rgba(64, 158, 255, 0.08);
        }
      }

      .is-current > .el-tree-node__content {
        background-color: rgba(64, 158, 255, 0.15);
        font-weight: 500;
      }
    }
  }

  .tree-node {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 1;
    padding: 4px 0;

    .node- {
      &label {
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        font-size: 13px;
        color: #303133;
      }

      &badge {
        margin-left: auto;
        :deep(.el-badge__content) {
          border-radius: 10px;
          font-size: 11px;
          height: 18px;
          line-height: 18px;
          padding: 0 6px;
          min-width: 18px;
        }
      }
    }
  }

  .user- {
    &items {
      display: flex;
      flex-direction: column;
      gap: 10px;
    }

    &item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 14px 16px;
      background: linear-gradient(135deg, #ffffff 0%, #fafbfc 100%);
      border: 1px solid #e4e7ed;
      border-radius: 12px;
      cursor: pointer;
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);

      &:hover {
        border-color: #409eff;
        background: linear-gradient(135deg, #ecf5ff 0%, #d9ecff 100%);
        box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
        transform: translateY(-2px);
      }

      &.is-selected {
        border-color: #67c23a;
        background: linear-gradient(135deg, #f0f9ff 0%, #e1f3d8 100%);
        box-shadow: 0 4px 12px rgba(103, 194, 58, 0.2);
      }

      .item- {
        &avatar {
          flex-shrink: 0;

          :deep(.el-avatar) {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            border: 2px solid #ffffff;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
          }
        }

        &content {
          flex: 1;
          min-width: 0;
        }

        &name {
          font-size: 14px;
          font-weight: 500;
          color: #303133;
          margin-bottom: 2px;
        }

        &code {
          font-size: 12px;
          color: #909399;
        }
      }

      .check- {
        &icon {
          flex-shrink: 0;
          animation: checkIn 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        }
      }
    }
  }

  .selected- {
    &area {
      padding: 16px 20px;
      background: linear-gradient(135deg, #f5f7fa 0%, #fafbfc 100%);
      border-bottom: 1px solid #e4e7ed;
      height: 140px;
      display: flex;
      flex-direction: column;
    }

    &header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 10px;
      flex-shrink: 0;

      .selected-label {
        font-size: 13px;
        font-weight: 600;
        color: #303133;
        display: flex;
        align-items: center;
        gap: 6px;

        &::before {
          content: "";
          width: 3px;
          height: 14px;
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          border-radius: 2px;
        }
      }

      .el-button {
        font-size: 12px;
        padding: 4px 12px;
        height: auto;
        border-radius: 12px;
      }
    }

    &content {
      flex: 1;
      min-height: 0;
      overflow-y: hidden;
      background-color: #ffffff;
      border-radius: 8px;
      padding: 8px 12px;

      &.has-content {
        overflow-y: auto;

        &::-webkit-scrollbar {
          width: 4px;
        }

        &::-webkit-scrollbar-thumb {
          background-color: #dcdfe6;
          border-radius: 2px;
        }
      }

      .empty-state {
        height: 100%;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        color: #909399;

        :deep(.el-empty) {
          padding: 0;
          margin: 0;
        }

        :deep(.el-empty__image) {
          width: 50px;
          height: 50px;
          opacity: 0.6;
        }

        :deep(.el-empty__description) {
          margin-top: 8px;
          font-size: 12px;
          color: #909399;
        }
      }

      .selected-tags {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        align-content: flex-start;
      }
    }
  }

  .selected- {
    &tag {
      padding: 6px 10px;
      font-size: 13px;
      height: auto;
      line-height: 1.5;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border: none;
      border-radius: 8px;
      color: #ffffff;
      font-weight: 500;
      box-shadow: 0 2px 6px rgba(102, 126, 234, 0.3);
      transition: all 0.3s;

      &:hover {
        box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
        transform: translateY(-1px);
      }

      :deep(.el-icon) {
        display: inline-flex;
        align-items: center;
        font-size: 14px;
      }

      :deep(.el-tag__close) {
        color: rgba(255, 255, 255, 0.8);

        &:hover {
          color: #ffffff;
          background-color: rgba(255, 255, 255, 0.2);
          border-radius: 50%;
        }
      }
    }
  }
}

// 列表过渡动画
.list-enter-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.list-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.list-enter-from {
  opacity: 0;
  transform: scale(0.9) translateY(-10px);
}

.list-leave-to {
  opacity: 0;
  transform: scale(0.9) translateX(20px);
}

// 勾选图标动画
@keyframes checkIn {
  0% {
    opacity: 0;
    transform: scale(0);
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    opacity: 1;
    transform: scale(1);
  }
}
</style>
