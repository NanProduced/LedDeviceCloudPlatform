package org.nan.cloud.message.infrastructure.websocket.routing;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 用户订阅信息
 * 
 * 管理单个用户的所有订阅信息，支持不同层次的订阅管理。
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Getter
public class UserSubscriptionInfo {
    
    /**
     * 用户ID
     */
    private final String userId;
    
    /**
     * 持久订阅集合
     * 这些订阅在用户连接期间始终有效
     */
    private final Set<String> persistentSubscriptions = new CopyOnWriteArraySet<>();
    
    /**
     * 会话订阅映射
     * Key: 会话ID
     * Value: 该会话的订阅主题集合
     */
    private final Map<String, Set<String>> sessionSubscriptions = new ConcurrentHashMap<>();
    
    /**
     * 临时订阅映射
     * Key: 临时订阅ID
     * Value: 订阅信息
     */
    private final Map<String, TemporarySubscription> temporarySubscriptions = new ConcurrentHashMap<>();
    
    /**
     * 订阅历史统计
     */
    private final Map<String, SubscriptionStats> subscriptionStats = new ConcurrentHashMap<>();
    
    /**
     * 创建时间
     */
    private final LocalDateTime createdTime = LocalDateTime.now();
    
    /**
     * 最后更新时间
     */
    private volatile LocalDateTime lastUpdated = LocalDateTime.now();
    
    public UserSubscriptionInfo(String userId) {
        this.userId = userId;
    }
    
    /**
     * 添加订阅
     * 
     * @param topic 主题路径
     * @param level 订阅层次
     * @param sessionId 会话ID（对于会话级别订阅）
     */
    public synchronized void addSubscription(String topic, SubscriptionLevel level, String sessionId) {
        try {
            switch (level) {
                case PERSISTENT:
                case GLOBAL:
                    persistentSubscriptions.add(topic);
                    log.debug("添加持久订阅 - 用户: {}, 主题: {}", userId, topic);
                    break;
                    
                case SESSION:
                case PAGE:
                    if (sessionId != null) {
                        sessionSubscriptions.computeIfAbsent(sessionId, k -> new CopyOnWriteArraySet<>())
                                           .add(topic);
                        log.debug("添加会话订阅 - 用户: {}, 会话: {}, 主题: {}", userId, sessionId, topic);
                    } else {
                        log.warn("会话级别订阅缺少会话ID - 用户: {}, 主题: {}", userId, topic);
                    }
                    break;
                    
                case TEMPORARY:
                    String tempId = generateTemporarySubscriptionId();
                    temporarySubscriptions.put(tempId, new TemporarySubscription(topic, LocalDateTime.now()));
                    log.debug("添加临时订阅 - 用户: {}, 主题: {}, 临时ID: {}", userId, topic, tempId);
                    break;
                    
                default:
                    log.warn("未知的订阅层次 - 用户: {}, 主题: {}, 层次: {}", userId, topic, level);
                    return;
            }
            
            // 更新统计信息
            updateSubscriptionStats(topic);
            lastUpdated = LocalDateTime.now();
            
        } catch (Exception e) {
            log.error("添加订阅失败 - 用户: {}, 主题: {}, 层次: {}, 错误: {}", 
                    userId, topic, level, e.getMessage(), e);
        }
    }
    
    /**
     * 移除订阅
     * 
     * @param topic 主题路径
     * @param sessionId 会话ID（可选）
     * @return true表示成功移除，false表示订阅不存在
     */
    public synchronized boolean removeSubscription(String topic, String sessionId) {
        boolean removed = false;
        
        try {
            // 尝试从持久订阅中移除
            if (persistentSubscriptions.remove(topic)) {
                removed = true;
                log.debug("移除持久订阅 - 用户: {}, 主题: {}", userId, topic);
            }
            
            // 尝试从会话订阅中移除
            if (sessionId != null) {
                Set<String> sessionTopics = sessionSubscriptions.get(sessionId);
                if (sessionTopics != null && sessionTopics.remove(topic)) {
                    removed = true;
                    log.debug("移除会话订阅 - 用户: {}, 会话: {}, 主题: {}", userId, sessionId, topic);
                    
                    // 如果会话没有其他订阅，移除会话
                    if (sessionTopics.isEmpty()) {
                        sessionSubscriptions.remove(sessionId);
                    }
                }
            } else {
                // 如果没有指定会话ID，从所有会话中移除
                for (Set<String> sessionTopics : sessionSubscriptions.values()) {
                    if (sessionTopics.remove(topic)) {
                        removed = true;
                    }
                }
            }
            
            // 尝试从临时订阅中移除
            temporarySubscriptions.entrySet().removeIf(entry -> {
                if (entry.getValue().getTopic().equals(topic)) {
                    log.debug("移除临时订阅 - 用户: {}, 主题: {}, 临时ID: {}", userId, topic, entry.getKey());
                    return true;
                }
                return false;
            });
            
            if (removed) {
                lastUpdated = LocalDateTime.now();
            }
            
        } catch (Exception e) {
            log.error("移除订阅失败 - 用户: {}, 主题: {}, 会话: {}, 错误: {}", 
                    userId, topic, sessionId, e.getMessage(), e);
        }
        
        return removed;
    }
    
    /**
     * 移除指定会话的所有订阅
     * 
     * @param sessionId 会话ID
     * @return 被移除的主题列表
     */
    public synchronized List<String> removeSessionSubscriptions(String sessionId) {
        List<String> removedTopics = new ArrayList<>();
        
        try {
            Set<String> sessionTopics = sessionSubscriptions.remove(sessionId);
            if (sessionTopics != null) {
                removedTopics.addAll(sessionTopics);
                log.debug("移除会话所有订阅 - 用户: {}, 会话: {}, 主题数: {}", 
                        userId, sessionId, sessionTopics.size());
            }
            
            if (!removedTopics.isEmpty()) {
                lastUpdated = LocalDateTime.now();
            }
            
        } catch (Exception e) {
            log.error("移除会话订阅失败 - 用户: {}, 会话: {}, 错误: {}", userId, sessionId, e.getMessage(), e);
        }
        
        return removedTopics;
    }
    
    /**
     * 清理过期的临时订阅
     * 
     * @param expiredBefore 过期时间点
     * @return 被清理的订阅数量
     */
    public synchronized int cleanupExpiredTemporarySubscriptions(LocalDateTime expiredBefore) {
        int cleanedCount = 0;
        
        try {
            Iterator<Map.Entry<String, TemporarySubscription>> iterator = 
                    temporarySubscriptions.entrySet().iterator();
            
            while (iterator.hasNext()) {
                Map.Entry<String, TemporarySubscription> entry = iterator.next();
                if (entry.getValue().getCreatedTime().isBefore(expiredBefore)) {
                    iterator.remove();
                    cleanedCount++;
                    log.debug("清理过期临时订阅 - 用户: {}, 主题: {}, 临时ID: {}", 
                            userId, entry.getValue().getTopic(), entry.getKey());
                }
            }
            
            if (cleanedCount > 0) {
                lastUpdated = LocalDateTime.now();
                log.info("清理过期临时订阅完成 - 用户: {}, 清理数量: {}", userId, cleanedCount);
            }
            
        } catch (Exception e) {
            log.error("清理过期临时订阅失败 - 用户: {}, 错误: {}", userId, e.getMessage(), e);
        }
        
        return cleanedCount;
    }
    
    /**
     * 检查是否订阅了指定主题
     * 
     * @param topic 主题路径
     * @return true表示已订阅，false表示未订阅
     */
    public boolean isSubscribedTo(String topic) {
        // 检查持久订阅
        if (persistentSubscriptions.contains(topic)) {
            return true;
        }
        
        // 检查会话订阅
        for (Set<String> sessionTopics : sessionSubscriptions.values()) {
            if (sessionTopics.contains(topic)) {
                return true;
            }
        }
        
        // 检查临时订阅
        for (TemporarySubscription tempSub : temporarySubscriptions.values()) {
            if (tempSub.getTopic().equals(topic)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取所有已订阅的主题
     * 
     * @return 所有订阅主题的集合
     */
    public Set<String> getAllSubscribedTopics() {

        // 添加持久订阅
        Set<String> allTopics = new HashSet<>(persistentSubscriptions);
        
        // 添加会话订阅
        for (Set<String> sessionTopics : sessionSubscriptions.values()) {
            allTopics.addAll(sessionTopics);
        }
        
        // 添加临时订阅
        for (TemporarySubscription tempSub : temporarySubscriptions.values()) {
            allTopics.add(tempSub.getTopic());
        }
        
        return allTopics;
    }
    
    /**
     * 获取指定会话的订阅主题
     * 
     * @param sessionId 会话ID
     * @return 会话订阅的主题集合
     */
    public Set<String> getSessionSubscribedTopics(String sessionId) {
        Set<String> sessionTopics = sessionSubscriptions.get(sessionId);
        return sessionTopics != null ? new HashSet<>(sessionTopics) : Collections.emptySet();
    }
    
    /**
     * 获取订阅统计信息
     * 
     * @return 订阅统计信息
     */
    public UserSubscriptionStats getSubscriptionStatistics() {
        return UserSubscriptionStats.builder()
                .userId(userId)
                .persistentSubscriptionCount(persistentSubscriptions.size())
                .sessionSubscriptionCount(sessionSubscriptions.values().stream().mapToInt(Set::size).sum())
                .temporarySubscriptionCount(temporarySubscriptions.size())
                .totalTopicsSubscribed(getAllSubscribedTopics().size())
                .createdTime(createdTime)
                .lastUpdated(lastUpdated)
                .build();
    }
    
    // ==================== 私有工具方法 ====================
    
    /**
     * 生成临时订阅ID
     */
    private String generateTemporarySubscriptionId() {
        return "temp-" + userId + "-" + System.currentTimeMillis() + "-" + 
               Integer.toHexString(new Random().nextInt());
    }
    
    /**
     * 更新订阅统计信息
     */
    private void updateSubscriptionStats(String topic) {
        SubscriptionStats stats = subscriptionStats.computeIfAbsent(topic, 
                k -> new SubscriptionStats(topic));
        stats.incrementSubscriptionCount();
        stats.updateLastSubscribed(LocalDateTime.now());
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 临时订阅信息
     */
    @Getter
    public static class TemporarySubscription {
        private final String topic;
        private final LocalDateTime createdTime;
        
        public TemporarySubscription(String topic, LocalDateTime createdTime) {
            this.topic = topic;
            this.createdTime = createdTime;
        }
    }
    
    /**
     * 订阅统计信息
     */
    @Getter
    public static class SubscriptionStats {
        private final String topic;
        private int subscriptionCount = 0;
        private final LocalDateTime firstSubscribed;
        private LocalDateTime lastSubscribed;
        
        public SubscriptionStats(String topic) {
            this.topic = topic;
            this.firstSubscribed = LocalDateTime.now();
        }
        
        public void incrementSubscriptionCount() {
            this.subscriptionCount++;
        }
        
        public void updateLastSubscribed(LocalDateTime time) {
            this.lastSubscribed = time;
        }
    }
}