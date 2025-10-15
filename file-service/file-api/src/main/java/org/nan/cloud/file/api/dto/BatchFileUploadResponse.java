package org.nan.cloud.file.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 批量文件上传响应
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    private List<FileUploadResponse> successUploads;

    /**
     * 上传失败的文件列表
     */
    @Schema(description = "上传失败的文件列表")
    private List<FailedUpload> failedUploads;

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
     * 上传时间
     */
    @Schema(description = "上传时间")
    private LocalDateTime uploadTime;

    /**
     * 失败上传信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "失败上传信息")
    public static class FailedUpload {
        
        /**
         * 文件名
         */
        @Schema(description = "文件名", example = "document.pdf")
        private String filename;

        /**
         * 文件大小
         */
        @Schema(description = "文件大小", example = "1048576")
        private Long fileSize;

        /**
         * 失败原因
         */
        @Schema(description = "失败原因", example = "文件格式不支持")
        private String errorMessage;

        /**
         * 错误代码
         */
        @Schema(description = "错误代码", example = "UNSUPPORTED_FORMAT")
        private String errorCode;
    }
}