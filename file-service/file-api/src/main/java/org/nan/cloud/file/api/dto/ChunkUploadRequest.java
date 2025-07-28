package org.nan.cloud.file.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 分块上传请求
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Schema(description = "分块上传请求")
public class ChunkUploadRequest {

    /**
     * 上传ID
     */
    @NotBlank(message = "上传ID不能为空")
    @Schema(description = "分块上传ID", required = true)
    private String uploadId;

    /**
     * 分块编号 (从1开始)
     */
    @NotNull(message = "分块编号不能为空")
    @Positive(message = "分块编号必须大于0")
    @Schema(description = "分块编号", required = true, example = "1")
    private Integer chunkNumber;

    /**
     * 分块MD5
     */
    @Schema(description = "分块数据MD5哈希值")
    private String chunkMD5;

    /**
     * 总分片数
     */
    @Schema(description = "总分片数", example = "10")
    private Integer totalChunks;

    /**
     * 分块大小 (字节)
     */
    @Schema(description = "分块实际大小")
    private Long chunkSize;

    /**
     * 是否最后一个分块
     */
    @Schema(description = "是否最后一个分块")
    private Boolean isLastChunk = false;

    /**
     * 重试次数
     */
    @Schema(description = "当前重试次数", example = "0")
    private Integer retryCount = 0;
}