package org.nan.cloud.core.DTO;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BatchOperationDTO {
    
    private Integer totalCount;
    
    private Integer successCount;
    
    private Integer failureCount;
    
    private List<OperationResult> successResults;
    
    private List<OperationResult> failureResults;
    
    @Data
    @Builder
    public static class OperationResult {
        private Long itemId;
        private String itemName;
        private String operationType;
        private String message;
    }
}