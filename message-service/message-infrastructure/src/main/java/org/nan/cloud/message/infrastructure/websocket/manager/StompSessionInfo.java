package org.nan.cloud.message.infrastructure.websocket.manager;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * STOMP会话信息
 * 
 * 封装单个STOMP连接会话的详细信息，包括：
 * 1. 会话标识和用户信息
 * 2. 连接时间和活跃状态
 * 3. 客户端信息和设备类型
 * 4. 扩展属性和元数据
 * 
 * 用于StompConnectionManager进行会话生命周期管理
 * 
 * @author Nan
 * @since 1.0.0
 */
@Data
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class StompSessionInfo {
    
    /**
     * STOMP会话ID（唯一标识）
     * 作为equals和hashCode的唯一依据
     */
    @EqualsAndHashCode.Include
    private String sessionId;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 组织ID
     */
    private Long organizationId;
    
    /**
     * 用户组ID
     */
    private Long ugid;
    
    /**
     * 用户类型
     * 0: 系统管理员, 1: 组织管理员, 2: 普通用户
     */
    private Integer userType;
    
    /**
     * 连接建立时间
     */
    private LocalDateTime connectTime;
    
    /**
     * 最后活跃时间
     * TODO: 在updateSessionActivity中更新
     */
    private LocalDateTime lastActiveTime;
    
    /**
     * 客户端信息
     * 包含User-Agent、IP地址等信息
     */
    private String clientInfo;
    
    /**
     * 客户端IP地址
     */
    private String clientIp;
    
    /**
     * 设备类型
     * 如：WEB、MOBILE、DESKTOP等
     */
    private String deviceType;
    
    /**
     * 设备标识
     * 用于区分同一用户的不同设备
     */
    private String deviceId;
    
    /**
     * 连接状态
     * ACTIVE: 活跃连接
     * IDLE: 空闲状态
     * DISCONNECTING: 正在断开
     */
    @Builder.Default
    private ConnectionStatus status = ConnectionStatus.ACTIVE;
    
    /**
     * 会话扩展属性
     * 用于存储额外的会话相关信息
     */
    @Builder.Default
    private Map<String, Object> attributes = new ConcurrentHashMap<>();
    
    /**
     * 当前订阅的Topic数量
     * TODO: 与SubscriptionManager集成后更新
     */
    @Builder.Default
    private int subscriptionCount = 0;
    
    /**
     * 发送的消息数量
     */
    @Builder.Default
    private long sentMessageCount = 0L;
    
    /**
     * 接收的消息数量
     */
    @Builder.Default
    private long receivedMessageCount = 0L;
    
    /**
     * 连接状态枚举
     */
    public enum ConnectionStatus {
        /**
         * 活跃连接
         */
        ACTIVE("活跃"),
        
        /**
         * 空闲状态
         */
        IDLE("空闲"),
        
        /**
         * 正在断开连接
         */
        DISCONNECTING("断开中");
        
        private final String description;
        
        ConnectionStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 获取连接持续时间（秒）
     * 
     * @return 连接持续时间
     */
    public long getConnectionDuration() {
        if (connectTime == null) {
            return 0;
        }
        
        LocalDateTime endTime = status == ConnectionStatus.ACTIVE ? 
                LocalDateTime.now() : (lastActiveTime != null ? lastActiveTime : LocalDateTime.now());
        
        return java.time.Duration.between(connectTime, endTime).toSeconds();
    }
    
    /**
     * 获取空闲时间（秒）
     * 
     * @return 空闲时间
     */
    public long getIdleTime() {
        if (lastActiveTime == null) {
            return 0;
        }
        
        return java.time.Duration.between(lastActiveTime, LocalDateTime.now()).toSeconds();
    }
    
    /**
     * 检查是否为管理员用户
     * 
     * @return true表示是管理员
     */
    public boolean isManager() {
        return userType != null && (userType == 0 || userType == 1);
    }
    
    /**
     * 检查是否为系统管理员
     * 
     * @return true表示是系统管理员
     */
    public boolean isSystemAdmin() {
        return userType != null && userType == 0;
    }
    
    /**
     * 检查是否为组织管理员
     * 
     * @return true表示是组织管理员
     */
    public boolean isOrgManager() {
        return userType != null && userType == 1;
    }
    
    /**
     * 增加发送消息计数
     */
    public void incrementSentMessage() {
        this.sentMessageCount++;
    }
    
    /**
     * 增加接收消息计数
     */
    public void incrementReceivedMessage() {
        this.receivedMessageCount++;
    }
    
    /**
     * 更新最后活跃时间
     */
    public void updateLastActiveTime() {
        this.lastActiveTime = LocalDateTime.now();
    }
    
    /**
     * 设置会话属性
     * 
     * @param key 属性键
     * @param value 属性值
     */
    public void setAttribute(String key, Object value) {
        if (attributes == null) {
            attributes = new ConcurrentHashMap<>();
        }
        attributes.put(key, value);
    }
    
    /**
     * 获取会话属性
     * 
     * @param key 属性键
     * @return 属性值，如果不存在则返回null
     */
    public Object getAttribute(String key) {
        return attributes != null ? attributes.get(key) : null;
    }
    
    /**
     * 获取会话属性（带类型转换）
     * 
     * @param key 属性键
     * @param type 期望的类型
     * @param <T> 类型参数
     * @return 转换后的属性值，如果不存在或类型不匹配则返回null
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, Class<T> type) {
        Object value = getAttribute(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * 移除会话属性
     * 
     * @param key 属性键
     * @return 被移除的属性值
     */
    public Object removeAttribute(String key) {
        return attributes != null ? attributes.remove(key) : null;
    }
    
    /**
     * 检查是否包含指定属性
     * 
     * @param key 属性键
     * @return true表示包含该属性
     */
    public boolean hasAttribute(String key) {
        return attributes != null && attributes.containsKey(key);
    }
    
    /**
     * 清空所有属性
     */
    public void clearAttributes() {
        if (attributes != null) {
            attributes.clear();
        }
    }
    
    /**
     * 获取会话摘要信息（用于日志和监控）
     * 
     * @return 会话摘要字符串
     */
    public String getSummary() {
        return String.format("Session[id=%s, user=%s, org=%s, status=%s, duration=%ds, subscriptions=%d]",
                sessionId, userId, organizationId, status.getDescription(), 
                getConnectionDuration(), subscriptionCount);
    }
}