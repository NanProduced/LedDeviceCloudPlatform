package org.nan.cloud.core.DTO;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 绑定冲突检查结果
 */
@Data
@Builder
public class BindingConflictCheckResult {
    
    /**
     * 是否存在冲突
     */
    private Boolean hasConflict;
    
    /**
     * 冲突类型
     */
    private ConflictType conflictType;
    
    /**
     * 冲突的终端组ID列表
     */
    private List<Long> conflictTerminalGroupIds;
    
    /**
     * 冲突描述信息
     */
    private String conflictMessage;
    
    /**
     * 建议的解决方案
     */
    private ConflictResolution suggestedResolution;
    
    /**
     * 冲突类型枚举
     */
    public enum ConflictType {
        /**
         * 父组已绑定（包含子组），现在要绑定子组
         */
        PARENT_ALREADY_BOUND_WITH_CHILDREN,
        
        /**
         * 子组已绑定，现在要绑定父组（包含子组）
         */
        CHILDREN_ALREADY_BOUND,
        
        /**
         * 直接重复绑定
         */
        DIRECT_DUPLICATE
    }
    
    /**
     * 冲突解决方案枚举
     */
    public enum ConflictResolution {
        /**
         * 拒绝绑定
         */
        REJECT_BINDING,
        
        /**
         * 替换现有绑定
         */
        REPLACE_EXISTING,
        
        /**
         * 移除冲突的子组绑定
         */
        REMOVE_CONFLICTING_CHILDREN,
        
        /**
         * 用户确认后继续
         */
        CONFIRM_AND_PROCEED
    }
}