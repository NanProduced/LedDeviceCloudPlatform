package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import org.nan.cloud.core.api.DTO.common.RoleDTO;

import java.util.List;

@Schema(description = "获取用户可分配角色")
@Data
@Builder
public class VisibleRolesResponse {

    @Schema(description = "用户Id")
    private Long uid;

    @Schema(description = "可分配角色（有权限角色）")
    private List<RoleDTO> visibleRoles;

}
