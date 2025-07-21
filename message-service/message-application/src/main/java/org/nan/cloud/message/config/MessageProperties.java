package org.nan.cloud.message.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 消息配置属性类
 * 
 * 位于application层，定义消息服务相关的配置属性。
 * Infrastructure层通过依赖注入使用这些配置，遵循DDD分层架构。
 * 
 * 配置示例：
 * message:
 *   default-ttl-days: 7
 *   batch-size: 50
 *   max-retry-count: 1
 *   template-cache-ttl: 600
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "message")
public class MessageProperties {
    
    /**
     * 消息默认存活时间（天）
     * 默认值: 30天
     */
    private Integer defaultTtlDays = 30;
    
    /**
     * 批量处理大小
     * 默认值: 100
     */
    private Integer batchSize = 100;
    
    /**
     * 最大重试次数
     * 默认值: 3
     */
    private Integer maxRetryCount = 3;
    
    /**
     * 模板缓存TTL（秒）
     * 默认值: 3600 (1小时)
     */
    private Integer templateCacheTtl = 3600;
    
    /**
     * 消息队列配置
     */
    private Queue queue = new Queue();
    
    /**
     * 持久化配置
     */
    private Persistence persistence = new Persistence();
    
    /**
     * 通知配置
     */
    private Notification notification = new Notification();
    
    /**
     * 消息队列配置内部类
     */
    @Data
    public static class Queue {
        
        /**
         * 队列前缀
         * 默认值: "message"
         */
        private String prefix = "message";
        
        /**
         * 死信队列后缀
         * 默认值: ".dlq"
         */
        private String deadLetterSuffix = ".dlq";
        
        /**
         * 消息TTL（毫秒）
         * 默认值: 3600000 (1小时)
         */
        private Long messageTtl = 3600000L;
        
        /**
         * 队列最大长度
         * 默认值: 10000
         */
        private Integer maxLength = 10000;
    }
    
    /**
     * 持久化配置内部类
     */
    @Data
    public static class Persistence {
        
        /**
         * 是否启用消息持久化
         * 默认值: true
         */
        private Boolean enabled = true;
        
        /**
         * 批量插入大小
         * 默认值: 200
         */
        private Integer batchInsertSize = 200;
        
        /**
         * 异步持久化
         * 默认值: true
         */
        private Boolean async = true;
        
        /**
         * 持久化线程池大小
         * 默认值: 5
         */
        private Integer threadPoolSize = 5;
    }
    
    /**
     * 通知配置内部类
     */
    @Data
    public static class Notification {
        
        /**
         * 是否启用推送通知
         * 默认值: true
         */
        private Boolean enabled = true;
        
        /**
         * 推送失败重试间隔（毫秒）
         * 默认值: 5000 (5秒)
         */
        private Long retryInterval = 5000L;
        
        /**
         * 通知模板默认语言
         * 默认值: "zh-CN"
         */
        private String defaultLanguage = "zh-CN";
        
        /**
         * 是否启用邮件通知
         * 默认值: false
         */
        private Boolean emailEnabled = false;
        
        /**
         * 是否启用短信通知
         * 默认值: false
         */
        private Boolean smsEnabled = false;
    }
}