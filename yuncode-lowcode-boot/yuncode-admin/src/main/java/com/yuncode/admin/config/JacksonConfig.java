package com.yuncode.admin.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigInteger;

/**
 * Jackson 配置
 * 将 Long 类型序列化为 String，解决 JavaScript 精度问题
 *
 * @author Yuncode
 * @since 2025-01-28
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            // 将所有 Long 类型序列化为 String
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
            builder.serializerByType(long.class, ToStringSerializer.instance);
            // BigInteger 也需要处理
            builder.serializerByType(BigInteger.class, ToStringSerializer.instance);
        };
    }
}
