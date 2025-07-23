package org.nan.cloud.common.mq.core.exception;

/**
 * 消息发送异常
 * 
 * 当消息发送失败时抛出此异常。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public class MessageSendException extends MqException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 消息ID
     */
    private final String messageId;
    
    /**
     * 路由键
     */
    private final String routingKey;
    
    /**
     * 交换机名称
     */
    private final String exchange;
    
    public MessageSendException(String messageId, String message) {
        super("MESSAGE_SEND_FAILED", message);
        this.messageId = messageId;
        this.routingKey = null;
        this.exchange = null;
    }
    
    public MessageSendException(String messageId, String routingKey, String exchange, String message) {
        super("MESSAGE_SEND_FAILED", message);
        this.messageId = messageId;
        this.routingKey = routingKey;
        this.exchange = exchange;
    }
    
    public MessageSendException(String messageId, String routingKey, String exchange, String message, Throwable cause) {
        super("MESSAGE_SEND_FAILED", message, cause);
        this.messageId = messageId;
        this.routingKey = routingKey;
        this.exchange = exchange;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public String getRoutingKey() {
        return routingKey;
    }
    
    public String getExchange() {
        return exchange;
    }
    
    @Override
    public String toString() {
        return String.format("MessageSendException[messageId=%s, exchange=%s, routingKey=%s, message=%s]", 
                messageId, exchange, routingKey, getMessage());
    }
}