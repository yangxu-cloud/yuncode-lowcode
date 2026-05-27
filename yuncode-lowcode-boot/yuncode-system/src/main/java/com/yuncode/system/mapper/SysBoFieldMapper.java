package com.yuncode.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuncode.system.entity.SysBoField;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface SysBoFieldMapper extends BaseMapper<SysBoField> {

    List<SysBoField> selectByTableId(@Param("tableId") Long tableId);

    int deleteByTableId(@Param("tableId") Long tableId);
}
