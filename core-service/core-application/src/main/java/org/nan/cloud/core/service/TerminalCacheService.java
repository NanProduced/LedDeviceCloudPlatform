package org.nan.cloud.core.service;

import java.util.Set;

/**
 * 终端相关缓存
 * 使用terminal-service模块共享缓存
 */
public interface TerminalCacheService {

    Set<Long> getOnlineTidsByOid(Long oid);
}
