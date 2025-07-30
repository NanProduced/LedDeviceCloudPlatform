package org.nan.cloud.message.infrastructure.mq.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * STOMP桥接队列配置
 * 
 * 配置用于MQ到STOMP消息桥接的RabbitMQ队列、交换器和绑定关系。
 * 支持不同类型业务消息的分类处理和路由。
 * 
 * 队列设计：
 * 1. device.status.queue - 设备状态变更消息
 * 2. command.result.queue - 指令执行结果消息  
 * 3. system.notification.queue - 系统通知消息
 * 4. batch.command.progress.queue - 批量指令进度消息
 * 5. stomp.bridge.queue - 通用桥接消息
 * 6. stomp.bridge.dlq - 死信队列
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableRabbit
public class StompBridgeQueueConfig {
    
    // ==================== 交换器定义 ====================
    
    /**
     * 设备相关消息交换器
     */
    @Bean
    public TopicExchange deviceTopicExchange() {
        return ExchangeBuilder.topicExchange("device.topic")
                .durable(true)
                .build();
    }
    
    /**
     * 用户相关消息交换器
     */
    @Bean
    public TopicExchange userTopicExchange() {
        return ExchangeBuilder.topicExchange("user.topic")
                .durable(true)
                .build();
    }
    
    /**
     * 系统相关消息交换器
     */
    @Bean
    public TopicExchange systemTopicExchange() {
        return ExchangeBuilder.topicExchange("system.topic")
                .durable(true)
                .build();
    }
    
    /**
     * STOMP桥接专用交换器
     */
    @Bean
    public TopicExchange stompBridgeExchange() {
        return ExchangeBuilder.topicExchange("stomp.bridge.topic")
                .durable(true)
                .build();
    }
    
    // ==================== 队列定义 ====================
    
    /**
     * 设备状态消息队列
     */
    @Bean
    public Queue deviceStatusQueue() {
        return QueueBuilder.durable("device.status.queue")
                .withArgument("x-dead-letter-exchange", "stomp.bridge.topic")
                .withArgument("x-dead-letter-routing-key", "stomp.bridge.dlq")
                .withArgument("x-message-ttl", 300000) // 5分钟TTL
                .build();
    }
    
    /**
     * 指令结果消息队列
     */
    @Bean
    public Queue commandResultQueue() {
        return QueueBuilder.durable("command.result.queue")
                .withArgument("x-dead-letter-exchange", "stomp.bridge.topic")
                .withArgument("x-dead-letter-routing-key", "stomp.bridge.dlq")
                .withArgument("x-message-ttl", 600000) // 10分钟TTL
                .build();
    }
    
    /**
     * 系统通知消息队列
     */
    @Bean
    public Queue systemNotificationQueue() {
        return QueueBuilder.durable("system.notification.queue")
                .withArgument("x-dead-letter-exchange", "stomp.bridge.topic")
                .withArgument("x-dead-letter-routing-key", "stomp.bridge.dlq")
                .withArgument("x-message-ttl", 1800000) // 30分钟TTL
                .build();
    }
    
    /**
     * 批量指令进度消息队列
     */
    @Bean
    public Queue batchCommandProgressQueue() {
        return QueueBuilder.durable("batch.command.progress.queue")
                .withArgument("x-dead-letter-exchange", "stomp.bridge.topic")
                .withArgument("x-dead-letter-routing-key", "stomp.bridge.dlq")
                .withArgument("x-message-ttl", 180000) // 3分钟TTL
                .build();
    }
    
    /**
     * 通用STOMP桥接消息队列
     */
    @Bean
    public Queue stompBridgeQueue() {
        return QueueBuilder.durable("stomp.bridge.queue")
                .withArgument("x-dead-letter-exchange", "stomp.bridge.topic")
                .withArgument("x-dead-letter-routing-key", "stomp.bridge.dlq")
                .withArgument("x-message-ttl", 300000) // 5分钟TTL
                .build();
    }
    
    /**
     * STOMP桥接死信队列
     */
    @Bean
    public Queue stompBridgeDlq() {
        return QueueBuilder.durable("stomp.bridge.dlq")
                .build();
    }
    
    // ==================== 队列绑定 ====================
    
    /**
     * 设备状态消息绑定
     * 路由键模式：device.status.{orgId}.{deviceId}
     */
    @Bean
    public Binding deviceStatusBinding() {
        return BindingBuilder.bind(deviceStatusQueue())
                .to(deviceTopicExchange())
                .with("device.status.*.*");
    }
    
    /**
     * 指令结果消息绑定
     * 路由键模式：command.result.{orgId}.{deviceId}
     */
    @Bean
    public Binding commandResultBinding() {
        return BindingBuilder.bind(commandResultQueue())
                .to(deviceTopicExchange())
                .with("command.result.*.*");
    }
    
    /**
     * 系统通知消息绑定
     * 路由键模式：notification.{type}.{orgId}
     */
    @Bean
    public Binding systemNotificationBinding() {
        return BindingBuilder.bind(systemNotificationQueue())
                .to(systemTopicExchange())
                .with("notification.*.*");
    }
    
    /**
     * 批量指令进度消息绑定
     * 路由键模式：batch.progress.{orgId}.{batchId}
     */
    @Bean
    public Binding batchCommandProgressBinding() {
        return BindingBuilder.bind(batchCommandProgressQueue())
                .to(userTopicExchange())
                .with("batch.progress.*.*");
    }
    
    /**
     * 通用STOMP桥接消息绑定
     * 路由键模式：stomp.bridge.*
     */
    @Bean
    public Binding stompBridgeBinding() {
        return BindingBuilder.bind(stompBridgeQueue())
                .to(stompBridgeExchange())
                .with("stomp.bridge.*");
    }
    
    /**
     * 死信队列绑定
     */
    @Bean
    public Binding stompBridgeDlqBinding() {
        return BindingBuilder.bind(stompBridgeDlq())
                .to(stompBridgeExchange())
                .with("stomp.bridge.dlq");
    }
    
    // ==================== 额外的业务队列绑定 ====================
    
    /**
     * 用户状态变更消息绑定
     * 路由键模式：user.status.{orgId}.{userId}
     */
    @Bean
    public Binding userStatusBinding() {
        return BindingBuilder.bind(stompBridgeQueue())
                .to(userTopicExchange())
                .with("user.status.*.*");
    }
    
    /**
     * 组织公告消息绑定
     * 路由键模式：org.announcement.{orgId}
     */
    @Bean
    public Binding orgAnnouncementBinding() {
        return BindingBuilder.bind(systemNotificationQueue())
                .to(systemTopicExchange())
                .with("org.announcement.*");
    }
    
    /**
     * 系统维护通知绑定
     * 路由键模式：system.maintenance.*
     */
    @Bean
    public Binding systemMaintenanceBinding() {
        return BindingBuilder.bind(systemNotificationQueue())
                .to(systemTopicExchange())
                .with("system.maintenance.*");
    }
    
    /**
     * 设备报警消息绑定
     * 路由键模式：device.alarm.{orgId}.{deviceId}
     */
    @Bean
    public Binding deviceAlarmBinding() {
        return BindingBuilder.bind(deviceStatusQueue())
                .to(deviceTopicExchange())
                .with("device.alarm.*.*");
    }
    
    /**
     * 终端组变更消息绑定
     * 路由键模式：terminalgroup.change.{orgId}.{groupId}
     */
    @Bean
    public Binding terminalGroupChangeBinding() {
        return BindingBuilder.bind(stompBridgeQueue())
                .to(systemTopicExchange())
                .with("terminalgroup.change.*.*");
    }
    
    /**
     * 权限变更通知绑定
     * 路由键模式：permission.change.{orgId}.{userId}
     */
    @Bean
    public Binding permissionChangeBinding() {
        return BindingBuilder.bind(stompBridgeQueue())
                .to(userTopicExchange())
                .with("permission.change.*.*");
    }
    
    // ==================== 配置信息日志 ====================
    
    /**
     * 配置加载完成后打印队列信息
     */
    @Bean
    public String logQueueConfiguration() {
        log.info("🔧 STOMP桥接队列配置加载完成");
        log.info("📋 已配置队列：");
        log.info("  - device.status.queue: 设备状态消息");
        log.info("  - command.result.queue: 指令执行结果");
        log.info("  - system.notification.queue: 系统通知");
        log.info("  - batch.command.progress.queue: 批量指令进度");
        log.info("  - stomp.bridge.queue: 通用桥接消息");
        log.info("  - stomp.bridge.dlq: 死信队列");
        log.info("🔀 已配置交换器：");
        log.info("  - device.topic: 设备相关消息");
        log.info("  - user.topic: 用户相关消息");
        log.info("  - system.topic: 系统相关消息");
        log.info("  - stomp.bridge.topic: STOMP桥接专用");
        log.info("✅ STOMP MQ桥接配置完成，准备监听消息");
        
        return "STOMP Bridge Queue Configuration Loaded";
    }
}