package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户信息响应DTO")
@Data
public class UserInfoResponse {

    @Schema(description = "用户Id")
    private Long uid;

    @Schema(description = "用户名称")
    private String username;

    @Schema(description = "组织Id")
    private Long oid;

    @Schema(description = "组织名称")
    private String orgName;

    @Schema(description = "用户组Id")
    private Long ugid;

    @Schema(description = "用户组名称")
    private String ugName;

    @Schema(description = "用户邮箱")
    private String email;

    @Schema(description = "用户电话号码")
    private String phone;
}
