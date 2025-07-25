package org.nan.cloud.file.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 分块上传完成请求
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Schema(description = "分块上传完成请求")
public class ChunkUploadCompleteRequest {

    /**
     * 上传ID
     */
    @NotBlank(message = "上传ID不能为空")
    @Schema(description = "分块上传ID", required = true)
    private String uploadId;

    /**
     * 分块信息列表
     */
    @NotEmpty(message = "分块信息列表不能为空")
    @Schema(description = "分块信息列表", required = true)
    private List<ChunkInfo> chunks;

    /**
     * 文件MD5 (可选，用于校验)
     */
    @Schema(description = "完整文件MD5哈希值")
    private String fileMd5;

    /**
     * 是否立即处理
     */
    @Schema(description = "是否立即进行后处理")
    private Boolean immediateProcess = true;

    /**
     * 分块信息
     */
    @Data
    @Schema(description = "分块信息")
    public static class ChunkInfo {
        
        /**
         * 分块编号
         */
        @Schema(description = "分块编号", required = true)
        private Integer chunkNumber;

        /**
         * 分块ETag
         */
        @Schema(description = "分块ETag", required = true)
        private String etag;

        /**
         * 分块大小
         */
        @Schema(description = "分块大小")
        private Long chunkSize;

        /**
         * 分块MD5
         */
        @Schema(description = "分块MD5")
        private String chunkMd5;
    }
}