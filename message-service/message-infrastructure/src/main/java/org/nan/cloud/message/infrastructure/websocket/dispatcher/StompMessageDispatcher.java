package org.nan.cloud.message.infrastructure.websocket.dispatcher;

import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.infrastructure.service.RealtimeMessagePersistenceService;
import org.nan.cloud.message.infrastructure.websocket.manager.StompConnectionManager;
import org.nan.cloud.message.infrastructure.websocket.routing.TopicRoutingDecision;
import org.nan.cloud.message.infrastructure.websocket.routing.TopicRoutingManager;
import org.nan.cloud.message.infrastructure.websocket.sender.StompMessageSender;
import org.nan.cloud.message.api.stomp.StompMessageTypes;
import org.nan.cloud.message.infrastructure.websocket.stomp.enums.StompTopic;
import org.nan.cloud.message.api.stomp.CommonStompMessage;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
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
public class StompMessageDispatcher {

    private final StompConnectionManager stompConnectionManager;
    private final TopicRoutingManager topicRoutingManager;
    private final StompMessageSender messageSender;
    private final RealtimeMessagePersistenceService realtimeMessagePersistenceService;
    
    public StompMessageDispatcher(StompConnectionManager stompConnectionManager,
                                TopicRoutingManager topicRoutingManager,
                                StompMessageSender messageSender,
                                RealtimeMessagePersistenceService  realtimeMessagePersistenceService) {
        this.stompConnectionManager = stompConnectionManager;
        this.topicRoutingManager = topicRoutingManager;
        this.messageSender = messageSender;
        this.realtimeMessagePersistenceService = realtimeMessagePersistenceService;
    }
    
    // ==================== 智能路由分发 ====================

    public DispatchResult smartDispatch(CommonStompMessage message) {
        try {
            log.debug("开始智能分发消息 - 消息类型: {}, 消息ID: {}", message.getMessageType(), message.getMessageId());

            // 设置消息基础信息
            enrichMessage(message);
            TopicRoutingDecision topicRoutingDecision = topicRoutingManager.decideRouting(message);

            DispatchResult result = new DispatchResult(message.getMessageId());

            realtimeMessagePersistenceService.persistMessageAsync(message, result);

            for (String topic : topicRoutingDecision.getTargetTopics()) {
                try {
                    if (topic.startsWith("/queue/")) {
                        sendToUser(message.getContext().getUid().toString(), message);
                    }
                    else {
                        sendToTopic(topic, message);
                    }
                    result.incrementSuccessCount();
                    result.addSuccessfulTopic(topic);
                } catch (Exception e) {
                    log.error("向主题分发消息失败 - 主题: {}, 消息ID: {}, 错误: {}",
                            topic, message.getMessageId(), e.getMessage(), e);
                    result.incrementFailureCount();
                    result.addFailedTopic(topic, e.getMessage());
                }

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
     * 注：这里的消息都会持久化
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
                // TODO: 可以考虑将消息存储到离线消息队列, 上线时推送
                // 暂时没必要
                return;
            }

            // 持久化
            realtimeMessagePersistenceService.persistMessageAsync(message, null);
            
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
    

    
    // ==================== 组织级别消息分发 ====================

    

    
    // ==================== 终端级别消息分发 ====================
    

    
    // ==================== 批量指令消息分发 ====================
    

    
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
            sendToTopic(StompTopic.SYSTEM_TOPIC, message);
            
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
            message.setTimestamp(Instant.now().toString());
        }
    }
    
}