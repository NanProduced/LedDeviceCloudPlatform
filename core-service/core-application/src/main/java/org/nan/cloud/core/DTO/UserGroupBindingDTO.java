package org.nan.cloud.core.DTO;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserGroupBindingDTO {
    
    private Long id;
    
    private Long tgid;
    
    private String terminalGroupName;
    
    private Long ugid;
    
    private String userGroupName;
    
    private Boolean includeChildren;
    
    private Long creatorId;
    
    private LocalDateTime createTime;
}