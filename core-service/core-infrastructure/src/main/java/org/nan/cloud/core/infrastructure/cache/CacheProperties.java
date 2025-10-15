package org.nan.cloud.core.infrastructure.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 缓存配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "cache")
public class CacheProperties {
    
    private Local local = new Local();
    private Redis redis = new Redis();
    private Sync sync = new Sync();
    
    @Data
    public static class Local {
        private boolean enabled = true;
        private int maximumSize = 10000;
        private Duration expireAfterWrite = Duration.ofMinutes(30);
        private Duration expireAfterAccess = Duration.ofMinutes(10);
        private Duration refreshAfterWrite = Duration.ofMinutes(20);
        private boolean recordStats = true;
    }
    
    @Data
    public static class Redis {
        private boolean enabled = true;
        private Duration defaultTtl = Duration.ofMinutes(30);
        private boolean enableKeyspaceNotifications = true;
    }
    
    @Data
    public static class Sync {
        private boolean enabled = true;
        private String topic = "cache:sync";
        private Duration timeout = Duration.ofSeconds(5);
    }
}