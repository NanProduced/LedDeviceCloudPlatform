package org.nan.cloud.message.infrastructure.websocket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.infrastructure.service.BroadcastMessagePersistenceService;
import org.nan.cloud.message.infrastructure.websocket.security.GatewayUserInfo;
import org.nan.cloud.message.infrastructure.websocket.interceptor.StompPrincipal;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 广播消息STOMP控制器
 * 
 * 处理广播消息的已读确认STOMP请求
 * 符合WebSocket消息格式前端处理规范
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class BroadcastMessageStompController {

    private final BroadcastMessagePersistenceService persistenceService;

    /**
     * 处理广播消息已读确认
     * 
     * 前端发送路径: /app/broadcast/mark-read
     * 前端接收路径: /user/queue/broadcast-ack
     */
    @MessageMapping("/broadcast/mark-read")
    @SendToUser("/queue/broadcast-ack")
    public BroadcastMessageAckResult markMessageAsRead(
            @Payload BroadcastMarkReadRequest request,
            StompHeaderAccessor accessor) {
        
        try {
            // 提取用户信息
            GatewayUserInfo userInfo = extractUserInfo(accessor.getUser());
            if (userInfo == null) {
                log.warn("无法提取用户信息进行广播消息确认 - 消息ID: {}", request.getMessageId());
                return BroadcastMessageAckResult.failure(request.getMessageId(), "用户信息无效");
            }
            
            Long userId = userInfo.getUid();
            Long orgId = userInfo.getOid();
            
            log.info("收到广播消息已读确认 - 用户: {}, 消息ID: {}", userId, request.getMessageId());
            
            // 异步标记已读
            persistenceService.markMessageAsReadAsync(request.getMessageId(), userId, orgId);
            
            return BroadcastMessageAckResult.success(request.getMessageId());
            
        } catch (Exception e) {
            log.error("处理广播消息已读确认失败 - 消息ID: {}, 错误: {}", 
                    request.getMessageId(), e.getMessage(), e);
            return BroadcastMessageAckResult.failure(request.getMessageId(), e.getMessage());
        }
    }

    /**
     * 批量处理广播消息已读确认
     * 
     * 前端发送路径: /app/broadcast/batch-mark-read
     * 前端接收路径: /user/queue/broadcast-ack
     */
    @MessageMapping("/broadcast/batch-mark-read")
    @SendToUser("/queue/broadcast-ack")
    public BatchBroadcastMessageAckResult batchMarkAsRead(
            @Payload BatchBroadcastMarkReadRequest request,
            StompHeaderAccessor accessor) {
        
        try {
            // 提取用户信息
            GatewayUserInfo userInfo = extractUserInfo(accessor.getUser());
            if (userInfo == null) {
                log.warn("无法提取用户信息进行批量广播消息确认 - 消息数量: {}", 
                        request.getMessageIds().size());
                return BatchBroadcastMessageAckResult.failure("用户信息无效");
            }
            
            Long userId = userInfo.getUid();
            Long orgId = userInfo.getOid();
            
            log.info("收到批量广播消息已读确认 - 用户: {}, 消息数量: {}", 
                    userId, request.getMessageIds().size());
            
            // 异步批量标记已读
            persistenceService.batchMarkMessagesAsReadAsync(request.getMessageIds(), userId, orgId);
            
            // 构造批量确认结果
            List<BroadcastMessageAckResult> results = request.getMessageIds().stream()
                    .map(BroadcastMessageAckResult::success)
                    .collect(Collectors.toList());
            
            return BatchBroadcastMessageAckResult.of(results);
            
        } catch (Exception e) {
            log.error("批量处理广播消息已读确认失败 - 消息数量: {}, 错误: {}", 
                    request.getMessageIds().size(), e.getMessage(), e);
            return BatchBroadcastMessageAckResult.failure(e.getMessage());
        }
    }

    /**
     * 从Principal中提取用户信息
     */
    private GatewayUserInfo extractUserInfo(Principal principal) {
        if (principal instanceof StompPrincipal stompPrincipal) {
            return stompPrincipal.getUserInfo();
        }
        return null;
    }

    /**
     * 广播消息已读确认请求DTO
     */
    public static class BroadcastMarkReadRequest {
        private String messageId;
        private Long orgId;

        public String getMessageId() {
            return messageId;
        }

        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }

        public Long getOrgId() {
            return orgId;
        }

        public void setOrgId(Long orgId) {
            this.orgId = orgId;
        }
    }

    /**
     * 批量广播消息已读确认请求DTO
     */
    public static class BatchBroadcastMarkReadRequest {
        private List<String> messageIds;
        private Long orgId;

        public List<String> getMessageIds() {
            return messageIds;
        }

        public void setMessageIds(List<String> messageIds) {
            this.messageIds = messageIds;
        }

        public Long getOrgId() {
            return orgId;
        }

        public void setOrgId(Long orgId) {
            this.orgId = orgId;
        }
    }

    /**
     * 广播消息确认结果DTO
     */
    public static class BroadcastMessageAckResult {
        private String messageId;
        private boolean success;
        private String errorMessage;
        private LocalDateTime timestamp;

        public static BroadcastMessageAckResult success(String messageId) {
            BroadcastMessageAckResult result = new BroadcastMessageAckResult();
            result.messageId = messageId;
            result.success = true;
            result.timestamp = LocalDateTime.now();
            return result;
        }

        public static BroadcastMessageAckResult failure(String messageId, String error) {
            BroadcastMessageAckResult result = new BroadcastMessageAckResult();
            result.messageId = messageId;
            result.success = false;
            result.errorMessage = error;
            result.timestamp = LocalDateTime.now();
            return result;
        }

        // Getters
        public String getMessageId() {
            return messageId;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }

    /**
     * 批量广播消息确认结果DTO
     */
    public static class BatchBroadcastMessageAckResult {
        private List<BroadcastMessageAckResult> results;
        private int totalCount;
        private int successCount;
        private boolean allSuccess;
        private String errorMessage;
        private LocalDateTime timestamp;

        public static BatchBroadcastMessageAckResult of(List<BroadcastMessageAckResult> results) {
            BatchBroadcastMessageAckResult batchResult = new BatchBroadcastMessageAckResult();
            batchResult.results = results;
            batchResult.totalCount = results.size();
            batchResult.successCount = (int) results.stream().mapToInt(r -> r.success ? 1 : 0).sum();
            batchResult.allSuccess = batchResult.successCount == batchResult.totalCount;
            batchResult.timestamp = LocalDateTime.now();
            return batchResult;
        }

        public static BatchBroadcastMessageAckResult failure(String error) {
            BatchBroadcastMessageAckResult result = new BatchBroadcastMessageAckResult();
            result.allSuccess = false;
            result.errorMessage = error;
            result.timestamp = LocalDateTime.now();
            return result;
        }

        // Getters
        public List<BroadcastMessageAckResult> getResults() {
            return results;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public boolean isAllSuccess() {
            return allSuccess;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }
}