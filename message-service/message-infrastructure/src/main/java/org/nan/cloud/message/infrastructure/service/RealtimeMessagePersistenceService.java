package org.nan.cloud.message.infrastructure.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.domain.RealtimeMessageDocument;
import org.nan.cloud.message.api.stomp.CommonStompMessage;
import org.nan.cloud.message.infrastructure.mongodb.repository.RealtimeMessageRepository;
import org.nan.cloud.message.infrastructure.websocket.dispatcher.DispatchResult;
import org.nan.cloud.message.infrastructure.websocket.manager.StompConnectionManager;
import org.nan.cloud.message.infrastructure.websocket.stomp.enums.StompTopic;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeMessagePersistenceService {

    private final RealtimeMessageRepository messageRepository;
    private final StompConnectionManager connectionManager;

    /**
     * 智能消息持久化策略
     * - 用户在线：先STOMP推送，再异步持久化
     * - 用户离线：直接同步持久化
     */
    @Async("messageExecutor")
    public void persistMessageAsync(CommonStompMessage stompMessage, DispatchResult dispatchResult) {
        try {
            // 判断是否需要持久化为实时消息
            if (Objects.nonNull(dispatchResult) && !shouldPersistAsRealtimeMessage(stompMessage, dispatchResult)) {
                return;
            }

            Long userId = extractUserId(stompMessage);
            if (userId == null) {
                log.warn("无法提取用户ID，跳过持久化 - 消息ID: {}", stompMessage.getMessageId());
                return;
            }

            // 检查用户在线状态
            boolean isUserOnline = connectionManager.isUserOnline(userId.toString());

            // 构建持久化文档
            RealtimeMessageDocument document = buildRealtimeMessageDocument(stompMessage);

            if (isUserOnline) {
                // 用户在线：异步持久化
                persistMessageForOnlineUser(document);
            } else {
                // 用户离线：优先持久化，后续推送时从数据库获取
                persistMessageForOfflineUser(document);
            }

        } catch (Exception e) {
            log.error("消息持久化失败 - 消息ID: {}, 错误: {}", stompMessage.getMessageId(), e.getMessage(), e);
        }
    }

    private void persistMessageForOnlineUser(RealtimeMessageDocument document) {
        // 用户在线时，降低持久化优先级，确保实时推送优先
        CompletableFuture.runAsync(() -> {
            messageRepository.save(document);
            log.debug("在线用户消息持久化完成 - 用户: {}, 消息: {}", document.getUid(), document.getMessageId());
        });
    }

    private void persistMessageForOfflineUser(RealtimeMessageDocument document) {
        // 用户离线时，同步持久化确保消息不丢失
        String savedId = messageRepository.save(document);
        log.info("离线用户消息持久化完成 - 用户: {}, 消息: {}, ID: {}",
                document.getUid(), document.getMessageId(), savedId);
    }

    private RealtimeMessageDocument buildRealtimeMessageDocument(CommonStompMessage stompMessage) {
        return RealtimeMessageDocument.builder()
                .messageId(stompMessage.getMessageId())
                .timestamp(stompMessage.getTimestamp())
                .oid(stompMessage.getOid())
                .uid(extractUserId(stompMessage))
                .messageType(stompMessage.getMessageType().name())
                .subType_1(stompMessage.getSubType_1())
                .subType_2(stompMessage.getSubType_2())
                .level(stompMessage.getLevel() != null ? stompMessage.getLevel().name() : null)
                .requireAsk(stompMessage.getRequireAck())
                .isRead(false) // 默认未读
                .title(stompMessage.getTitle())
                .content(stompMessage.getContent())
                .payload(stompMessage.getPayload())
                .indexKey(buildIndexKey(stompMessage))
                .build();
    }

    private String buildIndexKey(CommonStompMessage stompMessage) {
        return String.format("%s_%s_%s",
                stompMessage.getOid(),
                extractUserId(stompMessage),
                stompMessage.getMessageType().name());
    }

    private boolean shouldPersistAsRealtimeMessage(CommonStompMessage message, DispatchResult result) {
        // 只持久化发送到用户个人队列的消息
        return result.getSuccessfulTopics().stream()
                .anyMatch(topic -> topic.equals(StompTopic.USER_MESSAGES_QUEUE));
    }

    private Long extractUserId(CommonStompMessage message) {
        if (message.getContext() != null && message.getContext().getUid() != null) {
            return message.getContext().getUid();
        }
        return null;
    }


}
