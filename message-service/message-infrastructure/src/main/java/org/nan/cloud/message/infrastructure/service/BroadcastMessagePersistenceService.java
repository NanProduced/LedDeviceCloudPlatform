package org.nan.cloud.message.infrastructure.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.domain.BroadcastMessageDocument;
import org.nan.cloud.common.basic.domain.BroadcastMessageReadStatus;
import org.nan.cloud.common.basic.utils.JsonUtils;
import org.nan.cloud.message.api.stomp.CommonStompMessage;
import org.nan.cloud.message.infrastructure.mongodb.repository.BroadcastMessageRepository;
import org.nan.cloud.message.infrastructure.mongodb.repository.BroadcastMessageReadStatusRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 广播消息持久化服务
 * 
 * 负责广播消息在message-service中的存储和管理
 * 基于分离存储架构：消息内容 + 用户已读状态分别存储
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BroadcastMessagePersistenceService {

    private final BroadcastMessageRepository broadcastMessageRepository;
    private final BroadcastMessageReadStatusRepository readStatusRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 异步持久化广播消息内容
     * 
     * @param stompMessage STOMP消息对象
     */
    @Async("messageExecutor")
    public void persistBroadcastMessageAsync(CommonStompMessage stompMessage) {
        try {
            log.debug("开始持久化广播消息 - 消息ID: {}, 类型: {}", 
                    stompMessage.getMessageId(), stompMessage.getMessageType());
            
            // 构建广播消息文档
            BroadcastMessageDocument document = buildBroadcastMessageDocument(stompMessage);
            
            // 持久化消息内容（只存储一份）
            String savedId = broadcastMessageRepository.save(document);
            
            // 发布广播消息持久化事件（用于后续统计和推送）
            publishBroadcastMessageEvent(document);
            
            log.info("广播消息持久化完成 - 消息ID: {}, 范围: {}, MongoDB ID: {}", 
                    document.getMessageId(), document.getScope(), savedId);
                    
        } catch (Exception e) {
            log.error("广播消息持久化失败 - 消息ID: {}, 错误: {}", 
                    stompMessage.getMessageId(), e.getMessage(), e);
        }
    }

    /**
     * 异步标记消息已读
     * 
     * @param messageId 消息ID
     * @param userId 用户ID
     * @param orgId 组织ID
     */
    @Async("messageExecutor")
    public void markMessageAsReadAsync(String messageId, Long userId, Long orgId) {
        try {
            log.debug("开始标记广播消息已读 - 消息ID: {}, 用户ID: {}", messageId, userId);
            
            BroadcastMessageReadStatus readStatus = BroadcastMessageReadStatus.builder()
                .id(buildReadStatusId(messageId, userId))
                .messageId(messageId)
                .userId(userId)
                .orgId(orgId)
                .readAt(LocalDateTime.now())
                .build();
                
            String savedId = readStatusRepository.save(readStatus);
            
            log.info("广播消息标记已读完成 - 消息ID: {}, 用户ID: {}, MongoDB ID: {}", 
                    messageId, userId, savedId);
            
        } catch (Exception e) {
            log.error("广播消息标记已读失败 - 消息ID: {}, 用户ID: {}, 错误: {}", 
                    messageId, userId, e.getMessage(), e);
        }
    }

    /**
     * 批量异步标记消息已读
     * 
     * @param messageIds 消息ID列表
     * @param userId 用户ID
     * @param orgId 组织ID
     */
    @Async("messageExecutor")
    public void batchMarkMessagesAsReadAsync(List<String> messageIds, Long userId, Long orgId) {
        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }

        try {
            log.debug("开始批量标记广播消息已读 - 用户ID: {}, 消息数量: {}", userId, messageIds.size());
            
            List<BroadcastMessageReadStatus> statusList = messageIds.stream()
                    .map(messageId -> BroadcastMessageReadStatus.builder()
                        .id(buildReadStatusId(messageId, userId))
                        .messageId(messageId)
                        .userId(userId)
                        .orgId(orgId)
                        .readAt(LocalDateTime.now())
                        .build())
                    .collect(Collectors.toList());
            
            // 批量保存已读状态
            int savedCount = readStatusRepository.batchSave(statusList);
            
            log.info("批量标记广播消息已读完成 - 用户ID: {}, 成功数量: {}/{}", 
                    userId, savedCount, messageIds.size());
            
        } catch (Exception e) {
            log.error("批量标记广播消息已读失败 - 用户ID: {}, 消息数量: {}, 错误: {}", 
                    userId, messageIds.size(), e.getMessage(), e);
        }
    }

    /**
     * 构建广播消息文档对象
     */
    private BroadcastMessageDocument buildBroadcastMessageDocument(CommonStompMessage stompMessage) {
        String messagePayload = JsonUtils.toJson(stompMessage.getPayload());
        Map<String, Object> payload = JsonUtils.fromJson(messagePayload, Map.class);
        return BroadcastMessageDocument.builder()
                .messageId(stompMessage.getMessageId())
                .timestamp(stompMessage.getTimestamp())
                .oid(stompMessage.getOid())
                .messageType(stompMessage.getMessageType().name())
                .subType_1(stompMessage.getSubType_1())
                .subType_2(stompMessage.getSubType_2())
                .level(stompMessage.getLevel() != null ? stompMessage.getLevel().name() : null)
                .scope(extractScope(payload))
                .targetOid(extractTargetOid(payload))
                .title(stompMessage.getTitle())
                .content(stompMessage.getContent())
                .payload(stompMessage.getPayload())
                .expiredAt(extractExpiredAt(payload))
                .publisherId(extractPublisherId(stompMessage))
                .build();
    }

    /**
     * 构建已读状态复合ID
     */
    private String buildReadStatusId(String messageId, Long userId) {
        return messageId + "_" + userId;
    }

    /**
     * 从STOMP消息中提取消息范围
     */
    private String extractScope(Map<String, Object> payload ) {
        if (payload.containsKey("scope")) {
            return (String) payload.get("scope");
        }
        return "ORG"; // 默认为组织级
    }

    /**
     * 从STOMP消息中提取目标组织列表
     */
    @SuppressWarnings("unchecked")
    private List<Long> extractTargetOid(Map<String, Object> payload ) {
        if (payload.containsKey("targetOid")) {
            return (List<Long>) payload.get("targetOid");
        }
        return null;
    }

    /**
     * 从STOMP消息中提取过期时间
     */
    private LocalDateTime extractExpiredAt(Map<String, Object> payload ) {
        if (payload.containsKey("expiredAt")) {
            // 根据实际格式转换，这里简化处理
            return null;
        }
        return null;
    }

    /**
     * 从STOMP消息中提取发布者ID
     */
    private Long extractPublisherId(CommonStompMessage message) {
        if (message.getContext() != null && message.getContext().getUid() != null) {
            return message.getContext().getUid();
        }
        return null; // 系统发布消息时为null
    }

    /**
     * 发布广播消息事件
     */
    private void publishBroadcastMessageEvent(BroadcastMessageDocument document) {
        try {
            // TODO: 实现具体的事件发布逻辑
            log.debug("发布广播消息事件 - 消息ID: {}, 范围: {}", document.getMessageId(), document.getScope());
        } catch (Exception e) {
            log.warn("发布广播消息事件失败 - 消息ID: {}, 错误: {}", document.getMessageId(), e.getMessage());
        }
    }
}