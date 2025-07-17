package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "查询终端设备请求DTO")
@Data
public class QueryTerminalDeviceRequest {

    @Schema(description = "终端组ID")
    @NotNull
    private Long tgid;

    @Schema(description = "设备名称（模糊搜索）")
    private String deviceName;

    @Schema(description = "设备状态")
    private Integer deviceStatus;
}