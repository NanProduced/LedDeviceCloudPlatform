package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "移动用户请求DTO")
@Data
public class MoveUserRequest {

    @Schema(description = "用户Id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long uid;

    @Schema(description = "源用户组Id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long sourceUgid;

    @Schema(description = "目标用户组Id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long targetUgid;
}
