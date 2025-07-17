package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "查询终端组列表请求DTO")
@Data
public class QueryTerminalGroupListRequest {

    @Schema(description = "父级终端组ID")
    private Long parentTgid;

    @Schema(description = "终端组名称（模糊搜索）")
    private String terminalGroupName;
}