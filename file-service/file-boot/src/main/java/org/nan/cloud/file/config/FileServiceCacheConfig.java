package org.nan.cloud.file.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * File Service缓存配置类
 * 参照core-service标准，配置Redis + Caffeine双层缓存
 * 
 * 配置特性：
 * 1. Redis分布式缓存 - 支持多实例数据共享
 * 2. Caffeine本地缓存 - 高性能内存缓存
 * 3. 异步缓存操作 - 提升缓存访问性能
 * 4. 缓存统计监控 - 支持性能监控
 * 5. 专用线程池 - 缓存操作专用线程池
 * 6. JSON序列化 - 兼容复杂对象缓存
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableCaching
public class FileServiceCacheConfig {
    
    /**
     * 缓存专用线程池
     * 用于异步缓存操作，避免阻塞主线程
     */
    @Bean("fileServiceCacheExecutor")
    public Executor fileServiceCacheExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 根据CPU核心数配置线程池大小
        int corePoolSize = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        int maxPoolSize = Runtime.getRuntime().availableProcessors();
        
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("FileCache-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        
        log.info("File Service缓存线程池已初始化 - 核心线程数: {}, 最大线程数: {}, 队列容量: {}", 
                corePoolSize, maxPoolSize, 500);
        
        return executor;
    }
    
    /**
     * Redis模板配置 - File Service专用
     */
    @Bean("fileServiceRedisTemplate")
    @Primary
    public RedisTemplate<String, Object> fileServiceRedisTemplate(RedisConnectionFactory connectionFactory, 
                                                                 ObjectMapper objectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 配置支持类型信息的ObjectMapper
        ObjectMapper cacheObjectMapper = objectMapper.copy();
        cacheObjectMapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance, 
            DefaultTyping.NON_FINAL, 
            JsonTypeInfo.As.PROPERTY
        );
        
        // JSON序列化配置
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(cacheObjectMapper, Object.class);
        
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.setDefaultSerializer(serializer);
        
        template.afterPropertiesSet();
        
        log.info("File Service Redis模板已配置 - 使用JSON序列化");
        return template;
    }
    
    /**
     * 本地异步缓存 - File Service专用
     * 使用Caffeine提供高性能的本地缓存
     */
    @Bean("fileServiceLocalCache")
    public AsyncCache<String, Object> fileServiceLocalCache() {
        // 缓存配置 - 根据文件服务特点优化
        return Caffeine.newBuilder()
                // 最大缓存条目数 - 文件服务通常缓存较大对象
                .maximumSize(10000)
                // 写入后过期时间 - 文件信息相对稳定
                .expireAfterWrite(30, TimeUnit.MINUTES)
                // 访问后过期时间 - 保持热数据
                .expireAfterAccess(15, TimeUnit.MINUTES)
                // 启用统计 - 用于性能监控
                .recordStats()
                // 移除监听器 - 记录缓存驱逐情况
                .removalListener((key, value, cause) -> 
                    log.debug("本地缓存条目被移除 - key: {}, cause: {}", key, cause))
                .buildAsync();
    }
    
    /**
     * Redis缓存管理器 - 用于@Cacheable注解
     */
    @Bean("fileServiceRedisCacheManager")
    @Primary
    public CacheManager fileServiceRedisCacheManager(RedisConnectionFactory connectionFactory, 
                                                    ObjectMapper objectMapper) {
        // 配置支持类型信息的ObjectMapper
        ObjectMapper cacheObjectMapper = objectMapper.copy();
        cacheObjectMapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance, 
            DefaultTyping.NON_FINAL, 
            JsonTypeInfo.As.PROPERTY
        );
        
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(cacheObjectMapper, Object.class);
        
        // Redis缓存配置
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // 默认TTL - 文件服务30分钟
                .entryTtl(Duration.ofMinutes(30))
                // 键序列化
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // 值序列化
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                // 不缓存null值
                .disableCachingNullValues()
                // 键前缀
                .prefixCacheNameWith("file:cache:");
        
        // 构建缓存管理器
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .transactionAware()
                .build();
    }
    
    /**
     * 本地缓存管理器 - 备用缓存管理器
     */
    @Bean("fileServiceLocalCacheManager")
    public CacheManager fileServiceLocalCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // Caffeine缓存配置
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(20, TimeUnit.MINUTES)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .recordStats());
                
        cacheManager.setAllowNullValues(false);
        
        return cacheManager;
    }
    
    /**
     * Redis消息监听容器 - 支持缓存同步
     */
    @Bean("fileServiceRedisMessageListenerContainer")
    @ConditionalOnMissingBean
    public RedisMessageListenerContainer fileServiceRedisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        
        // 配置线程池
        container.setTaskExecutor(fileServiceCacheExecutor());
        
        log.info("File Service Redis消息监听容器已配置");
        return container;
    }
    
    /**
     * 缓存键生成器 - 自定义缓存键生成策略
     */
    @Bean("fileServiceCacheKeyGenerator")
    public org.springframework.cache.interceptor.KeyGenerator fileServiceCacheKeyGenerator() {
        return (target, method, params) -> {
            StringBuilder keyBuilder = new StringBuilder();
            keyBuilder.append("file:").append(target.getClass().getSimpleName())
                     .append(":").append(method.getName());
            
            for (Object param : params) {
                if (param != null) {
                    keyBuilder.append(":").append(param.toString());
                }
            }
            
            return keyBuilder.toString();
        };
    }
    
    /**
     * 缓存解析器 - 动态选择缓存管理器
     */
    @Bean("fileServiceCacheResolver")
    public org.springframework.cache.interceptor.CacheResolver fileServiceCacheResolver(
            CacheManager redisCacheManager, CacheManager localCacheManager) {
        return new org.springframework.cache.interceptor.SimpleCacheResolver() {
            @Override
            protected java.util.Collection<String> getCacheNames(org.springframework.cache.interceptor.CacheOperationInvocationContext<?> context) {
                // 根据方法或类的注解选择不同的缓存管理器
                // 这里简化实现，实际可以根据业务需求动态选择
                return super.getCacheNames(context);
            }
        };
    }
    
    /**
     * 缓存错误处理器 - 处理缓存操作异常
     */
    @Bean("fileServiceCacheErrorHandler") 
    public org.springframework.cache.interceptor.CacheErrorHandler fileServiceCacheErrorHandler() {
        return new org.springframework.cache.interceptor.CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, 
                    org.springframework.cache.Cache cache, Object key) {
                log.warn("缓存获取失败 - cache: {}, key: {}", cache.getName(), key, exception);
                // 不抛出异常，降级到直接调用方法
            }

            @Override
            public void handleCachePutError(RuntimeException exception, 
                    org.springframework.cache.Cache cache, Object key, Object value) {
                log.warn("缓存存储失败 - cache: {}, key: {}", cache.getName(), key, exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, 
                    org.springframework.cache.Cache cache, Object key) {
                log.warn("缓存删除失败 - cache: {}, key: {}", cache.getName(), key, exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, 
                    org.springframework.cache.Cache cache) {
                log.warn("缓存清空失败 - cache: {}", cache.getName(), exception);
            }
        };
    }
}