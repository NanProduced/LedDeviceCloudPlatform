package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "素材基础信息（含文件信息）")
public class BasicMaterialInfoResponse {

    @Schema(description = "素材ID")
    private Long materialId;

    @Schema(description = "组织ID")
    private Long oid;

    @Schema(description = "文件ID")
    private String fileId;

    @Schema(description = "存储路径")
    private String storagePath;

    @Schema(description = "素材名称")
    private String materialName;

    @Schema(description = "素材类型")
    private String materialType;
}

