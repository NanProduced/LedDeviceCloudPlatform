package org.nan.cloud.common.mq.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 消息队列配置属性
 * 
 * 统一管理RabbitMQ相关的配置参数，支持灵活的配置定制。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "nan.mq")
public class MqProperties {
    
    /**
     * 是否启用MQ功能
     */
    private boolean enabled = true;
    
    /**
     * 应用名称，用于标识消息来源
     */
    private String applicationName = "unknown-app";
    
    /**
     * 默认交换机配置
     */
    private Exchange defaultExchange = new Exchange();
    
    /**
     * 生产者配置
     */
    private Producer producer = new Producer();
    
    /**
     * 消费者配置
     */
    private Consumer consumer = new Consumer();
    
    /**
     * 死信队列配置
     */
    private DeadLetter deadLetter = new DeadLetter();
    
    /**
     * 监控配置
     */
    private Monitor monitor = new Monitor();
    
    /**
     * 序列化配置
     */
    private Serialization serialization = new Serialization();
    
    /**
     * 交换机配置
     */
    @Data
    public static class Exchange {
        /**
         * 默认交换机名称
         */
        private String name = "default.exchange";
        
        /**
         * 交换机类型：direct, topic, fanout, headers
         */
        private String type = "topic";
        
        /**
         * 是否持久化
         */
        private boolean durable = true;
        
        /**
         * 是否自动删除
         */
        private boolean autoDelete = false;
        
        /**
         * 交换机参数
         */
        private Map<String, Object> arguments = new HashMap<>();
    }
    
    /**
     * 生产者配置
     */
    @Data
    public static class Producer {
        /**
         * 是否启用发布确认
         */
        private boolean confirmEnabled = true;
        
        /**
         * 是否启用返回确认
         */
        private boolean returnsEnabled = true;
        
        /**
         * 发送超时时间
         */
        private Duration sendTimeout = Duration.ofSeconds(30);
        
        /**
         * 是否启用重试
         */
        private boolean retryEnabled = true;
        
        /**
         * 最大重试次数
         */
        private int maxRetryAttempts = 3;
        
        /**
         * 重试间隔
         */
        private Duration retryInterval = Duration.ofSeconds(1);
        
        /**
         * 是否启用事务
         */
        private boolean transactionEnabled = false;
        
        /**
         * 批量发送大小
         */
        private int batchSize = 100;
        
        /**
         * 批量发送超时时间
         */
        private Duration batchTimeout = Duration.ofSeconds(5);
    }
    
    /**
     * 消费者配置
     */
    @Data
    public static class Consumer {
        /**
         * 默认并发消费者数量
         */
        private int concurrency = 1;
        
        /**
         * 最大并发消费者数量
         */
        private int maxConcurrency = 3;
        
        /**
         * 预取数量
         */
        private int prefetchCount = 5;
        
        /**
         * 确认模式：auto, manual
         */
        private String acknowledgeMode = "manual";
        
        /**
         * 接收超时时间
         */
        private Duration receiveTimeout = Duration.ofSeconds(60);
        
        /**
         * 是否默认重新入队被拒绝的消息
         */
        private boolean defaultRequeueRejected = false;
        
        /**
         * 空闲事件间隔
         */
        private Duration idleEventInterval = Duration.ofSeconds(30);
        
        /**
         * 连接恢复间隔
         */
        private Duration recoveryInterval = Duration.ofSeconds(5);
        
        /**
         * 队列不存在时是否致命
         */
        private boolean missingQueuesFatal = false;
        
        /**
         * 连续活跃触发器
         */
        private int consecutiveActiveTrigger = 10;
        
        /**
         * 连续空闲触发器
         */
        private int consecutiveIdleTrigger = 5;
    }
    
    /**
     * 死信配置
     */
    @Data
    public static class DeadLetter {
        /**
         * 是否启用死信队列
         */
        private boolean enabled = true;
        
        /**
         * 死信交换机名称
         */
        private String exchangeName = "dlx.exchange";
        
        /**
         * 死信路由键前缀
         */
        private String routingKeyPrefix = "dlq";
        
        /**
         * 死信消息TTL（毫秒）
         */
        private long messageTtl = 24 * 60 * 60 * 1000; // 24小时
        
        /**
         * 死信队列最大长度
         */
        private long maxLength = 10000;
    }
    
    /**
     * 监控配置
     */
    @Data
    public static class Monitor {
        /**
         * 是否启用监控
         */
        private boolean enabled = true;
        
        /**
         * 是否启用指标收集
         */
        private boolean metricsEnabled = true;
        
        /**
         * 是否启用健康检查
         */
        private boolean healthCheckEnabled = true;
        
        /**
         * 性能统计间隔
         */
        private Duration statisticsInterval = Duration.ofMinutes(1);
        
        /**
         * 是否记录慢消息处理
         */
        private boolean slowMessageLoggingEnabled = true;
        
        /**
         * 慢消息处理阈值
         */
        private Duration slowMessageThreshold = Duration.ofSeconds(5);
    }
    
    /**
     * 序列化配置
     */
    @Data
    public static class Serialization {
        /**
         * 序列化类型：json, kryo, protobuf
         */
        private String type = "json";
        
        /**
         * 是否启用压缩
         */
        private boolean compressionEnabled = false;
        
        /**
         * 压缩阈值（字节）
         */
        private int compressionThreshold = 1024;
        
        /**
         * 压缩算法：gzip, lz4, snappy
         */
        private String compressionAlgorithm = "gzip";
    }
    
    /**
     * 队列配置
     */
    @Data
    public static class QueueConfig {
        /**
         * 队列名称
         */
        private String name;
        
        /**
         * 是否持久化
         */
        private boolean durable = true;
        
        /**
         * 是否排他
         */
        private boolean exclusive = false;
        
        /**
         * 是否自动删除
         */
        private boolean autoDelete = false;
        
        /**
         * 消息TTL
         */
        private Long messageTtl;
        
        /**
         * 队列最大长度
         */
        private Long maxLength;
        
        /**
         * 死信交换机
         */
        private String deadLetterExchange;
        
        /**
         * 死信路由键
         */
        private String deadLetterRoutingKey;
        
        /**
         * 队列参数
         */
        private Map<String, Object> arguments = new HashMap<>();
    }
}