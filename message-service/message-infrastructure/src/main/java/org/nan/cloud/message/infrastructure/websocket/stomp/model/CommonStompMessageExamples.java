package org.nan.cloud.message.infrastructure.websocket.stomp.model;

import org.nan.cloud.message.api.enums.Priority;
import org.nan.cloud.message.infrastructure.websocket.routing.SubscriptionLevel;
import org.nan.cloud.message.infrastructure.websocket.stomp.enums.StompMessageTypes;

import java.util.List;
import java.util.Map;

/**
 * CommonStompMessage快速构造方法使用示例
 * 
 * 这个类展示了如何使用CommonStompMessage的各种快速构造方法，
 * 简化STOMP消息的创建过程。
 * 
 * @author Nan
 * @since 1.0.0
 */
public class CommonStompMessageExamples {

    private CommonStompMessageExamples() {
        throw new UnsupportedOperationException("示例禁止实例化");
    }

    /**
     * 示例1: 创建简单文本消息
     */
    public void example1_SimpleText() {
        // 创建一个简单的文本消息
        CommonStompMessage message = CommonStompMessage.simpleText(
            StompMessageTypes.NOTIFICATION, 
            "这是一条简单的通知消息"
        );
        
        // 消息会自动生成messageId和timestamp
        // 默认优先级为NORMAL，不需要确认
    }

    /**
     * 示例2: 发送消息给特定用户
     */
    public void example2_ToUser() {
        Long userId = 12345L;
        
        // 发送设备状态给特定用户
        Map<String, Object> deviceStatus = Map.of(
            "deviceId", "LED-001",
            "status", "ONLINE",
            "battery", 85,
            "lastSeen", System.currentTimeMillis()
        );
        
        CommonStompMessage message = CommonStompMessage.toUser(
            StompMessageTypes.TERMINAL_STATUS_CHANGE,
            userId,
            deviceStatus
        );
        
        // 也可以同时发送payload和文本消息
        CommonStompMessage messageWithText = CommonStompMessage.toUser(
            StompMessageTypes.TERMINAL_STATUS_CHANGE,
            userId,
            deviceStatus,
            "设备LED-001状态已更新"
        );
    }

    /**
     * 示例3: 多用户广播消息
     */
    public void example3_ToUsers() {
        List<Long> userIds = List.of(123L, 456L, 789L);
        
        Map<String, Object> alertData = Map.of(
            "type", "SYSTEM_ALERT",
            "level", "HIGH",
            "message", "系统将在10分钟后维护"
        );
        
        CommonStompMessage message = CommonStompMessage.toUsers(
            StompMessageTypes.ALERT,
            userIds,
            alertData
        );
    }

    /**
     * 示例4: 组织广播消息
     */
    public void example4_ToOrganization() {
        Long orgId = 1001L;
        
        Map<String, Object> announcement = Map.of(
            "title", "重要通知",
            "content", "新版本功能已上线，请查看更新日志",
            "publishTime", System.currentTimeMillis(),
            "priority", "HIGH"
        );
        
        CommonStompMessage message = CommonStompMessage.toOrganization(
            StompMessageTypes.NOTIFICATION,
            orgId,
            announcement
        );
    }

    /**
     * 示例5: 主题消息
     */
    public void example5_ToTopic() {
        String topicPath = "/topic/device/status/building-A";
        
        Map<String, Object> statusUpdate = Map.of(
            "buildingId", "building-A",
            "deviceCount", 25,
            "onlineCount", 23,
            "offlineCount", 2,
            "updateTime", System.currentTimeMillis()
        );
        
        CommonStompMessage message = CommonStompMessage.toTopic(
            StompMessageTypes.MONITOR_DATA,
            topicPath,
            statusUpdate
        );
    }

    /**
     * 示例6: 可靠消息（需要确认）
     */
    public void example6_ReliableMessage() {
        Map<String, Object> criticalCommand = Map.of(
            "commandId", "CMD-001",
            "action", "EMERGENCY_SHUTDOWN",
            "deviceId", "LED-CRITICAL-001",
            "reason", "过热保护"
        );
        
        CommonStompMessage message = CommonStompMessage.reliable(
            StompMessageTypes.COMMAND_FEEDBACK,
            criticalCommand,
            Priority.URGENT
        );
        
        // 可靠消息会自动设置：
        // - persistent = true
        // - requireAck = true
        // - TTL = 5分钟
    }

    /**
     * 示例7: 使用专用的业务消息方法
     */
    public void example7_BusinessMessages() {
        // 系统通知
        CommonStompMessage systemNotification = CommonStompMessage.systemNotification(
            "系统维护通知",
            "系统将于今晚22:00-24:00进行例行维护，请提前保存工作",
            1001L
        );
        
        // 设备状态消息
        Map<String, Object> deviceData = Map.of(
            "temperature", 45.5,
            "humidity", 60.2,
            "powerLevel", 78
        );
        
        CommonStompMessage deviceStatus = CommonStompMessage.deviceStatus(
            "LED-001",
            1001L,
            deviceData
        );
        
        // 指令执行结果
        Map<String, Object> commandResult = Map.of(
            "success", true,
            "executionTime", 1250,
            "result", "设备已成功重启"
        );
        
        CommonStompMessage cmdResult = CommonStompMessage.commandResult(
            "CMD-123",
            "LED-001",
            12345L,
            commandResult
        );
        
        // 批量任务进度
        Map<String, Object> progressData = Map.of(
            "totalDevices", 100,
            "completedDevices", 75,
            "failedDevices", 2,
            "progress", 75.0
        );
        
        CommonStompMessage batchProgress = CommonStompMessage.batchProgress(
            "BATCH-001",
            12345L,
            progressData
        );
    }

    /**
     * 示例8: 消息副本和重试
     */
    public void example8_CopyAndRetry() {
        // 创建原始消息
        CommonStompMessage original = CommonStompMessage.toUser(
            StompMessageTypes.NOTIFICATION,
            12345L,
            Map.of("message", "这是原始消息")
        );
        
        // 基于原始消息创建副本，修改部分字段
        CommonStompMessage modified = CommonStompMessage.copyFrom(original)
            .message("这是修改后的消息")
            .metadata(CommonStompMessage.Metadata.builder()
                .priority(Priority.HIGH)
                .persistent(true)
                .requireAck(false)
                .retryCount(0)
                .build())
            .build();
        
        // 为重试生成新的消息ID
        CommonStompMessage retryMessage = original.withNewId();
        
        // 增加重试次数
        CommonStompMessage incrementedRetry = original.withIncrementedRetry();
    }

    /**
     * 示例9: 主题订阅反馈消息
     */
    public void example9_SubscriptionFeedback() {
        Long userId = 12345L;
        String topicPath = "/topic/device/status";
        
        // 成功订阅反馈
        StompTopicSubscribeFeedbackMsg successFeedback = 
            StompTopicSubscribeFeedbackMsg.successFeedback(
                userId, 
                SubscriptionLevel.SESSION, 
                topicPath
            );
        
        // 失败订阅反馈
        StompTopicSubscribeFeedbackMsg failureFeedback = 
            StompTopicSubscribeFeedbackMsg.failureFeedback(
                userId,
                SubscriptionLevel.SESSION,
                topicPath,
                "主题不存在"
            );
        
        // 权限不足反馈
        StompTopicSubscribeFeedbackMsg permissionDenied = 
            StompTopicSubscribeFeedbackMsg.permissionDeniedFeedback(
                userId,
                topicPath,
                "READ_DEVICE_STATUS"
            );
        
        // 取消订阅成功反馈
        StompTopicSubscribeFeedbackMsg unsubscribeSuccess = 
            StompTopicSubscribeFeedbackMsg.unsubscribeSuccessFeedback(
                userId,
                topicPath
            );
        
        // 检查反馈消息
        if (successFeedback.isSuccess() && successFeedback.isSubscribeOperation()) {
            System.out.println("订阅成功: " + successFeedback.getTopicPath());
        }
    }

    /**
     * 示例10: 链式调用和Builder模式组合
     */
    public void example10_ChainedBuilder() {
        // 使用快速方法创建基础消息，然后用Builder进行定制
        CommonStompMessage customMessage = CommonStompMessage.copyFrom(
            CommonStompMessage.toUser(
                StompMessageTypes.NOTIFICATION,
                12345L,
                Map.of("data", "基础数据")
            )
        )
        .message("定制化消息文本")
        .subType_1("CUSTOM_NOTIFICATION")
        .subType_2("PRIORITY_HIGH")
        .source(CommonStompMessage.Source.builder()
            .serviceId("custom-service")
            .resourceType("CUSTOM_RESOURCE")
            .resourceId("RESOURCE-001")
            .build())
        .metadata(CommonStompMessage.Metadata.builder()
            .priority(Priority.HIGH)
            .persistent(true)
            .requireAck(true)
            .ttl(600000L) // 10分钟
            .correlationId("CORR-123")
            .build())
        .build();
        
        // 这样既享受了快速构造的便利，又保持了完全的定制化能力
    }
}