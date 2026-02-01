/**
 * 图标工具类
 * 用于处理 Element Plus 图标名称到组件的映射
 */

import * as ElementPlusIcons from "@element-plus/icons-vue";

/**
 * 所有 Element Plus 图标的映射表
 */
export const IconMap: Record<string, any> = ElementPlusIcons;

/**
 * 根据图标名称获取图标组件
 * @param iconName 图标名称（如 "OfficeBuilding"）
 * @returns 图标组件，如果不存在返回 undefined
 */
export function getIconComponent(iconName: string | undefined | null) {
  if (!iconName) return undefined;
  return IconMap[iconName];
}

/**
 * 检查图标是否存在
 * @param iconName 图标名称
 * @returns 是否存在
 */
export function hasIcon(iconName: string): boolean {
  return !!IconMap[iconName];
}

/**
 * 获取所有可用的图标名称列表
 * @returns 图标名称数组
 */
export function getAllIconNames(): string[] {
  return Object.keys(IconMap).filter(name => name !== "default" && !name.startsWith("Icon"));
}
