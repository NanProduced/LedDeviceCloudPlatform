package org.nan.cloud.terminal.api.connection;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

/**
 * 连接管理器接口
 * 
 * 定义WebSocket连接管理的核心抽象，支持单机和集群模式
 * 
 * @author terminal-service
 * @since 1.0.0
 */
public interface ConnectionManager {

    /**
     * 添加WebSocket连接
     * 
     * @param deviceId 设备ID
     * @param session WebSocket会话
     * @return 是否添加成功
     */
    boolean addConnection(String deviceId, Object session);

    /**
     * 移除WebSocket连接
     * 
     * @param deviceId 设备ID
     * @return 被移除的会话，如果不存在返回null
     */
    Object removeConnection(String deviceId);

    /**
     * 获取WebSocket连接
     * 
     * @param deviceId 设备ID
     * @return WebSocket会话的Optional包装
     */
    Optional<Object> getConnection(String deviceId);

    /**
     * 检查设备是否在线
     * 
     * @param deviceId 设备ID
     * @return 是否在线
     */
    boolean isOnline(String deviceId);

    /**
     * 获取当前连接总数
     * 
     * @return 连接数量
     */
    int getConnectionCount();

    /**
     * 获取指定分片的连接数
     * 
     * @param shardIndex 分片索引
     * @return 该分片的连接数量
     */
    int getShardConnectionCount(int shardIndex);

    /**
     * 获取所有在线设备ID
     * 
     * @return 设备ID集合
     */
    Collection<String> getOnlineDeviceIds();

    /**
     * 发送消息到指定设备
     * 
     * @param deviceId 设备ID
     * @param message 消息内容
     * @return 是否发送成功
     */
    boolean sendMessage(String deviceId, String message);

    /**
     * 广播消息到所有在线设备
     * 
     * @param message 消息内容
     * @return 发送成功的设备数量
     */
    int broadcastMessage(String message);

    /**
     * 广播消息到指定组织的设备
     * 
     * @param organizationId 组织ID
     * @param message 消息内容
     * @return 发送成功的设备数量
     */
    int broadcastToOrganization(String organizationId, String message);

    /**
     * 更新设备最后活跃时间
     * 
     * @param deviceId 设备ID
     * @param lastActiveTime 最后活跃时间
     */
    void updateLastActiveTime(String deviceId, LocalDateTime lastActiveTime);

    /**
     * 清理过期连接
     * 
     * @param expireThreshold 过期时间阈值
     * @return 清理的连接数量
     */
    int cleanupExpiredConnections(LocalDateTime expireThreshold);

    /**
     * 获取连接管理器统计信息
     * 
     * @return 统计信息
     */
    ConnectionStats getConnectionStats();

    /**
     * 连接统计信息
     */
    interface ConnectionStats {
        /**
         * 获取总连接数
         */
        int getTotalConnections();

        /**
         * 获取各分片连接数
         */
        int[] getShardConnections();

        /**
         * 获取负载均衡因子
         */
        double getLoadBalanceFactor();

        /**
         * 获取平均响应时间(毫秒)
         */
        double getAverageResponseTime();

        /**
         * 获取最大锁等待时间(毫秒)
         */
        long getMaxLockWaitTime();
    }
}