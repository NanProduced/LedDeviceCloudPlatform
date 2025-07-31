package org.nan.cloud.message.infrastructure.websocket.processor.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.utils.JsonUtils;
import org.nan.cloud.message.api.enums.Priority;
import org.nan.cloud.message.infrastructure.websocket.dispatcher.DispatchResult;
import org.nan.cloud.message.infrastructure.websocket.dispatcher.StompMessageDispatcher;
import org.nan.cloud.message.infrastructure.websocket.processor.BusinessMessageProcessor;
import org.nan.cloud.message.infrastructure.websocket.stomp.enums.StompMessageTypes;
import org.nan.cloud.message.infrastructure.websocket.stomp.enums.StompTopic;
import org.nan.cloud.message.infrastructure.websocket.stomp.model.CommonStompMessage;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 状态消息处理器
 * 
 * 负责处理各种状态变更消息，包括：
 * 1. LED设备在线状态变更
 * 2. 设备运行状态变更（开启/关闭/故障等）
 * 3. 用户在线状态变更
 * 4. 系统服务状态变更
 * 5. 连接状态变更（WebSocket、TCP等）
 * 
 * 支持的消息类型：
 * - DEVICE_STATUS: 设备状态变更消息
 * - USER_STATUS: 用户状态变更消息
 * - SERVICE_STATUS: 服务状态变更消息
 * - CONNECTION_STATUS: 连接状态变更消息
 * 
 * 路由键模式：
 * - stomp.device.status.{orgId}.{deviceId}
 * - stomp.user.status.{orgId}.{userId}
 * - stomp.service.status.{serviceId}
 * - stomp.connection.status.{connectionType}.{resourceId}
 * 
 * 分发策略：
 * 1. 设备状态：广播给设备管理员和相关订阅者
 * 2. 用户状态：发送给同组织用户和管理员
 * 3. 服务状态：发送给系统管理员
 * 4. 连接状态：发送给相关业务方
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatusMessageProcessor implements BusinessMessageProcessor {
    
    private final StompMessageDispatcher stompDispatcher;
    
    @Override
    public String getSupportedMessageType() {
        return "STATUS";
    }
    
    @Override
    public boolean supports(String messageType, String routingKey) {
        if (messageType == null || routingKey == null) {
            return false;
        }
        
        // 支持状态相关的消息类型
        boolean typeSupported = "DEVICE_STATUS".equalsIgnoreCase(messageType) ||
                               "USER_STATUS".equalsIgnoreCase(messageType) ||
                               "SERVICE_STATUS".equalsIgnoreCase(messageType) ||
                               "CONNECTION_STATUS".equalsIgnoreCase(messageType) ||
                               "STATUS".equalsIgnoreCase(messageType);
        
        // 支持状态相关的路由键模式
        boolean routingSupported = routingKey.startsWith("stomp.device.status.") ||
                                 routingKey.startsWith("stomp.user.status.") ||
                                 routingKey.startsWith("stomp.service.status.") ||
                                 routingKey.startsWith("stomp.connection.status.") ||
                                 routingKey.contains(".status.");
        
        return typeSupported || routingSupported;
    }
    
    @Override
    public int getPriority() {
        return 20; // 中等优先级，状态变更需要及时通知但不如指令结果紧急
    }
    
    @Override
    public BusinessMessageProcessResult process(String messagePayload, String routingKey, 
                                              Map<String, Object> messageHeaders) {
        try {
            log.debug("开始处理状态消息 - 路由键: {}", routingKey);
            
            // 解析消息内容
            Map<String, Object> messageData = JsonUtils.fromJson(messagePayload, Map.class);
            
            // 根据路由键确定具体的处理策略
            if (routingKey.startsWith("stomp.device.status.")) {
                return processDeviceStatus(messageData, routingKey, messageHeaders);
            } else if (routingKey.startsWith("stomp.user.status.")) {
                return processUserStatus(messageData, routingKey, messageHeaders);
            } else if (routingKey.startsWith("stomp.service.status.")) {
                return processServiceStatus(messageData, routingKey, messageHeaders);
            } else if (routingKey.startsWith("stomp.connection.status.")) {
                return processConnectionStatus(messageData, routingKey, messageHeaders);
            } else {
                return processGenericStatus(messageData, routingKey, messageHeaders);
            }
            
        } catch (Exception e) {
            String errorMsg = String.format("状态消息处理异常 - 路由键: %s, 错误: %s", 
                    routingKey, e.getMessage());
            log.error("💥 {}", errorMsg, e);
            return BusinessMessageProcessResult.failure(null, errorMsg);
        }
    }
    
    /**
     * 处理设备状态变更消息
     * 路由键：stomp.device.status.{orgId}.{deviceId}
     */
    private BusinessMessageProcessResult processDeviceStatus(Map<String, Object> messageData, 
                                                           String routingKey, 
                                                           Map<String, Object> messageHeaders) {
        try {
            log.debug("处理设备状态变更 - 路由键: {}", routingKey);
            
            // 提取消息字段
            String deviceId = (String) messageData.get("deviceId");
            Long orgId = getLongValue(messageData, "orgId");
            String status = (String) messageData.get("status");
            String previousStatus = (String) messageData.get("previousStatus");
            String statusType = (String) messageData.get("statusType"); // ONLINE, RUNNING, HEALTH等
            Object statusData = messageData.get("statusData");
            Long timestamp = getLongValue(messageData, "timestamp");
            String reason = (String) messageData.get("reason");
            
            // 验证必要字段
            if (deviceId == null || status == null) {
                String errorMsg = "设备状态消息缺少必要字段: deviceId, status";
                log.warn("⚠️ {}", errorMsg);
                return BusinessMessageProcessResult.failure(null, errorMsg);
            }
            
            // 构建STOMP消息
            CommonStompMessage stompMessage = buildDeviceStatusMessage(
                    deviceId, orgId, status, previousStatus, statusType, statusData, timestamp, reason);
            
            // 分发消息
            DispatchResult dispatchResult = stompDispatcher.smartDispatch(stompMessage);
            
            log.info("✅ 设备状态消息处理完成 - 设备: {}, 状态: {} -> {}, 组织: {}", 
                    deviceId, previousStatus, status, orgId);
            
            return BusinessMessageProcessResult.success(stompMessage.getMessageId(), 
                    dispatchResult, stompMessage);
            
        } catch (Exception e) {
            String errorMsg = String.format("处理设备状态消息异常: %s", e.getMessage());
            log.error("💥 {}", errorMsg, e);
            return BusinessMessageProcessResult.failure(null, errorMsg);
        }
    }
    
    /**
     * 处理用户状态变更消息
     * 路由键：stomp.user.status.{orgId}.{userId}
     */
    private BusinessMessageProcessResult processUserStatus(Map<String, Object> messageData, 
                                                         String routingKey, 
                                                         Map<String, Object> messageHeaders) {
        try {
            log.debug("处理用户状态变更 - 路由键: {}", routingKey);
            
            // 提取消息字段
            Long userId = getLongValue(messageData, "userId");
            Long orgId = getLongValue(messageData, "orgId");
            String status = (String) messageData.get("status");
            String previousStatus = (String) messageData.get("previousStatus");
            String statusType = (String) messageData.get("statusType"); // ONLINE, ACTIVE, IDLE等
            Object statusData = messageData.get("statusData");
            Long timestamp = getLongValue(messageData, "timestamp");
            String sessionId = (String) messageData.get("sessionId");
            
            // 验证必要字段
            if (userId == null || status == null) {
                String errorMsg = "用户状态消息缺少必要字段: userId, status";
                log.warn("⚠️ {}", errorMsg);
                return BusinessMessageProcessResult.failure(null, errorMsg);
            }
            
            // 构建STOMP消息
            CommonStompMessage stompMessage = buildUserStatusMessage(
                    userId, orgId, status, previousStatus, statusType, statusData, timestamp, sessionId);
            
            // 分发消息
            DispatchResult dispatchResult = stompDispatcher.smartDispatch(stompMessage);
            
            log.info("✅ 用户状态消息处理完成 - 用户: {}, 状态: {} -> {}, 组织: {}", 
                    userId, previousStatus, status, orgId);
            
            return BusinessMessageProcessResult.success(stompMessage.getMessageId(), 
                    dispatchResult, stompMessage);
            
        } catch (Exception e) {
            String errorMsg = String.format("处理用户状态消息异常: %s", e.getMessage());
            log.error("💥 {}", errorMsg, e);
            return BusinessMessageProcessResult.failure(null, errorMsg);
        }
    }
    
    /**
     * 处理服务状态变更消息
     * 路由键：stomp.service.status.{serviceId}
     */
    private BusinessMessageProcessResult processServiceStatus(Map<String, Object> messageData, 
                                                            String routingKey, 
                                                            Map<String, Object> messageHeaders) {
        try {
            log.debug("处理服务状态变更 - 路由键: {}", routingKey);
            
            // 提取消息字段
            String serviceId = (String) messageData.get("serviceId");
            String serviceName = (String) messageData.get("serviceName");
            String status = (String) messageData.get("status");
            String previousStatus = (String) messageData.get("previousStatus");
            String instance = (String) messageData.get("instance");
            Object statusData = messageData.get("statusData");
            Long timestamp = getLongValue(messageData, "timestamp");
            String reason = (String) messageData.get("reason");
            
            // 验证必要字段
            if (serviceId == null || status == null) {
                String errorMsg = "服务状态消息缺少必要字段: serviceId, status";
                log.warn("⚠️ {}", errorMsg);
                return BusinessMessageProcessResult.failure(null, errorMsg);
            }
            
            // 构建STOMP消息
            CommonStompMessage stompMessage = buildServiceStatusMessage(
                    serviceId, serviceName, status, previousStatus, instance, statusData, timestamp, reason);
            
            // 分发消息
            DispatchResult dispatchResult = stompDispatcher.smartDispatch(stompMessage);
            
            log.info("✅ 服务状态消息处理完成 - 服务: {}, 状态: {} -> {}, 实例: {}", 
                    serviceId, previousStatus, status, instance);
            
            return BusinessMessageProcessResult.success(stompMessage.getMessageId(), 
                    dispatchResult, stompMessage);
            
        } catch (Exception e) {
            String errorMsg = String.format("处理服务状态消息异常: %s", e.getMessage());
            log.error("💥 {}", errorMsg, e);
            return BusinessMessageProcessResult.failure(null, errorMsg);
        }
    }
    
    /**
     * 处理连接状态变更消息
     * 路由键：stomp.connection.status.{connectionType}.{resourceId}
     */
    private BusinessMessageProcessResult processConnectionStatus(Map<String, Object> messageData, 
                                                               String routingKey, 
                                                               Map<String, Object> messageHeaders) {
        try {
            log.debug("处理连接状态变更 - 路由键: {}", routingKey);
            
            // 提取消息字段
            String connectionId = (String) messageData.get("connectionId");
            String connectionType = (String) messageData.get("connectionType"); // WEBSOCKET, TCP, HTTP等
            String resourceId = (String) messageData.get("resourceId");
            String status = (String) messageData.get("status");
            String previousStatus = (String) messageData.get("previousStatus");
            Object connectionData = messageData.get("connectionData");
            Long timestamp = getLongValue(messageData, "timestamp");
            String reason = (String) messageData.get("reason");
            
            // 验证必要字段
            if (connectionId == null || status == null) {
                String errorMsg = "连接状态消息缺少必要字段: connectionId, status";
                log.warn("⚠️ {}", errorMsg);
                return BusinessMessageProcessResult.failure(null, errorMsg);
            }
            
            // 构建STOMP消息
            CommonStompMessage stompMessage = buildConnectionStatusMessage(
                    connectionId, connectionType, resourceId, status, previousStatus, 
                    connectionData, timestamp, reason);
            
            // 分发消息
            DispatchResult dispatchResult = stompDispatcher.smartDispatch(stompMessage);
            
            log.info("✅ 连接状态消息处理完成 - 连接: {}, 类型: {}, 状态: {} -> {}", 
                    connectionId, connectionType, previousStatus, status);
            
            return BusinessMessageProcessResult.success(stompMessage.getMessageId(), 
                    dispatchResult, stompMessage);
            
        } catch (Exception e) {
            String errorMsg = String.format("处理连接状态消息异常: %s", e.getMessage());
            log.error("💥 {}", errorMsg, e);
            return BusinessMessageProcessResult.failure(null, errorMsg);
        }
    }
    
    /**
     * 处理通用状态消息
     */
    private BusinessMessageProcessResult processGenericStatus(Map<String, Object> messageData, 
                                                            String routingKey, 
                                                            Map<String, Object> messageHeaders) {
        try {
            log.debug("处理通用状态消息 - 路由键: {}", routingKey);
            
            // 构建通用状态消息
            String messageId = UUID.randomUUID().toString();
            
            CommonStompMessage stompMessage = CommonStompMessage.builder()
                    .messageId(messageId)
                    .timestamp(LocalDateTime.now())
                    .messageType(StompMessageTypes.TERMINAL_STATUS_CHANGE)
                    .subType_1("GENERIC")
                    .source(CommonStompMessage.Source.builder()
                            .serviceId("message-service")
                            .resourceType("STATUS")
                            .resourceId(routingKey)
                            .build())
                    .target(CommonStompMessage.Target.builder()
                            .targetType("TOPIC")
                            .destination(StompTopic.SYSTEM_TOPIC)
                            .build())
                    .payload(messageData)
                    .message("通用状态变更: " + routingKey)
                    .metadata(CommonStompMessage.Metadata.builder()
                            .priority(Priority.NORMAL)
                            .persistent(false)
                            .ttl(30000L)  // 30秒TTL
                            .build())
                    .build();
            
            // 分发消息
            DispatchResult dispatchResult = stompDispatcher.smartDispatch(stompMessage);
            
            log.info("✅ 通用状态消息处理完成 - 路由键: {}", routingKey);
            
            return BusinessMessageProcessResult.success(messageId, dispatchResult, stompMessage);
            
        } catch (Exception e) {
            String errorMsg = String.format("处理通用状态消息异常: %s", e.getMessage());
            log.error("💥 {}", errorMsg, e);
            return BusinessMessageProcessResult.failure(null, errorMsg);
        }
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 构建设备状态STOMP消息
     */
    private CommonStompMessage buildDeviceStatusMessage(String deviceId, Long orgId, String status, 
                                                       String previousStatus, String statusType, 
                                                       Object statusData, Long timestamp, String reason) {
        return CommonStompMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .messageType(StompMessageTypes.TERMINAL_STATUS_CHANGE)
                .subType_1(statusType != null ? statusType : "GENERIC")
                .subType_2(status)
                .source(CommonStompMessage.Source.builder()
                        .serviceId("terminal-service")
                        .resourceType("DEVICE")
                        .resourceId(deviceId)
                        .build())
                .target(CommonStompMessage.Target.builder()
                        .targetType("TERMINAL_AND_ORG")
                        .oid(orgId)
                        .destination(StompTopic.buildDeviceTopic(deviceId) +
                                  (orgId != null ? "," + StompTopic.buildOrgTopic(orgId.toString()) : ""))
                        .build())
                .payload(Map.of(
                        "deviceId", deviceId,
                        "status", status,
                        "previousStatus", previousStatus != null ? previousStatus : "",
                        "statusType", statusType != null ? statusType : "GENERIC",
                        "statusData", statusData != null ? statusData : Map.of(),
                        "timestamp", timestamp != null ? timestamp : System.currentTimeMillis(),
                        "reason", reason != null ? reason : "",
                        "changeTime", LocalDateTime.now()
                ))
                .message(String.format("设备%s状态变更: %s -> %s", deviceId, previousStatus, status))
                .metadata(CommonStompMessage.Metadata.builder()
                        .priority(isHighPriorityStatus(status) ? Priority.HIGH : Priority.NORMAL)
                        .persistent(false)  // 状态变更消息不需要持久化，时效性更重要
                        .ttl(30000L)  // 30秒TTL
                        .requireAck(false)
                        .build())
                .build();
    }
    
    /**
     * 构建用户状态STOMP消息
     */
    private CommonStompMessage buildUserStatusMessage(Long userId, Long orgId, String status, 
                                                     String previousStatus, String statusType, 
                                                     Object statusData, Long timestamp, String sessionId) {
        return CommonStompMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .messageType(StompMessageTypes.CONNECTION_STATUS)
                .subType_1(statusType != null ? statusType : "USER")
                .subType_2(status)
                .source(CommonStompMessage.Source.builder()
                        .serviceId("message-service")
                        .resourceType("USER")
                        .resourceId(userId.toString())
                        .executionId(sessionId)
                        .build())
                .target(CommonStompMessage.Target.builder()
                        .targetType("ORG")
                        .oid(orgId)
                        .destination(orgId != null ? StompTopic.buildOrgTopic(orgId.toString()) :
                                  StompTopic.USER_MESSAGES_QUEUE)
                        .build())
                .payload(Map.of(
                        "userId", userId,
                        "status", status,
                        "previousStatus", previousStatus != null ? previousStatus : "",
                        "statusType", statusType != null ? statusType : "USER",
                        "statusData", statusData != null ? statusData : Map.of(),
                        "timestamp", timestamp != null ? timestamp : System.currentTimeMillis(),
                        "sessionId", sessionId != null ? sessionId : "",
                        "changeTime", LocalDateTime.now()
                ))
                .message(String.format("用户%s状态变更: %s -> %s", userId, previousStatus, status))
                .metadata(CommonStompMessage.Metadata.builder()
                        .priority(Priority.LOW)  // 用户状态变更优先级较低
                        .persistent(false)
                        .ttl(60000L)  // 1分钟TTL
                        .build())
                .build();
    }
    
    /**
     * 构建服务状态STOMP消息
     */
    private CommonStompMessage buildServiceStatusMessage(String serviceId, String serviceName, 
                                                        String status, String previousStatus, 
                                                        String instance, Object statusData, 
                                                        Long timestamp, String reason) {
        return CommonStompMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .messageType(StompMessageTypes.SYSTEM_MESSAGE)
                .subType_1("SERVICE_STATUS")
                .subType_2(status)
                .source(CommonStompMessage.Source.builder()
                        .serviceId(serviceId)
                        .resourceType("SERVICE")
                        .resourceId(serviceId)
                        .executionId(instance)
                        .build())
                .target(CommonStompMessage.Target.builder()
                        .targetType("SYSTEM_ADMIN")
                        .destination(StompTopic.SYSTEM_TOPIC)
                        .build())
                .payload(Map.of(
                        "serviceId", serviceId,
                        "serviceName", serviceName != null ? serviceName : serviceId,
                        "status", status,
                        "previousStatus", previousStatus != null ? previousStatus : "",
                        "instance", instance != null ? instance : "",
                        "statusData", statusData != null ? statusData : Map.of(),
                        "timestamp", timestamp != null ? timestamp : System.currentTimeMillis(),
                        "reason", reason != null ? reason : "",
                        "changeTime", LocalDateTime.now()
                ))
                .message(String.format("服务%s状态变更: %s -> %s", serviceName != null ? serviceName : serviceId, 
                        previousStatus, status))
                .metadata(CommonStompMessage.Metadata.builder()
                        .priority(isServiceCritical(status) ? Priority.HIGH : Priority.NORMAL)
                        .persistent(true)  // 服务状态变更需要持久化
                        .ttl(300000L)  // 5分钟TTL
                        .requireAck(true)
                        .build())
                .build();
    }
    
    /**
     * 构建连接状态STOMP消息
     */
    private CommonStompMessage buildConnectionStatusMessage(String connectionId, String connectionType, 
                                                           String resourceId, String status, 
                                                           String previousStatus, Object connectionData, 
                                                           Long timestamp, String reason) {
        return CommonStompMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .messageType(StompMessageTypes.CONNECTION_STATUS)
                .subType_1(connectionType != null ? connectionType : "GENERIC")
                .subType_2(status)
                .source(CommonStompMessage.Source.builder()
                        .serviceId("message-service")
                        .resourceType("CONNECTION")
                        .resourceId(connectionId)
                        .build())
                .target(CommonStompMessage.Target.builder()
                        .targetType("SYSTEM")
                        .destination(StompTopic.SYSTEM_TOPIC)
                        .build())
                .payload(Map.of(
                        "connectionId", connectionId,
                        "connectionType", connectionType != null ? connectionType : "GENERIC",
                        "resourceId", resourceId != null ? resourceId : "",
                        "status", status,
                        "previousStatus", previousStatus != null ? previousStatus : "",
                        "connectionData", connectionData != null ? connectionData : Map.of(),
                        "timestamp", timestamp != null ? timestamp : System.currentTimeMillis(),
                        "reason", reason != null ? reason : "",
                        "changeTime", LocalDateTime.now()
                ))
                .message(String.format("连接%s状态变更: %s -> %s", connectionId, previousStatus, status))
                .metadata(CommonStompMessage.Metadata.builder()
                        .priority(Priority.LOW)
                        .persistent(false)
                        .ttl(30000L)  // 30秒TTL
                        .build())
                .build();
    }
    
    /**
     * 判断是否为高优先级状态
     */
    private boolean isHighPriorityStatus(String status) {
        if (status == null) return false;
        String upperStatus = status.toUpperCase();
        return upperStatus.contains("ERROR") || 
               upperStatus.contains("FAULT") || 
               upperStatus.contains("ALARM") ||
               upperStatus.contains("CRITICAL") ||
               upperStatus.equals("OFFLINE");
    }
    
    /**
     * 判断服务状态是否关键
     */
    private boolean isServiceCritical(String status) {
        if (status == null) return false;
        String upperStatus = status.toUpperCase();
        return upperStatus.equals("DOWN") || 
               upperStatus.equals("ERROR") || 
               upperStatus.equals("CRITICAL") ||
               upperStatus.contains("FAIL");
    }
    
    /**
     * 安全获取Long值
     */
    private Long getLongValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            log.warn("无法将{}转换为Long类型: {}", key, value);
            return null;
        }
    }
}