package org.nan.cloud.terminal.infrastructure.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 简化的Redis配置
 * 
 * 使用Spring Boot的自动配置，通过application.yml进行配置：
 * - spring.data.redis.host
 * - spring.data.redis.port  
 * - spring.data.redis.password
 * - spring.data.redis.lettuce.pool.*
 * 
 * Spring Boot会自动配置：
 * - LettuceConnectionFactory (with pool)
 * - RedisTemplate<Object, Object>
 * - StringRedisTemplate
 * - 连接池 (commons-pool2)
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Configuration
public class RedisConfig {

    /**
     * 配置RedisTemplate用于存储Java对象
     * 
     * @param connectionFactory Redis连接工厂
     * @return RedisTemplate实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 配置Jackson序列化器
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        objectMapper.registerModule(new JavaTimeModule()); // 支持Java 8时间类型

        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // Key使用String序列化
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);

        // Value使用JSON序列化
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Redis键名常量定义
     */
    public static class RedisKeys {
        // 认证相关键前缀
        public static final String AUTH_CACHE_PREFIX = "terminal:auth:cache:";

        /* websocket连接管理 */
        // tid与oid映射缓存
        public static final String TID_TO_OID_PATTERN = "terminal:tid_oid_map:%d";
        public static final String WS_TOTAL_CONNECTIONS = "terminal:ws:total:connections";
        public static final String WS_CONNECTION_LIMIT_PREFIX = "terminal:ws:limit:";
        public static final String WS_CONNECTION_COUNT_PREFIX = "terminal:ws:count:";
        
        // 设备在线状态键前缀  
        public static final String DEVICE_ONLINE_PREFIX = "terminal:device:online:";
        
        /* 指令队列键前缀 */
        // 待执行指令队列 (Sorted Set)
        public static final String COMMAND_QUEUE_PATTERN= "terminal:cmd:queue:%d:%d";
        // 去重索引映射 (Hash: author_url -> command_id)
        public static final String COMMAND_DEDUPLICATION_PATTERN = "terminal:cmd:dedup:%d:%d";
        // 指令详情缓存 (String)
        public static final String COMMAND_DETAIL_PATTERN = "terminal:cmd:detail:%d:%d:%d";
        // 指令执行情况 (Hash)
        public static final String COMMAND_EXECUTE_STATUS_PATTERN = "terminal:cmd:status:%d:%d";
        
        // 设备状态键前缀（存储Java对象）
        public static final String DEVICE_STATUS_PREFIX = "terminal:device:status:";
        
        // 配置缓存键前缀（存储Java对象）
        public static final String CONFIG_CACHE_PREFIX = "terminal:config:cache:";

        /* 终端信息缓存 */
        // 终端状态信息上报(led_status)
        public static final String TERMINAL_STATUS_REPORT_PATTERN = "terminal:report:status:%d:%d";
        // 在线终端列表
        public static final String TERMINAL_ONLINE_KEY_PATTERN = "terminal:online:org:%d";
        // 组织在线终端数量
        public static final String TERMINAL_ONLINE_COUNT_PATTERN = "terminal:online:count:org:%d";
        
        private RedisKeys() {
            // 工具类，禁止实例化
        }
    }
}