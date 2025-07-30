package org.nan.cloud.message.infrastructure.mq.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Message-Service统一RabbitMQ配置
 * 
 * 职责定位：
 * - 专注于实时消息推送到前端SPA
 * - MQ消息到STOMP消息的桥接转换
 * - 简化的队列架构，提高维护性
 * 
 * 设计原则：
 * - 不处理复杂业务逻辑，只做消息桥接
 * - 精简队列数量，统一命名规范
 * - 性能优化的监听器配置
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableRabbit
public class MessageServiceRabbitConfig {
    
    // ==================== 交换器名称常量 ====================
    
    public static final String STOMP_PUSH_EXCHANGE = "stomp.push.topic";
    public static final String STOMP_BRIDGE_EXCHANGE = "stomp.bridge.topic";
    public static final String STOMP_DLX_EXCHANGE = "stomp.dlx.topic";
    
    // ==================== 队列名称常量 ====================
    
    public static final String DEVICE_STATUS_QUEUE = "stomp.device.status.queue";
    public static final String COMMAND_RESULT_QUEUE = "stomp.command.result.queue";
    public static final String SYSTEM_NOTIFICATION_QUEUE = "stomp.system.notification.queue";
    public static final String BATCH_PROGRESS_QUEUE = "stomp.batch.progress.queue";
    public static final String BRIDGE_DLQ = "stomp.bridge.dlq";
    
    // ==================== 路由键常量 ====================
    
    public static final String DEVICE_STATUS_ROUTING_KEY = "stomp.device.status.*.*";
    public static final String COMMAND_RESULT_ROUTING_KEY = "stomp.command.result.*.*";
    public static final String SYSTEM_NOTIFICATION_ROUTING_KEY = "stomp.system.notification.*.*";
    public static final String BATCH_PROGRESS_ROUTING_KEY = "stomp.batch.progress.*.*";

    
    /**
     * 消息转换器 - 使用Jackson处理JSON序列化
     */
    @Bean
    public MessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setCreateMessageIds(true);
        log.info("🔧 配置JSON消息转换器");
        return converter;
    }
    
    /**
     * RabbitTemplate配置 - 优化的发送配置
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        
        // 启用发布确认
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.debug("✅ 消息发送成功: {}", correlationData);
            } else {
                log.error("❌ 消息发送失败: {}, 原因: {}", correlationData, cause);
            }
        });
        
        // 启用返回确认
        template.setReturnsCallback(returned -> {
            log.error("⚠️ 消息被退回: exchange={}, routingKey={}, replyText={}", 
                    returned.getExchange(), returned.getRoutingKey(), returned.getReplyText());
        });
        
        log.info("🔧 配置RabbitTemplate - 启用确认机制");
        return template;
    }
    
    /**
     * 监听器容器工厂配置 - 针对实时推送优化
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        
        // 并发配置 - 针对实时推送场景优化
        factory.setConcurrentConsumers(2);      // 每个队列2个消费者
        factory.setMaxConcurrentConsumers(4);   // 最大4个消费者
        
        // 预取配置 - 平衡延迟和吞吐量
        factory.setPrefetchCount(3);            // 较小的预取数量，降低延迟
        
        // 确认模式 - 手动确认保证可靠性
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        
        // 错误处理配置
        factory.setDefaultRequeueRejected(false);  // 失败消息直接进入死信队列
        
        // 超时配置
        factory.setReceiveTimeout(30000L);       // 30秒接收超时
        factory.setIdleEventInterval(15000L);    // 15秒空闲事件间隔
        
        // 容错配置
        factory.setMissingQueuesFatal(false);    // 队列不存在时不致命
        factory.setRecoveryInterval(3000L);      // 连接恢复间隔3秒
        factory.setConsecutiveActiveTrigger(5);  // 连续活跃触发器
        factory.setConsecutiveIdleTrigger(3);    // 连续空闲触发器
        
        log.info("🔧 配置监听器容器工厂 - 实时推送优化");
        return factory;
    }
    
    // ==================== 交换器配置 ====================
    
    /**
     * 实时推送交换器 - 主要的消息推送交换器
     */
    @Bean
    public TopicExchange stompPushExchange() {
        return ExchangeBuilder
                .topicExchange(STOMP_PUSH_EXCHANGE)
                .durable(true)
                .build();
    }
    
    /**
     * 桥接处理交换器 - 用于内部桥接逻辑
     */
    @Bean
    public TopicExchange stompBridgeExchange() {
        return ExchangeBuilder
                .topicExchange(STOMP_BRIDGE_EXCHANGE)
                .durable(true)
                .build();
    }
    
    /**
     * 死信交换器 - 统一死信处理
     */
    @Bean
    public DirectExchange stompDlxExchange() {
        return ExchangeBuilder
                .directExchange(STOMP_DLX_EXCHANGE)
                .durable(true)
                .build();
    }
    
    // ==================== 队列配置 ====================
    
    /**
     * 设备状态队列 - 设备状态变更推送
     */
    @Bean
    public Queue deviceStatusQueue() {
        return QueueBuilder
                .durable(DEVICE_STATUS_QUEUE)
                .withArgument("x-dead-letter-exchange", STOMP_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "device.status.dlq")
                .withArgument("x-message-ttl", 30000) // 30秒TTL - 状态消息时效性要求高
                .build();
    }
    
    /**
     * 指令结果队列 - 指令执行结果推送
     */
    @Bean
    public Queue commandResultQueue() {
        return QueueBuilder
                .durable(COMMAND_RESULT_QUEUE)
                .withArgument("x-dead-letter-exchange", STOMP_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "command.result.dlq")
                .withArgument("x-message-ttl", 300000) // 5分钟TTL - 指令结果需要保留更久
                .build();
    }
    
    /**
     * 系统通知队列 - 系统通知推送
     */
    @Bean
    public Queue systemNotificationQueue() {
        return QueueBuilder
                .durable(SYSTEM_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", STOMP_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "system.notification.dlq")
                .withArgument("x-message-ttl", 1800000) // 30分钟TTL - 通知消息可以保留久一些
                .build();
    }
    
    /**
     * 批量进度队列 - 批量指令进度推送
     */
    @Bean
    public Queue batchProgressQueue() {
        return QueueBuilder
                .durable(BATCH_PROGRESS_QUEUE)
                .withArgument("x-dead-letter-exchange", STOMP_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "batch.progress.dlq")
                .withArgument("x-message-ttl", 180000) // 3分钟TTL - 进度消息时效性要求高
                .build();
    }
    
    /**
     * 统一死信队列 - 简化的死信处理
     */
    @Bean
    public Queue bridgeDlq() {
        return QueueBuilder
                .durable(BRIDGE_DLQ)
                .withArgument("x-message-ttl", 86400000) // 24小时TTL
                .build();
    }
    
    // ==================== 绑定配置 ====================
    
    /**
     * 设备状态队列绑定
     * 路由键：stomp.device.status.{orgId}.{deviceId}
     */
    @Bean
    public Binding deviceStatusBinding() {
        return BindingBuilder
                .bind(deviceStatusQueue())
                .to(stompPushExchange())
                .with(DEVICE_STATUS_ROUTING_KEY);
    }
    
    /**
     * 指令结果队列绑定
     * 路由键：stomp.command.result.{orgId}.{userId}
     */
    @Bean
    public Binding commandResultBinding() {
        return BindingBuilder
                .bind(commandResultQueue())
                .to(stompPushExchange())
                .with(COMMAND_RESULT_ROUTING_KEY);
    }
    
    /**
     * 系统通知队列绑定
     * 路由键：stomp.system.notification.{type}.{orgId}
     */
    @Bean
    public Binding systemNotificationBinding() {
        return BindingBuilder
                .bind(systemNotificationQueue())
                .to(stompPushExchange())
                .with(SYSTEM_NOTIFICATION_ROUTING_KEY);
    }
    
    /**
     * 批量进度队列绑定
     * 路由键：stomp.batch.progress.{userId}.{batchId}
     */
    @Bean
    public Binding batchProgressBinding() {
        return BindingBuilder
                .bind(batchProgressQueue())
                .to(stompPushExchange())
                .with(BATCH_PROGRESS_ROUTING_KEY);
    }
    
    /**
     * 死信队列绑定 - 统一处理所有死信消息
     */
    @Bean
    public Binding bridgeDlqBinding() {
        return BindingBuilder
                .bind(bridgeDlq())
                .to(stompDlxExchange())
                .with("*.dlq");
    }
    
    // ==================== 配置加载日志 ====================
    
    /**
     * 配置加载完成后打印队列信息
     */
    @Bean
    public String logQueueConfiguration() {
        log.info("🚀 Message-Service RabbitMQ配置加载完成");
        log.info("📊 队列架构总览：");
        log.info("  ├─ 实时推送队列 (4个)：");
        log.info("  │  ├─ {} - 设备状态推送", DEVICE_STATUS_QUEUE);
        log.info("  │  ├─ {} - 指令结果推送", COMMAND_RESULT_QUEUE);
        log.info("  │  ├─ {} - 系统通知推送", SYSTEM_NOTIFICATION_QUEUE);
        log.info("  │  └─ {} - 批量进度推送", BATCH_PROGRESS_QUEUE);
        log.info("  └─ 死信队列 (1个)：");
        log.info("     └─ {} - 统一死信处理", BRIDGE_DLQ);
        log.info("🔀 交换器配置：");
        log.info("  ├─ {} - 主推送交换器", STOMP_PUSH_EXCHANGE);
        log.info("  ├─ {} - 桥接交换器", STOMP_BRIDGE_EXCHANGE);
        log.info("  └─ {} - 死信交换器", STOMP_DLX_EXCHANGE);
        log.info("✅ Message-Service专注实时推送，架构简化完成");
        
        return "MessageService RabbitMQ Configuration Loaded";
    }
    
    /*
     * ==================== 架构设计说明 ====================
     * 
     * 队列精简策略：
     * 1. 从原来的18个队列精简到5个核心队列
     * 2. 专注于实时推送场景，移除复杂业务逻辑
     * 3. 统一stomp.前缀命名，避免与其他服务冲突
     * 
     * 性能优化：
     * 1. 较小的预取数量(3)，降低推送延迟  
     * 2. 适中的并发数(2-4)，平衡性能和资源
     * 3. 合理的TTL设置，根据消息时效性区分
     * 
     * 可靠性保证：
     * 1. 手动确认模式，确保消息处理可靠性
     * 2. 统一死信队列，简化异常处理
     * 3. 发布确认机制，监控消息发送状态
     * 
     * 扩展性考虑：
     * 1. 预留桥接交换器，支持未来扩展
     * 2. 灵活的路由键设计，支持细粒度路由
     * 3. 清晰的命名规范，便于运维管理
     */
}