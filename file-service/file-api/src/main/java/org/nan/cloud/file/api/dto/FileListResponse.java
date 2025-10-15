package org.nan.cloud.file.api.dto;

import lombok.Data;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 文件列表响应
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "文件列表响应")
public class FileListResponse {

    /**
     * 文件列表
     */
    @Schema(description = "文件列表")
    private List<FileInfoResponse> files;

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
    private FileStatistics statistics;

    /**
     * 文件统计信息
     */
    @Data
    @Builder
    @Schema(description = "文件统计信息")
    public static class FileStatistics {
        
        /**
         * 总文件数
         */
        @Schema(description = "总文件数")
        private Long totalFiles;

        /**
         * 总文件大小 (字节)
         */
        @Schema(description = "总文件大小")
        private Long totalSize;

        /**
         * 平均文件大小 (字节)
         */
        @Schema(description = "平均文件大小")
        private Long avgSize;

        /**
         * 视频文件数
         */
        @Schema(description = "视频文件数")
        private Long videoCount;

        /**
         * 图片文件数
         */
        @Schema(description = "图片文件数")
        private Long imageCount;

        /**
         * 音频文件数
         */
        @Schema(description = "音频文件数")
        private Long audioCount;

        /**
         * 文档文件数
         */
        @Schema(description = "文档文件数")
        private Long documentCount;

        /**
         * 其他类型文件数
         */
        @Schema(description = "其他类型文件数")
        private Long otherCount;
    }
}