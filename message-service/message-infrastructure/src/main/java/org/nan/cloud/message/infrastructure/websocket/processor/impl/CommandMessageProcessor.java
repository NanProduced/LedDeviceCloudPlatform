package org.nan.cloud.message.infrastructure.websocket.processor.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.exception.BaseException;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.common.basic.utils.JsonUtils;
import org.nan.cloud.message.api.enums.Priority;
import org.nan.cloud.message.infrastructure.websocket.dispatcher.DispatchResult;
import org.nan.cloud.message.infrastructure.websocket.dispatcher.StompMessageDispatcher;
import org.nan.cloud.message.infrastructure.websocket.processor.BusinessMessageProcessor;
import org.nan.cloud.message.api.stomp.StompMessageTypes;
import org.nan.cloud.message.api.stomp.CommonStompMessage;
import org.springframework.stereotype.Component;

import java.time.Instant;
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
        boolean typeSupported = "COMMAND_RESULT".equalsIgnoreCase(messageType);
        
        // 支持指令相关的路由键模式
        // 单独指令和批量指令
        boolean routingSupported = routingKey.startsWith("stomp.command.") ||
                                 routingKey.startsWith("stomp.batch.progress.");
        
        return typeSupported || routingSupported;
    }
    
    @Override
    public int getPriority() {
        return 10; // 高优先级，因为指令结果需要及时反馈
    }

    /**
     * @param messagePayload MQ消息载荷（JSON字符串）
     * @param routingKey MQ路由键
     * @return
     */
    @Override
    public BusinessMessageProcessResult process(String messagePayload, String routingKey) {
        try {
            log.debug("开始处理指令消息 - 路由键: {}", routingKey);
            
            // 解析消息内容
            Map<String, Object> messageData = JsonUtils.fromJson(messagePayload, Map.class);
            
            // 根据路由键确定具体的处理策略
            if (routingKey.startsWith("stomp.command.result.")) {
                return processCommandResult(messageData, routingKey);
            }
            else if (routingKey.startsWith("stomp.batch.progress.")) {

                // todo: 待优化
                return processBatchCommandProgress(messageData, routingKey);
            }
            else {
                // 暂时不兜底，直接抛出异常
                throw new BaseException(ExceptionEnum.UNKNOWN_ROUTING_KEY, "invalid routing key - " + routingKey);
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
                                                            String routingKey) {
        try {
            log.debug("处理单个指令执行结果 - 路由键: {}", routingKey);
            
            // 提取消息字段
            String commandId = (String) messageData.get("commandId");
            Long tid = getLongValue(messageData, "terminalId");
            Long orgId = getLongValue(messageData, "orgId");
            Long userId = getLongValue(messageData, "userId");
            String status = (String) messageData.get("status");
            Map<String, Object> command = (Map<String, Object>) messageData.get("originalCommand");
            
            // 验证必要字段
            if (commandId == null || tid == null || userId == null) {
                String errorMsg = "指令结果消息缺少必要字段: commandId, deviceId, userId";
                log.warn("⚠️ {}", errorMsg);
                return BusinessMessageProcessResult.failure(null, errorMsg);
            }
            
            // 构建STOMP消息
            CommonStompMessage stompMessage = buildCommandResultMessage(
                    commandId, tid, orgId, userId, status, command);
            
            // 分发消息
            DispatchResult dispatchResult = stompDispatcher.smartDispatch(stompMessage);
            
            log.info("✅ 指令结果消息处理完成 - 指令: {}, 设备: {}, 用户: {}, 结果: {}", 
                    commandId, tid, userId, status);
            
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
                                                                   String routingKey) {
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


    // ==================== 辅助方法 ====================
    
    /**
     * 构建指令执行结果STOMP消息
     */
    private CommonStompMessage buildCommandResultMessage(String commandId, Long tid,
                                                        Long orgId, Long userId, String status, Map<String, Object> command) {
        return CommonStompMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .timestamp(Instant.now().toString())
                .oid(orgId)
                .messageType(StompMessageTypes.COMMAND_FEEDBACK)
                .subType_1("SINGLE")
                .subType_2(status)
                .payload(command)
                .context(CommonStompMessage.Context.commandContext(userId, tid, commandId))
                .priority(Priority.HIGH)
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
//        return CommonStompMessage.builder()
//                .messageId(UUID.randomUUID().toString())
//                .timestamp(LocalDateTime.now())
//                .messageType(StompMessageTypes.TASK_PROGRESS)
//                .subType_1("BATCH_COMMAND")
//                .subType_2(status != null ? status : "PROGRESS")
//                .source(CommonStompMessage.Source.builder()
//                        .serviceId("core-service")
//                        .resourceType("BATCH_COMMAND")
//                        .resourceId(batchId)
//                        .taskId(taskId != null ? taskId : batchId)
//                        .batchContext("COMMAND_BATCH")
//                        .build())
//                .target(CommonStompMessage.Target.builder()
//                        .targetType("USER_AND_TOPIC")
//                        .uids(List.of(userId))
//                        .oid(orgId)
//                        .destination(StompTopic.buildBatchAggTopic(batchId) + "," +
//                                  StompTopic.USER_MESSAGES_QUEUE)
//                        .build())
//                .payload(Map.of(
//                        "batchId", batchId,
//                        "taskId", taskId != null ? taskId : batchId,
//                        "progress", progress != null ? progress : "",
//                        "status", status != null ? status : "",
//                        "totalCount", totalCount != null ? totalCount : 0,
//                        "completedCount", completedCount != null ? completedCount : 0,
//                        "successCount", successCount != null ? successCount : 0,
//                        "failureCount", failureCount != null ? failureCount : 0,
//                        "progressData", progressData != null ? progressData : Map.of(),
//                        "timestamp", LocalDateTime.now()
//                ))
//                .message(String.format("批量任务%s进度更新: %s/%s", batchId, completedCount, totalCount))
//                .metadata(CommonStompMessage.Metadata.builder()
//                        .priority(Priority.NORMAL)
//                        .persistent(false)
//                        .ttl(180000L)  // 3分钟TTL
//                        .sequenceId(System.currentTimeMillis())
//                        .build())
//                .build();
        return null;
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