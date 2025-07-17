package org.nan.cloud.core.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchTerminalGroupDTO {
    
    private String keyword;
    
    private Integer tgType;
    
    private Long oid;
    
    private Long userId;
}