package org.nan.cloud.message.infrastructure.routing.enhanced;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.infrastructure.websocket.stomp.model.CommonStompMessage;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 动态路由引擎
 * 
 * 核心职责：
 * 1. 基于业务规则的智能路由决策
 * 2. 负载均衡和故障转移
 * 3. 路由策略管理和动态调整
 * 4. 路由性能监控和优化
 * 
 * 路由策略：
 * - 内容路由：基于消息内容的路由决策
 * - 负载均衡：多目标间的负载分配
 * - 故障转移：主路由失败时的备用路由
 * - 优先级路由：基于消息优先级的路由选择
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicRoutingEngine {
    
    private final EnhancedTopicManager enhancedTopicManager;
    
    /**
     * 路由规则缓存
     * Key: 路由键模式, Value: 路由规则
     */
    private final Map<String, RoutingRule> routingRules = new ConcurrentHashMap<>();
    
    /**
     * 路由性能统计
     * Key: 路由目标, Value: 性能统计
     */
    private final Map<String, RoutingPerformance> routingPerformances = new ConcurrentHashMap<>();
    
    /**
     * 路由决策计数器
     */
    private final AtomicLong totalRoutingDecisions = new AtomicLong(0);
    private final AtomicLong successfulRoutings = new AtomicLong(0);
    private final AtomicLong failedRoutings = new AtomicLong(0);
    
    /**
     * 执行动态路由决策
     * 
     * @param message 待路由的消息
     * @return 路由决策结果
     */
    public RoutingDecision makeRoutingDecision(CommonStompMessage message) {
        try {
            log.debug("开始动态路由决策 - 消息ID: {}, 类型: {}", message.getMessageId(), message.getMessageType());
            
            totalRoutingDecisions.incrementAndGet();
            
            // 生成路由键
            String routingKey = generateRoutingKey(message);
            
            // 获取路由规则
            RoutingRule rule = getOrCreateRoutingRule(routingKey, message);
            
            // 执行路由决策
            RoutingDecision decision = executeRouting(message, rule);
            
            // 更新性能统计
            updateRoutingPerformance(decision);
            
            if (decision.isSuccess()) {
                successfulRoutings.incrementAndGet();
                log.debug("路由决策成功 - 消息ID: {}, 目标数: {}", 
                        message.getMessageId(), decision.getTargets().size());
            } else {
                failedRoutings.incrementAndGet();
                log.warn("路由决策失败 - 消息ID: {}, 原因: {}", 
                        message.getMessageId(), decision.getFailureReason());
            }
            
            return decision;
            
        } catch (Exception e) {
            log.error("动态路由决策异常 - 消息ID: {}, 错误: {}", message.getMessageId(), e.getMessage(), e);
            failedRoutings.incrementAndGet();
            return RoutingDecision.failure("路由决策异常: " + e.getMessage());
        }
    }
    
    /**
     * 获取路由统计信息
     * 
     * @return 路由统计
     */
    public RoutingStats getRoutingStats() {
        return RoutingStats.builder()
                .totalRoutingDecisions(totalRoutingDecisions.get())
                .successfulRoutings(successfulRoutings.get())
                .failedRoutings(failedRoutings.get())
                .activeRoutingRules(routingRules.size())
                .build();
    }
    
    /**
     * 添加自定义路由规则
     * 
     * @param routingKey 路由键模式
     * @param rule 路由规则
     */
    public void addRoutingRule(String routingKey, RoutingRule rule) {
        routingRules.put(routingKey, rule);
        log.info("添加路由规则 - 路由键: {}, 策略: {}", routingKey, rule.getStrategy());
    }
    
    /**
     * 移除路由规则
     * 
     * @param routingKey 路由键模式
     */
    public void removeRoutingRule(String routingKey) {
        RoutingRule removed = routingRules.remove(routingKey);
        if (removed != null) {
            log.info("移除路由规则 - 路由键: {}", routingKey);
        }
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 生成路由键
     */
    private String generateRoutingKey(CommonStompMessage message) {
        StringBuilder keyBuilder = new StringBuilder();
        
        // 消息类型
        keyBuilder.append(message.getMessageType());
        
        // 子类型
        if (message.getSubType_1() != null) {
            keyBuilder.append(".").append(message.getSubType_1());
        }
        
        // 目标类型
        if (message.getTarget() != null && message.getTarget().getTargetType() != null) {
            keyBuilder.append(".").append(message.getTarget().getTargetType());
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * 获取或创建路由规则
     */
    private RoutingRule getOrCreateRoutingRule(String routingKey, CommonStompMessage message) {
        return routingRules.computeIfAbsent(routingKey, k -> {
            // 创建默认路由规则
            RoutingRule rule = new RoutingRule();
            rule.setRoutingKey(k);
            rule.setStrategy(RoutingStrategy.DIRECT);
            rule.setLoadBalancingType(LoadBalancingType.ROUND_ROBIN);
            rule.setFailoverEnabled(true);
            rule.setMaxRetries(3);
            
            // 根据消息类型调整策略
            adjustRoutingStrategy(rule, message);
            
            log.debug("创建路由规则 - 路由键: {}, 策略: {}", k, rule.getStrategy());
            
            return rule;
        });
    }
    
    /**
     * 根据消息类型调整路由策略
     */
    private void adjustRoutingStrategy(RoutingRule rule, CommonStompMessage message) {
        switch (message.getMessageType()) {
            case ALERT:
                // 告警消息：广播策略，高优先级
                rule.setStrategy(RoutingStrategy.BROADCAST);
                rule.setLoadBalancingType(LoadBalancingType.PRIORITY);
                break;
                
            case NOTIFICATION:
                // 通知消息：直接路由，轮询负载均衡
                rule.setStrategy(RoutingStrategy.DIRECT);
                rule.setLoadBalancingType(LoadBalancingType.ROUND_ROBIN);
                break;
                
            case TERMINAL_STATUS_CHANGE:
                // 终端状态：内容路由，权重负载均衡
                rule.setStrategy(RoutingStrategy.CONTENT_BASED);
                rule.setLoadBalancingType(LoadBalancingType.WEIGHTED);
                break;
                
            case TASK_PROGRESS:
                // 任务进度：条件路由，最少连接负载均衡
                rule.setStrategy(RoutingStrategy.CONDITIONAL);
                rule.setLoadBalancingType(LoadBalancingType.LEAST_CONNECTIONS);
                break;
                
            default:
                // 使用默认策略
                break;
        }
    }
    
    /**
     * 执行路由决策
     */
    private RoutingDecision executeRouting(CommonStompMessage message, RoutingRule rule) {
        try {
            // 获取候选目标
            List<String> candidateTargets = getCandidateTargets(message, rule);
            
            if (candidateTargets.isEmpty()) {
                return RoutingDecision.failure("无可用路由目标");
            }
            
            // 应用路由策略
            List<String> selectedTargets = applyRoutingStrategy(candidateTargets, message, rule);
            
            if (selectedTargets.isEmpty()) {
                return RoutingDecision.failure("路由策略未选择任何目标");
            }
            
            // 应用负载均衡
            List<String> finalTargets = applyLoadBalancing(selectedTargets, rule);
            
            return RoutingDecision.success(finalTargets, rule.getStrategy().name());
            
        } catch (Exception e) {
            log.error("执行路由决策失败 - 错误: {}", e.getMessage(), e);
            return RoutingDecision.failure("路由执行异常: " + e.getMessage());
        }
    }
    
    /**
     * 获取候选目标
     */
    private List<String> getCandidateTargets(CommonStompMessage message, RoutingRule rule) {
        List<String> targets = new ArrayList<>();
        
        // 基于消息目标获取候选路径
        if (message.getTarget() != null) {
            String topicPath = message.getTarget().getTopicPath();
            if (topicPath != null) {
                // 分割多个主题路径
                String[] paths = topicPath.split(",");
                for (String path : paths) {
                    targets.add(path.trim());
                }
            }
            
            // 基于用户ID生成目标
            if (message.getTarget().getUids() != null) {
                for (Long userId : message.getTarget().getUids()) {
                    targets.add("/topic/user/" + userId + "/notifications");
                }
            }
            
            // 基于组织ID生成目标
            if (message.getTarget().getOid() != null) {
                targets.add("/topic/org/" + message.getTarget().getOid() + "/announcements");
            }
        }
        
        // 使用增强主题管理器生成动态主题
        List<String> enhancedTargets = enhancedTopicManager.generateDynamicTopics(message);
        targets.addAll(enhancedTargets);
        
        return targets;
    }
    
    /**
     * 应用路由策略
     */
    private List<String> applyRoutingStrategy(List<String> candidateTargets, CommonStompMessage message, RoutingRule rule) {
        switch (rule.getStrategy()) {
            case DIRECT:
                // 直接路由：返回第一个目标
                return candidateTargets.isEmpty() ? Collections.emptyList() : 
                       Collections.singletonList(candidateTargets.get(0));
                
            case BROADCAST:
                // 广播路由：返回所有目标
                return new ArrayList<>(candidateTargets);
                
            case CONTENT_BASED:
                // 内容路由：基于消息内容过滤目标
                return filterTargetsByContent(candidateTargets, message);
                
            case CONDITIONAL:
                // 条件路由：基于条件过滤目标
                return filterTargetsByCondition(candidateTargets, message, rule);
                
            default:
                return candidateTargets;
        }
    }
    
    /**
     * 基于内容过滤目标
     */
    private List<String> filterTargetsByContent(List<String> targets, CommonStompMessage message) {
        // 简单的内容过滤逻辑
        List<String> filtered = new ArrayList<>();
        
        for (String target : targets) {
            // 根据消息内容决定是否包含此目标
            if (shouldIncludeTargetByContent(target, message)) {
                filtered.add(target);
            }
        }
        
        return filtered.isEmpty() ? targets : filtered;
    }
    
    /**
     * 基于条件过滤目标
     */
    private List<String> filterTargetsByCondition(List<String> targets, CommonStompMessage message, RoutingRule rule) {
        // 基于消息优先级、时间等条件过滤
        List<String> filtered = new ArrayList<>();
        
        for (String target : targets) {
            if (shouldIncludeTargetByCondition(target, message, rule)) {
                filtered.add(target);
            }
        }
        
        return filtered.isEmpty() ? targets : filtered;
    }
    
    /**
     * 应用负载均衡
     */
    private List<String> applyLoadBalancing(List<String> targets, RoutingRule rule) {
        if (targets.size() <= 1) {
            return targets;
        }
        
        switch (rule.getLoadBalancingType()) {
            case ROUND_ROBIN:
                return applyRoundRobinBalancing(targets);
                
            case WEIGHTED:
                return applyWeightedBalancing(targets);
                
            case LEAST_CONNECTIONS:
                return applyLeastConnectionsBalancing(targets);
                
            case PRIORITY:
                return applyPriorityBalancing(targets);
                
            default:
                return targets;
        }
    }
    
    /**
     * 轮询负载均衡
     */
    private List<String> applyRoundRobinBalancing(List<String> targets) {
        // 简单轮询：基于时间戳选择
        int index = (int) (System.currentTimeMillis() % targets.size());
        return Collections.singletonList(targets.get(index));
    }
    
    /**
     * 权重负载均衡
     */
    private List<String> applyWeightedBalancing(List<String> targets) {
        // 基于路由性能统计分配权重
        Map<String, Double> weights = new HashMap<>();
        for (String target : targets) {
            RoutingPerformance perf = routingPerformances.get(target);
            double weight = perf != null ? perf.calculateWeight() : 1.0;
            weights.put(target, weight);
        }
        
        // 根据权重选择目标
        String selected = selectByWeight(weights);
        return selected != null ? Collections.singletonList(selected) : targets;
    }
    
    /**
     * 最少连接负载均衡
     */
    private List<String> applyLeastConnectionsBalancing(List<String> targets) {
        String bestTarget = targets.stream()
                .min(Comparator.comparingInt(target -> {
                    RoutingPerformance perf = routingPerformances.get(target);
                    return perf != null ? perf.getActiveConnections() : 0;
                }))
                .orElse(targets.get(0));
        
        return Collections.singletonList(bestTarget);
    }
    
    /**
     * 优先级负载均衡
     */
    private List<String> applyPriorityBalancing(List<String> targets) {
        // 根据目标类型确定优先级
        String highestPriority = targets.stream()
                .max(Comparator.comparingInt(this::getTargetPriority))
                .orElse(targets.get(0));
        
        return Collections.singletonList(highestPriority);
    }
    
    /**
     * 更新路由性能统计
     */
    private void updateRoutingPerformance(RoutingDecision decision) {
        for (String target : decision.getTargets()) {
            RoutingPerformance perf = routingPerformances.computeIfAbsent(target, k -> new RoutingPerformance(k));
            
            if (decision.isSuccess()) {
                perf.recordSuccess();
            } else {
                perf.recordFailure();
            }
        }
    }
    
    // ==================== 辅助方法 ====================
    
    private boolean shouldIncludeTargetByContent(String target, CommonStompMessage message) {
        // 简单的内容匹配逻辑
        if (message.getMessage() != null && message.getMessage().contains("urgent")) {
            return target.contains("alert") || target.contains("admin");
        }
        return true;
    }
    
    private boolean shouldIncludeTargetByCondition(String target, CommonStompMessage message, RoutingRule rule) {
        // 基于消息优先级的条件过滤
        if (message.getMetadata() != null && message.getMetadata().getPriority() != null) {
            switch (message.getMetadata().getPriority()) {
                case HIGH:
                    return target.contains("admin") || target.contains("alert");
                case LOW:
                    return !target.contains("admin");
                default:
                    return true;
            }
        }
        return true;
    }
    
    private String selectByWeight(Map<String, Double> weights) {
        double totalWeight = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        double random = Math.random() * totalWeight;
        
        double currentWeight = 0;
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            currentWeight += entry.getValue();
            if (random <= currentWeight) {
                return entry.getKey();
            }
        }
        
        return weights.keySet().iterator().next();
    }
    
    private int getTargetPriority(String target) {
        if (target.contains("admin")) return 3;
        if (target.contains("alert")) return 2;
        return 1;
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 路由规则
     */
    public static class RoutingRule {
        private String routingKey;
        private RoutingStrategy strategy;
        private LoadBalancingType loadBalancingType;
        private boolean failoverEnabled;
        private int maxRetries;
        private Map<String, Object> conditions;
        
        public RoutingRule() {
            this.conditions = new HashMap<>();
        }
        
        // Getters and Setters
        public String getRoutingKey() { return routingKey; }
        public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }
        public RoutingStrategy getStrategy() { return strategy; }
        public void setStrategy(RoutingStrategy strategy) { this.strategy = strategy; }
        public LoadBalancingType getLoadBalancingType() { return loadBalancingType; }
        public void setLoadBalancingType(LoadBalancingType loadBalancingType) { this.loadBalancingType = loadBalancingType; }
        public boolean isFailoverEnabled() { return failoverEnabled; }
        public void setFailoverEnabled(boolean failoverEnabled) { this.failoverEnabled = failoverEnabled; }
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
        public Map<String, Object> getConditions() { return conditions; }
        public void setConditions(Map<String, Object> conditions) { this.conditions = conditions; }
    }
    
    /**
     * 路由性能统计
     */
    public static class RoutingPerformance {
        private final String target;
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong failureCount = new AtomicLong(0);
        private volatile int activeConnections = 0;
        private volatile long lastUpdateTime = System.currentTimeMillis();
        
        public RoutingPerformance(String target) {
            this.target = target;
        }
        
        public void recordSuccess() {
            successCount.incrementAndGet();
            lastUpdateTime = System.currentTimeMillis();
        }
        
        public void recordFailure() {
            failureCount.incrementAndGet();
            lastUpdateTime = System.currentTimeMillis();
        }
        
        public double calculateWeight() {
            long total = successCount.get() + failureCount.get();
            if (total == 0) return 1.0;
            return (double) successCount.get() / total;
        }
        
        // Getters
        public String getTarget() { return target; }
        public long getSuccessCount() { return successCount.get(); }
        public long getFailureCount() { return failureCount.get(); }
        public int getActiveConnections() { return activeConnections; }
        public long getLastUpdateTime() { return lastUpdateTime; }
    }
    
    /**
     * 路由决策结果
     */
    public static class RoutingDecision {
        private final boolean success;
        private final List<String> targets;
        private final String strategy;
        private final String failureReason;
        
        private RoutingDecision(boolean success, List<String> targets, String strategy, String failureReason) {
            this.success = success;
            this.targets = targets != null ? targets : Collections.emptyList();
            this.strategy = strategy;
            this.failureReason = failureReason;
        }
        
        public static RoutingDecision success(List<String> targets, String strategy) {
            return new RoutingDecision(true, targets, strategy, null);
        }
        
        public static RoutingDecision failure(String reason) {
            return new RoutingDecision(false, Collections.emptyList(), null, reason);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public List<String> getTargets() { return targets; }
        public String getStrategy() { return strategy; }
        public String getFailureReason() { return failureReason; }
    }
    
    /**
     * 路由统计
     */
    public static class RoutingStats {
        private long totalRoutingDecisions;
        private long successfulRoutings;
        private long failedRoutings;
        private int activeRoutingRules;
        
        public static RoutingStatsBuilder builder() {
            return new RoutingStatsBuilder();
        }
        
        // Getters
        public long getTotalRoutingDecisions() { return totalRoutingDecisions; }
        public long getSuccessfulRoutings() { return successfulRoutings; }
        public long getFailedRoutings() { return failedRoutings; }
        public int getActiveRoutingRules() { return activeRoutingRules; }
        public double getSuccessRate() {
            return totalRoutingDecisions > 0 ? (double) successfulRoutings / totalRoutingDecisions * 100 : 0;
        }
        
        public static class RoutingStatsBuilder {
            private RoutingStats stats = new RoutingStats();
            
            public RoutingStatsBuilder totalRoutingDecisions(long total) {
                stats.totalRoutingDecisions = total;
                return this;
            }
            
            public RoutingStatsBuilder successfulRoutings(long successful) {
                stats.successfulRoutings = successful;
                return this;
            }
            
            public RoutingStatsBuilder failedRoutings(long failed) {
                stats.failedRoutings = failed;
                return this;
            }
            
            public RoutingStatsBuilder activeRoutingRules(int active) {
                stats.activeRoutingRules = active;
                return this;
            }
            
            public RoutingStats build() {
                return stats;
            }
        }
    }
    
    /**
     * 路由策略枚举
     */
    public enum RoutingStrategy {
        DIRECT,         // 直接路由
        BROADCAST,      // 广播路由
        CONTENT_BASED,  // 内容路由
        CONDITIONAL     // 条件路由
    }
    
    /**
     * 负载均衡类型枚举
     */
    public enum LoadBalancingType {
        ROUND_ROBIN,        // 轮询
        WEIGHTED,           // 权重
        LEAST_CONNECTIONS,  // 最少连接
        PRIORITY            // 优先级
    }
}