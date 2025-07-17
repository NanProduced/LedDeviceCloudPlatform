package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "查询终端组历史请求DTO")
@Data
public class QueryTerminalGroupHistoryRequest {

    @Schema(description = "终端组ID")
    @NotNull
    private Long tgid;

    @Schema(description = "操作类型")
    private String operationType;

    @Schema(description = "操作用户ID")
    private Long operatorId;
}