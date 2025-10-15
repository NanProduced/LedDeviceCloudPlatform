package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Schema(description = "权限拒绝请求DTO")
@Data
public class PermissionDenyRequest {
    
    @Schema(description = "目标类型", example = "USER", allowableValues = {"USER", "ROLE"})
    @NotBlank(message = "目标类型不能为空")
    private String targetType;
    
    @Schema(description = "目标ID（用户ID或角色ID）", example = "123")
    @NotNull(message = "目标ID不能为空")
    private Long targetId;
    
    @Schema(description = "组织ID", example = "456")
    @NotNull(message = "组织ID不能为空")
    private Long orgId;
    
    @Schema(description = "接口URL", example = "/core/api/user/create")
    @NotBlank(message = "接口URL不能为空")
    private String url;
    
    @Schema(description = "HTTP方法", example = "POST", allowableValues = {"GET", "POST", "PUT", "DELETE"})
    @NotBlank(message = "HTTP方法不能为空")
    private String method;
    
    @Schema(description = "拒绝原因", example = "违反公司政策")
    private String reason;
    
    @Schema(description = "临时禁用时长（小时），如果不填则为永久禁用", example = "24")
    @Positive(message = "禁用时长必须大于0")
    private Integer durationHours;
}