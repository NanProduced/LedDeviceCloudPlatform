package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "搜索终端组请求DTO")
@Data
public class SearchTerminalGroupRequest {

    @Schema(description = "搜索关键词")
    @NotBlank
    private String keyword;
}