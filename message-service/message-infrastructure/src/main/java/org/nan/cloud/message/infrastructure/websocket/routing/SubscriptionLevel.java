package org.nan.cloud.message.infrastructure.websocket.routing;

/**
 * 订阅层次枚举
 * 
 * 简化的订阅生命周期层次，基于SPA实时通信最佳实践。
 * 
 * @author Nan
 * @since 1.0.0
 */
public enum SubscriptionLevel {
    
    /**
     * 会话订阅
     * 特定会话期间有效，会话结束后自动清理
     * 适用场景：个人消息、组织消息、系统消息、设备消息
     */
    SESSION,
    
    /**
     * 临时订阅
     * 特定操作期间有效，操作完成后立即清理
     * 适用场景：任务进度、批量操作聚合
     */
    TEMPORARY
}