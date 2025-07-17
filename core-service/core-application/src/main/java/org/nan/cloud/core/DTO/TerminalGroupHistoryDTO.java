package org.nan.cloud.core.DTO;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TerminalGroupHistoryDTO {
    
    private Long id;
    
    private Long tgid;
    
    private String terminalGroupName;
    
    private String operationType;
    
    private String operationDescription;
    
    private Long operatorId;
    
    private String operatorName;
    
    private String beforeData;
    
    private String afterData;
    
    private LocalDateTime operationTime;
}