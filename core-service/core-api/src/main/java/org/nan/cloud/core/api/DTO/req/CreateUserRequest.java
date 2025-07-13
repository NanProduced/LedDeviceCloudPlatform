package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "创建用户请求DTO")
@Data
public class CreateUserRequest {

    @Schema(description = "组织Id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long oid;

    @Schema(description = "用户组Id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long ugid;

    @Schema(description = "角色Id列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    private List<Long> roles;

    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String username;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String password;

    @Schema(description = "邮箱", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;



}
