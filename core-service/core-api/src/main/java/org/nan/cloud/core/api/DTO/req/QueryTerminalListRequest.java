package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "查询终端列表请求")
@Data
public class QueryTerminalListRequest {

    @Schema(description = "查询的终端组Id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long tgid;

    @Schema(description = "查询是否包含子组", defaultValue = "false")
    private boolean includeSubGroups;

    @Schema(description = "终端名称或描述关键字")
    private String keyword;

    @Schema(description = "终端型号筛选")
    private String terminalModel;

    @Schema(description = "终端在线状态筛选, 0:离线;1:在线", defaultValue = "Null")
    private Integer onlineStatus;
}