package org.nan.cloud.terminal.cache;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.terminal.infrastructure.config.RedisConfig;
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

    private final StringRedisTemplate stringRedisTemplate;

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
            stringRedisTemplate.expire(countKey, Duration.ofMinutes(2)); // 设置过期时间防止堆积
            log.info("终端上线: oid={}, tid={}", oid, tid);
        }
        log.debug("更新终端活跃: oid={}, tid={}", oid, tid);
    }

    /**
     * 获取组织在线终端列表
     */
    public Set<Long> getOnlineTerminals(Long oid) {
        String onlineKey = String.format(RedisConfig.RedisKeys.TERMINAL_ONLINE_COUNT_PATTERN, oid);
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
        String onlineKey = String.format(RedisConfig.RedisKeys.TERMINAL_ONLINE_COUNT_PATTERN, oid);
        Long actualCount = stringRedisTemplate.opsForZSet().count(onlineKey, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        // 重新设置计数缓存
        stringRedisTemplate.opsForValue().set(countKey, actualCount.toString(), Duration.ofMinutes(2));

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

            // 减少计数
            stringRedisTemplate.opsForValue().decrement(countKey);

            log.info("终端离线: oid={}, tid={}", oid, tid);
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
        if (onlineKeys == null || onlineKeys.isEmpty()) {
            return;
        }

        int totalCleaned = 0;

        for (String onlineKey : onlineKeys) {
            // 提取组织ID
            Long oid = extractOidFromKey(onlineKey);

            // 查找过期的终端（score < cutoffTime）
            Set<String> expiredTerminals = stringRedisTemplate.opsForZSet()
                    .rangeByScore(onlineKey, 0, cutoffTime);

            if (!expiredTerminals.isEmpty()) {
                // 批量移除过期终端
                String[] terminalArray = expiredTerminals.toArray(new String[0]);
                Long removedCount = stringRedisTemplate.opsForZSet().remove(onlineKey, (Object[]) terminalArray);

                if (removedCount > 0) {
                    // 更新计数
                    String countKey = String.format(RedisConfig.RedisKeys.TERMINAL_ONLINE_COUNT_PATTERN, oid);
                    stringRedisTemplate.opsForValue().decrement(countKey, removedCount);

                    totalCleaned += removedCount;
                    log.info("清理离线终端: oid={}, count={}", oid, removedCount);
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
