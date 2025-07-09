package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(name = "创建组织请求DTO")
@Data
public class CreateOrgRequest {

    @Schema(name = "组织名称")
    private String orgName;

    @Schema(name = "组织备注")
    private String remark;

    @Schema(name = "组织管理员用户名")
    private String managerName;

    @Schema(name = "组织管理员邮箱")
    private String email;

    @Schema(name = "组织管理员电话")
    private String phone;
}
