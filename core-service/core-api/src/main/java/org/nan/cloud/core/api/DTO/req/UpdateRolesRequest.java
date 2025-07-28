package org.nan.cloud.core.api.DTO.req;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "更新角色请求")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateRolesRequest {

    @Schema(description = "角色Id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long rid;

    @Schema(description = "更新操作权限Id(为覆盖操作，如果不更新权限只更新角色信息则传null)")
    private List<Long> operationPermissionIds;

    @Schema(description = "更新角色名称")
    private String name;

    @Schema(description = "更新角色描述")
    private String description;
}
