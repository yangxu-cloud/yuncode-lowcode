package com.yuncode.admin.config;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis Plus 配置
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * MyBatis Plus 拦截器
     * 注意：多租户插件必须放在分页插件之前
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 1. 多租户插件（自动添加 tenant_id 条件）
        TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor();
        tenantInterceptor.setTenantLineHandler(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                try {
                    // 从 Sa-Token Session 获取当前租户ID
                    if (!StpUtil.isLogin()) {
                        // 未登录时返回 null，不进行租户过滤
                        // 用于系统初始化等场景
                        System.out.println("[MyBatisPlus] 未登录，返回 null");
                        return null;
                    }

                    String roleCode = StpUtil.getSession().get("roleCode", "");
                    Long tenantId = StpUtil.getSession().get("tenantId", 0L);

                    System.out.println("[MyBatisPlus] getTenantId - roleCode=" + roleCode + ", tenantId=" + tenantId);

                    // 平台管理员也有自己的租户ID（系统租户），在进行增删改操作时应该使用这个租户ID
                    // 只有在需要查看所有租户数据时，才使用 @InterceptorIgnore 注解的方法
                    if (tenantId == null || tenantId == 0) {
                        System.out.println("[MyBatisPlus] tenantId 为 null 或 0，返回 null");
                        return null;
                    }

                    System.out.println("[MyBatisPlus] 返回 tenantId=" + tenantId);
                    return new LongValue(tenantId);
                } catch (Exception e) {
                    // 在异步线程等非 Web 上下文中，无法获取租户ID
                    // 返回 null，不进行租户过滤
                    // 常见场景：异步日志记录、定时任务等
                    System.out.println("[MyBatisPlus] 获取 tenantId 异常: " + e.getMessage());
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public boolean ignoreTable(String tableName) {
                // 忽略不需要租户隔离的表
                // sys_tenant - 租户表本身不需要隔离
                // sys_dict - 字典表通常是全局共享的
                // sys_dict_data - 字典数据表通常是全局共享的
                // sys_system_log - 系统日志表需要在全局范围内查询
                // sys_login_log - 登录日志表需要在全局范围内查询
                // sys_operation_log - 操作日志表需要在全局范围内查询和插入
                // sys_user_org - 用户组织关系表，tenant_id 字段使用组织的租户ID，不使用session的租户ID
                return "sys_tenant".equalsIgnoreCase(tableName)
                        || "sys_dict".equalsIgnoreCase(tableName)
                        || "sys_dict_data".equalsIgnoreCase(tableName)
                        || "sys_settings".equalsIgnoreCase(tableName)
                        || "sys_system_log".equalsIgnoreCase(tableName)
                        || "sys_login_log".equalsIgnoreCase(tableName)
                        || "sys_operation_log".equalsIgnoreCase(tableName)
                        || "sys_user_org".equalsIgnoreCase(tableName);
            }
        });
        interceptor.addInnerInterceptor(tenantInterceptor);

        // 2. 分页插件
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInnerInterceptor.setMaxLimit(500L); // 单页分页条数限制
        paginationInnerInterceptor.setOverflow(false); // 溢出总页数后是否进行处理

        interceptor.addInnerInterceptor(paginationInnerInterceptor);

        return interceptor;
    }

    /**
     * 字段自动填充处理器
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                // 自动填充租户ID
                try {
                    if (StpUtil.isLogin()) {
                        Long tenantId = StpUtil.getSession().get("tenantId", 0L);
                        this.strictInsertFill(metaObject, "tenantId", Long.class, tenantId);
                    }
                } catch (Exception e) {
                    // 在异步线程等非 Web 上下文中，无法获取租户ID
                    // 不进行租户ID填充
                    // 常见场景：异步日志记录、定时任务等
                }
                // 自动填充时间字段
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
                // 自动填充用户字段
                this.strictInsertFill(metaObject, "createBy", String.class, getCurrentUser());
                this.strictInsertFill(metaObject, "updateBy", String.class, getCurrentUser());
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
                this.strictUpdateFill(metaObject, "updateBy", String.class, getCurrentUser());
            }

            /**
             * 获取当前用户
             * TODO: 从请求上下文或 Security 上下文获取当前登录用户
             */
            private String getCurrentUser() {
                // 暂时返回固定值，后续可以从 Request 或 SecurityContext 获取
                return "system";
            }
        };
    }
}
