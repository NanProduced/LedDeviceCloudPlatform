package org.nan.cloud.message.infrastructure.websocket.routing;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户订阅统计信息
 * 简化版本，基于SESSION和TEMPORARY两种订阅层次
 * 
 * @author Nan
 * @since 1.0.0
 */
@Data
@Builder
public class UserSubscriptionStats {
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 会话订阅数量
     */
    private int sessionSubscriptionCount;
    
    /**
     * 临时订阅数量
     */
    private int temporarySubscriptionCount;
    
    /**
     * 总订阅主题数量
     */
    private int totalTopicsSubscribed;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdated;
}