package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "批量绑定操作请求DTO")
@Data
public class BatchBindingOperationRequest {
    
    @Schema(description = "用户组ID")
    @NotNull
    private Long ugid;
    
    @Schema(description = "要添加权限的终端组")
    private List<TerminalGroupPermission> grantPermissions;
    
    @Schema(description = "要移除权限的终端组ID列表")
    private List<Long> revokeTerminalGroupIds;
    
    @Schema(description = "操作说明")
    private String description;
    
    @Schema(description = "终端组权限")
    @Data
    public static class TerminalGroupPermission {
        @Schema(description = "终端组ID")
        private Long tgid;
        
        @Schema(description = "是否包含子组", example = "false")
        private Boolean includeChildren = false;
        
        @Schema(description = "权限说明")
        private String description;
    }
}