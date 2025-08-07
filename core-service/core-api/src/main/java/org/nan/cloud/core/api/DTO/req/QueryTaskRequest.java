package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "请求查询任务列表DTO")
public class QueryTaskRequest {

    @Schema(description = "任务类型筛选")
    private String taskType;

    @Schema(description = "任务状态筛选")
    private String taskStatus;
}
