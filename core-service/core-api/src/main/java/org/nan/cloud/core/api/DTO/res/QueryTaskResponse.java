package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "查询任务列表请求")
@Data
public class QueryTaskResponse {

    @Schema(description = "任务Id")
    private String taskId;

    @Schema(description = "任务类型")
    private String taskType;

    @Schema(description = "任务状态")
    private String taskStatus;

    @Schema(description = "关联的信息", example = "{上传的文件名}, {导出的文件名}, {转码的素材名}")
    private String ref;

    @Schema(description = "导出任务的文件下载链接")
    private String downloadUrl;

    @Schema(description = "任务进度(0-100)")
    private Integer progress;

    @Schema(description = "任务错误信息(当失败时存在)")
    private String errorMessage;

    @Schema(description = "缩略图地址(素材相关任务可选)")
    private String thumbnailUrl;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "完成时间")
    private LocalDateTime completeTime;
}
