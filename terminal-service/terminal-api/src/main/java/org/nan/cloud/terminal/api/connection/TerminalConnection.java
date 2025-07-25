package org.nan.cloud.terminal.api.connection;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备连接信息
 * 
 * 封装WebSocket连接的元数据信息
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Data
public class TerminalConnection {

    /**
     * 设备ID
     */
    private Long tid;

    /**
     * 组织ID
     */
    private Long oid;

    /**
     * 设备名称
     */
    private String terminalName;

    /**
     * WebSocket会话对象
     */
    private Object session;

    /**
     * 连接建立时间
     */
    private LocalDateTime connectTime;

    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveTime;

    /**
     * 最后心跳时间
     */
    private LocalDateTime lastHeartbeatTime;

    /**
     * 客户端IP地址
     */
    private String clientIp;

    /**
     * 连接状态
     */
    private ConnectionStatus status;

    /**
     * 发送消息计数
     */
    private long sentMessageCount;

    /**
     * 接收消息计数
     */
    private long receivedMessageCount;
    
    /**
     * 错误计数
     */
    private long errorCount;

    /**
     * 连接属性（扩展信息）
     */
    private String attributes;

    /**
     * 连接状态枚举
     */
    public enum ConnectionStatus {
        /**
         * 已连接
         */
        CONNECTED,
        
        /**
         * 心跳超时
         */
        HEARTBEAT_TIMEOUT,
        
        /**
         * 已断开
         */
        DISCONNECTED,
        
        /**
         * 异常状态
         */
        ERROR
    }

    /**
     * 创建设备连接
     * 
     * @param tid 设备ID
     * @param oid 组织ID
     * @param session WebSocket会话
     * @param clientIp 客户端IP
     * @return 设备连接对象
     */
    public static TerminalConnection create(Long tid, Long oid, Object session, String clientIp) {
        TerminalConnection connection = new TerminalConnection();
        connection.setTid(tid);
        connection.setOid(oid);
        connection.setSession(session);
        connection.setClientIp(clientIp);
        connection.setConnectTime(LocalDateTime.now());
        connection.setLastActiveTime(LocalDateTime.now());
        connection.setLastHeartbeatTime(LocalDateTime.now());
        connection.setStatus(ConnectionStatus.CONNECTED);
        connection.setSentMessageCount(0);
        connection.setReceivedMessageCount(0);
        return connection;
    }

    /**
     * 更新活跃时间
     */
    public void updateActiveTime() {
        this.lastActiveTime = LocalDateTime.now();
    }

    /**
     * 更新心跳时间
     */
    public void updateHeartbeatTime() {
        this.lastHeartbeatTime = LocalDateTime.now();
        this.lastActiveTime = LocalDateTime.now();
    }

    /**
     * 增加发送消息计数
     */
    public void incrementSentCount() {
        this.sentMessageCount++;
        updateActiveTime();
    }

    /**
     * 增加接收消息计数
     */
    public void incrementReceivedCount() {
        this.receivedMessageCount++;
        updateActiveTime();
    }
    
    /**
     * 增加错误计数
     */
    public void incrementErrorCount() {
        this.errorCount++;
        updateActiveTime();
    }
    
    /**
     * 获取WebSocket会话
     */
    public Object getWebSocketSession() {
        return session;
    }

    /**
     * 检查连接是否过期
     * 
     * @param expireThreshold 过期时间阈值
     * @return 是否过期
     */
    public boolean isExpired(LocalDateTime expireThreshold) {
        return lastHeartbeatTime != null && lastHeartbeatTime.isBefore(expireThreshold);
    }

    /**
     * 获取连接持续时间（秒）
     * 
     * @return 连接持续时间
     */
    public long getConnectionDurationSeconds() {
        if (connectTime == null) {
            return 0;
        }
        return java.time.Duration.between(connectTime, LocalDateTime.now()).getSeconds();
    }

    /**
     * 获取空闲时间（秒）
     * 
     * @return 空闲时间
     */
    public long getIdleTimeSeconds() {
        if (lastActiveTime == null) {
            return 0;
        }
        return java.time.Duration.between(lastActiveTime, LocalDateTime.now()).getSeconds();
    }
}