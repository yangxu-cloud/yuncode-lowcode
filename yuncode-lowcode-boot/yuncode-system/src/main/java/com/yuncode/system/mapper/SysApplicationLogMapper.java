package com.yuncode.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuncode.system.entity.SysApplicationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 应用日志Mapper接口
 *
 * @author Yuncode
 * @since 2025-01-30
 */
@Mapper
public interface SysApplicationLogMapper extends BaseMapper<SysApplicationLog> {

    /**
     * 分页查询应用日志列表
     *
     * @param page 分页参数
     * @param appId 应用ID（可选）
     * @param operationType 操作类型（可选）
     * @return 日志分页列表
     */
    @InterceptorIgnore(tenantLine = "true")
    IPage<SysApplicationLog> selectPage(Page<?> page,
                                        @Param("appId") Long appId,
                                        @Param("operationType") Integer operationType);
}