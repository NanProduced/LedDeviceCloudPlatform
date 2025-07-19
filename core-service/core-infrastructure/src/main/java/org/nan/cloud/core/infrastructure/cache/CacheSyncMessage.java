package org.nan.cloud.core.infrastructure.cache;

import lombok.Builder;
import lombok.Data;

import java.util.Collection;

/**
 * 缓存同步消息
 */
@Data
@Builder
public class CacheSyncMessage {
    private CacheSyncType type;
    private String key;
    private Collection<String> keys;
    private String pattern;
    private Long timestamp;
}

/**
 * 缓存同步类型
 */
enum CacheSyncType {
    EVICT,
    MULTI_EVICT, 
    EVICT_BY_PATTERN
}