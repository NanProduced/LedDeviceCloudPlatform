package org.nan.cloud.message.infrastructure.websocket.processor;

import org.nan.cloud.message.infrastructure.websocket.dispatcher.DispatchResult;
import org.nan.cloud.message.infrastructure.websocket.stomp.model.CommonStompMessage;

/**
 * 业务消息处理器接口
 * 
 * 定义业务消息的处理契约，支持不同类型的业务消息处理策略。
 * 每个实现类负责处理特定类型的业务消息，将MQ消息转换为STOMP消息并进行分发。
 * 
 * 设计原则：
 * 1. 单一职责：每个处理器只处理特定类型的业务消息
 * 2. 开闭原则：支持扩展新的业务消息类型，不修改现有代码
 * 3. 依赖倒置：依赖抽象接口，不依赖具体实现
 * 
 * 处理流程：
 * 1. 解析MQ消息内容和元数据
 * 2. 验证消息格式和业务规则
 * 3. 转换为标准的STOMP消息格式
 * 4. 确定分发策略和目标用户
 * 5. 调用分发器完成消息推送
 * 6. 记录处理结果和统计信息
 * 
 * @author Nan
 * @since 1.0.0
 */
public interface BusinessMessageProcessor {
    
    /**
     * 获取处理器支持的消息类型
     * 
     * @return 消息类型标识
     */
    String getSupportedMessageType();
    
    /**
     * 检查是否支持处理指定的消息
     * 
     * @param messageType MQ消息类型
     * @param routingKey MQ路由键
     * @return 是否支持处理
     */
    boolean supports(String messageType, String routingKey);
    
    /**
     * 处理业务消息
     * 
     * @param messagePayload MQ消息载荷（JSON字符串）
     * @param routingKey MQ路由键
     * @return 处理结果
     */
    BusinessMessageProcessResult process(String messagePayload, String routingKey);
    
    /**
     * 获取处理器优先级
     * 数值越小优先级越高，用于多个处理器都支持同一消息时的选择
     * 
     * @return 优先级数值
     */
    default int getPriority() {
        return 100;
    }
    
    /**
     * 业务消息处理结果
     */
    class BusinessMessageProcessResult {
        private final boolean success;
        private final String messageId;
        private final String errorMessage;
        private final DispatchResult dispatchResult;
        private final CommonStompMessage stompMessage;
        
        private BusinessMessageProcessResult(boolean success, String messageId, 
                                           String errorMessage, DispatchResult dispatchResult,
                                           CommonStompMessage stompMessage) {
            this.success = success;
            this.messageId = messageId;
            this.errorMessage = errorMessage;
            this.dispatchResult = dispatchResult;
            this.stompMessage = stompMessage;
        }
        
        public static BusinessMessageProcessResult success(String messageId, 
                                                         DispatchResult dispatchResult,
                                                         CommonStompMessage stompMessage) {
            return new BusinessMessageProcessResult(true, messageId, null, dispatchResult, stompMessage);
        }
        
        public static BusinessMessageProcessResult failure(String messageId, String errorMessage) {
            return new BusinessMessageProcessResult(false, messageId, errorMessage, null, null);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessageId() { return messageId; }
        public String getErrorMessage() { return errorMessage; }
        public DispatchResult getDispatchResult() { return dispatchResult; }
        public CommonStompMessage getStompMessage() { return stompMessage; }
    }
}