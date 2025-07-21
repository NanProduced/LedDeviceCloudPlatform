package org.nan.cloud.message.event.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.api.dto.response.UserOnlinePushResult;
import org.nan.cloud.message.service.UserOnlinePushService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 用户上线事件消费者
 * 
 * 监听用户登录/上线事件，自动触发未读消息和未查看任务结果的推送。
 * 这是LED设备云平台消息中心与用户认证系统的重要集成点。
 * 
 * 处理的事件类型：
 * - 用户登录事件：首次登录或重新登录
 * - 用户上线事件：从离线状态恢复在线
 * - 会话建立事件：WebSocket连接建立
 * - 设备登录事件：移动设备或新设备登录
 * 
 * 推送策略：
 * - 用户首次登录：推送所有未读消息和未查看任务
 * - 用户重连：仅推送高优先级消息和最新任务结果
 * - 会话恢复：推送会话相关的消息
 * - 设备切换：推送设备相关的消息和任务
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "message.push.enable", havingValue = "true", matchIfMissing = true)
public class UserOnlineEventConsumer {
    
    private final UserOnlinePushService userOnlinePushService;
    
    /**
     * 处理用户登录事件
     * 
     * 用户通过认证系统登录后，自动推送所有未读消息和未查看任务结果。
     * 这是最全面的推送场景，确保用户能获取到所有重要信息。
     * 
     * @param event 用户登录事件
     */
    @EventListener
    @Async("messageTaskExecutor")
    public void handleUserLoginEvent(UserLoginEvent event) {
        try {
            log.info("处理用户登录事件: userId={}, sessionId={}, organizationId={}, loginTime={}", 
                    event.getUserId(), event.getSessionId(), event.getOrganizationId(), event.getLoginTime());
            
            // 判断推送策略
            if (event.isFirstLogin()) {
                // 首次登录，执行完整推送
                log.debug("用户首次登录，执行完整推送: userId={}", event.getUserId());
                handleFullPush(event);
            } else {
                // 重复登录，执行增量推送
                log.debug("用户重复登录，执行增量推送: userId={}", event.getUserId());
                handleIncrementalPush(event);
            }
            
        } catch (Exception e) {
            log.error("处理用户登录事件失败: userId={}, error={}", 
                     event.getUserId(), e.getMessage(), e);
        }
    }
    
    /**
     * 处理用户上线事件
     * 
     * 用户从离线状态恢复在线时触发，通常是网络恢复或应用重启后。
     * 推送策略相对保守，主要关注高优先级消息。
     * 
     * @param event 用户上线事件
     */
    @EventListener
    @Async("messageTaskExecutor")
    public void handleUserOnlineEvent(UserOnlineEvent event) {
        try {
            log.info("处理用户上线事件: userId={}, sessionId={}, organizationId={}, onlineTime={}", 
                    event.getUserId(), event.getSessionId(), event.getOrganizationId(), event.getOnlineTime());
            
            // 检查离线时长，决定推送策略
            long offlineDurationMs = event.getOfflineDurationMs();
            if (offlineDurationMs > 3600000) { // 离线超过1小时
                log.debug("用户长时间离线，执行完整推送: userId={}, offlineDuration={}ms", 
                         event.getUserId(), offlineDurationMs);
                handleFullPush(event);
            } else {
                log.debug("用户短暂离线，执行快速推送: userId={}, offlineDuration={}ms", 
                         event.getUserId(), offlineDurationMs);
                handleQuickPush(event);
            }
            
        } catch (Exception e) {
            log.error("处理用户上线事件失败: userId={}, error={}", 
                     event.getUserId(), e.getMessage(), e);
        }
    }
    
    /**
     * 处理WebSocket连接建立事件
     * 
     * 用户建立WebSocket连接时触发，支持实时消息推送。
     * 主要推送实时性要求高的消息，如设备告警、系统通知等。
     * 
     * @param event WebSocket连接事件
     */
    @EventListener
    @Async("messageTaskExecutor")
    public void handleWebSocketConnectEvent(WebSocketConnectEvent event) {
        try {
            log.info("处理WebSocket连接事件: userId={}, sessionId={}, organizationId={}", 
                    event.getUserId(), event.getSessionId(), event.getOrganizationId());
            
            // WebSocket连接建立，推送实时消息
            handleRealtimePush(event);
            
        } catch (Exception e) {
            log.error("处理WebSocket连接事件失败: userId={}, sessionId={}, error={}", 
                     event.getUserId(), event.getSessionId(), e.getMessage(), e);
        }
    }
    
    /**
     * 处理设备登录事件
     * 
     * 用户在新设备上登录或切换设备时触发。
     * 需要推送与设备相关的消息和任务结果。
     * 
     * @param event 设备登录事件
     */
    @EventListener
    @Async("messageTaskExecutor")
    public void handleDeviceLoginEvent(DeviceLoginEvent event) {
        try {
            log.info("处理设备登录事件: userId={}, sessionId={}, deviceId={}, organizationId={}", 
                    event.getUserId(), event.getSessionId(), event.getDeviceId(), event.getOrganizationId());
            
            // 设备相关推送
            handleDeviceRelatedPush(event);
            
        } catch (Exception e) {
            log.error("处理设备登录事件失败: userId={}, deviceId={}, error={}", 
                     event.getUserId(), event.getDeviceId(), e.getMessage(), e);
        }
    }
    
    /**
     * 处理用户会话恢复事件
     * 
     * 用户会话从断开状态恢复时触发，通常发生在网络中断恢复后。
     * 主要推送会话相关的消息和任务结果。
     * 
     * @param event 会话恢复事件
     */
    @EventListener
    @Async("messageTaskExecutor")
    public void handleSessionResumeEvent(SessionResumeEvent event) {
        try {
            log.info("处理会话恢复事件: userId={}, sessionId={}, organizationId={}, resumeTime={}", 
                    event.getUserId(), event.getSessionId(), event.getOrganizationId(), event.getResumeTime());
            
            // 会话级推送
            handleSessionPush(event);
            
        } catch (Exception e) {
            log.error("处理会话恢复事件失败: userId={}, sessionId={}, error={}", 
                     event.getUserId(), event.getSessionId(), e.getMessage(), e);
        }
    }
    
    // ==================== 私有推送策略方法 ====================
    
    /**
     * 执行完整推送
     * 
     * 推送所有未读消息和未查看任务结果，适用于首次登录或长时间离线后上线。
     */
    private void handleFullPush(BaseUserEvent event) {
        try {
            log.debug("执行完整推送: userId={}, sessionId={}", event.getUserId(), event.getSessionId());
            
            UserOnlinePushResult result = userOnlinePushService.handleUserOnline(
                event.getUserId(), event.getSessionId(), event.getOrganizationId());
            
            if (result.getSuccess()) {
                log.info("完整推送成功: {}", result.getSummary());
            } else {
                log.warn("完整推送部分失败: userId={}, error={}", 
                        event.getUserId(), result.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("执行完整推送失败: userId={}, error={}", event.getUserId(), e.getMessage(), e);
        }
    }
    
    /**
     * 执行增量推送
     * 
     * 仅推送高优先级消息和最新任务结果，适用于频繁登录的用户。
     */
    private void handleIncrementalPush(BaseUserEvent event) {
        try {
            log.debug("执行增量推送: userId={}, sessionId={}", event.getUserId(), event.getSessionId());
            
            // 仅推送高优先级消息
            int highPriorityCount = userOnlinePushService.pushHighPriorityMessages(
                event.getUserId(), event.getSessionId(), event.getOrganizationId());
            
            // 推送最新任务结果（限制数量）
            int taskCount = userOnlinePushService.pushUnviewedTaskResults(
                event.getUserId(), event.getSessionId(), event.getOrganizationId());
            
            log.info("增量推送完成: userId={}, highPriorityMessages={}, tasks={}", 
                    event.getUserId(), highPriorityCount, taskCount);
            
        } catch (Exception e) {
            log.error("执行增量推送失败: userId={}, error={}", event.getUserId(), e.getMessage(), e);
        }
    }
    
    /**
     * 执行快速推送
     * 
     * 仅推送最紧急的消息，适用于短暂离线后恢复在线。
     */
    private void handleQuickPush(BaseUserEvent event) {
        try {
            log.debug("执行快速推送: userId={}, sessionId={}", event.getUserId(), event.getSessionId());
            
            // 仅推送高优先级消息
            int pushed = userOnlinePushService.pushHighPriorityMessages(
                event.getUserId(), event.getSessionId(), event.getOrganizationId());
            
            // 推送用户统计信息
            userOnlinePushService.pushUserStatistics(
                event.getUserId(), event.getSessionId(), event.getOrganizationId());
            
            log.info("快速推送完成: userId={}, pushedCount={}", event.getUserId(), pushed);
            
        } catch (Exception e) {
            log.error("执行快速推送失败: userId={}, error={}", event.getUserId(), e.getMessage(), e);
        }
    }
    
    /**
     * 执行实时推送
     * 
     * 推送实时性要求高的消息，适用于WebSocket连接建立时。
     */
    private void handleRealtimePush(BaseUserEvent event) {
        try {
            log.debug("执行实时推送: userId={}, sessionId={}", event.getUserId(), event.getSessionId());
            
            // 推送最新的高优先级消息（少量）
            int pushed = userOnlinePushService.pushUnreadMessages(
                event.getUserId(), event.getSessionId(), event.getOrganizationId(), 5);
            
            // 推送用户统计信息
            userOnlinePushService.pushUserStatistics(
                event.getUserId(), event.getSessionId(), event.getOrganizationId());
            
            log.info("实时推送完成: userId={}, pushedCount={}", event.getUserId(), pushed);
            
        } catch (Exception e) {
            log.error("执行实时推送失败: userId={}, error={}", event.getUserId(), e.getMessage(), e);
        }
    }
    
    /**
     * 执行设备相关推送
     * 
     * 推送与设备相关的消息和任务，适用于设备登录时。
     */
    private void handleDeviceRelatedPush(DeviceLoginEvent event) {
        try {
            log.debug("执行设备相关推送: userId={}, deviceId={}", event.getUserId(), event.getDeviceId());
            
            // 推送设备相关的高优先级消息
            int messageCount = userOnlinePushService.pushHighPriorityMessages(
                event.getUserId(), event.getSessionId(), event.getOrganizationId());
            
            // 推送设备相关的任务结果
            int taskCount = userOnlinePushService.pushUnviewedTaskResults(
                event.getUserId(), event.getSessionId(), event.getOrganizationId());
            
            log.info("设备相关推送完成: userId={}, deviceId={}, messages={}, tasks={}", 
                    event.getUserId(), event.getDeviceId(), messageCount, taskCount);
            
        } catch (Exception e) {
            log.error("执行设备相关推送失败: userId={}, deviceId={}, error={}", 
                     event.getUserId(), event.getDeviceId(), e.getMessage(), e);
        }
    }
    
    /**
     * 执行会话级推送
     * 
     * 推送特定会话相关的消息，适用于会话恢复时。
     */
    private void handleSessionPush(BaseUserEvent event) {
        try {
            log.debug("执行会话级推送: userId={}, sessionId={}", event.getUserId(), event.getSessionId());
            
            // 推送会话相关的消息（少量）
            int pushed = userOnlinePushService.pushUnreadMessages(
                event.getUserId(), event.getSessionId(), event.getOrganizationId(), 10);
            
            // 推送会话相关的任务结果
            int taskCount = userOnlinePushService.pushUnviewedTaskResults(
                event.getUserId(), event.getSessionId(), event.getOrganizationId());
            
            log.info("会话级推送完成: userId={}, sessionId={}, messages={}, tasks={}", 
                    event.getUserId(), event.getSessionId(), pushed, taskCount);
            
        } catch (Exception e) {
            log.error("执行会话级推送失败: userId={}, sessionId={}, error={}", 
                     event.getUserId(), event.getSessionId(), e.getMessage(), e);
        }
    }
    
    // ==================== 事件类定义 ====================
    
    /**
     * 基础用户事件
     */
    public static abstract class BaseUserEvent {
        private String userId;
        private String sessionId;
        private String organizationId;
        
        public BaseUserEvent(String userId, String sessionId, String organizationId) {
            this.userId = userId;
            this.sessionId = sessionId;
            this.organizationId = organizationId;
        }
        
        public String getUserId() { return userId; }
        public String getSessionId() { return sessionId; }
        public String getOrganizationId() { return organizationId; }
    }
    
    /**
     * 用户登录事件
     */
    public static class UserLoginEvent extends BaseUserEvent {
        private java.time.LocalDateTime loginTime;
        private boolean firstLogin;
        private String loginSource;
        
        public UserLoginEvent(String userId, String sessionId, String organizationId, 
                            java.time.LocalDateTime loginTime, boolean firstLogin, String loginSource) {
            super(userId, sessionId, organizationId);
            this.loginTime = loginTime;
            this.firstLogin = firstLogin;
            this.loginSource = loginSource;
        }
        
        public java.time.LocalDateTime getLoginTime() { return loginTime; }
        public boolean isFirstLogin() { return firstLogin; }
        public String getLoginSource() { return loginSource; }
    }
    
    /**
     * 用户上线事件
     */
    public static class UserOnlineEvent extends BaseUserEvent {
        private java.time.LocalDateTime onlineTime;
        private long offlineDurationMs;
        
        public UserOnlineEvent(String userId, String sessionId, String organizationId, 
                             java.time.LocalDateTime onlineTime, long offlineDurationMs) {
            super(userId, sessionId, organizationId);
            this.onlineTime = onlineTime;
            this.offlineDurationMs = offlineDurationMs;
        }
        
        public java.time.LocalDateTime getOnlineTime() { return onlineTime; }
        public long getOfflineDurationMs() { return offlineDurationMs; }
    }
    
    /**
     * WebSocket连接事件
     */
    public static class WebSocketConnectEvent extends BaseUserEvent {
        private java.time.LocalDateTime connectTime;
        
        public WebSocketConnectEvent(String userId, String sessionId, String organizationId, 
                                   java.time.LocalDateTime connectTime) {
            super(userId, sessionId, organizationId);
            this.connectTime = connectTime;
        }
        
        public java.time.LocalDateTime getConnectTime() { return connectTime; }
    }
    
    /**
     * 设备登录事件
     */
    public static class DeviceLoginEvent extends BaseUserEvent {
        private String deviceId;
        private String deviceType;
        private java.time.LocalDateTime loginTime;
        
        public DeviceLoginEvent(String userId, String sessionId, String organizationId, 
                              String deviceId, String deviceType, java.time.LocalDateTime loginTime) {
            super(userId, sessionId, organizationId);
            this.deviceId = deviceId;
            this.deviceType = deviceType;
            this.loginTime = loginTime;
        }
        
        public String getDeviceId() { return deviceId; }
        public String getDeviceType() { return deviceType; }
        public java.time.LocalDateTime getLoginTime() { return loginTime; }
    }
    
    /**
     * 会话恢复事件
     */
    public static class SessionResumeEvent extends BaseUserEvent {
        private java.time.LocalDateTime resumeTime;
        private long disconnectDurationMs;
        
        public SessionResumeEvent(String userId, String sessionId, String organizationId, 
                                java.time.LocalDateTime resumeTime, long disconnectDurationMs) {
            super(userId, sessionId, organizationId);
            this.resumeTime = resumeTime;
            this.disconnectDurationMs = disconnectDurationMs;
        }
        
        public java.time.LocalDateTime getResumeTime() { return resumeTime; }
        public long getDisconnectDurationMs() { return disconnectDurationMs; }
    }
}