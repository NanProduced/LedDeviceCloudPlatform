package org.nan.cloud.file.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 转码队列状态响应DTO
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "转码队列状态响应")
public class TranscodingQueueStatusResponse {

    @Schema(description = "队列名称", example = "default")
    private String queueName;

    @Schema(description = "队列总容量", example = "100")
    private Integer totalCapacity;

    @Schema(description = "当前队列大小", example = "25")
    private Integer currentSize;

    @Schema(description = "可用容量", example = "75")
    private Integer availableCapacity;

    @Schema(description = "正在处理的任务数", example = "3")
    private Integer processingCount;

    @Schema(description = "等待处理的任务数", example = "22")
    private Integer pendingCount;

    @Schema(description = "队列状态", example = "ACTIVE")
    private QueueStatus status;

    @Schema(description = "平均等待时间（分钟）", example = "5")
    private Double averageWaitTime;

    @Schema(description = "平均处理时间（分钟）", example = "15")
    private Double averageProcessingTime;

    @Schema(description = "队列健康状态", example = "HEALTHY")
    private HealthStatus healthStatus;

    @Schema(description = "最后更新时间")
    private LocalDateTime lastUpdateTime;

    @Schema(description = "处理器状态列表")
    private List<ProcessorStatus> processors;

    @Schema(description = "任务统计信息")
    private TaskStatistics taskStatistics;

    @Schema(description = "队列配置信息")
    private QueueConfiguration configuration;

    /**
     * 队列状态枚举
     */
    public enum QueueStatus {
        ACTIVE("活跃"),
        PAUSED("暂停"),
        STOPPED("停止"),
        MAINTENANCE("维护中");

        private final String description;

        QueueStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 健康状态枚举
     */
    public enum HealthStatus {
        HEALTHY("健康"),
        WARNING("警告"),
        CRITICAL("严重"),
        DOWN("宕机");

        private final String description;

        HealthStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 处理器状态
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "处理器状态")
    public static class ProcessorStatus {

        @Schema(description = "处理器ID", example = "processor-001")
        private String processorId;

        @Schema(description = "处理器名称", example = "GPU加速处理器")
        private String processorName;

        @Schema(description = "处理器状态", example = "BUSY")
        private ProcessorState state;

        @Schema(description = "当前处理的任务ID", example = "task_123456")
        private String currentTaskId;

        @Schema(description = "处理器负载(%)", example = "85")
        private Integer loadPercentage;

        @Schema(description = "已处理任务数", example = "127")
        private Long processedTasks;

        @Schema(description = "处理器启动时间")
        private LocalDateTime startTime;

        @Schema(description = "最后活跃时间")
        private LocalDateTime lastActiveTime;

        @Schema(description = "是否支持GPU加速", example = "true")
        private Boolean gpuEnabled;

        @Schema(description = "处理器版本", example = "v1.2.3")
        private String version;

        /**
         * 处理器状态枚举
         */
        public enum ProcessorState {
            IDLE("空闲"),
            BUSY("忙碌"),
            PAUSED("暂停"),
            ERROR("错误"),
            OFFLINE("离线");

            private final String description;

            ProcessorState(String description) {
                this.description = description;
            }

            public String getDescription() {
                return description;
            }
        }
    }

    /**
     * 任务统计信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "任务统计信息")
    public static class TaskStatistics {

        @Schema(description = "今日提交任务数", example = "45")
        private Long todaySubmitted;

        @Schema(description = "今日完成任务数", example = "38")
        private Long todayCompleted;

        @Schema(description = "今日失败任务数", example = "2")
        private Long todayFailed;

        @Schema(description = "本周提交任务数", example = "312")
        private Long weekSubmitted;

        @Schema(description = "本周完成任务数", example = "298")
        private Long weekCompleted;

        @Schema(description = "本周失败任务数", example = "8")
        private Long weekFailed;

        @Schema(description = "本月提交任务数", example = "1256")
        private Long monthSubmitted;

        @Schema(description = "本月完成任务数", example = "1198")
        private Long monthCompleted;

        @Schema(description = "本月失败任务数", example = "32")
        private Long monthFailed;

        @Schema(description = "总处理时间（小时）", example = "2487.5")
        private Double totalProcessingHours;

        @Schema(description = "平均任务大小（MB）", example = "245.8")
        private Double averageTaskSize;

        @Schema(description = "成功率(%)", example = "95.2")
        private Double successRate;
    }

    /**
     * 队列配置信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "队列配置信息")
    public static class QueueConfiguration {

        @Schema(description = "最大并发任务数", example = "5")
        private Integer maxConcurrentTasks;

        @Schema(description = "任务超时时间（分钟）", example = "120")
        private Integer taskTimeoutMinutes;

        @Schema(description = "最大重试次数", example = "3")
        private Integer maxRetries;

        @Schema(description = "优先级队列启用", example = "true")
        private Boolean priorityQueueEnabled;

        @Schema(description = "GPU加速启用", example = "true")
        private Boolean gpuAccelerationEnabled;

        @Schema(description = "自动清理过期任务", example = "true")
        private Boolean autoCleanupEnabled;

        @Schema(description = "清理过期时间（小时）", example = "24")
        private Integer cleanupAfterHours;

        @Schema(description = "队列监控启用", example = "true")
        private Boolean monitoringEnabled;

        @Schema(description = "告警阈值设置")
        private AlertThresholds alertThresholds;
    }

    /**
     * 告警阈值设置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "告警阈值设置")
    public static class AlertThresholds {

        @Schema(description = "队列满载阈值(%)", example = "90")
        private Integer queueFullThreshold;

        @Schema(description = "等待时间告警阈值（分钟）", example = "30")
        private Integer waitTimeThreshold;

        @Schema(description = "失败率告警阈值(%)", example = "10")
        private Integer failureRateThreshold;

        @Schema(description = "处理器离线告警阈值（分钟）", example = "5")
        private Integer processorOfflineThreshold;
    }
}