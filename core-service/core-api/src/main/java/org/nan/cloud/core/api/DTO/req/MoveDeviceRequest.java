package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "移动设备到其他终端组请求DTO")
@Data
public class MoveDeviceRequest {

    @Schema(description = "当前终端组ID")
    @NotNull
    private Long fromTgid;

    @Schema(description = "目标终端组ID")
    @NotNull
    private Long toTgid;

    @Schema(description = "设备ID")
    @NotNull
    private Long deviceId;
}