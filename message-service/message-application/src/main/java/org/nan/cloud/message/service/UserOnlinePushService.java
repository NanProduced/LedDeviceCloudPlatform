package org.nan.cloud.message.service;

import org.nan.cloud.message.api.dto.response.UserOnlinePushResult;

/**
 * 用户上线推送服务接口
 * 
 * 负责处理用户上线时的消息推送逻辑，包括未读消息推送、未查看任务结果通知等。
 * 这是LED设备云平台消息中心的核心功能之一，确保用户在上线后能及时获取重要信息。
 * 
 * 核心功能：
 * - 用户上线检测和处理
 * - 未读消息批量推送
 * - 未查看任务结果通知
 * - 推送结果统计和反馈
 * - 推送策略管理
 * 
 * 业务场景：
 * - 用户登录系统时自动推送
 * - 用户从离线状态恢复在线时推送
 * - 支持多设备登录的消息同步
 * - 优先级消息的即时推送
 * 
 * @author Nan
 * @since 1.0.0
 */
public interface UserOnlinePushService {
    
    /**
     * 处理用户上线事件
     * 
     * 当用户上线时调用，统一处理未读消息和未查看任务的推送。
     * 这是最常用的入口方法，封装了完整的上线推送流程。
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param organizationId 组织ID
     * @return 推送结果，包含推送的消息数量、任务数量等统计信息
     */
    UserOnlinePushResult handleUserOnline(String userId, String sessionId, String organizationId);
    
    /**
     * 推送用户未读消息
     * 
     * 专门处理用户未读消息的推送，包括个人消息、组织通知、系统消息等。
     * 支持按优先级排序推送，确保重要消息优先送达。
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID（可选，用于会话级推送）
     * @param organizationId 组织ID
     * @param maxCount 最大推送数量，防止消息过多影响性能
     * @return 成功推送的消息数量
     */
    int pushUnreadMessages(String userId, String sessionId, String organizationId, int maxCount);
    
    /**
     * 推送用户未查看的任务结果
     * 
     * 处理用户未查看的异步任务结果通知，如设备控制任务、批量操作任务等。
     * 确保用户能及时了解任务执行状态和结果。
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID（可选）
     * @param organizationId 组织ID
     * @return 成功推送的任务结果数量
     */
    int pushUnviewedTaskResults(String userId, String sessionId, String organizationId);
    
    /**
     * 推送高优先级消息
     * 
     * 专门推送高优先级或紧急消息，如设备告警、系统通知等。
     * 这类消息需要用户立即关注，因此单独处理。
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param organizationId 组织ID
     * @return 推送的高优先级消息数量
     */
    int pushHighPriorityMessages(String userId, String sessionId, String organizationId);
    
    /**
     * 推送用户统计信息
     * 
     * 推送用户的消息统计信息，如未读消息总数、未完成任务数等。
     * 通常作为用户上线后的概况信息展示。
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param organizationId 组织ID
     * @return 是否推送成功
     */
    boolean pushUserStatistics(String userId, String sessionId, String organizationId);
    
    /**
     * 检查用户是否需要推送
     * 
     * 在推送前检查用户状态，避免无效推送。
     * 检查内容包括用户在线状态、未读消息数量、上次推送时间等。
     * 
     * @param userId 用户ID
     * @param organizationId 组织ID
     * @return 是否需要推送
     */
    boolean shouldPushToUser(String userId, String organizationId);
    
    /**
     * 标记推送完成
     * 
     * 推送完成后更新相关状态，包括消息已读状态、任务查看状态等。
     * 防止重复推送和维护数据一致性。
     * 
     * @param userId 用户ID
     * @param organizationId 组织ID
     * @param pushedMessageIds 已推送的消息ID列表
     * @param pushedTaskIds 已推送的任务ID列表
     */
    void markPushCompleted(String userId, String organizationId, 
                          java.util.List<String> pushedMessageIds, 
                          java.util.List<String> pushedTaskIds);
    
    /**
     * 获取推送策略配置
     * 
     * 获取当前的推送策略配置，如最大推送数量、推送间隔、优先级规则等。
     * 支持运行时动态调整推送行为。
     * 
     * @param organizationId 组织ID
     * @return 推送策略配置
     */
    java.util.Map<String, Object> getPushStrategy(String organizationId);
    
    /**
     * 更新推送策略
     * 
     * 动态更新推送策略，支持按组织进行个性化配置。
     * 
     * @param organizationId 组织ID
     * @param strategy 新的推送策略配置
     * @return 是否更新成功
     */
    boolean updatePushStrategy(String organizationId, java.util.Map<String, Object> strategy);
    
    /**
     * 异步处理用户上线
     * 
     * 异步方式处理用户上线推送，避免阻塞主线程。
     * 适用于推送内容较多或推送逻辑复杂的场景。
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param organizationId 组织ID
     * @return 异步任务ID，可用于查询处理进度
     */
    String handleUserOnlineAsync(String userId, String sessionId, String organizationId);
    
    /**
     * 查询异步推送状态
     * 
     * 查询异步推送任务的执行状态和结果。
     * 
     * @param taskId 异步任务ID
     * @return 推送状态和结果
     */
    UserOnlinePushResult getAsyncPushResult(String taskId);
}