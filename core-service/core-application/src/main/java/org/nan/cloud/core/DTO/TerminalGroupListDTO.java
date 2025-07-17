package org.nan.cloud.core.DTO;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TerminalGroupListDTO {
    
    private Long tgid;
    
    private String terminalGroupName;
    
    private Long parent;
    
    private String parentName;
    
    private String description;
    
    private Integer tgType;
    
    private Integer childrenCount;
    
    private LocalDateTime createTime;
}