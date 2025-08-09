package org.nan.cloud.file.infrastructure.mq.config;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.mq.consumer.MessageConsumer;
import org.nan.cloud.common.mq.consumer.MessageConsumerManager;
import org.nan.cloud.common.mq.core.config.MqProperties;
import org.nan.cloud.common.mq.core.message.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * File-Service 消息消费者管理器
 * 监听 business.file.queue 并路由到具体的 MessageConsumer
 */
@Slf4j
@Primary
@Component
public class FileServiceMessageConsumerManager extends MessageConsumerManager {

    public FileServiceMessageConsumerManager(List<MessageConsumer> messageConsumers, MqProperties mqProperties) {
        super(messageConsumers, mqProperties);
    }

    /**
     * 处理文件服务业务消息
     * 队列：business.file.queue
     * 路由键：program.vsn.generate.*.* 等
     */
    @RabbitListener(queues = "business.file.queue")
    @RabbitHandler
    public void handleFileBusinessMessage(Message message, Channel channel,
                                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.debug("📥 File-Service收到业务消息: messageId={}, type={}",
                message.getMessageId(), message.getMessageType());

        // 委托给父类通用处理
        super.handleMessage(message, channel, deliveryTag, "file-business");
    }
}

