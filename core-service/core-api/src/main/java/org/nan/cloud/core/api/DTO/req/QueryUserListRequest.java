package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "查询用户列表请求")
@Data
public class QueryUserListRequest {

    @Schema(description = "查询的用户组Id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long ugid;

    @Schema(description = "查询是否包含子组", defaultValue = "false")
    private boolean includeSubGroups;

    @Schema(description = "用户名关键字")
    private String userNameKeyword;

    @Schema(description = "邮箱关键字")
    private String emailKeyword;

    @Schema(description = "手机号码关键字")
    private String phoneKeyword;



}
