package org.nan.cloud.message.infrastructure.websocket.routing.enhanced;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.infrastructure.websocket.stomp.model.CommonStompMessage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 增强版消息聚合器
 * 
 * 核心职责：
 * 1. 基于时间窗口的消息聚合
 * 2. 基于数量阈值的消息聚合
 * 3. 基于内容相似度的消息聚合
 * 4. 智能触发聚合推送策略
 * 
 * 聚合策略：
 * - 时间窗口聚合：相同主题的消息在时间窗口内聚合
 * - 数量阈值聚合：达到一定数量后触发聚合推送
 * - 内容相似度聚合：相似内容的消息进行合并
 * - 优先级聚合：高优先级消息立即推送，低优先级消息可聚合
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageAggregator {
    
    private final DynamicRoutingEngine dynamicRoutingEngine;
    
    /**
     * 待聚合的消息缓存
     * Key: 聚合键(主题+类型), Value: 消息列表
     */
    private final Map<String, List<CommonStompMessage>> pendingMessages = new ConcurrentHashMap<>();
    
    /**
     * 聚合规则缓存
     * Key: 聚合键, Value: 聚合规则
     */
    private final Map<String, AggregationRule> aggregationRules = new ConcurrentHashMap<>();
    
    /**
     * 最后聚合时间
     * Key: 聚合键, Value: 最后聚合时间戳
     */
    private final Map<String, Long> lastAggregationTimes = new ConcurrentHashMap<>();
    
    /**
     * 聚合统计计数器
     */
    private final AtomicLong totalAggregatedMessages = new AtomicLong(0);
    private final AtomicLong totalAggregationEvents = new AtomicLong(0);
    
    // 配置参数
    private static final long DEFAULT_TIME_WINDOW_MS = 5000;  // 5秒时间窗口
    private static final int DEFAULT_QUANTITY_THRESHOLD = 10; // 10条消息阈值
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.8; // 80%相似度阈值
    
    /**
     * 聚合消息
     * 
     * @param message 待聚合的消息
     * @return 聚合结果
     */
    public AggregationResult aggregateMessage(CommonStompMessage message) {
        try {
            log.debug("开始聚合消息 - 消息ID: {}, 类型: {}", message.getMessageId(), message.getMessageType());
            
            // 生成聚合键
            String aggregationKey = generateAggregationKey(message);
            
            // 获取聚合规则
            AggregationRule rule = getOrCreateAggregationRule(aggregationKey, message);
            
            // 检查是否需要立即推送（高优先级消息）
            if (shouldSkipAggregation(message, rule)) {
                log.debug("消息跳过聚合，立即推送 - 消息ID: {}", message.getMessageId());
                return AggregationResult.immediate(message);
            }
            
            // 添加到待聚合消息列表
            List<CommonStompMessage> messages = pendingMessages.computeIfAbsent(aggregationKey, k -> new ArrayList<>());
            synchronized (messages) {
                messages.add(message);
            }
            
            // 检查是否需要触发聚合
            if (shouldTriggerAggregation(aggregationKey, rule)) {
                return triggerAggregation(aggregationKey, rule);
            }
            
            log.debug("消息已加入聚合队列 - 聚合键: {}, 队列大小: {}", aggregationKey, messages.size());
            return AggregationResult.queued(message);
            
        } catch (Exception e) {
            log.error("聚合消息失败 - 消息ID: {}, 错误: {}", message.getMessageId(), e.getMessage(), e);
            // 失败时直接返回原消息，确保消息不丢失
            return AggregationResult.immediate(message);
        }
    }
    
    /**
     * 获取聚合统计信息
     * 
     * @return 聚合统计
     */
    public AggregationStats getAggregationStats() {
        return AggregationStats.builder()
                .totalAggregatedMessages(totalAggregatedMessages.get())
                .totalAggregationEvents(totalAggregationEvents.get())
                .pendingMessageCount(pendingMessages.values().stream().mapToInt(List::size).sum())
                .activeAggregationKeys(pendingMessages.keySet().size())
                .build();
    }
    
    /**
     * 定期检查超时的聚合消息
     * 每秒执行一次
     */
    @Scheduled(fixedRate = 1000)
    public void checkTimeoutAggregations() {
        try {
            long currentTime = System.currentTimeMillis();
            Set<String> keysToProcess = new HashSet<>();
            
            for (Map.Entry<String, Long> entry : lastAggregationTimes.entrySet()) {
                String aggregationKey = entry.getKey();
                Long lastTime = entry.getValue();
                
                AggregationRule rule = aggregationRules.get(aggregationKey);
                if (rule != null && (currentTime - lastTime) >= rule.getTimeWindowMs()) {
                    keysToProcess.add(aggregationKey);
                }
            }
            
            for (String key : keysToProcess) {
                AggregationRule rule = aggregationRules.get(key);
                if (rule != null) {
                    triggerAggregation(key, rule);
                }
            }
            
            if (!keysToProcess.isEmpty()) {
                log.debug("定期聚合检查完成 - 处理聚合键数: {}", keysToProcess.size());
            }
            
        } catch (Exception e) {
            log.error("定期聚合检查失败 - 错误: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 定期清理过期数据
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredData() {
        try {
            long currentTime = System.currentTimeMillis();
            long expireTime = 24 * 3600 * 1000; // 24小时过期时间
            
            // 清理过期的聚合时间记录
            Set<String> expiredKeys = lastAggregationTimes.entrySet().stream()
                    .filter(entry -> (currentTime - entry.getValue()) > expireTime)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            
            for (String key : expiredKeys) {
                lastAggregationTimes.remove(key);
                aggregationRules.remove(key);
                pendingMessages.remove(key);
            }
            
            if (!expiredKeys.isEmpty()) {
                log.info("清理过期聚合数据 - 清理数量: {}", expiredKeys.size());
            }
            
        } catch (Exception e) {
            log.error("清理过期聚合数据失败 - 错误: {}", e.getMessage(), e);
        }
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 生成聚合键
     */
    private String generateAggregationKey(CommonStompMessage message) {
        // 基于消息类型、子类型和目标组织生成聚合键
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(message.getMessageType());
        
        if (message.getSubType_1() != null) {
            keyBuilder.append(":").append(message.getSubType_1());
        }
        
        if (message.getTarget() != null && message.getTarget().getOid() != null) {
            keyBuilder.append(":org:").append(message.getTarget().getOid());
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * 获取或创建聚合规则
     */
    private AggregationRule getOrCreateAggregationRule(String aggregationKey, CommonStompMessage message) {
        return aggregationRules.computeIfAbsent(aggregationKey, k -> {
            // 根据消息类型创建不同的聚合规则
            AggregationRule rule = new AggregationRule();
            rule.setAggregationKey(k);
            rule.setTimeWindowMs(DEFAULT_TIME_WINDOW_MS);
            rule.setQuantityThreshold(DEFAULT_QUANTITY_THRESHOLD);
            rule.setSimilarityThreshold(DEFAULT_SIMILARITY_THRESHOLD);
            
            // 根据消息类型调整参数
            adjustRuleByMessageType(rule, message);
            
            log.debug("创建聚合规则 - 聚合键: {}, 时间窗口: {}ms, 数量阈值: {}", 
                    k, rule.getTimeWindowMs(), rule.getQuantityThreshold());
            
            return rule;
        });
    }
    
    /**
     * 根据消息类型调整聚合规则
     */
    private void adjustRuleByMessageType(AggregationRule rule, CommonStompMessage message) {
        switch (message.getMessageType()) {
            case ALERT:
                // 告警消息：短时间窗口，小数量阈值，快速聚合
                rule.setTimeWindowMs(2000);
                rule.setQuantityThreshold(3);
                break;
                
            case NOTIFICATION:
                // 通知消息：中等时间窗口，中等数量阈值
                rule.setTimeWindowMs(5000);
                rule.setQuantityThreshold(5);
                break;
                
            case TERMINAL_STATUS_CHANGE:
                // 终端状态：长时间窗口，大数量阈值
                rule.setTimeWindowMs(10000);
                rule.setQuantityThreshold(20);
                break;
                
            case TASK_PROGRESS:
                // 任务进度：中等时间窗口，小数量阈值
                rule.setTimeWindowMs(3000);
                rule.setQuantityThreshold(5);
                break;
                
            default:
                // 使用默认值
                break;
        }
    }
    
    /**
     * 检查是否应该跳过聚合
     */
    private boolean shouldSkipAggregation(CommonStompMessage message, AggregationRule rule) {
        // 高优先级消息跳过聚合
        if (message.getMetadata() != null && message.getMetadata().getPriority() != null) {
            switch (message.getMetadata().getPriority()) {
                case HIGH:
                    return true;
                case NORMAL:
                    // 检查是否需要确认
                    return message.getMetadata().isRequireAck();
                case LOW:
                    return false;
            }
        }
        
        // 告警消息通常跳过聚合
        return message.getMessageType() == org.nan.cloud.message.infrastructure.websocket.stomp.enums.StompMessageTypes.ALERT;
    }
    
    /**
     * 检查是否应该触发聚合
     */
    private boolean shouldTriggerAggregation(String aggregationKey, AggregationRule rule) {
        List<CommonStompMessage> messages = pendingMessages.get(aggregationKey);
        if (messages == null || messages.isEmpty()) {
            return false;
        }
        
        // 数量阈值检查
        if (messages.size() >= rule.getQuantityThreshold()) {
            return true;
        }
        
        // 时间窗口检查
        Long lastTime = lastAggregationTimes.get(aggregationKey);
        if (lastTime == null) {
            lastAggregationTimes.put(aggregationKey, System.currentTimeMillis());
            return false;
        }
        
        return (System.currentTimeMillis() - lastTime) >= rule.getTimeWindowMs();
    }
    
    /**
     * 触发聚合
     */
    private AggregationResult triggerAggregation(String aggregationKey, AggregationRule rule) {
        try {
            List<CommonStompMessage> messages = pendingMessages.get(aggregationKey);
            if (messages == null || messages.isEmpty()) {
                return AggregationResult.empty();
            }
            
            // 同步获取待聚合消息并清空队列
            List<CommonStompMessage> messagesToAggregate;
            synchronized (messages) {
                messagesToAggregate = new ArrayList<>(messages);
                messages.clear();
            }
            
            if (messagesToAggregate.isEmpty()) {
                return AggregationResult.empty();
            }
            
            log.debug("触发聚合 - 聚合键: {}, 消息数量: {}", aggregationKey, messagesToAggregate.size());
            
            // 执行聚合
            CommonStompMessage aggregatedMessage = performAggregation(messagesToAggregate, rule);
            
            // 更新统计和时间
            totalAggregatedMessages.addAndGet(messagesToAggregate.size());
            totalAggregationEvents.incrementAndGet();
            lastAggregationTimes.put(aggregationKey, System.currentTimeMillis());
            
            log.info("✅ 消息聚合完成 - 聚合键: {}, 原消息数: {}, 聚合后: 1条", 
                    aggregationKey, messagesToAggregate.size());
            
            return AggregationResult.aggregated(aggregatedMessage, messagesToAggregate);
            
        } catch (Exception e) {
            log.error("触发聚合失败 - 聚合键: {}, 错误: {}", aggregationKey, e.getMessage(), e);
            
            // 失败时返回原消息列表，确保消息不丢失
            List<CommonStompMessage> messages = pendingMessages.get(aggregationKey);
            if (messages != null && !messages.isEmpty()) {
                List<CommonStompMessage> failedMessages;
                synchronized (messages) {
                    failedMessages = new ArrayList<>(messages);
                    messages.clear();
                }
                return AggregationResult.failed(failedMessages);
            }
            
            return AggregationResult.empty();
        }
    }
    
    /**
     * 执行消息聚合
     */
    private CommonStompMessage performAggregation(List<CommonStompMessage> messages, AggregationRule rule) {
        if (messages.size() == 1) {
            return messages.get(0);
        }
        
        // 选择最新的消息作为模板
        CommonStompMessage template = messages.stream()
                .max(Comparator.comparing(CommonStompMessage::getTimestamp))
                .orElse(messages.get(0));
        
        // 构建聚合消息
        return CommonStompMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .messageType(template.getMessageType())
                .subType_1("AGGREGATED")
                .subType_2(template.getSubType_2())
                .source(template.getSource())
                .target(template.getTarget())
                .payload(buildAggregatedPayload(messages))
                .message(buildAggregatedMessage(messages))
                .metadata(template.getMetadata())
                .build();
    }
    
    /**
     * 构建聚合载荷
     */
    private Map<String, Object> buildAggregatedPayload(List<CommonStompMessage> messages) {
        Map<String, Object> aggregatedPayload = new HashMap<>();
        
        aggregatedPayload.put("aggregatedCount", messages.size());
        aggregatedPayload.put("aggregatedMessages", messages.stream()
                .map(msg -> Map.of(
                        "messageId", msg.getMessageId(),
                        "timestamp", msg.getTimestamp(),
                        "message", msg.getMessage(),
                        "payload", msg.getPayload() != null ? msg.getPayload() : Map.of()
                ))
                .collect(Collectors.toList()));
        aggregatedPayload.put("aggregatedAt", LocalDateTime.now());
        
        return aggregatedPayload;
    }
    
    /**
     * 构建聚合消息文本
     */
    private String buildAggregatedMessage(List<CommonStompMessage> messages) {
        if (messages.size() == 1) {
            return messages.get(0).getMessage();
        }
        
        return String.format("聚合消息 (%d条): %s", 
                messages.size(),
                messages.stream()
                        .map(CommonStompMessage::getMessage)
                        .filter(Objects::nonNull)
                        .limit(3)
                        .collect(Collectors.joining("; ")) +
                (messages.size() > 3 ? "..." : ""));
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 聚合规则
     */
    public static class AggregationRule {
        private String aggregationKey;
        private long timeWindowMs;
        private int quantityThreshold;
        private double similarityThreshold;
        
        // Getters and Setters
        public String getAggregationKey() { return aggregationKey; }
        public void setAggregationKey(String aggregationKey) { this.aggregationKey = aggregationKey; }
        public long getTimeWindowMs() { return timeWindowMs; }
        public void setTimeWindowMs(long timeWindowMs) { this.timeWindowMs = timeWindowMs; }
        public int getQuantityThreshold() { return quantityThreshold; }
        public void setQuantityThreshold(int quantityThreshold) { this.quantityThreshold = quantityThreshold; }
        public double getSimilarityThreshold() { return similarityThreshold; }
        public void setSimilarityThreshold(double similarityThreshold) { this.similarityThreshold = similarityThreshold; }
    }
    
    /**
     * 聚合结果
     */
    public static class AggregationResult {
        private final AggregationResultType type;
        private final CommonStompMessage aggregatedMessage;
        private final List<CommonStompMessage> originalMessages;
        
        private AggregationResult(AggregationResultType type, CommonStompMessage aggregatedMessage, List<CommonStompMessage> originalMessages) {
            this.type = type;
            this.aggregatedMessage = aggregatedMessage;
            this.originalMessages = originalMessages != null ? originalMessages : Collections.emptyList();
        }
        
        public static AggregationResult immediate(CommonStompMessage message) {
            return new AggregationResult(AggregationResultType.IMMEDIATE, message, List.of(message));
        }
        
        public static AggregationResult queued(CommonStompMessage message) {
            return new AggregationResult(AggregationResultType.QUEUED, null, List.of(message));
        }
        
        public static AggregationResult aggregated(CommonStompMessage aggregatedMessage, List<CommonStompMessage> originalMessages) {
            return new AggregationResult(AggregationResultType.AGGREGATED, aggregatedMessage, originalMessages);
        }
        
        public static AggregationResult failed(List<CommonStompMessage> messages) {
            return new AggregationResult(AggregationResultType.FAILED, null, messages);
        }
        
        public static AggregationResult empty() {
            return new AggregationResult(AggregationResultType.EMPTY, null, Collections.emptyList());
        }
        
        // Getters
        public AggregationResultType getType() { return type; }
        public CommonStompMessage getAggregatedMessage() { return aggregatedMessage; }
        public List<CommonStompMessage> getOriginalMessages() { return originalMessages; }
        public boolean hasMessage() { return aggregatedMessage != null; }
        public boolean shouldSend() { return type == AggregationResultType.IMMEDIATE || type == AggregationResultType.AGGREGATED; }
    }
    
    /**
     * 聚合结果类型
     */
    public enum AggregationResultType {
        IMMEDIATE,  // 立即发送
        QUEUED,     // 已加入队列
        AGGREGATED, // 已聚合
        FAILED,     // 聚合失败
        EMPTY       // 空结果
    }
    
    /**
     * 聚合统计
     */
    public static class AggregationStats {
        private long totalAggregatedMessages;
        private long totalAggregationEvents;
        private int pendingMessageCount;
        private int activeAggregationKeys;
        
        public static AggregationStatsBuilder builder() {
            return new AggregationStatsBuilder();
        }
        
        // Getters
        public long getTotalAggregatedMessages() { return totalAggregatedMessages; }
        public long getTotalAggregationEvents() { return totalAggregationEvents; }
        public int getPendingMessageCount() { return pendingMessageCount; }
        public int getActiveAggregationKeys() { return activeAggregationKeys; }
        
        public static class AggregationStatsBuilder {
            private AggregationStats stats = new AggregationStats();
            
            public AggregationStatsBuilder totalAggregatedMessages(long total) {
                stats.totalAggregatedMessages = total;
                return this;
            }
            
            public AggregationStatsBuilder totalAggregationEvents(long events) {
                stats.totalAggregationEvents = events;
                return this;
            }
            
            public AggregationStatsBuilder pendingMessageCount(int pending) {
                stats.pendingMessageCount = pending;
                return this;
            }
            
            public AggregationStatsBuilder activeAggregationKeys(int active) {
                stats.activeAggregationKeys = active;
                return this;
            }
            
            public AggregationStats build() {
                return stats;
            }
        }
    }
}