package org.nan.cloud.common.mq.consumer;

import org.nan.cloud.common.mq.core.message.Message;

/**
 * 消息消费者接口
 * 
 * 定义消息消费的统一接口，简化消息处理逻辑。
 * 实现类只需关注业务逻辑，框架负责消息确认、错误处理等。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface MessageConsumer {
    
    /**
     * 消费消息
     * 
     * @param message 消息对象
     * @return 消费结果
     */
    ConsumeResult consume(Message message);
    
    /**
     * 获取支持的消息类型
     * 
     * @return 消息类型数组，空数组表示支持所有类型
     */
    default String[] getSupportedMessageTypes() {
        return new String[0]; // 默认支持所有类型
    }
    
    /**
     * 获取消费者标识
     * 
     * @return 消费者唯一标识
     */
    default String getConsumerId() {
        return this.getClass().getSimpleName();
    }
    
    /**
     * 是否支持指定消息类型
     * 
     * @param messageType 消息类型
     * @return true表示支持
     */
    default boolean supports(String messageType) {
        String[] supportedTypes = getSupportedMessageTypes();
        if (supportedTypes.length == 0) {
            return true; // 支持所有类型
        }
        
        for (String supportedType : supportedTypes) {
            if (supportedType.equals(messageType) || "*".equals(supportedType)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 消息消费前置处理
     * 
     * @param message 消息对象
     * @return true表示继续处理，false表示跳过
     */
    default boolean preConsume(Message message) {
        return true;
    }
    
    /**
     * 消息消费后置处理
     * 
     * @param message 消息对象
     * @param result 消费结果
     */
    default void postConsume(Message message, ConsumeResult result) {
        // 默认空实现
    }
    
    /**
     * 消息消费异常处理
     * 
     * @param message 消息对象
     * @param exception 异常信息
     * @return 异常处理结果
     */
    default ConsumeResult onError(Message message, Exception exception) {
        return ConsumeResult.failure(
            message != null ? message.getMessageId() : "unknown",
            getConsumerId(),
            "CONSUME_EXCEPTION",
            "消费异常: " + exception.getMessage(),
            exception
        );
    }
}