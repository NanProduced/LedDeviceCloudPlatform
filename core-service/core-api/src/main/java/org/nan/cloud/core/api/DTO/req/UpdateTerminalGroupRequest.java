package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "更新终端组请求DTO")
@Data
public class UpdateTerminalGroupRequest {

    @Schema(description = "终端组ID")
    @NotNull
    private Long tgid;

    @Schema(description = "终端组名称")
    @NotBlank
    private String terminalGroupName;

    @Schema(description = "终端组描述")
    private String description;
}