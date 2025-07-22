package org.nan.cloud.message.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.api.dto.websocket.WebSocketMessage;
import org.nan.cloud.message.api.dto.response.*;
import org.nan.cloud.message.service.MessageService;
import org.nan.cloud.common.web.DynamicResponse;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息中心REST API控制器
 * 
 * 提供HTTP REST接口用于消息发送、广播、状态查询等操作。
 * 这些接口主要供其他微服务调用，也可以用于管理后台的消息管理功能。
 * 
 * 主要功能：
 * 1. 单用户消息发送
 * 2. 组织广播
 * 3. 系统通知
 * 4. 在线状态查询
 * 5. 统计信息获取
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
@Tag(name = "消息中心API", description = "消息发送、广播、状态查询相关接口")
public class MessageController {
    
    /**
     * 消息服务
     * 处理消息相关的业务逻辑
     */
    private final MessageService messageService;
    
    /**
     * 发送消息给指定用户
     * 
     * 通过HTTP接口发送消息给特定用户，消息会通过WebSocket实时推送。
     * 如果用户不在线，消息会被持久化等待用户上线后推送。
     * 
     * @param userId 目标用户ID
     * @param message 消息内容
     * @return 发送结果
     */
    @PostMapping("/send/{userId}")
    @Operation(summary = "发送消息给指定用户", description = "向特定用户发送实时消息")
    public SendMessageResponse sendMessageToUser(
            @Parameter(description = "目标用户ID", required = true)
            @PathVariable("userId") String userId,
            
            @Parameter(description = "消息内容", required = true)
            @RequestBody WebSocketMessage message) {
        
        log.info("收到发送消息请求 - 目标用户: {}, 消息类型: {}", userId, message.getType());
        
        // 调用消息服务发送消息，异常由GlobalExceptionHandler处理
        boolean success = messageService.sendMessageToUser(userId, message);
        
        SendMessageResponse response = new SendMessageResponse();
        response.setSuccess(success);
        response.setMessageId(message.getMessageId());
        response.setUserId(userId);
        response.setMessage(success ? "消息发送成功" : "消息发送失败，用户可能不在线");
        
        if (success) {
            log.info("消息发送成功 - 用户: {}, 消息ID: {}", userId, message.getMessageId());
        } else {
            log.warn("消息发送失败 - 用户: {}, 消息ID: {}", userId, message.getMessageId());
        }
        
        return response;
    }
    
    /**
     * 向组织广播消息
     * 
     * 向指定组织内的所有在线用户广播消息。
     * 
     * @param organizationId 目标组织ID
     * @param message 消息内容
     * @return 广播结果
     */
    @PostMapping("/broadcast/organization/{organizationId}")
    @Operation(summary = "组织内广播消息", description = "向指定组织内所有用户广播消息")
    public BroadcastResponse broadcastToOrganization(
            @Parameter(description = "目标组织ID", required = true)
            @PathVariable("organizationId") String organizationId,
            
            @Parameter(description = "消息内容", required = true)
            @RequestBody WebSocketMessage message) {
        
        log.info("收到组织广播请求 - 目标组织: {}, 消息类型: {}", organizationId, message.getType());
        
        // 调用消息服务进行组织广播
        int successCount = messageService.broadcastToOrganization(organizationId, message);
        
        BroadcastResponse response = new BroadcastResponse();
        response.setSuccess(successCount > 0);
        response.setMessageId(message.getMessageId());
        response.setOrganizationId(organizationId);
        response.setSuccessCount(successCount);
        response.setMessage(String.format("广播完成，成功推送给 %d 个用户", successCount));
        
        log.info("组织广播完成 - 组织: {}, 消息ID: {}, 成功数量: {}", 
                organizationId, message.getMessageId(), successCount);
        
        return response;
    }
    
    /**
     * 全平台广播消息
     * 
     * 向所有在线用户广播系统级重要消息。
     * 
     * @param message 消息内容
     * @return 广播结果
     */
    @PostMapping("/broadcast/all")
    @Operation(summary = "全平台广播消息", description = "向所有在线用户广播系统级消息")
    public BroadcastResponse broadcastToAll(
            @Parameter(description = "消息内容", required = true)
            @RequestBody WebSocketMessage message) {
        
        log.info("收到全平台广播请求 - 消息类型: {}", message.getType());
        
        // 调用消息服务进行全平台广播
        int successCount = messageService.broadcastToAll(message);
        
        BroadcastResponse response = new BroadcastResponse();
        response.setSuccess(successCount > 0);
        response.setMessageId(message.getMessageId());
        response.setSuccessCount(successCount);
        response.setMessage(String.format("全平台广播完成，成功推送给 %d 个用户", successCount));
        
        log.info("全平台广播完成 - 消息ID: {}, 成功数量: {}", message.getMessageId(), successCount);
        
        return response;
    }
    
    /**
     * 发送系统通知
     * 
     * 便捷接口，用于发送系统级通知消息。
     * 
     * @param request 通知请求参数
     * @return 发送结果
     */
    @PostMapping("/notification/system")
    @Operation(summary = "发送系统通知", description = "发送系统级通知消息")
    public SystemNotificationResponse sendSystemNotification(
            @Parameter(description = "系统通知请求", required = true)
            @RequestBody SystemNotificationRequest request) {
        
        log.info("收到系统通知请求 - 用户: {}, 组织: {}, 标题: {}", 
                request.getUserId(), request.getOrganizationId(), request.getTitle());
        
        // 调用消息服务发送系统通知，异常由GlobalExceptionHandler处理
        boolean success = messageService.sendSystemNotification(
                request.getUserId(), 
                request.getOrganizationId(), 
                request.getTitle(), 
                request.getContent()
        );
        
        SystemNotificationResponse response = new SystemNotificationResponse();
        response.setSuccess(success);
        response.setMessage(success ? "系统通知发送成功" : "系统通知发送失败");
        
        return response;
    }
    
    /**
     * 批量发送消息
     * 
     * 向多个用户批量发送相同的消息。
     * 
     * @param request 批量发送请求
     * @return 发送结果
     */
    @PostMapping("/batch-send")
    @Operation(summary = "批量发送消息", description = "向多个用户批量发送相同消息")
    public BroadcastResponse batchSendMessage(
            @Parameter(description = "批量发送请求", required = true)
            @RequestBody BatchSendRequest request) {
        
        log.info("收到批量发送请求 - 用户数量: {}, 消息类型: {}", 
                request.getUserIds().size(), request.getMessage().getType());
        
        // 调用消息服务批量发送消息，异常由GlobalExceptionHandler处理
        int successCount = messageService.batchSendMessage(request.getUserIds(), request.getMessage());
        
        BroadcastResponse response = new BroadcastResponse();
        response.setSuccess(successCount > 0);
        response.setSuccessCount(successCount);
        response.setMessage(String.format("批量发送完成，成功 %d 个，失败 %d 个", 
                successCount, request.getUserIds().size() - successCount));
        
        return response;
    }
    
    /**
     * 查询用户在线状态
     * 
     * @param userId 用户ID
     * @return 在线状态信息
     */
    @GetMapping("/status/user/{userId}")
    @Operation(summary = "查询用户在线状态", description = "查询指定用户的在线状态和连接信息")
    public UserStatusResponse getUserStatus(
            @Parameter(description = "用户ID", required = true)
            @PathVariable("userId") String userId) {
        
        boolean online = messageService.isUserOnline(userId);
        int connectionCount = messageService.getUserConnectionCount(userId);
        
        UserStatusResponse response = new UserStatusResponse();
        response.setUserId(userId);
        response.setOnline(online);
        response.setConnectionCount(connectionCount);
        response.setMessage(online ? "用户在线" : "用户离线");
        
        return response;
    }
    
    /**
     * 获取系统统计信息
     * 
     * @return 系统统计数据
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取系统统计信息", description = "获取消息中心的统计信息")
    public StatisticsResponse getStatistics() {
        // 获取统计信息，异常由GlobalExceptionHandler处理
        int totalOnlineUsers = messageService.getTotalOnlineUserCount();
        
        StatisticsResponse response = new StatisticsResponse();
        response.setTotalOnlineUsers(totalOnlineUsers);
        response.setTimestamp(System.currentTimeMillis());
        response.setMessage("统计信息获取成功");
        
        return response;
    }
    
    /**
     * 系统通知请求参数
     */
    @Setter
    @Getter
    public static class SystemNotificationRequest {
        // Getters and Setters
        private String userId;          // 可选，指定用户ID
        private String organizationId;  // 必填，组织ID
        private String title;           // 必填，通知标题
        private String content;         // 必填，通知内容

    }
    
    /**
     * 批量发送请求参数
     */
    @Setter
    @Getter
    public static class BatchSendRequest {
        // Getters and Setters
        private List<String> userIds;    // 必填，目标用户ID列表
        private WebSocketMessage message; // 必填，消息内容

    }
}