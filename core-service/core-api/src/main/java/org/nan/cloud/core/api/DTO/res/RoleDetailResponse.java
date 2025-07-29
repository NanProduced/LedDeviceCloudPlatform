package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "角色详情响应")
public class RoleDetailResponse {

    @Schema(description = "角色Id")
    private Long rid;

    @Schema(description = "角色名（后端业务由于区分唯一角色的名称，不显示）")
    private String roleName;

    @Schema(description = "前端显示的角色名（用户设置的）")
    private String displayName;

    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "角色创建者用户Id")
    private Long creatorId;

    @Schema(description = "角色创建者用户名称")
    private String creatorName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "角色更新者用户Id")
    private Long updaterId;

    @Schema(description = "角色更新者用户名称")
    private String updaterName;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "角色的操作权限")
    private List<OperationPermissionResponse> operationPermissions;
}
