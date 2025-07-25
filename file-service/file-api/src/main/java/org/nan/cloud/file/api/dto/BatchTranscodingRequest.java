package org.nan.cloud.file.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 批量转码请求
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Schema(description = "批量转码请求")
public class BatchTranscodingRequest {

    /**
     * 转码任务列表
     */
    @NotEmpty(message = "转码任务列表不能为空")
    @Valid
    @Schema(description = "转码任务列表", required = true)
    private List<TranscodingTaskRequest> tasks;

    /**
     * 批量处理优先级
     */
    @Schema(description = "批量处理优先级", example = "NORMAL")
    private String batchPriority = "NORMAL";

    /**
     * 最大并发数
     */
    @Schema(description = "最大并发处理数", example = "3")
    private Integer maxConcurrency = 3;
}