package org.nan.cloud.core.DTO;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BindUserGroupDTO {
    
    private Long tgid;
    
    private List<Long> ugids;
    
    private Boolean includeSub;
    
    private Long operatorId;
}