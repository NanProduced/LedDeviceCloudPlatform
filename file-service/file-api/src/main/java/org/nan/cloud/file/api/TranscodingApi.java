package org.nan.cloud.file.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.nan.cloud.file.api.dto.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 视频转码API接口
 * 
 * 功能说明：
 * - 视频格式转换和压缩
 * - 支持多种输出格式和质量设置
 * - 异步转码任务管理
 * - 转码进度实时监控
 * - GPU加速支持
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Tag(name = "视频转码", description = "视频转码处理接口")
@RequestMapping("/file/transcoding")
public interface TranscodingApi {

    /**
     * 提交转码任务
     * 
     * @param request 转码任务请求
     * @return 转码任务响应
     */
    @Operation(summary = "提交转码任务", description = "提交视频转码任务到队列")
    @PostMapping("/submit")
    TranscodingTaskResponse submitTranscodingTask(
            @RequestBody TranscodingTaskRequest request);

    /**
     * 批量提交转码任务
     * 
     * @param request 批量转码请求
     * @return 批量转码响应
     */
    @Operation(summary = "批量转码任务", description = "批量提交多个视频转码任务")
    @PostMapping("/batch-submit")
    BatchTranscodingResponse submitBatchTranscodingTasks(
            @RequestBody BatchTranscodingRequest request);

    /**
     * 查询转码进度
     * 
     * @param taskId 转码任务ID
     * @return 转码进度信息
     */
    @Operation(summary = "查询转码进度", description = "查询指定转码任务的进度")
    @GetMapping("/progress/{taskId}")
    TranscodingProgressResponse getTranscodingProgress(
            @Parameter(description = "转码任务ID") @PathVariable String taskId);

    /**
     * 获取转码任务列表
     * 
     * @param request 查询请求参数
     * @return 转码任务列表
     */
    @Operation(summary = "获取转码任务列表", description = "分页查询转码任务列表")
    @PostMapping("/tasks")
    TranscodingTaskListResponse getTranscodingTasks(
            @RequestBody TranscodingTaskQueryRequest request);

    /**
     * 取消转码任务
     * 
     * @param taskId 转码任务ID
     * @return 取消结果
     */
    @Operation(summary = "取消转码任务", description = "取消正在进行的转码任务")
    @PostMapping("/cancel/{taskId}")
    void cancelTranscodingTask(
            @Parameter(description = "转码任务ID") @PathVariable String taskId);

    /**
     * 重试转码任务
     * 
     * @param taskId 转码任务ID
     * @return 重试结果
     */
    @Operation(summary = "重试转码任务", description = "重新执行失败的转码任务")
    @PostMapping("/retry/{taskId}")
    TranscodingTaskResponse retryTranscodingTask(
            @Parameter(description = "转码任务ID") @PathVariable String taskId);

    /**
     * 获取转码预设配置
     * 
     * @return 预设配置列表
     */
    @Operation(summary = "获取转码预设", description = "获取系统预设的转码配置")
    @GetMapping("/presets")
    List<TranscodingPresetResponse> getTranscodingPresets();

    /**
     * 获取转码统计信息
     * 
     * @param request 统计查询请求
     * @return 转码统计信息
     */
    @Operation(summary = "转码统计信息", description = "获取转码任务的统计信息")
    @PostMapping("/statistics")
    TranscodingStatisticsResponse getTranscodingStatistics(
            @RequestBody TranscodingStatisticsRequest request);

    /**
     * 获取转码系统状态
     * 
     * @return 系统状态信息
     */
    @Operation(summary = "转码系统状态", description = "获取转码服务的系统状态")
    @GetMapping("/system-status")
    TranscodingSystemStatusResponse getSystemStatus();

    /**
     * 暂停转码任务
     * 
     * @param taskId 转码任务ID
     * @return 暂停结果
     */
    @Operation(summary = "暂停转码任务", description = "暂停正在进行的转码任务")
    @PostMapping("/pause/{taskId}")
    void pauseTranscodingTask(
            @Parameter(description = "转码任务ID") @PathVariable String taskId);

    /**
     * 恢复转码任务
     * 
     * @param taskId 转码任务ID
     * @return 恢复结果
     */
    @Operation(summary = "恢复转码任务", description = "恢复已暂停的转码任务")
    @PostMapping("/resume/{taskId}")
    void resumeTranscodingTask(
            @Parameter(description = "转码任务ID") @PathVariable String taskId);
}