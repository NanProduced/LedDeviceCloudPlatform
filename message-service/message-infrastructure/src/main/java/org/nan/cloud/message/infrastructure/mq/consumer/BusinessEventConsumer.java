package org.nan.cloud.message.infrastructure.mq.consumer;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.api.dto.websocket.WebSocketMessage;
import org.nan.cloud.message.api.enums.MessageType;
import org.nan.cloud.message.api.enums.Priority;
import org.nan.cloud.message.api.event.MessageEvent;
import org.nan.cloud.message.infrastructure.websocket.manager.WebSocketConnectionManager;
import org.nan.cloud.message.utils.MessageUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 业务事件消费者
 * 
 * 处理所有业务相关的事件消息，包括任务处理、数据同步、业务流程等。
 * 负责将业务事件转换为WebSocket消息推送给相关用户，确保业务处理状态的及时通知。
 * 
 * 主要处理的业务事件类型：
 * 1. TASK_CREATED - 任务创建事件
 * 2. TASK_STARTED - 任务开始执行事件
 * 3. TASK_COMPLETED - 任务完成事件
 * 4. TASK_FAILED - 任务失败事件
 * 5. DATA_SYNC_COMPLETED - 数据同步完成事件
 * 6. BUSINESS_PROCESS_UPDATED - 业务流程更新事件
 * 7. PAYMENT_PROCESSED - 支付处理事件
 * 8. ORDER_STATUS_CHANGED - 订单状态变更事件
 * 
 * 处理策略：
 * - 关键业务事件（支付、订单）立即推送
 * - 任务状态变更按优先级推送
 * - 数据同步事件批量推送，避免频繁通知
 * - 支持业务事件的追踪和审计
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BusinessEventConsumer {
    
    private final WebSocketConnectionManager webSocketConnectionManager;
    
    /**
     * 处理业务事件队列
     * 
     * 消费business.event.queue中的所有业务事件，根据事件类型和业务重要性
     * 决定推送策略和目标用户。
     * 
     * @param event 业务事件
     * @param message RabbitMQ原始消息
     * @param channel RabbitMQ通道
     */
    @RabbitListener(
        queues = "business.event.queue",
        ackMode = "MANUAL",
        concurrency = "3-8"
    )
    public void handleBusinessEvent(MessageEvent event, Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        
        try {
            log.info("处理业务事件: messageId={}, eventType={}, businessData={}", 
                    event.getMessageId(), event.getEventType(), event.getMetadata());
            
            // 根据业务事件类型进行不同处理
            switch (event.getEventType()) {
                case "TASK_CREATED":
                    handleTaskCreatedEvent(event);
                    break;
                case "TASK_STARTED":
                    handleTaskStartedEvent(event);
                    break;
                case "TASK_COMPLETED":
                    handleTaskCompletedEvent(event);
                    break;
                case "TASK_FAILED":
                    handleTaskFailedEvent(event);
                    break;
                case "DATA_SYNC_COMPLETED":
                    handleDataSyncCompletedEvent(event);
                    break;
                case "BUSINESS_PROCESS_UPDATED":
                    handleBusinessProcessUpdatedEvent(event);
                    break;
                case "PAYMENT_PROCESSED":
                    handlePaymentProcessedEvent(event);
                    break;
                case "ORDER_STATUS_CHANGED":
                    handleOrderStatusChangedEvent(event);
                    break;
                default:
                    log.warn("未知的业务事件类型: eventType={}, messageId={}", 
                            event.getEventType(), event.getMessageId());
                    handleUnknownBusinessEvent(event);
            }
            
            // 手动确认消息
            channel.basicAck(deliveryTag, false);
            log.info("业务事件处理完成: messageId={}, eventType={}", 
                    event.getMessageId(), event.getEventType());
                    
        } catch (Exception e) {
            log.error("处理业务事件失败: messageId={}, eventType={}, error={}", 
                     event.getMessageId(), event.getEventType(), e.getMessage(), e);
            
            handleBusinessEventProcessingError(event, channel, deliveryTag, e);
        }
    }
    
    /**
     * 处理任务创建事件
     * 
     * 当新任务被创建时触发，通知相关用户和负责人。
     * 
     * @param event 任务创建事件
     */
    private void handleTaskCreatedEvent(MessageEvent event) {
        try {
            Map<String, Object> taskInfo = event.getMetadata();
            String taskId = (String) taskInfo.get("taskId");
            String taskName = (String) taskInfo.get("taskName");
            String taskType = (String) taskInfo.get("taskType");
            String assignee = (String) taskInfo.get("assignee");
            String priority = (String) taskInfo.get("priority");
            
            log.info("处理任务创建事件: taskId={}, taskName={}, assignee={}, priority={}", 
                    taskId, taskName, assignee, priority);
            
            // 创建任务创建通知消息
            WebSocketMessage taskNotification = WebSocketMessage.builder()
                .messageId(MessageUtils.generateMessageId())
                .type(MessageType.TASK_NOTIFICATION)
                .title("新任务创建")
                .content(String.format("新任务 %s (%s) 已创建，任务类型：%s，优先级：%s。", 
                        taskName, taskId, taskType, priority))
                .priority(determinePriorityFromString(priority))
                .organizationId(event.getOrganizationId())
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                    "taskId", taskId,
                    "taskName", taskName,
                    "taskType", taskType,
                    "assignee", assignee != null ? assignee : "",
                    "priority", priority,
                    "eventType", "TASK_CREATED",
                    "createdTime", LocalDateTime.now().toString()
                ))
                .requireAck(false)
                .build();
            
            // 如果有指定负责人，优先通知负责人
            if (assignee != null && !assignee.isEmpty()) {
                webSocketConnectionManager.sendMessageToUser(assignee, taskNotification);
            }
            
            // 同时广播给组织内用户
            int sentCount = webSocketConnectionManager.broadcastToOrganization(
                event.getOrganizationId(), taskNotification);
            
            log.info("任务创建通知已发送: taskId={}, assignee={}, sentCount={}", 
                    taskId, assignee, sentCount);
            
        } catch (Exception e) {
            log.error("处理任务创建事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 处理任务开始事件
     * 
     * 当任务开始执行时触发，通知关注该任务的用户。
     * 
     * @param event 任务开始事件
     */
    private void handleTaskStartedEvent(MessageEvent event) {
        try {
            Map<String, Object> taskInfo = event.getMetadata();
            String taskId = (String) taskInfo.get("taskId");
            String taskName = (String) taskInfo.get("taskName");
            String executor = (String) taskInfo.get("executor");
            String estimatedDuration = (String) taskInfo.get("estimatedDuration");
            
            log.info("处理任务开始事件: taskId={}, executor={}, estimatedDuration={}", 
                    taskId, executor, estimatedDuration);
            
            // 创建任务开始通知消息
            WebSocketMessage startNotification = WebSocketMessage.builder()
                .messageId(MessageUtils.generateMessageId())
                .type(MessageType.TASK_NOTIFICATION)
                .title("任务开始执行")
                .content(String.format("任务 %s (%s) 已开始执行，执行人：%s。%s", 
                        taskName, taskId, executor,
                        estimatedDuration != null ? "预计耗时：" + estimatedDuration : ""))
                .priority(Priority.NORMAL)
                .organizationId(event.getOrganizationId())
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                    "taskId", taskId,
                    "taskName", taskName,
                    "executor", executor != null ? executor : "",
                    "estimatedDuration", estimatedDuration != null ? estimatedDuration : "",
                    "eventType", "TASK_STARTED",
                    "startTime", LocalDateTime.now().toString()
                ))
                .requireAck(false)
                .build();
            
            // 通知执行者
            if (executor != null && !executor.isEmpty()) {
                webSocketConnectionManager.sendMessageToUser(executor, startNotification);
            }
            
            // 推送给组织内用户
            int sentCount = webSocketConnectionManager.broadcastToOrganization(
                event.getOrganizationId(), startNotification);
            
            log.info("任务开始通知已发送: taskId={}, executor={}, sentCount={}", 
                    taskId, executor, sentCount);
            
        } catch (Exception e) {
            log.error("处理任务开始事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 处理任务完成事件
     * 
     * 当任务成功完成时触发，通知相关用户任务结果。
     * 
     * @param event 任务完成事件
     */
    private void handleTaskCompletedEvent(MessageEvent event) {
        try {
            Map<String, Object> taskInfo = event.getMetadata();
            String taskId = (String) taskInfo.get("taskId");
            String taskName = (String) taskInfo.get("taskName");
            String executor = (String) taskInfo.get("executor");
            String duration = (String) taskInfo.get("actualDuration");
            String result = (String) taskInfo.get("result");
            
            log.info("处理任务完成事件: taskId={}, executor={}, duration={}", 
                    taskId, executor, duration);
            
            // 创建任务完成通知消息
            WebSocketMessage completionNotification = WebSocketMessage.builder()
                .messageId(MessageUtils.generateMessageId())
                .type(MessageType.TASK_NOTIFICATION)
                .title("任务执行完成")
                .content(String.format("任务 %s (%s) 已成功完成，执行人：%s。%s%s", 
                        taskName, taskId, executor,
                        duration != null ? "实际耗时：" + duration + "。" : "",
                        result != null ? "执行结果：" + result : ""))
                .priority(Priority.NORMAL)
                .organizationId(event.getOrganizationId())
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                    "taskId", taskId,
                    "taskName", taskName,
                    "executor", executor != null ? executor : "",
                    "actualDuration", duration != null ? duration : "",
                    "result", result != null ? result : "",
                    "eventType", "TASK_COMPLETED",
                    "completedTime", LocalDateTime.now().toString()
                ))
                .requireAck(false)
                .build();
            
            // 通知执行者
            if (executor != null && !executor.isEmpty()) {
                webSocketConnectionManager.sendMessageToUser(executor, completionNotification);
            }
            
            // 推送给组织内用户
            int sentCount = webSocketConnectionManager.broadcastToOrganization(
                event.getOrganizationId(), completionNotification);
            
            log.info("任务完成通知已发送: taskId={}, executor={}, sentCount={}", 
                    taskId, executor, sentCount);
            
        } catch (Exception e) {
            log.error("处理任务完成事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 处理任务失败事件
     * 
     * 当任务执行失败时触发，及时通知相关人员处理。
     * 
     * @param event 任务失败事件
     */
    private void handleTaskFailedEvent(MessageEvent event) {
        try {
            Map<String, Object> taskInfo = event.getMetadata();
            String taskId = (String) taskInfo.get("taskId");
            String taskName = (String) taskInfo.get("taskName");
            String executor = (String) taskInfo.get("executor");
            String failureReason = (String) taskInfo.get("failureReason");
            String errorCode = (String) taskInfo.get("errorCode");
            
            log.error("处理任务失败事件: taskId={}, executor={}, reason={}", 
                     taskId, executor, failureReason);
            
            // 创建任务失败告警消息
            WebSocketMessage failureAlert = WebSocketMessage.builder()
                .messageId(MessageUtils.generateMessageId())
                .type(MessageType.TASK_NOTIFICATION)
                .title("任务执行失败")
                .content(String.format("任务 %s (%s) 执行失败，执行人：%s。失败原因：%s%s", 
                        taskName, taskId, executor, failureReason,
                        errorCode != null ? "，错误代码：" + errorCode : ""))
                .priority(Priority.HIGH) // 任务失败优先级较高
                .organizationId(event.getOrganizationId())
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                    "taskId", taskId,
                    "taskName", taskName,
                    "executor", executor != null ? executor : "",
                    "failureReason", failureReason != null ? failureReason : "",
                    "errorCode", errorCode != null ? errorCode : "",
                    "eventType", "TASK_FAILED",
                    "failedTime", LocalDateTime.now().toString()
                ))
                .requireAck(true) // 任务失败需要确认
                .build();
            
            // 通知执行者
            if (executor != null && !executor.isEmpty()) {
                webSocketConnectionManager.sendMessageToUser(executor, failureAlert);
            }
            
            // 推送给组织内用户
            int sentCount = webSocketConnectionManager.broadcastToOrganization(
                event.getOrganizationId(), failureAlert);
            
            log.error("任务失败告警已发送: taskId={}, executor={}, sentCount={}", 
                     taskId, executor, sentCount);
            
        } catch (Exception e) {
            log.error("处理任务失败事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 处理数据同步完成事件
     * 
     * 当数据同步操作完成时触发，通知相关用户同步结果。
     * 
     * @param event 数据同步完成事件
     */
    private void handleDataSyncCompletedEvent(MessageEvent event) {
        try {
            Map<String, Object> syncInfo = event.getMetadata();
            String syncId = (String) syncInfo.get("syncId");
            String syncType = (String) syncInfo.get("syncType");
            String sourceSystem = (String) syncInfo.get("sourceSystem");
            String targetSystem = (String) syncInfo.get("targetSystem");
            Integer recordCount = (Integer) syncInfo.get("recordCount");
            String status = (String) syncInfo.get("status");
            
            log.info("处理数据同步完成事件: syncId={}, type={}, {} -> {}, records={}, status={}", 
                    syncId, syncType, sourceSystem, targetSystem, recordCount, status);
            
            boolean isSuccess = "SUCCESS".equals(status);
            
            // 创建数据同步通知消息
            WebSocketMessage syncNotification = WebSocketMessage.builder()
                .messageId(MessageUtils.generateMessageId())
                .type(MessageType.BUSINESS_NOTIFICATION)
                .title("数据同步" + (isSuccess ? "完成" : "异常"))
                .content(String.format("数据同步任务 %s (%s) %s，同步类型：%s，从 %s 到 %s。%s", 
                        syncId, syncType, isSuccess ? "成功完成" : "执行异常",
                        syncType, sourceSystem, targetSystem,
                        recordCount != null ? "处理记录数：" + recordCount : ""))
                .priority(isSuccess ? Priority.LOW : Priority.HIGH)
                .organizationId(event.getOrganizationId())
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                    "syncId", syncId,
                    "syncType", syncType,
                    "sourceSystem", sourceSystem != null ? sourceSystem : "",
                    "targetSystem", targetSystem != null ? targetSystem : "",
                    "recordCount", recordCount != null ? recordCount.toString() : "0",
                    "status", status,
                    "eventType", "DATA_SYNC_COMPLETED",
                    "syncTime", LocalDateTime.now().toString()
                ))
                .requireAck(!isSuccess) // 同步失败需要确认
                .build();
            
            // 推送给组织内用户
            int sentCount = webSocketConnectionManager.broadcastToOrganization(
                event.getOrganizationId(), syncNotification);
            
            log.info("数据同步通知已发送: syncId={}, status={}, sentCount={}", 
                    syncId, status, sentCount);
            
        } catch (Exception e) {
            log.error("处理数据同步完成事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 处理业务流程更新事件
     * 
     * 当业务流程状态更新时触发，通知相关用户。
     * 
     * @param event 业务流程更新事件
     */
    private void handleBusinessProcessUpdatedEvent(MessageEvent event) {
        try {
            Map<String, Object> processInfo = event.getMetadata();
            String processId = (String) processInfo.get("processId");
            String processName = (String) processInfo.get("processName");
            String processType = (String) processInfo.get("processType");
            String oldStatus = (String) processInfo.get("oldStatus");
            String newStatus = (String) processInfo.get("newStatus");
            String updateReason = (String) processInfo.get("updateReason");
            
            log.info("处理业务流程更新事件: processId={}, {} -> {}, reason={}", 
                    processId, oldStatus, newStatus, updateReason);
            
            // 创建业务流程更新通知消息
            WebSocketMessage processNotification = WebSocketMessage.builder()
                .messageId(MessageUtils.generateMessageId())
                .type(MessageType.BUSINESS_NOTIFICATION)
                .title("业务流程状态更新")
                .content(String.format("业务流程 %s (%s) 状态从 %s 更新为 %s。%s", 
                        processName, processId, oldStatus, newStatus,
                        updateReason != null ? "更新原因：" + updateReason : ""))
                .priority(Priority.NORMAL)
                .organizationId(event.getOrganizationId())
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                    "processId", processId,
                    "processName", processName != null ? processName : "",
                    "processType", processType != null ? processType : "",
                    "oldStatus", oldStatus != null ? oldStatus : "",
                    "newStatus", newStatus != null ? newStatus : "",
                    "updateReason", updateReason != null ? updateReason : "",
                    "eventType", "BUSINESS_PROCESS_UPDATED",
                    "updateTime", LocalDateTime.now().toString()
                ))
                .requireAck(false)
                .build();
            
            // 推送给组织内用户
            int sentCount = webSocketConnectionManager.broadcastToOrganization(
                event.getOrganizationId(), processNotification);
            
            log.info("业务流程更新通知已发送: processId={}, status={}, sentCount={}", 
                    processId, newStatus, sentCount);
            
        } catch (Exception e) {
            log.error("处理业务流程更新事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 处理支付处理事件
     * 
     * 当支付操作完成时触发，这是关键业务事件，需要立即通知。
     * 
     * @param event 支付处理事件
     */
    private void handlePaymentProcessedEvent(MessageEvent event) {
        try {
            Map<String, Object> paymentInfo = event.getMetadata();
            String paymentId = (String) paymentInfo.get("paymentId");
            String orderId = (String) paymentInfo.get("orderId");
            String userId = (String) paymentInfo.get("userId");
            String amount = (String) paymentInfo.get("amount");
            String status = (String) paymentInfo.get("status");
            String paymentMethod = (String) paymentInfo.get("paymentMethod");
            
            log.info("处理支付处理事件: paymentId={}, orderId={}, userId={}, amount={}, status={}", 
                    paymentId, orderId, userId, amount, status);
            
            boolean isSuccess = "SUCCESS".equals(status);
            
            // 创建支付处理通知消息
            WebSocketMessage paymentNotification = WebSocketMessage.builder()
                .messageId(MessageUtils.generateMessageId())
                .type(MessageType.BUSINESS_NOTIFICATION)
                .title("支付" + (isSuccess ? "成功" : "失败"))
                .content(String.format("订单 %s 的支付 %s，支付金额：%s，支付方式：%s。支付ID：%s", 
                        orderId, isSuccess ? "已成功处理" : "处理失败",
                        amount, paymentMethod, paymentId))
                .priority(Priority.HIGH) // 支付事件高优先级
                .organizationId(event.getOrganizationId())
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                    "paymentId", paymentId,
                    "orderId", orderId != null ? orderId : "",
                    "userId", userId != null ? userId : "",
                    "amount", amount != null ? amount : "",
                    "status", status,
                    "paymentMethod", paymentMethod != null ? paymentMethod : "",
                    "eventType", "PAYMENT_PROCESSED",
                    "processedTime", LocalDateTime.now().toString()
                ))
                .requireAck(true) // 支付事件需要确认
                .build();
            
            // 优先通知相关用户
            if (userId != null && !userId.isEmpty()) {
                webSocketConnectionManager.sendMessageToUser(userId, paymentNotification);
            }
            
            // 推送给组织内用户
            int sentCount = webSocketConnectionManager.broadcastToOrganization(
                event.getOrganizationId(), paymentNotification);
            
            log.info("支付处理通知已发送: paymentId={}, status={}, sentCount={}", 
                    paymentId, status, sentCount);
            
        } catch (Exception e) {
            log.error("处理支付处理事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 处理订单状态变更事件
     * 
     * 当订单状态发生变更时触发，通知相关用户。
     * 
     * @param event 订单状态变更事件
     */
    private void handleOrderStatusChangedEvent(MessageEvent event) {
        try {
            Map<String, Object> orderInfo = event.getMetadata();
            String orderId = (String) orderInfo.get("orderId");
            String userId = (String) orderInfo.get("userId");
            String oldStatus = (String) orderInfo.get("oldStatus");
            String newStatus = (String) orderInfo.get("newStatus");
            String changeReason = (String) orderInfo.get("changeReason");
            
            log.info("处理订单状态变更事件: orderId={}, userId={}, {} -> {}, reason={}", 
                    orderId, userId, oldStatus, newStatus, changeReason);
            
            // 创建订单状态变更通知消息
            WebSocketMessage orderNotification = WebSocketMessage.builder()
                .messageId(MessageUtils.generateMessageId())
                .type(MessageType.BUSINESS_NOTIFICATION)
                .title("订单状态更新")
                .content(String.format("订单 %s 状态从 %s 更新为 %s。%s", 
                        orderId, oldStatus, newStatus,
                        changeReason != null ? "变更原因：" + changeReason : ""))
                .priority(Priority.NORMAL)
                .organizationId(event.getOrganizationId())
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                    "orderId", orderId,
                    "userId", userId != null ? userId : "",
                    "oldStatus", oldStatus != null ? oldStatus : "",
                    "newStatus", newStatus != null ? newStatus : "",
                    "changeReason", changeReason != null ? changeReason : "",
                    "eventType", "ORDER_STATUS_CHANGED",
                    "changedTime", LocalDateTime.now().toString()
                ))
                .requireAck(false)
                .build();
            
            // 优先通知订单相关用户
            if (userId != null && !userId.isEmpty()) {
                webSocketConnectionManager.sendMessageToUser(userId, orderNotification);
            }
            
            // 推送给组织内用户
            int sentCount = webSocketConnectionManager.broadcastToOrganization(
                event.getOrganizationId(), orderNotification);
            
            log.info("订单状态变更通知已发送: orderId={}, status={}, sentCount={}", 
                    orderId, newStatus, sentCount);
            
        } catch (Exception e) {
            log.error("处理订单状态变更事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 处理未知业务事件
     * 
     * 对于未识别的业务事件类型，进行通用处理。
     * 
     * @param event 未知业务事件
     */
    private void handleUnknownBusinessEvent(MessageEvent event) {
        try {
            log.warn("处理未知业务事件: eventType={}, messageId={}", 
                    event.getEventType(), event.getMessageId());
            
            // 创建通用业务事件通知
            WebSocketMessage unknownEventMessage = WebSocketMessage.builder()
                .messageId(MessageUtils.generateMessageId())
                .type(MessageType.BUSINESS_NOTIFICATION)
                .title("业务事件通知")
                .content(String.format("收到业务事件：%s。详情：%s", 
                        event.getEventType(), 
                        event.getContent() != null ? event.getContent() : "无详细信息"))
                .priority(Priority.LOW)
                .organizationId(event.getOrganizationId())
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                    "originalEventType", event.getEventType(),
                    "originalContent", event.getContent() != null ? event.getContent() : "",
                    "metadata", event.getMetadata() != null ? event.getMetadata().toString() : "",
                    "eventType", "UNKNOWN_BUSINESS_EVENT"
                ))
                .requireAck(false)
                .build();
            
            // 推送给组织内用户
            int sentCount = webSocketConnectionManager.broadcastToOrganization(
                event.getOrganizationId(), unknownEventMessage);
            
            log.info("未知业务事件通知已发送: eventType={}, sentCount={}", 
                    event.getEventType(), sentCount);
            
        } catch (Exception e) {
            log.error("处理未知业务事件异常: messageId={}, error={}", event.getMessageId(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 处理业务事件处理错误
     * 
     * @param event 失败的事件
     * @param channel RabbitMQ通道
     * @param deliveryTag 消息标签
     * @param exception 异常信息
     */
    private void handleBusinessEventProcessingError(MessageEvent event, Channel channel, 
                                                   long deliveryTag, Exception exception) {
        try {
            // 增加重试次数
            event.incrementRetry();
            event.setErrorMessage(exception.getMessage());
            event.setProcessedTime(LocalDateTime.now());
            
            if (event.canRetry()) {
                log.warn("业务事件处理失败，消息重新入队: messageId={}, retryCount={}, error={}", 
                        event.getMessageId(), event.getRetryCount(), exception.getMessage());
                // 拒绝消息，重新入队
                channel.basicNack(deliveryTag, false, true);
            } else {
                log.error("业务事件处理失败，超过最大重试次数，消息进入死信队列: messageId={}, retryCount={}, error={}", 
                        event.getMessageId(), event.getRetryCount(), exception.getMessage());
                // 拒绝消息，不重新入队，进入死信队列
                channel.basicNack(deliveryTag, false, false);
            }
            
        } catch (IOException e) {
            log.error("处理业务事件错误失败: messageId={}, error={}", event.getMessageId(), e.getMessage());
        }
    }
    
    /**
     * 根据字符串确定消息优先级
     * 
     * @param priorityStr 优先级字符串
     * @return 消息优先级
     */
    private Priority determinePriorityFromString(String priorityStr) {
        if (priorityStr == null) {
            return Priority.NORMAL;
        }
        
        switch (priorityStr.toUpperCase()) {
            case "URGENT":
                return Priority.URGENT;
            case "HIGH":
                return Priority.HIGH;
            case "LOW":
                return Priority.LOW;
            case "MEDIUM":
            default:
                return Priority.NORMAL;
        }
    }
}