package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "终端组历史记录响应体")
@Data
public class TerminalGroupHistoryResponse {

    @Schema(description = "历史记录ID")
    private Long id;

    @Schema(description = "终端组ID")
    private Long tgid;

    @Schema(description = "终端组名称")
    private String terminalGroupName;

    @Schema(description = "操作类型")
    private String operationType;

    @Schema(description = "操作描述")
    private String operationDescription;

    @Schema(description = "操作用户ID")
    private Long operatorId;

    @Schema(description = "操作用户名称")
    private String operatorName;

    @Schema(description = "操作前数据")
    private String beforeData;

    @Schema(description = "操作后数据")
    private String afterData;

    @Schema(description = "操作时间")
    private LocalDateTime operationTime;
}