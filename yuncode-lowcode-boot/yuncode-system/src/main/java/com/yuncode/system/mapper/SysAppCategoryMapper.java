package com.yuncode.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuncode.system.entity.SysAppCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 应用分类Mapper接口
 *
 * @author Yuncode
 * @since 2025-05-24
 */
@InterceptorIgnore(tenantLine = "true")
@Mapper
public interface SysAppCategoryMapper extends BaseMapper<SysAppCategory> {

    /**
     * 查询应用下的所有分类（按排序）
     */
    List<SysAppCategory> selectByAppId(@Param("appId") String appId);

    /**
     * 查询应用下的子分类
     */
    List<SysAppCategory> selectByParentId(@Param("appId") String appId, @Param("parentId") Long parentId);
}
