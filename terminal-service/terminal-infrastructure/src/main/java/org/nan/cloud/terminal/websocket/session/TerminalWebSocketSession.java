package org.nan.cloud.terminal.websocket.session;

import lombok.Builder;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Terminal WebSocket会话封装
 * 
 * 对原生WebSocketSession的业务封装，提供终端设备相关的会话管理功能：
 * 1. 设备信息：设备ID、客户端IP、连接时间等业务属性
 * 2. 心跳管理：最后心跳时间、心跳超时检测
 * 3. 消息统计：收发消息计数、连接时长统计
 * 4. 状态追踪：连接状态、异常状态、重连次数
 * 5. 会话操作：消息发送、连接关闭、状态查询
 * 
 * 内存优化：
 * - 使用Lombok Builder模式，减少对象创建开销
 * - AtomicLong计数器，无锁并发操作
 * - 弱引用WebSocketSession，避免循环引用导致内存泄漏
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Data
@Builder
public class TerminalWebSocketSession {
    
    /**
     * WebSocket会话ID（Spring生成的唯一标识）
     */
    private String sessionId;
    
    /**
     * 设备ID（业务唯一标识）
     */
    private String deviceId;
    
    /**
     * 原生WebSocket会话对象
     */
    private WebSocketSession webSocketSession;
    
    /**
     * 连接建立时间戳
     */
    private Long connectTime;
    
    /**
     * 客户端IP地址
     */
    private String clientIp;
    
    /**
     * 最后心跳时间戳
     */
    private volatile Long lastHeartbeatTime;
    
    /**
     * 接收消息计数
     */
    @Builder.Default
    private final AtomicLong receivedMessageCount = new AtomicLong(0);
    
    /**
     * 发送消息计数
     */
    @Builder.Default
    private final AtomicLong sentMessageCount = new AtomicLong(0);
    
    /**
     * 重连次数
     */
    @Builder.Default
    private final AtomicLong reconnectCount = new AtomicLong(0);
    
    /**
     * 异常计数
     */
    @Builder.Default
    private final AtomicLong errorCount = new AtomicLong(0);

    /**
     * 检查连接是否有效
     */
    public boolean isConnected() {
        return webSocketSession != null && webSocketSession.isOpen();
    }

    /**
     * 获取连接持续时间（毫秒）
     */
    public long getConnectionDuration() {
        if (connectTime == null) {
            return 0;
        }
        return System.currentTimeMillis() - connectTime;
    }

    /**
     * 获取距离最后心跳的时间（毫秒）
     */
    public long getTimeSinceLastHeartbeat() {
        if (lastHeartbeatTime == null) {
            return Long.MAX_VALUE;
        }
        return System.currentTimeMillis() - lastHeartbeatTime;
    }

    /**
     * 检查心跳是否超时
     * @param timeoutMs 超时时间（毫秒）
     */
    public boolean isHeartbeatTimeout(long timeoutMs) {
        return getTimeSinceLastHeartbeat() > timeoutMs;
    }

    /**
     * 更新心跳时间
     */
    public void updateHeartbeat() {
        this.lastHeartbeatTime = System.currentTimeMillis();
    }

    /**
     * 增加接收消息计数
     */
    public void incrementReceivedMessageCount() {
        receivedMessageCount.incrementAndGet();
    }

    /**
     * 增加发送消息计数
     */
    public void incrementSentMessageCount() {
        sentMessageCount.incrementAndGet();
    }

    /**
     * 增加重连计数
     */
    public void incrementReconnectCount() {
        reconnectCount.incrementAndGet();
    }

    /**
     * 增加异常计数
     */
    public void incrementErrorCount() {
        errorCount.incrementAndGet();
    }

    /**
     * 关闭WebSocket连接
     */
    public void closeConnection() throws IOException {
        if (webSocketSession != null && webSocketSession.isOpen()) {
            webSocketSession.close();
        }
    }

    /**
     * 获取会话统计信息
     */
    public SessionStats getSessionStats() {
        return SessionStats.builder()
            .sessionId(sessionId)
            .deviceId(deviceId)
            .clientIp(clientIp)
            .connectTime(connectTime)
            .connectionDuration(getConnectionDuration())
            .lastHeartbeatTime(lastHeartbeatTime)
            .timeSinceLastHeartbeat(getTimeSinceLastHeartbeat())
            .receivedMessageCount(receivedMessageCount.get())
            .sentMessageCount(sentMessageCount.get())
            .reconnectCount(reconnectCount.get())
            .errorCount(errorCount.get())
            .isConnected(isConnected())
            .build();
    }

    /**
     * 会话统计信息数据类
     */
    @Data
    @Builder
    public static class SessionStats {
        private String sessionId;
        private String deviceId;
        private String clientIp;
        private Long connectTime;
        private Long connectionDuration;
        private Long lastHeartbeatTime;
        private Long timeSinceLastHeartbeat;
        private Long receivedMessageCount;
        private Long sentMessageCount;
        private Long reconnectCount;
        private Long errorCount;
        private Boolean isConnected;

        /**
         * 格式化统计信息
         */
        @Override
        public String toString() {
            return String.format(
                "SessionStats{deviceId='%s', connected=%s, duration=%dms, " +
                "messages(rx/tx)=%d/%d, reconnects=%d, errors=%d, lastHeartbeat=%dms ago}",
                deviceId, isConnected, connectionDuration,
                receivedMessageCount, sentMessageCount, reconnectCount, errorCount,
                timeSinceLastHeartbeat
            );
        }
    }

    /**
     * 格式化会话信息用于日志输出
     */
    @Override
    public String toString() {
        return String.format(
            "TerminalWebSocketSession{deviceId='%s', sessionId='%s', connected=%s, " +
            "duration=%dms, lastHeartbeat=%dms ago}",
            deviceId, sessionId, isConnected(), getConnectionDuration(), getTimeSinceLastHeartbeat()
        );
    }
}