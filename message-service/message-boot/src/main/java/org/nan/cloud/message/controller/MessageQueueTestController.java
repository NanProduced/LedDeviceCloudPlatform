package org.nan.cloud.message.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.service.MessageQueueService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 消息队列测试控制器
 * 
 * 提供RabbitMQ消息队列功能的测试接口
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/test/mq")
@RequiredArgsConstructor
public class MessageQueueTestController {
    
    private final MessageQueueService messageQueueService;
    
    /**
     * 测试异步通知消息
     */
    @PostMapping("/notification")
    public Map<String, Object> testNotification(@RequestBody Map<String, String> request) {
        try {
            String receiverId = request.get("receiverId");
            String organizationId = request.get("organizationId");
            String title = request.get("title");
            String content = request.get("content");
            
            log.info("测试异步通知消息: receiverId={}, title={}", receiverId, title);
            
            messageQueueService.sendNotificationAsync(receiverId, organizationId, title, content);
            
            return Map.of(
                "success", true,
                "message", "通知消息已发送到队列",
                "timestamp", System.currentTimeMillis()
            );
        } catch (Exception e) {
            log.error("测试通知消息失败", e);
            return Map.of(
                "success", false,
                "message", "发送失败: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * 测试异步系统消息
     */
    @PostMapping("/system")
    public Map<String, Object> testSystemMessage(@RequestBody Map<String, String> request) {
        try {
            String organizationId = request.get("organizationId");
            String title = request.get("title");
            String content = request.get("content");
            
            log.info("测试异步系统消息: organizationId={}, title={}", organizationId, title);

            messageQueueService.sendSystemMessageAsync(organizationId, title, content);
            
            return Map.of(
                "success", true,
                "message", "系统消息已发送到队列",
                "timestamp", System.currentTimeMillis()
            );
        } catch (Exception e) {
            log.error("测试系统消息失败", e);
            return Map.of(
                "success", false,
                "message", "发送失败: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * 测试异步用户消息
     */
    @PostMapping("/user")
    public Map<String, Object> testUserMessage(@RequestBody Map<String, String> request) {
        try {
            String senderId = request.get("senderId");
            String senderName = request.get("senderName");
            String receiverId = request.get("receiverId");
            String organizationId = request.get("organizationId");
            String title = request.get("title");
            String content = request.get("content");
            
            log.info("测试异步用户消息: senderId={}, receiverId={}, title={}", 
                    senderId, receiverId, title);

            messageQueueService.sendUserMessageAsync(senderId, senderName, receiverId,
                    organizationId, title, content);
            
            return Map.of(
                "success", true,
                "message", "用户消息已发送到队列",
                "timestamp", System.currentTimeMillis()
            );
        } catch (Exception e) {
            log.error("测试用户消息失败", e);
            return Map.of(
                "success", false,
                "message", "发送失败: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * 测试异步广播消息
     */
    @PostMapping("/broadcast")
    public Map<String, Object> testBroadcast(@RequestBody Map<String, String> request) {
        try {
            String senderId = request.get("senderId");
            String senderName = request.get("senderName");
            String organizationId = request.get("organizationId");
            String title = request.get("title");
            String content = request.get("content");
            
            log.info("测试异步广播消息: senderId={}, organizationId={}, title={}", 
                    senderId, organizationId, title);

            messageQueueService.sendBroadcastAsync(senderId, senderName, organizationId, title, content);
            
            return Map.of(
                "success", true,
                "message", "广播消息已发送到队列",
                "timestamp", System.currentTimeMillis()
            );
        } catch (Exception e) {
            log.error("测试广播消息失败", e);
            return Map.of(
                "success", false,
                "message", "发送失败: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * 测试异步设备告警
     */
    @PostMapping("/device-alert")
    public Map<String, Object> testDeviceAlert(@RequestBody Map<String, String> request) {
        try {
            String userId = request.get("userId");
            String organizationId = request.get("organizationId");
            String deviceId = request.get("deviceId");
            String title = request.get("title");
            String content = request.get("content");
            
            log.info("测试异步设备告警: deviceId={}, userId={}, title={}", 
                    deviceId, userId, title);

            messageQueueService.sendDeviceAlertAsync(userId, organizationId, deviceId, title, content);
            
            return Map.of(
                "success", true,
                "message", "设备告警已发送到队列",
                "timestamp", System.currentTimeMillis()
            );
        } catch (Exception e) {
            log.error("测试设备告警失败", e);
            return Map.of(
                "success", false,
                "message", "发送失败: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * 快速测试 - 发送一条简单的通知消息
     */
    @GetMapping("/quick-test")
    public Map<String, Object> quickTest(@RequestParam(name = "userId", defaultValue = "test-user-001") String userId,
                                       @RequestParam(name = "orgId", defaultValue = "test-org-001") String orgId) {
        try {
            String title = "队列测试消息";
            String content = "这是一条通过RabbitMQ队列发送的测试消息，时间: " + 
                           java.time.LocalDateTime.now();
            
            log.info("快速测试RabbitMQ: userId={}, orgId={}", userId, orgId);

            messageQueueService.sendNotificationAsync(userId, orgId, title, content);
            
            return Map.of(
                "success", true,
                "message", "快速测试消息已发送",
                "userId", userId,
                "organizationId", orgId,
                "timestamp", System.currentTimeMillis()
            );
        } catch (Exception e) {
            log.error("快速测试失败", e);
            return Map.of(
                "success", false,
                "message", "测试失败: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
        }
    }
}