package org.nan.cloud.terminal.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 终端业务配置属性
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "terminal")
public class TerminalProperties {

    /**
     * 设备配置
     */
    private Device device = new Device();

    /**
     * 文件配置
     */
    private File file = new File();

    /**
     * 缓存配置
     */
    private Cache cache = new Cache();

    @Data
    public static class Device {
        /**
         * 设备指令超时时间(秒)
         */
        private int commandTimeout = 5;

        /**
         * 设备状态更新间隔(秒)
         */
        private int statusUpdateInterval = 15;

        /**
         * 设备离线判断阈值(秒)
         */
        private int offlineThreshold = 30;
    }

    @Data
    public static class File {
        /**
         * 文件上传路径
         */
        private String uploadPath = "./target/terminal-files";

        /**
         * 单个文件最大大小(MB)
         */
        private int maxFileSize = 10;

        /**
         * 总文件大小限制(GB)
         */
        private double totalSizeLimit = 0.5;

        /**
         * MD5校验开关
         */
        private boolean md5CheckEnabled = false;
    }

    @Data
    public static class Cache {
        /**
         * 设备状态缓存TTL(秒)
         */
        private int deviceStatusTtl = 30;

        /**
         * 指令缓存TTL(秒)
         */
        private int commandTtl = 120;

        /**
         * 认证缓存TTL(秒)
         */
        private int authTtl = 300;
    }
}