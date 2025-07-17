package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "终端组权限响应体")
@Data
public class TerminalGroupPermissionResponse {

    @Schema(description = "终端组ID")
    private Long tgid;

    @Schema(description = "终端组名称")
    private String terminalGroupName;

    @Schema(description = "是否有访问权限")
    private Boolean hasAccess;

    @Schema(description = "是否有管理权限")
    private Boolean hasManage;

    @Schema(description = "是否有设备控制权限")
    private Boolean hasDeviceControl;

    @Schema(description = "具体权限列表")
    private List<String> permissions;
}