/**
 * 菜单 Mock 数据
 * 用于前端开发和演示
 */

/**
 * 菜单树节点
 */
export interface MenuTreeItem {
  id: number;
  parentId: number;
  menuName: string;
  icon?: string;
  menuType: number; // 0: 目录, 1: 菜单, 2: 按钮
  path?: string;
  component?: string;
  permission?: string;
  sortOrder?: number;
  visible?: number; // 0: 显示, 1: 隐藏
  status?: number; // 0: 正常, 1: 停用
  children?: MenuTreeItem[];
}

/**
 * 菜单树 Mock 数据
 */
export const mockMenuTree: MenuTreeItem[] = [
  {
    id: 1,
    menuName: '办公',
    icon: 'OfficeBuilding',
    menuType: 0,
    path: '/office',
    sortOrder: 1,
    visible: 0,
    status: 0,
    parentId: 0,
    children: [
      {
        id: 11,
        menuName: '工作台',
        icon: 'Monitor',
        menuType: 1,
        path: '/office/dashboard',
        component: '/views/welcome/index.vue',
        sortOrder: 1,
        visible: 0,
        status: 0,
        parentId: 1,
        children: []
      },
      {
        id: 12,
        menuName: '日程管理',
        icon: 'Calendar',
        menuType: 1,
        path: '/office/calendar',
        component: '/views/welcome/index.vue',
        sortOrder: 2,
        visible: 0,
        status: 0,
        parentId: 1,
        children: []
      }
    ]
  },
  {
    id: 2,
    menuName: '基础管理',
    icon: 'Setting',
    menuType: 0,
    path: '/basic',
    sortOrder: 2,
    visible: 0,
    status: 0,
    parentId: 0,
    children: [
      {
        id: 21,
        menuName: '用户管理',
        icon: 'User',
        menuType: 1,
        path: '/basic/user',
        component: '/views/welcome/index.vue',
        sortOrder: 1,
        visible: 0,
        status: 0,
        parentId: 2,
        children: []
      },
      {
        id: 22,
        menuName: '角色管理',
        icon: 'UserFilled',
        menuType: 1,
        path: '/basic/role',
        component: '/views/welcome/index.vue',
        sortOrder: 2,
        visible: 0,
        status: 0,
        parentId: 2,
        children: []
      }
    ]
  }
];

/**
 * 菜单权限 Mock 数据
 */
export const mockMenuPermissions = [
  {
    id: 1,
    menuId: 1,
    targetType: 0,
    targetId: 1,
    targetName: '管理员',
    targetTypeName: '超级管理员角色'
  },
  {
    id: 2,
    menuId: 1,
    targetType: 1,
    targetId: 1,
    targetName: 'admin',
    targetTypeName: '系统管理员'
  }
];

/**
 * 获取菜单树（模拟延迟）
 */
export async function mockGetMenuTree(): Promise<{ code: number; message: string; data: MenuTreeItem[] }> {
  // 模拟网络延迟
  await new Promise(resolve => setTimeout(resolve, 300));
  return { code: 200, message: '获取成功', data: mockMenuTree };
}

/**
 * 获取菜单权限（模拟延迟）
 */
export async function mockGetMenuPermissions(menuId: number): Promise<{ code: number; message: string; data: any[] }> {
  await new Promise(resolve => setTimeout(resolve, 200));
  const permissions = mockMenuPermissions.filter(p => p.menuId === menuId);
  return { code: 200, message: '获取成功', data: permissions };
}
