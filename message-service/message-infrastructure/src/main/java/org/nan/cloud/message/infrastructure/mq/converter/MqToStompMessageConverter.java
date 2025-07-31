package org.nan.cloud.message.infrastructure.mq.converter;

import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.api.enums.Priority;
import org.nan.cloud.message.infrastructure.websocket.stomp.enums.StompMessageTypes;
import org.nan.cloud.message.infrastructure.websocket.stomp.model.CommonStompMessage;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * MQ消息到STOMP消息转换器
 * 
 * 负责将来自RabbitMQ的业务消息转换为标准的STOMP消息格式。
 * 支持不同类型的业务消息转换，包括设备状态、指令执行、系统通知等。
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
public class MqToStompMessageConverter {
    
    /**
     * 转换设备状态变更消息
     * 
     * @param deviceId 设备ID
     * @param orgId 组织ID
     * @param status 设备状态
     * @param statusData 状态详细数据
     * @return STOMP消息
     */
    public CommonStompMessage convertDeviceStatusMessage(String deviceId, Long orgId, String status, Object statusData) {
        log.debug("转换设备状态消息 - 设备: {}, 组织: {}, 状态: {}", deviceId, orgId, status);
        
        // 构建消息来源信息
        CommonStompMessage.Source source = CommonStompMessage.Source.builder()
                .serviceId("terminal-service")
                .resourceType("DEVICE")
                .resourceId(deviceId)
                .build();
        
        // 构建消息目标信息 - 设备状态消息发送到终端主题
        CommonStompMessage.Target target = CommonStompMessage.Target.builder()
                .targetType("TERMINAL")
                .oid(orgId)
                .destination("/topic/terminal/" + deviceId + "/status")
                .build();
        
        // 构建消息元数据
        CommonStompMessage.Metadata metadata = CommonStompMessage.Metadata.builder()
                .priority(Priority.NORMAL)
                .persistent(false)  // 设备状态消息不需要持久化
                .ttl(30000L)  // 30秒TTL
                .requireAck(false)
                .build();
        
        return CommonStompMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .messageType(StompMessageTypes.TERMINAL_STATUS_CHANGE)
                .source(source)
                .target(target)
                .payload(statusData)
                .message(String.format("设备%s状态变更为%s", deviceId, status))
                .metadata(metadata)
                .build();
    }
    
    /**
     * 转换指令执行结果消息
     * 
     * @param commandId 指令ID
     * @param deviceId 设备ID
     * @param orgId 组织ID
     * @param userId 用户ID
     * @param result 执行结果
     * @param resultData 结果详细数据
     * @return STOMP消息
     */
    public CommonStompMessage convertCommandResultMessage(String commandId, String deviceId, 
            Long orgId, Long userId, String result, Object resultData) {
        log.debug("转换指令执行结果消息 - 指令: {}, 设备: {}, 用户: {}, 结果: {}", 
                commandId, deviceId, userId, result);
        
        // 构建消息来源信息
        CommonStompMessage.Source source = CommonStompMessage.Source.builder()
                .serviceId("terminal-service")
                .resourceType("COMMAND")
                .resourceId(commandId)
                .taskId(commandId)
                .build();
        
        // 构建消息目标信息 - 指令结果发送给执行用户和相关终端主题
        CommonStompMessage.Target target = CommonStompMessage.Target.builder()
                .targetType("USER_AND_TERMINAL")
                .uids(List.of(userId))
                .oid(orgId)
                .destination("/topic/user/" + userId + "/commands," + "/topic/terminal/" + deviceId + "/commands")
                .build();
        
        // 构建消息元数据
        CommonStompMessage.Metadata metadata = CommonStompMessage.Metadata.builder()
                .priority(Priority.HIGH)  // 指令结果优先级高
                .persistent(true)  // 指令结果需要持久化
                .ttl(300000L)  // 5分钟TTL
                .requireAck(true)  // 需要确认收到
                .build();
        
        return CommonStompMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .messageType(StompMessageTypes.COMMAND_FEEDBACK)
                .source(source)
                .target(target)
                .payload(resultData)
                .message(String.format("设备%s指令%s执行结果: %s", deviceId, commandId, result))
                .metadata(metadata)
                .build();
    }
    
    /**
     * 转换系统通知消息
     * 
     * @param notificationType 通知类型
     * @param orgId 组织ID
     * @param targetUserIds 目标用户ID列表
     * @param title 通知标题
     * @param content 通知内容
     * @param notificationData 通知详细数据
     * @return STOMP消息
     */
    public CommonStompMessage convertSystemNotificationMessage(String notificationType, Long orgId,
            List<Long> targetUserIds, String title, String content, Object notificationData) {
        log.debug("转换系统通知消息 - 类型: {}, 组织: {}, 目标用户数: {}", 
                notificationType, orgId, targetUserIds != null ? targetUserIds.size() : 0);
        
        // 构建消息来源信息
        CommonStompMessage.Source source = CommonStompMessage.Source.builder()
                .serviceId("core-service")
                .resourceType("NOTIFICATION")
                .resourceId(notificationType)
                .build();
        
        // 构建消息目标信息
        CommonStompMessage.Target target;
        if (targetUserIds != null && !targetUserIds.isEmpty()) {
            // 发送给特定用户
            target = CommonStompMessage.Target.builder()
                    .targetType("USERS")
                    .uids(targetUserIds)
                    .oid(orgId)
                    .destination(buildUserNotificationTopics(targetUserIds))
                    .build();
        } else {
            // 发送给整个组织
            target = CommonStompMessage.Target.builder()
                    .targetType("ORG")
                    .oid(orgId)
                    .destination("/topic/org/" + orgId + "/notifications")
                    .build();
        }
        
        // 构建消息元数据
        CommonStompMessage.Metadata metadata = CommonStompMessage.Metadata.builder()
                .priority(Priority.NORMAL)
                .persistent(true)  // 通知消息需要持久化
                .ttl(86400000L)  // 24小时TTL
                .requireAck(false)
                .build();
        
        return CommonStompMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .messageType(StompMessageTypes.NOTIFICATION)
                .source(source)
                .target(target)
                .payload(notificationData)
                .message(title + ": " + content)
                .metadata(metadata)
                .build();
    }
    
    /**
     * 转换批量指令进度消息
     * 
     * @param batchId 批量任务ID
     * @param orgId 组织ID
     * @param userId 用户ID
     * @param progress 进度信息
     * @param progressData 进度详细数据
     * @return STOMP消息
     */
    public CommonStompMessage convertBatchCommandProgressMessage(String batchId, Long orgId, 
            Long userId, String progress, Object progressData) {
        log.debug("转换批量指令进度消息 - 批量任务: {}, 用户: {}, 进度: {}", batchId, userId, progress);
        
        // 构建消息来源信息
        CommonStompMessage.Source source = CommonStompMessage.Source.builder()
                .serviceId("terminal-service")
                .resourceType("BATCH_COMMAND")
                .resourceId(batchId)
                .taskId(batchId)
                .executionId(UUID.randomUUID().toString())
                .batchContext("PROGRESS_UPDATE")
                .build();
        
        // 构建消息目标信息 - 批量指令进度发送到专用主题
        CommonStompMessage.Target target = CommonStompMessage.Target.builder()
                .targetType("USER")
                .uids(List.of(userId))
                .oid(orgId)
                .destination("/topic/commandTask/" + batchId + "/progress")
                .build();
        
        // 构建消息元数据
        CommonStompMessage.Metadata metadata = CommonStompMessage.Metadata.builder()
                .priority(Priority.NORMAL)
                .persistent(false)  // 进度消息不需要持久化
                .ttl(60000L)  // 1分钟TTL
                .requireAck(false)
                .build();
        
        return CommonStompMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .messageType(StompMessageTypes.TASK_PROGRESS)
                .source(source)
                .target(target)
                .payload(progressData)
                .message(String.format("批量任务%s进度更新: %s", batchId, progress))
                .metadata(metadata)
                .build();
    }
    
    /**
     * 通用MQ消息转换方法
     * 
     * @param mqMessageType MQ消息类型
     * @param mqPayload MQ消息载荷
     * @param routingKey MQ路由键
     * @return STOMP消息，如果不支持转换则返回null
     */
    public CommonStompMessage convertGenericMqMessage(String mqMessageType, Object mqPayload, String routingKey) {
        log.debug("通用MQ消息转换 - 类型: {}, 路由键: {}", mqMessageType, routingKey);
        
        try {
            // 根据MQ消息类型和路由键进行转换
            switch (mqMessageType.toUpperCase()) {
                case "DEVICE_STATUS":
                    return convertMqDeviceStatusMessage(mqPayload, routingKey);
                case "COMMAND_RESULT":
                    return convertMqCommandResultMessage(mqPayload, routingKey);
                case "SYSTEM_NOTIFICATION":
                    return convertMqSystemNotificationMessage(mqPayload, routingKey);
                case "BATCH_PROGRESS":
                    return convertMqBatchProgressMessage(mqPayload, routingKey);
                default:
                    log.warn("不支持的MQ消息类型转换: {}", mqMessageType);
                    return null;
            }
        } catch (Exception e) {
            log.error("MQ消息转换异常 - 类型: {}, 错误: {}", mqMessageType, e.getMessage(), e);
            return null;
        }
    }
    
    // ==================== 私有工具方法 ====================
    
    /**
     * 构建用户通知主题路径
     */
    private String buildUserNotificationTopics(List<Long> userIds) {
        return userIds.stream()
                .map(userId -> "/topic/user/" + userId + "/notifications")
                .reduce((topic1, topic2) -> topic1 + "," + topic2)
                .orElse("");
    }
    
    /**
     * 转换MQ设备状态消息（从JSON解析）
     */
    private CommonStompMessage convertMqDeviceStatusMessage(Object payload, String routingKey) {
        // 这里应该根据实际的MQ消息格式进行解析
        // 示例实现，实际使用时需要根据具体的消息格式调整
        return null; // TODO: 实现具体的解析逻辑
    }
    
    /**
     * 转换MQ指令结果消息（从JSON解析）
     */
    private CommonStompMessage convertMqCommandResultMessage(Object payload, String routingKey) {
        // TODO: 实现具体的解析逻辑
        return null;
    }
    
    /**
     * 转换MQ系统通知消息（从JSON解析）
     */
    private CommonStompMessage convertMqSystemNotificationMessage(Object payload, String routingKey) {
        // TODO: 实现具体的解析逻辑
        return null;
    }
    
    /**
     * 转换MQ批量进度消息（从JSON解析）
     */
    private CommonStompMessage convertMqBatchProgressMessage(Object payload, String routingKey) {
        // TODO: 实现具体的解析逻辑
        return null;
    }
}