package org.nan.cloud.file.api.dto;

import lombok.Data;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 转码任务列表响应
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "转码任务列表响应")
public class TranscodingTaskListResponse {

    /**
     * 任务列表
     */
    @Schema(description = "转码任务列表")
    private List<TranscodingTaskResponse> tasks;

    /**
     * 总记录数
     */
    @Schema(description = "总记录数")
    private Long total;

    /**
     * 当前页码
     */
    @Schema(description = "当前页码")
    private Integer page;

    /**
     * 每页大小
     */
    @Schema(description = "每页大小")
    private Integer size;

    /**
     * 总页数
     */
    @Schema(description = "总页数")
    private Integer totalPages;

    /**
     * 是否有下一页
     */
    @Schema(description = "是否有下一页")
    private Boolean hasNext;

    /**
     * 是否有上一页
     */
    @Schema(description = "是否有上一页")
    private Boolean hasPrevious;

    /**
     * 统计信息
     */
    @Schema(description = "统计信息")
    private TaskStatistics statistics;

    /**
     * 任务统计信息
     */
    @Data
    @Builder
    @Schema(description = "任务统计信息")
    public static class TaskStatistics {
        
        /**
         * 待处理任务数
         */
        @Schema(description = "待处理任务数")
        private Long pendingCount;

        /**
         * 处理中任务数
         */
        @Schema(description = "处理中任务数")
        private Long processingCount;

        /**
         * 已完成任务数
         */
        @Schema(description = "已完成任务数")
        private Long completedCount;

        /**
         * 失败任务数
         */
        @Schema(description = "失败任务数")
        private Long failedCount;

        /**
         * 平均处理时间 (分钟)
         */
        @Schema(description = "平均处理时间")
        private Double averageProcessTime;
    }
}