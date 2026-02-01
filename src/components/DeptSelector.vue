<template>
  <div class="dept-selector">
    <el-dialog
      v-model="dialogVisible"
      :title="title"
      width="900px"
      :close-on-click-modal="false"
      @close="handleClose"
    >
      <!-- 上部：已选部门 -->
      <div class="selected-area">
        <div class="selected-header">
          <span class="selected-label">
            已选部门 ({{ selectedDepts.length }})：
          </span>
          <el-button
            v-if="selectedDepts.length > 0"
            link
            type="danger"
            size="small"
            @click="handleClearAll"
          >
            清空
          </el-button>
        </div>
        <div class="selected-content" :class="{ 'has-content': selectedDepts.length > 0 }">
          <transition-group name="list" tag="div" class="selected-tags">
            <el-tag
              v-for="dept in selectedDepts"
              :key="dept.id"
              closable
              @close="handleRemove(dept)"
              class="selected-tag"
              size="small"
            >
              <el-icon style="margin-right: 4px; font-size: 13px">
                <OfficeBuilding v-if="dept.orgType === 1" />
                <Folder v-else />
              </el-icon>
              {{ dept.label }}
            </el-tag>
          </transition-group>
          <el-empty
            v-if="selectedDepts.length === 0"
            description="请从下方选择部门"
            :image-size="40"
            class="empty-state"
          />
        </div>
      </div>

      <!-- 下部：左右分栏 -->
      <div class="dept-selector-content">
        <!-- 左侧：部门树 -->
        <div class="dept-tree-panel">
          <div class="panel-header">
            <span>全部部门</span>
            <el-input
              v-model="filterText"
              placeholder="搜索部门"
              prefix-icon="Search"
              clearable
              size="small"
              style="width: 200px"
            />
          </div>
          <div class="tree-wrapper">
            <el-tree
              ref="treeRef"
              :data="deptTreeData"
              :props="treeProps"
              :filter-node-method="filterNode"
              :show-checkbox="multiple"
              :check-strictly="false"
              node-key="id"
              highlight-current
              default-expand-all
              @node-click="handleNodeClick"
              @check="handleCheck"
            >
              <template #default="{ node, data }">
                <div class="tree-node">
                  <el-icon v-if="data.orgType === 1" color="#409eff">
                    <OfficeBuilding />
                  </el-icon>
                  <el-icon v-else color="#67c23a">
                    <Folder />
                  </el-icon>
                  <span class="node-label">{{ node.label }}</span>
                  <span v-if="data.orgType === 1" class="node-tag">
                    <el-tag size="small" type="primary">公司</el-tag>
                  </span>
                </div>
              </template>
            </el-tree>
          </div>
        </div>

        <!-- 右侧：常用部门 -->
        <div class="dept-frequent-panel">
          <div class="panel-header">
            <span>常用部门</span>
            <el-tooltip content="点击快捷选择，再次点击取消选择" placement="top">
              <el-icon :size="16" color="#909399">
                <QuestionFilled />
              </el-icon>
            </el-tooltip>
          </div>
          <div class="frequent-list">
            <el-empty
              v-if="frequentDepts.length === 0"
              description="暂无常用部门"
              :image-size="60"
            />
            <div v-else class="frequent-items">
              <div
                v-for="dept in frequentDepts"
                :key="dept.id"
                class="frequent-item"
                :class="{ 'is-selected': isDeptSelected(dept.id) }"
                @click="handleSelectFrequentDept(dept)"
              >
                <div class="item-icon">
                  <el-icon :color="dept.orgType === 1 ? '#409eff' : '#67c23a'" :size="20">
                    <OfficeBuilding v-if="dept.orgType === 1" />
                    <Folder v-else />
                  </el-icon>
                </div>
                <div class="item-content">
                  <div class="item-name">{{ dept.label }}</div>
                  <div class="item-code">{{ dept.orgCode }}</div>
                </div>
                <el-icon v-if="isDeptSelected(dept.id)" class="check-icon" color="#67c23a">
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
  OfficeBuilding,
  Folder,
  QuestionFilled,
  CircleCheck
} from "@element-plus/icons-vue";
import { getOrgTree } from "@/api/org";

/**
 * 部门选择器组件
 * 支持单选/多选模式，支持租户过滤
 * 左侧：完整部门树
 * 右侧：常用部门快捷选择
 * 底部：已选部门显示
 */

interface Props {
  modelValue?: number | number[];
  multiple?: boolean;
  tenantId?: number | null;
  title?: string;
  placeholder?: string;
  excludeIds?: number[]; // 排除的部门ID
  frequentLimit?: number; // 常用部门数量限制
}

interface Emits {
  (e: "update:modelValue", value: number | number[]): void;
  (e: "change", value: number | number[], items: any[]): void;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: undefined,
  multiple: false,
  tenantId: null,
  title: "选择部门",
  placeholder: "请选择部门",
  excludeIds: () => [],
  frequentLimit: 6 // 默认显示6个常用部门
});

const emit = defineEmits<Emits>();

// 对话框显示状态
const dialogVisible = ref(false);

// 搜索关键词
const filterText = ref("");

// 树形数据
const deptTreeData = ref<any[]>([]);

// 常用部门列表
const frequentDepts = ref<any[]>([]);

// 树形组件ref
const treeRef = ref();

// 已选部门列表
const selectedDepts = ref<any[]>([]);

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
  loadDeptTree();
  loadFrequentDepts();
  initSelectedDepts();
};

/**
 * 加载部门树
 */
const loadDeptTree = async () => {
  try {
    const tree = await getOrgTree();

    console.log("原始组织树数据:", tree);
    console.log("props.tenantId:", props.tenantId, "类型:", typeof props.tenantId);
    console.log("props.excludeIds:", props.excludeIds);

    // 过滤掉排除的部门
    const filterTree = (nodes: any[]): any[] => {
      if (!nodes || !Array.isArray(nodes)) {
        return [];
      }

      return nodes
        .filter(node => {
          console.log("检查节点:", node.label, "nodeType:", node.nodeType, "id:", node.id, "tenantId:", node.tenantId, "parentId:", node.parentId);

          // 只显示组织节点（没有 nodeType 字段的也保留，兼容旧数据）
          if (node.nodeType && node.nodeType !== "org") {
            console.log("  -> 过滤: 非组织节点");
            return false;
          }
          // 过滤排除的部门（兼容字符串和数字类型的 ID）
          const nodeId = typeof node.id === 'string' ? parseInt(node.id) : node.id;
          const excludeIdsNum = props.excludeIds.map(id => typeof id === 'string' ? parseInt(id) : id);
          console.log("  -> nodeId:", nodeId, "excludeIdsNum:", excludeIdsNum);
          if (excludeIdsNum.includes(nodeId)) {
            console.log("  -> 过滤: 在排除列表中");
            return false;
          }

          // 判断是否为根节点（虚拟容器节点）
          const isRootNode = node.parentId === "0" || node.parentId === 0 || parseInt(node.parentId) === 0;

          // 如果指定了租户ID，只显示该租户的部门（兼容字符串和数字类型）
          // 根节点不参与租户ID过滤
          if (!isRootNode && props.tenantId !== null) {
            const nodeTenantId = typeof node.tenantId === 'string' ? parseInt(node.tenantId) : node.tenantId;
            const propsTenantId = typeof props.tenantId === 'string' ? parseInt(props.tenantId) : props.tenantId;
            console.log("  -> nodeTenantId:", nodeTenantId, "propsTenantId:", propsTenantId);
            if (nodeTenantId !== propsTenantId) {
              console.log("  -> 过滤: 租户ID不匹配");
              return false;
            }
          }

          console.log("  -> 通过");
          return true;
        })
        .map(node => ({
          ...node,
          children: node.children ? filterTree(node.children) : []
        }));
    };

    const filteredTree = filterTree(tree);
    console.log("过滤后的组织树数据:", filteredTree);

    deptTreeData.value = filteredTree;
  } catch (error: any) {
    console.error("加载部门树失败:", error);
    ElMessage.error(error.message || "加载部门树失败");
  }
};

/**
 * 加载常用部门
 * 提取一级部门（父节点是根节点的一级子节点）
 */
const loadFrequentDepts = () => {
  if (deptTreeData.value.length === 0) return;

  // 获取所有一级部门
  const rootNode = deptTreeData.value[0];
  if (rootNode && rootNode.children && rootNode.children.length > 0) {
    // 取前 N 个部门
    frequentDepts.value = rootNode.children.slice(0, props.frequentLimit);
  }
};

/**
 * 初始化已选部门
 */
const initSelectedDepts = () => {
  if (props.multiple) {
    const ids = props.modelValue as number[];
    selectedDepts.value = ids.map(id => findDeptById(id)).filter(Boolean);
  } else {
    const id = props.modelValue as number;
    if (id) {
      const dept = findDeptById(id);
      selectedDepts.value = dept ? [dept] : [];
    } else {
      selectedDepts.value = [];
    }
  }
};

/**
 * 根据ID查找部门
 */
const findDeptById = (id: number): any => {
  const findInTree = (nodes: any[]): any => {
    for (const node of nodes) {
      if (node.id === id) {
        return node;
      }
      if (node.children) {
        const found = findInTree(node.children);
        if (found) return found;
      }
    }
    return null;
  };
  return findInTree(deptTreeData.value);
};

/**
 * 过滤节点
 */
const filterNode = (value: string, data: any) => {
  if (!value) return true;
  return data.label.includes(value);
};

/**
 * 判断部门是否已选
 */
const isDeptSelected = (id: number): boolean => {
  return selectedDepts.value.some(dept => dept.id === id);
};

/**
 * 节点点击
 */
const handleNodeClick = (data: any) => {
  if (!props.multiple) {
    // 单选模式：点击即选中
    selectedDepts.value = [data];
    if (treeRef.value) {
      treeRef.value.setCurrentNode(data);
    }
  }
};

/**
 * 复选框变化
 */
const handleCheck = () => {
  if (!props.multiple || !treeRef.value) return;

  const checkedNodes = treeRef.value.getCheckedNodes();
  selectedDepts.value = checkedNodes
    .filter(node => node.nodeType === "org")
    .map(node => ({
      ...node,
      children: undefined // 移除children避免循环引用
    }));
};

/**
 * 选择常用部门
 */
const handleSelectFrequentDept = (dept: any) => {
  if (props.multiple) {
    // 多选模式：切换选中状态
    const index = selectedDepts.value.findIndex(d => d.id === dept.id);
    if (index > -1) {
      // 已选中，取消选中
      selectedDepts.value.splice(index, 1);
      if (treeRef.value) {
        treeRef.value.setChecked(dept.id, false);
      }
    } else {
      // 未选中，添加选中
      selectedDepts.value.push({ ...dept, children: undefined });
      if (treeRef.value) {
        treeRef.value.setChecked(dept.id, true);
      }
    }
  } else {
    // 单选模式：直接选中
    selectedDepts.value = [{ ...dept, children: undefined }];
    if (treeRef.value) {
      treeRef.value.setCurrentNode(dept);
    }
  }
};

/**
 * 移除已选部门
 */
const handleRemove = (dept: any) => {
  const index = selectedDepts.value.findIndex(d => d.id === dept.id);
  if (index > -1) {
    selectedDepts.value.splice(index, 1);

    // 取消树形组件的选中状态
    if (treeRef.value) {
      if (props.multiple) {
        treeRef.value.setChecked(dept.id, false);
      } else {
        treeRef.value.setCurrentKey(null);
      }
    }
  }
};

/**
 * 清空所有
 */
const handleClearAll = () => {
  selectedDepts.value = [];
  if (treeRef.value) {
    if (props.multiple) {
      treeRef.value.setCheckedKeys([]);
    } else {
      treeRef.value.setCurrentKey(null);
    }
  }
};

/**
 * 确定
 */
const handleConfirm = () => {
  if (props.multiple) {
    const ids = selectedDepts.value.map(d => d.id);
    emit("update:modelValue", ids);
    emit("change", ids, selectedDepts.value);
  } else {
    const id = selectedDepts.value.length > 0 ? selectedDepts.value[0].id : 0;
    emit("update:modelValue", id);
    emit("change", id, selectedDepts.value);
  }
  handleClose();
};

/**
 * 关闭对话框
 */
const handleClose = () => {
  dialogVisible.value = false;
  filterText.value = "";
};

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
.dept-selector {
  &-content {
    display: flex;
    gap: 16px;
    height: 400px;
    border: 1px solid #e4e7ed;
    border-top: none;
    border-radius: 0 0 4px 4px;
    overflow: hidden;
  }

  .panel- {
    &header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      border-bottom: 1px solid #e4e7ed;
      background-color: #f5f7fa;
      font-weight: 500;
    }
  }

  .dept- {
    &tree- {
      &panel {
        flex: 1;
        display: flex;
        flex-direction: column;
        border-right: 1px solid #e4e7ed;
        overflow: hidden;
      }
    }

    &frequent- {
      &panel {
        width: 280px;
        display: flex;
        flex-direction: column;
        overflow: hidden;
      }
    }
  }

  .tree-wrapper {
    flex: 1;
    overflow-y: auto;
    padding: 12px;
  }

  .tree-node {
    display: flex;
    align-items: center;
    gap: 6px;
    flex: 1;

    .node- {
      &label {
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      &tag {
        margin-left: auto;
      }
    }
  }

  .frequent- {
    &list {
      flex: 1;
      overflow-y: auto;
      padding: 12px;
    }

    &items {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    &item {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 12px;
      background-color: #fff;
      border: 1px solid #e4e7ed;
      border-radius: 4px;
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        border-color: #409eff;
        background-color: #ecf5ff;
      }

      &.is- {
        &selected {
          border-color: #67c23a;
          background-color: #f0f9ff;
        }
      }

      .item- {
        &icon {
          flex-shrink: 0;
        }

        &content {
          flex: 1;
          min-width: 0;
        }

        &name {
          font-size: 14px;
          font-weight: 500;
          color: #303133;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        &code {
          font-size: 12px;
          color: #909399;
          margin-top: 2px;
        }
      }

      .check- {
        &icon {
          flex-shrink: 0;
        }
      }
    }
  }

  .selected- {
    &area {
      padding: 10px 12px;
      border: 1px solid #e4e7ed;
      border-radius: 4px 4px 0 0;
      background-color: #fff;
      height: 120px;
      display: flex;
      flex-direction: column;
    }

    &header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 6px;
      flex-shrink: 0;
    }

    &label {
      font-size: 13px;
      font-weight: 500;
      color: #606266;
    }

    &content {
      flex: 1;
      min-height: 0;
      overflow-y: hidden;

      &.has-content {
        overflow-y: auto;
      }

      .empty-state {
        height: 100%;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
      }

      :deep(.el-empty) {
        padding: 0;
        margin: 0;
      }

      :deep(.el-empty__image) {
        width: 40px;
        height: 40px;
      }

      :deep(.el-empty__description) {
        margin-top: 8px;
        font-size: 12px;
      }
    }

    &tags {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
    }
  }

  .selected- {
    &tag {
      padding: 2px 6px;
      font-size: 12px;
      height: auto;
      line-height: 1.5;
      display: inline-flex;
      align-items: center;
      gap: 4px;

      :deep(.el-icon) {
        display: inline-flex;
        align-items: center;
      }
    }
  }
}

// 列表过渡动画
.list-enter-active,
.list-leave-active {
  transition: all 0.3s ease;
}

.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateX(-30px);
}
</style>
