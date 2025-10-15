package org.nan.cloud.core.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.nan.cloud.core.api.DTO.req.TranscodingTaskQueryRequest;
import org.nan.cloud.core.api.DTO.res.TranscodingTaskResponse;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 转码任务查询API接口
 */
@Tag(name = "Transcoding(转码任务查询)", description = "转码任务查询相关操作")
public interface TranscodingApi {

    String PREFIX = "/transcoding";

    @Operation(
        summary = "查询用户转码任务列表", 
        description = "查询当前用户的转码任务，支持状态、时间范围等条件过滤"
    )
    @PostMapping(PREFIX + "/tasks/query")
    TranscodingTaskResponse queryTranscodingTasks(@RequestBody @Valid TranscodingTaskQueryRequest request);

    @Operation(
        summary = "根据任务ID获取转码任务详情",
        description = "获取指定转码任务的详细信息，包括转码前后对比"
    )
    @GetMapping(PREFIX + "/tasks/{taskId}")
    TranscodingTaskResponse.TranscodingTaskInfo getTranscodingTaskDetail(
        @Parameter(description = "任务ID") @PathVariable String taskId
    );

    @Operation(
        summary = "根据源素材ID查询转码任务",
        description = "查询指定源素材的所有转码任务"
    )
    @GetMapping(PREFIX + "/tasks/by-source/{sourceMaterialId}")
    TranscodingTaskResponse getTranscodingTasksBySource(
        @Parameter(description = "源素材ID") @PathVariable Long sourceMaterialId
    );
}