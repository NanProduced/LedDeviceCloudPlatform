package org.nan.cloud.core.infrastructure.mq.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.mq.core.message.Message;
import org.nan.cloud.common.mq.producer.MessageProducer;
import org.nan.cloud.common.mq.producer.SendResult;
import org.nan.cloud.core.event.mq.VsnGenerationRequestEvent;
import org.nan.cloud.core.service.VsnEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * 基于 common-mq 的 VSN 事件发布实现（Primary）
 * 交换器：business.topic
 * 路由键：program.vsn.generate.{orgId}.{programId}
 * messageType: EVENT, subject: program.vsn.generate
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class VsnEventPublisherImpl implements VsnEventPublisher {

    private static final String BUSINESS_EXCHANGE = "business.topic";

    private final MessageProducer messageProducer;

    @Override
    public void publishVsnGenerationRequest(VsnGenerationRequestEvent event) {
        send("program.vsn.generate", event);
    }

    @Override
    public void publishVsnRegenerationRequest(VsnGenerationRequestEvent event) {
        send("program.vsn.regenerate", event);
    }

    private void send(String subject, VsnGenerationRequestEvent event) {
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
            log.info("✅ 已发布VSN请求: rk={}, msgId={}", routingKey, result.getMessageId());
        } else {
            log.error("❌ 发布VSN请求失败: rk={}, err={}", routingKey, result.getErrorMessage());
        }
    }
}

