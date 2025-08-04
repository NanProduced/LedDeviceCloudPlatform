package org.nan.cloud.core.infrastructure.mq.config;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.mq.core.config.MqProperties;
import org.nan.cloud.common.mq.consumer.MessageConsumer;
import org.nan.cloud.common.mq.consumer.MessageConsumerManager;
import org.nan.cloud.common.mq.core.message.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Core-Service消息消费者管理器
 * 
 * 职责：
 * 1. 监听业务队列消息
 * 2. 路由到具体的消费者处理
 * 3. 统一异常处理和消息确认
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Primary
@Component
public class CoreServiceMessageConsumerManager extends MessageConsumerManager {

    public CoreServiceMessageConsumerManager(List<MessageConsumer> messageConsumers, MqProperties mqProperties) {
        super(messageConsumers, mqProperties);
    }

    /**
     * 处理核心业务消息
     * 队列：business.core.queue
     * 路由键：file.upload.*.*.*
     */
    @RabbitListener(queues = "business.core.queue")
    @RabbitHandler
    public void handleCoreBusinessMessage(Message message, Channel channel,
                                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.debug("📥 Core-Service收到业务消息: messageId={}, type={}", 
                message.getMessageId(), message.getMessageType());
        
        handleMessage(message, channel, deliveryTag, "core-business");
    }
    
    /**
     * 处理任务进度消息
     * 队列：business.task.progress.queue  
     * 路由键：task.progress.*.*
     */
    @RabbitListener(queues = "business.task.progress.queue")
    @RabbitHandler
    public void handleTaskProgressMessage(Message message, Channel channel,
                                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.debug("📈 Core-Service收到任务进度消息: messageId={}, type={}", 
                message.getMessageId(), message.getMessageType());
        
        handleMessage(message, channel, deliveryTag, "task-progress");
    }
}