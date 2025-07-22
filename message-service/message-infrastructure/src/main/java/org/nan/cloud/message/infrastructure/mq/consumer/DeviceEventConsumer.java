package org.nan.cloud.message.infrastructure.mq.consumer;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.api.dto.websocket.WebSocketMessage;
import org.nan.cloud.message.api.enums.MessageType;
import org.nan.cloud.message.api.enums.Priority;
import org.nan.cloud.message.api.event.MessageEvent;
import org.nan.cloud.message.infrastructure.websocket.manager.WebSocketConnectionManager;
import org.nan.cloud.message.utils.MessageUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 设备事件消费者
 * 
 * 处理所有设备相关的事件消息，包括设备状态变更、设备告警、设备上下线等。
 * 负责将设备事件转换为WebSocket消息推送给相关用户，确保用户能及时了解设备状态。
 * 
 * 主要处理的设备事件类型：
 * 1. DEVICE_ONLINE - 设备上线事件
 * 2. DEVICE_OFFLINE - 设备下线事件
 * 3. DEVICE_ALERT - 设备告警事件
 * 4. DEVICE_STATUS_CHANGED - 设备状态变更事件
 * 5. DEVICE_ERROR - 设备异常事件
 * 6. DEVICE_MAINTENANCE - 设备维护事件
 * 
 * 处理策略：
 * - 高优先级事件（告警、异常）立即推送
 * - 普通状态事件延迟推送，避免频繁通知
 * - 支持按设备类型和用户权限过滤推送
 * - 记录设备事件历史，用于后续分析
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceEventConsumer {
    
    private final WebSocketConnectionManager webSocketConnectionManager;
    
    /**
     * 处理设备事件队列
     * 
     * 消费device.event.queue中的所有设备事件，根据事件类型和优先级
     * 决定推送策略和目标用户。
     * 
     * @param event 设备事件
     * @param message RabbitMQ原始消息
     * @param channel RabbitMQ通道
     */
    @RabbitListener(
        queues = "device.event.queue",
        ackMode = "MANUAL",
        concurrency = "2-5"
    )
    public void handleDeviceEvent(MessageEvent event, Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        
        try {
            log.info("处理设备事件: messageId={}, eventType={}, deviceInfo={}", 
                    event.getMessageId(), event.getEventType(), event.getMetadata());
            
            // 根据设备事件类型进行不同处理
            switch (event.getEventType()) {
                case "DEVICE_ONLINE":
                    handleDeviceOnlineEvent(event);
                    break;
                case "DEVICE_OFFLINE":
                    handleDeviceOfflineEvent(event);
                    break;
                case "DEVICE_ALERT":
                    handleDeviceAlertEvent(event);
                    break;
                case "DEVICE_STATUS_CHANGED":
                    handleDeviceStatusChangedEvent(event);
                    break;
                case "DEVICE_ERROR":
                    handleDeviceErrorEvent(event);
                    break;
                case "DEVICE_MAINTENANCE":
                    handleDeviceMaintenanceEvent(event);
                    break;
                default:
                    log.warn("未知的设备事件类型: eventType={}, messageId={}", 
                            event.getEventType(), event.getMessageId());
                    handleUnknownDeviceEvent(event);
            }
            
            // 手动确认消息
            channel.basicAck(deliveryTag, false);
            log.info("设备事件处理完成: messageId={}, eventType={}", 
                    event.getMessageId(), event.getEventType());
                    
        } catch (Exception e) {
            log.error("处理设备事件失败: messageId={}, eventType={}, error={}", 
                     event.getMessageId(), event.getEventType(), e.getMessage(), e);
            
            handleDeviceEventProcessingError(event, channel, deliveryTag, e);
        }
    }
    
    /**
     * 处理设备上线事件
     * 
     * 当设备重新连接或启动时触发，通知相关用户设备已恢复正常。
     * 
     * @param event 设备上线事件
     */
    private void handleDeviceOnlineEvent(MessageEvent event) {
        try {
            Map<String, Object> deviceInfo = event.getMetadata();
            String deviceId = (String) deviceInfo.get("deviceId");
            String deviceName = (String) deviceInfo.get("deviceName");
            String deviceType = (String) deviceInfo.get("deviceType");
            
            log.info("处理设备上线事件: deviceId={}, deviceName={}, deviceType={}", 
                    deviceId, deviceName, deviceType);
            
            // 创建设备上线通知消息
            WebSocketMessage notificationMessage = WebSocketMessage.builder()
                .messageId(MessageUtils.generateMessageId())
                .type(MessageType.DEVICE_STATUS)
                .title("设备上线通知")
                .content(String.format("设备 %s (%s) 已成功上线，当前状态正常。", deviceName, deviceId))
                .priority(Priority.NORMAL)
                .organizationId(event.getOrganizationId())
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                    "deviceId", deviceId,
                    "deviceName", deviceName,
                    "deviceType", deviceType,
                    "eventType", "DEVICE_ONLINE",
                    "onlineTime", LocalDateTime.now().toString()
                ))
                .requireAck(false)
                .build();
            
            // 推送给组织内的用户（设备管理员和相关操作员）
            int sentCount = webSocketConnectionManager.broadcastToOrganization(
                event.getOrganizationId(), notificationMessage);
            
            log.info("设备上线通知已发送: deviceId={}, sentCount={}", deviceId, sentCount);
            
        } catch (Exception e) {
            log.error("处理设备上线事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 处理设备下线事件
     * 
     * 当设备断开连接或停止响应时触发，及时通知用户设备异常。
     * 
     * @param event 设备下线事件
     */
    private void handleDeviceOfflineEvent(MessageEvent event) {
        try {
            Map<String, Object> deviceInfo = event.getMetadata();
            String deviceId = (String) deviceInfo.get("deviceId");
            String deviceName = (String) deviceInfo.get("deviceName");
            String reason = (String) deviceInfo.get("offlineReason");
            
            log.warn("处理设备下线事件: deviceId={}, deviceName={}, reason={}", 
                    deviceId, deviceName, reason);
            
            // 创建设备下线告警消息
            WebSocketMessage alertMessage = WebSocketMessage.builder()
                .messageId(MessageUtils.generateMessageId())
                .type(MessageType.DEVICE_ALERT)
                .title("设备离线告警")
                .content(String.format("设备 %s (%s) 已离线。离线原因：%s。请及时检查设备状态。", 
                        deviceName, deviceId, reason != null ? reason : "未知"))
                .priority(Priority.HIGH)
                .organizationId(event.getOrganizationId())
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                    "deviceId", deviceId,
                    "deviceName", deviceName,
                    "eventType", "DEVICE_OFFLINE",
                    "offlineReason", reason != null ? reason : "未知",
                    "offlineTime", LocalDateTime.now().toString(),
                    "alertLevel", "WARNING"
                ))
                .requireAck(true) // 设备下线需要确认
                .build();
            
            // 推送给组织内的用户，优先级较高
            int sentCount = webSocketConnectionManager.broadcastToOrganization(
                event.getOrganizationId(), alertMessage);
            
            log.warn("设备下线告警已发送: deviceId={}, sentCount={}", deviceId, sentCount);
            
        } catch (Exception e) {
            log.error("处理设备下线事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 处理设备告警事件
     * 
     * 设备主动上报的告警信息，如传感器异常、硬件故障等。
     * 这类事件优先级最高，需要立即通知相关人员。
     * 
     * @param event 设备告警事件
     */
    private void handleDeviceAlertEvent(MessageEvent event) {
        try {
            Map<String, Object> alertInfo = event.getMetadata();
            String deviceId = (String) alertInfo.get("deviceId");
            String deviceName = (String) alertInfo.get("deviceName");
            String alertType = (String) alertInfo.get("alertType");
            String alertLevel = (String) alertInfo.get("alertLevel");
            String alertDescription = (String) alertInfo.get("alertDescription");
            
            log.error("处理设备告警事件: deviceId={}, alertType={}, alertLevel={}", 
                     deviceId, alertType, alertLevel);
            
            // 根据告警级别确定优先级
            Priority priority = determineAlertPriority(alertLevel);
            
            // 创建设备告警消息
            WebSocketMessage alertMessage = WebSocketMessage.builder()
                .messageId(MessageUtils.generateMessageId())
                .type(MessageType.DEVICE_ALERT)
                .title(String.format("设备告警 - %s", alertType))
                .content(String.format("设备 %s (%s) 发生 %s 级别的 %s 告警。详情：%s", 
                        deviceName, deviceId, alertLevel, alertType, alertDescription))
                .priority(priority)
                .organizationId(event.getOrganizationId())
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                    "deviceId", deviceId,
                    "deviceName", deviceName,
                    "alertType", alertType,
                    "alertLevel", alertLevel,
                    "alertDescription", alertDescription,
                    "eventType", "DEVICE_ALERT",
                    "alertTime", LocalDateTime.now().toString()
                ))
                .requireAck(true) // 设备告警必须确认
                .build();
            
            // 对于紧急告警，考虑发送给特定负责人
            if ("CRITICAL".equals(alertLevel) || "URGENT".equals(alertLevel)) {
                // TODO: 获取设备负责人列表，优先通知
                String deviceOwner = (String) alertInfo.get("deviceOwner");
                if (deviceOwner != null) {
                    webSocketConnectionManager.sendMessageToUser(deviceOwner, alertMessage);
                }
            }
            
            // 推送给组织内所有用户
            int sentCount = webSocketConnectionManager.broadcastToOrganization(
                event.getOrganizationId(), alertMessage);
            
            log.error("设备告警消息已发送: deviceId={}, alertType={}, alertLevel={}, sentCount={}", 
                     deviceId, alertType, alertLevel, sentCount);
            
        } catch (Exception e) {
            log.error("处理设备告警事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 处理设备状态变更事件
     * 
     * 设备状态的正常变更，如工作模式切换、参数调整等。
     * 
     * @param event 设备状态变更事件
     */
    private void handleDeviceStatusChangedEvent(MessageEvent event) {
        try {
            Map<String, Object> statusInfo = event.getMetadata();
            String deviceId = (String) statusInfo.get("deviceId");
            String deviceName = (String) statusInfo.get("deviceName");
            String oldStatus = (String) statusInfo.get("oldStatus");
            String newStatus = (String) statusInfo.get("newStatus");
            String changeReason = (String) statusInfo.get("changeReason");
            
            log.info("处理设备状态变更事件: deviceId={}, {} -> {}, reason={}", 
                    deviceId, oldStatus, newStatus, changeReason);
            
            // 创建状态变更通知消息
            WebSocketMessage statusMessage = WebSocketMessage.builder()
                .messageId(MessageUtils.generateMessageId())
                .type(MessageType.DEVICE_STATUS)
                .title("设备状态变更")
                .content(String.format("设备 %s (%s) 状态从 %s 变更为 %s。%s", 
                        deviceName, deviceId, oldStatus, newStatus, 
                        changeReason != null ? "变更原因：" + changeReason : ""))
                .priority(Priority.LOW) // 状态变更通常优先级较低
                .organizationId(event.getOrganizationId())
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                    "deviceId", deviceId,
                    "deviceName", deviceName,
                    "oldStatus", oldStatus,
                    "newStatus", newStatus,
                    "changeReason", changeReason != null ? changeReason : "",
                    "eventType", "DEVICE_STATUS_CHANGED",
                    "changeTime", LocalDateTime.now().toString()
                ))
                .requireAck(false)
                .build();
            
            // 推送给组织内用户
            int sentCount = webSocketConnectionManager.broadcastToOrganization(
                event.getOrganizationId(), statusMessage);
            
            log.info("设备状态变更通知已发送: deviceId={}, sentCount={}", deviceId, sentCount);
            
        } catch (Exception e) {
            log.error("处理设备状态变更事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 处理设备错误事件
     * 
     * 设备运行过程中的错误事件，如通信错误、数据异常等。
     * 
     * @param event 设备错误事件
     */
    private void handleDeviceErrorEvent(MessageEvent event) {
        try {
            Map<String, Object> errorInfo = event.getMetadata();
            String deviceId = (String) errorInfo.get("deviceId");
            String deviceName = (String) errorInfo.get("deviceName");
            String errorCode = (String) errorInfo.get("errorCode");
            String errorMessage = (String) errorInfo.get("errorMessage");
            String errorSeverity = (String) errorInfo.get("errorSeverity");
            
            log.error("处理设备错误事件: deviceId={}, errorCode={}, severity={}", 
                     deviceId, errorCode, errorSeverity);
            
            // 根据错误严重程度确定优先级
            Priority priority = "HIGH".equals(errorSeverity) ? Priority.HIGH : Priority.NORMAL;
            
            // 创建设备错误通知消息
            WebSocketMessage errorNotification = WebSocketMessage.builder()
                .messageId(MessageUtils.generateMessageId())
                .type(MessageType.DEVICE_ALERT)
                .title("设备错误通知")
                .content(String.format("设备 %s (%s) 发生错误。错误代码：%s，错误信息：%s", 
                        deviceName, deviceId, errorCode, errorMessage))
                .priority(priority)
                .organizationId(event.getOrganizationId())
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                    "deviceId", deviceId,
                    "deviceName", deviceName,
                    "errorCode", errorCode,
                    "errorMessage", errorMessage,
                    "errorSeverity", errorSeverity,
                    "eventType", "DEVICE_ERROR",
                    "errorTime", LocalDateTime.now().toString()
                ))
                .requireAck("HIGH".equals(errorSeverity)) // 高严重程度错误需要确认
                .build();
            
            // 推送给组织内用户
            int sentCount = webSocketConnectionManager.broadcastToOrganization(
                event.getOrganizationId(), errorNotification);
            
            log.error("设备错误通知已发送: deviceId={}, errorCode={}, sentCount={}", 
                     deviceId, errorCode, sentCount);
            
        } catch (Exception e) {
            log.error("处理设备错误事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 处理设备维护事件
     * 
     * 设备维护相关的事件，如维护开始、维护完成等。
     * 
     * @param event 设备维护事件
     */
    private void handleDeviceMaintenanceEvent(MessageEvent event) {
        try {
            Map<String, Object> maintenanceInfo = event.getMetadata();
            String deviceId = (String) maintenanceInfo.get("deviceId");
            String deviceName = (String) maintenanceInfo.get("deviceName");
            String maintenanceType = (String) maintenanceInfo.get("maintenanceType");
            String maintenanceStatus = (String) maintenanceInfo.get("maintenanceStatus");
            String maintainer = (String) maintenanceInfo.get("maintainer");
            
            log.info("处理设备维护事件: deviceId={}, type={}, status={}, maintainer={}", 
                    deviceId, maintenanceType, maintenanceStatus, maintainer);
            
            String messageContent = String.format("设备 %s (%s) 的 %s 维护%s。维护人员：%s", 
                    deviceName, deviceId, maintenanceType, 
                    "STARTED".equals(maintenanceStatus) ? "已开始" : 
                    "COMPLETED".equals(maintenanceStatus) ? "已完成" : "状态变更",
                    maintainer);
            
            // 创建设备维护通知消息
            WebSocketMessage maintenanceMessage = WebSocketMessage.builder()
                .messageId(MessageUtils.generateMessageId())
                .type(MessageType.DEVICE_STATUS)
                .title("设备维护通知")
                .content(messageContent)
                .priority(Priority.NORMAL)
                .organizationId(event.getOrganizationId())
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                    "deviceId", deviceId,
                    "deviceName", deviceName,
                    "maintenanceType", maintenanceType,
                    "maintenanceStatus", maintenanceStatus,
                    "maintainer", maintainer,
                    "eventType", "DEVICE_MAINTENANCE",
                    "maintenanceTime", LocalDateTime.now().toString()
                ))
                .requireAck(false)
                .build();
            
            // 推送给组织内用户
            int sentCount = webSocketConnectionManager.broadcastToOrganization(
                event.getOrganizationId(), maintenanceMessage);
            
            log.info("设备维护通知已发送: deviceId={}, type={}, sentCount={}", 
                    deviceId, maintenanceType, sentCount);
            
        } catch (Exception e) {
            log.error("处理设备维护事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 处理未知设备事件
     * 
     * 对于未识别的设备事件类型，进行通用处理。
     * 
     * @param event 未知设备事件
     */
    private void handleUnknownDeviceEvent(MessageEvent event) {
        try {
            log.warn("处理未知设备事件: eventType={}, messageId={}", 
                    event.getEventType(), event.getMessageId());
            
            // 创建通用设备事件通知
            WebSocketMessage unknownEventMessage = WebSocketMessage.builder()
                .messageId(MessageUtils.generateMessageId())
                .type(MessageType.DEVICE_STATUS)
                .title("设备事件通知")
                .content(String.format("收到设备事件：%s。详情：%s", 
                        event.getEventType(), 
                        event.getContent() != null ? event.getContent() : "无详细信息"))
                .priority(Priority.LOW)
                .organizationId(event.getOrganizationId())
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                    "originalEventType", event.getEventType(),
                    "originalContent", event.getContent() != null ? event.getContent() : "",
                    "metadata", event.getMetadata() != null ? event.getMetadata().toString() : "",
                    "eventType", "UNKNOWN_DEVICE_EVENT"
                ))
                .requireAck(false)
                .build();
            
            // 推送给组织内用户
            int sentCount = webSocketConnectionManager.broadcastToOrganization(
                event.getOrganizationId(), unknownEventMessage);
            
            log.info("未知设备事件通知已发送: eventType={}, sentCount={}", 
                    event.getEventType(), sentCount);
            
        } catch (Exception e) {
            log.error("处理未知设备事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 处理设备事件处理错误
     * 
     * @param event 失败的事件
     * @param channel RabbitMQ通道
     * @param deliveryTag 消息标签
     * @param exception 异常信息
     */
    private void handleDeviceEventProcessingError(MessageEvent event, Channel channel, 
                                                 long deliveryTag, Exception exception) {
        try {
            // 增加重试次数
            event.incrementRetry();
            event.setErrorMessage(exception.getMessage());
            event.setProcessedTime(LocalDateTime.now());
            
            if (event.canRetry()) {
                log.warn("设备事件处理失败，消息重新入队: messageId={}, retryCount={}, error={}", 
                        event.getMessageId(), event.getRetryCount(), exception.getMessage());
                // 拒绝消息，重新入队
                channel.basicNack(deliveryTag, false, true);
            } else {
                log.error("设备事件处理失败，超过最大重试次数，消息进入死信队列: messageId={}, retryCount={}, error={}", 
                        event.getMessageId(), event.getRetryCount(), exception.getMessage());
                // 拒绝消息，不重新入队，进入死信队列
                channel.basicNack(deliveryTag, false, false);
            }
            
        } catch (IOException e) {
            log.error("处理设备事件错误失败: messageId={}, error={}", event.getMessageId(), e.getMessage());
        }
    }
    
    /**
     * 根据告警级别确定消息优先级
     * 
     * @param alertLevel 告警级别
     * @return 消息优先级
     */
    private Priority determineAlertPriority(String alertLevel) {
        if (alertLevel == null) {
            return Priority.NORMAL;
        }
        
        switch (alertLevel.toUpperCase()) {
            case "CRITICAL":
            case "URGENT":
                return Priority.URGENT;
            case "HIGH":
                return Priority.HIGH;
            case "LOW":
            case "INFO":
                return Priority.LOW;
            case "MEDIUM":
            case "WARNING":
            default:
                return Priority.NORMAL;
        }
    }
}