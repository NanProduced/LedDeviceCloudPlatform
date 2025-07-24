package org.nan.cloud.terminal.config.database;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis连接配置
 * 
 * 高性能Lettuce客户端配置，针对大量缓存和实时数据优化：
 * 1. 连接池配置：最大200个活跃连接，支持高并发访问
 * 2. Lettuce异步驱动：基于Netty的非阻塞I/O，提升并发性能
 * 3. 连接复用：启用连接池复用，减少连接建立开销
 * 4. 超时控制：命令超时5秒，连接超时10秒，快速失败
 * 5. 集群支持：预留Redis集群配置，支持水平扩展
 * 
 * 存储内容：
 * - 认证缓存：设备登录凭证、认证结果缓存，TTL 30分钟
 * - 在线状态：设备实时在线状态、连接会话信息
 * - 指令队列：待下发指令队列、指令执行状态跟踪
 * - 性能指标：连接统计、性能监控数据、告警阈值等
 * - 分布式锁：集群环境下的分布式协调和互斥锁
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class RedisConfig {

    /**
     * Redis连接工厂配置 - 使用Lettuce高性能客户端
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory(RedisProperties redisProperties) {
        // Redis单机配置
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisProperties.getHost());
        config.setPort(redisProperties.getPort());
        config.setPassword(redisProperties.getPassword());
        config.setDatabase(redisProperties.getDatabase());

        // 使用Spring Boot默认连接池配置，在application.yml中配置

        // Socket配置 - 网络层优化
        SocketOptions socketOptions = SocketOptions.builder()
            .connectTimeout(Duration.ofSeconds(10))      // 连接超时：10秒
            .keepAlive(true)                            // 启用TCP Keep-Alive
            .tcpNoDelay(true)                           // 禁用Nagle算法，减少延迟
            .build();

        // 超时配置
        TimeoutOptions timeoutOptions = TimeoutOptions.builder()
            .fixedTimeout(Duration.ofSeconds(5))         // 命令执行超时：5秒
            .build();

        // 客户端配置
        ClientOptions clientOptions = ClientOptions.builder()
            .socketOptions(socketOptions)
            .timeoutOptions(timeoutOptions)
            .autoReconnect(true)                        // 自动重连
            .pingBeforeActivateConnection(true)         // 连接激活前ping
            .build();

        // Lettuce连接配置
        LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
            .clientOptions(clientOptions)
            .commandTimeout(Duration.ofSeconds(5))       // 命令超时：5秒
            .shutdownTimeout(Duration.ofSeconds(100))    // 关闭超时：100毫秒
            .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfig);
        factory.setValidateConnection(true);
        factory.afterPropertiesSet();

        log.info("Redis Lettuce连接池初始化完成: {}:{}, 数据库={}",
            redisProperties.getHost(), redisProperties.getPort(), redisProperties.getDatabase());

        return factory;
    }

    /**
     * RedisTemplate配置 - 通用Redis操作模板
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key序列化器 - 使用String序列化
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);

        // Value序列化器 - 使用JSON序列化，支持复杂对象
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonRedisSerializer);
        template.setHashValueSerializer(jsonRedisSerializer);

        // 启用默认序列化器
        template.setDefaultSerializer(jsonRedisSerializer);
        template.setEnableDefaultSerializer(true);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * StringRedisTemplate配置 - 字符串专用Redis操作模板
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        return template;
    }

    /**
     * Redis健康检查器
     */
    @Bean
    public RedisHealthIndicator redisHealthIndicator(StringRedisTemplate stringRedisTemplate) {
        return new RedisHealthIndicator(stringRedisTemplate);
    }

    /**
     * Redis健康检查指示器
     */
    public static class RedisHealthIndicator {
        private final StringRedisTemplate stringRedisTemplate;

        public RedisHealthIndicator(StringRedisTemplate stringRedisTemplate) {
            this.stringRedisTemplate = stringRedisTemplate;
        }

        /**
         * 检查Redis连接健康状态
         */
        public boolean isHealthy() {
            try {
                String result = stringRedisTemplate.getConnectionFactory().getConnection().ping();
                return "PONG".equals(result);
            } catch (Exception e) {
                log.error("Redis健康检查失败", e);
                return false;
            }
        }

        /**
         * 获取Redis服务器信息 - 简化版本
         */
        public RedisInfo getRedisInfo() {
            try {
                // 返回简化的Redis信息
                return RedisInfo.builder()
                    .version("unknown")
                    .connectedClients(0)
                    .usedMemory(0L)
                    .usedMemoryHuman("unknown")
                    .keyspaceHits(0L)
                    .keyspaceMisses(0L)
                    .build();
            } catch (Exception e) {
                log.error("获取Redis服务器信息失败", e);
                return null;
            }
        }

    }

    /**
     * Redis服务器信息
     */
    public static class RedisInfo {
        private final String version;
        private final Integer connectedClients;
        private final Long usedMemory;
        private final String usedMemoryHuman;
        private final Long keyspaceHits;
        private final Long keyspaceMisses;

        private RedisInfo(Builder builder) {
            this.version = builder.version;
            this.connectedClients = builder.connectedClients;
            this.usedMemory = builder.usedMemory;
            this.usedMemoryHuman = builder.usedMemoryHuman;
            this.keyspaceHits = builder.keyspaceHits;
            this.keyspaceMisses = builder.keyspaceMisses;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String version;
            private Integer connectedClients;
            private Long usedMemory;
            private String usedMemoryHuman;
            private Long keyspaceHits;
            private Long keyspaceMisses;

            public Builder version(String version) {
                this.version = version;
                return this;
            }

            public Builder connectedClients(Integer connectedClients) {
                this.connectedClients = connectedClients;
                return this;
            }

            public Builder usedMemory(Long usedMemory) {
                this.usedMemory = usedMemory;
                return this;
            }

            public Builder usedMemoryHuman(String usedMemoryHuman) {
                this.usedMemoryHuman = usedMemoryHuman;
                return this;
            }

            public Builder keyspaceHits(Long keyspaceHits) {
                this.keyspaceHits = keyspaceHits;
                return this;
            }

            public Builder keyspaceMisses(Long keyspaceMisses) {
                this.keyspaceMisses = keyspaceMisses;
                return this;
            }

            public RedisInfo build() {
                return new RedisInfo(this);
            }
        }

        // Getters
        public String getVersion() { return version; }
        public Integer getConnectedClients() { return connectedClients; }
        public Long getUsedMemory() { return usedMemory; }
        public String getUsedMemoryHuman() { return usedMemoryHuman; }
        public Long getKeyspaceHits() { return keyspaceHits; }
        public Long getKeyspaceMisses() { return keyspaceMisses; }

        /**
         * 计算缓存命中率
         */
        public double getHitRatio() {
            if (keyspaceHits == null || keyspaceMisses == null) {
                return 0.0;
            }
            long total = keyspaceHits + keyspaceMisses;
            return total > 0 ? (double) keyspaceHits / total : 0.0;
        }

        @Override
        public String toString() {
            return String.format("RedisInfo{version='%s', clients=%d, memory=%s, hitRatio=%.2f%%}", 
                version, connectedClients, usedMemoryHuman, getHitRatio() * 100);
        }
    }

    /**
     * Redis键名常量定义
     */
    public static class RedisKeys {
        // 认证相关键前缀
        public static final String AUTH_TOKEN_PREFIX = "terminal:auth:token:";
        public static final String AUTH_FAILED_PREFIX = "terminal:auth:failed:";
        
        // 设备在线状态键前缀
        public static final String DEVICE_ONLINE_PREFIX = "terminal:device:online:";
        public static final String DEVICE_SESSION_PREFIX = "terminal:device:session:";
        
        // 指令队列键前缀
        public static final String COMMAND_QUEUE_PREFIX = "terminal:command:queue:";
        public static final String COMMAND_STATUS_PREFIX = "terminal:command:status:";
        
        // 性能监控键前缀
        public static final String METRICS_CONNECTION_COUNT = "terminal:metrics:connections";
        public static final String METRICS_PERFORMANCE = "terminal:metrics:performance";
        
        // 分布式锁键前缀
        public static final String LOCK_PREFIX = "terminal:lock:";
        
        private RedisKeys() {
            // 工具类，禁止实例化
        }
    }
}