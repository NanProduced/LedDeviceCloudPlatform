package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "创建组织请求DTO")
@Data
public class CreateOrgRequest {

    @Schema(description = "组织名称")
    private String orgName;

    @Schema(description = "组织备注")
    private String remark;

    @Schema(description = "组织管理员用户名")
    private String managerName;

    @Schema(description = "组织管理员邮箱")
    private String email;

    @Schema(description = "组织管理员电话")
    private String phone;
}
