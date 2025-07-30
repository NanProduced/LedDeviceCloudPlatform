package org.nan.cloud.message.infrastructure.websocket.routing;

/**
 * 订阅层次枚举
 * 
 * 定义了不同的订阅生命周期层次。
 * 
 * @author Nan
 * @since 1.0.0
 */
public enum SubscriptionLevel {
    
    /**
     * 持久订阅
     * 用户连接期间始终有效，跨会话保持
     */
    PERSISTENT,
    
    /**
     * 会话订阅
     * 特定会话期间有效，会话结束后自动清理
     */
    SESSION,
    
    /**
     * 临时订阅
     * 特定操作期间有效，操作完成后立即清理
     */
    TEMPORARY,
    
    /**
     * 全局订阅
     * 系统级别的订阅，通常用于系统消息
     */
    GLOBAL,
    
    /**
     * 页面订阅
     * 与前端页面生命周期绑定的订阅
     */
    PAGE
}