package org.nan.cloud.core.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateTerminalGroupDTO {
    
    private String terminalGroupName;
    
    private Long parentTgid;
    
    private String description;
    
    private Long oid;
    
    private Long creatorId;
}