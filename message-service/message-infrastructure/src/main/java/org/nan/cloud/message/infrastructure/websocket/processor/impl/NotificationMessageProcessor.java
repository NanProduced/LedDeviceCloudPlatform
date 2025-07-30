package org.nan.cloud.message.infrastructure.websocket.processor.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.api.enums.Priority;
import org.nan.cloud.message.infrastructure.websocket.dispatcher.DispatchResult;
import org.nan.cloud.message.infrastructure.websocket.dispatcher.StompMessageDispatcher;
import org.nan.cloud.message.infrastructure.websocket.processor.BusinessMessageProcessor;
import org.nan.cloud.message.infrastructure.websocket.stomp.enums.StompMessageTypes;
import org.nan.cloud.message.infrastructure.websocket.stomp.enums.StompTopic;
import org.nan.cloud.message.infrastructure.websocket.stomp.model.CommonStompMessage;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 通知消息处理器
 * 
 * 负责处理各种通知消息，包括：
 * 1. 系统通知（系统公告、维护通知、紧急告警）
 * 2. 业务通知（操作结果、任务完成、审批消息）
 * 3. 用户通知（个人消息、提醒、私信）
 * 4. 组织通知（部门公告、组织消息、权限变更）
 * 
 * 支持的消息类型：
 * - SYSTEM_NOTIFICATION: 系统级通知消息
 * - BUSINESS_NOTIFICATION: 业务级通知消息
 * - USER_NOTIFICATION: 用户级通知消息
 * - ORG_NOTIFICATION: 组织级通知消息
 * 
 * 路由键模式：
 * - stomp.system.notification.{type}.{priority}
 * - stomp.business.notification.{businessType}.{orgId}
 * - stomp.user.notification.{userId}.{type}
 * - stomp.org.notification.{orgId}.{type}
 * 
 * 分发策略：
 * 1. 系统通知：全局广播或按角色分发
 * 2. 业务通知：发送给相关业务方和管理员
 * 3. 用户通知：发送给特定用户
 * 4. 组织通知：发送给组织内相关用户
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationMessageProcessor implements BusinessMessageProcessor {
    
    private final StompMessageDispatcher stompDispatcher;
    private final ObjectMapper objectMapper;
    
    @Override
    public String getSupportedMessageType() {
        return "NOTIFICATION";
    }
    
    @Override
    public boolean supports(String messageType, String routingKey) {
        if (messageType == null || routingKey == null) {
            return false;
        }
        
        // 支持通知相关的消息类型
        boolean typeSupported = "SYSTEM_NOTIFICATION".equalsIgnoreCase(messageType) ||
                               "BUSINESS_NOTIFICATION".equalsIgnoreCase(messageType) ||
                               "USER_NOTIFICATION".equalsIgnoreCase(messageType) ||
                               "ORG_NOTIFICATION".equalsIgnoreCase(messageType) ||
                               "NOTIFICATION".equalsIgnoreCase(messageType);
        
        // 支持通知相关的路由键模式
        boolean routingSupported = routingKey.startsWith("stomp.system.notification.") ||
                                 routingKey.startsWith("stomp.business.notification.") ||
                                 routingKey.startsWith("stomp.user.notification.") ||
                                 routingKey.startsWith("stomp.org.notification.") ||
                                 routingKey.contains(".notification.");
        
        return typeSupported || routingSupported;
    }
    
    @Override
    public int getPriority() {
        return 30; // 较低优先级，通知消息通常不如指令和状态紧急
    }
    
    @Override
    public BusinessMessageProcessResult process(String messagePayload, String routingKey, 
                                              Map<String, Object> messageHeaders) {
        try {
            log.debug("开始处理通知消息 - 路由键: {}", routingKey);
            
            // 解析消息内容
            Map<String, Object> messageData = objectMapper.readValue(messagePayload, Map.class);
            
            // 根据路由键确定具体的处理策略
            if (routingKey.startsWith("stomp.system.notification.")) {
                return processSystemNotification(messageData, routingKey, messageHeaders);
            } else if (routingKey.startsWith("stomp.business.notification.")) {
                return processBusinessNotification(messageData, routingKey, messageHeaders);
            } else if (routingKey.startsWith("stomp.user.notification.")) {
                return processUserNotification(messageData, routingKey, messageHeaders);
            } else if (routingKey.startsWith("stomp.org.notification.")) {
                return processOrgNotification(messageData, routingKey, messageHeaders);
            } else {
                return processGenericNotification(messageData, routingKey, messageHeaders);
            }
            
        } catch (Exception e) {
            String errorMsg = String.format("通知消息处理异常 - 路由键: %s, 错误: %s", 
                    routingKey, e.getMessage());
            log.error("💥 {}", errorMsg, e);
            return BusinessMessageProcessResult.failure(null, errorMsg);
        }
    }
    
    /**
     * 处理系统通知消息
     * 路由键：stomp.system.notification.{type}.{priority}
     */
    private BusinessMessageProcessResult processSystemNotification(Map<String, Object> messageData, 
                                                                 String routingKey, 
                                                                 Map<String, Object> messageHeaders) {
        try {
            log.debug("处理系统通知消息 - 路由键: {}", routingKey);
            
            // 提取消息字段
            String notificationType = (String) messageData.get("notificationType");
            String notificationId = (String) messageData.get("notificationId");
            String title = (String) messageData.get("title");
            String content = (String) messageData.get("content");
            String priority = (String) messageData.get("priority");
            String category = (String) messageData.get("category"); // ANNOUNCEMENT, MAINTENANCE, EMERGENCY
            Object notificationData = messageData.get("notificationData");
            Long expireTime = getLongValue(messageData, "expireTime");
            List<String> targetRoles = getStringList(messageData, "targetRoles");
            List<Long> targetOrgIds = getLongList(messageData, "targetOrgIds");
            
            // 验证必要字段
            if (title == null || content == null) {
                String errorMsg = "系统通知消息缺少必要字段: title, content";
                log.warn("⚠️ {}", errorMsg);
                return BusinessMessageProcessResult.failure(null, errorMsg);
            }
            
            // 构建STOMP消息
            CommonStompMessage stompMessage = buildSystemNotificationMessage(
                    notificationId, notificationType, title, content, priority, category,
                    notificationData, expireTime, targetRoles, targetOrgIds);
            
            // 分发消息
            DispatchResult dispatchResult = stompDispatcher.smartDispatch(stompMessage);
            
            log.info("✅ 系统通知消息处理完成 - 类型: {}, 标题: {}, 优先级: {}", 
                    notificationType, title, priority);
            
            return BusinessMessageProcessResult.success(stompMessage.getMessageId(), 
                    dispatchResult, stompMessage);
            
        } catch (Exception e) {
            String errorMsg = String.format("处理系统通知消息异常: %s", e.getMessage());
            log.error("💥 {}", errorMsg, e);
            return BusinessMessageProcessResult.failure(null, errorMsg);
        }
    }
    
    /**
     * 处理业务通知消息
     * 路由键：stomp.business.notification.{businessType}.{orgId}
     */
    private BusinessMessageProcessResult processBusinessNotification(Map<String, Object> messageData, 
                                                                   String routingKey, 
                                                                   Map<String, Object> messageHeaders) {
        try {
            log.debug("处理业务通知消息 - 路由键: {}", routingKey);
            
            // 提取消息字段
            String businessType = (String) messageData.get("businessType");
            String businessId = (String) messageData.get("businessId");
            Long orgId = getLongValue(messageData, "orgId");
            List<Long> targetUserIds = getLongList(messageData, "targetUserIds");
            String title = (String) messageData.get("title");
            String content = (String) messageData.get("content");
            String actionType = (String) messageData.get("actionType"); // CREATE, UPDATE, DELETE, APPROVE等
            String actionUrl = (String) messageData.get("actionUrl");
            Object businessData = messageData.get("businessData");
            Long createdBy = getLongValue(messageData, "createdBy");
            
            // 验证必要字段
            if (businessType == null || title == null || content == null) {
                String errorMsg = "业务通知消息缺少必要字段: businessType, title, content";
                log.warn("⚠️ {}", errorMsg);
                return BusinessMessageProcessResult.failure(null, errorMsg);
            }
            
            // 构建STOMP消息
            CommonStompMessage stompMessage = buildBusinessNotificationMessage(
                    businessType, businessId, orgId, targetUserIds, title, content,
                    actionType, actionUrl, businessData, createdBy);
            
            // 分发消息
            DispatchResult dispatchResult = stompDispatcher.smartDispatch(stompMessage);
            
            log.info("✅ 业务通知消息处理完成 - 业务类型: {}, 组织: {}, 目标用户数: {}", 
                    businessType, orgId, targetUserIds != null ? targetUserIds.size() : 0);
            
            return BusinessMessageProcessResult.success(stompMessage.getMessageId(), 
                    dispatchResult, stompMessage);
            
        } catch (Exception e) {
            String errorMsg = String.format("处理业务通知消息异常: %s", e.getMessage());
            log.error("💥 {}", errorMsg, e);
            return BusinessMessageProcessResult.failure(null, errorMsg);
        }
    }
    
    /**
     * 处理用户通知消息
     * 路由键：stomp.user.notification.{userId}.{type}
     */
    private BusinessMessageProcessResult processUserNotification(Map<String, Object> messageData, 
                                                               String routingKey, 
                                                               Map<String, Object> messageHeaders) {
        try {
            log.debug("处理用户通知消息 - 路由键: {}", routingKey);
            
            // 提取消息字段
            Long targetUserId = getLongValue(messageData, "targetUserId");
            Long fromUserId = getLongValue(messageData, "fromUserId");
            String notificationType = (String) messageData.get("notificationType");
            String title = (String) messageData.get("title");
            String content = (String) messageData.get("content");
            String messageType = (String) messageData.get("messageType"); // INFO, WARNING, ERROR, SUCCESS
            String actionUrl = (String) messageData.get("actionUrl");
            Object userData = messageData.get("userData");
            Boolean requireAck = getBooleanValue(messageData, "requireAck");
            
            // 验证必要字段
            if (targetUserId == null || title == null || content == null) {
                String errorMsg = "用户通知消息缺少必要字段: targetUserId, title, content";
                log.warn("⚠️ {}", errorMsg);
                return BusinessMessageProcessResult.failure(null, errorMsg);
            }
            
            // 构建STOMP消息
            CommonStompMessage stompMessage = buildUserNotificationMessage(
                    targetUserId, fromUserId, notificationType, title, content,
                    messageType, actionUrl, userData, requireAck);
            
            // 分发消息
            DispatchResult dispatchResult = stompDispatcher.smartDispatch(stompMessage);
            
            log.info("✅ 用户通知消息处理完成 - 目标用户: {}, 来源用户: {}, 类型: {}", 
                    targetUserId, fromUserId, notificationType);
            
            return BusinessMessageProcessResult.success(stompMessage.getMessageId(), 
                    dispatchResult, stompMessage);
            
        } catch (Exception e) {
            String errorMsg = String.format("处理用户通知消息异常: %s", e.getMessage());
            log.error("💥 {}", errorMsg, e);
            return BusinessMessageProcessResult.failure(null, errorMsg);
        }
    }
    
    /**
     * 处理组织通知消息
     * 路由键：stomp.org.notification.{orgId}.{type}
     */
    private BusinessMessageProcessResult processOrgNotification(Map<String, Object> messageData, 
                                                              String routingKey, 
                                                              Map<String, Object> messageHeaders) {
        try {
            log.debug("处理组织通知消息 - 路由键: {}", routingKey);
            
            // 提取消息字段
            Long orgId = getLongValue(messageData, "orgId");
            String notificationType = (String) messageData.get("notificationType");
            String title = (String) messageData.get("title");
            String content = (String) messageData.get("content");
            String priority = (String) messageData.get("priority");
            List<Long> targetUserIds = getLongList(messageData, "targetUserIds");
            Long publishedBy = getLongValue(messageData, "publishedBy");
            Long expireTime = getLongValue(messageData, "expireTime");
            
            // 验证必要字段
            if (orgId == null || title == null || content == null) {
                String errorMsg = "组织通知消息缺少必要字段: orgId, title, content";
                log.warn("⚠️ {}", errorMsg);
                return BusinessMessageProcessResult.failure(null, errorMsg);
            }
            
            // 构建STOMP消息
            CommonStompMessage stompMessage = buildOrgNotificationMessage(
                    orgId, notificationType, title, content, priority, targetUserIds, publishedBy, expireTime);
            
            // 分发消息
            DispatchResult dispatchResult = stompDispatcher.smartDispatch(stompMessage);
            
            log.info("✅ 组织通知消息处理完成 - 组织: {}, 类型: {}, 目标用户数: {}", 
                    orgId, notificationType, targetUserIds != null ? targetUserIds.size() : 0);
            
            return BusinessMessageProcessResult.success(stompMessage.getMessageId(), 
                    dispatchResult, stompMessage);
            
        } catch (Exception e) {
            String errorMsg = String.format("处理组织通知消息异常: %s", e.getMessage());
            log.error("💥 {}", errorMsg, e);
            return BusinessMessageProcessResult.failure(null, errorMsg);
        }
    }
    
    /**
     * 处理通用通知消息
     */
    private BusinessMessageProcessResult processGenericNotification(Map<String, Object> messageData, 
                                                                  String routingKey, 
                                                                  Map<String, Object> messageHeaders) {
        try {
            log.debug("处理通用通知消息 - 路由键: {}", routingKey);
            
            // 构建通用通知消息
            String messageId = UUID.randomUUID().toString();
            String title = getStringValue(messageData, "title", "通用通知");
            String content = getStringValue(messageData, "content", "");
            
            CommonStompMessage stompMessage = CommonStompMessage.builder()
                    .messageId(messageId)
                    .timestamp(LocalDateTime.now())
                    .messageType(StompMessageTypes.NOTIFICATION)
                    .subType_1("GENERIC")
                    .source(CommonStompMessage.Source.builder()
                            .serviceId("message-service")
                            .resourceType("NOTIFICATION")
                            .resourceId(routingKey)
                            .build())
                    .target(CommonStompMessage.Target.builder()
                            .targetType("TOPIC")
                            .topicPath(StompTopic.GLOBAL_SYSTEM_ANNOUNCEMENT_TOPIC)
                            .build())
                    .payload(messageData)
                    .message(title + ": " + content)
                    .metadata(CommonStompMessage.Metadata.builder()
                            .priority(Priority.NORMAL)
                            .persistent(false)
                            .ttl(1800000L)  // 30分钟TTL
                            .build())
                    .build();
            
            // 分发消息
            DispatchResult dispatchResult = stompDispatcher.smartDispatch(stompMessage);
            
            log.info("✅ 通用通知消息处理完成 - 路由键: {}", routingKey);
            
            return BusinessMessageProcessResult.success(messageId, dispatchResult, stompMessage);
            
        } catch (Exception e) {
            String errorMsg = String.format("处理通用通知消息异常: %s", e.getMessage());
            log.error("💥 {}", errorMsg, e);
            return BusinessMessageProcessResult.failure(null, errorMsg);
        }
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 构建系统通知STOMP消息
     */
    private CommonStompMessage buildSystemNotificationMessage(String notificationId, String notificationType, 
                                                             String title, String content, String priority, 
                                                             String category, Object notificationData, 
                                                             Long expireTime, List<String> targetRoles, 
                                                             List<Long> targetOrgIds) {
        // 根据类别选择主题
        String topicPath = determineSystemNotificationTopic(category);
        
        return CommonStompMessage.builder()
                .messageId(notificationId != null ? notificationId : UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .messageType(StompMessageTypes.SYSTEM_MESSAGE)
                .subType_1(category != null ? category : notificationType)
                .subType_2(priority != null ? priority : "NORMAL")
                .source(CommonStompMessage.Source.builder()
                        .serviceId("core-service")
                        .resourceType("SYSTEM_NOTIFICATION")
                        .resourceId(notificationId)
                        .build())
                .target(CommonStompMessage.Target.builder()
                        .targetType(targetOrgIds != null && !targetOrgIds.isEmpty() ? "MULTI_ORG" : "GLOBAL")
                        .oid(targetOrgIds != null && targetOrgIds.size() == 1 ? targetOrgIds.get(0) : null)
                        .topicPath(topicPath)
                        .build())
                .payload(Map.of(
                ))
                .message(title + ": " + content)
                .metadata(CommonStompMessage.Metadata.builder()
                        .priority(parsePriority(priority))
                        .persistent(true)
                        .ttl(expireTime != null ? expireTime : 86400000L)  // 默认24小时TTL
                        .requireAck("HIGH".equalsIgnoreCase(priority) || "CRITICAL".equalsIgnoreCase(priority))
                        .build())
                .build();
    }
    
    /**
     * 构建业务通知STOMP消息
     */
    private CommonStompMessage buildBusinessNotificationMessage(String businessType, String businessId, 
                                                               Long orgId, List<Long> targetUserIds, 
                                                               String title, String content, String actionType, 
                                                               String actionUrl, Object businessData, Long createdBy) {
        return CommonStompMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .messageType(StompMessageTypes.NOTIFICATION)
                .subType_1("BUSINESS")
                .subType_2(businessType)
                .source(CommonStompMessage.Source.builder()
                        .serviceId("core-service")
                        .resourceType(businessType)
                        .resourceId(businessId)
                        .executionId(createdBy != null ? createdBy.toString() : null)
                        .build())
                .target(CommonStompMessage.Target.builder()
                        .targetType(targetUserIds != null && !targetUserIds.isEmpty() ? "USERS" : "ORG")
                        .uids(targetUserIds)
                        .oid(orgId)
                        .topicPath(targetUserIds != null && !targetUserIds.isEmpty() ? 
                                  buildUserNotificationTopics(targetUserIds) : 
                                  (orgId != null ? StompTopic.buildOrgAnnouncementTopic(orgId.toString()) : 
                                   StompTopic.GLOBAL_SYSTEM_ANNOUNCEMENT_TOPIC))
                        .build())
                .payload(Map.of(
                        "businessType", businessType,
                        "businessId", businessId != null ? businessId : "",
                        "title", title,
                        "content", content,
                        "actionType", actionType != null ? actionType : "",
                        "actionUrl", actionUrl != null ? actionUrl : "",
                        "businessData", businessData != null ? businessData : Map.of(),
                        "createdBy", createdBy != null ? createdBy : 0L,
                        "timestamp", LocalDateTime.now()
                ))
                .message(title + ": " + content)
                .metadata(CommonStompMessage.Metadata.builder()
                        .priority(Priority.NORMAL)
                        .persistent(true)
                        .ttl(86400000L)  // 24小时TTL
                        .requireAck(false)
                        .build())
                .build();
    }
    
    /**
     * 构建用户通知STOMP消息
     */
    private CommonStompMessage buildUserNotificationMessage(Long targetUserId, Long fromUserId, 
                                                           String notificationType, String title, String content, 
                                                           String messageType, String actionUrl, Object userData, 
                                                           Boolean requireAck) {
        return CommonStompMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .messageType(StompMessageTypes.NOTIFICATION)
                .subType_1("USER")
                .subType_2(messageType != null ? messageType : notificationType)
                .source(CommonStompMessage.Source.builder()
                        .serviceId("message-service")
                        .resourceType("USER_NOTIFICATION")
                        .resourceId(targetUserId.toString())
                        .executionId(fromUserId != null ? fromUserId.toString() : null)
                        .build())
                .target(CommonStompMessage.Target.builder()
                        .targetType("USER")
                        .uids(List.of(targetUserId))
                        .topicPath(StompTopic.buildUserNotificationTopic(targetUserId.toString()))
                        .build())
                .payload(Map.of(
                        "targetUserId", targetUserId,
                        "fromUserId", fromUserId != null ? fromUserId : 0L,
                        "notificationType", notificationType != null ? notificationType : "",
                        "title", title,
                        "content", content,
                        "messageType", messageType != null ? messageType : "INFO",
                        "actionUrl", actionUrl != null ? actionUrl : "",
                        "userData", userData != null ? userData : Map.of(),
                        "timestamp", LocalDateTime.now()
                ))
                .message(title + ": " + content)
                .metadata(CommonStompMessage.Metadata.builder()
                        .priority("ERROR".equalsIgnoreCase(messageType) || "WARNING".equalsIgnoreCase(messageType) ? 
                                 Priority.HIGH : Priority.NORMAL)
                        .persistent(true)
                        .ttl(259200000L)  // 3天TTL
                        .requireAck(requireAck != null ? requireAck : false)
                        .build())
                .build();
    }
    
    /**
     * 构建组织通知STOMP消息
     */
    private CommonStompMessage buildOrgNotificationMessage(Long orgId, String notificationType, 
                                                          String title, String content, String priority, 
                                                          List<Long> targetUserIds, Long publishedBy, Long expireTime) {
        return CommonStompMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .messageType(StompMessageTypes.ORG_MESSAGE)
                .subType_1(notificationType != null ? notificationType : "GENERAL")
                .subType_2(priority != null ? priority : "NORMAL")
                .source(CommonStompMessage.Source.builder()
                        .serviceId("core-service")
                        .resourceType("ORG_NOTIFICATION")
                        .resourceId(orgId.toString())
                        .executionId(publishedBy != null ? publishedBy.toString() : null)
                        .build())
                .target(CommonStompMessage.Target.builder()
                        .targetType(targetUserIds != null && !targetUserIds.isEmpty() ? "USERS" : "ORG")
                        .uids(targetUserIds)
                        .oid(orgId)
                        .topicPath(targetUserIds != null && !targetUserIds.isEmpty() ? 
                                  buildUserNotificationTopics(targetUserIds) : 
                                  StompTopic.buildOrgAnnouncementTopic(orgId.toString()))
                        .build())
                .payload(null)
                .message(title + ": " + content)
                .metadata(CommonStompMessage.Metadata.builder()
                        .priority(parsePriority(priority))
                        .persistent(true)
                        .ttl(expireTime != null ? expireTime : 604800000L)  // 默认7天TTL
                        .requireAck("HIGH".equalsIgnoreCase(priority))
                        .build())
                .build();
    }
    
    /**
     * 根据系统通知类别确定主题路径
     */
    private String determineSystemNotificationTopic(String category) {
        if (category == null) {
            return StompTopic.GLOBAL_SYSTEM_ANNOUNCEMENT_TOPIC;
        }
        
        switch (category.toUpperCase()) {
            case "MAINTENANCE":
                return StompTopic.GLOBAL_MAINTENANCE_TOPIC;
            case "EMERGENCY":
            case "CRITICAL":
                return StompTopic.GLOBAL_EMERGENCY_TOPIC;
            case "ANNOUNCEMENT":
            default:
                return StompTopic.GLOBAL_SYSTEM_ANNOUNCEMENT_TOPIC;
        }
    }
    
    /**
     * 构建用户通知主题路径列表
     */
    private String buildUserNotificationTopics(List<Long> userIds) {
        return userIds.stream()
                .map(userId -> StompTopic.buildUserNotificationTopic(userId.toString()))
                .collect(Collectors.joining(","));
    }
    
    /**
     * 解析优先级
     */
    private Priority parsePriority(String priority) {
        if (priority == null) {
            return Priority.NORMAL;
        }
        
        try {
            return Priority.valueOf(priority.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("无法解析优先级: {}, 使用默认优先级", priority);
            return Priority.NORMAL;
        }
    }
    
    // 数据提取工具方法
    
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
    
    private Boolean getBooleanValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.valueOf(value.toString());
    }
    
    private String getStringValue(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    @SuppressWarnings("unchecked")
    private List<String> getStringList(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return list.stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private List<Long> getLongList(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return list.stream()
                    .map(item -> {
                        if (item instanceof Number) {
                            return ((Number) item).longValue();
                        }
                        try {
                            return Long.valueOf(item.toString());
                        } catch (NumberFormatException e) {
                            log.warn("无法将列表项转换为Long类型: {}", item);
                            return null;
                        }
                    })
                    .filter(item -> item != null)
                    .collect(Collectors.toList());
        }
        return null;
    }
}