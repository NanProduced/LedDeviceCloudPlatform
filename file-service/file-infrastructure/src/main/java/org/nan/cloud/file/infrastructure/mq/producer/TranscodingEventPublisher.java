package org.nan.cloud.file.infrastructure.mq.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.mq.core.message.Message;
import org.nan.cloud.common.mq.producer.MessageProducer;
import org.nan.cloud.common.mq.producer.SendResult;
import org.nan.cloud.file.application.domain.TaskContext;
import org.nan.cloud.file.application.service.TaskContextService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranscodingEventPublisher {

    private final MessageProducer messageProducer;
    private final TaskContextService taskContextService;

    private static final String STOMP_PUSH_EXCHANGE = "stomp.push.topic";
    private static final String BUSINESS_EXCHANGE = "business.topic";

    private static final String STOMP_ROUTING_KEY_TEMPLATE = "stomp.file.transcoding.%s.%s"; // {orgId}.{userId}
    private static final String BUSINESS_ROUTING_KEY_TEMPLATE = "file.transcoding.%s.%s.%s"; // {event}.{orgId}.{refId}

    public void publishTranscodingStarted(String taskId, Long oid, Long uid, Long sourceMaterialId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "STARTED");
        payload.put("taskId", taskId);
        payload.put("organizationId", String.valueOf(oid));
        payload.put("userId", String.valueOf(uid));
        payload.put("sourceMaterialId", sourceMaterialId);
        payload.put("timestamp", LocalDateTime.now());
        publishBusinessMessage("FILE_TRANSCODING_STARTED", payload, String.valueOf(oid), String.valueOf(sourceMaterialId), "转码开始");
        publishStomp(taskId, oid, uid, payload, "转码开始");
    }

    public void publishTranscodingProgress(String taskId, int progress, String status) {
        TaskContext ctx = taskContextService.getTaskContext(taskId);
        if (ctx == null) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "PROGRESS");
        payload.put("taskId", taskId);
        payload.put("progress", progress);
        payload.put("status", status);
        payload.put("timestamp", LocalDateTime.now());
        publishStomp(taskId, ctx.getOid(), ctx.getUid(), payload, "转码进度");
    }

    public void publishTranscodingCompleted(String taskId, Long oid, Long uid, Long sourceMaterialId,
                                            String sourceFileId, String targetFileId, String storagePath,
                                            String mimeType, Long fileSize, String fileExtension,
                                            String metadataId, String thumbnailPath, String presetName,
                                            String transcodeDetailId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "COMPLETED");
        payload.put("taskId", taskId);
        payload.put("organizationId", String.valueOf(oid));
        payload.put("userId", String.valueOf(uid));
        payload.put("sourceMaterialId", sourceMaterialId);
        payload.put("sourceFileId", sourceFileId);
        payload.put("targetFileId", targetFileId);
        payload.put("storagePath", storagePath);
        payload.put("mimeType", mimeType);
        payload.put("fileSize", fileSize);
        payload.put("fileExtension", fileExtension);
        payload.put("metadataId", metadataId);
        payload.put("thumbnailPath", thumbnailPath);
        payload.put("presetName", presetName);
        payload.put("transcodeDetailId", transcodeDetailId);
        payload.put("timestamp", LocalDateTime.now());
        publishBusinessMessage("FILE_TRANSCODING_COMPLETED", payload, String.valueOf(oid), targetFileId, "转码完成");
        publishStomp(taskId, oid, uid, payload, "转码完成");
    }

    public void publishTranscodingFailed(String taskId, Long oid, Long uid, Long sourceMaterialId, String errorMessage, String transcodeDetailId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "FAILED");
        payload.put("taskId", taskId);
        payload.put("organizationId", String.valueOf(oid));
        payload.put("userId", String.valueOf(uid));
        payload.put("sourceMaterialId", sourceMaterialId);
        payload.put("errorMessage", errorMessage);
        payload.put("transcodeDetailId", transcodeDetailId);
        payload.put("timestamp", LocalDateTime.now());
        publishBusinessMessage("FILE_TRANSCODING_FAILED", payload, String.valueOf(oid), String.valueOf(sourceMaterialId), "转码失败");
        publishStomp(taskId, oid, uid, payload, "转码失败");
    }

    private void publishStomp(String taskId, Long oid, Long uid, Map<String, Object> payload, String subject) {
        String routingKey = String.format(STOMP_ROUTING_KEY_TEMPLATE, oid, uid);
        Message message = Message.builder()
                .messageType("FILE_TRANSCODING")
                .subject(subject)
                .payload(payload)
                .senderId("file-service")
                .receiverId(String.valueOf(uid))
                .organizationId(String.valueOf(oid))
                .exchange(STOMP_PUSH_EXCHANGE)
                .routingKey(routingKey)
                .priority(5)
                .sourceSystem("file-service")
                .targetSystem("message-service")
                .build();
        SendResult result = messageProducer.send(message);
        if (result.isSuccess()) {
            log.debug("✅ STOMP转码消息发送成功 - 路由键: {}, 任务: {}", routingKey, taskId);
        } else {
            log.error("❌ STOMP转码消息发送失败 - 路由键: {}, 任务: {}, 错误: {}", routingKey, taskId, result.getErrorMessage());
        }
    }

    private void publishBusinessMessage(String messageType, Map<String, Object> payload, String organizationId, String refId, String subject) {
        String routingKey = String.format(BUSINESS_ROUTING_KEY_TEMPLATE, messageType.toLowerCase().replace("_", "."), organizationId, refId);
        Message message = Message.builder()
                .messageType(messageType)
                .subject(subject)
                .payload(payload)
                .organizationId(organizationId)
                .exchange(BUSINESS_EXCHANGE)
                .routingKey(routingKey)
                .priority(3)
                .sourceSystem("file-service")
                .targetSystem("core-service")
                .build();
        SendResult result = messageProducer.send(message);
        if (result.isSuccess()) {
            log.debug("✅ 业务转码消息发送成功 - 路由键: {}", routingKey);
        } else {
            log.error("❌ 业务转码消息发送失败 - 路由键: {}, 错误: {}", routingKey, result.getErrorMessage());
        }
    }
}

