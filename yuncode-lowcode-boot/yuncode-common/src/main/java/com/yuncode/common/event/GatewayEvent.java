package com.yuncode.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 网关事件基类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件ID
     */
    private String eventId;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 事件来源
     */
    private String source;

    /**
     * 事件时间
     */
    private LocalDateTime timestamp;

    /**
     * 事件数据
     */
    @Builder.Default
    private Map<String, Object> data = new HashMap<>();

    /**
     * 添加事件数据
     */
    public GatewayEvent addData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    /**
     * 获取事件数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String key) {
        return (T) this.data.get(key);
    }
}
