package org.nan.cloud.terminal.infrastructure.connection;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.terminal.cache.TerminalOnlineStatusManager;
import org.nan.cloud.terminal.cache.WebsocketConnectionCacheHandler;
import org.nan.cloud.terminal.websocket.session.TerminalWebSocketSession;
import org.springframework.stereotype.Component;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import jakarta.annotation.PreDestroy;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 基于组织ID(oid)的分片式连接管理器
 * 
 * 核心设计思想：
 * 1. 16个分片基于组织ID(oid)哈希分布，使用斐波那契哈希增强分散性
 * 2. 每个分片内按组织维度管理连接，优化批量操作性能  
 * 3. 支持核心业务场景：终端列表消息发送、组织广播等
 * 4. 总计支持10000个连接，每分片最大625个连接
 * 
 * 性能优势：
 * - 终端列表发送：O(1)定位组织 + O(k)发送，k为目标终端数
 * - 组织广播：O(1)定位组织 + O(m)发送，m为组织内终端数
 * - 单终端操作：O(1)直接访问
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Slf4j
@Component
public class ShardedConnectionManager implements ConnectionManager {

    /**
     * 分片数量 - 16个分片
     */
    private static final int SHARD_COUNT = 16;
    
    /**
     * 每个分片的最大连接数 - 625个连接
     */
    private static final int MAX_CONNECTIONS_PER_SHARD = 625;
    
    /**
     * 总的最大连接数 - 10000个连接
     */
    private static final int MAX_TOTAL_CONNECTIONS = SHARD_COUNT * MAX_CONNECTIONS_PER_SHARD;
    
    /**
     * 斐波那契哈希常数 (黄金比例 * 2^32)
     * 用于增强自增ID的分散性
     */
    private static final long FIBONACCI_HASH = 0x9E3779B9L;

    /**
     * 连接分片数组 - 每个分片独立管理连接
     */
    private final OrganizationShard[] shards;
    
    /**
     * 总连接数计数器 - 原子操作保证线程安全
     */
    private final AtomicInteger totalConnections = new AtomicInteger(0);
    
    /**
     * 消息发送统计计数器
     */
    private final AtomicLong totalMessagesSent = new AtomicLong(0);
    
    /**
     * 响应时间统计（用于计算平均值）
     */
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    private final AtomicLong responseTimeCount = new AtomicLong(0);

    private final TerminalOnlineStatusManager terminalOnlineStatusManager;
    private final WebsocketConnectionCacheHandler websocketConnectionCacheHandler;

    public ShardedConnectionManager(TerminalOnlineStatusManager terminalOnlineStatusManager, WebsocketConnectionCacheHandler websocketConnectionCacheHandler) {
        // 初始化16个组织分片
        this.shards = new OrganizationShard[SHARD_COUNT];
        for (int i = 0; i < SHARD_COUNT; i++) {
            this.shards[i] = new OrganizationShard(i, MAX_CONNECTIONS_PER_SHARD);
        }
        
        log.info("基于组织ID的分片式连接管理器初始化完成: 分片数={}, 每分片最大连接数={}, 总最大连接数={}", 
            SHARD_COUNT, MAX_CONNECTIONS_PER_SHARD, MAX_TOTAL_CONNECTIONS);

        this.terminalOnlineStatusManager = terminalOnlineStatusManager;
        this.websocketConnectionCacheHandler = websocketConnectionCacheHandler;
    }

    /**
     * 增强哈希算法 - 解决自增ID分散性问题
     * 使用斐波那契哈希 + 位运算优化
     */
    private int getShardIndex(Long oid) {
        if (oid == null) return 0;
        
        // 增强哈希：oid * 斐波那契常数，然后取高位
        long hash = oid * FIBONACCI_HASH;
        
        // 取高位的方式获得更好的分散性
        return (int) ((hash ^ (hash >>> 16)) & (SHARD_COUNT - 1));
    }

    @Override
    public boolean addConnection(Long tid, Object session) {
        if (tid == null || session == null) {
            log.warn("添加连接失败：设备ID或会话为空");
            return false;
        }

        TerminalWebSocketSession terminalSession = (TerminalWebSocketSession) session;
        Long oid = terminalSession.getOid();
        
        if (oid == null) {
            log.warn("添加连接失败：组织ID为空, tid={}", tid);
            return false;
        }

        // 检查总连接数限制
        if (totalConnections.get() >= MAX_TOTAL_CONNECTIONS) {
            log.warn("添加连接失败：已达到最大连接数限制 {}", MAX_TOTAL_CONNECTIONS);
            return false;
        }

        // 根据组织ID计算分片索引
        int shardIndex = getShardIndex(oid);
        OrganizationShard shard = shards[shardIndex];
        
        // 记录操作开始时间（用于性能统计）
        long startTime = System.currentTimeMillis();
        
        try {
            // 创建设备连接对象
            String clientIp = extractClientIp(session);
            TerminalConnection connection = TerminalConnection.create(tid, oid, session, clientIp);
            
            // 添加到对应分片
            boolean added = shard.addConnection(oid, tid, connection);
            if (added) {
                // 缓存tid和oid的映射关系
                websocketConnectionCacheHandler.setOidToTidMap(oid, tid);
                // websocket连接成功标注终端活跃（上线）
                terminalOnlineStatusManager.updateTerminalActivity(oid, tid);

                totalConnections.incrementAndGet();
                log.info("设备连接添加成功: tid={}, oid={}, shardIndex={}, 当前总连接数={}",
                        tid, oid, shardIndex, totalConnections.get());
                return true;
            } else {
                log.warn("设备连接添加失败: tid={}, oid={}, shardIndex={}", tid, oid, shardIndex);
                return false;
            }
        } finally {
            // 记录响应时间统计
            long responseTime = System.currentTimeMillis() - startTime;
            totalResponseTime.addAndGet(responseTime);
            responseTimeCount.incrementAndGet();
        }
    }

    @Override
    public Object removeConnection(Long tid) {
        if (tid == null) {
            return null;
        }

        Long oid = websocketConnectionCacheHandler.getOidByTid(tid);
        int shardIndex = getShardIndex(oid);
        OrganizationShard shard = shards[shardIndex];
        TerminalConnection connection = shard.removeConnectionByTid(tid);
        if (connection != null) {
            // 标记终端离线
            terminalOnlineStatusManager.markTerminalOffline(oid, tid);

            totalConnections.decrementAndGet();
            log.info("设备连接移除成功: tid={}, shardIndex={}, 当前总连接数={}",
                    tid, shardIndex, totalConnections.get());
            return connection.getSession();
        }
        return null;
    }

    @Override
    public Object removeConnection(Long tid, String sessionId) {
        if (tid == null) {
            return null;
        }

        Long oid = websocketConnectionCacheHandler.getOidByTid(tid);
        int shardIndex = getShardIndex(oid);
        OrganizationShard shard = shards[shardIndex];
        TerminalConnection connection = shard.removeConnectionByTidAndSession(tid, sessionId);

        if (connection != null) {
            // 标记终端离线
            terminalOnlineStatusManager.markTerminalOffline(oid, tid);

            totalConnections.decrementAndGet();
            log.info("设备连接移除成功: tid={}, sessionId={}, shardIndex={}, 当前总连接数={}",
                    tid, sessionId, shardIndex, totalConnections.get());
            return connection.getSession();
        }

        return null;

    }

    @Override
    public Optional<Object> getConnection(Long tid) {
        if (tid == null) {
            return Optional.empty();
        }

        // 遍历所有分片查找tid
        for (OrganizationShard shard : shards) {
            TerminalConnection connection = shard.getConnectionByTid(tid);
            if (connection != null) {
                return Optional.of(connection.getSession());
            }
        }
        
        return Optional.empty();
    }

    @Override
    public boolean isOnline(Long tid) {
        if (tid == null) {
            return false;
        }

        // 遍历所有分片查找tid
        for (OrganizationShard shard : shards) {
            if (shard.isTerminalOnline(tid)) {
                return true;
            }
        }
        
        return false;
    }

    @Override
    public int getConnectionCount() {
        return totalConnections.get();
    }

    @Override
    public int getShardConnectionCount(int shardIndex) {
        if (shardIndex < 0 || shardIndex >= SHARD_COUNT) {
            throw new IllegalArgumentException("分片索引超出范围: " + shardIndex);
        }
        return shards[shardIndex].getConnectionCount();
    }

    @Override
    public Collection<Long> getOnlineDeviceIds() {
        Set<Long> allDeviceIds = new HashSet<>();
        
        // 并行收集所有分片的设备ID
        for (OrganizationShard shard : shards) {
            allDeviceIds.addAll(shard.getOnlineTerminalIds());
        }
        
        return allDeviceIds;
    }

    @Override
    public boolean sendMessage(Long tid, String message) {
        if (tid == null || message == null) {
            return false;
        }

        // 遍历所有分片查找tid并发送消息
        for (OrganizationShard shard : shards) {
            if (shard.sendMessageToTerminal(tid, message)) {
                totalMessagesSent.incrementAndGet();
                return true;
            }
        }
        
        return false;
    }

    @Override
    public int sendMessageToTerminals(Long oid, List<Long> tidList, String message) {
        if (oid == null || tidList == null || tidList.isEmpty() || message == null) {
            return 0;
        }

        // O(1) 定位到组织分片
        int shardIndex = getShardIndex(oid);
        OrganizationShard shard = shards[shardIndex];
        
        // O(k) 发送消息，k为目标终端数量
        int sent = shard.sendMessageToTerminalList(oid, tidList, message);
        totalMessagesSent.addAndGet(sent);
        
        log.info("终端列表消息发送完成: oid={}, 目标终端数={}, 成功发送数={}", oid, tidList.size(), sent);
        return sent;
    }

    @Override
    public int sendMessageToOrganization(Long oid, String message) {
        if (oid == null || message == null) {
            return 0;
        }

        // O(1) 定位到组织分片
        int shardIndex = getShardIndex(oid);
        OrganizationShard shard = shards[shardIndex];
        
        // O(m) 发送消息，m为组织内终端数
        int sent = shard.broadcastToOrganization(oid, message);
        totalMessagesSent.addAndGet(sent);
        
        log.info("组织广播消息完成: oid={}, 成功发送数={}", oid, sent);
        return sent;
    }

    @Override
    public int broadcastMessage(String message) {
        if (message == null) {
            return 0;
        }

        int totalSent = 0;
        
        // 并行广播到所有分片
        for (OrganizationShard shard : shards) {
            totalSent += shard.broadcastMessage(message);
        }
        
        totalMessagesSent.addAndGet(totalSent);
        log.info("全局广播消息完成: 成功发送数={}, 消息长度={}", totalSent, message.length());
        
        return totalSent;
    }

    @Override
    public Collection<Long> getOrganizationOnlineDevices(Long oid) {
        if (oid == null) {
            return Collections.emptyList();
        }

        int shardIndex = getShardIndex(oid);
        return shards[shardIndex].getOrganizationOnlineTerminals(oid);
    }

    @Override
    public int getOrganizationConnectionCount(Long oid) {
        if (oid == null) {
            return 0;
        }

        int shardIndex = getShardIndex(oid);
        return shards[shardIndex].getOrganizationConnectionCount(oid);
    }

    @Override
    public void updateLastActiveTime(Long tid, LocalDateTime lastActiveTime) {
        if (tid == null) {
            return;
        }

        // 遍历所有分片查找tid并更新活跃时间
        for (OrganizationShard shard : shards) {
            if (shard.updateTerminalActiveTime(tid, lastActiveTime)) {
                return; // 找到并更新成功，直接返回
            }
        }
    }

    @Override
    public int cleanupExpiredConnections(LocalDateTime expireThreshold) {
        int totalCleaned = 0;
        
        // 并行清理所有分片的过期连接
        for (OrganizationShard shard : shards) {
            int cleaned = shard.cleanupExpiredConnections(expireThreshold);
            totalCleaned += cleaned;
        }
        
        // 更新总连接数
        totalConnections.addAndGet(-totalCleaned);
        
        if (totalCleaned > 0) {
            log.info("清理过期连接完成: 清理数量={}, 当前总连接数={}", totalCleaned, totalConnections.get());
        }
        
        return totalCleaned;
    }

    @Override
    public ConnectionStats getConnectionStats() {
        return new ConnectionStatsImpl();
    }

    /**
     * 从会话对象中提取客户端IP地址
     */
    private String extractClientIp(Object session) {
        if (session instanceof TerminalWebSocketSession) {
            TerminalWebSocketSession terminalSession = (TerminalWebSocketSession) session;
            return terminalSession.getClientIp();
        }
        return "unknown";
    }

    /**
     * 服务关闭时的清理操作
     */
    @PreDestroy
    public void shutdown() {
        log.info("基于组织ID的分片式连接管理器开始关闭...");
        
        int totalClosed = 0;
        for (OrganizationShard shard : shards) {
            totalClosed += shard.shutdown();
        }
        
        log.info("分片式连接管理器关闭完成: 关闭连接数={}", totalClosed);
    }

    /**
     * 连接统计信息实现类
     */
    private class ConnectionStatsImpl implements ConnectionStats {
        
        @Override
        public int getTotalConnections() {
            return totalConnections.get();
        }

        @Override
        public int[] getShardConnections() {
            int[] shardConnections = new int[SHARD_COUNT];
            for (int i = 0; i < SHARD_COUNT; i++) {
                shardConnections[i] = shards[i].getConnectionCount();
            }
            return shardConnections;
        }

        @Override
        public double getLoadBalanceFactor() {
            int[] shardConnections = getShardConnections();
            
            // 计算负载均衡因子 = 标准差 / 平均值
            double average = (double) getTotalConnections() / SHARD_COUNT;
            if (average == 0) {
                return 0.0;
            }
            
            double variance = 0.0;
            for (int count : shardConnections) {
                variance += Math.pow(count - average, 2);
            }
            variance /= SHARD_COUNT;
            
            double standardDeviation = Math.sqrt(variance);
            return standardDeviation / average;
        }

        @Override
        public double getAverageResponseTime() {
            long count = responseTimeCount.get();
            if (count == 0) {
                return 0.0;
            }
            return (double) totalResponseTime.get() / count;
        }

        @Override
        public long getMaxLockWaitTime() {
            long maxWaitTime = 0;
            for (OrganizationShard shard : shards) {
                maxWaitTime = Math.max(maxWaitTime, shard.getMaxLockWaitTime());
            }
            return maxWaitTime;
        }
    }

    /**
     * 组织分片类 - 管理单个分片内的所有组织连接
     */
    private static class OrganizationShard {
        private final int shardIndex;
        private final int maxConnections;
        // 组织维度的连接存储：oid -> 该组织的所有终端连接
        private final ConcurrentHashMap<Long, OrganizationConnections> organizations;
        private final ReentrantReadWriteLock lock;
        private final AtomicLong maxLockWaitTime = new AtomicLong(0);

        public OrganizationShard(int shardIndex, int maxConnections) {
            this.shardIndex = shardIndex;
            this.maxConnections = maxConnections;
            // 预分配容量，避免动态扩容
            this.organizations = new ConcurrentHashMap<>(32); // 假设每个分片有32个组织
            this.lock = new ReentrantReadWriteLock();
        }

        public boolean addConnection(Long oid, Long tid, TerminalConnection connection) {
            long startTime = System.currentTimeMillis();
            
            lock.writeLock().lock();
            try {
                // 获取或创建组织连接容器
                OrganizationConnections orgConnections = organizations.computeIfAbsent(oid, 
                    k -> new OrganizationConnections(oid));
                
                // 检查分片连接数限制
                int currentCount = getCurrentConnectionCount();
                if (currentCount >= maxConnections) {
                    log.warn("分片 {} 已达到最大连接数限制: {}", shardIndex, maxConnections);
                    return false;
                }
                
                return orgConnections.addTerminal(tid, connection);
            } finally {
                lock.writeLock().unlock();
                updateMaxLockWaitTime(startTime);
            }
        }

        public TerminalConnection removeConnectionByTid(Long tid) {
            long startTime = System.currentTimeMillis();
            
            lock.writeLock().lock();
            try {
                for (OrganizationConnections orgConnections : organizations.values()) {
                    TerminalConnection connection = orgConnections.removeTerminal(tid);
                    if (connection != null) {
                        // 如果组织内没有连接了，移除组织记录
                        if (orgConnections.isEmpty()) {
                            organizations.remove(orgConnections.getOid());
                        }
                        return connection;
                    }
                }
                return null;
            } finally {
                lock.writeLock().unlock();
                updateMaxLockWaitTime(startTime);
            }
        }

        public TerminalConnection removeConnectionByTidAndSession(Long tid, String sessionId) {
            long startTime = System.currentTimeMillis();
            
            lock.writeLock().lock();
            try {
                for (OrganizationConnections orgConnections : organizations.values()) {
                    TerminalConnection connection = orgConnections.getTerminal(tid);
                    if (connection != null) {
                        // 验证sessionId是否匹配
                        Object session = connection.getSession();
                        if (session instanceof TerminalWebSocketSession) {
                            TerminalWebSocketSession terminalSession = (TerminalWebSocketSession) session;
                            if (sessionId != null && !sessionId.equals(terminalSession.getSessionId())) {
                                continue; // sessionId不匹配，继续查找
                            }
                        }
                        
                        // 移除连接
                        TerminalConnection removed = orgConnections.removeTerminal(tid);
                        if (removed != null && orgConnections.isEmpty()) {
                            organizations.remove(orgConnections.getOid());
                        }
                        return removed;
                    }
                }
                return null;
            } finally {
                lock.writeLock().unlock();
                updateMaxLockWaitTime(startTime);
            }
        }

        public TerminalConnection getConnectionByTid(Long tid) {
            long startTime = System.currentTimeMillis();
            
            lock.readLock().lock();
            try {
                for (OrganizationConnections orgConnections : organizations.values()) {
                    TerminalConnection connection = orgConnections.getTerminal(tid);
                    if (connection != null) {
                        return connection;
                    }
                }
                return null;
            } finally {
                lock.readLock().unlock();
                updateMaxLockWaitTime(startTime);
            }
        }

        public boolean isTerminalOnline(Long tid) {
            lock.readLock().lock();
            try {
                for (OrganizationConnections orgConnections : organizations.values()) {
                    if (orgConnections.isTerminalOnline(tid)) {
                        return true;
                    }
                }
                return false;
            } finally {
                lock.readLock().unlock();
            }
        }

        public boolean sendMessageToTerminal(Long tid, String message) {
            lock.readLock().lock();
            try {
                for (OrganizationConnections orgConnections : organizations.values()) {
                    if (orgConnections.sendMessageToTerminal(tid, message)) {
                        return true;
                    }
                }
                return false;
            } finally {
                lock.readLock().unlock();
            }
        }

        /**
         * 发送消息到特定终端列表 - 核心业务场景
         */
        public int sendMessageToTerminalList(Long oid, List<Long> tidList, String message) {
            lock.readLock().lock();
            try {
                OrganizationConnections orgConnections = organizations.get(oid);
                if (orgConnections != null) {
                    return orgConnections.sendMessageToTerminalList(tidList, message);
                }
                return 0;
            } finally {
                lock.readLock().unlock();
            }
        }

        public int broadcastToOrganization(Long oid, String message) {
            lock.readLock().lock();
            try {
                OrganizationConnections orgConnections = organizations.get(oid);
                if (orgConnections != null) {
                    return orgConnections.broadcastMessage(message);
                }
                return 0;
            } finally {
                lock.readLock().unlock();
            }
        }

        public int broadcastMessage(String message) {
            lock.readLock().lock();
            try {
                int sent = 0;
                for (OrganizationConnections orgConnections : organizations.values()) {
                    sent += orgConnections.broadcastMessage(message);
                }
                return sent;
            } finally {
                lock.readLock().unlock();
            }
        }

        public Collection<Long> getOnlineTerminalIds() {
            lock.readLock().lock();
            try {
                Set<Long> terminalIds = new HashSet<>();
                for (OrganizationConnections orgConnections : organizations.values()) {
                    terminalIds.addAll(orgConnections.getOnlineTerminalIds());
                }
                return terminalIds;
            } finally {
                lock.readLock().unlock();
            }
        }

        public Collection<Long> getOrganizationOnlineTerminals(Long oid) {
            lock.readLock().lock();
            try {
                OrganizationConnections orgConnections = organizations.get(oid);
                if (orgConnections != null) {
                    return orgConnections.getOnlineTerminalIds();
                }
                return Collections.emptyList();
            } finally {
                lock.readLock().unlock();
            }
        }

        public int getOrganizationConnectionCount(Long oid) {
            lock.readLock().lock();
            try {
                OrganizationConnections orgConnections = organizations.get(oid);
                return orgConnections != null ? orgConnections.getConnectionCount() : 0;
            } finally {
                lock.readLock().unlock();
            }
        }

        public boolean updateTerminalActiveTime(Long tid, LocalDateTime lastActiveTime) {
            lock.readLock().lock();
            try {
                for (OrganizationConnections orgConnections : organizations.values()) {
                    if (orgConnections.updateTerminalActiveTime(tid, lastActiveTime)) {
                        return true;
                    }
                }
                return false;
            } finally {
                lock.readLock().unlock();
            }
        }

        public int cleanupExpiredConnections(LocalDateTime expireThreshold) {
            lock.writeLock().lock();
            try {
                int totalCleaned = 0;
                List<Long> emptyOrganizations = new ArrayList<>();
                
                for (Map.Entry<Long, OrganizationConnections> entry : organizations.entrySet()) {
                    OrganizationConnections orgConnections = entry.getValue();
                    int cleaned = orgConnections.cleanupExpiredConnections(expireThreshold);
                    totalCleaned += cleaned;
                    
                    // 记录空的组织，稍后移除
                    if (orgConnections.isEmpty()) {
                        emptyOrganizations.add(entry.getKey());
                    }
                }
                
                // 移除空的组织记录
                for (Long oid : emptyOrganizations) {
                    organizations.remove(oid);
                }
                
                return totalCleaned;
            } finally {
                lock.writeLock().unlock();
            }
        }

        public int getConnectionCount() {
            lock.readLock().lock();
            try {
                return getCurrentConnectionCount();
            } finally {
                lock.readLock().unlock();
            }
        }

        private int getCurrentConnectionCount() {
            int count = 0;
            for (OrganizationConnections orgConnections : organizations.values()) {
                count += orgConnections.getConnectionCount();
            }
            return count;
        }

        public int shutdown() {
            lock.writeLock().lock();
            try {
                int totalClosed = 0;
                
                // 关闭所有组织的连接
                for (OrganizationConnections orgConnections : organizations.values()) {
                    totalClosed += orgConnections.shutdown();
                }
                
                organizations.clear();
                return totalClosed;
            } finally {
                lock.writeLock().unlock();
            }
        }

        public long getMaxLockWaitTime() {
            return maxLockWaitTime.get();
        }

        private void updateMaxLockWaitTime(long startTime) {
            long waitTime = System.currentTimeMillis() - startTime;
            long currentMax = maxLockWaitTime.get();
            if (waitTime > currentMax) {
                maxLockWaitTime.compareAndSet(currentMax, waitTime);
            }
        }

        /**
         * 组织连接容器 - 管理单个组织内的所有终端连接
         */
        private static class OrganizationConnections {
            private final Long oid;
            // 该组织内的所有终端连接：tid -> TerminalConnection
            private final ConcurrentHashMap<Long, TerminalConnection> terminals;

            public OrganizationConnections(Long oid) {
                this.oid = oid;
                this.terminals = new ConcurrentHashMap<>();
            }

            public Long getOid() {
                return oid;
            }

            public boolean addTerminal(Long tid, TerminalConnection connection) {
                return terminals.put(tid, connection) == null;
            }

            public TerminalConnection removeTerminal(Long tid) {
                return terminals.remove(tid);
            }

            public TerminalConnection getTerminal(Long tid) {
                return terminals.get(tid);
            }

            public boolean isEmpty() {
                return terminals.isEmpty();
            }

            public int getConnectionCount() {
                return terminals.size();
            }

            public boolean isTerminalOnline(Long tid) {
                TerminalConnection connection = terminals.get(tid);
                return connection != null && 
                       connection.getStatus() == TerminalConnection.ConnectionStatus.CONNECTED;
            }

            public boolean sendMessageToTerminal(Long tid, String message) {
                TerminalConnection connection = terminals.get(tid);
                if (connection == null || 
                    connection.getStatus() != TerminalConnection.ConnectionStatus.CONNECTED) {
                    return false;
                }
                
                return sendMessageToConnection(connection, message);
            }

            /**
             * 发送消息到特定终端列表 - 主要业务场景
             */
            public int sendMessageToTerminalList(List<Long> tidList, String message) {
                int sent = 0;
                for (Long tid : tidList) {
                    TerminalConnection connection = terminals.get(tid);
                    if (connection != null && 
                        connection.getStatus() == TerminalConnection.ConnectionStatus.CONNECTED) {
                        if (sendMessageToConnection(connection, message)) {
                            sent++;
                        }
                    }
                }
                return sent;
            }

            /**
             * 广播到整个组织
             */
            public int broadcastMessage(String message) {
                int sent = 0;
                for (TerminalConnection connection : terminals.values()) {
                    if (connection.getStatus() == TerminalConnection.ConnectionStatus.CONNECTED) {
                        if (sendMessageToConnection(connection, message)) {
                            sent++;
                        }
                    }
                }
                return sent;
            }

            private boolean sendMessageToConnection(TerminalConnection connection, String message) {
                try {
                    Object wsSession = connection.getWebSocketSession();
                    if (wsSession instanceof TerminalWebSocketSession) {
                        TerminalWebSocketSession terminalSession = (TerminalWebSocketSession) wsSession;
                        if (terminalSession.isConnected()) {
                            terminalSession.getNettyChannel().writeAndFlush(new TextWebSocketFrame(message));
                            connection.incrementSentCount();
                            terminalSession.incrementSentMessageCount();
                            return true;
                        }
                    }
                    return false;
                } catch (Exception e) {
                    log.error("发送消息失败: oid={}, tid={}", oid, connection.getTid(), e);
                    connection.incrementErrorCount();
                    return false;
                }
            }

            public Collection<Long> getOnlineTerminalIds() {
                return terminals.values().stream()
                    .filter(conn -> conn.getStatus() == TerminalConnection.ConnectionStatus.CONNECTED)
                    .map(TerminalConnection::getTid)
                    .collect(java.util.stream.Collectors.toList());
            }

            public boolean updateTerminalActiveTime(Long tid, LocalDateTime lastActiveTime) {
                TerminalConnection connection = terminals.get(tid);
                if (connection != null) {
                    connection.setLastActiveTime(lastActiveTime);
                    return true;
                }
                return false;
            }

            public int cleanupExpiredConnections(LocalDateTime expireThreshold) {
                List<Long> expiredTerminalIds = new ArrayList<>();
                
                for (Map.Entry<Long, TerminalConnection> entry : terminals.entrySet()) {
                    TerminalConnection connection = entry.getValue();
                    if (connection.isExpired(expireThreshold)) {
                        expiredTerminalIds.add(entry.getKey());
                    }
                }
                
                // 移除过期连接
                for (Long tid : expiredTerminalIds) {
                    terminals.remove(tid);
                }
                
                return expiredTerminalIds.size();
            }

            public int shutdown() {
                int size = terminals.size();
                
                // 关闭所有连接的会话
                for (TerminalConnection connection : terminals.values()) {
                    try {
                        // TODO: 关闭WebSocket会话
                        connection.setStatus(TerminalConnection.ConnectionStatus.DISCONNECTED);
                    } catch (Exception e) {
                        log.warn("关闭连接会话失败: tid={}", connection.getTid(), e);
                    }
                }
                
                terminals.clear();
                return size;
            }
        }
    }
}