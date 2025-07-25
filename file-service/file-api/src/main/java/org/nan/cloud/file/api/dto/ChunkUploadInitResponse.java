package org.nan.cloud.file.api.dto;

import lombok.Data;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分块上传初始化响应
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "分块上传初始化响应")
public class ChunkUploadInitResponse {

    /**
     * 上传ID
     */
    @Schema(description = "分块上传唯一标识")
    private String uploadId;

    /**
     * 文件ID
     */
    @Schema(description = "文件ID")
    private String fileId;

    /**
     * 分块数量
     */
    @Schema(description = "总分块数量")
    private Integer totalChunks;

    /**
     * 分块大小 (字节)
     */
    @Schema(description = "分块大小")
    private Long chunkSize;

    /**
     * 已上传的分块列表
     */
    @Schema(description = "已上传的分块列表")
    private List<Integer> uploadedChunks;

    /**
     * 上传URL列表
     */
    @Schema(description = "预签名上传URL列表")
    private List<ChunkUploadUrl> uploadUrls;

    /**
     * 上传过期时间
     */
    @Schema(description = "上传链接过期时间")
    private LocalDateTime expirationTime;

    /**
     * 是否秒传
     */
    @Schema(description = "是否文件已存在(秒传)")
    private Boolean isQuickUpload;

    /**
     * 秒传文件信息
     */
    @Schema(description = "秒传时的文件信息")
    private FileUploadResponse quickUploadFile;

    /**
     * 分块上传URL信息
     */
    @Data
    @Builder
    @Schema(description = "分块上传URL信息")
    public static class ChunkUploadUrl {
        
        /**
         * 分块编号 (从1开始)
         */
        @Schema(description = "分块编号")
        private Integer chunkNumber;

        /**
         * 预签名上传URL
         */
        @Schema(description = "预签名上传URL")
        private String uploadUrl;

        /**
         * 分块大小
         */
        @Schema(description = "分块大小")
        private Long chunkSize;

        /**
         * 分块开始位置
         */
        @Schema(description = "分块开始位置")
        private Long startOffset;

        /**
         * 分块结束位置
         */
        @Schema(description = "分块结束位置")
        private Long endOffset;
    }
}