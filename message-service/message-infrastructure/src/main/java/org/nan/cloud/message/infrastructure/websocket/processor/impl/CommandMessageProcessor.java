package org.nan.cloud.message.infrastructure.websocket.processor.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.utils.JsonUtils;
import org.nan.cloud.message.api.enums.Priority;
import org.nan.cloud.message.infrastructure.websocket.dispatcher.DispatchResult;
import org.nan.cloud.message.infrastructure.websocket.dispatcher.StompMessageDispatcher;
import org.nan.cloud.message.infrastructure.websocket.processor.BusinessMessageProcessor;
import org.nan.cloud.message.infrastructure.websocket.stomp.enums.StompMessageTypes;
import org.nan.cloud.message.infrastructure.websocket.stomp.enums.StompTopic;
import org.nan.cloud.message.infrastructure.websocket.stomp.model.CommonStompMessage;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 指令消息处理器
 * 
 * 负责处理LED设备控制指令相关的消息，包括：
 * 1. 单个设备指令执行结果
 * 2. 批量设备指令执行进度
 * 3. 指令执行状态变更通知
 * 4. 指令执行异常告警
 * 
 * 支持的消息类型：
 * - COMMAND_RESULT: 指令执行结果消息
 * - BATCH_COMMAND_PROGRESS: 批量指令进度消息
 * - COMMAND_ERROR: 指令执行错误消息
 * 
 * 路由键模式：
 * - stomp.command.result.{orgId}.{userId}
 * - stomp.batch.progress.{userId}.{batchId}
 * - stomp.command.error.{orgId}.{deviceId}
 * 
 * 分发策略：
 * 1. 单个指令结果：发送给执行用户和相关订阅者
 * 2. 批量指令进度：发送给任务创建者和管理员
 * 3. 指令执行错误：发送给设备管理员和系统管理员
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommandMessageProcessor implements BusinessMessageProcessor {
    
    private final StompMessageDispatcher stompDispatcher;
    
    @Override
    public String getSupportedMessageType() {
        return "COMMAND";
    }
    
    @Override
    public boolean supports(String messageType, String routingKey) {
        if (messageType == null || routingKey == null) {
            return false;
        }
        
        // 支持指令相关的消息类型
        boolean typeSupported = "COMMAND_RESULT".equalsIgnoreCase(messageType) ||
                               "BATCH_COMMAND_PROGRESS".equalsIgnoreCase(messageType) ||
                               "COMMAND_ERROR".equalsIgnoreCase(messageType) ||
                               "COMMAND".equalsIgnoreCase(messageType);
        
        // 支持指令相关的路由键模式
        boolean routingSupported = routingKey.startsWith("stomp.command.") ||
                                 routingKey.startsWith("stomp.batch.progress.") ||
                                 routingKey.contains(".command.");
        
        return typeSupported || routingSupported;
    }
    
    @Override
    public int getPriority() {
        return 10; // 高优先级，因为指令结果需要及时反馈
    }
    
    @Override
    public BusinessMessageProcessResult process(String messagePayload, String routingKey, 
                                              Map<String, Object> messageHeaders) {
        try {
            log.debug("开始处理指令消息 - 路由键: {}", routingKey);
            
            // 解析消息内容
            Map<String, Object> messageData = JsonUtils.fromJson(messagePayload, Map.class);
            
            // 根据路由键确定具体的处理策略
            if (routingKey.startsWith("stomp.command.result.")) {
                return processCommandResult(messageData, routingKey, messageHeaders);
            } else if (routingKey.startsWith("stomp.batch.progress.")) {
                return processBatchCommandProgress(messageData, routingKey, messageHeaders);
            } else if (routingKey.contains(".command.error.")) {
                return processCommandError(messageData, routingKey, messageHeaders);
            } else {
                return processGenericCommand(messageData, routingKey, messageHeaders);
            }
            
        } catch (Exception e) {
            String errorMsg = String.format("指令消息处理异常 - 路由键: %s, 错误: %s", 
                    routingKey, e.getMessage());
            log.error("💥 {}", errorMsg, e);
            return BusinessMessageProcessResult.failure(null, errorMsg);
        }
    }
    
    /**
     * 处理单个指令执行结果消息
     * 路由键：stomp.command.result.{orgId}.{userId}
     */
    private BusinessMessageProcessResult processCommandResult(Map<String, Object> messageData, 
                                                            String routingKey, 
                                                            Map<String, Object> messageHeaders) {
        try {
            log.debug("处理单个指令执行结果 - 路由键: {}", routingKey);
            
            // 提取消息字段
            String commandId = (String) messageData.get("commandId");
            String tid = (String) messageData.get("terminalId");
            Long orgId = getLongValue(messageData, "orgId");
            Long userId = getLongValue(messageData, "userId");
            String result = (String) messageData.get("result");
            
            // 验证必要字段
            if (commandId == null || tid == null || userId == null) {
                String errorMsg = "指令结果消息缺少必要字段: commandId, deviceId, userId";
                log.warn("⚠️ {}", errorMsg);
                return BusinessMessageProcessResult.failure(null, errorMsg);
            }
            
            // 构建STOMP消息
            CommonStompMessage stompMessage = buildCommandResultMessage(
                    commandId, tid, orgId, userId, result);
            
            // 分发消息
            DispatchResult dispatchResult = stompDispatcher.smartDispatch(stompMessage);
            
            log.info("✅ 指令结果消息处理完成 - 指令: {}, 设备: {}, 用户: {}, 结果: {}", 
                    commandId, tid, userId, result);
            
            return BusinessMessageProcessResult.success(stompMessage.getMessageId(), 
                    dispatchResult, stompMessage);
            
        } catch (Exception e) {
            String errorMsg = String.format("处理指令结果消息异常: %s", e.getMessage());
            log.error("💥 {}", errorMsg, e);
            return BusinessMessageProcessResult.failure(null, errorMsg);
        }
    }
    
    /**
     * 处理批量指令进度消息
     * 路由键：stomp.batch.progress.{userId}.{batchId}
     */
    private BusinessMessageProcessResult processBatchCommandProgress(Map<String, Object> messageData, 
                                                                   String routingKey, 
                                                                   Map<String, Object> messageHeaders) {
        try {
            log.debug("处理批量指令进度消息 - 路由键: {}", routingKey);
            
            // 提取消息字段
            String batchId = (String) messageData.get("batchId");
            String taskId = (String) messageData.get("taskId");
            Long orgId = getLongValue(messageData, "orgId");
            Long userId = getLongValue(messageData, "userId");
            String progress = (String) messageData.get("progress");
            Integer totalCount = getIntValue(messageData, "totalCount");
            Integer completedCount = getIntValue(messageData, "completedCount");
            Integer successCount = getIntValue(messageData, "successCount");
            Integer failureCount = getIntValue(messageData, "failureCount");
            String status = (String) messageData.get("status");
            Object progressData = messageData.get("progressData");
            
            // 验证必要字段
            if (batchId == null || userId == null) {
                String errorMsg = "批量指令进度消息缺少必要字段: batchId, userId";
                log.warn("⚠️ {}", errorMsg);
                return BusinessMessageProcessResult.failure(null, errorMsg);
            }
            
            // 构建STOMP消息
            CommonStompMessage stompMessage = buildBatchProgressMessage(
                    batchId, taskId, orgId, userId, progress, status, 
                    totalCount, completedCount, successCount, failureCount, progressData);
            
            // 分发消息
            DispatchResult dispatchResult = stompDispatcher.smartDispatch(stompMessage);
            
            log.info("✅ 批量指令进度消息处理完成 - 批量ID: {}, 用户: {}, 进度: {}/{}", 
                    batchId, userId, completedCount, totalCount);
            
            return BusinessMessageProcessResult.success(stompMessage.getMessageId(), 
                    dispatchResult, stompMessage);
            
        } catch (Exception e) {
            String errorMsg = String.format("处理批量指令进度消息异常: %s", e.getMessage());
            log.error("💥 {}", errorMsg, e);
            return BusinessMessageProcessResult.failure(null, errorMsg);
        }
    }
    
    /**
     * 处理指令执行错误消息
     */
    private BusinessMessageProcessResult processCommandError(Map<String, Object> messageData, 
                                                           String routingKey, 
                                                           Map<String, Object> messageHeaders) {
        try {
            log.debug("处理指令执行错误消息 - 路由键: {}", routingKey);
            
            // 提取消息字段
            String commandId = (String) messageData.get("commandId");
            String deviceId = (String) messageData.get("deviceId");
            Long orgId = getLongValue(messageData, "orgId");
            Long userId = getLongValue(messageData, "userId");
            String errorCode = (String) messageData.get("errorCode");
            String errorMessage = (String) messageData.get("errorMessage");
            String severity = (String) messageData.get("severity");
            Object errorDetails = messageData.get("errorDetails");
            
            // 构建STOMP告警消息
            CommonStompMessage stompMessage = buildCommandErrorMessage(
                    commandId, deviceId, orgId, userId, errorCode, errorMessage, severity, errorDetails);
            
            // 分发消息
            DispatchResult dispatchResult = stompDispatcher.smartDispatch(stompMessage);
            
            log.info("✅ 指令错误消息处理完成 - 指令: {}, 设备: {}, 错误码: {}", 
                    commandId, deviceId, errorCode);
            
            return BusinessMessageProcessResult.success(stompMessage.getMessageId(), 
                    dispatchResult, stompMessage);
            
        } catch (Exception e) {
            String errorMsg = String.format("处理指令错误消息异常: %s", e.getMessage());
            log.error("💥 {}", errorMsg, e);
            return BusinessMessageProcessResult.failure(null, errorMsg);
        }
    }
    
    /**
     * 处理通用指令消息
     */
    private BusinessMessageProcessResult processGenericCommand(Map<String, Object> messageData, 
                                                             String routingKey, 
                                                             Map<String, Object> messageHeaders) {
        try {
            log.debug("处理通用指令消息 - 路由键: {}", routingKey);
            
            // 构建通用指令消息
            String messageId = UUID.randomUUID().toString();
            
            CommonStompMessage stompMessage = CommonStompMessage.builder()
                    .messageId(messageId)
                    .timestamp(LocalDateTime.now())
                    .messageType(StompMessageTypes.COMMAND_FEEDBACK)
                    .subType_1("GENERIC")
                    .source(CommonStompMessage.Source.builder()
                            .serviceId("message-service")
                            .resourceType("COMMAND")
                            .resourceId(routingKey)
                            .build())
                    .target(CommonStompMessage.Target.builder()
                            .targetType("TOPIC")
                            .destination(StompTopic.USER_MESSAGES_QUEUE)
                            .build())
                    .payload(messageData)
                    .message("通用指令消息: " + routingKey)
                    .metadata(CommonStompMessage.Metadata.builder()
                            .priority(Priority.NORMAL)
                            .persistent(false)
                            .ttl(60000L)  // 1分钟TTL
                            .build())
                    .build();
            
            // 分发消息
            DispatchResult dispatchResult = stompDispatcher.smartDispatch(stompMessage);
            
            log.info("✅ 通用指令消息处理完成 - 路由键: {}", routingKey);
            
            return BusinessMessageProcessResult.success(messageId, dispatchResult, stompMessage);
            
        } catch (Exception e) {
            String errorMsg = String.format("处理通用指令消息异常: %s", e.getMessage());
            log.error("💥 {}", errorMsg, e);
            return BusinessMessageProcessResult.failure(null, errorMsg);
        }
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 构建指令执行结果STOMP消息
     */
    private CommonStompMessage buildCommandResultMessage(String commandId, String deviceId, 
                                                        Long orgId, Long userId, String result) {
        return CommonStompMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .messageType(StompMessageTypes.COMMAND_FEEDBACK)
                .subType_1("SINGLE")
                .subType_2(result)
                .source(CommonStompMessage.Source.builder()
                        .serviceId("terminal-service")
                        .resourceType("COMMAND")
                        .executionId(commandId)
                        .build())
                .target(CommonStompMessage.Target.builder()
                        .targetType("USER_AND_TERMINAL")
                        .uids(List.of(userId))
                        .oid(orgId)
                        .destination(StompTopic.USER_MESSAGES_QUEUE + "," +
                                  StompTopic.buildDeviceTopic(deviceId))
                        .build())
                .payload(Map.of(
                        "commandId", commandId,
                        "deviceId", deviceId,
                        "result", result != null ? result : "",
                        "timestamp", LocalDateTime.now()
                ))
                .message(String.format("设备%s指令%s执行结果: %s", deviceId, commandId, result))
                .metadata(CommonStompMessage.Metadata.builder()
                        .priority(Priority.HIGH)
                        .persistent(true)
                        .ttl(300000L)  // 5分钟TTL
                        .requireAck(true)
                        .correlationId(commandId)
                        .build())
                .build();
    }
    
    /**
     * 构建批量指令进度STOMP消息
     */
    private CommonStompMessage buildBatchProgressMessage(String batchId, String taskId, Long orgId, 
                                                       Long userId, String progress, String status,
                                                       Integer totalCount, Integer completedCount,
                                                       Integer successCount, Integer failureCount,
                                                       Object progressData) {
        return CommonStompMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .messageType(StompMessageTypes.TASK_PROGRESS)
                .subType_1("BATCH_COMMAND")
                .subType_2(status != null ? status : "PROGRESS")
                .source(CommonStompMessage.Source.builder()
                        .serviceId("core-service")
                        .resourceType("BATCH_COMMAND")
                        .resourceId(batchId)
                        .taskId(taskId != null ? taskId : batchId)
                        .batchContext("COMMAND_BATCH")
                        .build())
                .target(CommonStompMessage.Target.builder()
                        .targetType("USER_AND_TOPIC")
                        .uids(List.of(userId))
                        .oid(orgId)
                        .destination(StompTopic.buildBatchAggTopic(batchId) + "," +
                                  StompTopic.USER_MESSAGES_QUEUE)
                        .build())
                .payload(Map.of(
                        "batchId", batchId,
                        "taskId", taskId != null ? taskId : batchId,
                        "progress", progress != null ? progress : "",
                        "status", status != null ? status : "",
                        "totalCount", totalCount != null ? totalCount : 0,
                        "completedCount", completedCount != null ? completedCount : 0,
                        "successCount", successCount != null ? successCount : 0,
                        "failureCount", failureCount != null ? failureCount : 0,
                        "progressData", progressData != null ? progressData : Map.of(),
                        "timestamp", LocalDateTime.now()
                ))
                .message(String.format("批量任务%s进度更新: %s/%s", batchId, completedCount, totalCount))
                .metadata(CommonStompMessage.Metadata.builder()
                        .priority(Priority.NORMAL)
                        .persistent(false)
                        .ttl(180000L)  // 3分钟TTL
                        .sequenceId(System.currentTimeMillis())
                        .build())
                .build();
    }
    
    /**
     * 构建指令错误STOMP消息
     */
    private CommonStompMessage buildCommandErrorMessage(String commandId, String deviceId, 
                                                      Long orgId, Long userId, String errorCode, 
                                                      String errorMessage, String severity, 
                                                      Object errorDetails) {
        return CommonStompMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .messageType(StompMessageTypes.ALERT)
                .subType_1("COMMAND_ERROR")
                .subType_2(severity != null ? severity : "ERROR")
                .source(CommonStompMessage.Source.builder()
                        .serviceId("terminal-service")
                        .resourceType("COMMAND")
                        .resourceId(commandId)
                        .build())
                .target(CommonStompMessage.Target.builder()
                        .targetType("USER_AND_ORG")
                        .uids(userId != null ? List.of(userId) : null)
                        .oid(orgId)
                        .destination(StompTopic.USER_MESSAGES_QUEUE)
                        .build())
                .payload(Map.of(
                        "commandId", commandId != null ? commandId : "",
                        "deviceId", deviceId != null ? deviceId : "",
                        "errorCode", errorCode != null ? errorCode : "",
                        "errorMessage", errorMessage != null ? errorMessage : "",
                        "severity", severity != null ? severity : "ERROR",
                        "errorDetails", errorDetails != null ? errorDetails : Map.of(),
                        "timestamp", LocalDateTime.now()
                ))
                .message(String.format("指令错误告警 - 设备: %s, 错误: %s", deviceId, errorMessage))
                .metadata(CommonStompMessage.Metadata.builder()
                        .priority("CRITICAL".equalsIgnoreCase(severity) ? Priority.HIGH : Priority.NORMAL)
                        .persistent(true)
                        .ttl(86400000L)  // 24小时TTL
                        .requireAck(true)
                        .build())
                .build();
    }
    
    /**
     * 安全获取Long值
     */
    private Long getLongValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            log.warn("无法将{}转换为Long类型: {}", key, value);
            return null;
        }
    }
    
    /**
     * 安全获取Integer值
     */
    private Integer getIntValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            log.warn("无法将{}转换为Integer类型: {}", key, value);
            return null;
        }
    }
}