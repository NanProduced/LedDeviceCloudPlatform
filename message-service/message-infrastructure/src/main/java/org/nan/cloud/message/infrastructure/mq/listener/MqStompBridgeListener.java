package org.nan.cloud.message.infrastructure.mq.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.utils.JsonUtils;
import org.nan.cloud.common.mq.core.message.Message;
import org.nan.cloud.message.infrastructure.mq.config.MessageServiceRabbitConfig;
import org.nan.cloud.message.infrastructure.websocket.processor.BusinessMessageProcessor;
import org.nan.cloud.message.infrastructure.websocket.processor.BusinessMessageProcessorManager;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * MQ到STOMP消息桥接监听器
 * 
 * 监听来自其他业务服务的RabbitMQ消息，将其转换为STOMP消息并推送给前端用户。
 * 支持多种消息类型的桥接，包括设备状态、指令结果、系统通知、批量任务进度等。
 * 
 * 消息流程：
 * 1. 监听RabbitMQ队列消息
 * 2. 解析消息类型和内容
 * 3. 使用业务消息处理器管理器选择合适的处理器
 * 4. 处理器负责转换为STOMP消息并进行分发
 * 5. 保留原有转换器作为降级备选方案
 * 
 * 架构升级：
 * - 引入业务消息处理器架构，支持策略模式
 * - 保持向后兼容性，原有逻辑作为降级方案
 * - 提供更灵活的消息处理和分发机制
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqStompBridgeListener {
    
    private final BusinessMessageProcessorManager processorManager;
    
    /**
     * 监听设备状态变更消息
     * 队列：stomp.device.status.queue
     * 路由键：stomp.device.status.{orgId}.{deviceId}
     */
    @RabbitListener(queues = MessageServiceRabbitConfig.DEVICE_STATUS_QUEUE)
    public void handleDeviceStatusMessage(@Payload Message message,
                                        @Header(name = "amqp_receivedRoutingKey", required = false) String routingKey) {
        try {
            log.debug("收到设备状态消息 - 路由键: {}", routingKey);

            String messagePayload = JsonUtils.toJson(message.getPayload());

            // 优先使用业务消息处理器管理器
            BusinessMessageProcessor.BusinessMessageProcessResult processResult = 
                    processorManager.processMessage("TERMINAL_STATUS", messagePayload, routingKey);
            
            if (processResult.isSuccess()) {
                log.info("✅ 设备状态消息处理完成 - 路由键: {}, 消息ID: {}, 分发结果: {}", 
                        routingKey, processResult.getMessageId(), 
                        processResult.getDispatchResult() != null ? processResult.getDispatchResult().isSuccess() : "N/A");
            }
            
        } catch (Exception e) {
            log.error("设备状态消息桥接异常 - 路由键: {}, 错误: {}", routingKey, e.getMessage(), e);

        }
    }

    
    /**
     * 监听指令执行结果消息
     * 队列：stomp.command.result.queue
     * 路由键：stomp.command.result.{orgId}.{userId}
     */
    @RabbitListener(queues = MessageServiceRabbitConfig.COMMAND_RESULT_QUEUE)
    public void handleCommandResultMessage(@Payload Message message,
                                         @Header(name = "amqp_receivedRoutingKey", required = false) String routingKey) {
        try {
            log.debug("收到指令执行结果消息 - 路由键: {}", routingKey);
            
            // 优先使用业务消息处理器管理器
            String messagePayload = JsonUtils.toJson(message.getPayload());
            BusinessMessageProcessor.BusinessMessageProcessResult processResult = 
                    processorManager.processMessage("COMMAND_RESULT", messagePayload, routingKey);
            
            if (processResult.isSuccess()) {
                log.info("✅ 指令结果消息处理完成 - 路由键: {}, 消息ID: {}, 分发结果: {}", 
                        routingKey, processResult.getMessageId(), 
                        processResult.getDispatchResult() != null ? processResult.getDispatchResult().isSuccess() : "N/A");
            }

            
        } catch (Exception e) {
            log.error("指令结果消息桥接异常 - 路由键: {}, 错误: {}", routingKey, e.getMessage(), e);
        }
    }
    
    /**
     * 监听文件上传消息
     * 队列：stomp.file.upload.queue
     * 路由键：stomp.file.upload.{orgId}.{userId}
     */
    @RabbitListener(queues = MessageServiceRabbitConfig.FILE_UPLOAD_QUEUE)
    public void handleFileUploadMessage(@Payload Message message,
                                      @Header(name = "amqp_receivedRoutingKey", required = false) String routingKey) {
        try {
            log.info("🔍 DEBUG: 收到文件上传消息 - 路由键: {}, 消息类型: {}, 载荷: {}", 
                    routingKey, message.getMessageType(), message.getPayload());
            
            String messagePayload = JsonUtils.toJson(message.getPayload());
            BusinessMessageProcessor.BusinessMessageProcessResult processResult = 
                    processorManager.processMessage(message.getMessageType(), messagePayload, routingKey);
            
            if (processResult.isSuccess()) {
                log.info("✅ 文件上传消息处理完成 - 路由键: {}, 消息ID: {}, 分发结果: {}", 
                        routingKey, processResult.getMessageId(), 
                        processResult.getDispatchResult() != null ? processResult.getDispatchResult().isSuccess() : "N/A");
            }
            
        } catch (Exception e) {
            log.error("文件上传消息桥接异常 - 路由键: {}, 错误: {}", routingKey, e.getMessage(), e);
        }
    }

    
    /**
     * 监听系统通知消息
     * 队列：system.notification.queue
     * 路由键：notification.{type}.{orgId}
     */
    @RabbitListener(queues = MessageServiceRabbitConfig.SYSTEM_NOTIFICATION_QUEUE)
    public void handleSystemNotificationMessage(@Payload Message message,
                                              @Header(name = "amqp_receivedRoutingKey", required = false) String routingKey) {
        try {
            log.debug("收到系统通知消息 - 路由键: {}", routingKey);

            String messagePayload = JsonUtils.toJson(message.getPayload());

            // 优先使用业务消息处理器管理器
            BusinessMessageProcessor.BusinessMessageProcessResult processResult = 
                    processorManager.processMessage("SYSTEM_NOTIFICATION", messagePayload, routingKey);
            
            if (processResult.isSuccess()) {
                log.info("✅ 系统通知消息处理完成 - 路由键: {}, 消息ID: {}, 分发结果: {}", 
                        routingKey, processResult.getMessageId(), 
                        processResult.getDispatchResult() != null ? processResult.getDispatchResult().isSuccess() : "N/A");
            }

            
        } catch (Exception e) {
            log.error("系统通知消息桥接异常 - 路由键: {}, 错误: {}", routingKey, e.getMessage(), e);

        }
    }
    

    


    
    /**
     * 安全获取Long值
     */
    private Long getLongValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 从完整的Message对象中提取payload部分
     * 
     * @param message Message对象
     * @return payload部分的Map数据
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractPayloadFromMessage(Message message) {
        Object payloadObj = message.getPayload();
        
        // 如果payload是Map类型，直接返回
        if (payloadObj instanceof Map) {
            log.debug("从Message对象中提取payload成功");
            return (Map<String, Object>) payloadObj;
        }
        
        // 如果payload是字符串，尝试解析为Map
        if (payloadObj instanceof String) {
            try {
                Map<String, Object> payloadMap = JsonUtils.fromJson((String) payloadObj, Map.class);
                log.debug("从字符串payload中解析Map成功");
                return payloadMap;
            } catch (Exception e) {
                log.warn("解析字符串payload失败: {}", e.getMessage());
                throw new RuntimeException("无法解析payload内容", e);
            }
        }
        
        log.warn("不支持的payload类型: {}", payloadObj != null ? payloadObj.getClass() : "null");
        throw new RuntimeException("不支持的payload类型");
    }
    
    
    /**
     * 处理消息桥接失败的情况
     * 队列：stomp.bridge.dlq (统一死信队列)
     */
    @RabbitListener(queues = MessageServiceRabbitConfig.BRIDGE_DLQ)
    public void handleBridgeFailureMessage(@Payload String messagePayload,
                                         @Header Map<String, Object> headers,
                                         @Header(name = "amqp_receivedRoutingKey", required = false) String routingKey) {
        try {
            log.error("收到消息桥接失败消息 - 路由键: {}", routingKey);
            
            // 记录失败的消息内容用于后续分析
            log.error("失败消息内容: {}", messagePayload);
            log.error("消息头部信息: {}", headers);
            
            // TODO: 可以在这里实现失败消息的重试逻辑或者告警机制
            // 比如：
            // 1. 将失败消息保存到数据库
            // 2. 发送告警通知给运维人员
            // 3. 根据失败原因尝试修复和重试
            
        } catch (Exception e) {
            log.error("处理桥接失败消息异常 - 路由键: {}, 错误: {}", routingKey, e.getMessage(), e);
        }
    }
}