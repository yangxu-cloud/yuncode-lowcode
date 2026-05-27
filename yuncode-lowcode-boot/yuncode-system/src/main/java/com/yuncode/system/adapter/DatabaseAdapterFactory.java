package com.yuncode.system.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据库适配器工厂
 * <p>
 * 根据 DataSource 元数据自动检测数据库类型，匹配对应的适配器。
 * 默认适配器为 MySQL，检测失败时回退到 MySQL。
 * </p>
 */
@Slf4j
@Component
public class DatabaseAdapterFactory {

    private final Map<String, DatabaseAdapter> adapterMap = new ConcurrentHashMap<>();
    private final DatabaseAdapter defaultAdapter;

    public DatabaseAdapterFactory(List<DatabaseAdapter> adapters, DataSource dataSource) {
        // 注册所有适配器
        for (DatabaseAdapter adapter : adapters) {
            adapterMap.put(adapter.getDatabaseType().toLowerCase(), adapter);
            log.debug("已注册数据库适配器: {}", adapter.getDatabaseType());
        }

        // 检测当前数据库
        String databaseType = detectDatabaseType(dataSource);
        DatabaseAdapter detected = adapterMap.get(databaseType);
        if (detected != null) {
            defaultAdapter = detected;
        } else {
            // 回退到 MySQL
            defaultAdapter = adapterMap.getOrDefault("mysql", adapters.get(0));
            log.warn("未找到数据库类型 {} 的适配器，回退到 {}", databaseType, defaultAdapter.getDatabaseType());
        }
        log.info("当前数据库适配器: {} ({})", defaultAdapter.getDatabaseType(), defaultAdapter.getClass().getSimpleName());
    }

    /**
     * 获取当前数据库适配器
     */
    public DatabaseAdapter getAdapter() {
        return defaultAdapter;
    }

    /**
     * 根据数据库类型获取指定适配器
     */
    public DatabaseAdapter getAdapter(String databaseType) {
        return adapterMap.get(databaseType.toLowerCase());
    }

    /**
     * 从 DataSource 元数据检测数据库类型
     */
    private String detectDatabaseType(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            String url = conn.getMetaData().getURL().toLowerCase();
            String driverName = conn.getMetaData().getDriverName().toLowerCase();

            if (url.contains(":mysql:") || driverName.contains("mysql")) {
                return "mysql";
            }
            if (url.contains(":dm:") || driverName.contains("dameng") || driverName.contains("dm")) {
                return "dm";
            }
            if (url.contains(":postgresql:") || driverName.contains("postgresql")) {
                return "postgresql";
            }
            if (url.contains(":kingbase:") || driverName.contains("kingbase")) {
                return "kingbase";
            }
            if (url.contains(":oracle:") || driverName.contains("oracle")) {
                return "oracle";
            }
            if (url.contains(":sqlserver:") || driverName.contains("sqlserver") || driverName.contains("microsoft")) {
                return "sqlserver";
            }

            // 尝试从 JDBC 驱动名判断
            String productName = conn.getMetaData().getDatabaseProductName().toLowerCase();
            if (productName.contains("mysql")) return "mysql";
            if (productName.contains("dameng") || productName.contains("dm")) return "dm";

            log.warn("无法检测数据库类型 (url={}, driver={}, product={})，回退 MySQL", url, driverName, productName);
            return "mysql";
        } catch (Exception e) {
            log.error("检测数据库类型失败，回退 MySQL", e);
            return "mysql";
        }
    }
}
