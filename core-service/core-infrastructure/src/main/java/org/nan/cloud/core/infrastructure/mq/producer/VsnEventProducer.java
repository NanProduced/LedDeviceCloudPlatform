package org.nan.cloud.core.infrastructure.mq.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.mq.core.message.Message;
import org.nan.cloud.common.mq.producer.MessageProducer;
import org.nan.cloud.common.mq.producer.SendResult;
import org.nan.cloud.core.event.mq.VsnGenerationRequestEvent;
import org.springframework.stereotype.Component;

/**
 * 核心服务 VSN 请求事件生产者
 * 交换器：business.topic
 * 路由键：program.vsn.generate.{orgId}.{programId}
 * messageType: EVENT, subject: program.vsn.generate
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VsnEventProducer {

    private static final String BUSINESS_EXCHANGE = "business.topic";

    private final MessageProducer messageProducer;

    public void sendGenerate(VsnGenerationRequestEvent event) {
        String subject = "program.vsn.generate";
        String routingKey = String.format("%s.%d.%d", subject, event.getOrganizationId(), event.getProgramId());

        Message message = Message.builder()
                .messageType("EVENT")
                .subject(subject)
                .payload(event)
                .organizationId(String.valueOf(event.getOrganizationId()))
                .exchange(BUSINESS_EXCHANGE)
                .routingKey(routingKey)
                .priority(5)
                .sourceSystem("core-service")
                .targetSystem("file-service")
                .build();

        SendResult result = messageProducer.send(message);
        if (result.isSuccess()) {
            log.info("✅ 已发布VSN生成请求: rk={}, msgId={}", routingKey, result.getMessageId());
        } else {
            log.error("❌ 发布VSN生成请求失败: rk={}, err={}", routingKey, result.getErrorMessage());
        }
    }
}

