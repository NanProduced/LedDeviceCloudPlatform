package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Schema(description = "查询素材列表响应 - 单个分享素材VO")
public class ListSharedMaterialResponse extends ListMaterialResponse {

    @Schema(description = "分享记录ID")
    private Long shareId;

    @Schema(description = "分享来源用户组ID")
    private Long sharedFrom;

    @Schema(description = "分享来源用户组名称")
    private String sharedFromGroupName;

    @Schema(description = "分享到的用户组ID")
    private Long sharedTo;

    @Schema(description = "分享到的用户组名称")
    private String sharedToGroupName;

    @Schema(description = "分享操作者ID")
    private Long sharedBy;

    @Schema(description = "分享操作者名称")
    private String sharedByUserName;

    @Schema(description = "分享时间")
    private LocalDateTime sharedTime;

    @Schema(description = "资源类型", example = "1:素材文件, 2:素材文件夹")
    private Integer resourceType;

}
