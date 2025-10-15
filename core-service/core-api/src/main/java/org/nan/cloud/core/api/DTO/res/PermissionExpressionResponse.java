package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.nan.cloud.common.basic.model.BindingType;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "权限表达式操作响应DTO")
@Data
public class PermissionExpressionResponse {
    
    @Schema(description = "操作是否成功", example = "true")
    private Boolean success;
    
    @Schema(description = "操作消息", example = "权限表达式更新成功")
    private String message;
    
    @Schema(description = "用户组ID")
    private Long ugid;
    
    @Schema(description = "操作统计信息")
    private OperationStatistics statistics;
    
    @Schema(description = "优化后的权限绑定列表")
    private List<OptimizedBinding> optimizedBindings;
    
    @Schema(description = "操作详情列表")
    private List<OperationDetail> operationDetails;
    
    @Schema(description = "操作时间")
    private LocalDateTime operationTime;
    
    @Schema(description = "操作统计信息")
    @Data
    public static class OperationStatistics {
        
        @Schema(description = "原始绑定数量", example = "15")
        private Integer originalCount;
        
        @Schema(description = "优化后绑定数量", example = "8")
        private Integer optimizedCount;
        
        @Schema(description = "减少的冗余绑定数量", example = "7")
        private Integer redundancyRemoved;
        
        @Schema(description = "新增绑定数量", example = "3")
        private Integer addedCount;
        
        @Schema(description = "更新绑定数量", example = "2")
        private Integer updatedCount;
        
        @Schema(description = "删除绑定数量", example = "5")
        private Integer deletedCount;
        
        @Schema(description = "优化率百分比", example = "46.67")
        private Double optimizationRatio;
    }
    
    @Schema(description = "优化后的绑定项")
    @Data
    public static class OptimizedBinding {
        
        @Schema(description = "终端组ID")
        private Long tgid;
        
        @Schema(description = "终端组名称")
        private String terminalGroupName;
        
        @Schema(description = "绑定类型")
        private BindingType bindingType;
        
        @Schema(description = "是否包含子组")
        private Boolean includeChildren;
        
        @Schema(description = "层级深度")
        private Integer depth;
        
        @Schema(description = "父终端组ID")
        private Long parentTgid;
        
        @Schema(description = "是否为智能优化的结果")
        private Boolean optimized;
    }
    
    @Schema(description = "操作详情")
    @Data
    public static class OperationDetail {
        
        @Schema(description = "终端组ID")
        private Long tgid;
        
        @Schema(description = "终端组名称")
        private String terminalGroupName;
        
        @Schema(description = "操作类型", allowableValues = {"CREATE", "UPDATE", "DELETE", "REDUNDANCY_REMOVED"})
        private String operationType;
        
        @Schema(description = "原绑定状态")
        private String oldBinding;
        
        @Schema(description = "新绑定状态")
        private String newBinding;
        
        @Schema(description = "操作原因")
        private String reason;
        
        @Schema(description = "是否成功")
        private Boolean success;
        
        @Schema(description = "错误信息")
        private String errorMessage;
    }
}