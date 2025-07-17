package org.nan.cloud.core.DTO;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BatchTerminalGroupOperationDTO {
    
    private String operationType;
    
    private List<TerminalGroupOperationItem> items;
    
    private Long operatorId;
    
    private Long oid;
    
    @Data
    @Builder
    public static class TerminalGroupOperationItem {
        private Long tgid;
        private String terminalGroupName;
        private Long parentTgid;
        private String description;
    }
}