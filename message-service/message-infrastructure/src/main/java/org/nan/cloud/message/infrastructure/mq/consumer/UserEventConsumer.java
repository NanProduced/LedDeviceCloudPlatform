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
 * 用户事件消费者
 * 
 * 处理所有用户相关的事件消息，包括用户行为跟踪、权限变更、账户状态更新等。
 * 负责将用户事件转换为WebSocket消息推送给相关用户和管理员，确保用户状态变更的及时通知。
 * 
 * 主要处理的用户事件类型：
 * 1. USER_LOGIN - 用户登录事件
 * 2. USER_LOGOUT - 用户登出事件
 * 3. USER_PERMISSION_CHANGED - 用户权限变更事件
 * 4. USER_PROFILE_UPDATED - 用户资料更新事件
 * 5. USER_ACCOUNT_LOCKED - 用户账户锁定事件
 * 6. USER_ACCOUNT_UNLOCKED - 用户账户解锁事件
 * 7. USER_PASSWORD_CHANGED - 用户密码变更事件
 * 8. USER_ROLE_ASSIGNED - 用户角色分配事件
 * 
 * 处理策略：
 * - 安全相关事件（权限、密码、锁定）高优先级处理
 * - 用户行为事件用于审计和统计
 * - 权限变更需要通知管理员和用户本人
 * - 支持用户事件的追踪和分析
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {
    
    private final WebSocketConnectionManager webSocketConnectionManager;
    
    /**
     * 处理用户事件队列
     * 
     * 消费user.event.queue中的所有用户事件，根据事件类型和安全级别
     * 决定推送策略和目标用户。
     * 
     * @param event 用户事件
     * @param message RabbitMQ原始消息
     * @param channel RabbitMQ通道
     */
    @RabbitListener(
        queues = "user.event.queue",
        ackMode = "MANUAL",
        concurrency = "2-5"
    )
    public void handleUserEvent(MessageEvent event, Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        
        try {
            log.info("处理用户事件: messageId={}, eventType={}, userData={}", 
                    event.getMessageId(), event.getEventType(), event.getMetadata());
            
            // 根据用户事件类型进行不同处理
            switch (event.getEventType()) {
                case "USER_LOGIN":
                    handleUserLoginEvent(event);
                    break;
                case "USER_LOGOUT":
                    handleUserLogoutEvent(event);
                    break;
                case "USER_PERMISSION_CHANGED":
                    handleUserPermissionChangedEvent(event);
                    break;
                case "USER_PROFILE_UPDATED":
                    handleUserProfileUpdatedEvent(event);
                    break;
                case "USER_ACCOUNT_LOCKED":
                    handleUserAccountLockedEvent(event);
                    break;
                case "USER_ACCOUNT_UNLOCKED":
                    handleUserAccountUnlockedEvent(event);
                    break;
                case "USER_PASSWORD_CHANGED":
                    handleUserPasswordChangedEvent(event);
                    break;
                case "USER_ROLE_ASSIGNED":
                    handleUserRoleAssignedEvent(event);
                    break;
                default:
                    log.warn("未知的用户事件类型: eventType={}, messageId={}", 
                            event.getEventType(), event.getMessageId());
                    handleUnknownUserEvent(event);
            }
            
            // 手动确认消息
            channel.basicAck(deliveryTag, false);
            log.info("用户事件处理完成: messageId={}, eventType={}", 
                    event.getMessageId(), event.getEventType());
                    
        } catch (Exception e) {
            log.error("处理用户事件失败: messageId={}, eventType={}, error={}", 
                     event.getMessageId(), event.getEventType(), e.getMessage(), e);
            
            handleUserEventProcessingError(event, channel, deliveryTag, e);
        }
    }
    
    /**
     * 处理用户登录事件
     * 
     * 当用户成功登录时触发，记录登录行为并可选择通知管理员。
     * 
     * @param event 用户登录事件
     */
    private void handleUserLoginEvent(MessageEvent event) {
        try {
            Map<String, Object> loginInfo = event.getMetadata();
            String userId = (String) loginInfo.get("userId");
            String username = (String) loginInfo.get("username");
            String loginIp = (String) loginInfo.get("loginIp");
            String userAgent = (String) loginInfo.get("userAgent");
            String loginLocation = (String) loginInfo.get("loginLocation");
            
            log.info("处理用户登录事件: userId={}, username={}, ip={}, location={}", 
                    userId, username, loginIp, loginLocation);
            
            // 检查是否为异常登录（异地登录、新设备等）
            boolean isAbnormalLogin = checkAbnormalLogin(loginInfo);
            
            if (isAbnormalLogin) {
                // 创建异常登录告警消息
                WebSocketMessage loginAlert = WebSocketMessage.builder()
                    .messageId(MessageUtils.generateMessageId())
                    .type(MessageType.SECURITY_ALERT)
                    .title("异常登录检测")
                    .content(String.format("用户 %s 存在异常登录行为，登录IP：%s，登录地点：%s。请及时确认是否为本人操作。", 
                            username, loginIp, loginLocation))
                    .priority(Priority.HIGH)
                    .organizationId(event.getOrganizationId())
                    .timestamp(LocalDateTime.now())
                    .data(Map.of(
                        "userId", userId,
                        "username", username,
                        "loginIp", loginIp != null ? loginIp : "",
                        "loginLocation", loginLocation != null ? loginLocation : "",
                        "userAgent", userAgent != null ? userAgent : "",
                        "eventType", "ABNORMAL_LOGIN",
                        "loginTime", LocalDateTime.now().toString()
                    ))
                    .requireAck(true)
                    .build();
                
                // 通知用户本人
                webSocketConnectionManager.sendMessageToUser(userId, loginAlert);
                
                // 通知管理员
                int adminSentCount = webSocketConnectionManager.broadcastToOrganization(
                    event.getOrganizationId(), loginAlert);
                
                log.warn("异常登录告警已发送: userId={}, ip={}, adminSentCount={}", 
                        userId, loginIp, adminSentCount);
            } else {
                // 普通登录通知（可选，通常不发送以避免过多通知）
                log.info("用户正常登录: userId={}, ip={}", userId, loginIp);
            }
            
        } catch (Exception e) {
            log.error("处理用户登录事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 处理用户登出事件
     * 
     * 当用户登出时触发，记录登出行为。
     * 
     * @param event 用户登出事件
     */
    private void handleUserLogoutEvent(MessageEvent event) {
        try {
            Map<String, Object> logoutInfo = event.getMetadata();
            String userId = (String) logoutInfo.get("userId");
            String username = (String) logoutInfo.get("username");
            String logoutType = (String) logoutInfo.get("logoutType"); // MANUAL, AUTO, FORCED
            
            log.info("处理用户登出事件: userId={}, username={}, type={}", 
                    userId, username, logoutType);
            
            // 对于强制登出，需要通知用户
            if ("FORCED".equals(logoutType)) {
                WebSocketMessage logoutNotification = WebSocketMessage.builder()
                    .messageId(MessageUtils.generateMessageId())
                    .type(MessageType.SYSTEM_NOTIFICATION)
                    .title("账户被强制登出")
                    .content("您的账户已被管理员强制登出，请重新登录。如有疑问，请联系管理员。")
                    .priority(Priority.HIGH)
                    .organizationId(event.getOrganizationId())
                    .timestamp(LocalDateTime.now())
                    .data(Map.of(
                        "userId", userId,
                        "username", username,
                        "logoutType", logoutType,
                        "eventType", "FORCED_LOGOUT",
                        "logoutTime", LocalDateTime.now().toString()
                    ))
                    .requireAck(true)
                    .build();
                
                // 通知用户本人
                webSocketConnectionManager.sendMessageToUser(userId, logoutNotification);
                
                log.warn("强制登出通知已发送: userId={}", userId);
            }
            
        } catch (Exception e) {
            log.error("处理用户登出事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 处理用户权限变更事件
     * 
     * 当用户权限发生变更时触发，需要通知用户和管理员。
     * 
     * @param event 用户权限变更事件
     */
    private void handleUserPermissionChangedEvent(MessageEvent event) {
        try {
            Map<String, Object> permissionInfo = event.getMetadata();
            String userId = (String) permissionInfo.get("userId");
            String username = (String) permissionInfo.get("username");
            String changeType = (String) permissionInfo.get("changeType"); // GRANTED, REVOKED, MODIFIED
            String permissions = (String) permissionInfo.get("permissions");
            String operator = (String) permissionInfo.get("operator");
            String reason = (String) permissionInfo.get("reason");
            
            log.info("处理用户权限变更事件: userId={}, type={}, permissions={}, operator={}", 
                    userId, changeType, permissions, operator);
            
            // 创建权限变更通知消息
            WebSocketMessage permissionNotification = WebSocketMessage.builder()
                .messageId(MessageUtils.generateMessageId())
                .type(MessageType.SECURITY_NOTIFICATION)
                .title("用户权限变更")
                .content(String.format("用户 %s 的权限已%s，涉及权限：%s。操作人：%s。%s", 
                        username, 
                        "GRANTED".equals(changeType) ? "授予" : 
                        "REVOKED".equals(changeType) ? "撤销" : "修改",
                        permissions, operator,
                        reason != null ? "变更原因：" + reason : ""))
                .priority(Priority.HIGH) // 权限变更高优先级
                .organizationId(event.getOrganizationId())
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                    "userId", userId,
                    "username", username,
                    "changeType", changeType,
                    "permissions", permissions != null ? permissions : "",
                    "operator", operator != null ? operator : "",
                    "reason", reason != null ? reason : "",
                    "eventType", "PERMISSION_CHANGED",
                    "changedTime", LocalDateTime.now().toString()
                ))
                .requireAck(true)
                .build();
            
            // 通知用户本人
            webSocketConnectionManager.sendMessageToUser(userId, permissionNotification);
            
            // 通知管理员
            int adminSentCount = webSocketConnectionManager.broadcastToOrganization(
                event.getOrganizationId(), permissionNotification);
            
            log.info("权限变更通知已发送: userId={}, changeType={}, adminSentCount={}", 
                    userId, changeType, adminSentCount);
            
        } catch (Exception e) {
            log.error("处理用户权限变更事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 处理用户资料更新事件
     * 
     * 当用户更新个人资料时触发。
     * 
     * @param event 用户资料更新事件
     */
    private void handleUserProfileUpdatedEvent(MessageEvent event) {
        try {
            Map<String, Object> profileInfo = event.getMetadata();
            String userId = (String) profileInfo.get("userId");
            String username = (String) profileInfo.get("username");
            String updatedFields = (String) profileInfo.get("updatedFields");
            String updateSource = (String) profileInfo.get("updateSource"); // SELF, ADMIN
            
            log.info("处理用户资料更新事件: userId={}, fields={}, source={}", 
                    userId, updatedFields, updateSource);
            
            // 只对重要字段更新进行通知
            if (isImportantProfileUpdate(updatedFields)) {
                WebSocketMessage profileNotification = WebSocketMessage.builder()
                    .messageId(MessageUtils.generateMessageId())
                    .type(MessageType.SYSTEM_NOTIFICATION)
                    .title("用户资料更新")
                    .content(String.format("用户 %s 的资料已更新，更新字段：%s。%s", 
                            username, updatedFields,
                            "ADMIN".equals(updateSource) ? "此更新由管理员执行。" : ""))
                    .priority(Priority.NORMAL)
                    .organizationId(event.getOrganizationId())
                    .timestamp(LocalDateTime.now())
                    .data(Map.of(
                        "userId", userId,
                        "username", username,
                        "updatedFields", updatedFields,
                        "updateSource", updateSource != null ? updateSource : "",
                        "eventType", "PROFILE_UPDATED",
                        "updatedTime", LocalDateTime.now().toString()
                    ))
                    .requireAck(false)
                    .build();
                
                // 通知用户本人
                webSocketConnectionManager.sendMessageToUser(userId, profileNotification);
                
                log.info("用户资料更新通知已发送: userId={}, fields={}", userId, updatedFields);
            }
            
        } catch (Exception e) {
            log.error("处理用户资料更新事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 处理用户账户锁定事件
     * 
     * 当用户账户被锁定时触发，需要立即通知用户和管理员。
     * 
     * @param event 用户账户锁定事件
     */
    private void handleUserAccountLockedEvent(MessageEvent event) {
        try {
            Map<String, Object> lockInfo = event.getMetadata();
            String userId = (String) lockInfo.get("userId");
            String username = (String) lockInfo.get("username");
            String lockReason = (String) lockInfo.get("lockReason");
            String operator = (String) lockInfo.get("operator");
            String lockDuration = (String) lockInfo.get("lockDuration");
            
            log.warn("处理用户账户锁定事件: userId={}, reason={}, operator={}, duration={}", 
                    userId, lockReason, operator, lockDuration);
            
            // 创建账户锁定告警消息
            WebSocketMessage lockAlert = WebSocketMessage.builder()
                .messageId(MessageUtils.generateMessageId())
                .type(MessageType.SECURITY_ALERT)
                .title("账户已被锁定")
                .content(String.format("用户 %s 的账户已被锁定。锁定原因：%s。操作人：%s。%s", 
                        username, lockReason, operator,
                        lockDuration != null ? "锁定时长：" + lockDuration : ""))
                .priority(Priority.URGENT) // 账户锁定紧急优先级
                .organizationId(event.getOrganizationId())
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                    "userId", userId,
                    "username", username,
                    "lockReason", lockReason != null ? lockReason : "",
                    "operator", operator != null ? operator : "",
                    "lockDuration", lockDuration != null ? lockDuration : "",
                    "eventType", "ACCOUNT_LOCKED",
                    "lockedTime", LocalDateTime.now().toString()
                ))
                .requireAck(true)
                .build();
            
            // 通知用户本人
            webSocketConnectionManager.sendMessageToUser(userId, lockAlert);
            
            // 通知管理员
            int adminSentCount = webSocketConnectionManager.broadcastToOrganization(
                event.getOrganizationId(), lockAlert);
            
            log.warn("账户锁定告警已发送: userId={}, reason={}, adminSentCount={}", 
                    userId, lockReason, adminSentCount);
            
        } catch (Exception e) {
            log.error("处理用户账户锁定事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 处理用户账户解锁事件
     * 
     * 当用户账户被解锁时触发，通知用户账户恢复正常。
     * 
     * @param event 用户账户解锁事件
     */
    private void handleUserAccountUnlockedEvent(MessageEvent event) {
        try {
            Map<String, Object> unlockInfo = event.getMetadata();
            String userId = (String) unlockInfo.get("userId");
            String username = (String) unlockInfo.get("username");
            String unlockReason = (String) unlockInfo.get("unlockReason");
            String operator = (String) unlockInfo.get("operator");
            
            log.info("处理用户账户解锁事件: userId={}, reason={}, operator={}", 
                    userId, unlockReason, operator);
            
            // 创建账户解锁通知消息
            WebSocketMessage unlockNotification = WebSocketMessage.builder()
                .messageId(MessageUtils.generateMessageId())
                .type(MessageType.SYSTEM_NOTIFICATION)
                .title("账户已解锁")
                .content(String.format("用户 %s 的账户已解锁，可以正常使用。解锁原因：%s。操作人：%s。", 
                        username, unlockReason, operator))
                .priority(Priority.HIGH)
                .organizationId(event.getOrganizationId())
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                    "userId", userId,
                    "username", username,
                    "unlockReason", unlockReason != null ? unlockReason : "",
                    "operator", operator != null ? operator : "",
                    "eventType", "ACCOUNT_UNLOCKED",
                    "unlockedTime", LocalDateTime.now().toString()
                ))
                .requireAck(false)
                .build();
            
            // 通知用户本人
            webSocketConnectionManager.sendMessageToUser(userId, unlockNotification);
            
            // 通知管理员
            int adminSentCount = webSocketConnectionManager.broadcastToOrganization(
                event.getOrganizationId(), unlockNotification);
            
            log.info("账户解锁通知已发送: userId={}, adminSentCount={}", userId, adminSentCount);
            
        } catch (Exception e) {
            log.error("处理用户账户解锁事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 处理用户密码变更事件
     * 
     * 当用户密码被修改时触发，这是重要的安全事件。
     * 
     * @param event 用户密码变更事件
     */
    private void handleUserPasswordChangedEvent(MessageEvent event) {
        try {
            Map<String, Object> passwordInfo = event.getMetadata();
            String userId = (String) passwordInfo.get("userId");
            String username = (String) passwordInfo.get("username");
            String changeSource = (String) passwordInfo.get("changeSource"); // SELF, ADMIN, RESET
            String changeIp = (String) passwordInfo.get("changeIp");
            
            log.info("处理用户密码变更事件: userId={}, source={}, ip={}", 
                    userId, changeSource, changeIp);
            
            // 创建密码变更通知消息
            WebSocketMessage passwordNotification = WebSocketMessage.builder()
                .messageId(MessageUtils.generateMessageId())
                .type(MessageType.SECURITY_NOTIFICATION)
                .title("密码已变更")
                .content(String.format("用户 %s 的登录密码已变更。变更方式：%s，操作IP：%s。如非本人操作，请立即联系管理员。", 
                        username, 
                        "SELF".equals(changeSource) ? "用户自行修改" :
                        "ADMIN".equals(changeSource) ? "管理员重置" : "系统重置",
                        changeIp))
                .priority(Priority.HIGH) // 密码变更高优先级
                .organizationId(event.getOrganizationId())
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                    "userId", userId,
                    "username", username,
                    "changeSource", changeSource != null ? changeSource : "",
                    "changeIp", changeIp != null ? changeIp : "",
                    "eventType", "PASSWORD_CHANGED",
                    "changedTime", LocalDateTime.now().toString()
                ))
                .requireAck(true)
                .build();
            
            // 通知用户本人
            webSocketConnectionManager.sendMessageToUser(userId, passwordNotification);
            
            // 如果是管理员操作，也要通知管理员
            if ("ADMIN".equals(changeSource)) {
                int adminSentCount = webSocketConnectionManager.broadcastToOrganization(
                    event.getOrganizationId(), passwordNotification);
                log.info("密码变更通知已发送给管理员: userId={}, adminSentCount={}", userId, adminSentCount);
            }
            
            log.info("密码变更通知已发送: userId={}, source={}", userId, changeSource);
            
        } catch (Exception e) {
            log.error("处理用户密码变更事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 处理用户角色分配事件
     * 
     * 当用户被分配新角色时触发。
     * 
     * @param event 用户角色分配事件
     */
    private void handleUserRoleAssignedEvent(MessageEvent event) {
        try {
            Map<String, Object> roleInfo = event.getMetadata();
            String userId = (String) roleInfo.get("userId");
            String username = (String) roleInfo.get("username");
            String roleName = (String) roleInfo.get("roleName");
            String assignType = (String) roleInfo.get("assignType"); // ASSIGNED, REMOVED
            String operator = (String) roleInfo.get("operator");
            
            log.info("处理用户角色分配事件: userId={}, role={}, type={}, operator={}", 
                    userId, roleName, assignType, operator);
            
            // 创建角色分配通知消息
            WebSocketMessage roleNotification = WebSocketMessage.builder()
                .messageId(MessageUtils.generateMessageId())
                .type(MessageType.SYSTEM_NOTIFICATION)
                .title("用户角色变更")
                .content(String.format("用户 %s %s角色 %s。操作人：%s。", 
                        username,
                        "ASSIGNED".equals(assignType) ? "被分配" : "被移除",
                        roleName, operator))
                .priority(Priority.NORMAL)
                .organizationId(event.getOrganizationId())
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                    "userId", userId,
                    "username", username,
                    "roleName", roleName != null ? roleName : "",
                    "assignType", assignType != null ? assignType : "",
                    "operator", operator != null ? operator : "",
                    "eventType", "ROLE_ASSIGNED",
                    "assignedTime", LocalDateTime.now().toString()
                ))
                .requireAck(false)
                .build();
            
            // 通知用户本人
            webSocketConnectionManager.sendMessageToUser(userId, roleNotification);
            
            // 通知管理员
            int adminSentCount = webSocketConnectionManager.broadcastToOrganization(
                event.getOrganizationId(), roleNotification);
            
            log.info("角色分配通知已发送: userId={}, role={}, type={}, adminSentCount={}", 
                    userId, roleName, assignType, adminSentCount);
            
        } catch (Exception e) {
            log.error("处理用户角色分配事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 处理未知用户事件
     * 
     * 对于未识别的用户事件类型，进行通用处理。
     * 
     * @param event 未知用户事件
     */
    private void handleUnknownUserEvent(MessageEvent event) {
        try {
            log.warn("处理未知用户事件: eventType={}, messageId={}", 
                    event.getEventType(), event.getMessageId());
            
            // 创建通用用户事件通知
            WebSocketMessage unknownEventMessage = WebSocketMessage.builder()
                .messageId(MessageUtils.generateMessageId())
                .type(MessageType.SYSTEM_NOTIFICATION)
                .title("用户事件通知")
                .content(String.format("收到用户事件：%s。详情：%s", 
                        event.getEventType(), 
                        event.getContent() != null ? event.getContent() : "无详细信息"))
                .priority(Priority.LOW)
                .organizationId(event.getOrganizationId())
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                    "originalEventType", event.getEventType(),
                    "originalContent", event.getContent() != null ? event.getContent() : "",
                    "metadata", event.getMetadata() != null ? event.getMetadata().toString() : "",
                    "eventType", "UNKNOWN_USER_EVENT"
                ))
                .requireAck(false)
                .build();
            
            // 推送给组织内用户
            int sentCount = webSocketConnectionManager.broadcastToOrganization(
                event.getOrganizationId(), unknownEventMessage);
            
            log.info("未知用户事件通知已发送: eventType={}, sentCount={}", 
                    event.getEventType(), sentCount);
            
        } catch (Exception e) {
            log.error("处理未知用户事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 处理用户事件处理错误
     * 
     * @param event 失败的事件
     * @param channel RabbitMQ通道
     * @param deliveryTag 消息标签
     * @param exception 异常信息
     */
    private void handleUserEventProcessingError(MessageEvent event, Channel channel, 
                                              long deliveryTag, Exception exception) {
        try {
            // 增加重试次数
            event.incrementRetry();
            event.setErrorMessage(exception.getMessage());
            event.setProcessedTime(LocalDateTime.now());
            
            if (event.canRetry()) {
                log.warn("用户事件处理失败，消息重新入队: messageId={}, retryCount={}, error={}", 
                        event.getMessageId(), event.getRetryCount(), exception.getMessage());
                // 拒绝消息，重新入队
                channel.basicNack(deliveryTag, false, true);
            } else {
                log.error("用户事件处理失败，超过最大重试次数，消息进入死信队列: messageId={}, retryCount={}, error={}", 
                        event.getMessageId(), event.getRetryCount(), exception.getMessage());
                // 拒绝消息，不重新入队，进入死信队列
                channel.basicNack(deliveryTag, false, false);
            }
            
        } catch (IOException e) {
            log.error("处理用户事件错误失败: messageId={}, error={}", event.getMessageId(), e.getMessage());
        }
    }
    
    /**
     * 检查是否为异常登录
     * 
     * @param loginInfo 登录信息
     * @return true表示异常登录
     */
    private boolean checkAbnormalLogin(Map<String, Object> loginInfo) {
        // TODO: 实现异常登录检测逻辑
        // 可以基于：异地登录、新设备、登录时间、登录频率等
        
        String loginLocation = (String) loginInfo.get("loginLocation");
        String userAgent = (String) loginInfo.get("userAgent");
        
        // 简单示例：如果登录地点包含"异地"或用户代理包含"Unknown"
        return (loginLocation != null && loginLocation.contains("异地")) ||
               (userAgent != null && userAgent.contains("Unknown"));
    }
    
    /**
     * 检查是否为重要的资料更新
     * 
     * @param updatedFields 更新的字段
     * @return true表示重要更新
     */
    private boolean isImportantProfileUpdate(String updatedFields) {
        if (updatedFields == null) {
            return false;
        }
        
        // 重要字段：邮箱、手机号、真实姓名、身份证号等
        return updatedFields.contains("email") ||
               updatedFields.contains("phone") ||
               updatedFields.contains("realName") ||
               updatedFields.contains("idCard") ||
               updatedFields.contains("bankAccount");
    }
}