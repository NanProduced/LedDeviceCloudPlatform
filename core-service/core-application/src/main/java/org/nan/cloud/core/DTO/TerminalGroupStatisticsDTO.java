package org.nan.cloud.core.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TerminalGroupStatisticsDTO {
    
    private Long tgid;
    
    private String terminalGroupName;
    
    private Integer directChildCount;
    
    private Integer totalChildCount;
    
    private Integer bindingUserGroupCount;
}