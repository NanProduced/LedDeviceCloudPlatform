package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "批量绑定操作响应DTO")
@Data
public class BatchBindingOperationResponse {
    
    @Schema(description = "操作是否成功")
    private Boolean success;
    
    @Schema(description = "操作消息")
    private String message;
    
    @Schema(description = "操作统计")
    private OperationStatistics statistics;
    
    @Schema(description = "操作详情")
    private List<OperationDetail> details;
    
    @Schema(description = "操作统计")
    @Data
    public static class OperationStatistics {
        @Schema(description = "添加权限数量")
        private Integer grantedCount;
        
        @Schema(description = "移除权限数量")
        private Integer revokedCount;
        
        @Schema(description = "无变化数量")
        private Integer noChangeCount;
    }
    
    @Schema(description = "操作详情")
    @Data
    public static class OperationDetail {
        @Schema(description = "终端组ID")
        private Long tgid;
        
        @Schema(description = "终端组名称")
        private String terminalGroupName;
        
        @Schema(description = "操作结果")
        private String result;
        
        @Schema(description = "操作说明")
        private String description;
    }
}