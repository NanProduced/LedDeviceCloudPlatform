package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;

/**
 * 转码任务查询请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "转码任务查询请求")
public class TranscodingTaskQueryRequest {

    @Schema(description = "转码状态过滤", example = "COMPLETED")
    private String status;

    @Schema(description = "转码预设过滤")
    private String transcodePreset;

    @Schema(description = "源素材ID")
    private Long sourceMaterialId;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间") 
    private LocalDateTime endTime;

    @Schema(description = "页码", example = "1")
    @Min(value = 1, message = "页码不能小于1")
    @Builder.Default
    private Integer page = 1;

    @Schema(description = "每页大小", example = "20")
    @Min(value = 1, message = "每页大小不能小于1")
    @Max(value = 100, message = "每页大小不能超过100")
    @Builder.Default
    private Integer size = 20;

    @Schema(description = "排序字段", example = "createTime")
    @Builder.Default
    private String sortBy = "createTime";

    @Schema(description = "排序方向", example = "DESC")
    @Builder.Default
    private String sortDirection = "DESC";
}