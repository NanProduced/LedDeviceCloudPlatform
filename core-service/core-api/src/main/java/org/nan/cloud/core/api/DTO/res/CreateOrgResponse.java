package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(name = "创建组织响应DTO")
@Data
@Builder
public class CreateOrgResponse {

    @Schema(name = "组织ID")
    private Long oid;

    @Schema(name = "组织名称")
    private String orgName;

    @Schema(name = "组织后缀", description = "用于登录时区分组织,组织内不可重名")
    private Integer suffix;

    @Schema(name = "用户ID")
    private Long uid;

    @Schema(name = "组织管理员用户名")
    private String username;

    @Schema(name = "组织管理员密码")
    private String password;

}
