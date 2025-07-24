package org.nan.cloud.terminal.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Netty配置属性
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "netty")
public class NettyProperties {

    /**
     * Boss线程数(接受连接)
     */
    private int bossThreads = 1;

    /**
     * Worker线程数(处理I/O)
     */
    private int workerThreads = 2;

    /**
     * Socket配置
     */
    private Socket socket = new Socket();

    @Data
    public static class Socket {
        /**
         * TCP连接队列大小
         */
        private int backlog = 512;
    }
}