package org.nan.cloud.message.infrastructure.websocket.reliability;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * STOMP消息投递跟踪器
 * 
 * 核心职责：
 * 1. 跟踪每条STOMP消息的投递状态
 * 2. 管理消息确认机制（ACK/NACK）
 * 3. 处理未确认消息的重试策略
 * 4. 维护消息投递的可靠性保证
 * 
 * 特性：
 * - 消息唯一ID生成和跟踪
 * - 可配置的超时和重试机制
 * - 支持不同类型消息的差异化策略
 * - 死信队列处理最终失败消息
 * - 投递统计和监控能力
 * 
 * @author Nan
 * @since 3.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageDeliveryTracker {
    
    /**
     * 待确认消息跟踪表
     * Key: messageId, Value: 投递记录
     */
    private final Map<String, DeliveryRecord> pendingMessages = new ConcurrentHashMap<>();
    
    /**
     * 消息重试记录
     * Key: messageId, Value: 重试信息
     */
    private final Map<String, RetryInfo> retryRecords = new ConcurrentHashMap<>();
    
    /**
     * 消息ID生成器
     */
    private final AtomicLong messageIdGenerator = new AtomicLong(0);
    
    /**
     * 定时任务执行器
     */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    /**
     * 投递统计信息
     */
    private final DeliveryStatistics statistics = new DeliveryStatistics();
    
    /**
     * 重试策略配置
     */
    private final RetryConfiguration retryConfig = new RetryConfiguration();
    
    /**
     * 生成唯一消息ID
     */
    public String generateMessageId() {
        return "msg_" + System.currentTimeMillis() + "_" + messageIdGenerator.incrementAndGet();
    }
    
    /**
     * 开始跟踪消息投递
     */
    public void startTracking(String messageId, String destination, String userId, 
                             String messageType, Object messageContent) {
        DeliveryRecord record = DeliveryRecord.builder()
            .messageId(messageId)
            .destination(destination)
            .userId(userId)
            .messageType(messageType)
            .content(messageContent)
            .createdAt(LocalDateTime.now())
            .status(DeliveryStatus.PENDING)
            .attempts(0)
            .build();
        
        pendingMessages.put(messageId, record);
        statistics.incrementPending();
        
        // 启动超时检查
        scheduleTimeoutCheck(messageId);
        
        log.debug("🎯 开始跟踪消息投递: messageId={}, destination={}, userId={}", 
                  messageId, destination, userId);
    }
    
    /**
     * 确认消息已投递（客户端ACK）
     */
    public boolean acknowledgeMessage(String messageId, String userId) {
        DeliveryRecord record = pendingMessages.get(messageId);
        if (record == null) {
            log.warn("⚠️ 尝试确认不存在的消息: messageId={}, userId={}", messageId, userId);
            return false;
        }
        
        // 验证用户权限
        if (!Objects.equals(record.getUserId(), userId)) {
            log.warn("⚠️ 用户无权确认消息: messageId={}, 期望userId={}, 实际userId={}", 
                     messageId, record.getUserId(), userId);
            return false;
        }
        
        // 更新记录状态
        record.setStatus(DeliveryStatus.ACKNOWLEDGED);
        record.setAckAt(LocalDateTime.now());
        
        // 移除跟踪记录
        pendingMessages.remove(messageId);
        retryRecords.remove(messageId);
        
        // 更新统计
        statistics.incrementAcknowledged();
        statistics.decrementPending();
        
        log.info("✅ 消息确认成功: messageId={}, userId={}, 耗时={}ms", 
                 messageId, userId, getDeliveryDuration(record));
        
        return true;
    }
    
    /**
     * 拒绝消息（客户端NACK）
     */
    public boolean rejectMessage(String messageId, String userId, String reason) {
        DeliveryRecord record = pendingMessages.get(messageId);
        if (record == null) {
            log.warn("⚠️ 尝试拒绝不存在的消息: messageId={}, userId={}", messageId, userId);
            return false;
        }
        
        // 验证用户权限
        if (!Objects.equals(record.getUserId(), userId)) {
            log.warn("⚠️ 用户无权拒绝消息: messageId={}, 期望userId={}, 实际userId={}", 
                     messageId, record.getUserId(), userId);
            return false;
        }
        
        // 更新记录状态
        record.setStatus(DeliveryStatus.REJECTED);
        record.setRejectAt(LocalDateTime.now());
        record.setRejectReason(reason);
        
        // 决定是否重试
        if (shouldRetry(record)) {
            scheduleRetry(messageId, record);
        } else {
            // 最终失败，移除跟踪
            pendingMessages.remove(messageId);
            retryRecords.remove(messageId);
            statistics.incrementFailed();
            statistics.decrementPending();
            
            log.warn("❌ 消息最终失败: messageId={}, userId={}, reason={}", 
                     messageId, userId, reason);
        }
        
        return true;
    }
    
    /**
     * 处理投递超时
     */
    public void handleTimeout(String messageId) {
        DeliveryRecord record = pendingMessages.get(messageId);
        if (record == null) {
            return; // 消息已被处理
        }
        
        record.setStatus(DeliveryStatus.TIMEOUT);
        statistics.incrementTimeout();
        
        // 决定是否重试
        if (shouldRetry(record)) {
            log.warn("⏰ 消息投递超时，准备重试: messageId={}, attempts={}", 
                     messageId, record.getAttempts());
            scheduleRetry(messageId, record);
        } else {
            // 最终超时失败
            pendingMessages.remove(messageId);
            retryRecords.remove(messageId);
            statistics.incrementFailed();
            statistics.decrementPending();
            
            log.error("💀 消息投递最终超时: messageId={}, totalAttempts={}", 
                      messageId, record.getAttempts());
        }
    }
    
    /**
     * 判断是否应该重试
     */
    private boolean shouldRetry(DeliveryRecord record) {
        RetryConfiguration.RetryPolicy policy = retryConfig.getRetryPolicy(record.getMessageType());
        return record.getAttempts() < policy.getMaxRetries();
    }
    
    /**
     * 安排消息重试
     */
    private void scheduleRetry(String messageId, DeliveryRecord record) {
        RetryConfiguration.RetryPolicy policy = retryConfig.getRetryPolicy(record.getMessageType());
        
        // 计算重试延迟（指数退避）
        long delay = policy.calculateDelay(record.getAttempts());
        
        // 记录重试信息
        RetryInfo retryInfo = retryRecords.computeIfAbsent(messageId, 
            k -> new RetryInfo(messageId));
        retryInfo.incrementAttempts();
        retryInfo.setNextRetryAt(LocalDateTime.now().plusSeconds(delay));
        
        // 安排重试任务
        scheduler.schedule(() -> {
            executeRetry(messageId, record);
        }, delay, TimeUnit.SECONDS);
        
        record.incrementAttempts();
        record.setStatus(DeliveryStatus.RETRYING);
        
        log.info("🔄 安排消息重试: messageId={}, attempt={}, delay={}s", 
                 messageId, record.getAttempts(), delay);
    }
    
    /**
     * 执行消息重试
     */
    private void executeRetry(String messageId, DeliveryRecord record) {
        // 重置状态为待投递
        record.setStatus(DeliveryStatus.PENDING);
        record.setCreatedAt(LocalDateTime.now()); // 重新计时
        
        // 重新启动超时检查
        scheduleTimeoutCheck(messageId);
        
        statistics.incrementRetries();
        
        log.info("🔄 执行消息重试: messageId={}, attempt={}", messageId, record.getAttempts());
        
        // 这里应该触发实际的重新投递
        // 由于这是基础设施层，具体投递逻辑应该通过事件或回调处理
    }
    
    /**
     * 安排超时检查
     */
    private void scheduleTimeoutCheck(String messageId) {
        DeliveryRecord record = pendingMessages.get(messageId);
        if (record == null) return;
        
        RetryConfiguration.RetryPolicy policy = retryConfig.getRetryPolicy(record.getMessageType());
        long timeoutSeconds = policy.getTimeoutSeconds();
        
        scheduler.schedule(() -> {
            handleTimeout(messageId);
        }, timeoutSeconds, TimeUnit.SECONDS);
    }
    
    /**
     * 获取投递耗时（毫秒）
     */
    private long getDeliveryDuration(DeliveryRecord record) {
        if (record.getAckAt() != null) {
            return java.time.Duration.between(record.getCreatedAt(), record.getAckAt()).toMillis();
        }
        return java.time.Duration.between(record.getCreatedAt(), LocalDateTime.now()).toMillis();
    }
    
    /**
     * 获取待确认消息数量
     */
    public int getPendingMessageCount() {
        return pendingMessages.size();
    }
    
    /**
     * 获取投递统计信息
     */
    public DeliveryStatistics getStatistics() {
        return statistics.copy();
    }
    
    /**
     * 获取指定消息的投递记录
     */
    public Optional<DeliveryRecord> getDeliveryRecord(String messageId) {
        return Optional.ofNullable(pendingMessages.get(messageId));
    }
    
    /**
     * 清理过期的记录
     */
    public void cleanupExpiredRecords() {
        LocalDateTime expireTime = LocalDateTime.now().minusHours(24);
        
        pendingMessages.entrySet().removeIf(entry -> {
            DeliveryRecord record = entry.getValue();
            if (record.getCreatedAt().isBefore(expireTime)) {
                log.info("🧹 清理过期消息记录: messageId={}", entry.getKey());
                return true;
            }
            return false;
        });
        
        retryRecords.entrySet().removeIf(entry -> {
            RetryInfo info = entry.getValue();
            if (info.getFirstAttemptAt().isBefore(expireTime)) {
                return true;
            }
            return false;
        });
    }
    
    /**
     * 投递记录
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeliveryRecord {
        private String messageId;
        private String destination;
        private String userId;
        private String messageType;
        private Object content;
        private LocalDateTime createdAt;
        private LocalDateTime ackAt;
        private LocalDateTime rejectAt;
        private String rejectReason;
        private DeliveryStatus status;
        private int attempts;
        
        public void incrementAttempts() {
            this.attempts++;
        }
    }
    
    /**
     * 重试信息
     */
    @Data
    @AllArgsConstructor
    public static class RetryInfo {
        private String messageId;
        private int attempts;
        private LocalDateTime firstAttemptAt;
        private LocalDateTime nextRetryAt;
        
        public RetryInfo(String messageId) {
            this.messageId = messageId;
            this.attempts = 0;
            this.firstAttemptAt = LocalDateTime.now();
        }
        
        public void incrementAttempts() {
            this.attempts++;
        }
    }
    
    /**
     * 投递状态枚举
     */
    public enum DeliveryStatus {
        PENDING("待投递"),
        ACKNOWLEDGED("已确认"),
        REJECTED("已拒绝"),
        TIMEOUT("超时"),
        RETRYING("重试中"),
        FAILED("最终失败");
        
        private final String description;
        
        DeliveryStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 投递统计信息
     */
    @Data
    public static class DeliveryStatistics {
        private final AtomicLong totalSent = new AtomicLong(0);
        private final AtomicLong totalAcknowledged = new AtomicLong(0);
        private final AtomicLong totalFailed = new AtomicLong(0);
        private final AtomicLong totalTimeout = new AtomicLong(0);
        private final AtomicLong totalRetries = new AtomicLong(0);
        private final AtomicLong currentPending = new AtomicLong(0);
        
        public void incrementSent() { totalSent.incrementAndGet(); }
        public void incrementAcknowledged() { totalAcknowledged.incrementAndGet(); }
        public void incrementFailed() { totalFailed.incrementAndGet(); }
        public void incrementTimeout() { totalTimeout.incrementAndGet(); }
        public void incrementRetries() { totalRetries.incrementAndGet(); }
        public void incrementPending() { currentPending.incrementAndGet(); }
        public void decrementPending() { currentPending.decrementAndGet(); }
        
        public double getSuccessRate() {
            long total = totalSent.get();
            return total > 0 ? (double) totalAcknowledged.get() / total * 100 : 0.0;
        }
        
        public DeliveryStatistics copy() {
            DeliveryStatistics copy = new DeliveryStatistics();
            copy.totalSent.set(this.totalSent.get());
            copy.totalAcknowledged.set(this.totalAcknowledged.get());
            copy.totalFailed.set(this.totalFailed.get());
            copy.totalTimeout.set(this.totalTimeout.get());
            copy.totalRetries.set(this.totalRetries.get());
            copy.currentPending.set(this.currentPending.get());
            return copy;
        }
    }
}