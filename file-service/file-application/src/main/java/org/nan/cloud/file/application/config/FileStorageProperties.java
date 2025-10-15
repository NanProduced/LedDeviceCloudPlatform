package org.nan.cloud.file.application.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * 文件存储配置属性
 * 
 * 映射application.yml中的file.*配置项，提供类型安全的配置访问
 * 支持IDE配置提示和验证
 * 
 * 按照DDD分层架构，配置属性类放置在application层，
 * 这样application层和infrastructure层都可以注入使用
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "file")
public class FileStorageProperties {

    /**
     * 存储相关配置
     */
    @Valid
    @NestedConfigurationProperty
    private Storage storage = new Storage();

    /**
     * 转码相关配置
     */
    @Valid
    @NestedConfigurationProperty
    private Transcoding transcoding = new Transcoding();

    /**
     * 文件验证配置
     */
    @Valid
    @NestedConfigurationProperty
    private Validation validation = new Validation();

    /**
     * 缩略图配置
     */
    @Valid
    @NestedConfigurationProperty
    private Thumbnail thumbnail = new Thumbnail();

    /**
     * 存储配置
     */
    @Data
    public static class Storage {
        
        /**
         * 默认存储策略
         */
        @NotBlank(message = "默认存储策略不能为空")
        private String defaultStrategy = "LOCAL";

        /**
         * 本地存储配置
         */
        @Valid
        @NestedConfigurationProperty
        private Local local = new Local();

        /**
         * OSS存储配置
         */
        @Valid
        @NestedConfigurationProperty
        private Oss oss = new Oss();

        /**
         * 本地存储配置
         */
        @Data
        public static class Local {
            /**
             * 存储根目录
             */
            @NotBlank(message = "本地存储根目录不能为空")
            private String basePath = "C:/Users/nanpr/javaProject/filePath";

            /**
             * 临时文件目录
             */
            @NotBlank(message = "临时文件目录不能为空")
            private String tempPath = "C:/Users/nanpr/javaProject/filePath/temp";

            /**
             * 缩略图存储目录
             */
            @NotBlank(message = "缩略图存储目录不能为空")
            private String thumbnailBasePath = "C:/Users/nanpr/javaProject/filePath/thumbnails";

            /**
             * 访问URL前缀
             */
            @NotBlank(message = "访问URL前缀不能为空")
            private String urlPrefix = "http://localhost:8085/files";

            /**
             * 最大文件大小（字节）
             */
            private long maxFileSize = 5368709120L; // 5GB
        }

        /**
         * OSS存储配置
         */
        @Data
        public static class Oss {
            /**
             * OSS访问端点
             */
            private String endpoint;

            /**
             * Access Key ID
             */
            private String accessKeyId;

            /**
             * Access Key Secret
             */
            private String accessKeySecret;

            /**
             * 存储桶名称
             */
            private String bucketName;

            /**
             * 访问URL前缀
             */
            private String urlPrefix;
        }
    }

    /**
     * 转码配置
     */
    @Data
    public static class Transcoding {
        
        @Valid
        @NestedConfigurationProperty
        private Ffmpeg ffmpeg = new Ffmpeg();

        @Valid
        @NestedConfigurationProperty
        private Temp temp = new Temp();

        @Valid
        @NestedConfigurationProperty
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
            private String dir = "C:/temp/transcoding";
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

    /**
     * 文件验证配置
     */
    @Data
    public static class Validation {
        
        /**
         * 支持的文件类型
         */
        @NotNull
        private Map<String, List<String>> supportedTypes;

        /**
         * 文件大小限制
         */
        @NotNull
        private Map<String, Long> maxFileSize;
    }

    /**
     * 缩略图配置
     */
    @Data
    public static class Thumbnail {
        /**
         * 是否启用缩略图生成
         */
        private boolean enabled = true;

        /**
         * 缩略图尺寸列表
         */
        private List<String> sizes = List.of("150x150", "300x300", "600x600");

        /**
         * 图片质量 (0.0-1.0)
         */
        private double quality = 0.8;

        /**
         * 输出格式
         */
        private String format = "jpg";
        
        /**
         * 视频缩略图配置
         */
        @Valid
        @NestedConfigurationProperty
        private Video video = new Video();
        
        /**
         * 视频缩略图配置
         */
        @Data
        public static class Video {
            /**
             * 是否启用视频缩略图生成
             */
            private boolean enabled = true;
            
            /**
             * 视频帧率
             */
            private double frameRate = 1.0;
            
            /**
             * 是否启用GPU硬件加速
             */
            private boolean gpuAcceleration = false;
        }
    }
}