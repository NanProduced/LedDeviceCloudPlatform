package org.nan.cloud.message.infrastructure.websocket.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 业务消息处理器管理器
 * 
 * 负责管理所有业务消息处理器，提供统一的消息处理入口。
 * 支持处理器的自动注册、路由选择和处理结果统计。
 * 
 * 核心功能：
 * 1. 自动发现和注册所有BusinessMessageProcessor实现
 * 2. 根据消息类型和路由键选择合适的处理器
 * 3. 支持处理器优先级排序
 * 4. 提供处理结果统计和监控
 * 5. 异常处理和降级机制
 * 
 * 选择策略：
 * 1. 首先查找明确支持该消息类型的处理器
 * 2. 如果有多个处理器支持，按优先级选择
 * 3. 如果没有专门的处理器，使用默认处理器
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
public class BusinessMessageProcessorManager implements InitializingBean {
    
    private final List<BusinessMessageProcessor> processors;
    
    public BusinessMessageProcessorManager(List<BusinessMessageProcessor> processors) {
        this.processors = processors;
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        // 按优先级排序处理器
        processors.sort(Comparator.comparingInt(BusinessMessageProcessor::getPriority));
        
        log.info("🔧 业务消息处理器管理器初始化完成");
        log.info("📊 已注册处理器数量: {}", processors.size());
        
        processors.forEach(processor -> {
            log.info("  ├─ {} - 支持类型: {}, 优先级: {}", 
                    processor.getClass().getSimpleName(),
                    processor.getSupportedMessageType(),
                    processor.getPriority());
        });
        
        log.info("✅ 业务消息处理器管理器启动完成");
    }
    
    /**
     * 处理业务消息
     * 
     * @param messageType MQ消息类型
     * @param messagePayload MQ消息载荷
     * @param routingKey MQ路由键
     * @return 处理结果
     */
    public BusinessMessageProcessor.BusinessMessageProcessResult processMessage(
            String messageType, String messagePayload, String routingKey) {
        
        try {
            log.debug("开始处理业务消息 - 类型: {}, 路由键: {}", messageType, routingKey);
            
            // 选择合适的处理器
            BusinessMessageProcessor selectedProcessor = selectProcessor(messageType, routingKey);
            
            if (selectedProcessor == null) {
                String errorMsg = String.format("未找到支持的消息处理器 - 类型: %s, 路由键: %s", 
                        messageType, routingKey);
                log.warn("⚠️ {}", errorMsg);
                return BusinessMessageProcessor.BusinessMessageProcessResult.failure(null, errorMsg);
            }
            
            log.debug("选择处理器: {} - 类型: {}", 
                    selectedProcessor.getClass().getSimpleName(), messageType);
            
            // 执行消息处理
            BusinessMessageProcessor.BusinessMessageProcessResult result = 
                    selectedProcessor.process(messagePayload, routingKey);
            
            if (result.isSuccess()) {
                log.info("✅ 业务消息处理成功 - 处理器: {}, 消息ID: {}, 分发结果: {}", 
                        selectedProcessor.getClass().getSimpleName(),
                        result.getMessageId(),
                        result.getDispatchResult() != null ? result.getDispatchResult().isSuccess() : "N/A");
            } else {
                log.error("❌ 业务消息处理失败 - 处理器: {}, 消息ID: {}, 错误: {}", 
                        selectedProcessor.getClass().getSimpleName(),
                        result.getMessageId(),
                        result.getErrorMessage());
            }
            
            return result;
            
        } catch (Exception e) {
            String errorMsg = String.format("业务消息处理异常 - 类型: %s, 路由键: %s, 错误: %s", 
                    messageType, routingKey, e.getMessage());
            log.error("💥 {}", errorMsg, e);
            return BusinessMessageProcessor.BusinessMessageProcessResult.failure(null, errorMsg);
        }
    }
    
    /**
     * 选择合适的消息处理器
     * 
     * @param messageType 消息类型
     * @param routingKey 路由键
     * @return 选中的处理器，如果没有合适的则返回null
     */
    private BusinessMessageProcessor selectProcessor(String messageType, String routingKey) {
        return processors.stream()
                .filter(processor -> processor.supports(messageType, routingKey))
                .findFirst()  // 由于已按优先级排序，取第一个即可
                .orElse(null);
    }
    
    /**
     * 获取所有已注册的处理器信息
     * 
     * @return 处理器信息列表
     */
    public List<ProcessorInfo> getProcessorInfos() {
        return processors.stream()
                .map(processor -> new ProcessorInfo(
                        processor.getClass().getSimpleName(),
                        processor.getSupportedMessageType(),
                        processor.getPriority()
                ))
                .toList();
    }
    
    /**
     * 处理器信息
     */
    public static class ProcessorInfo {
        private final String className;
        private final String supportedMessageType;
        private final int priority;
        
        public ProcessorInfo(String className, String supportedMessageType, int priority) {
            this.className = className;
            this.supportedMessageType = supportedMessageType;
            this.priority = priority;
        }
        
        // Getters
        public String getClassName() { return className; }
        public String getSupportedMessageType() { return supportedMessageType; }
        public int getPriority() { return priority; }
    }
}