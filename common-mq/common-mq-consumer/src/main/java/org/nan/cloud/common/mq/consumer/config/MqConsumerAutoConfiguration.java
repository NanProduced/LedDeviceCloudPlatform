package org.nan.cloud.common.mq.consumer.config;

import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.mq.core.config.MqProperties;
import org.nan.cloud.common.mq.consumer.MessageConsumer;
import org.nan.cloud.common.mq.consumer.MessageConsumerManager;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * 消息消费者自动配置
 * 
 * 自动配置消息消费者相关的Bean，包括监听器容器工厂、消费者管理器等。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass({MessageConsumer.class, MessageConsumerManager.class})
@ConditionalOnProperty(prefix = "nan.mq", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MqProperties.class)
public class MqConsumerAutoConfiguration {
    
    /**
     * RabbitMQ监听器容器工厂配置
     */
    @Bean
    @ConditionalOnMissingBean(name = "rabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter,
            MqProperties mqProperties) {
        
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        
        MqProperties.Consumer consumerConfig = mqProperties.getConsumer();
        
        // 并发消费者配置
        factory.setConcurrentConsumers(consumerConfig.getConcurrency());
        factory.setMaxConcurrentConsumers(consumerConfig.getMaxConcurrency());
        
        // 预取配置
        factory.setPrefetchCount(consumerConfig.getPrefetchCount());
        
        // 确认模式
        AcknowledgeMode ackMode = "auto".equalsIgnoreCase(consumerConfig.getAcknowledgeMode()) 
            ? AcknowledgeMode.AUTO : AcknowledgeMode.MANUAL;
        factory.setAcknowledgeMode(ackMode);
        
        // 错误处理配置
        factory.setDefaultRequeueRejected(consumerConfig.isDefaultRequeueRejected());
        
        // 超时和连接配置
        if (consumerConfig.getReceiveTimeout() != null) {
            factory.setReceiveTimeout(consumerConfig.getReceiveTimeout().toMillis());
        }
        
        if (consumerConfig.getIdleEventInterval() != null) {
            factory.setIdleEventInterval(consumerConfig.getIdleEventInterval().toMillis());
        }
        
        // 容错配置
        factory.setMissingQueuesFatal(consumerConfig.isMissingQueuesFatal());
        
        // 连接恢复配置
        if (consumerConfig.getRecoveryInterval() != null) {
            factory.setRecoveryInterval(consumerConfig.getRecoveryInterval().toMillis());
        }
        
        factory.setConsecutiveActiveTrigger(consumerConfig.getConsecutiveActiveTrigger());
        factory.setConsecutiveIdleTrigger(consumerConfig.getConsecutiveIdleTrigger());
        
        log.info("配置RabbitMQ监听器容器工厂: concurrency={}-{}, prefetch={}, ackMode={}", 
                consumerConfig.getConcurrency(),
                consumerConfig.getMaxConcurrency(),
                consumerConfig.getPrefetchCount(),
                ackMode);
        
        return factory;
    }
    
    /**
     * 消息消费者管理器
     */
    @Bean
    @ConditionalOnMissingBean(MessageConsumerManager.class)
    public MessageConsumerManager messageConsumerManager(List<MessageConsumer> messageConsumers,
                                                        MqProperties mqProperties) {
        
        log.info("配置消息消费者管理器: 注册消费者数量={}", messageConsumers.size());
        
        // 打印注册的消费者信息
        messageConsumers.forEach(consumer -> {
            String supportedTypes = consumer.getSupportedMessageTypes().length == 0 ? 
                "ALL" : String.join(",", consumer.getSupportedMessageTypes());
            log.info("注册消费者: {} (支持类型: {})", consumer.getConsumerId(), supportedTypes);
        });
        
        return new MessageConsumerManager(messageConsumers, mqProperties);
    }
    
    /**
     * 消费者健康检查端点（如果Actuator可用）
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.boot.actuate.health.HealthIndicator")
    @ConditionalOnProperty(prefix = "nan.mq.monitor", name = "health-check-enabled", havingValue = "true", matchIfMissing = true)
    public MqConsumerHealthIndicator mqConsumerHealthIndicator(MessageConsumerManager consumerManager) {
        log.info("配置消息消费者健康检查");
        return new MqConsumerHealthIndicator(consumerManager);
    }
}