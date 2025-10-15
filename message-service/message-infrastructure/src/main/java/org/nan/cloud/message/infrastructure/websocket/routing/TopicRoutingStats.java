package org.nan.cloud.message.infrastructure.websocket.routing;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 主题路由统计信息
 * 
 * 提供整个路由系统的统计概览。
 * 
 * @author Nan
 * @since 1.0.0
 */
@Data
@Builder
public class TopicRoutingStats {
    
    /**
     * 总路由规则数量
     */
    private int totalRoutingRules;
    
    /**
     * 总订阅用户数量
     */
    private int totalSubscribedUsers;
    
    /**
     * 总主题数量
     */
    private int totalTopics;
    
    
    /**
     * 活跃用户数量
     */
    private int activeUserCount;
    
    /**
     * 总消息路由数量
     */
    private long totalRoutedMessages;
}