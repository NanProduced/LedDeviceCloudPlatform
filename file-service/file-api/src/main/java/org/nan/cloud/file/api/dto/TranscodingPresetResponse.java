package org.nan.cloud.file.api.dto;

import lombok.Data;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * 转码预设响应
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "转码预设响应")
public class TranscodingPresetResponse {

    /**
     * 预设列表
     */
    @Schema(description = "转码预设列表")
    private List<PresetInfo> presets;

    /**
     * 支持的格式
     */
    @Schema(description = "支持的输出格式")
    private List<String> supportedFormats;

    /**
     * 预设信息
     */
    @Data
    @Builder
    @Schema(description = "转码预设信息")
    public static class PresetInfo {
        
        /**
         * 预设名称
         */
        @Schema(description = "预设名称", example = "HD")
        private String name;

        /**
         * 预设显示名称
         */
        @Schema(description = "预设显示名称", example = "高清(720p)")
        private String displayName;

        /**
         * 预设描述
         */
        @Schema(description = "预设描述")
        private String description;

        /**
         * 视频参数
         */
        @Schema(description = "视频参数")
        private VideoConfig videoConfig;

        /**
         * 音频参数
         */
        @Schema(description = "音频参数")
        private AudioConfig audioConfig;

        /**
         * 是否默认预设
         */
        @Schema(description = "是否默认预设")
        private Boolean isDefault;

        /**
         * 适用场景
         */
        @Schema(description = "适用场景", example = "WEB")
        private String scene;

        /**
         * 预计文件大小比例
         */
        @Schema(description = "预计文件大小比例")
        private Double estimatedSizeRatio;
    }

    /**
     * 视频配置
     */
    @Data
    @Builder
    @Schema(description = "视频配置")
    public static class VideoConfig {
        
        /**
         * 分辨率
         */
        @Schema(description = "分辨率", example = "1280x720")
        private String resolution;

        /**
         * 码率 (kbps)
         */
        @Schema(description = "码率")
        private Integer bitrate;

        /**
         * 帧率
         */
        @Schema(description = "帧率")
        private Integer frameRate;

        /**
         * 编码器
         */
        @Schema(description = "编码器", example = "h264")
        private String codec;

        /**
         * 质量设置
         */
        @Schema(description = "质量设置")
        private String quality;
    }

    /**
     * 音频配置
     */
    @Data
    @Builder
    @Schema(description = "音频配置")
    public static class AudioConfig {
        
        /**
         * 码率 (kbps)
         */
        @Schema(description = "音频码率")
        private Integer bitrate;

        /**
         * 采样率 (Hz)
         */
        @Schema(description = "采样率")
        private Integer sampleRate;

        /**
         * 声道数
         */
        @Schema(description = "声道数")
        private Integer channels;

        /**
         * 编码器
         */
        @Schema(description = "音频编码器", example = "aac")
        private String codec;
    }
}