package org.nan.cloud.core.infrastructure.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Binding.DestinationType;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Core-Service RabbitMQ 基础绑定配置
 *
 * - 仅声明与转码相关的绑定，复用既有的 business.topic 交换器与 business.core.queue 队列
 * - 避免在此处重复声明队列以与其他服务的参数冲突
 */
@Configuration
public class CoreRabbitConfig {

    /**
     * 业务事件交换器（幂等声明）
     */
    @Bean
    public TopicExchange businessExchange() {
        return new TopicExchange("business.topic", true, false);
    }

    /**
     * 将转码事件绑定到核心业务队列
     * 路由键：file.transcoding.{eventType}.{orgId}.{refId}
     */
    @Bean
    public Binding transcodingBusinessBinding(TopicExchange businessExchange) {
        return new Binding(
                "business.core.queue",
                DestinationType.QUEUE,
                businessExchange.getName(),
                "file.transcoding.*.*.*",
                null
        );
    }
}

