package org.nan.cloud.message.infrastructure.websocket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.infrastructure.websocket.security.GatewayUserInfo;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 实时消息STOMP控制器
 * 
 * 处理前端通过WebSocket发送的消息确认、标记已读等操作
 * 使用 /app 前缀的STOMP消息映射
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class RealtimeMessageStompController {
    
    private final RealtimeMessageAckService realtimeMessageAckService;
    
    /**
     * 处理单个消息确认（标记已读）
     * 
     * 前端发送格式：
     * {
     *   "messageId": "消息ID",
     *   "action": "READ"
     * }
     * 
     * @param payload 确认消息载荷
     * @param principal 用户认证信息
     * @return 确认结果
     */
    @MessageMapping("/message/ack")
    @SendToUser("/queue/ack-result")
    public MessageAckResponse handleMessageAck(@Payload MessageAckRequest payload, Principal principal) {
        try {
            log.debug("处理消息确认 - 消息ID: {}, 用户: {}", payload.getMessageId(), principal.getName());
            
            // 获取用户信息
            GatewayUserInfo userInfo = (GatewayUserInfo) principal;
            Long userId = userInfo.getUid();
            
            // 验证参数
            if (payload.getMessageId() == null || payload.getMessageId().trim().isEmpty()) {
                return MessageAckResponse.failure(null, "消息ID不能为空");
            }
            
            // 执行消息确认操作
            boolean success = false;
            String errorMessage = null;
            
            try {
                if ("READ".equals(payload.getAction())) {
                    success = realtimeMessageAckService.markMessageAsRead(payload.getMessageId(), userId);
                } else {
                    errorMessage = "不支持的操作类型: " + payload.getAction();
                }
            } catch (Exception e) {
                log.error("执行消息确认操作失败 - 消息ID: {}, 用户ID: {}, 错误: {}", 
                        payload.getMessageId(), userId, e.getMessage(), e);
                errorMessage = "操作执行失败: " + e.getMessage();
            }
            
            if (success) {
                log.info("消息确认成功 - 消息ID: {}, 用户ID: {}, 操作: {}", 
                        payload.getMessageId(), userId, payload.getAction());
                return MessageAckResponse.success(payload.getMessageId(), "消息已标记为已读");
            } else {
                log.warn("消息确认失败 - 消息ID: {}, 用户ID: {}, 错误: {}", 
                        payload.getMessageId(), userId, errorMessage);
                return MessageAckResponse.failure(payload.getMessageId(), errorMessage != null ? errorMessage : "操作失败");
            }
            
        } catch (Exception e) {
            log.error("处理消息确认异常 - 消息ID: {}, 错误: {}", 
                    payload.getMessageId(), e.getMessage(), e);
            return MessageAckResponse.failure(payload.getMessageId(), "系统异常: " + e.getMessage());
        }
    }
    
    /**
     * 处理批量消息确认（批量标记已读）
     * 
     * 前端发送格式：
     * {
     *   "messageIds": ["消息ID1", "消息ID2", ...],
     *   "action": "READ_BATCH"
     * }
     * 
     * @param payload 批量确认消息载荷
     * @param principal 用户认证信息
     * @return 批量确认结果
     */
    @MessageMapping("/message/batch-ack")
    @SendToUser("/queue/ack-result")
    public BatchMessageAckResponse handleBatchMessageAck(@Payload BatchMessageAckRequest payload, Principal principal) {
        try {
            log.debug("处理批量消息确认 - 消息数量: {}, 用户: {}", 
                    payload.getMessageIds() != null ? payload.getMessageIds().size() : 0, principal.getName());
            
            // 获取用户信息
            GatewayUserInfo userInfo = (GatewayUserInfo) principal;
            Long userId = userInfo.getUid();
            
            // 验证参数
            if (payload.getMessageIds() == null || payload.getMessageIds().isEmpty()) {
                return BatchMessageAckResponse.failure("消息ID列表不能为空");
            }
            
            if (payload.getMessageIds().size() > 100) {
                return BatchMessageAckResponse.failure("批量操作数量不能超过100个");
            }
            
            // 执行批量消息确认操作
            try {
                if ("READ_BATCH".equals(payload.getAction())) {
                    BatchAckResult result = realtimeMessageAckService.batchMarkMessagesAsRead(payload.getMessageIds(), userId);
                    
                    log.info("批量消息确认完成 - 用户ID: {}, 总数: {}, 成功: {}, 失败: {}", 
                            userId, result.getTotalCount(), result.getSuccessCount(), result.getFailureCount());
                    
                    return BatchMessageAckResponse.success(result);
                } else {
                    return BatchMessageAckResponse.failure("不支持的操作类型: " + payload.getAction());
                }
            } catch (Exception e) {
                log.error("执行批量消息确认操作失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
                return BatchMessageAckResponse.failure("批量操作执行失败: " + e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("处理批量消息确认异常 - 错误: {}", e.getMessage(), e);
            return BatchMessageAckResponse.failure("系统异常: " + e.getMessage());
        }
    }
    
    // ==================== 请求和响应模型类 ====================
    
    /**
     * 消息确认请求
     */
    public static class MessageAckRequest {
        private String messageId;
        private String action = "READ";
        
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
    }
    
    /**
     * 批量消息确认请求
     */
    public static class BatchMessageAckRequest {
        private List<String> messageIds;
        private String action = "READ_BATCH";
        
        public List<String> getMessageIds() { return messageIds; }
        public void setMessageIds(List<String> messageIds) { this.messageIds = messageIds; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
    }
    
    /**
     * 消息确认响应
     */
    public static class MessageAckResponse {
        private boolean success;
        private String messageId;
        private String message;
        private String timestamp;
        
        private MessageAckResponse(boolean success, String messageId, String message) {
            this.success = success;
            this.messageId = messageId;
            this.message = message;
            this.timestamp = Instant.now().toString();
        }
        
        public static MessageAckResponse success(String messageId, String message) {
            return new MessageAckResponse(true, messageId, message);
        }
        
        public static MessageAckResponse failure(String messageId, String message) {
            return new MessageAckResponse(false, messageId, message);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessageId() { return messageId; }
        public String getMessage() { return message; }
        public String getTimestamp() { return timestamp; }
    }
    
    /**
     * 批量消息确认响应
     */
    public static class BatchMessageAckResponse {
        private boolean success;
        private String message;
        private BatchAckResult result;
        private String timestamp;
        
        private BatchMessageAckResponse(boolean success, String message, BatchAckResult result) {
            this.success = success;
            this.message = message;
            this.result = result;
            this.timestamp = Instant.now().toString();
        }
        
        public static BatchMessageAckResponse success(BatchAckResult result) {
            return new BatchMessageAckResponse(true, "批量操作完成", result);
        }
        
        public static BatchMessageAckResponse failure(String message) {
            return new BatchMessageAckResponse(false, message, null);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public BatchAckResult getResult() { return result; }
        public String getTimestamp() { return timestamp; }
    }
    
    /**
     * 批量确认结果
     */
    public static class BatchAckResult {
        private int totalCount;
        private int successCount;
        private int failureCount;
        private Map<String, String> failureReasons;
        
        public BatchAckResult(int totalCount, int successCount, int failureCount, Map<String, String> failureReasons) {
            this.totalCount = totalCount;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.failureReasons = failureReasons;
        }
        
        // Getters
        public int getTotalCount() { return totalCount; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public Map<String, String> getFailureReasons() { return failureReasons; }
    }
}