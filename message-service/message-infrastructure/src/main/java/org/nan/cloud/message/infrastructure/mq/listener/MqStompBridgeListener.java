package org.nan.cloud.message.infrastructure.mq.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.infrastructure.mq.config.MessageServiceRabbitConfig;
import org.nan.cloud.message.infrastructure.mq.converter.MqToStompMessageConverter;
import org.nan.cloud.message.infrastructure.websocket.dispatcher.StompMessageDispatcher;
import org.nan.cloud.message.infrastructure.websocket.processor.BusinessMessageProcessor;
import org.nan.cloud.message.infrastructure.websocket.processor.BusinessMessageProcessorManager;
import org.nan.cloud.message.infrastructure.websocket.stomp.model.CommonStompMessage;
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
    private final MqToStompMessageConverter messageConverter;
    private final StompMessageDispatcher stompDispatcher;
    private final ObjectMapper objectMapper;
    
    /**
     * 监听设备状态变更消息
     * 队列：stomp.device.status.queue
     * 路由键：stomp.device.status.{orgId}.{deviceId}
     */
    @RabbitListener(queues = MessageServiceRabbitConfig.DEVICE_STATUS_QUEUE)
    public void handleDeviceStatusMessage(@Payload String messagePayload, 
                                        @Header Map<String, Object> headers,
                                        @Header("routingKey") String routingKey) {
        try {
            log.debug("收到设备状态消息 - 路由键: {}", routingKey);
            
            // 优先使用业务消息处理器管理器
            BusinessMessageProcessor.BusinessMessageProcessResult processResult = 
                    processorManager.processMessage("DEVICE_STATUS", messagePayload, routingKey, headers);
            
            if (processResult.isSuccess()) {
                log.info("✅ 设备状态消息处理完成 - 路由键: {}, 消息ID: {}, 分发结果: {}", 
                        routingKey, processResult.getMessageId(), 
                        processResult.getDispatchResult() != null ? processResult.getDispatchResult().isSuccess() : "N/A");
                return;
            }
            
            // 降级到原有处理逻辑
            log.info("⬇️ 设备状态消息处理器失败，降级到原有逻辑 - 路由键: {}, 错误: {}", 
                    routingKey, processResult.getErrorMessage());
            
            handleDeviceStatusMessageLegacy(messagePayload, headers, routingKey);
            
        } catch (Exception e) {
            log.error("设备状态消息桥接异常 - 路由键: {}, 错误: {}", routingKey, e.getMessage(), e);
            
            // 异常情况下也尝试降级处理
            try {
                handleDeviceStatusMessageLegacy(messagePayload, headers, routingKey);
            } catch (Exception fallbackException) {
                log.error("设备状态消息降级处理也失败 - 路由键: {}, 错误: {}", routingKey, fallbackException.getMessage(), fallbackException);
            }
        }
    }
    
    /**
     * 设备状态消息降级处理方法（原有逻辑）
     */
    private void handleDeviceStatusMessageLegacy(@Payload String messagePayload, 
                                               @Header Map<String, Object> headers,
                                               @Header("routingKey") String routingKey) {
        try {
            // 解析消息内容
            Map<String, Object> messageData = objectMapper.readValue(messagePayload, Map.class);
            
            String deviceId = (String) messageData.get("deviceId");
            Long orgId = Long.valueOf(messageData.get("orgId").toString());
            String status = (String) messageData.get("status");
            Object statusData = messageData.get("statusData");
            
            // 转换为STOMP消息
            CommonStompMessage stompMessage = messageConverter.convertDeviceStatusMessage(
                    deviceId, orgId, status, statusData);
            
            if (stompMessage != null) {
                // 分发STOMP消息
                var dispatchResult = stompDispatcher.smartDispatch(stompMessage);
                
                log.info("✅ 设备状态消息降级处理完成 - 设备: {}, 状态: {}, 分发结果: {}", 
                        deviceId, status, dispatchResult.isSuccess());
            } else {
                log.warn("❌ 设备状态消息转换失败 - 设备: {}", deviceId);
            }
            
        } catch (Exception e) {
            log.error("设备状态消息降级处理异常 - 路由键: {}, 错误: {}", routingKey, e.getMessage(), e);
        }
    }
    
    /**
     * 监听指令执行结果消息
     * 队列：stomp.command.result.queue
     * 路由键：stomp.command.result.{orgId}.{userId}
     */
    @RabbitListener(queues = MessageServiceRabbitConfig.COMMAND_RESULT_QUEUE)
    public void handleCommandResultMessage(@Payload String messagePayload,
                                         @Header Map<String, Object> headers,
                                         @Header("routingKey") String routingKey) {
        try {
            log.debug("收到指令执行结果消息 - 路由键: {}", routingKey);
            
            // 优先使用业务消息处理器管理器
            BusinessMessageProcessor.BusinessMessageProcessResult processResult = 
                    processorManager.processMessage("COMMAND_RESULT", messagePayload, routingKey, headers);
            
            if (processResult.isSuccess()) {
                log.info("✅ 指令结果消息处理完成 - 路由键: {}, 消息ID: {}, 分发结果: {}", 
                        routingKey, processResult.getMessageId(), 
                        processResult.getDispatchResult() != null ? processResult.getDispatchResult().isSuccess() : "N/A");
                return;
            }
            
            // 降级到原有处理逻辑
            log.info("⬇️ 指令结果消息处理器失败，降级到原有逻辑 - 路由键: {}, 错误: {}", 
                    routingKey, processResult.getErrorMessage());
            
            handleCommandResultMessageLegacy(messagePayload, headers, routingKey);
            
        } catch (Exception e) {
            log.error("指令结果消息桥接异常 - 路由键: {}, 错误: {}", routingKey, e.getMessage(), e);
            
            // 异常情况下也尝试降级处理
            try {
                handleCommandResultMessageLegacy(messagePayload, headers, routingKey);
            } catch (Exception fallbackException) {
                log.error("指令结果消息降级处理也失败 - 路由键: {}, 错误: {}", routingKey, fallbackException.getMessage(), fallbackException);
            }
        }
    }
    
    /**
     * 指令结果消息降级处理方法（原有逻辑）
     */
    private void handleCommandResultMessageLegacy(@Payload String messagePayload,
                                                @Header Map<String, Object> headers,
                                                @Header("routingKey") String routingKey) {
        try {
            // 解析消息内容
            Map<String, Object> messageData = objectMapper.readValue(messagePayload, Map.class);
            
            String commandId = (String) messageData.get("commandId");
            String deviceId = (String) messageData.get("deviceId");
            Long orgId = Long.valueOf(messageData.get("orgId").toString());
            Long userId = Long.valueOf(messageData.get("userId").toString());
            String result = (String) messageData.get("result");
            Object resultData = messageData.get("resultData");
            
            // 转换为STOMP消息
            CommonStompMessage stompMessage = messageConverter.convertCommandResultMessage(
                    commandId, deviceId, orgId, userId, result, resultData);
            
            if (stompMessage != null) {
                // 分发STOMP消息
                var dispatchResult = stompDispatcher.smartDispatch(stompMessage);
                
                log.info("✅ 指令结果消息降级处理完成 - 指令: {}, 设备: {}, 用户: {}, 分发结果: {}", 
                        commandId, deviceId, userId, dispatchResult.isSuccess());
            } else {
                log.warn("❌ 指令结果消息转换失败 - 指令: {}", commandId);
            }
            
        } catch (Exception e) {
            log.error("指令结果消息降级处理异常 - 路由键: {}, 错误: {}", routingKey, e.getMessage(), e);
        }
    }
    
    /**
     * 监听系统通知消息
     * 队列：system.notification.queue
     * 路由键：notification.{type}.{orgId}
     */
    @RabbitListener(queues = MessageServiceRabbitConfig.SYSTEM_NOTIFICATION_QUEUE)
    public void handleSystemNotificationMessage(@Payload String messagePayload,
                                              @Header Map<String, Object> headers,
                                              @Header("routingKey") String routingKey) {
        try {
            log.debug("收到系统通知消息 - 路由键: {}", routingKey);
            
            // 优先使用业务消息处理器管理器
            BusinessMessageProcessor.BusinessMessageProcessResult processResult = 
                    processorManager.processMessage("SYSTEM_NOTIFICATION", messagePayload, routingKey, headers);
            
            if (processResult.isSuccess()) {
                log.info("✅ 系统通知消息处理完成 - 路由键: {}, 消息ID: {}, 分发结果: {}", 
                        routingKey, processResult.getMessageId(), 
                        processResult.getDispatchResult() != null ? processResult.getDispatchResult().isSuccess() : "N/A");
                return;
            }
            
            // 降级到原有处理逻辑
            log.info("⬇️ 系统通知消息处理器失败，降级到原有逻辑 - 路由键: {}, 错误: {}", 
                    routingKey, processResult.getErrorMessage());
            
            handleSystemNotificationMessageLegacy(messagePayload, headers, routingKey);
            
        } catch (Exception e) {
            log.error("系统通知消息桥接异常 - 路由键: {}, 错误: {}", routingKey, e.getMessage(), e);
            
            // 异常情况下也尝试降级处理
            try {
                handleSystemNotificationMessageLegacy(messagePayload, headers, routingKey);
            } catch (Exception fallbackException) {
                log.error("系统通知消息降级处理也失败 - 路由键: {}, 错误: {}", routingKey, fallbackException.getMessage(), fallbackException);
            }
        }
    }
    
    /**
     * 系统通知消息降级处理方法（原有逻辑）
     */
    private void handleSystemNotificationMessageLegacy(@Payload String messagePayload,
                                                     @Header Map<String, Object> headers,
                                                     @Header("routingKey") String routingKey) {
        try {
            // 解析消息内容
            Map<String, Object> messageData = objectMapper.readValue(messagePayload, Map.class);
            
            String notificationType = (String) messageData.get("notificationType");
            Long orgId = Long.valueOf(messageData.get("orgId").toString());
            Object targetUserIdsObj = messageData.get("targetUserIds");
            String title = (String) messageData.get("title");
            String content = (String) messageData.get("content");
            Object notificationData = messageData.get("notificationData");
            
            // 处理目标用户ID列表
            java.util.List<Long> targetUserIds = null;
            if (targetUserIdsObj instanceof java.util.List) {
                targetUserIds = ((java.util.List<?>) targetUserIdsObj).stream()
                        .map(id -> Long.valueOf(id.toString()))
                        .collect(java.util.stream.Collectors.toList());
            }
            
            // 转换为STOMP消息
            CommonStompMessage stompMessage = messageConverter.convertSystemNotificationMessage(
                    notificationType, orgId, targetUserIds, title, content, notificationData);
            
            if (stompMessage != null) {
                // 分发STOMP消息
                var dispatchResult = stompDispatcher.smartDispatch(stompMessage);
                
                log.info("✅ 系统通知消息降级处理完成 - 类型: {}, 组织: {}, 目标用户数: {}, 分发结果: {}", 
                        notificationType, orgId, 
                        targetUserIds != null ? targetUserIds.size() : 0, 
                        dispatchResult.isSuccess());
            } else {
                log.warn("❌ 系统通知消息转换失败 - 类型: {}", notificationType);
            }
            
        } catch (Exception e) {
            log.error("系统通知消息降级处理异常 - 路由键: {}, 错误: {}", routingKey, e.getMessage(), e);
        }
    }
    
    /**
     * 监听批量指令进度消息
     * 队列：batch.command.progress.queue
     * 路由键：batch.progress.{orgId}.{batchId}
     */
    @RabbitListener(queues = MessageServiceRabbitConfig.BATCH_PROGRESS_QUEUE)
    public void handleBatchCommandProgressMessage(@Payload String messagePayload,
                                                @Header Map<String, Object> headers,
                                                @Header("routingKey") String routingKey) {
        try {
            log.debug("收到批量指令进度消息 - 路由键: {}", routingKey);
            
            // 优先使用业务消息处理器管理器
            BusinessMessageProcessor.BusinessMessageProcessResult processResult = 
                    processorManager.processMessage("BATCH_COMMAND_PROGRESS", messagePayload, routingKey, headers);
            
            if (processResult.isSuccess()) {
                log.info("✅ 批量指令进度消息处理完成 - 路由键: {}, 消息ID: {}, 分发结果: {}", 
                        routingKey, processResult.getMessageId(), 
                        processResult.getDispatchResult() != null ? processResult.getDispatchResult().isSuccess() : "N/A");
                return;
            }
            
            // 降级到原有处理逻辑
            log.info("⬇️ 批量指令进度消息处理器失败，降级到原有逻辑 - 路由键: {}, 错误: {}", 
                    routingKey, processResult.getErrorMessage());
            
            handleBatchCommandProgressMessageLegacy(messagePayload, headers, routingKey);
            
        } catch (Exception e) {
            log.error("批量指令进度消息桥接异常 - 路由键: {}, 错误: {}", routingKey, e.getMessage(), e);
            
            // 异常情况下也尝试降级处理
            try {
                handleBatchCommandProgressMessageLegacy(messagePayload, headers, routingKey);
            } catch (Exception fallbackException) {
                log.error("批量指令进度消息降级处理也失败 - 路由键: {}, 错误: {}", routingKey, fallbackException.getMessage(), fallbackException);
            }
        }
    }
    
    /**
     * 批量指令进度消息降级处理方法（原有逻辑）
     */
    private void handleBatchCommandProgressMessageLegacy(@Payload String messagePayload,
                                                       @Header Map<String, Object> headers,
                                                       @Header("routingKey") String routingKey) {
        try {
            // 解析消息内容
            Map<String, Object> messageData = objectMapper.readValue(messagePayload, Map.class);
            
            String batchId = (String) messageData.get("batchId");
            Long orgId = Long.valueOf(messageData.get("orgId").toString());
            Long userId = Long.valueOf(messageData.get("userId").toString());
            String progress = (String) messageData.get("progress");
            Object progressData = messageData.get("progressData");
            
            // 转换为STOMP消息
            CommonStompMessage stompMessage = messageConverter.convertBatchCommandProgressMessage(
                    batchId, orgId, userId, progress, progressData);
            
            if (stompMessage != null) {
                // 分发STOMP消息
                var dispatchResult = stompDispatcher.smartDispatch(stompMessage);
                
                log.info("✅ 批量指令进度消息降级处理完成 - 批量任务: {}, 用户: {}, 进度: {}, 分发结果: {}", 
                        batchId, userId, progress, dispatchResult.isSuccess());
            } else {
                log.warn("❌ 批量指令进度消息转换失败 - 批量任务: {}", batchId);
            }
            
        } catch (Exception e) {
            log.error("批量指令进度消息降级处理异常 - 路由键: {}, 错误: {}", routingKey, e.getMessage(), e);
        }
    }
    
    
    /**
     * 处理消息桥接失败的情况
     * 队列：stomp.bridge.dlq (统一死信队列)
     */
    @RabbitListener(queues = MessageServiceRabbitConfig.BRIDGE_DLQ)
    public void handleBridgeFailureMessage(@Payload String messagePayload,
                                         @Header Map<String, Object> headers,
                                         @Header("routingKey") String routingKey) {
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