package org.nan.cloud.file.infrastructure.mq.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * File-Service RabbitMQ配置
 * 
 * 负责声明file-service使用的所有MQ资源:
 * - 交换器: business.topic, business.dlx.topic, stomp.push.topic
 * - 队列: business.file.queue
 * - 绑定: VSN生成请求路由绑定
 * 
 * 设计原则:
 * - 队列自动声明，避免启动时404错误
 * - TTL和死信处理，保证消息可靠性
 * - 与common-mq模块协作，统一消息处理
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableRabbit
public class FileServiceRabbitConfig {
    
    // ==================== 交换器名称常量 ====================
    
    public static final String BUSINESS_EXCHANGE = "business.topic";
    public static final String BUSINESS_DLX_EXCHANGE = "business.dlx.topic";
    public static final String STOMP_PUSH_EXCHANGE = "stomp.push.topic";
    
    // ==================== 队列名称常量 ====================
    
    public static final String BUSINESS_FILE_QUEUE = "business.file.queue";
    
    // ==================== 路由键常量 ====================
    
    public static final String VSN_GENERATE_ROUTING_KEY = "program.vsn.generate.*.*";
    public static final String FILE_TRANSCODING_CREATED_ROUTING_KEY = "file.transcoding.created.*.*";
    public static final String FILE_BUSINESS_DLQ_ROUTING_KEY = "file.business.dlq";
    
    // ==================== 交换器配置 ====================
    
    /**
     * 业务事件交换器 - 发布文件上传业务事件到core-service
     */
    @Bean
    public TopicExchange businessTopicExchange() {
        return ExchangeBuilder
                .topicExchange(BUSINESS_EXCHANGE)
                .durable(true)
                .build();
    }
    
    /**
     * 业务死信交换器 - 文件服务DLQ绑定
     */
    @Bean
    public TopicExchange businessDlxExchange() {
        return ExchangeBuilder
                .topicExchange(BUSINESS_DLX_EXCHANGE)
                .durable(true)
                .build();
    }
    
    /**
     * STOMP推送交换器 - 发布实时消息到message-service
     */
    @Bean
    public TopicExchange stompPushExchange() {
        return ExchangeBuilder
                .topicExchange(STOMP_PUSH_EXCHANGE)
                .durable(true)
                .build();
    }
    
    // ==================== 队列配置 ====================
    
    /**
     * 文件服务业务队列 - 接收文件相关请求(含VSN生成)
     * 
     * 配置说明:
     * - TTL: 15分钟 (900000ms) - VSN生成需要足够处理时间
     * - 死信交换器: business.dlx.topic
     * - 死信路由键: file.business.dlq
     */
    @Bean
    public Queue businessFileQueue() {
        return QueueBuilder
                .durable(BUSINESS_FILE_QUEUE)
                .withArgument("x-dead-letter-exchange", BUSINESS_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", FILE_BUSINESS_DLQ_ROUTING_KEY)
                .withArgument("x-message-ttl", 900000) // 15分钟TTL
                .build();
    }
    
    // ==================== 绑定配置 ====================
    
    /**
     * VSN生成请求绑定 - 路由键: program.vsn.generate.{orgId}.{programId}
     * 
     * 绑定说明:
     * - 从 business.topic 交换器绑定到 business.file.queue 队列
     * - 路由键模式: program.vsn.generate.*.*
     * - 用于接收来自core-service的VSN生成请求
     */
    @Bean
    public Binding vsnGenerateBinding() {
        return BindingBuilder
                .bind(businessFileQueue())
                .to(businessTopicExchange())
                .with(VSN_GENERATE_ROUTING_KEY);
    }

    /**
     * 转码任务创建绑定 - 路由键: file.transcoding.created.{orgId}.{materialId}
     *
     * 用于接收来自 core-service 的转码任务下发事件
     */
    @Bean
    public Binding transcodingCreatedBinding() {
        return BindingBuilder
                .bind(businessFileQueue())
                .to(businessTopicExchange())
                .with(FILE_TRANSCODING_CREATED_ROUTING_KEY);
    }
    
    /**
     * 初始化完成日志
     */
    @Bean
    public String fileServiceRabbitInitialization() {
        log.info("✅ File-Service RabbitMQ配置初始化完成:");
        log.info("   📡 交换器: {}, {}, {}", BUSINESS_EXCHANGE, BUSINESS_DLX_EXCHANGE, STOMP_PUSH_EXCHANGE);
        log.info("   📥 队列: {} (TTL: 15分钟)", BUSINESS_FILE_QUEUE);
        log.info("   🔗 绑定: {} -> {} (路由键: {})", 
                BUSINESS_EXCHANGE, BUSINESS_FILE_QUEUE, VSN_GENERATE_ROUTING_KEY);
        log.info("   💀 死信: {} -> {}", BUSINESS_DLX_EXCHANGE, FILE_BUSINESS_DLQ_ROUTING_KEY);
        return "file-service-rabbit-initialized";
    }
}