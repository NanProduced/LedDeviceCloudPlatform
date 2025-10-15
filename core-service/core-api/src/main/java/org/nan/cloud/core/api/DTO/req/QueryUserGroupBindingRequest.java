package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "查询用户组绑定请求DTO")
@Data
public class QueryUserGroupBindingRequest {

    @Schema(description = "终端组ID")
    @NotNull
    private Long tgid;

    @Schema(description = "用户组名称（模糊搜索）")
    private String userGroupName;
}