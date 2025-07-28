package org.nan.cloud.file.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.web.DynamicResponse;
import org.nan.cloud.file.api.TranscodingApi;
import org.nan.cloud.file.api.dto.*;
import org.nan.cloud.file.application.service.TranscodingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 视频转码控制器
 * 
 * 实现视频转码相关的REST API接口
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "视频转码", description = "视频转码处理接口")
public class TranscodingController implements TranscodingApi {

    private final TranscodingService transcodingService;

    @Override
    public TranscodingTaskResponse submitTranscodingTask(
            @RequestBody TranscodingTaskRequest request) {
        
        log.info("提交转码任务 - 源文件: {}, 输出格式: {}, 质量: {}", 
                request.getSourceFileId(), request.getOutputFormat(), request.getVideoQuality());

        TranscodingTaskResponse response = transcodingService.submitTranscodingTask(request);
        
        log.info("转码任务提交成功 - 任务ID: {}, 源文件: {}", 
                response.getTaskId(), request.getSourceFileId());
        
        return response;
    }

    @Override
    public BatchTranscodingResponse submitBatchTranscodingTasks(
            @RequestBody BatchTranscodingRequest request) {
        
        log.info("批量提交转码任务 - 任务数量: {}", request.getTasks().size());

        BatchTranscodingResponse response = transcodingService.submitBatchTranscodingTasks(request);
        
        log.info("批量转码任务提交完成 - 总数: {}, 成功: {}, 失败: {}", 
                response.getTotalTasks(), response.getSuccessCount(), response.getFailedCount());
        
        return response;
    }

    @Override
    public TranscodingProgressResponse getTranscodingProgress(
            @Parameter(description = "转码任务ID") @PathVariable String taskId) {
        
        log.debug("查询转码进度 - 任务ID: {}", taskId);

        return transcodingService.getTranscodingProgress(taskId);
    }

    @Override
    public TranscodingTaskListResponse getTranscodingTasks(
            @RequestBody TranscodingTaskQueryRequest request) {
        
        log.info("查询转码任务列表 - 组织: {}, 状态: {}, 页码: {}", 
                request.getOrganizationId(), request.getStatus(), request.getPage());

        TranscodingTaskListResponse response = transcodingService.getTranscodingTasks(request);
        
        log.debug("转码任务列表查询完成 - 总数: {}", response.getTotal());
        
        return response;
    }

    @Override
    public void cancelTranscodingTask(
            @Parameter(description = "转码任务ID") @PathVariable String taskId) {
        
        log.info("取消转码任务 - 任务ID: {}", taskId);

        transcodingService.cancelTranscodingTask(taskId);
        
        log.info("转码任务已取消 - 任务ID: {}", taskId);
    }

    @Override
    public TranscodingTaskResponse retryTranscodingTask(
            @Parameter(description = "转码任务ID") @PathVariable String taskId) {
        
        log.info("重试转码任务 - 任务ID: {}", taskId);

        TranscodingTaskResponse response = transcodingService.retryTranscodingTask(taskId);
        
        log.info("转码任务重试成功 - 任务ID: {}", taskId);
        
        return response;
    }

    @Override
    public List<TranscodingPresetResponse> getTranscodingPresets() {
        
        log.debug("获取转码预设配置");

        List<TranscodingPresetResponse> response = transcodingService.getTranscodingPresets();
        
        log.debug("转码预设配置获取成功 - 数量: {}", response.size());
        
        return response;
    }

    @Override
    public TranscodingStatisticsResponse getTranscodingStatistics(
            @RequestBody TranscodingStatisticsRequest request) {
        
        log.info("获取转码统计信息 - 组织: {}, 时间范围: {} 到 {}", 
                request.getOrganizationId(), request.getStartDate(), request.getEndDate());

        TranscodingStatisticsResponse response = transcodingService.getTranscodingStatistics(request);
        
        log.debug("转码统计信息获取成功");
        
        return response;
    }

    @Override
    public TranscodingSystemStatusResponse getSystemStatus() {
        
        log.debug("获取转码系统状态");

        return transcodingService.getSystemStatus();
    }

    @Override
    public void pauseTranscodingTask(
            @Parameter(description = "转码任务ID") @PathVariable String taskId) {
        
        log.info("暂停转码任务 - 任务ID: {}", taskId);

        transcodingService.pauseTranscodingTask(taskId);
        
        log.info("转码任务已暂停 - 任务ID: {}", taskId);
    }

    @Override
    public void resumeTranscodingTask(
            @Parameter(description = "转码任务ID") @PathVariable String taskId) {
        
        log.info("恢复转码任务 - 任务ID: {}", taskId);

        transcodingService.resumeTranscodingTask(taskId);
        
        log.info("转码任务已恢复 - 任务ID: {}", taskId);
    }
}