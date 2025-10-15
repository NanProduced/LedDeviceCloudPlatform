package org.nan.cloud.terminal.infrastructure.connection;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 连接管理器接口
 * 
 * 定义WebSocket连接管理的核心抽象，支持单机和集群模式
 * 基于组织ID(oid)进行分片优化，提升批量操作性能
 * 
 * @author terminal-service
 * @since 1.0.0
 */
public interface ConnectionManager {

    /**
     * 添加WebSocket连接
     * 
     * @param tid 设备ID
     * @param session WebSocket会话
     * @return 是否添加成功
     */
    boolean addConnection(Long tid, Object session);

    /**
     * 移除WebSocket连接
     * 
     * @param tid 设备ID
     * @return 被移除的会话，如果不存在返回null
     */
    Object removeConnection(Long tid);

    /**
     * 移除WebSocket连接（指定会话ID）
     * 
     * @param tid 设备ID
     * @param sessionId 会话ID
     * @return 被移除的会话，如果不存在返回null
     */
    Object removeConnection(Long tid, String sessionId);

    /**
     * 获取WebSocket连接
     * 
     * @param tid 设备ID
     * @return WebSocket会话的Optional包装
     */
    Optional<Object> getConnection(Long tid);

    /**
     * 检查设备是否在线
     * 
     * @param tid 设备ID
     * @return 是否在线
     */
    boolean isOnline(Long tid);

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
    Collection<Long> getOnlineDeviceIds();

    /**
     * 发送消息到指定设备
     *
     * @param oid 组织ID
     * @param tid 设备ID
     * @param message 消息内容
     * @return 是否发送成功
     */
    boolean sendMessage(Long oid, Long tid, String message);

    /**
     * 发送消息到指定设备列表（核心业务场景）
     * 
     * @param oid 组织ID
     * @param tidList 设备ID列表
     * @param message 消息内容
     * @return 发送成功的设备数量
     */
    int sendMessageToTerminals(Long oid, List<Long> tidList, String message);

    /**
     * 广播消息到指定组织的所有设备
     * 
     * @param oid 组织ID
     * @param message 消息内容
     * @return 发送成功的设备数量
     */
    int sendMessageToOrganization(Long oid, String message);

    /**
     * 广播消息到所有在线设备
     * 
     * @param message 消息内容
     * @return 发送成功的设备数量
     */
    int broadcastMessage(String message);

    /**
     * 获取组织的在线设备ID列表
     * 
     * @param oid 组织ID
     * @return 设备ID集合
     */
    Collection<Long> getOrganizationOnlineDevices(Long oid);

    /**
     * 获取组织的连接数
     * 
     * @param oid 组织ID
     * @return 连接数量
     */
    int getOrganizationConnectionCount(Long oid);

    /**
     * 更新设备最后活跃时间
     * 
     * @param tid 设备ID
     * @param lastActiveTime 最后活跃时间
     */
    void updateLastActiveTime(Long tid, LocalDateTime lastActiveTime);

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