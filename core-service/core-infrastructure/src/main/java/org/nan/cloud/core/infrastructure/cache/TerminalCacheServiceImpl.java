package org.nan.cloud.core.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.core.service.CacheService;
import org.nan.cloud.core.service.TerminalCacheService;
import org.nan.cloud.terminal.api.common.redis.ShareKeys;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class TerminalCacheServiceImpl implements TerminalCacheService {

    private final CacheService cacheService;

    @Override
    public Set<Long> getOnlineTidsByOid(Long oid) {
        String onlineKey = String.format(ShareKeys.TERMINAL_ONLINE_KEY_PATTERN, oid);
        return Set.of();
    }
}
