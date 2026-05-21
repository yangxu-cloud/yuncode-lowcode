package com.yuncode.admin.controller;

import com.yuncode.common.event.EventTypes;
import com.yuncode.common.event.GatewayEvent;
import com.yuncode.common.event.SimpleEventBus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 事件管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/event")
@RequiredArgsConstructor
public class EventController {

    private final SimpleEventBus eventBus;

    /**
     * 发布测试事件
     */
    @PostMapping("/test")
    public Map<String, Object> publishTestEvent(@RequestBody Map<String, Object> data) {
        String eventType = (String) data.getOrDefault("eventType", EventTypes.SYSTEM_ALERT);
        String source = (String) data.getOrDefault("source", "test");

        @SuppressWarnings("unchecked")
        Map<String, Object> eventData = (Map<String, Object>) data.getOrDefault("data", new HashMap<>());

        GatewayEvent event = GatewayEvent.builder()
            .eventType(eventType)
            .source(source)
            .data(eventData)
            .build();

        eventBus.publish(event);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Event published");
        result.put("eventId", event.getEventId());
        return result;
    }

    /**
     * 发布用户登录事件
     */
    @PostMapping("/demo/login")
    public Map<String, Object> publishLoginEvent(
        @RequestParam(defaultValue = "1001") String userId,
        @RequestParam(defaultValue = "test-user") String username) {

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("username", username);
        data.put("loginTime", System.currentTimeMillis());

        eventBus.publish(EventTypes.USER_LOGIN, "demo", data);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Login event published");
        return result;
    }

    /**
     * 发布应用创建事件
     */
    @PostMapping("/demo/app")
    public Map<String, Object> publishAppEvent(
        @RequestParam(defaultValue = "com.yuncode.app.demo") String appId,
        @RequestParam(defaultValue = "Demo App") String appName,
        @RequestParam(defaultValue = "admin") String creator) {

        Map<String, Object> data = new HashMap<>();
        data.put("appId", appId);
        data.put("appName", appName);
        data.put("creator", creator);

        eventBus.publish(EventTypes.APP_CREATE, "demo", data);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "App create event published");
        return result;
    }

    /**
     * 获取事件类型列表
     */
    @GetMapping("/types")
    public Map<String, Object> getEventTypes() {
        Map<String, String> types = new HashMap<>();
        types.put(EventTypes.USER_LOGIN, "用户登录");
        types.put(EventTypes.USER_LOGOUT, "用户登出");
        types.put(EventTypes.USER_REGISTER, "用户注册");
        types.put(EventTypes.APP_CREATE, "应用创建");
        types.put(EventTypes.APP_UPDATE, "应用更新");
        types.put(EventTypes.APP_DELETE, "应用删除");
        types.put(EventTypes.APP_DEPLOY, "应用部署");
        types.put(EventTypes.APP_START, "应用启动");
        types.put(EventTypes.APP_STOP, "应用停止");
        types.put(EventTypes.SYSTEM_ALERT, "系统告警");
        types.put(EventTypes.JOB_EXECUTE, "任务执行");

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", types);
        return result;
    }
}
