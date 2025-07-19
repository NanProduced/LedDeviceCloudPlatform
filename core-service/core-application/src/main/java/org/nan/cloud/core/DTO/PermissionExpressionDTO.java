package org.nan.cloud.core.DTO;

import lombok.Data;
import org.nan.cloud.common.basic.model.BindingType;

import java.util.List;

/**
 * 权限表达式DTO
 */
@Data
public class PermissionExpressionDTO {
    
    /** 用户组ID */
    private Long ugid;
    
    /** 权限绑定表达式列表 */
    private List<PermissionBindingDTO> permissionBindings;
    
    /** 操作说明 */
    private String description;
    
    /** 是否执行智能冗余清理 */
    private Boolean enableRedundancyOptimization = true;
    
    /** 操作者ID */
    private Long operatorId;
    
    /** 组织ID */
    private Long oid;
    
    /**
     * 权限绑定项DTO
     */
    @Data
    public static class PermissionBindingDTO {
        
        /** 终端组ID */
        private Long tgid;
        
        /** 绑定类型 */
        private BindingType bindingType;
        
        /** 是否包含子终端组 */
        private Boolean includeChildren;
        
        /** 备注说明 */
        private String remarks;
    }
}