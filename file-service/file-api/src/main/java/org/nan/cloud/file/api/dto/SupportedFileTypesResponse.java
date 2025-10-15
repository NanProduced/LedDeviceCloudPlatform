package org.nan.cloud.file.api.dto;

import lombok.Data;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * 支持的文件类型响应
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "支持的文件类型响应")
public class SupportedFileTypesResponse {

    /**
     * 支持的文件类型
     */
    @Schema(description = "支持的文件类型")
    private Map<String, FileTypeInfo> supportedTypes;

    /**
     * 文件大小限制 (字节)
     */
    @Schema(description = "文件大小限制")
    private Map<String, Long> sizeLimits;

    /**
     * 支持的转码格式
     */
    @Schema(description = "支持的转码格式")
    private Map<String, List<String>> transcodingFormats;

    /**
     * 系统配置
     */
    @Schema(description = "系统配置")
    private SystemConfig systemConfig;

    /**
     * 文件类型信息
     */
    @Data
    @Builder
    @Schema(description = "文件类型信息")
    public static class FileTypeInfo {
        
        /**
         * 文件类型分类
         */
        @Schema(description = "文件类型分类", example = "VIDEO")
        private String category;

        /**
         * 支持的扩展名
         */
        @Schema(description = "支持的扩展名")
        private List<String> extensions;

        /**
         * MIME类型
         */
        @Schema(description = "MIME类型")
        private List<String> mimeTypes;

        /**
         * 最大文件大小 (字节)
         */
        @Schema(description = "最大文件大小")
        private Long maxSize;

        /**
         * 是否支持转码
         */
        @Schema(description = "是否支持转码")
        private Boolean supportsTranscoding;

        /**
         * 是否支持预览
         */
        @Schema(description = "是否支持预览")
        private Boolean supportsPreview;

        /**
         * 是否支持缩略图
         */
        @Schema(description = "是否支持缩略图")
        private Boolean supportsThumbnail;

        /**
         * 描述
         */
        @Schema(description = "文件类型描述")
        private String description;
    }

    /**
     * 系统配置
     */
    @Data
    @Builder
    @Schema(description = "系统配置")
    public static class SystemConfig {
        
        /**
         * 最大并发上传数
         */
        @Schema(description = "最大并发上传数")
        private Integer maxConcurrentUploads;

        /**
         * 分块上传大小 (字节)
         */
        @Schema(description = "推荐分块大小")
        private Long recommendedChunkSize;

        /**
         * 支持的存储策略
         */
        @Schema(description = "支持的存储策略")
        private List<String> storageStrategies;

        /**
         * 文件保留时间 (天)
         */
        @Schema(description = "临时文件保留时间")
        private Integer tempFileRetentionDays;

        /**
         * 是否启用病毒扫描
         */
        @Schema(description = "是否启用病毒扫描")
        private Boolean virusScanEnabled;

        /**
         * 是否启用内容识别
         */
        @Schema(description = "是否启用内容识别")
        private Boolean contentRecognitionEnabled;
    }
}