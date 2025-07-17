package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Schema(description = "批量权限检查请求DTO")
@Data
public class BatchPermissionCheckRequest {

    @Schema(description = "终端组ID列表")
    @NotEmpty
    private List<Long> tgids;

    @Schema(description = "权限类型（可选）")
    private String permissionType;
}