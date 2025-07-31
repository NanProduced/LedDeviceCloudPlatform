package org.nan.cloud.message.config;

import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.infrastructure.websocket.reliability.MessageDeliveryTracker;
import org.nan.cloud.message.infrastructure.websocket.reliability.ReliableMessageSender;
import org.nan.cloud.message.infrastructure.websocket.reliability.StompAckHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * STOMP可靠性功能配置
 * 
 * 集成Phase 3.1的可靠性功能，包括：
 * 1. 定时清理过期记录
 * 2. 监控和统计任务
 * 3. 与现有消息服务的集成配置
 * 
 * 配置项：
 * - message.reliability.enabled: 是否启用可靠性功能
 * - message.reliability.cleanup.enabled: 是否启用自动清理
 * - message.reliability.monitoring.enabled: 是否启用监控统计
 * 
 * @author Nan
 * @since 3.1.0
 */
@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "message.reliability.enabled", havingValue = "true", matchIfMissing = true)
public class ReliabilityConfig {
    
    private final MessageDeliveryTracker deliveryTracker;
    private final StompAckHandler ackHandler;
    private final ReliableMessageSender reliableMessageSender;
    
    public ReliabilityConfig(MessageDeliveryTracker deliveryTracker,
                           StompAckHandler ackHandler,
                           ReliableMessageSender reliableMessageSender) {
        this.deliveryTracker = deliveryTracker;
        this.ackHandler = ackHandler;
        this.reliableMessageSender = reliableMessageSender;
        
        log.info("🔧 STOMP可靠性功能配置初始化完成");
    }
    
    /**
     * 清理过期投递记录 - 每小时执行
     */
    @Scheduled(fixedRate = 3600000) // 1小时
    @ConditionalOnProperty(name = "message.reliability.cleanup.enabled", havingValue = "true", matchIfMissing = true)
    public void cleanupExpiredDeliveryRecords() {
        try {
            log.debug("🧹 开始清理过期投递记录");
            deliveryTracker.cleanupExpiredRecords();
            log.debug("✅ 过期投递记录清理完成");
        } catch (Exception e) {
            log.error("❌ 清理过期投递记录失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 清理过期连接状态 - 每30分钟执行
     */
    @Scheduled(fixedRate = 1800000) // 30分钟
    @ConditionalOnProperty(name = "message.reliability.cleanup.enabled", havingValue = "true", matchIfMissing = true)
    public void cleanupExpiredConnections() {
        try {
            log.debug("🧹 开始清理过期连接状态");
            ackHandler.cleanupExpiredConnections(60); // 60分钟超时
            log.debug("✅ 过期连接状态清理完成");
        } catch (Exception e) {
            log.error("❌ 清理过期连接状态失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 清理已完成的重试任务 - 每15分钟执行
     */
    @Scheduled(fixedRate = 900000) // 15分钟
    @ConditionalOnProperty(name = "message.reliability.cleanup.enabled", havingValue = "true", matchIfMissing = true)
    public void cleanupCompletedRetryTasks() {
        try {
            log.debug("🧹 开始清理已完成的重试任务");
            reliableMessageSender.cleanupCompletedRetryTasks();
            log.debug("✅ 已完成重试任务清理完成");
        } catch (Exception e) {
            log.error("❌ 清理重试任务失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 输出可靠性统计信息 - 每10分钟执行
     */
    @Scheduled(fixedRate = 600000) // 10分钟
    @ConditionalOnProperty(name = "message.reliability.monitoring.enabled", havingValue = "true", matchIfMissing = true)
    public void logReliabilityStatistics() {
        try {
            // 获取投递统计
            MessageDeliveryTracker.DeliveryStatistics deliveryStats = deliveryTracker.getStatistics();
            
            // 获取连接统计
            long activeConnections = ackHandler.getActiveConnectionCount();
            int pendingMessages = deliveryTracker.getPendingMessageCount();
            int pendingRetries = reliableMessageSender.getPendingRetryCount();
            
            log.info("📊 STOMP可靠性统计 - " +
                    "活跃连接: {}, 待确认: {}, 待重试: {}, " +
                    "总发送: {}, 已确认: {}, 成功率: {:.1f}%, " +
                    "超时: {}, 失败: {}, 重试: {}",
                    activeConnections, pendingMessages, pendingRetries,
                    deliveryStats.getTotalSent().get(), deliveryStats.getTotalAcknowledged().get(),
                    deliveryStats.getSuccessRate(), deliveryStats.getTotalTimeout().get(),
                    deliveryStats.getTotalFailed().get(), deliveryStats.getTotalRetries().get());
                    
        } catch (Exception e) {
            log.error("❌ 输出可靠性统计失败: {}", e.getMessage(), e);
        }
    }
}