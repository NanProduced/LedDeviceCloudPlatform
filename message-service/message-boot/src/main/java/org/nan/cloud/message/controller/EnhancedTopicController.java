package org.nan.cloud.message.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.web.response.ResponseVO;
import org.nan.cloud.message.infrastructure.routing.enhanced.DynamicRoutingEngine;
import org.nan.cloud.message.infrastructure.routing.enhanced.MessageAggregator;
import org.nan.cloud.message.infrastructure.routing.enhanced.RoutingStrategyManager;
import org.nan.cloud.message.infrastructure.routing.enhanced.EnhancedTopicManager;
import org.nan.cloud.message.infrastructure.websocket.dispatcher.StompMessageDispatcher;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 增强版Topic管理API控制器 (Phase 2.4)
 * 
 * 提供Phase 2.4增强版Topic模式的管理和监控API：
 * 1. 消息聚合统计和配置
 * 2. 动态路由引擎管理
 * 3. 路由策略配置
 * 4. 增强Topic统计信息
 * 5. 系统性能监控
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/message/enhanced-topic")
@RequiredArgsConstructor
public class EnhancedTopicController {
    
    private final MessageAggregator messageAggregator;
    private final DynamicRoutingEngine dynamicRoutingEngine;
    private final RoutingStrategyManager routingStrategyManager;
    private final EnhancedTopicManager enhancedTopicManager;
    private final StompMessageDispatcher stompMessageDispatcher;
    
    // ==================== 系统统计API ====================
    
    /**
     * 获取增强Topic系统综合统计
     * 
     * @return 系统统计信息
     */
    @GetMapping("/stats")
    public ResponseVO<Map<String, Object>> getEnhancedTopicStats() {
        try {
            log.debug("获取增强Topic系统统计信息");
            
            Map<String, Object> stats = stompMessageDispatcher.getEnhancedTopicStats();
            
            // 添加增强Topic管理器统计
            EnhancedTopicManager.TopicStats topicStats = enhancedTopicManager.getTopicStats();
            stats.put("topic", Map.of(
                    "totalDynamicTopics", topicStats.getTotalDynamicTopics(),
                    "totalWildcardPatterns", topicStats.getTotalWildcardPatterns(),
                    "totalSubscriptions", topicStats.getTotalSubscriptions(),
                    "activeTopics", topicStats.getActiveTopics()
            ));
            
            return ResponseVO.success(stats);
            
        } catch (Exception e) {
            log.error("获取增强Topic系统统计失败 - 错误: {}", e.getMessage(), e);
            return ResponseVO.error("获取系统统计失败: " + e.getMessage());
        }
    }
    
    // ==================== 消息聚合管理API ====================
    
    /**
     * 获取消息聚合统计信息
     * 
     * @return 聚合统计
     */
    @GetMapping("/aggregation/stats")
    public ResponseVO<MessageAggregator.AggregationStats> getAggregationStats() {
        try {
            log.debug("获取消息聚合统计信息");
            
            MessageAggregator.AggregationStats stats = messageAggregator.getAggregationStats();
            return ResponseVO.success(stats);
            
        } catch (Exception e) {
            log.error("获取消息聚合统计失败 - 错误: {}", e.getMessage(), e);
            return ResponseVO.error("获取聚合统计失败: " + e.getMessage());
        }
    }
    
    // ==================== 动态路由管理API ====================
    
    /**
     * 获取动态路由统计信息
     * 
     * @return 路由统计
     */
    @GetMapping("/routing/stats")
    public ResponseVO<DynamicRoutingEngine.RoutingStats> getRoutingStats() {
        try {
            log.debug("获取动态路由统计信息");
            
            DynamicRoutingEngine.RoutingStats stats = dynamicRoutingEngine.getRoutingStats();
            return ResponseVO.success(stats);
            
        } catch (Exception e) {
            log.error("获取动态路由统计失败 - 错误: {}", e.getMessage(), e);
            return ResponseVO.error("获取路由统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 添加自定义路由规则
     * 
     * @param routingKey 路由键
     * @param request 路由规则配置
     * @return 操作结果
     */
    @PostMapping("/routing/rules/{routingKey}")
    public ResponseVO<String> addRoutingRule(@PathVariable String routingKey, 
                                           @RequestBody RoutingRuleRequest request) {
        try {
            log.info("添加路由规则 - 路由键: {}", routingKey);
            
            DynamicRoutingEngine.RoutingRule rule = new DynamicRoutingEngine.RoutingRule();
            rule.setRoutingKey(routingKey);
            rule.setStrategy(request.getStrategy());
            rule.setLoadBalancingType(request.getLoadBalancingType());
            rule.setFailoverEnabled(request.isFailoverEnabled());
            rule.setMaxRetries(request.getMaxRetries());
            
            dynamicRoutingEngine.addRoutingRule(routingKey, rule);
            
            return ResponseVO.success("路由规则添加成功");
            
        } catch (Exception e) {
            log.error("添加路由规则失败 - 路由键: {}, 错误: {}", routingKey, e.getMessage(), e);
            return ResponseVO.error("添加路由规则失败: " + e.getMessage());
        }
    }
    
    /**
     * 移除路由规则
     * 
     * @param routingKey 路由键
     * @return 操作结果
     */
    @DeleteMapping("/routing/rules/{routingKey}")
    public ResponseVO<String> removeRoutingRule(@PathVariable String routingKey) {
        try {
            log.info("移除路由规则 - 路由键: {}", routingKey);
            
            dynamicRoutingEngine.removeRoutingRule(routingKey);
            
            return ResponseVO.success("路由规则移除成功");
            
        } catch (Exception e) {
            log.error("移除路由规则失败 - 路由键: {}, 错误: {}", routingKey, e.getMessage(), e);
            return ResponseVO.error("移除路由规则失败: " + e.getMessage());
        }
    }
    
    // ==================== 路由策略管理API ====================
    
    /**
     * 获取路由策略统计信息
     * 
     * @return 策略统计
     */
    @GetMapping("/strategy/stats")
    public ResponseVO<RoutingStrategyManager.StrategyStats> getStrategyStats() {
        try {
            log.debug("获取路由策略统计信息");
            
            RoutingStrategyManager.StrategyStats stats = routingStrategyManager.getStrategyStats();
            return ResponseVO.success(stats);
            
        } catch (Exception e) {
            log.error("获取路由策略统计失败 - 错误: {}", e.getMessage(), e);
            return ResponseVO.error("获取策略统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 设置活跃策略
     * 
     * @param messageType 消息类型
     * @param strategyName 策略名称
     * @return 操作结果
     */
    @PutMapping("/strategy/active/{messageType}/{strategyName}")
    public ResponseVO<String> setActiveStrategy(@PathVariable String messageType, 
                                              @PathVariable String strategyName) {
        try {
            log.info("设置活跃策略 - 消息类型: {}, 策略: {}", messageType, strategyName);
            
            routingStrategyManager.setActiveStrategy(messageType, strategyName);
            
            return ResponseVO.success("活跃策略设置成功");
            
        } catch (Exception e) {
            log.error("设置活跃策略失败 - 消息类型: {}, 策略: {}, 错误: {}", 
                    messageType, strategyName, e.getMessage(), e);
            return ResponseVO.error("设置活跃策略失败: " + e.getMessage());
        }
    }
    
    // ==================== Topic管理API ====================
    
    /**
     * 获取Topic统计信息
     * 
     * @return Topic统计
     */
    @GetMapping("/topic/stats")
    public ResponseVO<EnhancedTopicManager.TopicStats> getTopicStats() {
        try {
            log.debug("获取Topic统计信息");
            
            EnhancedTopicManager.TopicStats stats = enhancedTopicManager.getTopicStats();
            return ResponseVO.success(stats);
            
        } catch (Exception e) {
            log.error("获取Topic统计失败 - 错误: {}", e.getMessage(), e);
            return ResponseVO.error("获取Topic统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试通配符Topic匹配
     * 
     * @param topicPattern 通配符模式
     * @param actualTopic 实际Topic路径
     * @return 匹配结果
     */
    @GetMapping("/topic/wildcard/match")
    public ResponseVO<Boolean> testWildcardMatch(@RequestParam String topicPattern, 
                                               @RequestParam String actualTopic) {
        try {
            log.debug("测试通配符匹配 - 模式: {}, Topic: {}", topicPattern, actualTopic);
            
            boolean matches = enhancedTopicManager.matchWildcardTopic(topicPattern, actualTopic);
            
            return ResponseVO.success(matches);
            
        } catch (Exception e) {
            log.error("测试通配符匹配失败 - 模式: {}, Topic: {}, 错误: {}", 
                    topicPattern, actualTopic, e.getMessage(), e);
            return ResponseVO.error("通配符匹配测试失败: " + e.getMessage());
        }
    }
    
    /**
     * 清理未使用的Topic
     * 
     * @param maxIdleHours 最大空闲小时数，默认24小时
     * @return 操作结果
     */
    @PostMapping("/topic/cleanup")
    public ResponseVO<String> cleanupUnusedTopics(@RequestParam(defaultValue = "24") int maxIdleHours) {
        try {
            log.info("清理未使用Topic - 最大空闲时间: {}小时", maxIdleHours);
            
            long maxIdleTimeMs = maxIdleHours * 3600 * 1000L;
            enhancedTopicManager.cleanupUnusedTopics(maxIdleTimeMs);
            
            return ResponseVO.success("Topic清理完成");
            
        } catch (Exception e) {
            log.error("清理未使用Topic失败 - 错误: {}", e.getMessage(), e);
            return ResponseVO.error("Topic清理失败: " + e.getMessage());
        }
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 路由规则请求
     */
    public static class RoutingRuleRequest {
        private DynamicRoutingEngine.RoutingStrategy strategy;
        private DynamicRoutingEngine.LoadBalancingType loadBalancingType;
        private boolean failoverEnabled;
        private int maxRetries;
        
        // Getters and Setters
        public DynamicRoutingEngine.RoutingStrategy getStrategy() { return strategy; }
        public void setStrategy(DynamicRoutingEngine.RoutingStrategy strategy) { this.strategy = strategy; }
        public DynamicRoutingEngine.LoadBalancingType getLoadBalancingType() { return loadBalancingType; }
        public void setLoadBalancingType(DynamicRoutingEngine.LoadBalancingType loadBalancingType) { this.loadBalancingType = loadBalancingType; }
        public boolean isFailoverEnabled() { return failoverEnabled; }
        public void setFailoverEnabled(boolean failoverEnabled) { this.failoverEnabled = failoverEnabled; }
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    }
}