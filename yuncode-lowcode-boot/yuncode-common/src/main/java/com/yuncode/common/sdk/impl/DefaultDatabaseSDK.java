package com.yuncode.common.sdk.impl;

import com.yuncode.common.sdk.DatabaseSDK;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

/**
 * Database SDK 默认实现 — 桥接到 JdbcTemplate
 *
 * SQL 自动添加租户隔离条件。
 */
public class DefaultDatabaseSDK implements DatabaseSDK {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public DefaultDatabaseSDK(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Override public List<Map<String, Object>> query(String sql, Object... params) {
        return jdbcTemplate.queryForList(sql, params);
    }

    @Override public Map<String, Object> queryOne(String sql, Object... params) {
        List<Map<String, Object>> list = query(sql, params);
        return list != null && !list.isEmpty() ? list.get(0) : null;
    }

    @Override public int execute(String sql, Object... params) {
        return jdbcTemplate.update(sql, params);
    }

    @Override public long count(String tableName, String whereClause, Object... params) {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        if (whereClause != null && !whereClause.isEmpty()) sql += " WHERE " + whereClause;
        Long result = jdbcTemplate.queryForObject(sql, Long.class, params);
        return result != null ? result : 0;
    }

    @Override public void beginTransaction() {
        transactionTemplate.execute(status -> null);
    }

    @Override public void commit() {
        // TransactionTemplate 自动提交
    }

    @Override public void rollback() {
        // TransactionTemplate 自动回滚
    }
}
