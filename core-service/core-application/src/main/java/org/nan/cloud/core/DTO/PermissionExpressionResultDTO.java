package org.nan.cloud.core.DTO;

import lombok.Builder;
import lombok.Data;
import org.nan.cloud.common.basic.model.BindingType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 权限表达式操作结果DTO
 */
@Data
@Builder
public class PermissionExpressionResultDTO {
    
    /** 操作是否成功 */
    private Boolean success;
    
    /** 操作消息 */
    private String message;
    
    /** 用户组ID */
    private Long ugid;
    
    /** 操作统计信息 */
    private OperationStatisticsDTO statistics;
    
    /** 优化后的权限绑定列表 */
    private List<OptimizedBindingDTO> optimizedBindings;
    
    /** 操作详情列表 */
    private List<OperationDetailDTO> operationDetails;
    
    /** 操作时间 */
    private LocalDateTime operationTime;
    
    /**
     * 操作统计信息DTO
     */
    @Data
    @Builder
    public static class OperationStatisticsDTO {
        
        /** 原始绑定数量 */
        private Integer originalCount;
        
        /** 优化后绑定数量 */
        private Integer optimizedCount;
        
        /** 减少的冗余绑定数量 */
        private Integer redundancyRemoved;
        
        /** 新增绑定数量 */
        private Integer addedCount;
        
        /** 更新绑定数量 */
        private Integer updatedCount;
        
        /** 删除绑定数量 */
        private Integer deletedCount;
        
        /** 优化率百分比 */
        private Double optimizationRatio;
    }
    
    /**
     * 优化后的绑定项DTO
     */
    @Data
    @Builder
    public static class OptimizedBindingDTO {
        
        /** 终端组ID */
        private Long tgid;
        
        /** 终端组名称 */
        private String terminalGroupName;
        
        /** 绑定类型 */
        private BindingType bindingType;
        
        /** 是否包含子组 */
        private Boolean includeChildren;
        
        /** 层级深度 */
        private Integer depth;
        
        /** 父终端组ID */
        private Long parentTgid;
        
        /** 是否为智能优化的结果 */
        private Boolean optimized;
    }
    
    /**
     * 操作详情DTO
     */
    @Data
    @Builder
    public static class OperationDetailDTO {
        
        /** 终端组ID */
        private Long tgid;
        
        /** 终端组名称 */
        private String terminalGroupName;
        
        /** 操作类型 */
        private String operationType;
        
        /** 原绑定状态 */
        private String oldBinding;
        
        /** 新绑定状态 */
        private String newBinding;
        
        /** 操作原因 */
        private String reason;
        
        /** 是否成功 */
        private Boolean success;
        
        /** 错误信息 */
        private String errorMessage;
    }
}