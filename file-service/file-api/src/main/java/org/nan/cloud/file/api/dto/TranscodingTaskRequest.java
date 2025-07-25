package org.nan.cloud.file.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Map;

/**
 * 转码任务请求DTO
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Schema(description = "转码任务请求")
public class TranscodingTaskRequest {

    @Schema(description = "源文件ID", required = true, example = "file_123456789")
    @NotBlank(message = "源文件ID不能为空")
    private String sourceFileId;

    @Schema(description = "输出文件名", example = "output_video.mp4")
    private String outputFilename;

    @Schema(description = "转码预设ID", example = "preset_hd")
    private String presetId;

    @Schema(description = "输出格式", required = true, example = "MP4")
    @NotNull(message = "输出格式不能为空")
    private OutputFormat outputFormat;

    @Schema(description = "视频质量", example = "HD")
    private VideoQuality videoQuality;

    @Schema(description = "自定义转码参数")
    private TranscodingParameters customParameters;

    @Schema(description = "任务优先级", example = "NORMAL")
    private TaskPriority priority = TaskPriority.NORMAL;

    @Schema(description = "是否启用GPU加速", example = "true")
    private Boolean enableGpuAcceleration = true;

    @Schema(description = "回调URL", example = "https://api.example.com/callback/transcoding")
    private String callbackUrl;

    @Schema(description = "任务标签")
    private Map<String, String> tags;

    @Schema(description = "最大重试次数", example = "3")
    private Integer maxRetries = 3;

    @Schema(description = "任务超时时间（分钟）", example = "60")
    @Positive(message = "超时时间必须大于0")
    private Integer timeoutMinutes = 60;

    /**
     * 输出格式枚举
     */
    public enum OutputFormat {
        MP4,        // MP4格式
        AVI,        // AVI格式
        MOV,        // MOV格式
        FLV,        // FLV格式
        WEBM,       // WEBM格式
        MKV,        // MKV格式
        WMV,        // WMV格式
        M4V,        // M4V格式
        TS,         // TS格式
        M3U8        // HLS格式
    }

    /**
     * 视频质量枚举
     */
    public enum VideoQuality {
        LOW(480, 800),          // 标清
        MEDIUM(720, 1500),      // 高清
        HD(1080, 3000),         // 全高清
        FULL_HD(1080, 5000),    // 超清
        UHD_4K(2160, 8000),     // 4K超高清
        UHD_8K(4320, 16000);    // 8K超高清

        private final int height;
        private final int bitrate;

        VideoQuality(int height, int bitrate) {
            this.height = height;
            this.bitrate = bitrate;
        }

        public int getHeight() { return height; }
        public int getBitrate() { return bitrate; }
    }

    /**
     * 任务优先级枚举
     */
    public enum TaskPriority {
        LOW(1),         // 低优先级
        NORMAL(5),      // 普通优先级
        HIGH(8),        // 高优先级
        URGENT(10);     // 紧急优先级

        private final int level;

        TaskPriority(int level) {
            this.level = level;
        }

        public int getLevel() { return level; }
    }

    /**
     * 转码参数
     */
    @Data
    @Schema(description = "转码参数")
    public static class TranscodingParameters {

        @Schema(description = "视频编码器", example = "libx264")
        private String videoCodec;

        @Schema(description = "音频编码器", example = "aac")
        private String audioCodec;

        @Schema(description = "视频比特率", example = "3000")
        private Integer videoBitrate;

        @Schema(description = "音频比特率", example = "128")
        private Integer audioBitrate;

        @Schema(description = "帧率", example = "30")
        private Double frameRate;

        @Schema(description = "分辨率宽度", example = "1920")
        private Integer width;

        @Schema(description = "分辨率高度", example = "1080")
        private Integer height;

        @Schema(description = "纵横比", example = "16:9")
        private String aspectRatio;

        @Schema(description = "GOP大小", example = "30")
        private Integer gopSize;

        @Schema(description = "B帧数量", example = "2")
        private Integer bFrames;

        @Schema(description = "编码预设", example = "fast")
        private String preset;

        @Schema(description = "CRF质量参数", example = "23")
        private Integer crf;

        @Schema(description = "音频采样率", example = "44100")
        private Integer audioSampleRate;

        @Schema(description = "音频通道数", example = "2")
        private Integer audioChannels;

        @Schema(description = "开始时间（秒）", example = "10")
        private Double startTime;

        @Schema(description = "持续时间（秒）", example = "60")
        private Double duration;

        @Schema(description = "是否移除音频", example = "false")
        private Boolean removeAudio = false;

        @Schema(description = "是否移除视频", example = "false")
        private Boolean removeVideo = false;

        @Schema(description = "水印设置")
        private WatermarkSettings watermark;

        @Schema(description = "字幕设置")
        private SubtitleSettings subtitle;

        /**
         * 水印设置
         */
        @Data
        @Schema(description = "水印设置")
        public static class WatermarkSettings {

            @Schema(description = "水印图片URL", example = "https://example.com/watermark.png")
            private String imageUrl;

            @Schema(description = "水印位置", example = "TOP_RIGHT")
            private WatermarkPosition position = WatermarkPosition.BOTTOM_RIGHT;

            @Schema(description = "水印透明度", example = "0.5")
            private Double opacity = 0.5;

            @Schema(description = "边距X", example = "10")
            private Integer marginX = 10;

            @Schema(description = "边距Y", example = "10")
            private Integer marginY = 10;

            public enum WatermarkPosition {
                TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER
            }
        }

        /**
         * 字幕设置
         */
        @Data
        @Schema(description = "字幕设置")
        public static class SubtitleSettings {

            @Schema(description = "字幕文件URL", example = "https://example.com/subtitle.srt")
            private String subtitleUrl;

            @Schema(description = "字幕语言", example = "zh-CN")
            private String language = "zh-CN";

            @Schema(description = "字体名称", example = "Arial")
            private String fontName = "Arial";

            @Schema(description = "字体大小", example = "16")
            private Integer fontSize = 16;

            @Schema(description = "字体颜色", example = "#FFFFFF")
            private String fontColor = "#FFFFFF";

            @Schema(description = "是否嵌入字幕", example = "true")
            private Boolean embed = true;
        }
    }
}