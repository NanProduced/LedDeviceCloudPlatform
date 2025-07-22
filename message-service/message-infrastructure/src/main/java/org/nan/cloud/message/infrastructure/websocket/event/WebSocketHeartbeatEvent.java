package org.nan.cloud.message.infrastructure.websocket.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * WebSocket心跳事件
 * 
 * 用于解耦WebSocket连接管理器和心跳管理器之间的依赖关系。
 * 通过事件机制实现心跳更新的异步处理。
 * 
 * @author Nan
 * @since 1.0.0
 */
@Getter
public class WebSocketHeartbeatEvent extends ApplicationEvent {
    
    /**
     * WebSocket会话ID
     */
    private final String sessionId;
    
    /**
     * 事件类型
     */
    private final HeartbeatEventType eventType;
    
    /**
     * 构造心跳事件
     * 
     * @param source 事件源
     * @param sessionId 会话ID
     * @param eventType 事件类型
     */
    public WebSocketHeartbeatEvent(Object source, String sessionId, HeartbeatEventType eventType) {
        super(source);
        this.sessionId = sessionId;
        this.eventType = eventType;
    }
    
    /**
     * 心跳事件类型枚举
     */
    public enum HeartbeatEventType {
        /**
         * 更新心跳
         */
        UPDATE,
        
        /**
         * 注册连接
         */
        REGISTER,
        
        /**
         * 移除连接
         */
        UNREGISTER
    }
    
    @Override
    public String toString() {
        return String.format("WebSocketHeartbeatEvent{sessionId='%s', eventType=%s}", 
                sessionId, eventType);
    }
}