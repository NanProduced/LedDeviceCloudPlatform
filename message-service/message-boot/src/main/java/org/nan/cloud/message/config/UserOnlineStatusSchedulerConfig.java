package org.nan.cloud.message.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.service.UserOnlineStatusService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 用户在线状态定时任务配置
 * 
 * 负责定期清理过期的在线状态、同步集群状态等定时任务。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "message.online-status.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class UserOnlineStatusSchedulerConfig {
    
    private final UserOnlineStatusService userOnlineStatusService;
    
    @Value("${message.online-status.cleanup.timeout-minutes:5}")
    private int cleanupTimeoutMinutes;
    
    @Value("${spring.application.name:message-service}")
    private String instanceId;
    
    /**
     * 定期清理过期的在线状态
     * 
     * 每5分钟执行一次，清理超时未更新心跳的在线状态
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void cleanupExpiredOnlineStatus() {
        try {
            log.debug("开始执行过期在线状态清理任务");
            
            int cleanedCount = userOnlineStatusService.cleanupExpiredOnlineStatus(cleanupTimeoutMinutes);
            
            if (cleanedCount > 0) {
                log.info("过期在线状态清理完成: cleanedCount={}, timeoutMinutes={}", 
                        cleanedCount, cleanupTimeoutMinutes);
            } else {
                log.debug("过期在线状态清理完成: 无需清理");
            }
            
        } catch (Exception e) {
            log.error("过期在线状态清理任务执行失败: error={}", e.getMessage(), e);
        }
    }
    
    /**
     * 定期同步在线状态统计
     * 
     * 每分钟执行一次，更新各组织的在线用户统计
     */
    @Scheduled(fixedRate = 60000) // 1分钟
    public void syncOnlineStatusStatistics() {
        try {
            log.trace("开始执行在线状态统计同步任务");
            
            // 获取平台统计信息
            var platformStats = userOnlineStatusService.getPlatformOnlineStats();
            
            log.trace("在线状态统计同步完成: totalOnlineUsers={}, totalConnections={}", 
                     platformStats.getTotalOnlineUsers(), platformStats.getTotalConnections());
            
        } catch (Exception e) {
            log.error("在线状态统计同步任务执行失败: error={}", e.getMessage(), e);
        }
    }
    
    /**
     * 定期记录在线状态监控指标
     * 
     * 每30分钟执行一次，记录关键监控指标
     */
    @Scheduled(fixedRate = 1800000) // 30分钟
    public void recordOnlineStatusMetrics() {
        try {
            log.debug("开始记录在线状态监控指标");
            
            var platformStats = userOnlineStatusService.getPlatformOnlineStats();
            var config = userOnlineStatusService.getOnlineStatusConfig();
            
            log.info("在线状态监控指标 - 实例: {}, 在线用户: {}, 总连接: {}, 配置: {}", 
                    instanceId, 
                    platformStats.getTotalOnlineUsers(), 
                    platformStats.getTotalConnections(),
                    config);
            
        } catch (Exception e) {
            log.error("在线状态监控指标记录失败: error={}", e.getMessage(), e);
        }
    }
    
    /**
     * 集群状态同步
     * 
     * 每10分钟执行一次，在集群环境下同步各节点的状态信息
     */
    @Scheduled(fixedRate = 600000) // 10分钟
    @ConditionalOnProperty(name = "message.online-status.cluster.enabled", havingValue = "true")
    public void syncClusterOnlineStatus() {
        try {
            log.debug("开始执行集群在线状态同步任务: instanceId={}", instanceId);
            
            int syncCount = userOnlineStatusService.syncOnlineStatus(instanceId);
            
            if (syncCount > 0) {
                log.info("集群在线状态同步完成: instanceId={}, syncCount={}", instanceId, syncCount);
            } else {
                log.debug("集群在线状态同步完成: 无需同步");
            }
            
        } catch (Exception e) {
            log.error("集群在线状态同步任务执行失败: instanceId={}, error={}", 
                     instanceId, e.getMessage(), e);
        }
    }
}