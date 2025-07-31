package org.nan.cloud.message.infrastructure.websocket.subscription;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.core.api.feign.StompPermissionClient;
import org.nan.cloud.core.api.DTO.req.TopicPermissionRequest;
import org.nan.cloud.core.api.DTO.res.TopicPermissionResponse;
import org.nan.cloud.message.infrastructure.websocket.routing.SubscriptionLevel;
import org.nan.cloud.message.infrastructure.websocket.routing.TopicRoutingManager;
import org.nan.cloud.message.infrastructure.websocket.security.GatewayUserInfo;
import org.nan.cloud.message.infrastructure.websocket.stomp.enums.StompTopic;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * STOMP订阅管理器
 * 
 * 核心职责：
 * 1. STOMP订阅生命周期管理 - 处理订阅/取消订阅事件
 * 2. 权限验证集成 - 通过Feign RPC调用core-service验证权限
 * 3. 自动订阅规则实现 - 基于明确业务规则的自动订阅
 * 4. 订阅状态维护 - 与TopicRoutingManager协作管理订阅状态
 * 
 * 设计原则：
 * - 权限验证通过core-service实现，避免在message-service中引入数据库逻辑
 * - 自动订阅基于明确规则：用户个人topic + 组织topic
 * - 终端topic需要显式订阅，不自动订阅
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

    public SubscriptionManager(TopicRoutingManager topicRoutingManager,
                               @Lazy StompPermissionClient stompPermissionClient
    ) {
        this.topicRoutingManager = topicRoutingManager;
        this.stompPermissionClient = stompPermissionClient;
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
                return SubscriptionResult.denied("无权限订阅主题: " + topicPath);
            }
            
            // 2. 确定订阅层次
            SubscriptionLevel subscriptionLevel = determineSubscriptionLevel(topicPath);
            
            // 3. 注册订阅到路由管理器
            topicRoutingManager.registerUserSubscription(userId, topicPath, subscriptionLevel, sessionId);
            
            log.info("✅ 用户订阅成功 - 用户: {}, 主题: {}, 层次: {}", userId, topicPath, subscriptionLevel);

            return SubscriptionResult.success(topicPath, subscriptionLevel);
            
        } catch (Exception e) {
            log.error("处理用户订阅失败 - 用户: {}, 主题: {}, 错误: {}", 
                    userInfo.getUid(), topicPath, e.getMessage(), e);
            return SubscriptionResult.error("订阅处理异常: " + e.getMessage());
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
            return SubscriptionResult.success(topicPath, null);
            
        } catch (Exception e) {
            log.error("处理用户取消订阅失败 - 用户: {}, 主题: {}, 错误: {}", 
                    userInfo.getUid(), topicPath, e.getMessage(), e);
            return SubscriptionResult.error("取消订阅处理异常: " + e.getMessage());
        }
    }
    
    // ==================== 自动订阅管理 ====================
    
    /**
     * 为新连接的用户执行自动订阅
     * 
     * 自动订阅规则（基于明确的业务规则）：
     * 1. 用户个人通知主题：/topic/user/{userId}/notifications
     * 2. 组织公告主题：/topic/org/{orgId}/announcements
     * 
     * 注意：终端相关主题不自动订阅，需要用户进入特定页面后显式订阅
     * 
     * @param userInfo 用户信息
     * @param sessionId 会话ID
     * @return 自动订阅结果
     */
    public AutoSubscriptionResult performAutoSubscription(GatewayUserInfo userInfo, String sessionId) {
        try {
            String userId = userInfo.getUid().toString();
            String orgId = userInfo.getOid().toString();
            
            log.info("开始自动订阅 - 用户: {}, 组织: {}, 会话: {}", userId, orgId, sessionId);
            
            // 构建自动订阅主题列表
            List<String> autoSubscriptionTopics = buildAutoSubscriptionTopics(userInfo);
            
            // 使用auth认证信息中的信息构造订阅topic，不需要权限验证
            // 执行自动订阅
            List<String> successfulSubscriptions = new ArrayList<>();
            List<String> failedSubscriptions = new ArrayList<>();
            
            for (String topicPath : autoSubscriptionTopics) {
                try {
                    SubscriptionLevel level = determineSubscriptionLevel(topicPath);
                    topicRoutingManager.registerUserSubscription(userId, topicPath, level, sessionId);
                    successfulSubscriptions.add(topicPath);
                    
                    log.debug("✅ 自动订阅成功 - 用户: {}, 主题: {}", userId, topicPath);
                    
                } catch (Exception e) {
                    failedSubscriptions.add(topicPath);
                    log.warn("❌ 自动订阅失败 - 用户: {}, 主题: {}, 错误: {}", 
                            userId, topicPath, e.getMessage());
                }
            }
            
            log.info("✅ 自动订阅完成 - 用户: {}, 成功: {}, 失败: {}", 
                    userId, successfulSubscriptions.size(), failedSubscriptions.size());
            
            return AutoSubscriptionResult.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .requestedTopics(autoSubscriptionTopics)
                    .allowedTopics(autoSubscriptionTopics)
                    .successfulSubscriptions(successfulSubscriptions)
                    .failedSubscriptions(failedSubscriptions)
                    .build();
            
        } catch (Exception e) {
            log.error("自动订阅异常 - 用户: {}, 会话: {}, 错误: {}", 
                    userInfo.getUid(), sessionId, e.getMessage(), e);
            
            return AutoSubscriptionResult.builder()
                    .userId(userInfo.getUid().toString())
                    .sessionId(sessionId)
                    .errorMessage("自动订阅异常: " + e.getMessage())
                    .build();
        }
    }
    
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
        // 从主题路径中提取终端ID（如果是终端相关主题）
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
     * 构建自动订阅主题列表
     */
    private List<String> buildAutoSubscriptionTopics(GatewayUserInfo userInfo) {
        List<String> topics = new ArrayList<>();
        
        String userId = userInfo.getUid().toString();
        String orgId = userInfo.getOid().toString();
        
        // 1. 用户个人通知主题
        topics.add(StompTopic.buildUserNotificationTopic(userId));
        
        // 2. 组织公告主题
        topics.add(StompTopic.buildOrgAnnouncementTopic(orgId));
        
        // 注意：不自动订阅终端相关主题，需要用户显式订阅
        
        return topics;
    }
    
    /**
     * 确定订阅层次
     */
    private SubscriptionLevel determineSubscriptionLevel(String topicPath) {
        // 用户个人主题和组织主题使用持久订阅
        if (topicPath.startsWith("/topic/user/") || topicPath.startsWith("/topic/org/")) {
            return SubscriptionLevel.PERSISTENT;
        }
        
        // 终端相关主题使用会话订阅
        if (topicPath.startsWith("/topic/terminal/")) {
            return SubscriptionLevel.SESSION;
        }
        
        // 批量指令相关主题使用临时订阅
        if (topicPath.startsWith("/topic/commandTask/")) {
            return SubscriptionLevel.TEMPORARY;
        }
        
        // 系统主题使用全局订阅
        if (topicPath.startsWith("/topic/global/")) {
            return SubscriptionLevel.GLOBAL;
        }
        
        // 默认使用会话订阅
        return SubscriptionLevel.SESSION;
    }
    
    /**
     * 确定主题类型
     */
    private String determineTopicType(String topicPath) {
        if (topicPath.startsWith("/topic/user/")) {
            return "USER";
        } else if (topicPath.startsWith("/topic/org/")) {
            return "ORG";
        } else if (topicPath.startsWith("/topic/terminal/")) {
            return "TERMINAL";
        } else if (topicPath.startsWith("/topic/global/")) {
            return "SYSTEM";
        } else if (topicPath.startsWith("/topic/commandTask/")) {
            return "BATCH_COMMAND";
        }
        return "UNKNOWN";
    }
    
    /**
     * 从主题路径中提取终端ID
     */
    private String extractTerminalIdFromTopic(String topicPath) {
        if (topicPath != null && topicPath.startsWith("/topic/terminal/")) {
            String[] parts = topicPath.split("/");
            if (parts.length >= 4) {
                return parts[3]; // /topic/terminal/{terminalId}/...
            }
        }
        return null;
    }
}