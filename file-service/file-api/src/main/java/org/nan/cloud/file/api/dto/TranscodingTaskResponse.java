package org.nan.cloud.file.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 转码任务响应DTO
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "转码任务响应")
public class TranscodingTaskResponse {

    @Schema(description = "任务ID", example = "transcode_123456789")
    private String taskId;

    @Schema(description = "源文件ID", example = "file_123456789")
    private String sourceFileId;

    @Schema(description = "输出文件ID", example = "file_987654321")
    private String outputFileId;

    @Schema(description = "任务状态", example = "PROCESSING")
    private TaskStatus status;

    @Schema(description = "任务进度（0-100）", example = "75")
    private Integer progress;

    @Schema(description = "预计剩余时间（秒）", example = "120")
    private Long estimatedTimeRemaining;

    @Schema(description = "任务优先级", example = "NORMAL")
    private String priority;

    @Schema(description = "转码配置")
    private TranscodingConfig config;

    @Schema(description = "任务创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "任务开始时间")
    private LocalDateTime startTime;

    @Schema(description = "任务完成时间")
    private LocalDateTime completedTime;

    @Schema(description = "已用时间（秒）", example = "180")
    private Long elapsedTime;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "错误代码", example = "FFMPEG_ERROR")
    private String errorCode;

    @Schema(description = "重试次数", example = "1")
    private Integer retryCount;

    @Schema(description = "最大重试次数", example = "3")
    private Integer maxRetries;

    @Schema(description = "任务标签")
    private Map<String, String> tags;

    @Schema(description = "执行节点信息")
    private ExecutionNode executionNode;

    @Schema(description = "性能统计")
    private PerformanceStats performanceStats;

    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        PENDING,        // 等待中
        QUEUED,         // 已排队
        PROCESSING,     // 处理中
        PAUSED,         // 已暂停
        COMPLETED,      // 已完成
        FAILED,         // 失败
        CANCELLED,      // 已取消
        TIMEOUT         // 超时
    }

    /**
     * 转码配置信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "转码配置")
    public static class TranscodingConfig {

        @Schema(description = "输出格式", example = "MP4")
        private String outputFormat;

        @Schema(description = "视频质量", example = "HD")
        private String videoQuality;

        @Schema(description = "视频编码器", example = "libx264")
        private String videoCodec;

        @Schema(description = "音频编码器", example = "aac")
        private String audioCodec;

        @Schema(description = "分辨率", example = "1920x1080")
        private String resolution;

        @Schema(description = "帧率", example = "30")
        private Double frameRate;

        @Schema(description = "视频比特率", example = "3000")
        private Integer videoBitrate;

        @Schema(description = "音频比特率", example = "128")
        private Integer audioBitrate;

        @Schema(description = "是否启用GPU加速", example = "true")
        private Boolean gpuAcceleration;
    }

    /**
     * 执行节点信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "执行节点信息")
    public static class ExecutionNode {

        @Schema(description = "节点ID", example = "node-001")
        private String nodeId;

        @Schema(description = "节点名称", example = "transcode-worker-1")
        private String nodeName;

        @Schema(description = "节点IP", example = "192.168.1.100")
        private String nodeIp;

        @Schema(description = "CPU使用率", example = "85.5")
        private Double cpuUsage;

        @Schema(description = "内存使用率", example = "72.3")
        private Double memoryUsage;

        @Schema(description = "GPU使用率", example = "90.2")
        private Double gpuUsage;
    }

    /**
     * 性能统计信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "性能统计")
    public static class PerformanceStats {

        @Schema(description = "输入文件大小（字节）", example = "1073741824")
        private Long inputFileSize;

        @Schema(description = "输出文件大小（字节）", example = "536870912")
        private Long outputFileSize;

        @Schema(description = "压缩比", example = "0.5")
        private Double compressionRatio;

        @Schema(description = "处理速度（fps）", example = "25.5")
        private Double processingSpeed;

        @Schema(description = "平均CPU使用率", example = "78.5")
        private Double avgCpuUsage;

        @Schema(description = "峰值内存使用量（MB）", example = "2048")
        private Long peakMemoryUsage;

        @Schema(description = "网络I/O（字节）", example = "1048576")
        private Long networkIo;

        @Schema(description = "磁盘I/O（字节）", example = "2097152")
        private Long diskIo;
    }
}