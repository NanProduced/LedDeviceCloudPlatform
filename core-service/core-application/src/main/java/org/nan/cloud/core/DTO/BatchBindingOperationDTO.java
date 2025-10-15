package org.nan.cloud.core.DTO;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BatchBindingOperationDTO {
    
    /**
     * 用户组ID
     */
    private Long ugid;
    
    /**
     * 要添加权限的终端组
     */
    private List<TerminalGroupPermissionDTO> grantPermissions;
    
    /**
     * 要移除权限的终端组ID列表
     */
    private List<Long> revokeTerminalGroupIds;
    
    /**
     * 操作者ID
     */
    private Long operatorId;
    
    /**
     * 操作说明
     */
    private String operationDescription;
    
    @Data
    @Builder
    public static class TerminalGroupPermissionDTO {
        /**
         * 终端组ID
         */
        private Long tgid;
        
        /**
         * 是否包含子组
         */
        private Boolean includeChildren;
        
        /**
         * 权限说明
         */
        private String description;
    }
}