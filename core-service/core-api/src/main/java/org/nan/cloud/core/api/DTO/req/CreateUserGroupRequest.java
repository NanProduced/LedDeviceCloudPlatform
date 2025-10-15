package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "创建用户组请求DTO")
@Data
public class CreateUserGroupRequest {

    @Schema(description = "父级组Id")
    @NotNull
    private Long parentUgid;

    @Schema(description = "用户组名称")
    @NotBlank
    private String userGroupName;

    @Schema(description = "用户组描述")
    private String description;
}
