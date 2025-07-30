package org.nan.cloud.message.infrastructure.mq.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.infrastructure.mq.converter.MqToStompMessageConverter;
import org.nan.cloud.message.infrastructure.websocket.dispatcher.StompMessageDispatcher;
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
 * 3. 调用转换器转换为STOMP消息
 * 4. 通过STOMP分发器推送给前端
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqStompBridgeListener {
    
    private final MqToStompMessageConverter messageConverter;
    private final StompMessageDispatcher stompDispatcher;
    private final ObjectMapper objectMapper;
    
    /**
     * 监听设备状态变更消息
     * 队列：device.status.queue
     * 路由键：device.status.{orgId}.{deviceId}
     */
    @RabbitListener(queues = "device.status.queue")
    public void handleDeviceStatusMessage(@Payload String messagePayload, 
                                        @Header Map<String, Object> headers,
                                        @Header("routingKey") String routingKey) {
        try {
            log.debug("收到设备状态消息 - 路由键: {}", routingKey);
            
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
                
                log.info("✅ 设备状态消息桥接完成 - 设备: {}, 状态: {}, 分发结果: {}", 
                        deviceId, status, dispatchResult.isSuccess());
            } else {
                log.warn("❌ 设备状态消息转换失败 - 设备: {}", deviceId);
            }
            
        } catch (Exception e) {
            log.error("设备状态消息桥接异常 - 路由键: {}, 错误: {}", routingKey, e.getMessage(), e);
        }
    }
    
    /**
     * 监听指令执行结果消息
     * 队列：command.result.queue
     * 路由键：command.result.{orgId}.{deviceId}
     */
    @RabbitListener(queues = "command.result.queue")
    public void handleCommandResultMessage(@Payload String messagePayload,
                                         @Header Map<String, Object> headers,
                                         @Header("routingKey") String routingKey) {
        try {
            log.debug("收到指令执行结果消息 - 路由键: {}", routingKey);
            
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
                
                log.info("✅ 指令结果消息桥接完成 - 指令: {}, 设备: {}, 用户: {}, 分发结果: {}", 
                        commandId, deviceId, userId, dispatchResult.isSuccess());
            } else {
                log.warn("❌ 指令结果消息转换失败 - 指令: {}", commandId);
            }
            
        } catch (Exception e) {
            log.error("指令结果消息桥接异常 - 路由键: {}, 错误: {}", routingKey, e.getMessage(), e);
        }
    }
    
    /**
     * 监听系统通知消息
     * 队列：system.notification.queue
     * 路由键：notification.{type}.{orgId}
     */
    @RabbitListener(queues = "system.notification.queue")
    public void handleSystemNotificationMessage(@Payload String messagePayload,
                                              @Header Map<String, Object> headers,
                                              @Header("routingKey") String routingKey) {
        try {
            log.debug("收到系统通知消息 - 路由键: {}", routingKey);
            
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
                
                log.info("✅ 系统通知消息桥接完成 - 类型: {}, 组织: {}, 目标用户数: {}, 分发结果: {}", 
                        notificationType, orgId, 
                        targetUserIds != null ? targetUserIds.size() : 0, 
                        dispatchResult.isSuccess());
            } else {
                log.warn("❌ 系统通知消息转换失败 - 类型: {}", notificationType);
            }
            
        } catch (Exception e) {
            log.error("系统通知消息桥接异常 - 路由键: {}, 错误: {}", routingKey, e.getMessage(), e);
        }
    }
    
    /**
     * 监听批量指令进度消息
     * 队列：batch.command.progress.queue
     * 路由键：batch.progress.{orgId}.{batchId}
     */
    @RabbitListener(queues = "batch.command.progress.queue")
    public void handleBatchCommandProgressMessage(@Payload String messagePayload,
                                                @Header Map<String, Object> headers,
                                                @Header("routingKey") String routingKey) {
        try {
            log.debug("收到批量指令进度消息 - 路由键: {}", routingKey);
            
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
                
                log.info("✅ 批量指令进度消息桥接完成 - 批量任务: {}, 用户: {}, 进度: {}, 分发结果: {}", 
                        batchId, userId, progress, dispatchResult.isSuccess());
            } else {
                log.warn("❌ 批量指令进度消息转换失败 - 批量任务: {}", batchId);
            }
            
        } catch (Exception e) {
            log.error("批量指令进度消息桥接异常 - 路由键: {}, 错误: {}", routingKey, e.getMessage(), e);
        }
    }
    
    /**
     * 通用消息桥接监听器
     * 队列：stomp.bridge.queue
     * 用于处理其他类型的消息桥接需求
     */
    @RabbitListener(queues = "stomp.bridge.queue")
    public void handleGenericBridgeMessage(@Payload String messagePayload,
                                         @Header Map<String, Object> headers,
                                         @Header("routingKey") String routingKey,
                                         @Header(value = "messageType", required = false) String messageType) {
        try {
            log.debug("收到通用桥接消息 - 路由键: {}, 消息类型: {}", routingKey, messageType);
            
            if (messageType == null) {
                log.warn("通用桥接消息缺少messageType头部信息 - 路由键: {}", routingKey);
                return;
            }
            
            // 解析消息内容
            Map<String, Object> messageData = objectMapper.readValue(messagePayload, Map.class);
            
            // 使用通用转换器转换消息
            CommonStompMessage stompMessage = messageConverter.convertGenericMqMessage(
                    messageType, messageData, routingKey);
            
            if (stompMessage != null) {
                // 分发STOMP消息
                var dispatchResult = stompDispatcher.smartDispatch(stompMessage);
                
                log.info("✅ 通用消息桥接完成 - 类型: {}, 路由键: {}, 分发结果: {}", 
                        messageType, routingKey, dispatchResult.isSuccess());
            } else {
                log.warn("❌ 通用消息转换失败 - 类型: {}, 路由键: {}", messageType, routingKey);
            }
            
        } catch (Exception e) {
            log.error("通用消息桥接异常 - 路由键: {}, 消息类型: {}, 错误: {}", 
                    routingKey, messageType, e.getMessage(), e);
        }
    }
    
    /**
     * 处理消息桥接失败的情况
     * 队列：stomp.bridge.dlq (死信队列)
     */
    @RabbitListener(queues = "stomp.bridge.dlq")
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