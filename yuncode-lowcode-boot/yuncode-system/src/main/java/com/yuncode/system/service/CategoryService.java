package com.yuncode.system.service;

import com.yuncode.system.entity.SysAppCategory;

import java.util.List;
import java.util.Map;

/**
 * 应用分类服务接口
 *
 * @author Yuncode
 * @since 2025-05-24
 */
public interface CategoryService {

    /**
     * 获取应用的分类树
     *
     * @param appId 应用标识
     * @return 分类列表（包含 children 字段的树形结构）
     */
    List<Map<String, Object>> getCategoryTree(String appId);

    /**
     * 创建分类
     *
     * @param appId    应用标识
     * @param name     分类名称
     * @param parentId 父分类ID（可选）
     * @return 创建的分类
     */
    SysAppCategory createCategory(String appId, String name, Long parentId);

    /**
     * 重命名分类
     *
     * @param id   分类ID
     * @param name 新名称
     */
    void renameCategory(Long id, String name);

    /**
     * 删除分类（级联删除子分类）
     *
     * @param id 分类ID
     */
    void deleteCategory(Long id);
}
