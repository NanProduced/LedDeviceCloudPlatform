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
    
    // 交换机名称
    public static final String MESSAGE_EXCHANGE = "message.exchange";
    public static final String MESSAGE_DLX_EXCHANGE = "message.dlx.exchange";
    
    // 队列名称
    public static final String MESSAGE_NOTIFICATION_QUEUE = "message.notification.queue";
    public static final String MESSAGE_SYSTEM_QUEUE = "message.system.queue";
    public static final String MESSAGE_USER_QUEUE = "message.user.queue";
    
    // 路由键
    public static final String NOTIFICATION_ROUTING_KEY = "message.notification";
    public static final String SYSTEM_ROUTING_KEY = "message.system";
    public static final String USER_ROUTING_KEY = "message.user";
    
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
     * 监听器容器工厂配置
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        
        // 设置并发消费者数量
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(5);
        
        // 设置预取数量
        factory.setPrefetchCount(10);
        
        // 启用手动确认
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        
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
    
    /**
     * 通配符绑定 - 所有消息类型
     */
    @Bean
    public Binding allMessagesBinding() {
        return BindingBuilder
                .bind(notificationQueue())
                .to(messageExchange())
                .with("message.*");
    }

}