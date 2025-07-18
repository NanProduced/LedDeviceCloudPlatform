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
    
    @Schema(description = "要添加权限的终端组ID列表")
    private List<Long> grantTerminalGroupIds;
    
    @Schema(description = "要移除权限的终端组ID列表")
    private List<Long> revokeTerminalGroupIds;
    
    @Schema(description = "操作说明")
    private String description;
}