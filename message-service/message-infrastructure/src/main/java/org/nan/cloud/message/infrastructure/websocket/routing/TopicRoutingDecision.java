package org.nan.cloud.message.infrastructure.websocket.routing;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 主题路由决策结果
 * 
 * 封装了路由管理器做出的路由决策信息，包括目标主题、路由策略等。
 * 
 * @author Nan
 * @since 1.0.0
 */
@Data
@Builder
public class TopicRoutingDecision {
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 路由策略
     */
    private RoutingStrategy routingStrategy;
    
    /**
     * 目标主题列表
     */
    private List<String> targetTopics;
    
    /**
     * 应用的路由规则名称（如果是基于规则的路由）
     */
    private String appliedRule;
    
    /**
     * 决策时间
     */
    @Builder.Default
    private LocalDateTime decisionTime = LocalDateTime.now();
    
    /**
     * 预期的订阅者数量估算
     */
    private Integer estimatedSubscribers;
    
    /**
     * 路由优先级
     */
    @Builder.Default
    private int priority = 5; // 默认中等优先级
    
    /**
     * 是否需要确认收到
     */
    @Builder.Default
    private boolean requiresAcknowledgment = false;
    
    /**
     * 消息TTL（生存时间，秒）
     */
    private Long messageTtl;
}