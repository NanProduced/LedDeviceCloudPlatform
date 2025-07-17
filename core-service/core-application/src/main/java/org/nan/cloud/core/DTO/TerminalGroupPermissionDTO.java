package org.nan.cloud.core.DTO;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TerminalGroupPermissionDTO {
    
    private Long tgid;
    
    private String terminalGroupName;
    
    private Boolean hasAccess;
    
    private Boolean hasManage;
    
    private Boolean hasDeviceControl;
    
    private List<String> permissions;
}