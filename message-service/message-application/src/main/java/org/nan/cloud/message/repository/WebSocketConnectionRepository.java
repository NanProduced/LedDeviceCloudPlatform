package org.nan.cloud.message.repository;

import org.nan.cloud.message.api.dto.websocket.WebSocketMessage;

/**
 * WebSocket连接仓储接口
 * 
 * 这是application层定义的仓储接口，遵循DDD架构原则。
 * 定义了WebSocket连接管理的抽象操作，具体实现在infrastructure层。
 * 
 * 架构说明：
 * - application层定义接口（这个文件）
 * - infrastructure层实现接口（WebSocketConnectionRepositoryImpl）
 * - 通过依赖倒置原则，application层不依赖infrastructure层
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface WebSocketConnectionRepository {
    
    /**
     * 向指定用户发送消息
     * 
     * @param userId 目标用户ID
     * @param message 要发送的消息
     * @return 发送成功的连接数量
     */
    int sendMessageToUser(String userId, WebSocketMessage message);
    
    /**
     * 向组织内所有用户广播消息
     * 
     * @param organizationId 目标组织ID
     * @param message 要广播的消息
     * @return 发送成功的连接数量
     */
    int broadcastToOrganization(String organizationId, WebSocketMessage message);
    
    /**
     * 向所有在线用户广播消息
     * 
     * @param message 要广播的消息
     * @return 发送成功的连接数量
     */
    int broadcastToAll(WebSocketMessage message);
    
    /**
     * 检查用户是否在线
     * 
     * @param userId 用户ID
     * @return true表示用户在线，false表示用户离线
     */
    boolean isUserOnline(String userId);
    
    /**
     * 获取用户的连接数量
     * 
     * @param userId 用户ID
     * @return 用户的连接数量
     */
    int getUserConnectionCount(String userId);
    
    /**
     * 获取总在线用户数
     * 
     * @return 当前在线用户数
     */
    int getOnlineUserCount();
    
    /**
     * 获取总连接数
     * 
     * @return 当前总连接数
     */
    int getTotalConnectionCount();
}