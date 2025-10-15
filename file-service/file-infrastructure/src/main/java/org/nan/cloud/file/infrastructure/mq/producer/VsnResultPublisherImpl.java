package org.nan.cloud.file.infrastructure.mq.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.mq.core.message.Message;
import org.nan.cloud.common.mq.producer.MessageProducer;
import org.nan.cloud.common.mq.producer.SendResult;
import org.nan.cloud.file.application.port.VsnResultPublisher;
import org.springframework.stereotype.Service;
import org.nan.cloud.file.application.config.FileStorageProperties;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class VsnResultPublisherImpl implements VsnResultPublisher {

    private static final String BUSINESS_EXCHANGE = "business.topic";

    private final MessageProducer messageProducer;
    private final FileStorageProperties fileStorageProperties;

    @Override
    public void publishResultCompleted(Long orgId, Long programId, Integer version,
                                       String vsnFileId, String vsnFilePath, String thumbnailPath) {
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("eventType", "COMPLETED");
        payload.put("programId", programId);
        payload.put("version", version);
        payload.put("organizationId", orgId);
        payload.put("status", "COMPLETED");
        payload.put("vsnFileId", vsnFileId);
        payload.put("vsnFilePath", vsnFilePath);
        payload.put("thumbnailPath", thumbnailPath);
        payload.put("timestamp", LocalDateTime.now());
        // 附加可用信息：URL 与文件大小
        String urlPrefix = fileStorageProperties.getStorage().getLocal().getUrlPrefix();
        if (urlPrefix != null) {
            String normalized = urlPrefix.endsWith("/") ? urlPrefix.substring(0, urlPrefix.length() - 1) : urlPrefix;
            payload.put("vsnFileUrl", normalized + "/" + vsnFilePath.replace('\\','/'));
        }
        try {
            java.nio.file.Path p = java.nio.file.Paths.get(fileStorageProperties.getStorage().getLocal().getBasePath(), vsnFilePath);
            if (java.nio.file.Files.exists(p)) {
                payload.put("vsnFileSize", java.nio.file.Files.size(p));
            }
        } catch (Exception ignored) {}

        send("program.vsn.result", orgId, programId, payload);
    }

    @Override
    public void publishResultFailed(Long orgId, Long programId, Integer version, String errorMessage, String errorDetails) {
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("eventType", "FAILED");
        payload.put("programId", programId);
        payload.put("version", version);
        payload.put("organizationId", orgId);
        payload.put("status", "FAILED");
        payload.put("errorMessage", errorMessage);
        payload.put("errorDetails", errorDetails);
        payload.put("timestamp", LocalDateTime.now());

        send("program.vsn.result", orgId, programId, payload);
    }

    private void send(String subject, Long orgId, Long programId, Object payload) {
        String routingKey = String.format("%s.%d.%d", subject, orgId, programId);

        Message message = Message.builder()
                .messageType("EVENT")
                .subject(subject)
                .payload(payload)
                .organizationId(String.valueOf(orgId))
                .exchange(BUSINESS_EXCHANGE)
                .routingKey(routingKey)
                .priority(5)
                .sourceSystem("file-service")
                .targetSystem("core-service")
                .build();

        SendResult result = messageProducer.send(message);
        if (result.isSuccess()) {
            log.info("✅ 已推送VSN结果: rk={}, msgId={}", routingKey, result.getMessageId());
        } else {
            log.error("❌ 推送VSN结果失败: rk={}, err={}", routingKey, result.getErrorMessage());
        }
    }
}

