package org.nan.cloud.core.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.core.service.TerminalCacheService;
import org.nan.cloud.terminal.api.common.redis.ShareKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TerminalCacheServiceImpl implements TerminalCacheService {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Set<Long> getOnlineTidsByOid(Long oid) {
        String onlineKey = String.format(ShareKeys.TERMINAL_ONLINE_KEY_PATTERN, oid);
        Set<String> tids = stringRedisTemplate.opsForZSet().range(onlineKey, 0, -1);
        if (CollectionUtils.isEmpty(tids)) return Collections.emptySet();
        return tids.stream()
                .map(Long::valueOf)
                .collect(Collectors.toSet());
    }
}
