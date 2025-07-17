package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "绑定设备到终端组请求DTO")
@Data
public class BindDevicesRequest {

    @Schema(description = "终端组ID")
    @NotNull
    private Long tgid;

    @Schema(description = "设备ID列表")
    @NotEmpty
    private List<Long> deviceIds;

    @Schema(description = "是否强制绑定")
    private Boolean force = false;
}