package com.yuncode.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuncode.system.entity.SysBoTable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface SysBoTableMapper extends BaseMapper<SysBoTable> {

    List<SysBoTable> selectByAppId(@Param("appId") String appId);

    List<SysBoTable> selectByCategory(@Param("appId") String appId, @Param("categoryId") Long categoryId);

    long selectCountByAppId(@Param("appId") String appId);

    long selectCountByAppIdAndCategory(@Param("appId") String appId, @Param("categoryName") String categoryName);

    long selectCountByStorageName(@Param("storageName") String storageName);

    void updateCategoryNameByCategoryId(@Param("appId") String appId, @Param("categoryId") Long categoryId, @Param("categoryName") String categoryName);
}
