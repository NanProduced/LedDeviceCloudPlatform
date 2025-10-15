package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "创建终端组请求DTO")
@Data
public class CreateTerminalGroupRequest {

    @Schema(description = "父级终端组Id")
    @NotNull
    private Long parentTgid;

    @Schema(description = "终端组名称")
    @NotBlank
    private String terminalGroupName;

    @Schema(description = "终端组描述")
    private String description;
}