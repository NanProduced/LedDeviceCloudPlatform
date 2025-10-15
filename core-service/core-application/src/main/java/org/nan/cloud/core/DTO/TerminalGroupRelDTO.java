package org.nan.cloud.core.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TerminalGroupRelDTO {
    
    private Long tgid;
    
    private String name;
    
    private Long parent;
    
    private String path;
    
    private Integer tgType;
    
    private String description;
}