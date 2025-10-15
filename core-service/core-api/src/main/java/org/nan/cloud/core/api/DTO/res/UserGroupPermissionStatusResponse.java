package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.nan.cloud.common.basic.model.BindingType;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "用户组权限状态响应DTO")
@Data
public class UserGroupPermissionStatusResponse {
    
    @Schema(description = "用户组ID")
    private Long ugid;
    
    @Schema(description = "用户组名称")
    private String userGroupName;
    
    @Schema(description = "权限绑定列表")
    private List<PermissionBindingStatus> permissionBindings;
    
    @Schema(description = "统计信息")
    private BindingStatistics statistics;
    
    @Schema(description = "最后更新时间")
    private LocalDateTime lastUpdateTime;
    
    @Schema(description = "权限绑定状态")
    @Data
    public static class PermissionBindingStatus {
        
        @Schema(description = "绑定ID")
        private Long bindingId;
        
        @Schema(description = "终端组ID")
        private Long tgid;
        
        @Schema(description = "终端组名称")
        private String terminalGroupName;
        
        @Schema(description = "终端组路径", example = "/root/device/led")
        private String terminalGroupPath;
        
        @Schema(description = "绑定类型")
        private BindingType bindingType;
        
        @Schema(description = "是否包含子组")
        private Boolean includeChildren;
        
        @Schema(description = "层级深度")
        private Integer depth;
        
        @Schema(description = "父终端组ID")
        private Long parentTgid;
        
        @Schema(description = "创建时间")
        private LocalDateTime createTime;
        
        @Schema(description = "更新时间")
        private LocalDateTime updateTime;
        
        @Schema(description = "备注")
        private String remarks;
    }
    
    @Schema(description = "绑定统计信息")
    @Data
    public static class BindingStatistics {
        
        @Schema(description = "总绑定数量")
        private Integer totalBindings;
        
        @Schema(description = "INCLUDE类型绑定数量")
        private Integer includeBindings;
        
        @Schema(description = "EXCLUDE类型绑定数量")
        private Integer excludeBindings;
        
        @Schema(description = "包含子组的绑定数量")
        private Integer includeChildrenBindings;
        
        @Schema(description = "覆盖的终端组总数")
        private Integer totalCoveredTerminalGroups;
        
        @Schema(description = "最大层级深度")
        private Integer maxDepth;
    }
}