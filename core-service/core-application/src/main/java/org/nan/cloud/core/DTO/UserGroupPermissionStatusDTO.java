package org.nan.cloud.core.DTO;

import lombok.Builder;
import lombok.Data;
import org.nan.cloud.common.basic.model.BindingType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户组权限状态DTO
 */
@Data
@Builder
public class UserGroupPermissionStatusDTO {
    
    /** 用户组ID */
    private Long ugid;
    
    /** 用户组名称 */
    private String userGroupName;
    
    /** 权限绑定列表 */
    private List<PermissionBindingStatusDTO> permissionBindings;
    
    /** 统计信息 */
    private BindingStatisticsDTO statistics;
    
    /** 最后更新时间 */
    private LocalDateTime lastUpdateTime;
    
    /**
     * 权限绑定状态DTO
     */
    @Data
    @Builder
    public static class PermissionBindingStatusDTO {
        
        /** 绑定ID */
        private Long bindingId;
        
        /** 终端组ID */
        private Long tgid;
        
        /** 终端组名称 */
        private String terminalGroupName;
        
        /** 终端组路径 */
        private String terminalGroupPath;
        
        /** 绑定类型 */
        private BindingType bindingType;
        
        /** 是否包含子组 */
        private Boolean includeChildren;
        
        /** 层级深度 */
        private Integer depth;
        
        /** 父终端组ID */
        private Long parentTgid;
        
        /** 子终端组数量 */
        private Integer childCount;
        
        /** 实际生效状态 */
        private String effectiveStatus;
        
        /** 创建时间 */
        private LocalDateTime createTime;
        
        /** 更新时间 */
        private LocalDateTime updateTime;
        
        /** 创建者 */
        private String creator;
        
        /** 备注 */
        private String remarks;
    }
    
    /**
     * 绑定统计信息DTO
     */
    @Data
    @Builder
    public static class BindingStatisticsDTO {
        
        /** 总绑定数量 */
        private Integer totalBindings;
        
        /** INCLUDE类型绑定数量 */
        private Integer includeBindings;
        
        /** EXCLUDE类型绑定数量 */
        private Integer excludeBindings;
        
        /** 包含子组的绑定数量 */
        private Integer includeChildrenBindings;
        
        /** 覆盖的终端组总数 */
        private Integer totalCoveredTerminalGroups;
        
        /** 最大层级深度 */
        private Integer maxDepth;

    }
}