package org.nan.cloud.terminal.cache;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.terminal.infrastructure.config.RedisConfig;
import org.nan.cloud.terminal.infrastructure.connection.ConnectionManager;
import org.nan.cloud.terminal.websocket.session.TerminalWebSocketSession;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TerminalOnlineStatusManager {

    private static final int OFFLINE_THRESHOLD_SECONDS = 60;
    private static final int ONLINE_COUNT_CACHE_MINUTES = 30; // 在线计数缓存超时时间（分钟）

    private final StringRedisTemplate stringRedisTemplate;
    private final ConnectionManager connectionManager;

    public void updateTerminalActivity(Long oid, Long tid) {
        long currentTime = System.currentTimeMillis();
        String onlineKey = String.format(RedisConfig.RedisKeys.TERMINAL_ONLINE_KEY_PATTERN, oid);

        // 检查是否是新上线的终端
        Double existingScore = stringRedisTemplate.opsForZSet().score(onlineKey, tid.toString());
        boolean isNewOnline = (existingScore==null);

        // 更新最后活跃时间
        stringRedisTemplate.opsForZSet().add(onlineKey, tid.toString(), currentTime);
        // 如果是新上线的终端，增加计数
        if (isNewOnline) {
            String countKey = String.format(RedisConfig.RedisKeys.TERMINAL_ONLINE_COUNT_PATTERN, oid);
            stringRedisTemplate.opsForValue().increment(countKey);
            stringRedisTemplate.expire(countKey, Duration.ofMinutes(ONLINE_COUNT_CACHE_MINUTES)); // 设置过期时间防止堆积
            log.info("终端上线: oid={}, tid={}", oid, tid);
        }
        log.debug("更新终端活跃: oid={}, tid={}", oid, tid);
    }

    /**
     * 获取组织在线终端列表
     */
    public Set<Long> getOnlineTerminals(Long oid) {
        String onlineKey = String.format(RedisConfig.RedisKeys.TERMINAL_ONLINE_KEY_PATTERN, oid);
        Set<String> terminalIds = stringRedisTemplate.opsForZSet().range(onlineKey, 0, -1);
        if (CollectionUtils.isEmpty(terminalIds)) return Collections.emptySet();
        return terminalIds.stream()
                .map(Long::valueOf)
                .collect(Collectors.toSet());
    }

    /**
     * 获取组织在线终端数量
     */
    public long getOnlineTerminalCount(Long oid) {
        String countKey = String.format(RedisConfig.RedisKeys.TERMINAL_ONLINE_COUNT_PATTERN, oid);
        String count = stringRedisTemplate.opsForValue().get(countKey);

        if (count != null) {
            return Long.parseLong(count);
        }

        // 如果计数缓存不存在，从Sorted Set重新计算
        String onlineKey = String.format(RedisConfig.RedisKeys.TERMINAL_ONLINE_KEY_PATTERN, oid);
        Long actualCount = stringRedisTemplate.opsForZSet().count(onlineKey, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        // 重新设置计数缓存
        stringRedisTemplate.opsForValue().set(countKey, actualCount.toString(), Duration.ofMinutes(ONLINE_COUNT_CACHE_MINUTES));

        return actualCount;
    }

    /**
     * 检查特定终端是否在线
     */
    public boolean isTerminalOnline(Long oid, Long tid) {
        String onlineKey = String.format(RedisConfig.RedisKeys.TERMINAL_ONLINE_KEY_PATTERN, oid);
        Double score = stringRedisTemplate.opsForZSet().score(onlineKey, tid.toString());

        if (score == null) {
            return false;
        }

        // 检查是否超过60秒阈值
        long lastActiveTime = score.longValue();
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastActiveTime) <= (OFFLINE_THRESHOLD_SECONDS * 1000);
    }

    /**
     * 批量检查终端在线状态
     */
    public Map<Long, Boolean> batchCheckOnlineStatus(Long oid, List<Long> terminalIds) {
        if (terminalIds == null || terminalIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String onlineKey = String.format(RedisConfig.RedisKeys.TERMINAL_ONLINE_KEY_PATTERN, oid);
        long currentTime = System.currentTimeMillis();
        long offlineThreshold = currentTime - (OFFLINE_THRESHOLD_SECONDS * 1000);

        Map<Long, Boolean> statusMap = new HashMap<>();

        for (Long tid : terminalIds) {
            Double score = stringRedisTemplate.opsForZSet().score(onlineKey, tid.toString());
            boolean isOnline = (score != null && score.longValue() > offlineThreshold);
            statusMap.put(tid, isOnline);
        }

        return statusMap;
    }

    /**
     * 手动标记终端离线（用于WebSocket断开等场景）
     */
    public void markTerminalOffline(Long oid, Long tid) {
        String onlineKey = String.format(RedisConfig.RedisKeys.TERMINAL_ONLINE_KEY_PATTERN, oid);
        String countKey = String.format(RedisConfig.RedisKeys.TERMINAL_ONLINE_COUNT_PATTERN, oid);

        // 检查终端是否在线列表中
        Double existingScore = stringRedisTemplate.opsForZSet().score(onlineKey, tid.toString());
        if (existingScore != null) {
            // 从在线列表移除
            stringRedisTemplate.opsForZSet().remove(onlineKey, tid.toString());

            // 减少计数，防止负数
            Long currentCount = stringRedisTemplate.opsForValue().decrement(countKey);
            if (currentCount < 0) {
                log.warn("检测到在线计数为负数，重置为0: oid={}, count={}", oid, currentCount);
                stringRedisTemplate.opsForValue().set(countKey, "0", Duration.ofMinutes(ONLINE_COUNT_CACHE_MINUTES));
            }

            log.info("终端离线: oid={}, tid={}", oid, tid);
        }
    }

    /**
     * 检查终端WebSocket连接是否有效
     */
    private boolean isWebSocketConnected(Long tid) {
        try {
            Optional<Object> sessionOpt = connectionManager.getConnection(tid);
            if (sessionOpt.isPresent() && sessionOpt.get() instanceof TerminalWebSocketSession) {
                TerminalWebSocketSession session = (TerminalWebSocketSession) sessionOpt.get();
                return session.isConnected();
            }
            return false;
        } catch (Exception e) {
            log.debug("检查WebSocket连接状态异常: tid={}", tid, e);
            return false;
        }
    }

    /**
     * 定时清理过期的离线终端
     */
    @Scheduled(fixedRate = 30000) // 每30秒执行一次
    public void cleanupOfflineTerminals() {
        long currentTime = System.currentTimeMillis();
        long cutoffTime = currentTime - (OFFLINE_THRESHOLD_SECONDS * 1000);

        // 获取所有组织的在线终端键
        Set<String> onlineKeys = stringRedisTemplate.keys("terminal:online:org:*");
        if (onlineKeys.isEmpty()) {
            return;
        }

        int totalCleaned = 0;

        for (String onlineKey : onlineKeys) {
            // 提取组织ID
            Long oid = extractOidFromKey(onlineKey);

            // 查找超时的终端（score < cutoffTime）但需要进一步验证WebSocket连接状态
            Set<String> timeoutTerminals = stringRedisTemplate.opsForZSet()
                    .rangeByScore(onlineKey, 0, cutoffTime);

            if (!timeoutTerminals.isEmpty()) {
                List<String> reallyOfflineTerminals = new ArrayList<>();
                
                // 对于每个超时的终端，检查其WebSocket连接状态
                for (String tidStr : timeoutTerminals) {
                    try {
                        Long tid = Long.valueOf(tidStr);
                        
                        // 如果WebSocket连接仍然有效，则不应该被清理
                        if (!isWebSocketConnected(tid)) {
                            reallyOfflineTerminals.add(tidStr);
                        } else {
                            // WebSocket连接仍然有效，更新活跃时间避免下次被误清理
                            updateTerminalActivity(oid, tid);
                            log.debug("终端WebSocket连接有效，更新活跃时间: oid={}, tid={}", oid, tid);
                        }
                    } catch (NumberFormatException e) {
                        log.warn("终端ID格式错误，将被清理: oid={}, tid={}", oid, tidStr);
                        reallyOfflineTerminals.add(tidStr);
                    }
                }

                // 只清理真正离线的终端
                if (!reallyOfflineTerminals.isEmpty()) {
                    String[] terminalArray = reallyOfflineTerminals.toArray(new String[0]);
                    Long removedCount = stringRedisTemplate.opsForZSet().remove(onlineKey, (Object[]) terminalArray);

                    if (removedCount > 0) {
                        // 更新计数，防止负数
                        String countKey = String.format(RedisConfig.RedisKeys.TERMINAL_ONLINE_COUNT_PATTERN, oid);
                        Long currentCount = stringRedisTemplate.opsForValue().decrement(countKey, removedCount);
                        if (currentCount < 0) {
                            log.warn("定时清理导致计数为负数，重置为0: oid={}, count={}, removedCount={}", 
                                    oid, currentCount, removedCount);
                            stringRedisTemplate.opsForValue().set(countKey, "0", Duration.ofMinutes(ONLINE_COUNT_CACHE_MINUTES));
                        }

                        totalCleaned += removedCount;
                        log.info("清理离线终端: oid={}, 超时终端数={}, 实际清理数={}", oid, timeoutTerminals.size(), removedCount);
                    }
                } else {
                    log.debug("组织 {} 的所有超时终端都有有效WebSocket连接，无需清理", oid);
                }
            }
        }

        if (totalCleaned > 0) {
            log.info("定时清理完成，总计清理离线终端: {}", totalCleaned);
        }
    }

    /**
     * 从Redis key中提取组织ID
     */
    private Long extractOidFromKey(String key) {
        // terminal:online:org:123 -> 123
        return Long.valueOf(key.substring(key.lastIndexOf(':') + 1));
    }

    /**
     * 获取组织在线终端详情（用于监控和调试）
     */
    public List<TerminalOnlineInfo> getOnlineTerminalDetails(Long oid) {
        String onlineKey = String.format(RedisConfig.RedisKeys.TERMINAL_ONLINE_KEY_PATTERN, oid);
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .rangeWithScores(onlineKey, 0, -1);

        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }

        long currentTime = System.currentTimeMillis();

        return tuples.stream()
                .map(tuple -> {
                    Long tid = Long.valueOf(tuple.getValue());
                    long lastActiveTime = tuple.getScore().longValue();
                    long idleTime = currentTime - lastActiveTime;
                    boolean isOnline = idleTime <= (OFFLINE_THRESHOLD_SECONDS * 1000);

                    return TerminalOnlineInfo.builder()
                            .tid(tid)
                            .lastActiveTime(lastActiveTime)
                            .idleTimeSeconds(idleTime / 1000)
                            .isOnline(isOnline)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 终端在线信息数据类
     */
    @Data
    @Builder
    public static class TerminalOnlineInfo {
        private Long tid;
        private Long lastActiveTime;
        private Long idleTimeSeconds;
        private Boolean isOnline;
    }


}
