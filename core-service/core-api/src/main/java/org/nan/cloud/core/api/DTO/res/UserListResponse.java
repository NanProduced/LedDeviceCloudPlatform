package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import org.nan.cloud.core.api.DTO.common.RoleDTO;

import java.time.LocalDateTime;
import java.util.Set;

@Schema(description = "用户列表查询结果")
@Data
@Builder
public class UserListResponse {

    @Schema(description = "用户Id")
    private Long uid;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户组Id")
    private Long ugid;

    @Schema(description = "用户组名称")
    private String ugName;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "状态")
    private Integer active;

    @Schema(description = "用户角色")
    private Set<RoleDTO> roles;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
