package org.nan.cloud.common.mq.examples.consumer;

import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.mq.consumer.ConsumeResult;
import org.nan.cloud.common.mq.consumer.MessageConsumer;
import org.nan.cloud.common.mq.core.message.Message;
import org.springframework.stereotype.Component;

/**
 * 通知消息消费者示例
 * 
 * 演示如何实现MessageConsumer接口来处理通知消息。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class NotificationConsumer implements MessageConsumer {
    
    @Override
    public ConsumeResult consume(Message message) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("处理通知消息: messageId={}, subject={}, receiverId={}", 
                    message.getMessageId(), message.getSubject(), message.getReceiverId());
            
            // 模拟业务处理
            processNotification(message);
            
            long duration = System.currentTimeMillis() - startTime;
            return ConsumeResult.success(message.getMessageId(), getConsumerId(), duration);
            
        } catch (Exception e) {
            log.error("处理通知消息失败: messageId={}, error={}", message.getMessageId(), e.getMessage(), e);
            return ConsumeResult.failure(message.getMessageId(), getConsumerId(), 
                    "PROCESS_FAILED", e.getMessage(), e);
        }
    }
    
    @Override
    public String[] getSupportedMessageTypes() {
        return new String[]{"NOTIFICATION"};
    }
    
    @Override
    public String getConsumerId() {
        return "NotificationConsumer";
    }
    
    @Override
    public boolean preConsume(Message message) {
        // 检查消息是否过期
        if (message.isExpired()) {
            log.warn("通知消息已过期，跳过处理: messageId={}, expireTime={}", 
                    message.getMessageId(), message.getExpireTime());
            return false;
        }
        
        // 检查是否有接收者
        if (message.getReceiverId() == null || message.getReceiverId().isEmpty()) {
            log.warn("通知消息缺少接收者，跳过处理: messageId={}", message.getMessageId());
            return false;
        }
        
        return true;
    }
    
    @Override
    public void postConsume(Message message, ConsumeResult result) {
        if (result.isSuccess()) {
            log.info("✅ 通知消息处理成功: messageId={}, duration={}ms", 
                    message.getMessageId(), result.getDuration());
            
            // 可以在这里添加成功后的处理逻辑，比如发送确认等
            sendDeliveryConfirmation(message);
        } else {
            log.error("❌ 通知消息处理失败: messageId={}, error={}", 
                    message.getMessageId(), result.getErrorMessage());
        }
    }
    
    @Override
    public ConsumeResult onError(Message message, Exception exception) {
        log.error("通知消息处理异常: messageId={}, error={}", 
                message.getMessageId(), exception.getMessage(), exception);
        
        // 根据异常类型决定处理策略
        if (exception instanceof IllegalArgumentException) {
            // 参数错误，不重试
            return ConsumeResult.reject(message.getMessageId(), getConsumerId(), 
                    "INVALID_ARGUMENT", exception.getMessage());
        } else {
            // 其他异常，可以重试
            return ConsumeResult.failure(message.getMessageId(), getConsumerId(), 
                    "PROCESS_EXCEPTION", exception.getMessage(), exception);
        }
    }
    
    /**
     * 处理通知消息的具体业务逻辑
     */
    private void processNotification(Message message) {
        // 模拟处理时间
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("处理被中断", e);
        }
        
        // 这里实现具体的通知处理逻辑
        // 比如：发送邮件、短信、推送等
        log.info("📧 发送通知给用户: userId={}, subject={}, content={}", 
                message.getReceiverId(), message.getSubject(), message.getPayload());
    }
    
    /**
     * 发送投递确认
     */
    private void sendDeliveryConfirmation(Message message) {
        log.debug("发送投递确认: messageId={}, receiverId={}", 
                message.getMessageId(), message.getReceiverId());
        // 实现投递确认逻辑
    }
}