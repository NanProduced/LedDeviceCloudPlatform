package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "给指定用户分配角色DTO")
@Data
public class AssignRolesRequest {

    @Schema(description = "目标用户Id")
    private Long targetUid;

    @Schema(description = "角色Id")
    private List<Long> rids;
}
