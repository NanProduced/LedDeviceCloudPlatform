package org.nan.cloud.message.infrastructure.websocket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.infrastructure.mongodb.repository.RealtimeMessageRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 实时消息确认服务
 * 
 * 处理消息确认（标记已读）相关的业务逻辑
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeMessageAckService {
    
    private final RealtimeMessageRepository realtimeMessageRepository;
    
    /**
     * 标记消息为已读
     * 
     * @param messageId 消息ID
     * @param userId 用户ID
     * @return 是否成功
     */
    public boolean markMessageAsRead(String messageId, Long userId) {
        try {
            log.debug("标记消息为已读 - 消息ID: {}, 用户ID: {}", messageId, userId);
            
            String readAt = Instant.now().toString();
            boolean success = realtimeMessageRepository.updateReadStatus(messageId, userId, readAt);
            
            if (success) {
                log.info("标记消息为已读成功 - 消息ID: {}, 用户ID: {}", messageId, userId);
                
                // TODO: 发布消息已读事件，用于清除缓存
                // applicationEventPublisher.publishEvent(new MessageReadEvent(messageId, userId));
            } else {
                log.warn("标记消息为已读失败 - 消息ID: {}, 用户ID: {} (可能消息不存在或无权限)", messageId, userId);
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("标记消息为已读异常 - 消息ID: {}, 用户ID: {}, 错误: {}", messageId, userId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 批量标记消息为已读
     * 
     * @param messageIds 消息ID列表
     * @param userId 用户ID
     * @return 批量操作结果
     */
    public RealtimeMessageStompController.BatchAckResult batchMarkMessagesAsRead(List<String> messageIds, Long userId) {
        log.info("批量标记消息为已读 - 用户ID: {}, 消息数量: {}", userId, messageIds.size());
        
        int totalCount = messageIds.size();
        int successCount = 0;
        int failureCount = 0;
        Map<String, String> failureReasons = new HashMap<>();
        
        String readAt = Instant.now().toString();
        
        for (String messageId : messageIds) {
            try {
                boolean success = realtimeMessageRepository.updateReadStatus(messageId, userId, readAt);
                
                if (success) {
                    successCount++;
                    log.debug("批量标记消息已读成功 - 消息ID: {}, 用户ID: {}", messageId, userId);
                } else {
                    failureCount++;
                    failureReasons.put(messageId, "消息不存在或无权限访问");
                    log.debug("批量标记消息已读失败 - 消息ID: {}, 用户ID: {} (消息不存在或无权限)", messageId, userId);
                }
                
            } catch (Exception e) {
                failureCount++;
                failureReasons.put(messageId, "操作异常: " + e.getMessage());
                log.warn("批量标记消息已读异常 - 消息ID: {}, 用户ID: {}, 错误: {}", messageId, userId, e.getMessage());
            }
        }
        
        log.info("批量标记消息为已读完成 - 用户ID: {}, 总数: {}, 成功: {}, 失败: {}", 
                userId, totalCount, successCount, failureCount);
        
        // TODO: 发布批量消息已读事件，用于清除缓存
        // if (successCount > 0) {
        //     applicationEventPublisher.publishEvent(new BatchMessageReadEvent(userId, successCount));
        // }
        
        return new RealtimeMessageStompController.BatchAckResult(totalCount, successCount, failureCount, failureReasons);
    }
}