package org.nan.cloud.message.service;

import org.nan.cloud.message.api.dto.response.UserStatusResponse;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户在线状态管理服务接口
 * 
 * 提供用户在线状态的完整管理功能，包括状态追踪、统计分析、事件通知等。
 * 这是消息中心的核心服务之一，为整个平台提供实时的用户在线状态信息。
 * 
 * 核心功能：
 * - 用户上线/下线状态管理
 * - 多设备登录支持
 * - 在线状态实时查询
 * - 组织维度统计分析
 * - 在线状态变更事件通知
 * - 心跳检测和超时处理
 * - 活跃度统计分析
 * 
 * 应用场景：
 * - 实时显示用户在线状态
 * - 消息推送前的在线状态检查
 * - 组织内在线用户统计
 * - 用户活跃度分析
 * - 设备管理和多设备登录控制
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface UserOnlineStatusService {
    
    // ==================== 基础状态管理 ====================
    
    /**
     * 标记用户上线
     * 
     * 用户建立WebSocket连接或登录时调用，记录用户的在线状态。
     * 支持多设备登录，会维护所有设备的连接信息。
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param organizationId 组织ID
     * @param deviceType 设备类型（WEB/MOBILE/DESKTOP/TABLET/IOT）
     * @param ipAddress 客户端IP地址
     * @param userAgent 用户代理信息
     * @return 用户当前的在线状态信息
     */
    UserStatusResponse markUserOnline(String userId, String sessionId, String organizationId, 
                                    String deviceType, String ipAddress, String userAgent);
    
    /**
     * 标记用户离线
     * 
     * 用户断开WebSocket连接或登出时调用。
     * 如果用户有多个设备连接，只标记指定会话离线。
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID（可选，为null时标记所有会话离线）
     * @param reason 离线原因（LOGOUT/TIMEOUT/CONNECTION_LOST/MANUAL）
     * @return 是否成功标记离线
     */
    boolean markUserOffline(String userId, String sessionId, String reason);
    
    /**
     * 更新用户心跳
     * 
     * WebSocket心跳检测时调用，更新用户的最后活跃时间。
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @return 是否更新成功
     */
    boolean updateUserHeartbeat(String userId, String sessionId);
    
    // ==================== 状态查询 ====================
    
    /**
     * 检查用户是否在线
     * 
     * @param userId 用户ID
     * @return 是否在线
     */
    boolean isUserOnline(String userId);
    
    /**
     * 获取用户详细在线状态
     * 
     * @param userId 用户ID
     * @return 用户在线状态详情，包括设备信息、连接时间等
     */
    UserStatusResponse getUserOnlineStatus(String userId);
    
    /**
     * 批量查询用户在线状态
     * 
     * @param userIds 用户ID列表
     * @return 用户ID到在线状态的映射
     */
    Map<String, UserStatusResponse> batchGetUserOnlineStatus(List<String> userIds);
    
    /**
     * 获取用户的所有在线设备
     * 
     * @param userId 用户ID
     * @return 在线设备列表（会话信息）
     */
    List<UserStatusResponse.DeviceInfo> getUserOnlineDevices(String userId);
    
    // ==================== 组织维度统计 ====================
    
    /**
     * 获取组织内在线用户列表
     * 
     * @param organizationId 组织ID
     * @param includeSubOrg 是否包含子组织
     * @return 在线用户列表
     */
    List<UserStatusResponse> getOrganizationOnlineUsers(String organizationId, boolean includeSubOrg);
    
    /**
     * 获取组织在线用户统计
     * 
     * @param organizationId 组织ID
     * @return 在线用户统计信息
     */
    UserStatusResponse.OrganizationStats getOrganizationOnlineStats(String organizationId);
    
    /**
     * 获取平台总体在线统计
     * 
     * @return 平台在线统计信息
     */
    UserStatusResponse.PlatformStats getPlatformOnlineStats();
    
    // ==================== 活跃度分析 ====================
    
    /**
     * 记录用户活跃行为
     * 
     * 用户发送消息、操作设备等行为时调用。
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param action 行为类型（SEND_MESSAGE/CONTROL_DEVICE/VIEW_PAGE等）
     * @param details 行为详情
     */
    void recordUserActivity(String userId, String sessionId, String action, Map<String, Object> details);
    
    /**
     * 获取用户活跃度信息
     * 
     * @param userId 用户ID
     * @param hours 统计时间范围（小时）
     * @return 用户活跃度统计
     */
    UserStatusResponse.ActivityStats getUserActivityStats(String userId, int hours);
    
    /**
     * 获取组织活跃度统计
     * 
     * @param organizationId 组织ID
     * @param hours 统计时间范围（小时）
     * @return 组织活跃度统计
     */
    UserStatusResponse.OrganizationActivityStats getOrganizationActivityStats(String organizationId, int hours);
    
    // ==================== 高级功能 ====================
    
    /**
     * 强制用户下线
     * 
     * 管理员操作，强制指定用户下线。
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID（可选，为null时下线所有会话）
     * @param reason 强制下线原因
     * @param operatorId 操作者ID
     * @return 是否成功强制下线
     */
    boolean forceUserOffline(String userId, String sessionId, String reason, String operatorId);
    
    /**
     * 获取用户在线历史
     * 
     * @param userId 用户ID
     * @param days 查询天数
     * @return 在线历史记录
     */
    List<UserStatusResponse.OnlineHistory> getUserOnlineHistory(String userId, int days);
    
    /**
     * 清理过期的在线状态
     * 
     * 定时任务调用，清理超时未更新心跳的在线状态。
     * 
     * @param timeoutMinutes 超时时间（分钟）
     * @return 清理的用户数量
     */
    int cleanupExpiredOnlineStatus(int timeoutMinutes);
    
    /**
     * 同步在线状态
     * 
     * 集群环境下同步各节点的在线状态信息。
     * 
     * @param sourceInstanceId 源实例ID
     * @return 同步的状态数量
     */
    int syncOnlineStatus(String sourceInstanceId);
    
    // ==================== 事件通知 ====================
    
    /**
     * 订阅用户状态变更事件
     * 
     * @param organizationId 组织ID（可选，为null时订阅全平台事件）
     * @param callback 状态变更回调函数
     * @return 订阅ID，用于取消订阅
     */
    String subscribeStatusChangeEvents(String organizationId, 
                                     java.util.function.Consumer<UserStatusResponse.StatusChangeEvent> callback);
    
    /**
     * 取消状态变更事件订阅
     * 
     * @param subscriptionId 订阅ID
     * @return 是否成功取消订阅
     */
    boolean unsubscribeStatusChangeEvents(String subscriptionId);
    
    /**
     * 广播用户状态变更事件
     * 
     * 内部方法，用户状态发生变更时自动调用。
     * 
     * @param userId 用户ID
     * @param oldStatus 旧状态
     * @param newStatus 新状态
     * @param changeReason 变更原因
     */
    void broadcastStatusChangeEvent(String userId, UserStatusResponse oldStatus, 
                                  UserStatusResponse newStatus, String changeReason);
    
    // ==================== 配置管理 ====================
    
    /**
     * 获取在线状态管理配置
     * 
     * @return 配置信息
     */
    Map<String, Object> getOnlineStatusConfig();
    
    /**
     * 更新在线状态管理配置
     * 
     * @param config 新的配置信息
     * @return 是否更新成功
     */
    boolean updateOnlineStatusConfig(Map<String, Object> config);
}