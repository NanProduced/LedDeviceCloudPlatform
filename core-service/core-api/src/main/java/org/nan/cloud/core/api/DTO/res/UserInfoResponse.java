package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.nan.cloud.core.api.DTO.common.RoleDTO;

import java.time.LocalDateTime;
import java.util.List;

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

    @Schema(description = "用户角色")
    private List<RoleDTO> roles;

    @Schema(description = "用户组Id")
    private Long ugid;

    @Schema(description = "用户组名称")
    private String ugName;

    @Schema(description = "用户邮箱")
    private String email;

    @Schema(description = "用户电话号码")
    private String phone;

    @Schema(description = "账号状态, 0:正常;1:封禁")
    private Integer active;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
