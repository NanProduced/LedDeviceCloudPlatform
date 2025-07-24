package org.nan.cloud.terminal.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * WebSocket配置属性
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "websocket")
public class WebSocketProperties {

    /**
     * WebSocket端点路径
     */
    private String endpoint = "/terminal/ws";

    /**
     * 心跳配置
     */
    private Heartbeat heartbeat = new Heartbeat();

    /**
     * 连接配置
     */
    private Connection connection = new Connection();

    @Data
    public static class Heartbeat {
        /**
         * 心跳间隔(秒)
         */
        private int interval = 30;

        /**
         * 心跳超时(秒)
         */
        private int timeout = 60;
    }

    @Data
    public static class Connection {
        /**
         * 最大连接数
         */
        private int maxConnections = 100;

        /**
         * 分片数量
         */
        private int shardCount = 2;
    }
}