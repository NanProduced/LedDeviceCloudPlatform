package org.nan.cloud.message.service;

import org.nan.cloud.message.api.dto.websocket.WebSocketMessage;
import org.nan.cloud.message.api.enums.MessageType;

/**
 * 消息服务接口
 * 
 * 定义消息中心的核心业务接口，包括消息发送、广播、状态查询等功能。
 * 这个接口抽象了消息处理的业务逻辑，具体实现可以是WebSocket、邮件、短信等不同方式。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface MessageService {
    
    /**
     * 发送消息给指定用户
     * 
     * 向特定用户发送个人消息，如私信、个人通知等。
     * 如果用户不在线，消息会被持久化等待用户上线后推送。
     * 
     * @param userId 目标用户ID
     * @param message 要发送的消息
     * @return 发送结果：true表示发送成功，false表示发送失败
     */
    boolean sendMessageToUser(String userId, WebSocketMessage message);
    
    /**
     * 向组织内所有用户广播消息
     * 
     * 向指定组织内的所有在线用户发送广播消息，如组织公告、重要通知等。
     * 支持多租户隔离，确保消息只在对应组织内传播。
     * 
     * @param organizationId 目标组织ID
     * @param message 要广播的消息
     * @return 成功接收消息的用户数量
     */
    int broadcastToOrganization(String organizationId, WebSocketMessage message);
    
    /**
     * 向所有在线用户广播消息
     * 
     * 向平台内所有在线用户发送广播消息，通常用于系统级重要通知，
     * 如系统维护、紧急公告等。请谨慎使用此功能。
     * 
     * @param message 要广播的消息
     * @return 成功接收消息的用户数量
     */
    int broadcastToAll(WebSocketMessage message);
    
    /**
     * 发送系统通知
     * 
     * 快捷方法，用于发送系统级通知消息。
     * 系统通知通常优先级较高，需要用户关注。
     * 
     * @param userId 目标用户ID，如果为null则广播给所有用户
     * @param organizationId 目标组织ID
     * @param title 通知标题
     * @param content 通知内容
     * @return 发送结果
     */
    boolean sendSystemNotification(String userId, String organizationId, String title, String content);
    
    /**
     * 发送设备告警消息
     * 
     * 快捷方法，用于发送设备相关的告警消息。
     * 设备告警通常是高优先级消息，需要立即处理。
     * 
     * @param userId 目标用户ID，如果为null则广播给组织内所有用户
     * @param organizationId 组织ID
     * @param deviceId 设备ID
     * @param title 告警标题
     * @param content 告警内容
     * @return 发送结果
     */
    boolean sendDeviceAlert(String userId, String organizationId, String deviceId, String title, String content);
    
    /**
     * 检查用户是否在线
     * 
     * 查询指定用户当前的在线状态。
     * 可用于判断是否需要发送实时消息，或者选择合适的通知方式。
     * 
     * @param userId 用户ID
     * @return true表示用户在线，false表示用户离线
     */
    boolean isUserOnline(String userId);
    
    /**
     * 获取用户的连接数量
     * 
     * 获取指定用户当前的WebSocket连接数量。
     * 用户可能在多个设备上同时登录，每个设备对应一个连接。
     * 
     * @param userId 用户ID
     * @return 用户的连接数量
     */
    int getUserConnectionCount(String userId);
    
    /**
     * 获取组织内在线用户数量
     * 
     * 统计指定组织内当前在线的用户数量。
     * 可用于组织管理和统计分析。
     * 
     * @param organizationId 组织ID
     * @return 在线用户数量
     */
    int getOrganizationOnlineUserCount(String organizationId);
    
    /**
     * 获取平台总在线用户数
     * 
     * 统计整个平台当前在线的用户总数。
     * 用于系统监控和负载评估。
     * 
     * @return 总在线用户数
     */
    int getTotalOnlineUserCount();
    
    /**
     * 批量发送消息
     * 
     * 向多个用户批量发送相同的消息。
     * 相比逐个发送，批量发送可以提高性能和减少资源消耗。
     * 
     * @param userIds 目标用户ID列表
     * @param message 要发送的消息
     * @return 成功发送的用户数量
     */
    int batchSendMessage(java.util.List<String> userIds, WebSocketMessage message);
    
    /**
     * 根据条件发送消息
     * 
     * 根据指定条件（如角色、部门、权限等）筛选用户并发送消息。
     * 这是一个扩展功能，可以实现更灵活的消息推送策略。
     * 
     * @param organizationId 组织ID
     * @param messageType 消息类型
     * @param filterConditions 筛选条件（具体格式待定义）
     * @param message 要发送的消息
     * @return 成功发送的用户数量
     */
    int sendMessageByConditions(String organizationId, MessageType messageType, 
                               java.util.Map<String, Object> filterConditions, 
                               WebSocketMessage message);
}