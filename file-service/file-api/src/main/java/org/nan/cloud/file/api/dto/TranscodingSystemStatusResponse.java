package org.nan.cloud.file.api.dto;

import lombok.Data;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 转码系统状态响应
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "转码系统状态响应")
public class TranscodingSystemStatusResponse {

    /**
     * 系统状态
     */
    @Schema(description = "系统状态", example = "HEALTHY")
    private String systemStatus;

    /**
     * 队列状态
     */
    @Schema(description = "队列状态")
    private QueueStatus queueStatus;

    /**
     * 工作节点状态
     */
    @Schema(description = "工作节点状态")
    private List<WorkerStatus> workerNodes;

    /**
     * 系统资源使用情况
     */
    @Schema(description = "系统资源使用情况")
    private ResourceUsage resourceUsage;

    /**
     * 性能指标
     */
    @Schema(description = "性能指标")
    private PerformanceMetrics performance;

    /**
     * 最后更新时间
     */
    @Schema(description = "最后更新时间")
    private LocalDateTime lastUpdateTime;

    /**
     * 队列状态
     */
    @Data
    @Builder
    @Schema(description = "队列状态")
    public static class QueueStatus {
        
        /**
         * 等待队列长度
         */
        @Schema(description = "等待队列长度")
        private Integer pendingCount;

        /**
         * 处理中任务数
         */
        @Schema(description = "处理中任务数")
        private Integer processingCount;

        /**
         * 队列最大容量
         */
        @Schema(description = "队列最大容量")
        private Integer maxCapacity;

        /**
         * 平均等待时间 (分钟)
         */
        @Schema(description = "平均等待时间")
        private Double avgWaitTime;

        /**
         * 高优先级任务数
         */
        @Schema(description = "高优先级任务数")
        private Integer highPriorityCount;
    }

    /**
     * 工作节点状态
     */
    @Data
    @Builder
    @Schema(description = "工作节点状态")
    public static class WorkerStatus {
        
        /**
         * 节点ID
         */
        @Schema(description = "节点ID")
        private String nodeId;

        /**
         * 节点状态
         */
        @Schema(description = "节点状态", example = "ACTIVE")
        private String status;

        /**
         * 当前处理任务数
         */
        @Schema(description = "当前处理任务数")
        private Integer currentTasks;

        /**
         * 最大并发数
         */
        @Schema(description = "最大并发数")
        private Integer maxConcurrency;

        /**
         * CPU使用率
         */
        @Schema(description = "CPU使用率")
        private Double cpuUsage;

        /**
         * 内存使用率
         */
        @Schema(description = "内存使用率")
        private Double memoryUsage;

        /**
         * GPU使用率
         */
        @Schema(description = "GPU使用率")
        private Double gpuUsage;

        /**
         * 最后心跳时间
         */
        @Schema(description = "最后心跳时间")
        private LocalDateTime lastHeartbeat;
    }

    /**
     * 资源使用情况
     */
    @Data
    @Builder
    @Schema(description = "资源使用情况")
    public static class ResourceUsage {
        
        /**
         * 整体CPU使用率
         */
        @Schema(description = "整体CPU使用率")
        private Double cpuUsage;

        /**
         * 整体内存使用率
         */
        @Schema(description = "整体内存使用率")
        private Double memoryUsage;

        /**
         * 磁盘使用率
         */
        @Schema(description = "磁盘使用率")
        private Double diskUsage;

        /**
         * 网络使用率
         */
        @Schema(description = "网络使用率")
        private Double networkUsage;

        /**
         * GPU使用情况
         */
        @Schema(description = "GPU使用情况")
        private Map<String, Double> gpuUsage;

        /**
         * 存储空间使用情况
         */
        @Schema(description = "存储空间使用情况")
        private StorageUsage storageUsage;
    }

    /**
     * 存储使用情况
     */
    @Data
    @Builder
    @Schema(description = "存储使用情况")
    public static class StorageUsage {
        
        /**
         * 临时存储使用量 (GB)
         */
        @Schema(description = "临时存储使用量")
        private Double tempStorageUsed;

        /**
         * 输出存储使用量 (GB)
         */
        @Schema(description = "输出存储使用量")
        private Double outputStorageUsed;

        /**
         * 可用存储空间 (GB)
         */
        @Schema(description = "可用存储空间")
        private Double availableStorage;
    }

    /**
     * 性能指标
     */
    @Data
    @Builder
    @Schema(description = "性能指标")
    public static class PerformanceMetrics {
        
        /**
         * 平均处理速度 (fps)
         */
        @Schema(description = "平均处理速度")
        private Double avgProcessingSpeed;

        /**
         * 任务完成率 (tasks/hour)
         */
        @Schema(description = "任务完成率")
        private Double taskCompletionRate;

        /**
         * 系统吞吐量 (GB/hour)
         */
        @Schema(description = "系统吞吐量")
        private Double systemThroughput;

        /**
         * 错误率
         */
        @Schema(description = "错误率")
        private Double errorRate;

        /**
         * 平均响应时间 (ms)
         */
        @Schema(description = "平均响应时间")
        private Double avgResponseTime;
    }
}