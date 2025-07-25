package org.nan.cloud.file.api.dto;

import lombok.Data;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 批量文件上传响应
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "批量文件上传响应")
public class BatchFileUploadResponse {

    /**
     * 批量上传ID
     */
    @Schema(description = "批量上传ID")
    private String batchId;

    /**
     * 上传成功的文件列表
     */
    @Schema(description = "上传成功的文件列表")
    private List<FileUploadResponse> successFiles;

    /**
     * 上传失败的文件列表
     */
    @Schema(description = "上传失败的文件列表")
    private List<FailedFileInfo> failedFiles;

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
     * 总文件大小 (字节)
     */
    @Schema(description = "总文件大小")
    private Long totalSize;

    /**
     * 已上传文件大小 (字节)
     */
    @Schema(description = "已上传文件大小")
    private Long uploadedSize;

    /**
     * 失败文件信息
     */
    @Data
    @Builder
    @Schema(description = "失败文件信息")
    public static class FailedFileInfo {
        
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

        /**
         * 文件大小
         */
        @Schema(description = "文件大小")
        private Long fileSize;
    }
}