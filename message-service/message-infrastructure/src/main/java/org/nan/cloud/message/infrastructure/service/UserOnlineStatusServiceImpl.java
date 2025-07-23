package org.nan.cloud.message.infrastructure.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.api.dto.response.UserStatusResponse;
import org.nan.cloud.message.infrastructure.redis.manager.MessageCacheManager;
import org.nan.cloud.message.infrastructure.websocket.session.WebSocketSessionInfo;
import org.nan.cloud.message.infrastructure.websocket.session.WebSocketSessionStore;
import org.nan.cloud.message.service.UserOnlineStatusService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 用户在线状态管理服务实现
 * 
 * 基于Redis和WebSocket会话管理实现完整的用户在线状态管理功能。
 * 集成现有的MessageCacheManager和WebSocketSessionStore，提供统一的状态管理接口。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserOnlineStatusServiceImpl implements UserOnlineStatusService {
    
    private final MessageCacheManager messageCacheManager;
    private final WebSocketSessionStore webSocketSessionStore;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${message.online-status.default-expire-seconds:1800}")
    private int defaultExpireSeconds;
    
    @Value("${message.online-status.heartbeat-interval-seconds:30}")
    private int heartbeatIntervalSeconds;
    
    @Value("${message.online-status.max-devices-per-user:10}")
    private int maxDevicesPerUser;
    
    @Value("${spring.application.name:message-service}")
    private String instanceId;
    
    // 状态变更事件订阅管理
    private final Map<String, Consumer<UserStatusResponse.StatusChangeEvent>> eventSubscribers = 
        new ConcurrentHashMap<>();
    
    // ==================== 基础状态管理 ====================
    
    @Override
    public UserStatusResponse markUserOnline(String userId, String sessionId, String organizationId, 
                                           String deviceType, String ipAddress, String userAgent) {
        try {
            log.info("标记用户上线: userId={}, sessionId={}, organizationId={}, deviceType={}", 
                    userId, sessionId, organizationId, deviceType);
            
            // 获取用户当前状态
            UserStatusResponse oldStatus = getUserOnlineStatus(userId);
            
            // 创建会话信息
            WebSocketSessionInfo sessionInfo = createSessionInfo(
                userId, sessionId, organizationId, deviceType, ipAddress, userAgent
            );
            
            // 存储会话信息到Redis
            webSocketSessionStore.storeSession(sessionInfo, defaultExpireSeconds);
            
            // 标记用户在线
            messageCacheManager.markUserOnline(userId, defaultExpireSeconds);
            
            // 注册WebSocket会话
            messageCacheManager.registerWebSocketSession(userId, sessionId, instanceId, defaultExpireSeconds);
            
            // 更新组织在线用户统计
            updateOrganizationOnlineCount(organizationId, 1);
            
            // 记录用户活跃度
            recordUserActivity(userId, sessionId, "USER_ONLINE", 
                Map.of("deviceType", deviceType, "ipAddress", ipAddress));
            
            // 获取新状态
            UserStatusResponse newStatus = getUserOnlineStatus(userId);
            
            // 异步广播状态变更事件
            asyncBroadcastStatusChange(userId, oldStatus, newStatus, "USER_LOGIN");
            
            log.info("用户上线标记成功: userId={}, sessionId={}, deviceCount={}", 
                    userId, sessionId, newStatus.getDeviceCount());
            
            return newStatus;
            
        } catch (Exception e) {
            log.error("标记用户上线失败: userId={}, sessionId={}, error={}", 
                     userId, sessionId, e.getMessage(), e);
            throw new RuntimeException("标记用户上线失败", e);
        }
    }
    
    @Override
    public boolean markUserOffline(String userId, String sessionId, String reason) {
        try {
            log.info("标记用户离线: userId={}, sessionId={}, reason={}", userId, sessionId, reason);
            
            // 获取用户当前状态
            UserStatusResponse oldStatus = getUserOnlineStatus(userId);
            
            boolean success = false;
            
            if (sessionId != null) {
                // 标记指定会话离线
                webSocketSessionStore.removeSession(sessionId);
                messageCacheManager.removeWebSocketSession(userId, sessionId);
                
                // 记录离线活跃度
                recordUserActivity(userId, sessionId, "USER_OFFLINE", 
                    Map.of("reason", reason));
                
                success = true;
                
                // 检查用户是否还有其他在线会话
                Map<Object, Object> activeSessions = messageCacheManager.getUserActiveSessions(userId);
                if (activeSessions.isEmpty()) {
                    // 没有其他会话，标记用户完全离线
                    messageCacheManager.markUserOffline(userId);
                    
                    // 更新组织在线统计
                    String organizationId = oldStatus != null ? oldStatus.getOrganizationId() : null;
                    if (organizationId != null) {
                        updateOrganizationOnlineCount(organizationId, -1);
                    }
                }
            } else {
                // 标记用户所有会话离线
                Map<Object, Object> activeSessions = messageCacheManager.getUserActiveSessions(userId);
                
                for (Object sessionIdObj : activeSessions.keySet()) {
                    String sid = (String) sessionIdObj;
                    webSocketSessionStore.removeSession(sid);
                    messageCacheManager.removeWebSocketSession(userId, sid);
                }
                
                // 标记用户离线
                messageCacheManager.markUserOffline(userId);
                
                // 更新组织在线统计
                String organizationId = oldStatus != null ? oldStatus.getOrganizationId() : null;
                if (organizationId != null) {
                    updateOrganizationOnlineCount(organizationId, -1);
                }

            }
            
            // 获取新状态
            UserStatusResponse newStatus = getUserOnlineStatus(userId);
            
            // 异步广播状态变更事件
            asyncBroadcastStatusChange(userId, oldStatus, newStatus, reason);
            
            log.info("用户离线标记成功: userId={}, sessionId={}, reason={}", userId, sessionId, reason);
            return true;
            
        } catch (Exception e) {
            log.error("标记用户离线失败: userId={}, sessionId={}, error={}", 
                     userId, sessionId, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean updateUserHeartbeat(String userId, String sessionId) {
        try {
            // 更新会话心跳时间
            WebSocketSessionInfo sessionInfo = webSocketSessionStore.getSession(sessionId);
            if (sessionInfo != null) {
                sessionInfo.setLastHeartbeatTime(LocalDateTime.now());
                webSocketSessionStore.storeSession(sessionInfo, defaultExpireSeconds);
                
                // 延长用户在线状态
                messageCacheManager.markUserOnline(userId, defaultExpireSeconds);
                
                // 记录心跳活跃度
                recordUserActivity(userId, sessionId, "HEARTBEAT", Map.of());
                
                log.debug("用户心跳更新成功: userId={}, sessionId={}", userId, sessionId);
                return true;
            } else {
                log.warn("会话不存在，无法更新心跳: userId={}, sessionId={}", userId, sessionId);
                return false;
            }
        } catch (Exception e) {
            log.error("更新用户心跳失败: userId={}, sessionId={}, error={}", 
                     userId, sessionId, e.getMessage(), e);
            return false;
        }
    }
    
    // ==================== 状态查询 ====================
    
    @Override
    public boolean isUserOnline(String userId) {
        try {
            return messageCacheManager.isUserOnline(userId);
        } catch (Exception e) {
            log.error("检查用户在线状态失败: userId={}, error={}", userId, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public UserStatusResponse getUserOnlineStatus(String userId) {
        try {
            UserStatusResponse response = new UserStatusResponse();
            response.setUserId(userId);
            response.setOnline(messageCacheManager.isUserOnline(userId));
            
            if (response.isOnline()) {
                // 获取用户的所有活跃会话
                Map<Object, Object> activeSessions = messageCacheManager.getUserActiveSessions(userId);
                List<UserStatusResponse.DeviceInfo> devices = new ArrayList<>();
                
                for (Object sessionIdObj : activeSessions.keySet()) {
                    String sessionId = (String) sessionIdObj;
                    WebSocketSessionInfo sessionInfo = webSocketSessionStore.getSession(sessionId);
                    
                    if (sessionInfo != null) {
                        UserStatusResponse.DeviceInfo deviceInfo = new UserStatusResponse.DeviceInfo();
                        deviceInfo.setSessionId(sessionId);
                        deviceInfo.setDeviceType(sessionInfo.getDeviceType().name());
                        deviceInfo.setIpAddress(sessionInfo.getIpAddress());
                        deviceInfo.setUserAgent(sessionInfo.getUserAgent());
                        deviceInfo.setConnectTime(sessionInfo.getConnectTime());
                        deviceInfo.setLastHeartbeatTime(sessionInfo.getLastHeartbeatTime());
                        
                        devices.add(deviceInfo);
                        
                        // 设置组织ID（从第一个会话获取）
                        if (response.getOrganizationId() == null) {
                            response.setOrganizationId(sessionInfo.getOrganizationId());
                        }
                    }
                }
                
                response.setDevices(devices);
                response.setDeviceCount(devices.size());
                
                // 计算总在线时长（取最早连接时间）
                LocalDateTime earliestConnectTime = devices.stream()
                    .map(UserStatusResponse.DeviceInfo::getConnectTime)
                    .min(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());
                
                response.setOnlineDurationMinutes(
                    (int) java.time.Duration.between(earliestConnectTime, LocalDateTime.now()).toMinutes()
                );
            } else {
                response.setDevices(List.of());
                response.setDeviceCount(0);
                response.setOnlineDurationMinutes(0);
            }
            
            response.setLastUpdateTime(LocalDateTime.now());
            return response;
            
        } catch (Exception e) {
            log.error("获取用户在线状态失败: userId={}, error={}", userId, e.getMessage(), e);
            
            // 返回默认的离线状态
            UserStatusResponse response = new UserStatusResponse();
            response.setUserId(userId);
            response.setOnline(false);
            response.setDevices(List.of());
            response.setDeviceCount(0);
            return response;
        }
    }
    
    @Override
    public Map<String, UserStatusResponse> batchGetUserOnlineStatus(List<String> userIds) {
        Map<String, UserStatusResponse> resultMap = new HashMap<>();
        
        try {
            // 批量检查用户在线状态
            for (String userId : userIds) {
                UserStatusResponse status = getUserOnlineStatus(userId);
                resultMap.put(userId, status);
            }
            
            log.debug("批量获取用户在线状态成功: userCount={}", userIds.size());
            
        } catch (Exception e) {
            log.error("批量获取用户在线状态失败: userIds={}, error={}", userIds.size(), e.getMessage(), e);
            
            // 出错时返回离线状态
            for (String userId : userIds) {
                UserStatusResponse response = new UserStatusResponse();
                response.setUserId(userId);
                response.setOnline(false);
                resultMap.put(userId, response);
            }
        }
        
        return resultMap;
    }
    
    @Override
    public List<UserStatusResponse.DeviceInfo> getUserOnlineDevices(String userId) {
        try {
            UserStatusResponse status = getUserOnlineStatus(userId);
            return status.getDevices();
        } catch (Exception e) {
            log.error("获取用户在线设备失败: userId={}, error={}", userId, e.getMessage(), e);
            return List.of();
        }
    }
    
    // ==================== 组织维度统计 ====================
    
    @Override
    public List<UserStatusResponse> getOrganizationOnlineUsers(String organizationId, boolean includeSubOrg) {
        try {
            // TODO: 实现组织用户查询逻辑
            // 这里需要集成用户管理服务来获取组织用户列表
            log.warn("组织在线用户查询功能待实现: organizationId={}", organizationId);
            return List.of();
        } catch (Exception e) {
            log.error("获取组织在线用户失败: organizationId={}, error={}", organizationId, e.getMessage(), e);
            return List.of();
        }
    }
    
    @Override
    public UserStatusResponse.OrganizationStats getOrganizationOnlineStats(String organizationId) {
        try {
            String statsKey = "org_online_stats:" + organizationId;
            String onlineCountStr = redisTemplate.opsForValue().get(statsKey);
            
            UserStatusResponse.OrganizationStats stats = new UserStatusResponse.OrganizationStats();
            stats.setOrganizationId(organizationId);
            stats.setOnlineUserCount(onlineCountStr != null ? Integer.parseInt(onlineCountStr) : 0);
            stats.setLastUpdateTime(LocalDateTime.now());
            
            return stats;
        } catch (Exception e) {
            log.error("获取组织在线统计失败: organizationId={}, error={}", organizationId, e.getMessage(), e);
            
            UserStatusResponse.OrganizationStats stats = new UserStatusResponse.OrganizationStats();
            stats.setOrganizationId(organizationId);
            stats.setOnlineUserCount(0);
            return stats;
        }
    }
    
    @Override
    public UserStatusResponse.PlatformStats getPlatformOnlineStats() {
        try {
            // 统计全平台在线用户数
            Set<String> onlineUsers = redisTemplate.keys("user_online:*");
            int totalOnlineUsers = onlineUsers.size();
            
            // 统计总连接数
            Set<String> allSessions = redisTemplate.keys("websocket_instance:*");
            int totalConnections = allSessions.size();
            
            UserStatusResponse.PlatformStats stats = new UserStatusResponse.PlatformStats();
            stats.setTotalOnlineUsers(totalOnlineUsers);
            stats.setTotalConnections(totalConnections);
            stats.setLastUpdateTime(LocalDateTime.now());
            
            log.debug("平台在线统计: onlineUsers={}, connections={}", totalOnlineUsers, totalConnections);
            return stats;
            
        } catch (Exception e) {
            log.error("获取平台在线统计失败: error={}", e.getMessage(), e);
            
            UserStatusResponse.PlatformStats stats = new UserStatusResponse.PlatformStats();
            stats.setTotalOnlineUsers(0);
            stats.setTotalConnections(0);
            return stats;
        }
    }
    
    // ==================== 活跃度分析 ====================
    
    @Override
    public void recordUserActivity(String userId, String sessionId, String action, Map<String, Object> details) {
        try {
            String activityKey = "user_activity:" + userId;
            long timestamp = System.currentTimeMillis();
            
            // 构建活跃度记录
            Map<String, Object> activityRecord = new HashMap<>();
            activityRecord.put("action", action);
            activityRecord.put("sessionId", sessionId);
            activityRecord.put("timestamp", timestamp);
            activityRecord.putAll(details);
            
            // 使用ZSet存储活跃度记录，按时间戳排序
            String recordJson = objectMapper.writeValueAsString(activityRecord);
            redisTemplate.opsForZSet().add(activityKey, recordJson, timestamp);
            
            // 只保留最近24小时的活跃度记录
            long yesterday = timestamp - 24 * 60 * 60 * 1000;
            redisTemplate.opsForZSet().removeRangeByScore(activityKey, 0, yesterday);
            
            // 设置过期时间
            redisTemplate.expire(activityKey, 25, TimeUnit.HOURS);
            
            log.debug("用户活跃度记录成功: userId={}, action={}", userId, action);
            
        } catch (Exception e) {
            log.error("记录用户活跃度失败: userId={}, action={}, error={}", 
                     userId, action, e.getMessage(), e);
        }
    }
    
    @Override
    public UserStatusResponse.ActivityStats getUserActivityStats(String userId, int hours) {
        try {
            String activityKey = "user_activity:" + userId;
            long endTime = System.currentTimeMillis();
            long startTime = endTime - hours * 60 * 60 * 1000L;
            
            // 获取指定时间范围内的活跃度记录
            Set<String> activities = redisTemplate.opsForZSet().rangeByScore(activityKey, startTime, endTime);
            
            UserStatusResponse.ActivityStats stats = new UserStatusResponse.ActivityStats();
            stats.setUserId(userId);
            stats.setHours(hours);
            stats.setTotalActivities(activities.size());
            
            // 统计各类活动数量
            Map<String, Integer> actionCounts = new HashMap<>();
            for (String activityJson : activities) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> activity = objectMapper.readValue(activityJson, Map.class);
                    String action = (String) activity.get("action");
                    actionCounts.merge(action, 1, Integer::sum);
                } catch (Exception e) {
                    log.warn("解析活跃度记录失败: {}", activityJson);
                }
            }
            
            stats.setActionCounts(actionCounts);
            stats.setLastUpdateTime(LocalDateTime.now());
            
            return stats;
            
        } catch (Exception e) {
            log.error("获取用户活跃度统计失败: userId={}, hours={}, error={}", 
                     userId, hours, e.getMessage(), e);
            
            UserStatusResponse.ActivityStats stats = new UserStatusResponse.ActivityStats();
            stats.setUserId(userId);
            stats.setHours(hours);
            stats.setTotalActivities(0);
            return stats;
        }
    }
    
    @Override
    public UserStatusResponse.OrganizationActivityStats getOrganizationActivityStats(String organizationId, int hours) {
        // TODO: 实现组织活跃度统计
        log.warn("组织活跃度统计功能待实现: organizationId={}", organizationId);
        
        UserStatusResponse.OrganizationActivityStats stats = new UserStatusResponse.OrganizationActivityStats();
        stats.setOrganizationId(organizationId);
        stats.setHours(hours);
        stats.setTotalActivities(0);
        return stats;
    }
    
    // ==================== 高级功能 ====================
    
    @Override
    public boolean forceUserOffline(String userId, String sessionId, String reason, String operatorId) {
        try {
            log.warn("强制用户下线: userId={}, sessionId={}, reason={}, operator={}", 
                    userId, sessionId, reason, operatorId);
            
            // 记录强制下线操作
            recordUserActivity(userId, sessionId, "FORCE_OFFLINE", 
                Map.of("reason", reason, "operator", operatorId));
            
            // 执行下线操作
            return markUserOffline(userId, sessionId, "FORCE_OFFLINE:" + reason);
            
        } catch (Exception e) {
            log.error("强制用户下线失败: userId={}, error={}", userId, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public List<UserStatusResponse.OnlineHistory> getUserOnlineHistory(String userId, int days) {
        // TODO: 实现用户在线历史查询
        log.warn("用户在线历史查询功能待实现: userId={}, days={}", userId, days);
        return List.of();
    }
    
    @Override
    public int cleanupExpiredOnlineStatus(int timeoutMinutes) {
        try {
            log.info("开始清理过期在线状态: timeoutMinutes={}", timeoutMinutes);
            
            int cleanedCount = 0;
            Set<String> onlineKeys = redisTemplate.keys("user_online:*");

            for (String key : onlineKeys) {
                Long ttl = redisTemplate.getExpire(key);
                if (ttl <= 0) {
                    // TTL已过期，清理相关数据
                    String userId = key.substring("user_online:".length());

                    // 清理用户会话
                    Map<Object, Object> sessions = messageCacheManager.getUserActiveSessions(userId);
                    for (Object sessionIdObj : sessions.keySet()) {
                        String sessionId = (String) sessionIdObj;
                        webSocketSessionStore.removeSession(sessionId);
                    }

                    // 删除在线状态
                    redisTemplate.delete(key);
                    cleanedCount++;

                    log.debug("清理过期在线状态: userId={}", userId);
                }
            }

            log.info("清理过期在线状态完成: cleanedCount={}", cleanedCount);
            return cleanedCount;
            
        } catch (Exception e) {
            log.error("清理过期在线状态失败: error={}", e.getMessage(), e);
            return 0;
        }
    }
    
    @Override
    public int syncOnlineStatus(String sourceInstanceId) {
        // TODO: 实现集群状态同步
        log.warn("集群状态同步功能待实现: sourceInstanceId={}", sourceInstanceId);
        return 0;
    }
    
    // ==================== 事件通知 ====================
    
    @Override
    public String subscribeStatusChangeEvents(String organizationId, 
                                            Consumer<UserStatusResponse.StatusChangeEvent> callback) {
        String subscriptionId = UUID.randomUUID().toString();
        eventSubscribers.put(subscriptionId, callback);
        
        log.info("状态变更事件订阅成功: subscriptionId={}, organizationId={}", 
                subscriptionId, organizationId);
        
        return subscriptionId;
    }
    
    @Override
    public boolean unsubscribeStatusChangeEvents(String subscriptionId) {
        Consumer<UserStatusResponse.StatusChangeEvent> removed = eventSubscribers.remove(subscriptionId);
        boolean success = removed != null;
        
        log.info("取消状态变更事件订阅: subscriptionId={}, success={}", subscriptionId, success);
        return success;
    }
    
    @Override
    @Async
    public void broadcastStatusChangeEvent(String userId, UserStatusResponse oldStatus, 
                                         UserStatusResponse newStatus, String changeReason) {
        try {
            UserStatusResponse.StatusChangeEvent event = new UserStatusResponse.StatusChangeEvent();
            event.setUserId(userId);
            event.setOldStatus(oldStatus);
            event.setNewStatus(newStatus);
            event.setChangeReason(changeReason);
            event.setTimestamp(LocalDateTime.now());
            
            // 通知所有订阅者
            for (Consumer<UserStatusResponse.StatusChangeEvent> callback : eventSubscribers.values()) {
                try {
                    callback.accept(event);
                } catch (Exception e) {
                    log.error("状态变更事件通知失败: userId={}, error={}", userId, e.getMessage(), e);
                }
            }
            
            log.debug("状态变更事件广播完成: userId={}, reason={}, subscriberCount={}", 
                     userId, changeReason, eventSubscribers.size());
            
        } catch (Exception e) {
            log.error("广播状态变更事件失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    // ==================== 配置管理 ====================
    
    @Override
    public Map<String, Object> getOnlineStatusConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("defaultExpireSeconds", defaultExpireSeconds);
        config.put("heartbeatIntervalSeconds", heartbeatIntervalSeconds);
        config.put("maxDevicesPerUser", maxDevicesPerUser);
        config.put("instanceId", instanceId);
        
        return config;
    }
    
    @Override
    public boolean updateOnlineStatusConfig(Map<String, Object> config) {
        // TODO: 实现动态配置更新
        log.warn("动态配置更新功能待实现: config={}", config);
        return false;
    }
    
    // ==================== 私有辅助方法 ====================
    
    private WebSocketSessionInfo createSessionInfo(String userId, String sessionId, String organizationId,
                                                 String deviceType, String ipAddress, String userAgent) {
        WebSocketSessionInfo sessionInfo = new WebSocketSessionInfo();
        sessionInfo.setSessionId(sessionId);
        sessionInfo.setUserId(userId);
        sessionInfo.setOrganizationId(organizationId);
        
        // 设置设备类型
        try {
            sessionInfo.setDeviceType(WebSocketSessionInfo.DeviceType.valueOf(deviceType.toUpperCase()));
        } catch (IllegalArgumentException e) {
            log.warn("无效的设备类型: {}, 使用默认类型WEB", deviceType);
            sessionInfo.setDeviceType(WebSocketSessionInfo.DeviceType.WEB);
        }
        
        // 设置IP地址和用户代理
        sessionInfo.setClientIp(ipAddress);
        sessionInfo.setUserAgent(userAgent);
        
        // 设置时间信息
        LocalDateTime now = LocalDateTime.now();
        sessionInfo.setConnectTime(now);
        sessionInfo.setLastActivityTime(now);
        sessionInfo.setLastHeartbeatTime(now);
        
        // 设置状态和节点信息
        sessionInfo.setStatus(WebSocketSessionInfo.ConnectionStatus.CONNECTED);
        sessionInfo.setNodeId(instanceId);
        
        return sessionInfo;
    }
    
    private void updateOrganizationOnlineCount(String organizationId, int delta) {
        try {
            String statsKey = "org_online_stats:" + organizationId;
            Long newCount = redisTemplate.opsForValue().increment(statsKey, delta);
            
            // 确保计数不为负数
            if (newCount != null && newCount < 0) {
                redisTemplate.opsForValue().set(statsKey, "0");
                newCount = 0L;
            }
            
            // 设置过期时间
            redisTemplate.expire(statsKey, 24, TimeUnit.HOURS);
            
            log.debug("组织在线统计更新: organizationId={}, delta={}, newCount={}", 
                     organizationId, delta, newCount);
            
        } catch (Exception e) {
            log.error("更新组织在线统计失败: organizationId={}, delta={}, error={}", 
                     organizationId, delta, e.getMessage(), e);
        }
    }
    
    @Async
    protected void asyncBroadcastStatusChange(String userId, UserStatusResponse oldStatus,
                                              UserStatusResponse newStatus, String changeReason) {
        broadcastStatusChangeEvent(userId, oldStatus, newStatus, changeReason);
    }
}