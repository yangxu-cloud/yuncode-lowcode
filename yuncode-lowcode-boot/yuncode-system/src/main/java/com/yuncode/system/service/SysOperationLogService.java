package com.yuncode.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuncode.system.dto.OperationLogQueryDTO;
import com.yuncode.system.entity.SysOperationLog;
import com.yuncode.system.mapper.SysOperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 操作日志服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysOperationLogService {

    private final SysOperationLogMapper operationLogMapper;

    /**
     * 分页查询操作日志
     */
    public Page<SysOperationLog> getOperationLogPage(OperationLogQueryDTO dto) {
        Page<SysOperationLog> page = new Page<>(dto.getPage(), dto.getSize());

        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();

        // 查询条件
        if (StringUtils.hasText(dto.getUsername())) {
            wrapper.like(SysOperationLog::getUsername, dto.getUsername());
        }
        if (StringUtils.hasText(dto.getModule())) {
            wrapper.like(SysOperationLog::getModule, dto.getModule());
        }
        if (StringUtils.hasText(dto.getOperation())) {
            wrapper.like(SysOperationLog::getOperation, dto.getOperation());
        }
        if (StringUtils.hasText(dto.getStatus())) {
            wrapper.eq(SysOperationLog::getStatus, dto.getStatus());
        }
        if (StringUtils.hasText(dto.getTraceId())) {
            wrapper.like(SysOperationLog::getTraceId, dto.getTraceId());
        }

        // 按创建时间倒序
        wrapper.orderByDesc(SysOperationLog::getCreatedAt);

        return operationLogMapper.selectPage(page, wrapper);
    }

    /**
     * 记录操作日志
     */
    public void recordOperationLog(SysOperationLog operationLog) {
        operationLogMapper.insert(operationLog);
        log.debug("操作日志记录成功: username={}, operation={}, traceId={}",
                operationLog.getUsername(), operationLog.getOperation(), operationLog.getTraceId());
    }

    /**
     * 根据 TraceId 查询操作日志
     */
    public List<SysOperationLog> getLogsByTraceId(String traceId) {
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysOperationLog::getTraceId, traceId)
                .orderByAsc(SysOperationLog::getCreatedAt);
        return operationLogMapper.selectList(wrapper);
    }
}
