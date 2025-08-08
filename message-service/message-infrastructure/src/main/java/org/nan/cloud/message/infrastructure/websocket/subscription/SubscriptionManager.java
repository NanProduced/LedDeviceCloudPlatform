package org.nan.cloud.message.infrastructure.websocket.subscription;

import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.core.api.feign.StompPermissionClient;
import org.nan.cloud.core.api.DTO.req.TopicPermissionRequest;
import org.nan.cloud.core.api.DTO.res.TopicPermissionResponse;
import org.nan.cloud.message.api.enums.Priority;
import org.nan.cloud.message.api.stomp.CommonStompMessage;
import org.nan.cloud.message.api.stomp.StompMessageLevel;
import org.nan.cloud.message.api.stomp.StompMessageTypes;
import org.nan.cloud.message.infrastructure.websocket.routing.SubscriptionLevel;
import org.nan.cloud.message.infrastructure.websocket.routing.TopicRoutingManager;
import org.nan.cloud.message.infrastructure.websocket.security.GatewayUserInfo;
import org.nan.cloud.message.infrastructure.websocket.stomp.enums.StompTopic;
import org.nan.cloud.message.api.stomp.StompTopicType;
import org.nan.cloud.message.infrastructure.websocket.sender.StompMessageSender;
import org.nan.cloud.message.infrastructure.websocket.stomp.payload.SubscribeFeedbackPayload;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
/**
 * STOMP订阅管理器
 * 
 * 核心职责：
 * 1. STOMP订阅生命周期管理 - 处理订阅/取消订阅事件
 * 2. 权限验证集成 - 通过Feign RPC调用core-service验证权限
 * 3. 订阅状态维护 - 与TopicRoutingManager协作管理订阅状态
 * 4. 订阅反馈消息发送 - 向客户端发送订阅结果反馈
 * 
 * 设计原则：
 * - 权限验证通过core-service实现，避免在message-service中引入数据库逻辑
 * - 所有订阅由客户端主动发起，服务器端不执行自动订阅
 * - 作为技术基础设施，对最终用户透明
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Component
public class SubscriptionManager {
    
    private final TopicRoutingManager topicRoutingManager;
    private final StompPermissionClient stompPermissionClient;
    private final StompMessageSender stompMessageSender;

    public SubscriptionManager(TopicRoutingManager topicRoutingManager,
                               @Lazy StompPermissionClient stompPermissionClient,
                               @Lazy StompMessageSender stompMessageSender
    ) {
        this.topicRoutingManager = topicRoutingManager;
        this.stompPermissionClient = stompPermissionClient;
        this.stompMessageSender = stompMessageSender;
    }

    // ==================== 订阅权限验证 ====================
    
    /**
     * 验证用户是否有权限订阅指定主题
     * 
     * @param userInfo 用户信息
     * @param topicPath 主题路径
     * @param sessionId 会话ID
     * @return true表示有权限，false表示无权限
     */
    public boolean verifySubscriptionPermission(GatewayUserInfo userInfo, String topicPath, String sessionId) {
        try {
            log.debug("验证订阅权限 - 用户: {}, 主题: {}, 会话: {}", userInfo.getUid(), topicPath, sessionId);

            // 构建权限验证请求
            TopicPermissionRequest request = buildPermissionRequest(userInfo, topicPath, sessionId);

            // 用户个人队列和系统队列不用验证
            if (request.getTopicType().equals(StompTopicType.USER.name()) || request.getTopicType().equals(StompTopicType.SYSTEM.name())) {
                return true;
            }

            // 调用core-service验证权限
            TopicPermissionResponse response = stompPermissionClient.verifyTopicSubscriptionPermission(request);

            if (response != null && Boolean.TRUE.equals(response.getHasPermission())) {
                log.debug("✅ 订阅权限验证通过 - 用户: {}, 主题: {}", userInfo.getUid(), topicPath);
                return true;
            } else {
                String reason = response != null ? response.getDeniedReason() : "权限验证服务无响应";
                log.warn("❌ 订阅权限验证失败 - 用户: {}, 主题: {}, 原因: {}",
                        userInfo.getUid(), topicPath, reason);
                return false;
            }

        } catch (Exception e) {
            log.error("订阅权限验证异常 - 用户: {}, 主题: {}, 错误: {}",
                    userInfo.getUid(), topicPath, e.getMessage(), e);
            // 权限验证异常时，为了安全起见，拒绝订阅
            return false;
        }
    }
    
    /**
     * 批量验证用户对多个主题的订阅权限
     * 
     * @param userInfo 用户信息
     * @param topicPaths 主题路径列表
     * @param sessionId 会话ID
     * @return 有权限的主题列表
     */
    public List<String> batchVerifySubscriptionPermissions(GatewayUserInfo userInfo, List<String> topicPaths, String sessionId) {
        List<String> allowedTopics = new ArrayList<>();
        
        try {
            log.debug("批量验证订阅权限 - 用户: {}, 主题数: {}, 会话: {}", 
                    userInfo.getUid(), topicPaths.size(), sessionId);
            
            // 构建批量权限验证请求
            TopicPermissionRequest request = TopicPermissionRequest.builder()
                    .uid(userInfo.getUid())
                    .oid(userInfo.getOid())
                    .ugid(userInfo.getUgid())
                    .topicPaths(topicPaths)
                    .sessionId(sessionId)
                    .build();
            
            // 调用core-service批量验证权限
            TopicPermissionResponse response = stompPermissionClient.batchVerifyTopicSubscriptionPermission(request);
            
            if (response != null && response.getBatchResults() != null) {
                for (var entry : response.getBatchResults().entrySet()) {
                    String topicPath = entry.getKey();
                    var result = entry.getValue();
                    
                    if (Boolean.TRUE.equals(result.getHasPermission())) {
                        allowedTopics.add(topicPath);
                        log.debug("✅ 批量权限验证通过 - 用户: {}, 主题: {}", userInfo.getUid(), topicPath);
                    } else {
                        log.debug("❌ 批量权限验证失败 - 用户: {}, 主题: {}, 原因: {}", 
                                userInfo.getUid(), topicPath, result.getDeniedReason());
                    }
                }
            }
            
            log.info("✅ 批量权限验证完成 - 用户: {}, 总主题: {}, 通过: {}", 
                    userInfo.getUid(), topicPaths.size(), allowedTopics.size());
            
        } catch (Exception e) {
            log.error("批量订阅权限验证异常 - 用户: {}, 主题数: {}, 错误: {}", 
                    userInfo.getUid(), topicPaths.size(), e.getMessage(), e);
        }
        
        return allowedTopics;
    }
    
    // ==================== 订阅生命周期管理 ====================
    
    /**
     * 处理用户订阅请求
     * 
     * @param userInfo 用户信息
     * @param topicPath 主题路径
     * @param sessionId 会话ID
     * @return 订阅处理结果
     */
    public SubscriptionResult handleSubscription(GatewayUserInfo userInfo, String topicPath, String sessionId) {
        try {
            String userId = userInfo.getUid().toString();
            log.info("处理用户订阅 - 用户: {}, 主题: {}, 会话: {}", userId, topicPath, sessionId);
            
            // 1. 验证订阅权限
            if (!verifySubscriptionPermission(userInfo, topicPath, sessionId)) {
                String errorMessage = "无权限订阅主题: " + topicPath;
                
                // 发送订阅失败反馈消息给客户端
                sendSubscriptionFeedback(userInfo, topicPath, null, false, errorMessage);
                
                return SubscriptionResult.denied(errorMessage);
            }
            
            // 2. 确定订阅层次
            SubscriptionLevel subscriptionLevel = determineSubscriptionLevel(topicPath);
            
            // 3. 注册订阅到路由管理器
            boolean isNewSubscription = topicRoutingManager.registerUserSubscription(userId, topicPath, subscriptionLevel, sessionId);
            
            if (isNewSubscription) {
                log.info("✅ 用户订阅成功 - 用户: {}, 主题: {}, 层次: {}", userId, topicPath, subscriptionLevel);
                
                // 4. 发送订阅成功反馈消息给客户端
                if (!topicPath.startsWith("/user/queue/message")) {
                    sendSubscriptionFeedback(userInfo, topicPath, subscriptionLevel, true, null);
                }
            } else {
                log.info("⚠️ 用户重复订阅 - 用户: {}, 主题: {}, 层次: {} (订阅已存在，忽略)", userId, topicPath, subscriptionLevel);
                
                // 对于重复订阅，仍然返回成功，但不发送反馈消息，避免客户端收到重复通知
            }

            return SubscriptionResult.success(topicPath, subscriptionLevel);
            
        } catch (Exception e) {
            log.error("处理用户订阅失败 - 用户: {}, 主题: {}, 错误: {}", 
                    userInfo.getUid(), topicPath, e.getMessage(), e);
            
            String errorMessage = "订阅处理异常: " + e.getMessage();
            
            // 发送订阅异常反馈消息给客户端
            sendSubscriptionFeedback(userInfo, topicPath, null, false, errorMessage);
            
            return SubscriptionResult.error(errorMessage);
        }
    }
    
    /**
     * 处理用户取消订阅请求
     * 
     * @param userInfo 用户信息
     * @param topicPath 主题路径
     * @param sessionId 会话ID
     * @return 取消订阅处理结果
     */
    public SubscriptionResult handleUnsubscription(GatewayUserInfo userInfo, String topicPath, String sessionId) {
        try {
            String userId = userInfo.getUid().toString();
            log.info("处理用户取消订阅 - 用户: {}, 主题: {}, 会话: {}", userId, topicPath, sessionId);
            
            // 从路由管理器移除订阅
            topicRoutingManager.removeUserSubscription(userId, topicPath, sessionId);
            
            log.info("✅ 用户取消订阅成功 - 用户: {}, 主题: {}", userId, topicPath);
            
            // 发送取消订阅成功反馈消息给客户端
            sendUnsubscriptionFeedback(userInfo, topicPath, true, null);
            
            return SubscriptionResult.success(topicPath, null);
            
        } catch (Exception e) {
            log.error("处理用户取消订阅失败 - 用户: {}, 主题: {}, 错误: {}", 
                    userInfo.getUid(), topicPath, e.getMessage(), e);
            
            String errorMessage = "取消订阅处理异常: " + e.getMessage();
            
            // 发送取消订阅失败反馈消息给客户端
            sendUnsubscriptionFeedback(userInfo, topicPath, false, errorMessage);
            
            return SubscriptionResult.error(errorMessage);
        }
    }
    
    // ==================== 会话清理管理 ====================
    
    
    /**
     * 清理用户会话相关的所有订阅
     * 在用户断开连接时调用
     * 
     * @param userInfo 用户信息
     * @param sessionId 会话ID
     */
    public void cleanupUserSessionSubscriptions(GatewayUserInfo userInfo, String sessionId) {
        try {
            String userId = userInfo.getUid().toString();
            log.info("清理用户会话订阅 - 用户: {}, 会话: {}", userId, sessionId);
            
            // 调用路由管理器清理会话订阅
            topicRoutingManager.cleanupUserSessionSubscriptions(userId, sessionId);
            
            log.info("✅ 用户会话订阅清理完成 - 用户: {}, 会话: {}", userId, sessionId);
            
        } catch (Exception e) {
            log.error("清理用户会话订阅失败 - 用户: {}, 会话: {}, 错误: {}", 
                    userInfo.getUid(), sessionId, e.getMessage(), e);
        }
    }
    
    /**
     * 获取用户当前已订阅的主题
     * 
     * @param userId 用户ID
     * @return 已订阅主题集合
     */
    public Set<String> getUserSubscribedTopics(String userId) {
        return topicRoutingManager.getUserSubscribedTopics(userId);
    }
    
    // ==================== 私有工具方法 ====================
    
    /**
     * 构建权限验证请求
     */
    private TopicPermissionRequest buildPermissionRequest(GatewayUserInfo userInfo, String topicPath, String sessionId) {
        // 从主题路径中提取资源ID（设备ID、任务ID等）
        String terminalId = extractTerminalIdFromTopic(topicPath);
        
        return TopicPermissionRequest.builder()
                .uid(userInfo.getUid())
                .oid(userInfo.getOid())
                .ugid(userInfo.getUgid())
                .userType(userInfo.getUserType())
                .tid(terminalId)
                .topicPath(topicPath)
                .topicType(determineTopicType(topicPath))
                .sessionId(sessionId)
                .build();
    }
    
    
    /**
     * 确定订阅层次 (根据极简化Topic结构)
     */
    private SubscriptionLevel determineSubscriptionLevel(String topicPath) {
        // 个人消息队列使用会话订阅，允许重连后重新订阅
        if (topicPath.startsWith("/user/queue/")) {
            return SubscriptionLevel.SESSION;
        }
        
        // 组织消息主题使用持久订阅
        if (topicPath.startsWith("/topic/org/")) {
            return SubscriptionLevel.PERSISTENT;
        }
        
        // 系统消息主题使用全局订阅
        if (topicPath.equals(StompTopic.SYSTEM_TOPIC)) {
            return SubscriptionLevel.GLOBAL;
        }
        
        // 设备相关主题使用会话订阅（按需订阅）
        if (topicPath.startsWith("/topic/device/")) {
            return SubscriptionLevel.SESSION;
        }
        
        // 任务相关主题使用临时订阅
        if (topicPath.startsWith("/topic/task/")) {
            return SubscriptionLevel.TEMPORARY;
        }
        
        // 批量聚合相关主题使用临时订阅
        if (topicPath.startsWith("/topic/batch/")) {
            return SubscriptionLevel.TEMPORARY;
        }
        
        // 默认使用会话订阅
        return SubscriptionLevel.SESSION;
    }
    
    /**
     * 确定主题类型 (根据极简化Topic结构)
     */
    private String determineTopicType(String topicPath) {
        // 个人消息队列
        if (topicPath.startsWith("/user/queue/")) {
            return StompTopicType.USER.name();
        }
        // 组织消息主题
        else if (topicPath.startsWith("/topic/org/")) {
            return StompTopicType.ORG.name();
        }
        // 设备消息主题（包含终端）
        else if (topicPath.startsWith("/topic/device/")) {
            return StompTopicType.DEVICE.name();
        }
        // 任务消息主题
        else if (topicPath.startsWith("/topic/task/")) {
            return StompTopicType.TASK.name();
        }
        // 批量聚合消息主题
        else if (topicPath.startsWith("/topic/batch/")) {
            return StompTopicType.BATCH.name();
        }
        // 系统消息主题
        else if (topicPath.equals(StompTopic.SYSTEM_TOPIC)) {
            return StompTopicType.SYSTEM.name();
        }
        return "UNKNOWN";
    }
    
    /**
     * 从主题路径中提取资源ID (设备ID/任务ID/批量ID等)
     */
    private String extractTerminalIdFromTopic(String topicPath) {
        if (topicPath != null) {
            // 设备主题: /topic/device/{deviceId}
            if (topicPath.startsWith("/topic/device/")) {
                String[] parts = topicPath.split("/");
                if (parts.length >= 4) {
                    return parts[3]; // /topic/device/{deviceId}
                }
            }
        }
        return null;
    }
    
    // ==================== 订阅反馈消息发送 ====================
    
    /**
     * 发送订阅反馈消息给客户端
     * 
     * @param userInfo 用户信息
     * @param topicPath 主题路径
     * @param subscriptionLevel 订阅层次（成功时）
     * @param success 是否成功
     * @param errorMessage 错误消息（失败时）
     */
    private void sendSubscriptionFeedback(GatewayUserInfo userInfo, String topicPath, 
                                        SubscriptionLevel subscriptionLevel, boolean success, String errorMessage) {
        try {
            String userId = userInfo.getUid().toString();

            SubscribeFeedbackPayload payload = SubscribeFeedbackPayload.builder()
                    .uid(userInfo.getUid())
                    .subscriptionLevel(subscriptionLevel)
                    .topic(topicPath)
                    .success(success)
                    .errorMsg(success ? null : errorMessage)
                    .operation(SubscribeFeedbackPayload.SubscribeOperation.SUBSCRIBE)
                    .build();

            CommonStompMessage message = CommonStompMessage.builder()
                    .messageId(UUID.randomUUID().toString())
                    .timestamp(Instant.now().toString())
                    .oid(userInfo.getOid())
                    .messageType(StompMessageTypes.TOPIC_SUBSCRIBE_FEEDBACK)
                    .level(StompMessageLevel.IGNORE)
                    .payload(payload)
                    .priority(Priority.NORMAL)
                    .requireAck(false)
                    .build();
            
            // 发送反馈消息到用户的个人消息队列
            boolean sent = stompMessageSender.sendToUser(userId, StompTopic.USER_MESSAGES_QUEUE, message);
            
            if (sent) {
                log.debug("✅ 订阅反馈消息发送成功 - 用户: {}, 主题: {}, 成功: {}", 
                        userId, topicPath, success);
            } else {
                log.warn("⚠️ 订阅反馈消息发送失败 - 用户: {}, 主题: {}", userId, topicPath);
            }
            
        } catch (Exception e) {
            log.error("发送订阅反馈消息异常 - 用户: {}, 主题: {}, 错误: {}", 
                    userInfo.getUid(), topicPath, e.getMessage(), e);
        }
    }
    
    /**
     * 发送取消订阅反馈消息给客户端
     * 
     * @param userInfo 用户信息
     * @param topicPath 主题路径
     * @param success 是否成功
     * @param errorMessage 错误消息（失败时）
     */
    private void sendUnsubscriptionFeedback(GatewayUserInfo userInfo, String topicPath, 
                                          boolean success, String errorMessage) {
        try {
            String userId = userInfo.getUid().toString();

            SubscribeFeedbackPayload payload = SubscribeFeedbackPayload.builder()
                    .uid(userInfo.getUid())
                    .topic(topicPath)
                    .success(success)
                    .errorMsg(success ? null : errorMessage)
                    .operation(SubscribeFeedbackPayload.SubscribeOperation.UNSUBSCRIBE)
                    .build();

            CommonStompMessage message = CommonStompMessage.builder()
                    .messageId(UUID.randomUUID().toString())
                    .timestamp(Instant.now().toString())
                    .oid(userInfo.getOid())
                    .messageType(StompMessageTypes.TOPIC_SUBSCRIBE_FEEDBACK)
                    .level(StompMessageLevel.IGNORE)
                    .payload(payload)
                    .priority(Priority.NORMAL)
                    .requireAck(false)
                    .build();
            
            // 发送反馈消息到用户的个人消息队列
            boolean sent = stompMessageSender.sendToUser(userId, StompTopic.USER_MESSAGES_QUEUE, message);
            
            if (sent) {
                log.debug("✅ 取消订阅反馈消息发送成功 - 用户: {}, 主题: {}, 成功: {}", 
                        userId, topicPath, success);
            } else {
                log.warn("⚠️ 取消订阅反馈消息发送失败 - 用户: {}, 主题: {}", userId, topicPath);
            }
            
        } catch (Exception e) {
            log.error("发送取消订阅反馈消息异常 - 用户: {}, 主题: {}, 错误: {}", 
                    userInfo.getUid(), topicPath, e.getMessage(), e);
        }
    }
}