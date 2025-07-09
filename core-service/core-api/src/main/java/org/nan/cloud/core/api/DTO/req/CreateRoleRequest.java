package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(name = "创建角色请求DTO")
@Data
public class CreateRoleRequest {

    @Schema(name = "角色名称")
    private String roleName;

    @Schema(name = "角色权限", description = "多选")
    private List<Long> permissions;
}
