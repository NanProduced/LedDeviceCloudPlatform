package org.nan.cloud.message.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.api.dto.websocket.WebSocketMessage;
import org.nan.cloud.message.service.MessageService;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Map<String, Object>> sendMessageToUser(
            @Parameter(description = "目标用户ID", required = true)
            @PathVariable String userId,
            
            @Parameter(description = "消息内容", required = true)
            @RequestBody WebSocketMessage message) {
        
        log.info("收到发送消息请求 - 目标用户: {}, 消息类型: {}", userId, message.getType());
        
        try {
            // 调用消息服务发送消息
            boolean success = messageService.sendMessageToUser(userId, message);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("messageId", message.getMessageId());
            response.put("userId", userId);
            
            if (success) {
                response.put("message", "消息发送成功");
                log.info("消息发送成功 - 用户: {}, 消息ID: {}", userId, message.getMessageId());
                return ResponseEntity.ok(response);
            } else {
                response.put("message", "消息发送失败，用户可能不在线");
                log.warn("消息发送失败 - 用户: {}, 消息ID: {}", userId, message.getMessageId());
                return ResponseEntity.ok(response); // 即使发送失败也返回200，业务逻辑由success字段判断
            }
            
        } catch (Exception e) {
            log.error("发送消息异常 - 用户: {}, 错误: {}", userId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "服务器内部错误: " + e.getMessage());
            errorResponse.put("userId", userId);
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
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
    public ResponseEntity<Map<String, Object>> broadcastToOrganization(
            @Parameter(description = "目标组织ID", required = true)
            @PathVariable String organizationId,
            
            @Parameter(description = "消息内容", required = true)
            @RequestBody WebSocketMessage message) {
        
        log.info("收到组织广播请求 - 目标组织: {}, 消息类型: {}", organizationId, message.getType());
        
        try {
            // 调用消息服务进行组织广播
            int successCount = messageService.broadcastToOrganization(organizationId, message);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", successCount > 0);
            response.put("messageId", message.getMessageId());
            response.put("organizationId", organizationId);
            response.put("successCount", successCount);
            response.put("message", String.format("广播完成，成功推送给 %d 个用户", successCount));
            
            log.info("组织广播完成 - 组织: {}, 消息ID: {}, 成功数量: {}", 
                    organizationId, message.getMessageId(), successCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("组织广播异常 - 组织: {}, 错误: {}", organizationId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "服务器内部错误: " + e.getMessage());
            errorResponse.put("organizationId", organizationId);
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
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
    public ResponseEntity<Map<String, Object>> broadcastToAll(
            @Parameter(description = "消息内容", required = true)
            @RequestBody WebSocketMessage message) {
        
        log.info("收到全平台广播请求 - 消息类型: {}", message.getType());
        
        try {
            // 调用消息服务进行全平台广播
            int successCount = messageService.broadcastToAll(message);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", successCount > 0);
            response.put("messageId", message.getMessageId());
            response.put("successCount", successCount);
            response.put("message", String.format("全平台广播完成，成功推送给 %d 个用户", successCount));
            
            log.info("全平台广播完成 - 消息ID: {}, 成功数量: {}", message.getMessageId(), successCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("全平台广播异常 - 错误: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "服务器内部错误: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
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
    public ResponseEntity<Map<String, Object>> sendSystemNotification(
            @Parameter(description = "系统通知请求", required = true)
            @RequestBody SystemNotificationRequest request) {
        
        log.info("收到系统通知请求 - 用户: {}, 组织: {}, 标题: {}", 
                request.getUserId(), request.getOrganizationId(), request.getTitle());
        
        try {
            // 调用消息服务发送系统通知
            boolean success = messageService.sendSystemNotification(
                    request.getUserId(), 
                    request.getOrganizationId(), 
                    request.getTitle(), 
                    request.getContent()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "系统通知发送成功" : "系统通知发送失败");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("发送系统通知异常 - 错误: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "服务器内部错误: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
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
    public ResponseEntity<Map<String, Object>> batchSendMessage(
            @Parameter(description = "批量发送请求", required = true)
            @RequestBody BatchSendRequest request) {
        
        log.info("收到批量发送请求 - 用户数量: {}, 消息类型: {}", 
                request.getUserIds().size(), request.getMessage().getType());
        
        try {
            // 调用消息服务批量发送消息
            int successCount = messageService.batchSendMessage(request.getUserIds(), request.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", successCount > 0);
            response.put("totalUsers", request.getUserIds().size());
            response.put("successCount", successCount);
            response.put("failureCount", request.getUserIds().size() - successCount);
            response.put("message", String.format("批量发送完成，成功 %d 个，失败 %d 个", 
                    successCount, request.getUserIds().size() - successCount));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("批量发送异常 - 错误: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "服务器内部错误: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 查询用户在线状态
     * 
     * @param userId 用户ID
     * @return 在线状态信息
     */
    @GetMapping("/status/user/{userId}")
    @Operation(summary = "查询用户在线状态", description = "查询指定用户的在线状态和连接信息")
    public ResponseEntity<Map<String, Object>> getUserStatus(
            @Parameter(description = "用户ID", required = true)
            @PathVariable String userId) {
        
        try {
            boolean online = messageService.isUserOnline(userId);
            int connectionCount = messageService.getUserConnectionCount(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("online", online);
            response.put("connectionCount", connectionCount);
            response.put("message", online ? "用户在线" : "用户离线");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("查询用户状态异常 - 用户: {}, 错误: {}", userId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "服务器内部错误: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 获取系统统计信息
     * 
     * @return 系统统计数据
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取系统统计信息", description = "获取消息中心的统计信息")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            int totalOnlineUsers = messageService.getTotalOnlineUserCount();
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalOnlineUsers", totalOnlineUsers);
            response.put("timestamp", System.currentTimeMillis());
            response.put("message", "统计信息获取成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取统计信息异常 - 错误: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "服务器内部错误: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 系统通知请求参数
     */
    public static class SystemNotificationRequest {
        private String userId;          // 可选，指定用户ID
        private String organizationId;  // 必填，组织ID
        private String title;           // 必填，通知标题
        private String content;         // 必填，通知内容
        
        // Getters and Setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getOrganizationId() { return organizationId; }
        public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
    
    /**
     * 批量发送请求参数
     */
    public static class BatchSendRequest {
        private List<String> userIds;    // 必填，目标用户ID列表
        private WebSocketMessage message; // 必填，消息内容
        
        // Getters and Setters
        public List<String> getUserIds() { return userIds; }
        public void setUserIds(List<String> userIds) { this.userIds = userIds; }
        
        public WebSocketMessage getMessage() { return message; }
        public void setMessage(WebSocketMessage message) { this.message = message; }
    }
}