package com.yuncode.common.event;

/**
 * 事件类型常量
 */
public class EventTypes {

    private EventTypes() {
    }

    // ==================== 用户相关事件 ====================
    public static final String USER_LOGIN = "user.login";
    public static final String USER_LOGOUT = "user.logout";
    public static final String USER_REGISTER = "user.register";

    // ==================== 应用相关事件 ====================
    public static final String APP_CREATE = "app.create";
    public static final String APP_UPDATE = "app.update";
    public static final String APP_DELETE = "app.delete";
    public static final String APP_DEPLOY = "app.deploy";
    public static final String APP_UNDEPLOY = "app.undeploy";
    public static final String APP_START = "app.start";
    public static final String APP_STOP = "app.stop";

    // ==================== 系统相关事件 ====================
    public static final String SYSTEM_STARTUP = "system.startup";
    public static final String SYSTEM_SHUTDOWN = "system.shutdown";
    public static final String SYSTEM_ALERT = "system.alert";

    // ==================== 网关相关事件 ====================
    public static final String GATEWAY_REQUEST = "gateway.request";
    public static final String GATEWAY_ERROR = "gateway.error";

    // ==================== 定时任务相关事件 ====================
    public static final String JOB_EXECUTE = "job.execute";
    public static final String JOB_SUCCESS = "job.success";
    public static final String JOB_FAIL = "job.fail";
}
