package org.nan.cloud.core.infrastructure.mq.producer;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.mq.core.message.Message;
import org.nan.cloud.common.mq.producer.MessageProducer;
import org.nan.cloud.core.service.TranscodeEventPublisher;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TranscodeEventPublisherImpl implements TranscodeEventPublisher {

    private static final String BUSINESS_EXCHANGE = "business.topic";

    private final MessageProducer messageProducer;

    @Override
    public void publishTranscodeTask(Long materialId, String taskId, Long oid, Long Ugid, Long uid) {

        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "CREATED");
        payload.put("taskId", taskId);
        payload.put("organizationId", String.valueOf(oid));
        payload.put("userId", String.valueOf(uid));
        payload.put("userGroupId", String.valueOf(Ugid));
        payload.put("sourceMaterialId", materialId);

        Message message = Message.builder()
                .messageType("FILE_TRANSCODING_CREATED")
                .payload(payload)
                .organizationId(String.valueOf(oid))
                .exchange(BUSINESS_EXCHANGE)
                .routingKey(String.format("file.transcoding.created.%s.%s", oid, materialId))
                .priority(3)
                .sourceSystem("core-service")
                .targetSystem("file-service")
                .build();
        messageProducer.send(message);
    }
}
