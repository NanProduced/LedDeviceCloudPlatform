package org.nan.cloud.message.infrastructure.websocket.routing.enhanced;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.infrastructure.websocket.stomp.enums.StompTopic;
import org.nan.cloud.message.infrastructure.websocket.stomp.model.CommonStompMessage;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 增强版Topic管理器
 * 
 * 核心职责：
 * 1. 动态Topic创建和管理
 * 2. Topic通配符支持和匹配
 * 3. 层次化Topic结构管理
 * 4. Topic生命周期管理
 * 
 * 增强功能：
 * - 通配符支持：* 和 ** 通配符匹配
 * - 动态创建：基于业务规则动态生成Topic
 * - 层次化管理：支持多级Topic结构
 * - 生命周期：自动清理未使用的Topic
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnhancedTopicManager {
    
    /**
     * 动态Topic缓存
     * Key: Topic模式, Value: Topic信息
     */
    private final Map<String, TopicInfo> dynamicTopics = new ConcurrentHashMap<>();
    
    /**
     * Topic通配符模式缓存
     * Key: 通配符模式, Value: 编译后的正则表达式
     */
    private final Map<String, Pattern> wildcardPatterns = new ConcurrentHashMap<>();
    
    /**
     * Topic订阅统计
     * Key: Topic路径, Value: 订阅数量
     */
    private final Map<String, Integer> topicSubscriptions = new ConcurrentHashMap<>();
    
    /**
     * 生成动态Topic列表
     * 
     * @param message STOMP消息
     * @return 动态生成的Topic列表
     */
    public List<String> generateDynamicTopics(CommonStompMessage message) {
        try {
            log.debug("生成动态Topic - 消息ID: {}, 类型: {}", message.getMessageId(), message.getMessageType());
            
            List<String> topics = new ArrayList<>();
            
            // 基于消息类型生成Topic
            topics.addAll(generateTopicsByMessageType(message));
            
            // 基于目标信息生成Topic
            topics.addAll(generateTopicsByTarget(message));
            
            // 基于内容生成Topic
            topics.addAll(generateTopicsByContent(message));
            
            // 基于时间生成Topic
            topics.addAll(generateTopicsByTime(message));
            
            // 去重并记录
            List<String> uniqueTopics = topics.stream().distinct().toList();
            
            // 更新Topic信息
            for (String topic : uniqueTopics) {
                updateTopicInfo(topic, message);
            }
            
            log.debug("动态Topic生成完成 - 消息ID: {}, Topic数: {}", 
                    message.getMessageId(), uniqueTopics.size());
            
            return uniqueTopics;
            
        } catch (Exception e) {
            log.error("生成动态Topic失败 - 消息ID: {}, 错误: {}", message.getMessageId(), e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 匹配通配符Topic
     * 
     * @param topicPattern 通配符模式
     * @param actualTopic 实际Topic路径
     * @return 是否匹配
     */
    public boolean matchWildcardTopic(String topicPattern, String actualTopic) {
        try {
            Pattern pattern = wildcardPatterns.computeIfAbsent(topicPattern, this::compileWildcardPattern);
            return pattern.matcher(actualTopic).matches();
            
        } catch (Exception e) {
            log.warn("通配符Topic匹配失败 - 模式: {}, Topic: {}, 错误: {}", 
                    topicPattern, actualTopic, e.getMessage());
            return false;
        }
    }
    
    /**
     * 创建层次化Topic
     * 
     * @param baseTopicPath 基础Topic路径
     * @param hierarchyLevels 层次级别
     * @return 层次化Topic列表
     */
    public List<String> createHierarchicalTopics(String baseTopicPath, String... hierarchyLevels) {
        List<String> topics = new ArrayList<>();
        
        StringBuilder currentPath = new StringBuilder(baseTopicPath);
        topics.add(currentPath.toString());
        
        for (String level : hierarchyLevels) {
            currentPath.append("/").append(level);
            topics.add(currentPath.toString());
        }
        
        log.debug("创建层次化Topic - 基础路径: {}, 层次数: {}", baseTopicPath, topics.size());
        
        return topics;
    }
    
    /**
     * 获取Topic统计信息
     * 
     * @return Topic统计
     */
    public TopicStats getTopicStats() {
        return TopicStats.builder()
                .totalDynamicTopics(dynamicTopics.size())
                .totalWildcardPatterns(wildcardPatterns.size())
                .totalSubscriptions(topicSubscriptions.values().stream().mapToInt(Integer::intValue).sum())
                .activeTopics(topicSubscriptions.size())
                .build();
    }
    
    /**
     * 清理未使用的Topic
     * 
     * @param maxIdleTimeMs 最大空闲时间（毫秒）
     */
    public void cleanupUnusedTopics(long maxIdleTimeMs) {
        try {
            long currentTime = System.currentTimeMillis();
            List<String> topicsToRemove = new ArrayList<>();
            
            for (Map.Entry<String, TopicInfo> entry : dynamicTopics.entrySet()) {
                TopicInfo info = entry.getValue();
                if ((currentTime - info.getLastAccessTime()) > maxIdleTimeMs && 
                    topicSubscriptions.getOrDefault(entry.getKey(), 0) == 0) {
                    topicsToRemove.add(entry.getKey());
                }
            }
            
            for (String topic : topicsToRemove) {
                dynamicTopics.remove(topic);
                topicSubscriptions.remove(topic);
            }
            
            if (!topicsToRemove.isEmpty()) {
                log.info("清理未使用Topic - 清理数量: {}", topicsToRemove.size());
            }
            
        } catch (Exception e) {
            log.error("清理未使用Topic失败 - 错误: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 注册Topic订阅
     * 
     * @param topicPath Topic路径
     */
    public void registerTopicSubscription(String topicPath) {
        topicSubscriptions.merge(topicPath, 1, Integer::sum);
        log.debug("注册Topic订阅 - Topic: {}, 订阅数: {}", topicPath, topicSubscriptions.get(topicPath));
    }
    
    /**
     * 取消Topic订阅
     * 
     * @param topicPath Topic路径
     */
    public void unregisterTopicSubscription(String topicPath) {
        topicSubscriptions.computeIfPresent(topicPath, (k, v) -> v > 1 ? v - 1 : null);
        log.debug("取消Topic订阅 - Topic: {}, 剩余订阅数: {}", 
                topicPath, topicSubscriptions.getOrDefault(topicPath, 0));
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 基于消息类型生成Topic
     */
    private List<String> generateTopicsByMessageType(CommonStompMessage message) {
        List<String> topics = new ArrayList<>();
        
        switch (message.getMessageType()) {
            case ALERT:
                topics.add("/topic/alerts/global");
                if (message.getTarget() != null && message.getTarget().getOid() != null) {
                    topics.add("/topic/alerts/org/" + message.getTarget().getOid());
                }
                break;
                
            case NOTIFICATION:
                topics.add("/topic/notifications/global");
                if (message.getSubType_1() != null) {
                    topics.add("/topic/notifications/" + message.getSubType_1().toLowerCase());
                }
                break;
                
            case TERMINAL_STATUS_CHANGE:
                topics.add("/topic/terminals/status/changes");
                if (message.getSource() != null && message.getSource().getResourceId() != null) {
                    topics.add("/topic/terminal/" + message.getSource().getResourceId() + "/status");
                }
                break;
                
            case TASK_PROGRESS:
                topics.add("/topic/tasks/progress");
                if (message.getSource() != null && message.getSource().getTaskId() != null) {
                    topics.add("/topic/task/" + message.getSource().getTaskId() + "/progress");
                }
                break;
                
            case COMMAND_FEEDBACK:
                topics.add("/topic/commands/feedback");
                break;
                
            default:
                topics.add("/topic/general/messages");
                break;
        }
        
        return topics;
    }
    
    /**
     * 基于目标信息生成Topic
     */
    private List<String> generateTopicsByTarget(CommonStompMessage message) {
        List<String> topics = new ArrayList<>();
        
        if (message.getTarget() == null) {
            return topics;
        }
        
        // 基于用户ID生成Topic
        if (message.getTarget().getUids() != null) {
            topics.add(StompTopic.USER_MESSAGES_QUEUE);
        }
        
        // 基于组织ID生成Topic
        if (message.getTarget().getOid() != null) {
            topics.add(StompTopic.buildOrgTopic(message.getTarget().getOid().toString()));
            
            // 生成组织层次Topic
            topics.addAll(generateOrgHierarchicalTopics(message.getTarget().getOid()));
        }
        
        // 基于目标类型生成Topic
        if (message.getTarget().getTargetType() != null) {
            topics.add("/topic/targets/" + message.getTarget().getTargetType().toLowerCase());
        }
        
        return topics;
    }
    
    /**
     * 基于内容生成Topic
     */
    private List<String> generateTopicsByContent(CommonStompMessage message) {
        List<String> topics = new ArrayList<>();
        
        // 基于消息内容关键词生成Topic
        if (message.getMessage() != null) {
            String messageText = message.getMessage().toLowerCase();
            
            if (messageText.contains("urgent") || messageText.contains("紧急")) {
                topics.add("/topic/urgent/messages");
            }
            
            if (messageText.contains("error") || messageText.contains("错误")) {
                topics.add("/topic/errors/messages");
            }
            
            if (messageText.contains("warning") || messageText.contains("警告")) {
                topics.add("/topic/warnings/messages");
            }
            
            if (messageText.contains("success") || messageText.contains("成功")) {
                topics.add("/topic/success/messages");
            }
        }
        
        // 基于载荷内容生成Topic
        if (message.getPayload() != null) {
            topics.addAll(generateTopicsByPayload((Map<String, Object>) message.getPayload()));
        }
        
        return topics;
    }
    
    /**
     * 基于时间生成Topic
     */
    private List<String> generateTopicsByTime(CommonStompMessage message) {
        List<String> topics = new ArrayList<>();
        
        if (message.getTimestamp() != null) {
            int hour = message.getTimestamp().getHour();
            int dayOfWeek = message.getTimestamp().getDayOfWeek().getValue();
            
            // 基于小时生成Topic
            topics.add("/topic/time/hour/" + hour);
            
            // 基于星期生成Topic
            topics.add("/topic/time/weekday/" + dayOfWeek);
            
            // 基于工作时间/非工作时间生成Topic
            if (hour >= 9 && hour < 18 && dayOfWeek <= 5) {
                topics.add("/topic/time/business-hours");
            } else {
                topics.add("/topic/time/non-business-hours");
            }
        }
        
        return topics;
    }
    
    /**
     * 基于载荷生成Topic
     */
    private List<String> generateTopicsByPayload(Map<String, Object> payload) {
        List<String> topics = new ArrayList<>();
        
        // 基于载荷中的特定字段生成Topic
        if (payload.containsKey("category")) {
            String category = payload.get("category").toString();
            topics.add("/topic/category/" + category);
        }
        
        if (payload.containsKey("priority")) {
            String priority = payload.get("priority").toString();
            topics.add("/topic/priority/" + priority.toLowerCase());
        }
        
        if (payload.containsKey("source")) {
            String source = payload.get("source").toString();
            topics.add("/topic/source/" + source);
        }
        
        return topics;
    }
    
    /**
     * 生成组织层次Topic
     */
    private List<String> generateOrgHierarchicalTopics(Long orgId) {
        List<String> topics = new ArrayList<>();
        
        // 简单的组织层次模拟
        topics.add("/topic/org/" + orgId + "/all");
        topics.add("/topic/org/" + orgId + "/announcements");
        topics.add("/topic/org/" + orgId + "/notifications");
        
        // 基于组织ID生成父级组织Topic
        if (orgId > 1000) {
            Long parentOrgId = orgId / 10;
            topics.add("/topic/org/" + parentOrgId + "/children");
        }
        
        return topics;
    }
    
    /**
     * 编译通配符模式为正则表达式
     */
    private Pattern compileWildcardPattern(String wildcardPattern) {
        String regex = wildcardPattern
                .replace(".", "\\.")
                .replace("**", ".*")
                .replace("*", "[^/]*");
        
        return Pattern.compile(regex);
    }
    
    /**
     * 更新Topic信息
     */
    private void updateTopicInfo(String topicPath, CommonStompMessage message) {
        TopicInfo info = dynamicTopics.computeIfAbsent(topicPath, k -> new TopicInfo(k));
        info.updateAccess();
        info.incrementMessageCount();
        
        // 记录消息类型统计
        info.addMessageType(message.getMessageType().name());
    }
    
    // ==================== 内部类 ====================
    
    /**
     * Topic信息
     */
    public static class TopicInfo {
        private final String topicPath;
        private volatile long createdTime;
        private volatile long lastAccessTime;
        private volatile int messageCount;
        private final Set<String> messageTypes;
        
        public TopicInfo(String topicPath) {
            this.topicPath = topicPath;
            this.createdTime = System.currentTimeMillis();
            this.lastAccessTime = System.currentTimeMillis();
            this.messageCount = 0;
            this.messageTypes = ConcurrentHashMap.newKeySet();
        }
        
        public void updateAccess() {
            this.lastAccessTime = System.currentTimeMillis();
        }
        
        public void incrementMessageCount() {
            this.messageCount++;
        }
        
        public void addMessageType(String messageType) {
            this.messageTypes.add(messageType);
        }
        
        // Getters
        public String getTopicPath() { return topicPath; }
        public long getCreatedTime() { return createdTime; }
        public long getLastAccessTime() { return lastAccessTime; }
        public int getMessageCount() { return messageCount; }
        public Set<String> getMessageTypes() { return new HashSet<>(messageTypes); }
    }
    
    /**
     * Topic统计
     */
    public static class TopicStats {
        private int totalDynamicTopics;
        private int totalWildcardPatterns;
        private int totalSubscriptions;
        private int activeTopics;
        
        public static TopicStatsBuilder builder() {
            return new TopicStatsBuilder();
        }
        
        // Getters
        public int getTotalDynamicTopics() { return totalDynamicTopics; }
        public int getTotalWildcardPatterns() { return totalWildcardPatterns; }
        public int getTotalSubscriptions() { return totalSubscriptions; }
        public int getActiveTopics() { return activeTopics; }
        
        public static class TopicStatsBuilder {
            private TopicStats stats = new TopicStats();
            
            public TopicStatsBuilder totalDynamicTopics(int total) {
                stats.totalDynamicTopics = total;
                return this;
            }
            
            public TopicStatsBuilder totalWildcardPatterns(int total) {
                stats.totalWildcardPatterns = total;
                return this;
            }
            
            public TopicStatsBuilder totalSubscriptions(int total) {
                stats.totalSubscriptions = total;
                return this;
            }
            
            public TopicStatsBuilder activeTopics(int active) {
                stats.activeTopics = active;
                return this;
            }
            
            public TopicStats build() {
                return stats;
            }
        }
    }
}