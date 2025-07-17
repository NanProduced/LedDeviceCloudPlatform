package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "终端设备列表响应体")
@Data
public class TerminalDeviceListResponse {

    @Schema(description = "设备ID")
    private Long deviceId;

    @Schema(description = "设备编码")
    private String deviceCode;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "设备类型")
    private Integer deviceType;

    @Schema(description = "设备状态")
    private Integer deviceStatus;

    @Schema(description = "终端组ID")
    private Long tgid;

    @Schema(description = "终端组名称")
    private String terminalGroupName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}