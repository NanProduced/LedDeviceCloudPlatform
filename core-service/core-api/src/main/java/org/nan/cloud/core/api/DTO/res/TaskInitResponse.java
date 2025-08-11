package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "任务初始化响应")
public class TaskInitResponse {

    @Schema(description = "任务ID")
    private String taskId;

    @Schema(description = "任务类型")
    private String taskType;

    @Schema(description = "任务状态")
    private String status;

    @Schema(description = "进度订阅地址")
    private String progressSubscriptionUrl;

    @Schema(description = "提示信息")
    private String message;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}

