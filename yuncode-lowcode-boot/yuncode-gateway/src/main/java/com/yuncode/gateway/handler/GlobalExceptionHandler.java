package com.yuncode.gateway.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuncode.common.model.util.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Gateway 全局异常处理器
 * 返回格式与 Admin Result 对齐：{code, message, data, timestamp}
 */
@Slf4j
@Component
@Order(-1)
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "系统内部错误";

        if (ex instanceof ResponseStatusException rse) {
            status = (HttpStatus) rse.getStatusCode();
            message = rse.getReason() != null ? rse.getReason() : status.getReasonPhrase();
        }

        log.error("[Gateway] Exception: ", ex);
        response.setStatusCode(status);

        // 使用与 Admin 一致的 Result 格式
        Result<?> result = Result.error(status.value(), message);
        return writeJson(response, result);
    }

    private Mono<Void> writeJson(ServerHttpResponse response, Object value) {
        try {
            byte[] bytes = OBJECT_MAPPER.writeValueAsBytes(value);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("[Gateway] Failed to serialize error response", e);
            DataBuffer buffer = response.bufferFactory()
                    .wrap("{\"code\":500,\"message\":\"Internal error\",\"data\":null}".getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        }
    }
}
