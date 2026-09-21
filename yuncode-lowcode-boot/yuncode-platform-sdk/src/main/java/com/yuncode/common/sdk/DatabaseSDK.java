package com.yuncode.common.sdk;

import java.util.List;
import java.util.Map;

/**
 * Database SDK — 数据库操作
 *
 * App 通过此接口操作自己的业务数据表。
 * 平台自动处理租户隔离和数据权限。
 */
public interface DatabaseSDK {

    /** 查询列表 */
    List<Map<String, Object>> query(String sql, Object... params);

    /** 查询单条 */
    Map<String, Object> queryOne(String sql, Object... params);

    /** 执行更新（INSERT/UPDATE/DELETE） */
    int execute(String sql, Object... params);

    /** 统计行数 */
    long count(String tableName, String whereClause, Object... params);

    /** 开启事务（由平台管理） */
    void beginTransaction();

    /** 提交事务 */
    void commit();

    /** 回滚事务 */
    void rollback();
}
