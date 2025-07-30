package org.nan.cloud.message.infrastructure.websocket.routing;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 主题订阅统计信息
 * 
 * 记录单个主题的订阅者数量、消息数量等统计信息。
 * 
 * @author Nan
 * @since 1.0.0
 */
@Getter
public class TopicSubscriptionStats {
    
    /**
     * 主题路径
     */
    private final String topicPath;
    
    /**
     * 订阅者数量
     */
    private final AtomicLong subscriberCount = new AtomicLong(0);
    
    /**
     * 消息数量
     */
    private final AtomicLong messageCount = new AtomicLong(0);
    
    /**
     * 创建时间
     */
    private final LocalDateTime createdTime = LocalDateTime.now();
    
    /**
     * 最后更新时间
     */
    private volatile LocalDateTime lastUpdated = LocalDateTime.now();
    
    /**
     * 最后订阅时间
     */
    private volatile LocalDateTime lastSubscribed;
    
    /**
     * 最后消息时间
     */
    private volatile LocalDateTime lastMessage;
    
    public TopicSubscriptionStats(String topicPath) {
        this.topicPath = topicPath;
    }
    
    /**
     * 增加订阅者数量
     */
    public void incrementSubscriberCount() {
        subscriberCount.incrementAndGet();
        lastSubscribed = LocalDateTime.now();
        lastUpdated = LocalDateTime.now();
    }
    
    /**
     * 减少订阅者数量
     */
    public void decrementSubscriberCount() {
        long count = subscriberCount.decrementAndGet();
        if (count < 0) {
            subscriberCount.set(0); // 防止负数
        }
        lastUpdated = LocalDateTime.now();
    }
    
    /**
     * 增加消息数量
     */
    public void incrementMessageCount() {
        messageCount.incrementAndGet();
        lastMessage = LocalDateTime.now();
        lastUpdated = LocalDateTime.now();
    }
    
    /**
     * 获取订阅者数量
     */
    public long getSubscriberCount() {
        return subscriberCount.get();
    }
    
    /**
     * 获取消息数量
     */
    public long getMessageCount() {
        return messageCount.get();
    }
}