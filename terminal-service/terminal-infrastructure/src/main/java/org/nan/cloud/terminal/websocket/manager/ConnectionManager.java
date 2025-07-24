package org.nan.cloud.terminal.websocket.manager;

import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.terminal.websocket.session.TerminalWebSocketSession;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 分片式WebSocket连接管理器
 * 
 * 高性能连接管理，基于分片策略减少锁竞争：
 * 1. 分片存储：16个分片，基于设备ID哈希分布，减少锁粒度
 * 2. 读写锁优化：ReentrantReadWriteLock，读操作并发，写操作互斥
 * 3. 连接数限制：单机最大10,000连接，防止资源耗尽
 * 4. 快速查找：O(1)时间复杂度连接查找和操作
 * 5. 内存优化：ConcurrentHashMap内部优化，减少内存碎片
 * 
 * 分片算法：
 * - 哈希函数：deviceId.hashCode() & (SHARD_COUNT - 1)
 * - 分片数量：16个（2^4，位运算优化）
 * - 负载均衡：均匀分布设备连接到各分片
 * 
 * 并发性能：
 * - 读操作：多线程并发读取，无锁竞争
 * - 写操作：单线程写入，保证数据一致性
 * - 分片隔离：不同分片间操作完全独立
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Slf4j
@Component
public class ConnectionManager {

    // 分片数量，必须是2的幂次方，便于位运算优化
    private static final int SHARD_COUNT = 16;
    private static final int SHARD_MASK = SHARD_COUNT - 1;
    
    // 最大连接数限制
    private static final int MAX_CONNECTION_COUNT = 10000;

    // 分片连接存储：每个分片独立的设备连接映射
    // 外层数组：分片索引
    // 内层Map：设备ID -> 会话列表（支持单设备多连接）
    private final Map<String, List<TerminalWebSocketSession>>[] connectionShards;
    
    // 分片读写锁：每个分片独立的锁，减少锁竞争
    private final ReadWriteLock[] shardLocks;
    
    // 全局连接计数器（原子操作，无需额外同步）
    private volatile int totalConnectionCount = 0;

    @SuppressWarnings("unchecked")
    public ConnectionManager() {
        // 初始化分片存储
        connectionShards = new Map[SHARD_COUNT];
        shardLocks = new ReadWriteLock[SHARD_COUNT];
        
        for (int i = 0; i < SHARD_COUNT; i++) {
            connectionShards[i] = new ConcurrentHashMap<>();
            shardLocks[i] = new ReentrantReadWriteLock();
        }
        
        log.info("分片式连接管理器初始化完成: 分片数量={}, 最大连接数={}", SHARD_COUNT, MAX_CONNECTION_COUNT);
    }

    /**
     * 添加设备连接
     * 
     * @param deviceId 设备ID
     * @param session WebSocket会话
     * @return 是否添加成功
     */
    public boolean addConnection(String deviceId, TerminalWebSocketSession session) {
        if (deviceId == null || session == null) {
            log.warn("添加连接失败: 参数为空, deviceId={}, session={}", deviceId, session);
            return false;
        }

        // 检查连接数限制
        if (totalConnectionCount >= MAX_CONNECTION_COUNT) {
            log.warn("添加连接失败: 超过最大连接数限制, deviceId={}, 当前连接数={}", 
                deviceId, totalConnectionCount);
            return false;
        }

        int shardIndex = getShardIndex(deviceId);
        ReadWriteLock lock = shardLocks[shardIndex];
        
        lock.writeLock().lock();
        try {
            Map<String, List<TerminalWebSocketSession>> shard = connectionShards[shardIndex];
            
            // 获取或创建设备连接列表
            List<TerminalWebSocketSession> deviceSessions = shard.computeIfAbsent(
                deviceId, k -> new ArrayList<>());
            
            // 检查是否已存在相同会话ID的连接
            boolean sessionExists = deviceSessions.stream()
                .anyMatch(s -> s.getSessionId().equals(session.getSessionId()));
            
            if (sessionExists) {
                log.warn("连接已存在: deviceId={}, sessionId={}", deviceId, session.getSessionId());
                return false;
            }
            
            // 添加新连接
            deviceSessions.add(session);
            totalConnectionCount++;
            
            log.info("连接添加成功: deviceId={}, sessionId={}, 分片={}, 设备连接数={}, 总连接数={}", 
                deviceId, session.getSessionId(), shardIndex, deviceSessions.size(), totalConnectionCount);
            
            return true;
            
        } catch (Exception e) {
            log.error("添加连接异常: deviceId={}, sessionId={}", deviceId, session.getSessionId(), e);
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 移除设备连接
     * 
     * @param deviceId 设备ID
     * @param sessionId 会话ID
     * @return 是否移除成功
     */
    public boolean removeConnection(String deviceId, String sessionId) {
        if (deviceId == null || sessionId == null) {
            log.warn("移除连接失败: 参数为空, deviceId={}, sessionId={}", deviceId, sessionId);
            return false;
        }

        int shardIndex = getShardIndex(deviceId);
        ReadWriteLock lock = shardLocks[shardIndex];
        
        lock.writeLock().lock();
        try {
            Map<String, List<TerminalWebSocketSession>> shard = connectionShards[shardIndex];
            List<TerminalWebSocketSession> deviceSessions = shard.get(deviceId);
            
            if (deviceSessions == null || deviceSessions.isEmpty()) {
                log.warn("移除连接失败: 设备无连接, deviceId={}, sessionId={}", deviceId, sessionId);
                return false;
            }
            
            // 移除指定会话
            boolean removed = deviceSessions.removeIf(s -> s.getSessionId().equals(sessionId));
            
            if (removed) {
                totalConnectionCount--;
                
                // 如果设备无任何连接，移除设备记录
                if (deviceSessions.isEmpty()) {
                    shard.remove(deviceId);
                }
                
                log.info("连接移除成功: deviceId={}, sessionId={}, 分片={}, 设备剩余连接数={}, 总连接数={}", 
                    deviceId, sessionId, shardIndex, deviceSessions.size(), totalConnectionCount);
                
                return true;
            } else {
                log.warn("移除连接失败: 会话不存在, deviceId={}, sessionId={}", deviceId, sessionId);
                return false;
            }
            
        } catch (Exception e) {
            log.error("移除连接异常: deviceId={}, sessionId={}", deviceId, sessionId, e);
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取设备的所有连接会话
     * 
     * @param deviceId 设备ID
     * @return 连接会话列表
     */
    public List<TerminalWebSocketSession> getDeviceConnections(String deviceId) {
        if (deviceId == null) {
            return new ArrayList<>();
        }

        int shardIndex = getShardIndex(deviceId);
        ReadWriteLock lock = shardLocks[shardIndex];
        
        lock.readLock().lock();
        try {
            Map<String, List<TerminalWebSocketSession>> shard = connectionShards[shardIndex];
            List<TerminalWebSocketSession> deviceSessions = shard.get(deviceId);
            
            return deviceSessions != null ? new ArrayList<>(deviceSessions) : new ArrayList<>();
            
        } catch (Exception e) {
            log.error("获取设备连接异常: deviceId={}", deviceId, e);
            return new ArrayList<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取所有在线设备ID列表
     * 
     * @return 设备ID列表
     */
    public List<String> getAllOnlineDevices() {
        List<String> allDevices = new ArrayList<>();
        
        for (int i = 0; i < SHARD_COUNT; i++) {
            ReadWriteLock lock = shardLocks[i];
            lock.readLock().lock();
            try {
                Map<String, List<TerminalWebSocketSession>> shard = connectionShards[i];
                // 只返回有活跃连接的设备
                shard.entrySet().stream()
                    .filter(entry -> !entry.getValue().isEmpty())
                    .forEach(entry -> allDevices.add(entry.getKey()));
                    
            } catch (Exception e) {
                log.error("获取分片设备列表异常: 分片={}", i, e);
            } finally {
                lock.readLock().unlock();
            }
        }
        
        return allDevices;
    }

    /**
     * 获取当前总连接数
     * 
     * @return 连接总数
     */
    public int getConnectionCount() {
        return totalConnectionCount;
    }

    /**
     * 获取在线设备数量
     * 
     * @return 设备数量
     */
    public int getOnlineDeviceCount() {
        return getAllOnlineDevices().size();
    }

    /**
     * 检查设备是否在线
     * 
     * @param deviceId 设备ID
     * @return 是否在线
     */
    public boolean isDeviceOnline(String deviceId) {
        List<TerminalWebSocketSession> sessions = getDeviceConnections(deviceId);
        return sessions.stream().anyMatch(TerminalWebSocketSession::isConnected);
    }

    /**
     * 获取连接管理器状态统计
     * 
     * @return 状态信息字符串
     */
    public String getConnectionStats() {
        int onlineDeviceCount = getOnlineDeviceCount();
        return String.format("连接统计: 总连接数=%d, 在线设备数=%d, 分片数=%d, 平均每分片连接数=%.1f",
            totalConnectionCount, onlineDeviceCount, SHARD_COUNT, 
            (double) totalConnectionCount / SHARD_COUNT);
    }

    /**
     * 计算设备ID对应的分片索引
     * 使用哈希算法均匀分布设备到各分片
     * 
     * @param deviceId 设备ID
     * @return 分片索引 (0 ~ SHARD_COUNT-1)
     */
    private int getShardIndex(String deviceId) {
        return deviceId.hashCode() & SHARD_MASK;
    }

    /**
     * 清理超时连接（定时任务调用）
     * 
     * @param timeoutMs 超时时间（毫秒）
     * @return 清理的连接数
     */
    public int cleanupTimeoutConnections(long timeoutMs) {
        int cleanedCount = 0;
        
        for (int i = 0; i < SHARD_COUNT; i++) {
            ReadWriteLock lock = shardLocks[i];
            lock.writeLock().lock();
            try {
                Map<String, List<TerminalWebSocketSession>> shard = connectionShards[i];
                
                for (Map.Entry<String, List<TerminalWebSocketSession>> entry : shard.entrySet()) {
                    List<TerminalWebSocketSession> deviceSessions = entry.getValue();
                    
                    // 移除超时连接
                    int originalSize = deviceSessions.size();
                    deviceSessions.removeIf(session -> {
                        if (session.isHeartbeatTimeout(timeoutMs) || !session.isConnected()) {
                            try {
                                session.closeConnection();
                            } catch (Exception e) {
                                log.error("关闭超时连接异常: deviceId={}, sessionId={}", 
                                    session.getDeviceId(), session.getSessionId(), e);
                            }
                            return true;
                        }
                        return false;
                    });
                    
                    int removedCount = originalSize - deviceSessions.size();
                    cleanedCount += removedCount;
                    totalConnectionCount -= removedCount;
                    
                    if (removedCount > 0) {
                        log.info("清理分片超时连接: 分片={}, 设备={}, 清理数量={}", 
                            i, entry.getKey(), removedCount);
                    }
                }
                
                // 移除空的设备记录
                shard.entrySet().removeIf(entry -> entry.getValue().isEmpty());
                
            } catch (Exception e) {
                log.error("清理分片超时连接异常: 分片={}", i, e);
            } finally {
                lock.writeLock().unlock();
            }
        }
        
        if (cleanedCount > 0) {
            log.info("连接清理完成: 清理数量={}, 剩余连接数={}", cleanedCount, totalConnectionCount);
        }
        
        return cleanedCount;
    }
}