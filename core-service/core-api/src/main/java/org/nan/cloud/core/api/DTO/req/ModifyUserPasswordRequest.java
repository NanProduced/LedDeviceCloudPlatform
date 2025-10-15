package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Schema(description = "修改用户密码请求")
@Data
public class ModifyUserPasswordRequest {

    @Schema(description = "旧密码")
    @NotEmpty
    private String oldPassword;

    @Schema(description = "新密码")
    @NotEmpty
    private String newPassword;
}
