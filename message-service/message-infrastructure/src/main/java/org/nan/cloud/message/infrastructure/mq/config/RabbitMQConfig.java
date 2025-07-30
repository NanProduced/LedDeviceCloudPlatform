package org.nan.cloud.message.infrastructure.mq.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.config.MessageProperties;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 * 
 * 负责配置消息队列、交换机、绑定关系以及消息转换器
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig {
    
    private final MessageProperties messageProperties;
    
    // ===================== 交换机名称 =====================
    public static final String MESSAGE_EXCHANGE = "message.exchange";
    public static final String EVENT_EXCHANGE = "event.exchange";
    public static final String MESSAGE_DLX_EXCHANGE = "message.dlx.exchange";
    
    // ===================== 消息队列名称 ===================== 
    public static final String MESSAGE_NOTIFICATION_QUEUE = "message.notification.queue";
    public static final String MESSAGE_SYSTEM_QUEUE = "message.system.queue";
    public static final String MESSAGE_USER_QUEUE = "message.user.queue";
    
    // ===================== 事件队列名称 =====================
    public static final String BUSINESS_EVENT_QUEUE = "business.event.queue";
    public static final String DEVICE_EVENT_QUEUE = "device.event.queue";
    public static final String USER_EVENT_QUEUE = "user.event.queue";
    public static final String SYSTEM_NOTIFICATION_QUEUE = "system.notification.queue";
    
    // ===================== 死信队列名称 =====================
    public static final String MESSAGE_FAILED_QUEUE = "message.failed.queue";
    public static final String DEVICE_FAILED_QUEUE = "device.failed.queue";
    public static final String USER_FAILED_QUEUE = "user.failed.queue";
    public static final String BUSINESS_FAILED_QUEUE = "business.failed.queue";
    public static final String SYSTEM_FAILED_QUEUE = "system.failed.queue";
    
    // ===================== 路由键 =====================
    public static final String NOTIFICATION_ROUTING_KEY = "message.notification";
    public static final String SYSTEM_ROUTING_KEY = "message.system";
    public static final String USER_ROUTING_KEY = "message.user";
    
    // 事件路由键
    public static final String BUSINESS_EVENT_ROUTING_KEY = "event.business";
    public static final String DEVICE_EVENT_ROUTING_KEY = "event.device";
    public static final String USER_EVENT_ROUTING_KEY = "event.user";
    public static final String SYSTEM_EVENT_ROUTING_KEY = "event.system";
    
    /**
     * 消息转换器 - 使用Jackson处理JSON序列化
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    /**
     * RabbitTemplate配置
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        
        // 启用发布确认
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.debug("消息发送成功: {}", correlationData);
            } else {
                log.error("消息发送失败: {}, 原因: {}", correlationData, cause);
            }
        });
        
        // 启用返回确认
        template.setReturnsCallback(returned -> {
            log.error("消息被退回: exchange={}, routingKey={}, replyText={}", 
                    returned.getExchange(), returned.getRoutingKey(), returned.getReplyText());
        });
        
        return template;
    }
    
    /**
     * 监听器容器工厂配置 - 针对性能优化
     * 
     * 性能优化策略：
     * 1. 适中的并发数避免资源竞争
     * 2. 合理的预取数量平衡内存和吞吐量
     * 3. 手动确认保证消息可靠性
     * 4. 设置合理的超时时间
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        
        // 并发消费者配置 - 根据队列数量适当调整
        factory.setConcurrentConsumers(1);      // 每个队列至少1个消费者
        factory.setMaxConcurrentConsumers(3);   // 最大3个消费者，避免过度并发
        
        // 预取配置 - 平衡内存使用和处理效率
        factory.setPrefetchCount(5);            // 减少预取数量，避免消息堆积
        
        // 确认模式
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        
        // 错误处理配置
        factory.setDefaultRequeueRejected(false);  // 默认不重新入队，避免死循环
        
        // 超时和连接配置
        factory.setReceiveTimeout(60000L);       // 60秒接收超时
        factory.setIdleEventInterval(30000L);    // 30秒空闲事件间隔
        
        // 容错配置 - 提高系统稳定性
        factory.setMissingQueuesFatal(false);    // 队列不存在时不致命，允许动态创建
        
        // 连接恢复配置
        factory.setRecoveryInterval(5000L);      // 连接恢复间隔5秒
        factory.setConsecutiveActiveTrigger(10); // 连续活跃触发器
        factory.setConsecutiveIdleTrigger(5);    // 连续空闲触发器
        
        return factory;
    }
    
    /**
     * 主交换机 - Topic类型，支持路由键模式匹配
     */
    @Bean
    public TopicExchange messageExchange() {
        return ExchangeBuilder
                .topicExchange(MESSAGE_EXCHANGE)
                .durable(true)
                .build();
    }
    
    /**
     * 事件交换机 - Topic类型，处理各种业务事件
     */
    @Bean
    public TopicExchange eventExchange() {
        return ExchangeBuilder
                .topicExchange(EVENT_EXCHANGE)
                .durable(true)
                .build();
    }
    
    /**
     * 死信交换机
     */
    @Bean
    public DirectExchange messageDlxExchange() {
        return ExchangeBuilder
                .directExchange(MESSAGE_DLX_EXCHANGE)
                .durable(true)
                .build();
    }
    
    /**
     * 通知消息队列
     */
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder
                .durable(MESSAGE_NOTIFICATION_QUEUE)
                .withArgument("x-message-ttl", messageProperties.getQueue().getMessageTtl())
                .withArgument("x-max-length", messageProperties.getQueue().getMaxLength())
                .withArgument("x-dead-letter-exchange", MESSAGE_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "notification.dlq")
                .build();
    }
    
    /**
     * 系统消息队列
     */
    @Bean
    public Queue systemQueue() {
        return QueueBuilder
                .durable(MESSAGE_SYSTEM_QUEUE)
                .withArgument("x-message-ttl", messageProperties.getQueue().getMessageTtl())
                .withArgument("x-max-length", messageProperties.getQueue().getMaxLength())
                .withArgument("x-dead-letter-exchange", MESSAGE_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "system.dlq")
                .build();
    }
    
    /**
     * 用户消息队列
     */
    @Bean
    public Queue userQueue() {
        return QueueBuilder
                .durable(MESSAGE_USER_QUEUE)
                .withArgument("x-message-ttl", messageProperties.getQueue().getMessageTtl())
                .withArgument("x-max-length", messageProperties.getQueue().getMaxLength())
                .withArgument("x-dead-letter-exchange", MESSAGE_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "user.dlq")
                .build();
    }
    
    // ===================== 事件队列配置 =====================
    
    /**
     * 业务事件队列 - 处理任务、支付、订单等业务事件
     */
    @Bean
    public Queue businessEventQueue() {
        return QueueBuilder
                .durable(BUSINESS_EVENT_QUEUE)
                .withArgument("x-message-ttl", messageProperties.getQueue().getMessageTtl())
                .withArgument("x-max-length", messageProperties.getQueue().getMaxLength())
                .withArgument("x-dead-letter-exchange", MESSAGE_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "business.event.dlq")
                .build();
    }
    
    /**
     * 设备事件队列 - 处理设备状态、告警等事件
     */
    @Bean
    public Queue deviceEventQueue() {
        return QueueBuilder
                .durable(DEVICE_EVENT_QUEUE)
                .withArgument("x-message-ttl", messageProperties.getQueue().getMessageTtl())
                .withArgument("x-max-length", messageProperties.getQueue().getMaxLength())
                .withArgument("x-dead-letter-exchange", MESSAGE_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "device.event.dlq")
                .build();
    }
    
    /**
     * 用户事件队列 - 处理用户行为、权限变更等事件
     */
    @Bean
    public Queue userEventQueue() {
        return QueueBuilder
                .durable(USER_EVENT_QUEUE)
                .withArgument("x-message-ttl", messageProperties.getQueue().getMessageTtl())
                .withArgument("x-max-length", messageProperties.getQueue().getMaxLength())
                .withArgument("x-dead-letter-exchange", MESSAGE_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "user.event.dlq")
                .build();
    }
    
    /**
     * 系统监控队列 - 处理系统性能、健康检查等事件
     */
//    @Bean
    public Queue systemNotificationQueue() {
        return QueueBuilder
                .durable(SYSTEM_NOTIFICATION_QUEUE)
                .withArgument("x-message-ttl", messageProperties.getQueue().getMessageTtl())
                .withArgument("x-max-length", messageProperties.getQueue().getMaxLength())
                .withArgument("x-dead-letter-exchange", MESSAGE_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "system.notification.dlq")
                .build();
    }
    
    // ===================== 死信队列配置 =====================
    
    /**
     * 消息处理失败队列
     */
    @Bean
    public Queue messageFailedQueue() {
        return QueueBuilder
                .durable(MESSAGE_FAILED_QUEUE)
                .withArgument("x-message-ttl", 24 * 60 * 60 * 1000) // 24小时TTL
                .build();
    }
    
    /**
     * 设备事件处理失败队列
     */
    @Bean
    public Queue deviceFailedQueue() {
        return QueueBuilder
                .durable(DEVICE_FAILED_QUEUE)
                .withArgument("x-message-ttl", 24 * 60 * 60 * 1000)
                .build();
    }
    
    /**
     * 用户事件处理失败队列
     */
    @Bean
    public Queue userFailedQueue() {
        return QueueBuilder
                .durable(USER_FAILED_QUEUE)
                .withArgument("x-message-ttl", 24 * 60 * 60 * 1000)
                .build();
    }
    
    /**
     * 业务事件处理失败队列
     */
    @Bean
    public Queue businessFailedQueue() {
        return QueueBuilder
                .durable(BUSINESS_FAILED_QUEUE)
                .withArgument("x-message-ttl", 24 * 60 * 60 * 1000)
                .build();
    }
    
    /**
     * 系统事件处理失败队列
     */
    @Bean
    public Queue systemFailedQueue() {
        return QueueBuilder
                .durable(SYSTEM_FAILED_QUEUE)
                .withArgument("x-message-ttl", 24 * 60 * 60 * 1000)
                .build();
    }
    
    /**
     * 通知队列绑定
     */
    @Bean
    public Binding notificationBinding() {
        return BindingBuilder
                .bind(notificationQueue())
                .to(messageExchange())
                .with(NOTIFICATION_ROUTING_KEY);
    }
    
    /**
     * 系统队列绑定
     */
    @Bean
    public Binding systemBinding() {
        return BindingBuilder
                .bind(systemQueue())
                .to(messageExchange())
                .with(SYSTEM_ROUTING_KEY);
    }
    
    /**
     * 用户队列绑定
     */
    @Bean
    public Binding userBinding() {
        return BindingBuilder
                .bind(userQueue())
                .to(messageExchange())
                .with(USER_ROUTING_KEY);
    }
    
    // ===================== 事件队列绑定配置 =====================
    
    /**
     * 业务事件队列绑定
     */
    @Bean
    public Binding businessEventBinding() {
        return BindingBuilder
                .bind(businessEventQueue())
                .to(eventExchange())
                .with(BUSINESS_EVENT_ROUTING_KEY);
    }
    
    /**
     * 设备事件队列绑定
     */
    @Bean
    public Binding deviceEventBinding() {
        return BindingBuilder
                .bind(deviceEventQueue())
                .to(eventExchange())
                .with(DEVICE_EVENT_ROUTING_KEY);
    }
    
    /**
     * 用户事件队列绑定
     */
    @Bean
    public Binding userEventBinding() {
        return BindingBuilder
                .bind(userEventQueue())
                .to(eventExchange())
                .with(USER_EVENT_ROUTING_KEY);
    }
    
    /**
     * 系统监控队列绑定
     */
//    @Bean
    public Binding systemNotificationBinding() {
        return BindingBuilder
                .bind(systemNotificationQueue())
                .to(eventExchange())
                .with(SYSTEM_EVENT_ROUTING_KEY);
    }
    
    // ===================== 死信队列绑定配置 =====================
    
    /**
     * 消息失败队列绑定
     */
    @Bean
    public Binding messageFailedBinding() {
        return BindingBuilder
                .bind(messageFailedQueue())
                .to(messageDlxExchange())
                .with("message.dlq");
    }
    
    /**
     * 设备事件失败队列绑定
     */
    @Bean
    public Binding deviceFailedBinding() {
        return BindingBuilder
                .bind(deviceFailedQueue())
                .to(messageDlxExchange())
                .with("device.event.dlq");
    }
    
    /**
     * 用户事件失败队列绑定
     */
    @Bean
    public Binding userFailedBinding() {
        return BindingBuilder
                .bind(userFailedQueue())
                .to(messageDlxExchange())
                .with("user.event.dlq");
    }
    
    /**
     * 业务事件失败队列绑定
     */
    @Bean
    public Binding businessFailedBinding() {
        return BindingBuilder
                .bind(businessFailedQueue())
                .to(messageDlxExchange())
                .with("business.event.dlq");
    }
    
    /**
     * 系统事件失败队列绑定
     */
    @Bean
    public Binding systemFailedBinding() {
        return BindingBuilder
                .bind(systemFailedQueue())
                .to(messageDlxExchange())
                .with("system.notification.dlq");
    }
    
    /*
     * ===================== 性能优化说明 =====================
     * 
     * 队列性能优化策略：
     * 
     * 1. 队列设计优化：
     *    - 按业务类型分离队列，避免不同优先级消息相互影响
     *    - 使用Topic交换机实现灵活的路由策略
     *    - 设置合理的队列长度限制和TTL，防止消息积压
     * 
     * 2. 消费者配置优化：
     *    - 并发消费者数量：1-3个，避免资源竞争
     *    - 预取数量：5个消息，平衡内存和吞吐量
     *    - 手动确认模式：保证消息可靠性
     * 
     * 3. 连接和通道管理：
     *    - 使用连接池复用连接
     *    - 合理设置心跳间隔
     *    - 启用发布确认机制
     * 
     * 4. 死信队列设计：
     *    - 24小时TTL，避免失败消息无限堆积
     *    - 按业务类型分离死信队列，便于问题排查
     *    - 定期清理过期的死信消息
     * 
     * 5. 监控建议：
     *    - 监控队列长度，及时发现消息积压
     *    - 监控消费速率，评估处理能力
     *    - 监控死信队列，及时处理失败消息
     *    - 监控连接数和通道数，避免资源泄露
     * 
     * 当前配置支持的队列数量：
     * - 消息队列：3个 (notification, system, user)
     * - 事件队列：4个 (business, device, user, system)  
     * - 死信队列：5个 (message, device, user, business, system)
     * 总计：12个队列
     * 
     * 在中等负载下，这个数量对RabbitMQ性能影响有限。
     * 如果队列数量继续增长，建议：
     * 1. 考虑队列合并或分片策略
     * 2. 使用多个RabbitMQ实例做集群
     * 3. 引入消息路由规则优化
     */

}