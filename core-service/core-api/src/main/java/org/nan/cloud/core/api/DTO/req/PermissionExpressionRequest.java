package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.nan.cloud.common.basic.model.BindingType;

import java.util.List;

@Schema(description = "权限表达式请求DTO")
@Data
public class PermissionExpressionRequest {
    
    @Schema(description = "用户组ID", example = "1001")
    @NotNull(message = "用户组ID不能为空")
    private Long ugid;
    
    @Schema(description = "权限绑定表达式列表")
    @NotEmpty(message = "权限表达式不能为空")
    @Valid
    private List<PermissionBinding> permissionBindings;
    
    @Schema(description = "操作说明")
    private String description;
    
    @Schema(description = "是否执行智能冗余清理", example = "true")
    private Boolean enableRedundancyOptimization = true;
    
    @Schema(description = "权限绑定项")
    @Data
    public static class PermissionBinding {
        
        @Schema(description = "终端组ID", example = "2001")
        @NotNull(message = "终端组ID不能为空")
        private Long tgid;
        
        @Schema(description = "绑定类型", example = "INCLUDE", allowableValues = {"INCLUDE", "EXCLUDE"})
        @NotNull(message = "绑定类型不能为空")
        private BindingType bindingType;
        
        @Schema(description = "是否包含子终端组", example = "true")
        @NotNull(message = "包含子组标志不能为空")
        private Boolean includeChildren;
        
        @Schema(description = "备注说明")
        private String remarks;
    }
}