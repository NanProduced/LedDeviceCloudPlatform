package org.nan.cloud.message.infrastructure.websocket.routing;

/**
 * 路由策略枚举
 * 
 * 定义了不同的消息路由策略类型。
 * 
 * @author Nan
 * @since 1.0.0
 */
public enum RoutingStrategy {
    
    /**
     * 基于规则的路由
     * 通过预定义的路由规则进行消息分发
     */
    RULE_BASED,
    
    /**
     * 直接路由
     * 消息直接包含目标主题信息，无需额外处理
     */
    DIRECT,
    
    /**
     * 广播路由
     * 向所有相关订阅者广播消息
     */
    BROADCAST,
    
    /**
     * 负载均衡路由
     * 在多个可选主题间进行负载均衡
     */
    LOAD_BALANCED,
    
    /**
     * 回退路由
     * 当其他路由策略失败时使用的默认策略
     */
    FALLBACK,
    
    /**
     * 条件路由
     * 根据动态条件选择路由目标
     */
    CONDITIONAL,
    
    /**
     * 聚合路由
     * 将消息聚合后发送到聚合主题
     */
    AGGREGATED
}