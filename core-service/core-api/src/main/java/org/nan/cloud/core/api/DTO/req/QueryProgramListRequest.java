package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "查询节目列表DTO")
public class QueryProgramListRequest {

    @Schema(description = "节目名关键字搜索")
    private String keyword;

    @Schema(description = "筛选节目状态")
    private String status;
}
