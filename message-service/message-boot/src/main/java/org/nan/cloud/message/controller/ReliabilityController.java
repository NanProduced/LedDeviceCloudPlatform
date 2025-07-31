package org.nan.cloud.message.controller;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.web.DynamicResponse;
import org.nan.cloud.message.api.enums.Priority;
import org.nan.cloud.message.infrastructure.websocket.reliability.*;
import static org.nan.cloud.message.infrastructure.websocket.reliability.MessageHelper.*;
import org.nan.cloud.message.infrastructure.websocket.stomp.enums.StompMessageTypes;
import org.nan.cloud.message.infrastructure.websocket.stomp.model.CommonStompMessage;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * STOMP可靠性管理API控制器
 * 
 * 提供Phase 3.1功能的REST API接口：
 * 1. 消息投递跟踪和统计
 * 2. 重试策略配置管理
 * 3. 客户端连接状态监控
 * 4. 可靠性消息发送接口
 * 
 * @author Nan
 * @since 3.1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/message/reliability")
@RequiredArgsConstructor
public class ReliabilityController {
    
    private final MessageDeliveryTracker deliveryTracker;
    private final RetryConfiguration retryConfiguration;
    private final StompAckHandler ackHandler;
    private final ReliableMessageSender reliableMessageSender;
    
    // ==================== 消息投递跟踪API ====================
    
    /**
     * 获取消息投递统计信息
     * 
     * @return 投递统计
     */
    @GetMapping("/delivery/stats")
    public DynamicResponse<MessageDeliveryTracker.DeliveryStatistics> getDeliveryStats() {
        try {
            log.debug("获取消息投递统计信息");
            
            MessageDeliveryTracker.DeliveryStatistics stats = deliveryTracker.getStatistics();
            return DynamicResponse.success(stats);
            
        } catch (Exception e) {
            log.error("获取投递统计失败 - 错误: {}", e.getMessage(), e);
            return DynamicResponse.fail("获取投递统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取待确认消息数量
     * 
     * @return 待确认消息数量
     */
    @GetMapping("/delivery/pending-count")
    public DynamicResponse<Integer> getPendingMessageCount() {
        try {
            int count = deliveryTracker.getPendingMessageCount();
            return DynamicResponse.success(count);
            
        } catch (Exception e) {
            log.error("获取待确认消息数量失败 - 错误: {}", e.getMessage(), e);
            return DynamicResponse.fail("获取待确认消息数量失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取指定消息的投递记录
     * 
     * @param messageId 消息ID
     * @return 投递记录
     */
    @GetMapping("/delivery/record/{messageId}")
    public DynamicResponse<MessageDeliveryTracker.DeliveryRecord> getDeliveryRecord(@PathVariable String messageId) {
        try {
            log.debug("获取消息投递记录 - messageId: {}", messageId);
            
            Optional<MessageDeliveryTracker.DeliveryRecord> record = deliveryTracker.getDeliveryRecord(messageId);
            
            if (record.isPresent()) {
                return DynamicResponse.success(record.get());
            } else {
                return DynamicResponse.fail("投递记录不存在");
            }
            
        } catch (Exception e) {
            log.error("获取投递记录失败 - messageId: {}, 错误: {}", messageId, e.getMessage(), e);
            return DynamicResponse.fail("获取投递记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 手动确认消息
     * 
     * @param messageId 消息ID
     * @param request 确认请求
     * @return 操作结果
     */
    @PostMapping("/delivery/acknowledge/{messageId}")
    public DynamicResponse<String> acknowledgeMessage(@PathVariable String messageId,
                                               @RequestBody AckRequest request) {
        try {
            log.info("手动确认消息 - messageId: {}, userId: {}", messageId, request.getUserId());
            
            boolean success = deliveryTracker.acknowledgeMessage(messageId, request.getUserId());
            
            if (success) {
                return DynamicResponse.success("消息确认成功");
            } else {
                return DynamicResponse.fail("消息确认失败 - 消息不存在或权限不足");
            }
            
        } catch (Exception e) {
            log.error("手动确认消息失败 - messageId: {}, 错误: {}", messageId, e.getMessage(), e);
            return DynamicResponse.fail("消息确认失败: " + e.getMessage());
        }
    }
    
    // ==================== 重试策略配置API ====================
    
    /**
     * 获取所有重试策略
     * 
     * @return 重试策略映射
     */
    @GetMapping("/retry/policies")
    public DynamicResponse<Map<String, RetryConfiguration.RetryPolicy>> getAllRetryPolicies() {
        try {
            log.debug("获取所有重试策略");
            
            Map<String, RetryConfiguration.RetryPolicy> policies = retryConfiguration.getAllRetryPolicies();
            return DynamicResponse.success(policies);
            
        } catch (Exception e) {
            log.error("获取重试策略失败 - 错误: {}", e.getMessage(), e);
            return DynamicResponse.fail("获取重试策略失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取指定消息类型的重试策略
     * 
     * @param messageType 消息类型
     * @return 重试策略
     */
    @GetMapping("/retry/policy/{messageType}")
    public DynamicResponse<RetryConfiguration.RetryPolicy> getRetryPolicy(@PathVariable String messageType) {
        try {
            log.debug("获取重试策略 - messageType: {}", messageType);
            
            RetryConfiguration.RetryPolicy policy = retryConfiguration.getRetryPolicy(messageType);
            return DynamicResponse.success(policy);
            
        } catch (Exception e) {
            log.error("获取重试策略失败 - messageType: {}, 错误: {}", messageType, e.getMessage(), e);
            return DynamicResponse.fail("获取重试策略失败: " + e.getMessage());
        }
    }
    
    /**
     * 设置自定义重试策略
     * 
     * @param messageType 消息类型
     * @param policy 重试策略
     * @return 操作结果
     */
    @PutMapping("/retry/policy/{messageType}")
    public DynamicResponse<String> setRetryPolicy(@PathVariable String messageType,
                                           @RequestBody RetryConfiguration.RetryPolicy policy) {
        try {
            log.info("设置重试策略 - messageType: {}", messageType);
            
            retryConfiguration.setRetryPolicy(messageType, policy);
            return DynamicResponse.success("重试策略设置成功");
            
        } catch (Exception e) {
            log.error("设置重试策略失败 - messageType: {}, 错误: {}", messageType, e.getMessage(), e);
            return DynamicResponse.fail("设置重试策略失败: " + e.getMessage());
        }
    }
    
    /**
     * 移除自定义重试策略
     * 
     * @param messageType 消息类型
     * @return 操作结果
     */
    @DeleteMapping("/retry/policy/{messageType}")
    public DynamicResponse<String> removeRetryPolicy(@PathVariable String messageType) {
        try {
            log.info("移除重试策略 - messageType: {}", messageType);
            
            retryConfiguration.removeRetryPolicy(messageType);
            return DynamicResponse.success("重试策略移除成功");
            
        } catch (Exception e) {
            log.error("移除重试策略失败 - messageType: {}, 错误: {}", messageType, e.getMessage(), e);
            return DynamicResponse.fail("移除重试策略失败: " + e.getMessage());
        }
    }
    
    // ==================== 客户端连接状态API ====================
    
    /**
     * 获取客户端连接状态
     * 
     * @param userId 用户ID
     * @return 连接状态
     */
    @GetMapping("/connection/status/{userId}")
    public DynamicResponse<StompAckHandler.ClientConnectionStatus> getConnectionStatus(@PathVariable String userId) {
        try {
            log.debug("获取客户端连接状态 - userId: {}", userId);
            
            StompAckHandler.ClientConnectionStatus status = ackHandler.getConnectionStatus(userId);
            
            if (status != null) {
                return DynamicResponse.success(status);
            } else {
                return DynamicResponse.fail("连接状态不存在");
            }
            
        } catch (Exception e) {
            log.error("获取连接状态失败 - userId: {}, 错误: {}", userId, e.getMessage(), e);
            return DynamicResponse.fail("获取连接状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取活跃连接数量
     * 
     * @return 活跃连接数量
     */
    @GetMapping("/connection/active-count")
    public DynamicResponse<Long> getActiveConnectionCount() {
        try {
            long count = ackHandler.getActiveConnectionCount();
            return DynamicResponse.success(count);
            
        } catch (Exception e) {
            log.error("获取活跃连接数量失败 - 错误: {}", e.getMessage(), e);
            return DynamicResponse.fail("获取活跃连接数量失败: " + e.getMessage());
        }
    }
    
    // ==================== 可靠性消息发送API ====================
    
    /**
     * 发送可靠性消息到指定用户
     * 
     * @param request 发送请求
     * @return 发送结果
     */
    @PostMapping("/send/user")
    public DynamicResponse<ReliableMessageSender.MessageSendResult> sendReliableUserMessage(
            @RequestBody ReliableMessageRequest request) {
        try {
            log.info("发送可靠性用户消息 - userId: {}, messageType: {}", 
                     request.getUserId(), request.getMessageType());
            
            CommonStompMessage message = createReliableMessage(
                null, // messageId 将由ReliableMessageSender生成
                StompMessageTypes.valueOf(request.getMessageType()),
                request.getContent(),
                Priority.valueOf(request.getPriority()),
                request.isRequiresAck()
            );
            
            ReliableMessageSender.MessageSendResult result = reliableMessageSender.sendReliableMessage(
                request.getUserId(), request.getDestination(), message, request.isRequiresAck());
            
            return DynamicResponse.success(result);
            
        } catch (Exception e) {
            log.error("发送可靠性用户消息失败 - 错误: {}", e.getMessage(), e);
            return DynamicResponse.fail("发送消息失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送可靠性主题消息
     * 
     * @param request 发送请求
     * @return 发送结果
     */
    @PostMapping("/send/topic")
    public DynamicResponse<ReliableMessageSender.MessageSendResult> sendReliableTopicMessage(
            @RequestBody ReliableTopicMessageRequest request) {
        try {
            log.info("发送可靠性主题消息 - topic: {}, messageType: {}", 
                     request.getTopic(), request.getMessageType());
            
            CommonStompMessage message = createReliableMessage(
                null, // messageId 将由ReliableMessageSender生成
                StompMessageTypes.valueOf(request.getMessageType()),
                request.getContent(),
                Priority.valueOf(request.getPriority()),
                request.isRequiresAck()
            );
            
            ReliableMessageSender.MessageSendResult result = reliableMessageSender.sendReliableTopicMessage(
                request.getTopic(), message, request.isRequiresAck());
            
            return DynamicResponse.success(result);
            
        } catch (Exception e) {
            log.error("发送可靠性主题消息失败 - 错误: {}", e.getMessage(), e);
            return DynamicResponse.fail("发送消息失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取待重试消息数量
     * 
     * @return 待重试消息数量
     */
    @GetMapping("/retry/pending-count")
    public DynamicResponse<Integer> getPendingRetryCount() {
        try {
            int count = reliableMessageSender.getPendingRetryCount();
            return DynamicResponse.success(count);
            
        } catch (Exception e) {
            log.error("获取待重试消息数量失败 - 错误: {}", e.getMessage(), e);
            return DynamicResponse.fail("获取待重试消息数量失败: " + e.getMessage());
        }
    }
    
    /**
     * 取消消息重试
     * 
     * @param messageId 消息ID
     * @return 操作结果
     */
    @PostMapping("/retry/cancel/{messageId}")
    public DynamicResponse<String> cancelRetry(@PathVariable String messageId) {
        try {
            log.info("取消消息重试 - messageId: {}", messageId);
            
            boolean success = reliableMessageSender.cancelRetry(messageId);
            
            if (success) {
                return DynamicResponse.success("重试已取消");
            } else {
                return DynamicResponse.fail("取消重试失败 - 重试任务不存在或已完成");
            }
            
        } catch (Exception e) {
            log.error("取消消息重试失败 - messageId: {}, 错误: {}", messageId, e.getMessage(), e);
            return DynamicResponse.fail("取消重试失败: " + e.getMessage());
        }
    }
    
    // ==================== 维护API ====================
    
    /**
     * 清理过期记录
     * 
     * @return 操作结果
     */
    @PostMapping("/maintenance/cleanup")
    public DynamicResponse<String> cleanupExpiredRecords() {
        try {
            log.info("执行可靠性数据清理");
            
            // 清理投递跟踪记录
            deliveryTracker.cleanupExpiredRecords();
            
            // 清理连接状态记录
            ackHandler.cleanupExpiredConnections(60); // 60分钟超时
            
            // 清理重试任务
            reliableMessageSender.cleanupCompletedRetryTasks();
            
            return DynamicResponse.success("数据清理完成");
            
        } catch (Exception e) {
            log.error("数据清理失败 - 错误: {}", e.getMessage(), e);
            return DynamicResponse.fail("数据清理失败: " + e.getMessage());
        }
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 确认请求
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AckRequest {
        private String userId;
        private String reason;
    }
    
    /**
     * 可靠性消息发送请求
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReliableMessageRequest {
        private String userId;
        private String destination;
        private String messageType;
        private Object content;
        private String priority;
        private boolean requiresAck;
    }
    
    /**
     * 可靠性主题消息发送请求
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReliableTopicMessageRequest {
        private String topic;
        private String messageType;
        private Object content;
        private String priority;
        private boolean requiresAck;
    }
}