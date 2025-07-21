package org.nan.cloud.message.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.api.dto.websocket.WebSocketMessage;
import org.nan.cloud.message.infrastructure.websocket.manager.WebSocketConnectionManager;
import org.nan.cloud.message.repository.WebSocketConnectionRepository;
import org.springframework.stereotype.Repository;

/**
 * WebSocket连接仓储实现类
 * 
 * 这是infrastructure层的仓储实现，实现了application层定义的接口。
 * 遵循DDD架构的依赖倒置原则：
 * - application层定义接口（WebSocketConnectionRepository）
 * - infrastructure层实现接口（这个类）
 * - 通过Spring依赖注入，application层使用接口，不直接依赖infrastructure层
 * 
 * 这样做的好处：
 * 1. 保持架构分层清晰
 * 2. application层可以独立测试
 * 3. 可以轻松替换底层实现
 * 4. 符合六边形架构思想
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class WebSocketConnectionRepositoryImpl implements WebSocketConnectionRepository {
    
    /**
     * WebSocket连接管理器
     * 这是infrastructure层的具体实现组件
     */
    private final WebSocketConnectionManager connectionManager;
    
    /**
     * 向指定用户发送消息
     * 
     * @param userId 目标用户ID
     * @param message 要发送的消息
     * @return 发送成功的连接数量
     */
    @Override
    public int sendMessageToUser(String userId, WebSocketMessage message) {
        log.debug("仓储层：向用户 {} 发送消息", userId);
        return connectionManager.sendMessageToUser(userId, message);
    }
    
    /**
     * 向组织内所有用户广播消息
     * 
     * @param organizationId 目标组织ID
     * @param message 要广播的消息
     * @return 发送成功的连接数量
     */
    @Override
    public int broadcastToOrganization(String organizationId, WebSocketMessage message) {
        log.debug("仓储层：向组织 {} 广播消息", organizationId);
        return connectionManager.broadcastToOrganization(organizationId, message);
    }
    
    /**
     * 向所有在线用户广播消息
     * 
     * @param message 要广播的消息
     * @return 发送成功的连接数量
     */
    @Override
    public int broadcastToAll(WebSocketMessage message) {
        log.debug("仓储层：向所有用户广播消息");
        return connectionManager.broadcastToAll(message);
    }
    
    /**
     * 检查用户是否在线
     * 
     * @param userId 用户ID
     * @return true表示用户在线，false表示用户离线
     */
    @Override
    public boolean isUserOnline(String userId) {
        return connectionManager.isUserOnline(userId);
    }
    
    /**
     * 获取用户的连接数量
     * 
     * @param userId 用户ID
     * @return 用户的连接数量
     */
    @Override
    public int getUserConnectionCount(String userId) {
        return connectionManager.getUserConnectionCount(userId);
    }
    
    /**
     * 获取总在线用户数
     * 
     * @return 当前在线用户数
     */
    @Override
    public int getOnlineUserCount() {
        return connectionManager.getOnlineUserCount();
    }
    
    /**
     * 获取总连接数
     * 
     * @return 当前总连接数
     */
    @Override
    public int getTotalConnectionCount() {
        return connectionManager.getTotalConnectionCount();
    }
}