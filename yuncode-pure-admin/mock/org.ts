/**
 * 组织 Mock 数据
 * 用于前端开发和演示
 */

/**
 * 组织树节点
 */
export interface OrgTreeNode {
  id: number;
  nodeType: 'org' | 'user';
  label: string;
  type?: 'company' | 'department' | 'position';
  parentId?: number | null;
  children?: OrgTreeNode[];
  sortOrder?: number;
  leader?: string;
  phone?: string;
  email?: string;
  description?: string;
  orgType?: number; // 0-根节点, 1-公司, 2-部门
  orgCode?: string;
  tenantId?: number;
  tenantCode?: string;
}

/**
 * 组织树 Mock 数据
 */
export const mockOrgTree: OrgTreeNode[] = [
  {
    id: 1,
    nodeType: 'org',
    orgType: 0, // 根节点
    label: '组织架构',
    parentId: null,
    sortOrder: 0,
    children: [
      {
        id: 11,
        nodeType: 'org',
        orgType: 1, // 公司
        type: 'company',
        label: '云创科技集团',
        orgCode: 'YUNCODE',
        tenantId: 1,
        tenantCode: 'yuncode',
        parentId: 1,
        sortOrder: 1,
        leader: '张三',
        phone: '010-12345678',
        email: 'contact@yuncode.com',
        description: '云创科技集团总部',
        children: [
          {
            id: 111,
            nodeType: 'org',
            orgType: 2, // 部门
            type: 'department',
            label: '研发中心',
            orgCode: 'RD',
            parentId: 11,
            sortOrder: 1,
            leader: '李四',
            phone: '010-12345679',
            email: 'rd@yuncode.com',
            description: '产品研发部门',
            children: [
              {
                id: 1111,
                nodeType: 'org',
                orgType: 2,
                type: 'department',
                label: '前端开发组',
                orgCode: 'FRONTEND',
                parentId: 111,
                sortOrder: 1,
                leader: '王五',
                phone: '010-12345680',
                description: 'Web前端开发',
                children: []
              },
              {
                id: 1112,
                nodeType: 'org',
                orgType: 2,
                type: 'department',
                label: '后端开发组',
                orgCode: 'BACKEND',
                parentId: 111,
                sortOrder: 2,
                leader: '赵六',
                phone: '010-12345681',
                description: '服务端开发',
                children: []
              }
            ]
          },
          {
            id: 112,
            nodeType: 'org',
            orgType: 2,
            type: 'department',
            label: '产品中心',
            orgCode: 'PRODUCT',
            parentId: 11,
            sortOrder: 2,
            leader: '孙七',
            phone: '010-12345682',
            email: 'product@yuncode.com',
            description: '产品规划与设计',
            children: [
              {
                id: 1121,
                nodeType: 'org',
                orgType: 2,
                type: 'department',
                label: '产品设计部',
                orgCode: 'DESIGN',
                parentId: 112,
                sortOrder: 1,
                leader: '周八',
                description: 'UI/UX设计',
                children: []
              }
            ]
          },
          {
            id: 113,
            nodeType: 'org',
            orgType: 2,
            type: 'department',
            label: '市场中心',
            orgCode: 'MARKET',
            parentId: 11,
            sortOrder: 3,
            leader: '吴九',
            phone: '010-12345683',
            description: '市场营销与推广',
            children: []
          }
        ]
      }
    ]
  }
];

/**
 * 获取组织树（模拟延迟）
 */
export async function mockGetOrgTree(): Promise<{ code: number; message: string; data: OrgTreeNode[] }> {
  await new Promise(resolve => setTimeout(resolve, 300));
  return { code: 200, message: '获取成功', data: mockOrgTree };
}

/**
 * 获取组织详情（模拟延迟）
 */
export async function mockGetOrgDetail(id: number): Promise<{ data: any }> {
  await new Promise(resolve => setTimeout(resolve, 200));

  // 递归查找节点
  function findNode(nodes: OrgTreeNode[], targetId: number): OrgTreeNode | null {
    for (const node of nodes) {
      if (node.id === targetId) return node;
      if (node.children) {
        const found = findNode(node.children, targetId);
        if (found) return found;
      }
    }
    return null;
  }

  const node = findNode(mockOrgTree, id);
  return { data: node };
}
