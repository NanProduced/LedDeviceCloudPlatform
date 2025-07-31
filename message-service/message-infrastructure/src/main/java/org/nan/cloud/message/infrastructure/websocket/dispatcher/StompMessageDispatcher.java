package org.nan.cloud.message.infrastructure.websocket.dispatcher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.infrastructure.websocket.routing.enhanced.DynamicRoutingEngine;
import org.nan.cloud.message.infrastructure.websocket.routing.enhanced.MessageAggregator;
import org.nan.cloud.message.infrastructure.websocket.routing.enhanced.RoutingStrategyManager;
import org.nan.cloud.message.infrastructure.websocket.manager.StompConnectionManager;
import org.nan.cloud.message.infrastructure.websocket.routing.TopicRoutingDecision;
import org.nan.cloud.message.infrastructure.websocket.routing.TopicRoutingManager;
import org.nan.cloud.message.infrastructure.websocket.sender.StompMessageSender;
import org.nan.cloud.message.infrastructure.websocket.stomp.enums.StompMessageTypes;
import org.nan.cloud.message.infrastructure.websocket.stomp.enums.StompTopic;
import org.nan.cloud.message.infrastructure.websocket.stomp.model.CommonStompMessage;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * STOMP消息分发器
 * 
 * 核心职责：
 * 1. 接收来自业务服务的各类消息
 * 2. 转换为统一的CommonStompMessage格式
 * 3. 根据目标类型和路由规则进行消息分发
 * 4. 与StompConnectionManager协作完成实际推送
 * 5. 支持单用户、多用户、组织、终端等多种分发模式
 * 
 * Phase 2.4 增强功能：
 * - 集成消息聚合器：智能聚合相似消息
 * - 集成动态路由引擎：基于业务规则的智能路由
 * - 集成路由策略管理器：支持多种路由策略和故障转移
 * - 增强版Topic模式：支持通配符、动态创建、层次化管理
 * 
 * 分发策略：
 * - 用户级别：直接推送到用户的个人队列和订阅主题
 * - 组织级别：广播到组织内所有在线用户
 * - 终端级别：推送到终端相关的所有订阅者
 * - 批量指令：聚合推送到任务相关的所有用户
 * - 系统级别：全局广播到所有在线用户
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompMessageDispatcher {

    private final StompConnectionManager stompConnectionManager;
    private final TopicRoutingManager topicRoutingManager;
    private final StompMessageSender messageSender;
    
    // Phase 2.4: 增强版Topic模式组件
    private final MessageAggregator messageAggregator;
    private final DynamicRoutingEngine dynamicRoutingEngine;
    private final RoutingStrategyManager routingStrategyManager;
    
    // ==================== 智能路由分发 ====================
    
    /**
     * 智能分发消息 (Phase 2.4增强版)
     * 集成消息聚合、动态路由引擎和增强Topic管理
     * 
     * @param message STOMP消息
     * @return 分发结果统计
     */
    public DispatchResult smartDispatch(CommonStompMessage message) {
        try {
            log.debug("开始智能分发消息 (Phase 2.4) - 消息类型: {}, 消息ID: {}", message.getMessageType(), message.getMessageId());
            
            // 设置消息基础信息
            enrichMessage(message);
            
            // Phase 2.4: Step 1 - 消息聚合处理
            MessageAggregator.AggregationResult aggregationResult = messageAggregator.aggregateMessage(message);
            
            // 如果消息被加入聚合队列，则等待聚合触发
            if (aggregationResult.getType() == MessageAggregator.AggregationResultType.QUEUED) {
                log.debug("消息已加入聚合队列 - 消息ID: {}", message.getMessageId());
                return DispatchResult.queued(message.getMessageId());
            }
            
            // 获取要分发的消息（可能是聚合后的消息）
            CommonStompMessage messageToDispatch = aggregationResult.hasMessage() ? 
                    aggregationResult.getAggregatedMessage() : message;
            
            // Phase 2.4: Step 2 - 选择路由策略
            RoutingStrategyManager.RoutingStrategy strategy = routingStrategyManager.selectStrategy(messageToDispatch);
            
            // Phase 2.4: Step 3 - 动态路由决策
            DynamicRoutingEngine.RoutingDecision routingDecision = dynamicRoutingEngine.makeRoutingDecision(messageToDispatch);
            
            DispatchResult result = new DispatchResult(messageToDispatch.getMessageId());
            
            // 如果动态路由决策失败，回退到传统路由
            if (!routingDecision.isSuccess()) {
                log.warn("动态路由决策失败，回退到传统路由 - 消息ID: {}, 原因: {}", 
                        messageToDispatch.getMessageId(), routingDecision.getFailureReason());
                
                TopicRoutingDecision fallbackDecision = topicRoutingManager.decideRouting(messageToDispatch);
                return executeFallbackRouting(messageToDispatch, fallbackDecision, result);
            }
            
            // Phase 2.4: Step 4 - 执行增强路由分发
            for (String topicPath : routingDecision.getTargets()) {
                try {
                    sendToTopic(topicPath, messageToDispatch);
                    result.incrementSuccessCount();
                    result.addSuccessfulTopic(topicPath);
                } catch (Exception e) {
                    log.error("向主题分发消息失败 - 主题: {}, 消息ID: {}, 错误: {}", 
                            topicPath, messageToDispatch.getMessageId(), e.getMessage(), e);
                    result.incrementFailureCount();
                    result.addFailedTopic(topicPath, e.getMessage());
                }
            }
            
            // 记录聚合信息
            if (aggregationResult.getType() == MessageAggregator.AggregationResultType.AGGREGATED) {
                result.setAggregatedMessageCount(aggregationResult.getOriginalMessages().size());
                log.info("✅ 增强智能分发完成 (含聚合) - 消息ID: {}, 路由策略: {}, 聚合消息数: {}, 成功: {}, 失败: {}", 
                        messageToDispatch.getMessageId(), routingDecision.getStrategy(), 
                        aggregationResult.getOriginalMessages().size(), result.getSuccessCount(), result.getFailureCount());
            } else {
                log.info("✅ 增强智能分发完成 - 消息ID: {}, 路由策略: {}, 成功: {}, 失败: {}", 
                        messageToDispatch.getMessageId(), routingDecision.getStrategy(), 
                        result.getSuccessCount(), result.getFailureCount());
            }
                    
            return result;
            
        } catch (Exception e) {
            log.error("增强智能分发消息失败 - 消息ID: {}, 错误: {}", message.getMessageId(), e.getMessage(), e);
            return DispatchResult.failure(message.getMessageId(), e.getMessage());
        }
    }
    
    // ==================== 用户级别消息分发 ====================
    
    /**
     * 发送消息给单个用户
     * 
     * @param userId 目标用户ID
     * @param message STOMP消息对象
     */
    public void sendToUser(String userId, CommonStompMessage message) {
        try {
            log.debug("开始向用户发送消息 - 用户ID: {}, 消息类型: {}", userId, message.getMessageType());
            
            // 设置消息基础信息
            enrichMessage(message);
            
            // 检查用户是否在线
            if (!stompConnectionManager.isUserOnline(userId)) {
                log.warn("用户不在线，跳过STOMP消息推送 - 用户ID: {}", userId);
                // TODO: 可以考虑将消息存储到离线消息队列
                return;
            }
            
            // 使用StompConnectionManager进行推送
            boolean sent = stompConnectionManager.sendToUser(userId, message);
            
            if (sent) {
                log.info("✅ 用户消息发送成功 - 用户ID: {}, 消息ID: {}", userId, message.getMessageId());
            } else {
                log.warn("⚠️ 用户消息发送失败 - 用户ID: {}, 消息ID: {}", userId, message.getMessageId());
            }
            
        } catch (Exception e) {
            log.error("发送用户消息时发生异常 - 用户ID: {}, 消息ID: {}, 错误: {}", 
                    userId, message.getMessageId(), e.getMessage(), e);
        }
    }
    
    /**
     * 发送消息给多个用户
     * 
     * @param userIds 目标用户ID列表
     * @param message STOMP消息对象
     */
    public void sendToUsers(List<String> userIds, CommonStompMessage message) {
        if (userIds == null || userIds.isEmpty()) {
            log.warn("用户ID列表为空，跳过消息发送");
            return;
        }
        
        try {
            log.info("开始向多个用户发送消息 - 用户数量: {}, 消息类型: {}", userIds.size(), message.getMessageType());
            
            // 设置消息基础信息
            enrichMessage(message);
            
            int successCount = 0;
            int onlineCount = 0;
            
            for (String userId : userIds) {
                if (stompConnectionManager.isUserOnline(userId)) {
                    onlineCount++;
                    boolean sent = stompConnectionManager.sendToUser(userId, message);
                    if (sent) {
                        successCount++;
                    }
                }
            }
            
            log.info("✅ 批量用户消息发送完成 - 总用户: {}, 在线用户: {}, 成功发送: {}, 消息ID: {}", 
                    userIds.size(), onlineCount, successCount, message.getMessageId());
                    
        } catch (Exception e) {
            log.error("批量发送用户消息时发生异常 - 用户数量: {}, 消息ID: {}, 错误: {}", 
                    userIds.size(), message.getMessageId(), e.getMessage(), e);
        }
    }
    
    /**
     * 发送用户通知消息
     * <p>通知消息为用于前端在浏览器弹窗显示的业务消息，如操作反馈等</p>
     * 
     * @param userId 用户ID
     * @param title 通知标题
     * @param content 通知内容
     * @param notificationType 通知类型
     */
    public void sendUserNotification(String userId, String title, String content, String notificationType) {
        try {
            CommonStompMessage message = CommonStompMessage.builder()
                    .messageType(StompMessageTypes.NOTIFICATION)
                    .subType_1("USER")
                    .subType_2(notificationType)
                    .target(CommonStompMessage.Target.builder()
                            .targetType("USER")
                            .uids(List.of(Long.valueOf(userId)))
                            .topicPath(StompTopic.buildUserNotificationTopic(userId))
                            .build())
                    .payload(Map.of(
                            "title", title,
                            "content", content,
                            "type", notificationType,
                            "timestamp", LocalDateTime.now()
                    ))
                    .build();
            
            sendToUser(userId, message);
            log.info("✅ 用户通知发送完成 - 用户ID: {}, 类型: {}, 标题: {}", userId, notificationType, title);
            
        } catch (Exception e) {
            log.error("发送用户通知失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
        }
    }
    
    // ==================== 组织级别消息分发 ====================
    
    /**
     * 向组织内所有在线用户广播消息
     * 
     * @param organizationId 组织ID
     * @param message STOMP消息对象
     */
    public void broadcastToOrganization(Long organizationId, CommonStompMessage message) {
        try {
            log.info("开始向组织广播消息 - 组织ID: {}, 消息类型: {}", organizationId, message.getMessageType());
            
            // 设置消息基础信息
            enrichMessage(message);
            
            // 使用StompConnectionManager进行组织广播
            int sentCount = stompConnectionManager.broadcastToOrganization(organizationId, message);
            
            log.info("✅ 组织广播消息完成 - 组织ID: {}, 发送用户数: {}, 消息ID: {}", 
                    organizationId, sentCount, message.getMessageId());
                    
        } catch (Exception e) {
            log.error("组织广播消息时发生异常 - 组织ID: {}, 消息ID: {}, 错误: {}", 
                    organizationId, message.getMessageId(), e.getMessage(), e);
        }
    }
    
    /**
     * 发送组织公告
     * 
     * @param organizationId 组织ID
     * @param title 公告标题
     * @param content 公告内容
     * @param priority 优先级
     */
    public void sendOrganizationAnnouncement(Long organizationId, String title, String content, String priority) {
        try {
            CommonStompMessage message = CommonStompMessage.builder()
                    .messageType(StompMessageTypes.NOTIFICATION)
                    .subType_1("ORG")
                    .subType_2(priority)
                    .target(CommonStompMessage.Target.builder()
                            .targetType("ORGANIZATION")
                            .oid(organizationId)
                            .topicPath(StompTopic.buildOrgAnnouncementTopic(organizationId.toString()))
                            .build())
                    .payload(Map.of(
                            "title", title,
                            "content", content,
                            "priority", priority,
                            "publishTime", LocalDateTime.now()
                    ))
                    .metadata(CommonStompMessage.Metadata.builder()
                            .priority(org.nan.cloud.message.api.enums.Priority.valueOf(priority.toUpperCase()))
                            .persistent(true)
                            .requireAck(true)
                            .build())
                    .build();
            
            broadcastToOrganization(organizationId, message);
            log.info("✅ 组织公告发送完成 - 组织ID: {}, 优先级: {}, 标题: {}", organizationId, priority, title);
            
        } catch (Exception e) {
            log.error("发送组织公告失败 - 组织ID: {}, 错误: {}", organizationId, e.getMessage(), e);
        }
    }
    
    // ==================== 终端级别消息分发 ====================
    
    /**
     * 发送终端状态消息到相关订阅者
     * 
     * @param terminalId 终端ID
     * @param type 终端状态数据类型
     * @param additionalData 附加数据
     */
    public void sendTerminalStatus(String terminalId, String type, Map<String, Object> additionalData) {
        try {
            log.debug("发送终端状态消息 - 终端ID: {}, 类型: {}", terminalId, type);
            
            CommonStompMessage message = CommonStompMessage.builder()
                    .messageType(StompMessageTypes.TERMINAL_STATUS_CHANGE)
                    .subType_1(type)
                    .source(CommonStompMessage.Source.builder()
                            .serviceId("terminal-service")
                            .resourceType("TERMINAL")
                            .resourceId(terminalId)
                            .build())
                    .target(CommonStompMessage.Target.builder()
                            .targetType("TOPIC")
                            .topicPath(StompTopic.buildTerminalStatusTopic(terminalId))
                            .build())
                    .payload(Map.of(
                            "terminalId", terminalId,
                            "type", type,
                            "timestamp", LocalDateTime.now(),
                            "data", additionalData != null ? additionalData : Map.of()
                    ))
                    .build();
            
            // 向终端状态主题发布消息
            sendToTopic(message.getTarget().getTopicPath(), message);
            log.info("✅ 终端状态消息发送完成 - 终端ID: {}, 类型: {}", terminalId, type);
            
        } catch (Exception e) {
            log.error("发送终端状态消息失败 - 终端ID: {}, 类型: {}, 错误: {}", terminalId, type, e.getMessage(), e);
        }
    }
    
    /**
     * 发送终端指令执行结果
     * 
     * @param terminalId 终端ID
     * @param commandId 指令ID
     * @param result 执行结果
     * @param executorUserId 执行用户ID
     */
    public void sendTerminalCommandResult(String terminalId, String commandId, Map<String, Object> result, String executorUserId) {
        try {
            log.debug("发送终端指令结果 - 终端ID: {}, 指令ID: {}, 执行用户: {}", terminalId, commandId, executorUserId);
            
            CommonStompMessage message = CommonStompMessage.builder()
                    .messageType(StompMessageTypes.COMMAND_FEEDBACK)
                    .subType_1("SINGLE")
                    .subType_2(commandId)
                    .source(CommonStompMessage.Source.builder()
                            .serviceId("terminal-service")
                            .resourceType("TERMINAL")
                            .resourceId(terminalId)
                            .executionId(commandId)
                            .build())
                    .target(CommonStompMessage.Target.builder()
                            .targetType("USER")
                            .uids(List.of(Long.valueOf(executorUserId)))
                            .topicPath(StompTopic.buildUserCommandFeedbackTopic(executorUserId))
                            .build())
                    .payload(Map.of(
                            "terminalId", terminalId,
                            "commandId", commandId,
                            "result", result,
                            "executorUserId", executorUserId,
                            "timestamp", LocalDateTime.now()
                    ))
                    .metadata(CommonStompMessage.Metadata.builder()
                            .requireAck(true)
                            .correlationId(commandId)
                            .build())
                    .build();
            
            // 向执行用户发送指令反馈
            sendToUser(executorUserId, message);
            
            // 同时向终端指令主题发布消息供其他订阅者 - 暂时不需要
            // String terminalCommandTopic = StompTopic.TERMINAL_COMMAND_TOPIC_TEMPLATE.replace("{terminalId}", terminalId);
            // sendToTopic(terminalCommandTopic, message);
            
            log.info("✅ 终端指令结果发送完成 - 终端ID: {}, 指令ID: {}, 执行用户: {}", terminalId, commandId, executorUserId);
            
        } catch (Exception e) {
            log.error("发送终端指令结果失败 - 终端ID: {}, 指令ID: {}, 错误: {}", terminalId, commandId, e.getMessage(), e);
        }
    }
    
    // ==================== 批量指令消息分发 ====================
    
    /**
     * 发送批量指令进度更新
     * 
     * @param taskId 任务ID
     * @param progress 进度信息
     * @param targetUserIds 目标用户ID列表
     */
    public void sendBatchCommandProgress(String taskId, Map<String, Object> progress, List<String> targetUserIds) {
        try {
            log.debug("发送批量指令进度 - 任务ID: {}, 目标用户数: {}", taskId, targetUserIds != null ? targetUserIds.size() : 0);
            
            CommonStompMessage message = CommonStompMessage.builder()
                    .messageType(StompMessageTypes.COMMAND_FEEDBACK)
                    .subType_1("BATCH")
                    .subType_2(taskId)
                    .source(CommonStompMessage.Source.builder()
                            .serviceId("core-service")
                            .resourceType("BATCH_COMMAND")
                            .taskId(taskId)
                            .build())
                    .target(CommonStompMessage.Target.builder()
                            .targetType("BATCH_USERS")
                            .topicPath(StompTopic.buildBatchCommandSummaryTopic(taskId))
                            .build())
                    .payload(Map.of(
                            "taskId", taskId,
                            "progress", progress,
                            "timestamp", LocalDateTime.now()
                    ))
                    .metadata(CommonStompMessage.Metadata.builder()
                            .sequenceId(System.currentTimeMillis())
                            .persistent(false)
                            .build())
                    .build();
            
            // 向批量指令主题发布进度消息
            sendToTopic(message.getTarget().getTopicPath(), message);
            
            // 同时向相关用户发送个人进度通知
            if (targetUserIds != null && !targetUserIds.isEmpty()) {
                sendToUsers(targetUserIds, message);
            }
            
            log.info("✅ 批量指令进度发送完成 - 任务ID: {}, 目标用户数: {}", taskId, targetUserIds != null ? targetUserIds.size() : 0);
            
        } catch (Exception e) {
            log.error("发送批量指令进度失败 - 任务ID: {}, 错误: {}", taskId, e.getMessage(), e);
        }
    }
    
    // ==================== 主题级别消息分发 ====================
    
    /**
     * 向指定主题发布消息
     * 
     * @param topicPath 主题路径
     * @param message STOMP消息对象
     */
    public void sendToTopic(String topicPath, CommonStompMessage message) {
        try {
            log.debug("向主题发布消息 - 主题: {}, 消息类型: {}", topicPath, message.getMessageType());
            
            // 设置消息基础信息
            enrichMessage(message);
            
            // 使用StompMessageSender发送到主题
            messageSender.sendToTopic(topicPath, message);
            
            log.debug("✅ 主题消息发布完成 - 主题: {}, 消息ID: {}", topicPath, message.getMessageId());
            
        } catch (Exception e) {
            log.error("向主题发布消息失败 - 主题: {}, 消息ID: {}, 错误: {}", topicPath, message.getMessageId(), e.getMessage(), e);
        }
    }
    
    /**
     * 全局系统广播
     * 
     * @param message STOMP消息对象
     */
    public void globalBroadcast(CommonStompMessage message) {
        try {
            log.info("开始全局系统广播 - 消息类型: {}", message.getMessageType());
            
            // 设置消息基础信息
            enrichMessage(message);
            
            // 向全局系统主题广播
            sendToTopic(StompTopic.GLOBAL_SYSTEM_ANNOUNCEMENT_TOPIC, message);
            
            // 同时向所有在线用户发送
            int onlineUserCount = stompConnectionManager.getOnlineUserCount();
            List<String> onlineUserIds = stompConnectionManager.getAllOnlineUserIds();
            sendToUsers(onlineUserIds, message);
            
            log.info("✅ 全局系统广播完成 - 在线用户数: {}, 消息ID: {}", onlineUserCount, message.getMessageId());
            
        } catch (Exception e) {
            log.error("全局系统广播失败 - 消息ID: {}, 错误: {}", message.getMessageId(), e.getMessage(), e);
        }
    }
    
    /**
     * 执行回退路由（当动态路由失败时）
     */
    private DispatchResult executeFallbackRouting(CommonStompMessage message, TopicRoutingDecision routingDecision, DispatchResult result) {
        for (String topicPath : routingDecision.getTargetTopics()) {
            try {
                sendToTopic(topicPath, message);
                result.incrementSuccessCount();
                result.addSuccessfulTopic(topicPath);
            } catch (Exception e) {
                log.error("回退路由分发失败 - 主题: {}, 消息ID: {}, 错误: {}", 
                        topicPath, message.getMessageId(), e.getMessage(), e);
                result.incrementFailureCount();
                result.addFailedTopic(topicPath, e.getMessage());
            }
        }
        
        log.info("✅ 回退路由分发完成 - 消息ID: {}, 路由策略: {}, 成功: {}, 失败: {}", 
                message.getMessageId(), routingDecision.getRoutingStrategy(), 
                result.getSuccessCount(), result.getFailureCount());
        
        return result;
    }
    
    /**
     * 获取增强Topic统计信息
     * 
     * @return Phase 2.4增强Topic统计
     */
    public Map<String, Object> getEnhancedTopicStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 消息聚合统计
        MessageAggregator.AggregationStats aggregationStats = messageAggregator.getAggregationStats();
        stats.put("aggregation", Map.of(
                "totalAggregatedMessages", aggregationStats.getTotalAggregatedMessages(),
                "totalAggregationEvents", aggregationStats.getTotalAggregationEvents(),
                "pendingMessageCount", aggregationStats.getPendingMessageCount(),
                "activeAggregationKeys", aggregationStats.getActiveAggregationKeys()
        ));
        
        // 动态路由统计
        DynamicRoutingEngine.RoutingStats routingStats = dynamicRoutingEngine.getRoutingStats();
        stats.put("routing", Map.of(
                "totalRoutingDecisions", routingStats.getTotalRoutingDecisions(),
                "successfulRoutings", routingStats.getSuccessfulRoutings(),
                "failedRoutings", routingStats.getFailedRoutings(),
                "successRate", routingStats.getSuccessRate(),
                "activeRoutingRules", routingStats.getActiveRoutingRules()
        ));
        
        // 路由策略统计
        RoutingStrategyManager.StrategyStats strategyStats = routingStrategyManager.getStrategyStats();
        stats.put("strategy", Map.of(
                "totalStrategyExecutions", strategyStats.getTotalStrategyExecutions(),
                "successfulExecutions", strategyStats.getSuccessfulExecutions(),
                "failedExecutions", strategyStats.getFailedExecutions(),
                "successRate", strategyStats.getSuccessRate(),
                "totalStrategies", strategyStats.getTotalStrategies(),
                "activeStrategies", strategyStats.getActiveStrategies()
        ));
        
        return stats;
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 为消息补充基础信息
     * 
     * @param message STOMP消息对象
     */
    private void enrichMessage(CommonStompMessage message) {
        if (message.getMessageId() == null) {
            message.setMessageId(UUID.randomUUID().toString());
        }
        if (message.getTimestamp() == null) {
            message.setTimestamp(LocalDateTime.now());
        }
    }
    
}