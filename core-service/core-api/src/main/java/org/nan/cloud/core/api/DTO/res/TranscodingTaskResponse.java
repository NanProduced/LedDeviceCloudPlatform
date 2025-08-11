package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 转码任务查询响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "转码任务查询响应")
public class TranscodingTaskResponse {

    @Schema(description = "转码任务列表")
    private List<TranscodingTaskInfo> tasks;

    @Schema(description = "总数量")
    private Integer total;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "转码任务信息")
    public static class TranscodingTaskInfo {

        @Schema(description = "任务ID")
        private String taskId;

        @Schema(description = "转码状态")
        private String status;

        @Schema(description = "转码进度(0-100)")
        private Integer progress;

        @Schema(description = "转码预设")
        private String transcodePreset;

        @Schema(description = "源素材信息")
        private MaterialInfo sourceMaterial;

        @Schema(description = "转码后素材信息")
        private MaterialInfo targetMaterial;

        @Schema(description = "转码详情信息")
        private TranscodingDetailInfo transcodingDetail;

        @Schema(description = "创建时间")
        private LocalDateTime createTime;

        @Schema(description = "完成时间")
        private LocalDateTime completeTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "素材信息")
    public static class MaterialInfo {

        @Schema(description = "素材ID")
        private Long materialId;

        @Schema(description = "素材名称")
        private String materialName;

        @Schema(description = "文件ID")
        private String fileId;

        @Schema(description = "文件大小(字节)")
        private Long fileSize;

        @Schema(description = "文件类型")
        private String mimeType;

        @Schema(description = "文件扩展名")
        private String fileExtension;

        @Schema(description = "存储路径")
        private String storagePath;

        @Schema(description = "缩略图路径")
        private String thumbnailPath;

        @Schema(description = "MD5哈希")
        private String md5Hash;

        @Schema(description = "创建时间")
        private LocalDateTime createTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "转码详情信息")
    public static class TranscodingDetailInfo {

        @Schema(description = "转码详情ID")
        private String transcodingDetailId;

        @Schema(description = "转码耗时(毫秒)")
        private Long durationMs;

        @Schema(description = "视频时长(秒)")
        private Double videoDurationSeconds;

        @Schema(description = "源视频分辨率")
        private String sourceResolution;

        @Schema(description = "目标视频分辨率")
        private String targetResolution;

        @Schema(description = "源视频编码")
        private String sourceVideoCodec;

        @Schema(description = "目标视频编码")
        private String targetVideoCodec;

        @Schema(description = "压缩比")
        private Double compressionRatio;

        @Schema(description = "质量评分")
        private Integer qualityScore;

        @Schema(description = "错误信息")
        private String errorMessage;
    }
}