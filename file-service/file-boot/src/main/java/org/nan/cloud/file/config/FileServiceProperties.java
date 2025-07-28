package org.nan.cloud.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 文件服务配置属性
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "file")
@org.springframework.boot.context.properties.EnableConfigurationProperties
public class FileServiceProperties {

    private Storage storage = new Storage();
    private Transcoding transcoding = new Transcoding();
    private Validation validation = new Validation();
    private Thumbnail thumbnail = new Thumbnail();
    private Progress progress = new Progress();

    @Data
    public static class Storage {
        private String defaultStrategy = "AUTO";
        private Local local = new Local();
        private Oss oss = new Oss();

        @Data
        public static class Local {
            private String basePath = "/data/files";
            private String tempPath = "/data/temp";
            private String urlPrefix = "http://localhost:8085/files";
        }

        @Data
        public static class Oss {
            private String endpoint = "oss-cn-hangzhou.aliyuncs.com";
            private String accessKeyId = "your-access-key";
            private String accessKeySecret = "your-access-secret";
            private String bucketName = "led-device-files";
            private String urlPrefix = "https://led-device-files.oss-cn-hangzhou.aliyuncs.com";
        }
    }

    @Data
    public static class Transcoding {
        private Ffmpeg ffmpeg = new Ffmpeg();
        private Temp temp = new Temp();
        private Queue queue = new Queue();
        private List<Preset> presets;

        @Data
        public static class Ffmpeg {
            private String path = "/usr/local/bin/ffmpeg";
            private int threads = 8;
            private int timeout = 3600;
            private boolean enableGpu = true;
        }

        @Data
        public static class Temp {
            private String dir = "/data/transcoding";
        }

        @Data
        public static class Queue {
            private int maxConcurrentTasks = 3;
            private int maxQueueSize = 100;
            private boolean priorityEnabled = true;
        }

        @Data
        public static class Preset {
            private String id;
            private String name;
            private String videoCodec;
            private String audioCodec;
            private int width;
            private int height;
            private int videoBitrate;
            private int audioBitrate;
            private int crf;
            private String preset;
        }
    }

    @Data
    public static class Validation {
        private Map<String, List<String>> supportedTypes;
        private Map<String, Long> maxFileSize;
    }

    @Data
    public static class Thumbnail {
        private boolean enabled = true;
        private List<String> sizes;
        private double quality = 0.8;
        private String format = "jpg";
    }

    @Data
    public static class Progress {
        private Websocket websocket = new Websocket();
        private Redis redis = new Redis();

        @Data
        public static class Websocket {
            private boolean enabled = true;
            private String endpoint = "/file/progress";
        }

        @Data
        public static class Redis {
            private String keyPrefix = "file:progress";
            private int expireSeconds = 3600;
        }
    }
}