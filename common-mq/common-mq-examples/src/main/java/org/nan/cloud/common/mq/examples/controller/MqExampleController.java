package org.nan.cloud.common.mq.examples.controller;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.mq.examples.service.NotificationService;
import org.nan.cloud.common.mq.producer.MessageProducer;
import org.nan.cloud.common.mq.producer.ProducerStats;
import org.nan.cloud.common.mq.producer.HealthStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 消息队列示例控制器
 * 
 * 提供REST API来演示消息发送功能。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/mq/examples")
@RequiredArgsConstructor
public class MqExampleController {
    
    private final NotificationService notificationService;
    private final MessageProducer messageProducer;
    
    /**
     * 发送用户通知
     */
    @PostMapping("/notification/user")
    public Map<String, Object> sendUserNotification(
            @RequestParam String userId,
            @RequestParam String title,
            @RequestParam String content) {
        
        notificationService.sendUserNotification(userId, title, content);
        
        return Map.of(
            "success", true,
            "message", "用户通知已发送",
            "userId", userId,
            "title", title
        );
    }
    
    /**
     * 发送系统消息
     */
    @PostMapping("/notification/system")
    public Map<String, Object> sendSystemMessage(
            @RequestParam String title,
            @RequestParam String content) {
        
        notificationService.sendSystemMessage(title, content);
        
        return Map.of(
            "success", true,
            "message", "系统消息已发送",
            "title", title
        );
    }
    
    /**
     * 批量发送通知
     */
    @PostMapping("/notification/batch")
    public Map<String, Object> sendBatchNotifications(
            @RequestBody List<String> userIds,
            @RequestParam String title,
            @RequestParam String content) {
        
        notificationService.sendBatchNotifications(userIds, title, content);
        
        return Map.of(
            "success", true,
            "message", "批量通知已发送",
            "userCount", userIds.size(),
            "title", title
        );
    }
    
    /**
     * 发送业务事件
     */
    @PostMapping("/event")
    public Map<String, Object> sendBusinessEvent(
            @RequestParam String eventType,
            @RequestBody Map<String, Object> eventData) {
        
        notificationService.sendBusinessEvent(eventType, eventData);
        
        return Map.of(
            "success", true,
            "message", "业务事件已发送",
            "eventType", eventType
        );
    }
    
    /**
     * 发送延迟消息
     */
    @PostMapping("/notification/delayed")
    public Map<String, Object> sendDelayedMessage(
            @RequestParam String userId,
            @RequestParam String content,
            @RequestParam(defaultValue = "5000") long delayMillis) {
        
        notificationService.sendDelayedMessage(userId, content, delayMillis);
        
        return Map.of(
            "success", true,
            "message", "延迟消息已发送",
            "userId", userId,
            "delayMillis", delayMillis
        );
    }
    
    /**
     * 获取生产者统计信息
     */
    @GetMapping("/stats/producer")
    public ProducerStats getProducerStats() {
        return messageProducer.getStats();
    }
    
    /**
     * 获取生产者健康状态
     */
    @GetMapping("/health/producer")
    public HealthStatus getProducerHealth() {
        return messageProducer.getHealth();
    }
    
    /**
     * 测试接口
     */
    @PostMapping("/test")
    public Map<String, Object> test() {
        // 发送各种类型的测试消息
        notificationService.sendUserNotification("test-user", "测试通知", "这是一条测试通知消息");
        notificationService.sendSystemMessage("系统测试", "这是一条系统测试消息");
        notificationService.sendBusinessEvent("USER_LOGIN", Map.of("userId", "test-user", "timestamp", System.currentTimeMillis()));
        
        return Map.of(
            "success", true,
            "message", "测试消息已发送",
            "timestamp", System.currentTimeMillis()
        );
    }
}