package org.nan.cloud.core.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QueryTerminalGroupHistoryDTO {
    
    private Long tgid;
    
    private String operationType;
    
    private Long operatorId;
}