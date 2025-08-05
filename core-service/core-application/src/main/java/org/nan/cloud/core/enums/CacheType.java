package org.nan.cloud.core.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

/**
 * 缓存类型枚举
 * 定义不同业务数据的缓存策略
 */
@Getter
@RequiredArgsConstructor
public enum CacheType {
    
    // 权限相关缓存
    PERMISSION_EXPRESSION("permission:expression", Duration.ofMinutes(30), true, true),
    USER_PERMISSIONS("user:permissions", Duration.ofMinutes(30), true, true),
    
    // 用户相关缓存
    USER_INFO("user:info", Duration.ofMinutes(30), true, true),
    USER_GROUPS("user:groups", Duration.ofMinutes(30), true, true),
    
    // 终端组缓存
    TERMINAL_GROUP_INFO("terminal:group:info", Duration.ofMinutes(30), true, true),
    TERMINAL_GROUP_PERMISSIONS("terminal:group:permissions", Duration.ofMinutes(30), true, true),
    
    // 系统配置缓存
    SYSTEM_CONFIG("system:config", Duration.ofHours(12), true, false),

    // 任务进度缓存
    TASK_PROGRESS("terminal:task:progress", Duration.ofMinutes(30), false, true);

    private final String keyPrefix;
    private final Duration defaultTtl;
    private final boolean useLocalCache;    // 是否使用本地缓存
    private final boolean useDistributedCache; // 是否使用分布式缓存
    
    /**
     * 构建完整的缓存键（不包含组织隔离）
     */
    public String buildKey(String... keyParts) {
        if (keyParts == null || keyParts.length == 0) {
            return keyPrefix;
        }
        return keyPrefix + ":" + String.join(":", keyParts);
    }
    
    /**
     * 构建组织隔离的缓存键
     * @param orgId 组织ID
     * @param keyParts 其他键部分
     * @return 带组织隔离的缓存键
     */
    public String buildOrgKey(Long orgId, String... keyParts) {
        String baseKey = "org:" + orgId + ":" + keyPrefix;
        if (keyParts == null || keyParts.length == 0) {
            return baseKey;
        }
        return baseKey + ":" + String.join(":", keyParts);
    }

    public String buildTaskKey(String taskId) {
        return keyPrefix + ":" + taskId;
    }
    
    /**
     * 构建组织级别的缓存键模式（用于批量清理）
     * @param orgId 组织ID
     * @return 组织缓存键模式
     */
    public String buildOrgPattern(Long orgId) {
        return "org:" + orgId + ":" + keyPrefix + ":*";
    }
}