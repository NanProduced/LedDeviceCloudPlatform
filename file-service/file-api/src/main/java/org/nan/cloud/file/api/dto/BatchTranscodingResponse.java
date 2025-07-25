package org.nan.cloud.file.api.dto;

import lombok.Data;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 批量转码响应
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "批量转码响应")
public class BatchTranscodingResponse {

    /**
     * 批量任务ID
     */
    @Schema(description = "批量任务ID")
    private String batchId;

    /**
     * 提交成功的任务列表
     */
    @Schema(description = "提交成功的任务列表")
    private List<TranscodingTaskResponse> successTasks;

    /**
     * 提交失败的任务列表
     */
    @Schema(description = "提交失败的任务列表")
    private List<FailedTaskInfo> failedTasks;

    /**
     * 总任务数
     */
    @Schema(description = "总任务数")
    private Integer totalTasks;

    /**
     * 成功任务数
     */
    @Schema(description = "成功任务数")
    private Integer successCount;

    /**
     * 失败任务数
     */
    @Schema(description = "失败任务数")
    private Integer failedCount;

    /**
     * 失败任务信息
     */
    @Data
    @Builder
    @Schema(description = "失败任务信息")
    public static class FailedTaskInfo {
        
        /**
         * 文件ID
         */
        @Schema(description = "文件ID")
        private String fileId;

        /**
         * 失败原因
         */
        @Schema(description = "失败原因")
        private String reason;

        /**
         * 错误代码
         */
        @Schema(description = "错误代码")
        private String errorCode;
    }
}