package org.nan.cloud.file.api.dto;

import lombok.Data;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 分块上传响应
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "分块上传响应")
public class ChunkUploadResponse {

    /**
     * 上传ID
     */
    @Schema(description = "分块上传ID")
    private String uploadId;

    /**
     * 分块编号
     */
    @Schema(description = "分块编号")
    private Integer chunkNumber;

    /**
     * 上传状态
     */
    @Schema(description = "上传状态", example = "SUCCESS")
    private String status;

    /**
     * ETag (用于合并分块)
     */
    @Schema(description = "分块ETag标识")
    private String etag;

    /**
     * 分块大小
     */
    @Schema(description = "分块大小")
    private Long chunkSize;

    /**
     * 上传进度
     */
    @Schema(description = "整体上传进度百分比")
    private Double progress;

    /**
     * 已上传分块数
     */
    @Schema(description = "已上传分块数")
    private Integer uploadedChunks;

    /**
     * 总分块数
     */
    @Schema(description = "总分块数")
    private Integer totalChunks;

    /**
     * 已上传大小 (字节)
     */
    @Schema(description = "已上传大小")
    private Long uploadedSize;

    /**
     * 总文件大小 (字节)
     */
    @Schema(description = "总文件大小")
    private Long totalSize;

    /**
     * 分片路径
     */
    @Schema(description = "分片存储路径")
    private String chunkPath;

    /**
     * 分片MD5
     */
    @Schema(description = "分片MD5哈希值")
    private String chunkMD5;

    /**
     * 上传时间
     */
    @Schema(description = "分块上传时间")
    private LocalDateTime uploadTime;

    /**
     * 错误信息
     */
    @Schema(description = "错误信息")
    private String errorMessage;

    /**
     * 是否需要重试
     */
    @Schema(description = "是否建议重试")
    private Boolean shouldRetry = false;

    /**
     * 剩余重试次数
     */
    @Schema(description = "剩余重试次数")
    private Integer remainingRetries;

    /**
     * 预计完成时间
     */
    @Schema(description = "预计完成时间")
    private LocalDateTime estimatedCompletionTime;
}