package org.nan.cloud.core.api.DTO.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "角色")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleDTO {

    @Schema(description = "角色Id")
    private Long rid;

    @Schema(description = "组织Id")
    private Long oid;

    @Schema(description = "组织名称")
    private String roleName;
}
