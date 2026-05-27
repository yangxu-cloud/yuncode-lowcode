package com.yuncode.admin.config;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis Plus 配置
 */
@Slf4j
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
                    // 从 Sa-Token Session 获取当前租户ID和登录类型
                    if (!StpUtil.isLogin()) {
                        // 未登录时返回 null，不进行租户过滤
                        log.info("多租户插件 - 未登录，不进行租户过滤");
                        return null;
                    }

                    // 使用 getTokenSession 获取 Session（与登录时保持一致）
                    String loginType = StpUtil.getTokenSession().get("loginType", "");
                    Long tenantId = StpUtil.getTokenSession().get("tenantId", null);

                    log.info("多租户插件 - loginType: {}, tenantId: {}", loginType, tenantId);

                    // 平台管理员（loginType=admin）不进行租户过滤，可以查看所有租户数据
                    if ("admin".equals(loginType)) {
                        log.info("多租户插件 - 平台管理员，不进行租户过滤");
                        return null;
                    }

                    // 其他登录类型（tenant/user）进行租户过滤
                    // 如果 tenantId 为 null 或 0，不进行租户过滤（避免生成 tenant_id = null）
                    if (tenantId == null || tenantId == 0) {
                        log.info("多租户插件 - tenantId为空，不进行租户过滤");
                        return null;
                    }

                    log.info("多租户插件 - 使用租户ID: {}", tenantId);
                    return new LongValue(tenantId);
                } catch (cn.dev33.satoken.exception.NotWebContextException e) {
                    // 非 web 上下文异常，静默处理，不进行租户过滤
                    log.info("多租户插件 - 非Web上下文，不进行租户过滤");
                    return null;
                } catch (Exception e) {
                    log.error("多租户插件 - 获取租户ID异常", e);
                    // 其他异常也返回 null，不进行租户过滤
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
                // sys_user - 用户表需要根据登录类型动态过滤，不在拦截器层面处理
                // sys_user_org - 用户组织关系表，tenant_id 字段使用组织的租户ID，不使用session的租户ID
                // sys_role - 角色表需要根据登录类型动态过滤，不在拦截器层面处理
                // sys_org - 组织/部门表需要根据登录类型动态过滤，不在拦截器层面处理
                // sys_role_dept - 角色-部门关联表，tenant_id 字段使用部门的租户ID，不使用session的租户ID
                // sys_role_user - 角色-用户关联表，tenant_id 字段使用用户的租户ID，不使用session的租户ID
                // sys_role_permission - 角色-权限关联表，由Service层动态控制租户过滤
                return "sys_tenant".equalsIgnoreCase(tableName)
                        || "sys_dict".equalsIgnoreCase(tableName)
                        || "sys_dict_data".equalsIgnoreCase(tableName)
                        || "sys_settings".equalsIgnoreCase(tableName)
                        || "sys_system_log".equalsIgnoreCase(tableName)
                        || "sys_login_log".equalsIgnoreCase(tableName)
                        || "sys_operation_log".equalsIgnoreCase(tableName)
                        || "sys_user".equalsIgnoreCase(tableName)  // 用户表，由Service层动态控制租户过滤
                        || "sys_user_org".equalsIgnoreCase(tableName)
                        || "sys_role".equalsIgnoreCase(tableName)  // 角色表，由Service层动态控制租户过滤
                        || "sys_org".equalsIgnoreCase(tableName)  // 组织/部门表，由Service层动态控制租户过滤
                        || "sys_role_dept".equalsIgnoreCase(tableName)  // 角色-部门关联表，由Service层动态控制租户过滤
                        || "sys_role_user".equalsIgnoreCase(tableName)  // 角色-用户关联表，由Service层动态控制租户过滤
                        || "sys_role_permission".equalsIgnoreCase(tableName)  // 角色-权限关联表，由Service层动态控制租户过滤
                    || "sys_application".equalsIgnoreCase(tableName)  // 应用表，由Service层手动处理租户过滤
                    || "sys_application_log".equalsIgnoreCase(tableName);  // 应用日志表，由Service层手动处理租户过滤
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
                        Long tenantId = StpUtil.getTokenSession().get("tenantId", 0L);
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
             * 获取当前登录用户名（从 Sa-Token Session 读取）
             */
            private String getCurrentUser() {
                try {
                    if (StpUtil.isLogin()) {
                        return StpUtil.getTokenSession().get("username", "system");
                    }
                } catch (Exception e) {
                    // 非 Web 上下文（异步线程等），使用默认值
                }
                return "system";
            }
        };
    }
}
