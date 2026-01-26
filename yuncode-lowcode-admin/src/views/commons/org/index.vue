<template>
  <div class="org-container">
    <div class="org-header">
      <h2>组织管理</h2>
      <el-button type="primary" @click="handleAdd">添加组织</el-button>
    </div>

    <div class="org-content">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>组织架构树</span>
                <el-input
                  v-model="searchText"
                  placeholder="搜索组织或人员"
                  prefix-icon="Search"
                  style="width: 200px"
                  clearable
                />
              </div>
            </template>
            <el-tree
              v-if="orgTreeData.length > 0"
              :data="orgTreeData"
              :props="treeProps"
              node-key="id"
              @node-click="handleNodeClick"
            >
              <template #default="{ node, data }">
                <span class="custom-tree-node">
                  <el-icon v-if="data.nodeType === 'org'">
                    <OfficeBuilding />
                  </el-icon>
                  <el-icon v-else>
                    <User />
                  </el-icon>
                  <span class="node-label">{{ node.label }}</span>
                </span>
              </template>
            </el-tree>
            <el-empty v-else description="暂无数据" />
          </el-card>
        </el-col>

        <el-col :span="16">
          <el-card>
            <template #header>
              <span>详细信息</span>
            </template>
            <div v-if="selectedNode" class="node-detail">
              <el-descriptions :column="2" border>
                <el-descriptions-item label="名称">
                  {{ selectedNode.label }}
                </el-descriptions-item>
                <el-descriptions-item label="类型">
                  {{ selectedNode.nodeType === 'org' ? '组织' : '人员' }}
                </el-descriptions-item>
              </el-descriptions>
            </div>
            <el-empty v-else description="请选择左侧节点查看详情" />
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { OfficeBuilding, User } from '@element-plus/icons-vue'

interface OrgTreeNode {
  id: number
  nodeType: 'org' | 'user'
  label: string
  children?: OrgTreeNode[]
}

const orgTreeData = ref<OrgTreeNode[]>([])
const selectedNode = ref<OrgTreeNode | null>(null)
const searchText = ref('')

const treeProps = {
  children: 'children',
  label: 'label'
}

// 加载组织树数据
const loadOrgTree = async () => {
  try {
    // TODO: 调用后端API获取组织树
    // const response = await getOrgTree()
    // orgTreeData.value = response.data

    // 模拟数据
    orgTreeData.value = [
      {
        id: 1,
        nodeType: 'org',
        label: '组织架构',
        children: [
          {
            id: 2,
            nodeType: 'org',
            label: '技术部',
            children: []
          },
          {
            id: 3,
            nodeType: 'org',
            label: '市场部',
            children: []
          }
        ]
      }
    ]
  } catch (error) {
    console.error('加载组织树失败:', error)
  }
}

const handleNodeClick = (data: OrgTreeNode) => {
  selectedNode.value = data
}

const handleAdd = () => {
  // TODO: 实现添加组织功能
  console.log('添加组织')
}

onMounted(() => {
  loadOrgTree()
})
</script>

<style scoped>
.org-container {
  padding: 20px;
}

.org-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.org-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 500;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.custom-tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
}

.node-label {
  font-size: 14px;
}

.node-detail {
  padding: 10px 0;
}
</style>
