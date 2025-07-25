package org.nan.cloud.file.api.dto;

import lombok.Data;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 文件批量操作响应
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "文件批量操作响应")
public class FileBatchOperationResponse {

    /**
     * 批量操作ID
     */
    @Schema(description = "批量操作ID")
    private String batchId;

    /**
     * 操作状态
     */
    @Schema(description = "操作状态", example = "SUCCESS")
    private String status;

    /**
     * 成功处理的文件ID列表
     */
    @Schema(description = "成功处理的文件ID列表")
    private List<String> successFiles;

    /**
     * 失败处理的文件列表
     */
    @Schema(description = "失败处理的文件列表")
    private List<FailedFileOperation> failedFiles;

    /**
     * 总文件数
     */
    @Schema(description = "总文件数")
    private Integer totalFiles;

    /**
     * 成功文件数
     */
    @Schema(description = "成功文件数")
    private Integer successCount;

    /**
     * 失败文件数
     */
    @Schema(description = "失败文件数")
    private Integer failedCount;

    /**
     * 操作进度 (0-100)
     */
    @Schema(description = "操作进度")
    private Integer progress;

    /**
     * 是否异步执行
     */
    @Schema(description = "是否异步执行")
    private Boolean isAsync;

    /**
     * 失败文件操作信息
     */
    @Data
    @Builder
    @Schema(description = "失败文件操作信息")
    public static class FailedFileOperation {
        
        /**
         * 文件ID
         */
        @Schema(description = "文件ID")
        private String fileId;

        /**
         * 文件名
         */
        @Schema(description = "文件名")
        private String fileName;

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