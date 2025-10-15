package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Schema(description = "查询素材列表响应 - 单个素材VO")
public class ListMaterialResponse {

    @Schema(description = "素材ID")
    private Long mid;

    @Schema(description = "素材文件名")
    private String materialName;

    @Schema(description = "文件ID")
    private String fileId;

    @Schema(description = "素材类型", example = "IMAGE/VIDEO/AUDIO/DOCUMENT")
    private String materialType;

    @Schema(description = "文件大小(字节)")
    private Long fileSize;

    @Schema(description = "文件大小(格式化)", example = "2.3 MB")
    private String fileSizeFormatted;

    @Schema(description = "MIME类型", example = "video/mp4")
    private String mimeType;

    @Schema(description = "文件扩展名", example = "mp4")
    private String fileExtension;

    @Schema(description = "文件状态", example = "1:已完成, 2:处理中, 3:失败")
    private Integer fileStatus;

    @Schema(description = "文件状态描述", example = "已完成")
    private String fileStatusDesc;

    @Schema(description = "处理进度(仅处理中状态有效)", example = "65")
    private Integer processProgress;

    @Schema(description = "素材描述")
    private String description;

    @Schema(description = "使用次数")
    private Long usageCount;

    @Schema(description = "所属用户组ID")
    private Long ugid;

    @Schema(description = "所属文件夹ID")
    private Long fid;

    @Schema(description = "上传者ID")
    private Long uploadedBy;

    @Schema(description = "上传者名称")
    private String uploaderName;

    @Schema(description = "上传时间")
    private LocalDateTime uploadTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
