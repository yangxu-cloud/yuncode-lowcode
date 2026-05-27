package com.yuncode.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuncode.system.entity.SysApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 应用Mapper接口
 *
 * @author Yuncode
 * @since 2025-01-30
 */
@Mapper
public interface SysApplicationMapper extends BaseMapper<SysApplication> {

    /**
     * 分页查询应用列表
     *
     * @param page 分页参数
     * @param tenantId 租户ID
     * @param appName 应用名称（可选）
     * @return 应用分页列表
     */
    IPage<SysApplication> selectPage(Page<?> page, @Param("tenantId") Long tenantId, @Param("appName") String appName);

    /**
     * 根据ID查询应用
     *
     * @param id 应用ID
     * @return 应用实体
     */
    SysApplication selectById(@Param("id") Long id);

    /**
     * 检查应用ID是否存在
     *
     * @param appId 应用ID
     * @param tenantId 租户ID
     * @return 是否存在
     */
    @InterceptorIgnore(tenantLine = "true")
    boolean existsByAppId(@Param("appId") String appId, @Param("tenantId") Long tenantId);

    /**
     * 根据 appId（应用标识）和租户ID查询应用
     */
    @InterceptorIgnore(tenantLine = "true")
    SysApplication selectByAppId(@Param("appId") String appId, @Param("tenantId") Long tenantId);

    /**
     * 物理删除（彻底删除，不经过逻辑删除）
     */
    int physicalDeleteById(@Param("id") Long id);
}