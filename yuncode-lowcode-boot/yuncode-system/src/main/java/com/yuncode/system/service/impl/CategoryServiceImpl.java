package com.yuncode.system.service.impl;

import com.yuncode.common.utils.SecurityUtil;
import com.yuncode.system.entity.SysAppCategory;
import com.yuncode.system.mapper.SysAppCategoryMapper;
import com.yuncode.system.mapper.SysBoTableMapper;
import com.yuncode.system.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 应用分类服务实现
 *
 * @author Yuncode
 * @since 2025-05-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final SysAppCategoryMapper categoryMapper;
    private final SysBoTableMapper boTableMapper;

    @Override
    public List<Map<String, Object>> getCategoryTree(String appId) {
        List<SysAppCategory> all = categoryMapper.selectByAppId(appId);
        return buildTree(all, null);
    }

    @Override
    @Transactional
    public SysAppCategory createCategory(String appId, String name, Long parentId) {
        Long tenantId = SecurityUtil.getTenantId();

        // 如果是子分类，校验父分类存在且父分类本身不是子分类（最多两级）
        if (parentId != null) {
            SysAppCategory parent = categoryMapper.selectById(parentId);
            if (parent == null || !parent.getAppId().equals(appId)) {
                throw new RuntimeException("父分类不存在");
            }
            if (parent.getParentId() != null) {
                throw new RuntimeException("最多支持两级分类，无法在子分类下继续添加");
            }
            // 检查该父分类下是否已有同名子分类
            List<SysAppCategory> siblings = categoryMapper.selectByParentId(appId, parentId);
            if (siblings.stream().anyMatch(c -> c.getName().equals(name))) {
                throw new RuntimeException("该分类下已存在同名子分类");
            }
        } else {
            // 一级分类，检查同名
            List<SysAppCategory> topLevel = categoryMapper.selectByParentId(appId, null);
            if (topLevel.stream().anyMatch(c -> c.getName().equals(name))) {
                throw new RuntimeException("已存在同名一级分类");
            }
        }

        SysAppCategory category = new SysAppCategory();
        category.setAppId(appId);
        category.setName(name);
        category.setParentId(parentId);
        category.setSort(0);
        category.setTenantId(tenantId);
        categoryMapper.insert(category);
        return category;
    }

    @Override
    @Transactional
    public void renameCategory(Long id, String name) {
        SysAppCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }
        String oldName = category.getName();
        category.setName(name);
        categoryMapper.updateById(category);

        // 同步更新该分类下所有 BO 的 category_name
        if (!oldName.equals(name)) {
            try {
                boTableMapper.updateCategoryNameByCategoryId(category.getAppId(), id, name);
                log.info("同步更新 BO 分类名称: {} → {}", oldName, name);
            } catch (Exception e) {
                log.warn("同步 BO 分类名称失败（可忽略）: {}", e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        SysAppCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }
        // 级联删除子分类
        List<SysAppCategory> children = categoryMapper.selectByParentId(category.getAppId(), id);
        for (SysAppCategory child : children) {
            categoryMapper.deleteById(child.getId());
        }
        // 删除自身
        categoryMapper.deleteById(id);
    }

    /**
     * 构建树形结构
     */
    private List<Map<String, Object>> buildTree(List<SysAppCategory> all, Long parentId) {
        return all.stream()
                .filter(c -> parentId == null ? c.getParentId() == null : java.util.Objects.equals(c.getParentId(), parentId))
                .map(c -> {
                    Map<String, Object> node = new LinkedHashMap<>();
                    node.put("id", c.getId());
                    node.put("name", c.getName());
                    node.put("appId", c.getAppId());
                    node.put("parentId", c.getParentId());
                    node.put("sort", c.getSort());
                    node.put("children", buildTree(all, c.getId()));
                    return node;
                })
                .collect(Collectors.toList());
    }
}
