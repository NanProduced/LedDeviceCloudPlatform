package org.nan.cloud.core.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateTerminalGroupDTO {
    
    private Long tgid;
    
    private String terminalGroupName;
    
    private String description;
    
    private Long updatorId;
}