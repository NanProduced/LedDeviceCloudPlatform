package org.nan.cloud.common.mq.producer.config;

import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.mq.core.config.MqProperties;
import org.nan.cloud.common.mq.core.serializer.JsonMessageSerializer;
import org.nan.cloud.common.mq.core.serializer.MessageSerializer;
import org.nan.cloud.common.mq.producer.MessageProducer;
import org.nan.cloud.common.mq.producer.impl.DefaultMessageProducer;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 消息生产者自动配置
 * 
 * 自动配置消息生产者相关的Bean，包括RabbitTemplate、MessageSerializer等。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass({RabbitTemplate.class, MessageProducer.class})
@ConditionalOnProperty(prefix = "nan.mq", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MqProperties.class)
public class MqProducerAutoConfiguration {
    
    /**
     * 消息序列化器
     */
    @Bean
    @ConditionalOnMissingBean(MessageSerializer.class)
    public MessageSerializer messageSerializer(MqProperties mqProperties) {
        MqProperties.Serialization serialization = mqProperties.getSerialization();
        
        JsonMessageSerializer serializer = new JsonMessageSerializer(
            serialization.isCompressionEnabled(),
            serialization.getCompressionThreshold()
        );
        
        log.info("配置消息序列化器: type={}, compression={}, threshold={}", 
                serialization.getType(), 
                serialization.isCompressionEnabled(),
                serialization.getCompressionThreshold());
        
        return serializer;
    }
    
    /**
     * RabbitMQ消息转换器
     */
    @Bean
    @ConditionalOnMissingBean(MessageConverter.class)
    public MessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        
        // 配置对象映射器
        converter.setCreateMessageIds(true);
        
        log.info("配置RabbitMQ消息转换器: {}", converter.getClass().getSimpleName());
        return converter;
    }
    
    /**
     * RabbitTemplate配置
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(RabbitTemplate.class)
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, 
                                       MessageConverter messageConverter,
                                       MqProperties mqProperties) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        
        MqProperties.Producer producerConfig = mqProperties.getProducer();
        
        // 配置发布确认
        if (producerConfig.isConfirmEnabled()) {
            template.setConfirmCallback((correlationData, ack, cause) -> {
                if (ack) {
                    log.debug("✅ 消息发布确认成功: correlationData={}", correlationData);
                } else {
                    log.error("❌ 消息发布确认失败: correlationData={}, cause={}", correlationData, cause);
                }
            });
        }
        
        // 配置返回回调
        if (producerConfig.isReturnsEnabled()) {
            template.setReturnsCallback(returned -> {
                log.warn("⚠️ 消息被退回: exchange={}, routingKey={}, replyText={}, message={}", 
                        returned.getExchange(), 
                        returned.getRoutingKey(), 
                        returned.getReplyText(),
                        returned.getMessage());
            });
        }
        
        // 配置发送超时
        if (producerConfig.getSendTimeout() != null) {
            template.setReceiveTimeout(producerConfig.getSendTimeout().toMillis());
        }
        
        log.info("配置RabbitTemplate: confirmEnabled={}, returnsEnabled={}, sendTimeout={}ms", 
                producerConfig.isConfirmEnabled(),
                producerConfig.isReturnsEnabled(),
                producerConfig.getSendTimeout() != null ? producerConfig.getSendTimeout().toMillis() : "default");
        
        return template;
    }
    
    /**
     * 消息生产者
     */
    @Bean
    @ConditionalOnMissingBean(MessageProducer.class)
    public MessageProducer messageProducer(RabbitTemplate rabbitTemplate,
                                         MessageSerializer messageSerializer,
                                         MqProperties mqProperties) {
        
        DefaultMessageProducer producer = new DefaultMessageProducer(
            rabbitTemplate, messageSerializer, mqProperties
        );
        
        log.info("配置消息生产者: applicationName={}, defaultExchange={}", 
                mqProperties.getApplicationName(),
                mqProperties.getDefaultExchange().getName());
        
        return producer;
    }
    
    /**
     * 生产者健康检查端点（如果Actuator可用）
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.boot.actuate.health.HealthIndicator")
    @ConditionalOnProperty(prefix = "nan.mq.monitor", name = "health-check-enabled", havingValue = "true", matchIfMissing = true)
    public MqProducerHealthIndicator mqProducerHealthIndicator(MessageProducer messageProducer) {
        log.info("配置消息生产者健康检查");
        return new MqProducerHealthIndicator(messageProducer);
    }
}