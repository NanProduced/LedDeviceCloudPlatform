package org.nan.cloud.message.infrastructure.websocket.session;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * WebSocket会话信息实体
 * 
 * 封装WebSocket连接的会话信息，用于Redis持久化存储。
 * 包含连接的基本信息、用户信息、状态信息等。
 * 
 * 这个类的数据会被序列化为JSON存储在Redis中，
 * 支持跨节点的会话信息共享和状态管理。
 * 
 * @author Nan
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketSessionInfo {
    
    /**
     * WebSocket会话ID
     * Spring WebSocket自动生成的唯一标识
     */
    private String sessionId;
    
    /**
     * 用户ID
     * 连接对应的用户标识
     */
    private String userId;
    
    /**
     * 组织ID
     * 用户所属的组织标识，用于组织级别的消息广播
     */
    private String organizationId;
    
    /**
     * 用户认证令牌
     * JWT token或其他形式的认证凭证
     */
    private String token;
    
    /**
     * 连接建立时间
     * 记录WebSocket连接建立的时间点
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime connectTime;
    
    /**
     * 最后活跃时间
     * 记录连接最后一次有活动（发送/接收消息）的时间
     * 用于心跳检测和会话过期判断
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastActivityTime;
    
    /**
     * 客户端IP地址
     * 连接来源的IP地址，用于安全审计和分析
     */
    private String clientIp;
    
    /**
     * 用户代理信息
     * 客户端的浏览器或应用信息
     */
    private String userAgent;
    
    /**
     * 连接状态
     * CONNECTED: 已连接
     * DISCONNECTED: 已断开
     * RECONNECTING: 重连中
     */
    @Builder.Default
    private ConnectionStatus status = ConnectionStatus.CONNECTED;
    
    /**
     * 服务节点标识
     * 标识处理此连接的服务器节点，用于分布式环境下的负载均衡
     */
    private String nodeId;
    
    /**
     * 设备类型
     * WEB: 网页端
     * MOBILE: 移动端
     * DESKTOP: 桌面端
     */
    private DeviceType deviceType;
    
    /**
     * 设备标识
     * 设备的唯一标识符，用于支持多设备登录
     */
    private String deviceId;
    
    /**
     * 连接协议版本
     * WebSocket协议版本或自定义协议版本
     */
    private String protocolVersion;
    
    /**
     * 心跳间隔（秒）
     * 客户端心跳发送间隔
     */
    @Builder.Default
    private Integer heartbeatInterval = 30;
    
    /**
     * 最后心跳时间
     * 最后一次收到客户端心跳的时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastHeartbeatTime;
    
    /**
     * 连接失败重试次数
     * 记录连接重试的次数，用于连接质量分析
     */
    @Builder.Default
    private Integer retryCount = 0;
    
    /**
     * 扩展属性
     * 存储额外的自定义属性，以JSON字符串形式存储
     */
    private String extendedProperties;
    
    /**
     * 连接状态枚举
     */
    public enum ConnectionStatus {
        CONNECTED,      // 已连接
        DISCONNECTED,   // 已断开
        RECONNECTING    // 重连中
    }
    
    /**
     * 设备类型枚举
     */
    public enum DeviceType {
        WEB,        // 网页端
        MOBILE,     // 移动端
        DESKTOP,    // 桌面端
        TABLET,     // 平板端
        IOT,        // 物联网设备
        UNKNOWN     // 未知类型
    }
    
    /**
     * 更新活跃时间
     * 便捷方法，更新最后活跃时间为当前时间
     */
    public void updateActivity() {
        this.lastActivityTime = LocalDateTime.now();
    }
    
    /**
     * 更新心跳时间
     * 便捷方法，更新最后心跳时间为当前时间
     */
    public void updateHeartbeat() {
        this.lastHeartbeatTime = LocalDateTime.now();
        updateActivity(); // 心跳也算活跃
    }
    
    /**
     * 检查会话是否过期
     * 
     * @param expireMinutes 过期时间（分钟）
     * @return true表示过期，false表示未过期
     */
    public boolean isExpired(long expireMinutes) {
        if (lastActivityTime == null) {
            return false;
        }
        
        LocalDateTime expireTime = lastActivityTime.plusMinutes(expireMinutes);
        return LocalDateTime.now().isAfter(expireTime);
    }
    
    /**
     * 检查心跳是否超时
     * 
     * @param timeoutMinutes 超时时间（分钟）
     * @return true表示超时，false表示未超时
     */
    public boolean isHeartbeatTimeout(long timeoutMinutes) {
        if (lastHeartbeatTime == null) {
            return true; // 没有心跳记录认为超时
        }
        
        LocalDateTime timeoutTime = lastHeartbeatTime.plusMinutes(timeoutMinutes);
        return LocalDateTime.now().isAfter(timeoutTime);
    }
    
    /**
     * 获取连接持续时间（分钟）
     * 
     * @return 连接持续时间
     */
    public long getConnectionDurationMinutes() {
        if (connectTime == null) {
            return 0;
        }
        
        return java.time.Duration.between(connectTime, LocalDateTime.now()).toMinutes();
    }
    
    /**
     * 增加重试次数
     */
    public void incrementRetryCount() {
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
    }
    
    /**
     * 获取IP地址
     * 为了兼容性，提供ipAddress的getter方法
     * 
     * @return IP地址
     */
    public String getIpAddress() {
        return this.clientIp;
    }
    
    /**
     * 设置IP地址
     * 为了兼容性，提供ipAddress的setter方法
     * 
     * @param ipAddress IP地址
     */
    public void setIpAddress(String ipAddress) {
        this.clientIp = ipAddress;
    }
    
    @Override
    public String toString() {
        return String.format("WebSocketSessionInfo{sessionId='%s', userId='%s', organizationId='%s', status=%s, nodeId='%s', deviceType=%s}", 
                sessionId, userId, organizationId, status, nodeId, deviceType);
    }
}