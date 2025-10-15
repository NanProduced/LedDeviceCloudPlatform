package org.nan.cloud.message.infrastructure.websocket.routing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.api.stomp.StompMessageTypes;
import org.nan.cloud.message.api.stomp.StompResourceType;
import org.nan.cloud.message.infrastructure.websocket.stomp.enums.StompTopic;
import org.nan.cloud.message.api.stomp.CommonStompMessage;
import org.nan.cloud.message.infrastructure.websocket.manager.StompConnectionManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Topic路由管理器
 * 
 * 核心职责：
 * 1. 智能路由决策 - 根据消息类型和目标动态选择最优主题路径
 * 2. 层次化订阅管理 - 支持全局、页面、临时三层订阅模式  
 * 3. 路由规则引擎 - 可配置的消息路由策略
 * 4. 订阅生命周期管理 - 技术层面的订阅创建、维护、清理机制
 * 
 * 设计原则：
 * - 基于明确规则的路由决策，不依赖用户偏好或推荐算法
 * - 所有主题路径和订阅规则都是预定义的，遵循业务逻辑
 * - 前端SPA和后端服务协同决定订阅策略
 * - 作为技术基础设施，实现细节对最终用户不可见
 * 
 * 路由层次结构：
 * - Organization Level: 组织级别主题，组织内用户可见
 * - User Level: 用户级别主题，个人专属
 * - Terminal Level: 终端级别主题，有权限的用户可见
 * - Task Level: 任务级别主题，相关用户可见
 * - System Level: 系统级别主题，按权限控制可见性
 * 
 * 简化的订阅生命周期：
 * - Session: 会话订阅，会话期间有效，适用于大部分场景
 * - Temporary: 临时订阅，特定操作期间有效，适用于任务和批量操作
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
public class TopicRoutingManager {
    
    private final StompConnectionManager stompConnectionManager;
    
    public TopicRoutingManager(@Lazy StompConnectionManager stompConnectionManager) {
        this.stompConnectionManager = stompConnectionManager;
    }
    
    // ==================== 路由规则存储 ====================
    
    /**
     * 消息类型到主题路径的路由规则映射
     * Key: 消息类型
     * Value: 路由规则列表
     */
    private final Map<StompMessageTypes, List<TopicRoutingRule>> messageTypeRoutes = new ConcurrentHashMap<>();
    
    /**
     * 用户订阅的主题映射
     * Key: 用户ID
     * Value: 订阅的主题集合（按层次分类）
     */
    private final Map<String, UserSubscriptionInfo> userSubscriptions = new ConcurrentHashMap<>();
    
    /**
     * 主题的订阅者统计
     * Key: 主题路径
     * Value: 订阅统计信息
     */
    private final Map<String, TopicSubscriptionStats> topicStats = new ConcurrentHashMap<>();
    
    // ==================== 初始化路由规则 ====================
    
    /**
     * 初始化默认路由规则
     * 在Bean初始化后自动调用
     */
    @jakarta.annotation.PostConstruct
    public void initializeDefaultRoutes() {
        log.info("初始化TopicRoutingManager默认路由规则");
        
        // 通知消息路由规则
        addRoutingRule(StompMessageTypes.NOTIFICATION, 
                       createUserNotificationRule(),
                       createOrgNotificationRule());
        
        // 终端状态变更路由规则
        addRoutingRule(StompMessageTypes.TERMINAL_STATUS,
                       createTerminalStatusRule());
        
        // 指令反馈路由规则
        addRoutingRule(StompMessageTypes.COMMAND_FEEDBACK,
                       createCommandFeedbackRule());
        
        // 任务进度路由规则
        addRoutingRule(StompMessageTypes.TASK_PROGRESS,
                       createTaskProgressRule());
        
        // 系统消息路由规则
        addRoutingRule(StompMessageTypes.SYSTEM_MESSAGE,
                       createSystemMessageRule());
        
        // 监控数据路由规则
        
        log.info("✅ TopicRoutingManager路由规则初始化完成 - 消息类型数: {}", messageTypeRoutes.size());
    }
    
    // ==================== 智能路由决策 ====================
    
    /**
     * 根据消息内容决定路由策略
     * 
     * @param message STOMP消息
     * @return 路由决策结果
     */
    public TopicRoutingDecision decideRouting(CommonStompMessage message) {
        try {
            log.debug("开始路由决策 - 消息类型: {}, 消息ID: {}", message.getMessageType(), message.getMessageId());
            
            // 获取该消息类型的路由规则
            List<TopicRoutingRule> rules = messageTypeRoutes.get(message.getMessageType());
            if (rules == null || rules.isEmpty()) {
                log.warn("未找到消息类型的路由规则 - 类型: {}", message.getMessageType());
                return createFallbackRoutingDecision(message);
            }
            
            // 遍历规则，找到匹配的路由
            for (TopicRoutingRule rule : rules) {
                if (rule.matches(message)) {
                    TopicRoutingDecision decision = rule.apply(message);
                    
                    log.debug("路由规则匹配成功 - 规则: {}, 目标主题数: {}", 
                            rule.getRuleName(), decision.getTargetTopics().size());
                    
                    // 更新主题统计信息
                    updateTopicStats(decision.getTargetTopics());
                    
                    return decision;
                }
            }
            
            log.warn("没有路由规则匹配消息 - 消息类型: {}, 使用回退策略", message.getMessageType());
            return createFallbackRoutingDecision(message);
            
        } catch (Exception e) {
            log.error("路由决策失败 - 消息ID: {}, 错误: {}", message.getMessageId(), e.getMessage(), e);
            return createFallbackRoutingDecision(message);
        }
    }
    
    
    // ==================== 订阅管理 ====================
    
    /**
     * 注册用户订阅
     * 
     * @param userId 用户ID
     * @param topic 订阅主题
     * @param subscriptionLevel 订阅层次
     * @param sessionId 会话ID（可选）
     * @return true表示成功添加新订阅，false表示订阅已存在
     */
    public boolean registerUserSubscription(String userId, String topic, SubscriptionLevel subscriptionLevel, String sessionId) {
        try {
            UserSubscriptionInfo subscriptionInfo = userSubscriptions.computeIfAbsent(userId, 
                    k -> new UserSubscriptionInfo(userId));
            
            // 在添加新订阅前，清理无效会话的订阅（核心修复）
            cleanupInvalidSessionsForUser(userId);
            
            boolean added = subscriptionInfo.addSubscription(topic, subscriptionLevel, sessionId);
            
            // 只有在真正添加了新订阅时才更新主题订阅统计
            if (added) {
                TopicSubscriptionStats stats = topicStats.computeIfAbsent(topic, 
                        k -> new TopicSubscriptionStats(topic));
                stats.incrementSubscriberCount();
                
                log.debug("注册用户订阅 - 用户ID: {}, 主题: {}, 层次: {}, 会话: {}", 
                        userId, topic, subscriptionLevel, sessionId);
            } else {
                log.debug("用户订阅已存在 - 用户ID: {}, 主题: {}, 层次: {}, 会话: {}", 
                        userId, topic, subscriptionLevel, sessionId);
            }
            
            return added;
                    
        } catch (Exception e) {
            log.error("注册用户订阅失败 - 用户ID: {}, 主题: {}, 错误: {}", userId, topic, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 移除用户订阅
     * 
     * @param userId 用户ID
     * @param topic 取消订阅的主题
     * @param sessionId 会话ID（可选，用于移除特定会话的订阅）
     */
    public void removeUserSubscription(String userId, String topic, String sessionId) {
        try {
            UserSubscriptionInfo subscriptionInfo = userSubscriptions.get(userId);
            if (subscriptionInfo != null) {
                boolean removed = subscriptionInfo.removeSubscription(topic, sessionId);
                
                if (removed) {
                    // 更新主题订阅统计
                    TopicSubscriptionStats stats = topicStats.get(topic);
                    if (stats != null) {
                        stats.decrementSubscriberCount();
                    }
                    
                    log.debug("移除用户订阅 - 用户ID: {}, 主题: {}, 会话: {}", userId, topic, sessionId);
                }
            }
            
        } catch (Exception e) {
            log.error("移除用户订阅失败 - 用户ID: {}, 主题: {}, 错误: {}", userId, topic, e.getMessage(), e);
        }
    }
    
    /**
     * 清理用户的所有会话订阅
     * 在用户断开连接时调用
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     */
    public void cleanupUserSessionSubscriptions(String userId, String sessionId) {
        try {
            UserSubscriptionInfo subscriptionInfo = userSubscriptions.get(userId);
            if (subscriptionInfo != null) {
                List<String> removedTopics = subscriptionInfo.removeSessionSubscriptions(sessionId);
                
                // 更新主题统计
                for (String topic : removedTopics) {
                    TopicSubscriptionStats stats = topicStats.get(topic);
                    if (stats != null) {
                        stats.decrementSubscriberCount();
                    }
                }
                
                log.info("清理用户会话订阅 - 用户ID: {}, 会话ID: {}, 清理主题数: {}", 
                        userId, sessionId, removedTopics.size());
            }
            
        } catch (Exception e) {
            log.error("清理用户会话订阅失败 - 用户ID: {}, 会话ID: {}, 错误: {}", userId, sessionId, e.getMessage(), e);
        }
    }
    
    /**
     * 获取用户的所有订阅主题
     * 
     * @param userId 用户ID
     * @return 用户订阅的主题集合
     */
    public Set<String> getUserSubscribedTopics(String userId) {
        UserSubscriptionInfo subscriptionInfo = userSubscriptions.get(userId);
        return subscriptionInfo != null ? subscriptionInfo.getAllSubscribedTopics() : Collections.emptySet();
    }
    
    /**
     * 检查用户是否订阅了指定主题
     * 
     * @param userId 用户ID
     * @param topic 主题路径
     * @return true表示已订阅，false表示未订阅
     */
    public boolean isUserSubscribedToTopic(String userId, String topic) {
        UserSubscriptionInfo subscriptionInfo = userSubscriptions.get(userId);
        return subscriptionInfo != null && subscriptionInfo.isSubscribedTo(topic);
    }
    
    // ==================== 主题统计和优化 ====================
    
    /**
     * 获取主题订阅统计信息
     * 
     * @param topic 主题路径
     * @return 主题统计信息
     */
    public TopicSubscriptionStats getTopicStats(String topic) {
        return topicStats.get(topic);
    }
    
    
    /**
     * 获取路由统计信息
     * 
     * @return 路由统计信息
     */
    public TopicRoutingStats getRoutingStats() {
        return TopicRoutingStats.builder()
                .totalRoutingRules(messageTypeRoutes.values().stream().mapToInt(List::size).sum())
                .totalSubscribedUsers(userSubscriptions.size())
                .totalTopics(topicStats.size())
                .build();
    }
    
    /**
     * 清理指定用户的无效会话订阅
     * 
     * @param userId 用户ID
     */
    private void cleanupInvalidSessionsForUser(String userId) {
        try {
            UserSubscriptionInfo subscriptionInfo = userSubscriptions.get(userId);
            if (subscriptionInfo != null) {
                // 获取当前活跃的会话ID
                Set<String> activeSessionIds = stompConnectionManager.getAllActiveSessionIds();
                
                // 清理无效会话订阅
                int cleanedCount = subscriptionInfo.cleanupInvalidSessions(activeSessionIds);
                
                if (cleanedCount > 0) {
                    // 更新主题统计（减少订阅者数量）
                    // 注意：这里简化处理，实际应该根据具体清理的主题来更新
                    log.debug("用户 {} 清理了 {} 个无效会话订阅", userId, cleanedCount);
                }
            }
            
        } catch (Exception e) {
            log.warn("清理用户无效会话订阅失败 - 用户ID: {}, 错误: {}", userId, e.getMessage());
            // 清理失败不应该影响正常订阅流程，只记录警告
        }
    }
    
    /**
     * 批量清理所有用户的无效会话订阅
     * 可用于定期清理任务
     * 
     * @return 总清理的订阅数量
     */
    public int cleanupAllInvalidSessions() {
        int totalCleaned = 0;
        Set<String> activeSessionIds = stompConnectionManager.getAllActiveSessionIds();
        
        for (UserSubscriptionInfo subscriptionInfo : userSubscriptions.values()) {
            try {
                int cleaned = subscriptionInfo.cleanupInvalidSessions(activeSessionIds);
                totalCleaned += cleaned;
            } catch (Exception e) {
                log.warn("清理用户 {} 的无效会话订阅失败: {}", subscriptionInfo.getUserId(), e.getMessage());
            }
        }
        
        if (totalCleaned > 0) {
            log.info("✅ 批量清理无效会话订阅完成 - 总清理数量: {}", totalCleaned);
        }
        
        return totalCleaned;
    }
    
    // ==================== 私有工具方法 ====================
    
    /**
     * 添加路由规则
     */
    private void addRoutingRule(StompMessageTypes messageType, TopicRoutingRule... rules) {
        messageTypeRoutes.computeIfAbsent(messageType, k -> new ArrayList<>())
                         .addAll(Arrays.asList(rules));
    }
    
    /**
     * 创建用户通知路由规则
     * Context中存在Uid
     */
    private TopicRoutingRule createUserNotificationRule() {
        return TopicRoutingRule.builder()
                .ruleName("USER_NOTIFICATION")
                .priority(1)
                .matcher(message -> "USER".equals(message.getSubType_1()))
                .topicGenerator(message -> {
                    if (message.getContext() != null && message.getContext().getUid() != null) {
                        return List.of(StompTopic.USER_MESSAGES_QUEUE);
                    }
                    return Collections.emptyList();
                })
                .build();
    }
    
    /**
     * 创建组织通知路由规则
     */
    private TopicRoutingRule createOrgNotificationRule() {
        return TopicRoutingRule.builder()
                .ruleName("ORG_NOTIFICATION")
                .priority(2)
                .matcher(message -> "ORG".equals(message.getSubType_1()))
                .topicGenerator(message -> {
                    if (message.getOid() != null) {
                        return List.of(StompTopic.buildOrgTopic(message.getOid().toString()));
                    }
                    return Collections.emptyList();
                })
                .build();
    }
    
    /**
     * 创建终端状态路由规则
     */
    private TopicRoutingRule createTerminalStatusRule() {
        return TopicRoutingRule.builder()
                .ruleName("TERMINAL_STATUS")
                .priority(1)
                .matcher(message -> message.getContext().getResourceType().equals(StompResourceType.TERMINAL))
                .topicGenerator(message -> {
                    if (message.getContext().getTid() != null) {
                        return List.of(StompTopic.buildDeviceTopic(message.getContext().getTid().toString()));
                    }
                    return Collections.emptyList();
                })
                .build();
    }
    
    /**
     * 创建指令反馈路由规则
     */
    private TopicRoutingRule createCommandFeedbackRule() {
        return TopicRoutingRule.builder()
                .ruleName("COMMAND_FEEDBACK")
                .priority(1)
                .matcher(message -> true) // 所有指令反馈都匹配
                .topicGenerator(message -> {
                    if (message.getContext() != null && message.getContext().getUid() != null) {
                        return List.of(StompTopic.USER_MESSAGES_QUEUE);
                    }
                    return Collections.emptyList();
                })
                .build();
    }
    
    /**
     * 创建任务进度路由规则
     */
    private TopicRoutingRule createTaskProgressRule() {
        return TopicRoutingRule.builder()
                .ruleName("TASK_PROGRESS")
                .priority(1)
                .matcher(message -> true)
                .topicGenerator(message -> {
                    String taskId = message.getContext().getTaskId();
                    if (taskId != null) {
                        return List.of(StompTopic.buildBatchAggTopic(taskId));
                    }
                    return Collections.emptyList();
                })
                .build();
    }
    
    /**
     * 创建系统消息路由规则
     */
    private TopicRoutingRule createSystemMessageRule() {
        return TopicRoutingRule.builder()
                .ruleName("SYSTEM_MESSAGE")
                .priority(1)
                .matcher(message -> true)
                .topicGenerator(message -> List.of(StompTopic.SYSTEM_TOPIC))
                .build();
    }
    
    /**
     * 创建回退路由决策
     */
    private TopicRoutingDecision createFallbackRoutingDecision(CommonStompMessage message) {
        List<String> fallbackTopics = new ArrayList<>();
        
        // 如果有明确的目标主题路径，使用它
        if (message.getContext().getUid() != null) {
            fallbackTopics.add(StompTopic.USER_MESSAGES_QUEUE);
        } else {
            // 否则使用系统默认主题
            fallbackTopics.add(StompTopic.SYSTEM_TOPIC);
        }
        
        return TopicRoutingDecision.builder()
                .messageId(message.getMessageId())
                .routingStrategy(RoutingStrategy.FALLBACK)
                .targetTopics(fallbackTopics)
                .build();
    }
    
    /**
     * 更新主题统计信息
     */
    private void updateTopicStats(List<String> topics) {
        for (String topic : topics) {
            TopicSubscriptionStats stats = topicStats.computeIfAbsent(topic, 
                    k -> new TopicSubscriptionStats(topic));
            stats.incrementMessageCount();
        }
    }
}