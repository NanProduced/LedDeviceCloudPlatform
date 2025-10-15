package org.nan.cloud.common.mq.core.exception;

/**
 * 消息消费异常
 * 
 * 当消息消费失败时抛出此异常。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public class MessageConsumeException extends MqException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 消息ID
     */
    private final String messageId;
    
    /**
     * 队列名称
     */
    private final String queueName;
    
    /**
     * 消费者标识
     */
    private final String consumerId;
    
    public MessageConsumeException(String messageId, String message) {
        super("MESSAGE_CONSUME_FAILED", message);
        this.messageId = messageId;
        this.queueName = null;
        this.consumerId = null;
    }
    
    public MessageConsumeException(String messageId, String queueName, String consumerId, String message) {
        super("MESSAGE_CONSUME_FAILED", message);
        this.messageId = messageId;
        this.queueName = queueName;
        this.consumerId = consumerId;
    }
    
    public MessageConsumeException(String messageId, String queueName, String consumerId, String message, Throwable cause) {
        super("MESSAGE_CONSUME_FAILED", message, cause);
        this.messageId = messageId;
        this.queueName = queueName;
        this.consumerId = consumerId;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public String getQueueName() {
        return queueName;
    }
    
    public String getConsumerId() {
        return consumerId;
    }
    
    @Override
    public String toString() {
        return String.format("MessageConsumeException[messageId=%s, queue=%s, consumerId=%s, message=%s]", 
                messageId, queueName, consumerId, getMessage());
    }
}