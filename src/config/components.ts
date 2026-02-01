/**
 * 组件配置
 * 自动扫描项目中的 Vue 组件，用于菜单配置
 */

import {
  User,
  Setting,
  OfficeBuilding,
  Document,
  Folder,
  Monitor,
  Operation,
  Tools,
  Message,
  Calendar,
  Menu,
  Lock,
  Bell,
  Warning,
  InfoFilled
} from "@element-plus/icons-vue";

export interface ComponentItem {
  path: string;
  label: string;
  category: string;
  icon?: any;
  description?: string;
}

/**
 * 组件列表配置
 * 分类组织，便于管理
 */
export const componentList: ComponentItem[] = [
  // ==================== 系统管理 ====================
  {
    path: "/views/facilities/navigation/index.vue",
    label: "导航管理",
    category: "system",
    icon: Menu,
    description: "菜单树和权限管理"
  },
  {
    path: "/views/facilities/org/index.vue",
    label: "组织管理",
    category: "system",
    icon: OfficeBuilding,
    description: "组织架构和部门管理"
  },

  // ==================== 权限管理 ====================
  {
    path: "/views/permission/page/index.vue",
    label: "页面权限",
    category: "permission",
    icon: Lock,
    description: "页面级权限配置"
  },
  {
    path: "/views/permission/button/perms.vue",
    label: "按钮权限",
    category: "permission",
    icon: Lock,
    description: "按钮级权限配置"
  },

  // ==================== 运维管理 ====================
  {
    path: "/views/system/online-users/index.vue",
    label: "在线用户",
    category: "operations",
    icon: User,
    description: "在线用户监控"
  },
  {
    path: "/views/operations/operation-log/index.vue",
    label: "操作日志",
    category: "operations",
    icon: Document,
    description: "用户操作日志"
  },
  {
    path: "/views/operations/system-log/index.vue",
    label: "系统日志",
    category: "operations",
    icon: Document,
    description: "系统运行日志"
  },
  {
    path: "/views/operations/personnel-log/index.vue",
    label: "人员日志",
    category: "operations",
    icon: Document,
    description: "人员操作日志"
  },

  // ==================== 系统设置 ====================
  {
    path: "/views/settings/index.vue",
    label: "系统设置",
    category: "settings",
    icon: Setting,
    description: "系统基础设置"
  },
  {
    path: "/views/settings/Basic.vue",
    label: "基础设置",
    category: "settings",
    icon: Setting,
    description: "基本配置"
  },
  {
    path: "/views/settings/Security.vue",
    label: "安全设置",
    category: "settings",
    icon: Lock,
    description: "安全相关配置"
  },
  {
    path: "/views/settings/System.vue",
    label: "系统参数",
    category: "settings",
    icon: Monitor,
    description: "系统参数配置"
  },

  // ==================== 错误页面 ====================
  {
    path: "/views/error/403.vue",
    label: "403页面",
    category: "error",
    icon: Warning,
    description: "无权限页面"
  },
  {
    path: "/views/error/404.vue",
    label: "404页面",
    category: "error",
    icon: Warning,
    description: "页面不存在"
  },
  {
    path: "/views/error/500.vue",
    label: "500页面",
    category: "error",
    icon: Warning,
    description: "服务器错误"
  },

  // ==================== 其他页面 ====================
  {
    path: "/views/welcome/index.vue",
    label: "欢迎页",
    category: "other",
    icon: InfoFilled,
    description: "系统欢迎页面"
  },
  {
    path: "/views/login/index.vue",
    label: "登录页",
    category: "other",
    icon: User,
    description: "用户登录"
  }
];

/**
 * 根据分类获取组件列表
 */
export const getComponentsByCategory = (category: string): ComponentItem[] => {
  return componentList.filter(comp => comp.category === category);
};

/**
 * 获取所有分类
 */
export const getCategories = (): string[] => {
  const categories = new Set(componentList.map(comp => comp.category));
  return Array.from(categories);
};

/**
 * 根据路径搜索组件
 */
export const searchComponents = (keyword: string): ComponentItem[] => {
  if (!keyword) return componentList;
  const lowerKeyword = keyword.toLowerCase();
  return componentList.filter(
    comp =>
      comp.label.toLowerCase().includes(lowerKeyword) ||
      comp.path.toLowerCase().includes(lowerKeyword) ||
      comp.description?.toLowerCase().includes(lowerKeyword)
  );
};

/**
 * 组件分类映射（中文显示名称）
 */
export const categoryLabels: Record<string, string> = {
  system: "系统管理",
  permission: "权限管理",
  operations: "运维管理",
  settings: "系统设置",
  error: "错误页面",
  other: "其他页面"
};
