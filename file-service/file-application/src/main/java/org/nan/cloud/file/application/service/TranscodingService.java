package org.nan.cloud.file.application.service;

import org.nan.cloud.file.api.dto.*;
import org.nan.cloud.file.application.domain.FileInfo;

import java.util.List;

/**
 * 视频转码服务接口
 * 
 * 提供视频转码相关的业务逻辑：
 * - 转码任务提交和管理
 * - 转码进度监控
 * - 转码参数配置
 * - 转码性能优化
 * 
 * TODO: 创建 TranscodingServiceImpl 实现类
 * TODO: 实现异步转码任务处理和队列管理
 * TODO: 集成 RabbitMQ 消息队列进行任务分发
 * TODO: 完善转码预设和配置管理功能
 * TODO: 实现转码失败重试机制
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface TranscodingService {

    /**
     * 提交转码任务
     * 
     * @param request 转码任务请求
     * @return 转码任务响应
     */
    TranscodingTaskResponse submitTranscodingTask(TranscodingTaskRequest request);

    /**
     * 批量提交转码任务
     * 
     * @param request 批量转码请求
     * @return 批量转码响应
     */
    BatchTranscodingResponse submitBatchTranscodingTasks(BatchTranscodingRequest request);

    /**
     * 异步提交转码任务
     * 
     * @param fileInfo 文件信息
     * @param presetId 转码预设ID
     * @return 转码任务ID
     */
    String submitTranscodingTaskAsync(FileInfo fileInfo, String presetId);

    /**
     * 获取转码进度
     * 
     * @param taskId 转码任务ID
     * @return 转码进度信息
     */
    TranscodingProgressResponse getTranscodingProgress(String taskId);

    /**
     * 获取转码任务列表
     * 
     * @param request 查询请求
     * @return 转码任务列表
     */
    TranscodingTaskListResponse getTranscodingTasks(TranscodingTaskQueryRequest request);

    /**
     * 取消转码任务
     * 
     * @param taskId 转码任务ID
     */
    void cancelTranscodingTask(String taskId);

    /**
     * 重试转码任务
     * 
     * @param taskId 转码任务ID
     * @return 重试后的任务信息
     */
    TranscodingTaskResponse retryTranscodingTask(String taskId);

    /**
     * 暂停转码任务
     * 
     * @param taskId 转码任务ID
     */
    void pauseTranscodingTask(String taskId);

    /**
     * 恢复转码任务
     * 
     * @param taskId 转码任务ID
     */
    void resumeTranscodingTask(String taskId);

    /**
     * 获取转码预设配置
     * 
     * @return 预设配置列表
     */
    List<TranscodingPresetResponse> getTranscodingPresets();

    /**
     * 获取转码统计信息
     * 
     * @param request 统计请求
     * @return 统计信息
     */
    TranscodingStatisticsResponse getTranscodingStatistics(TranscodingStatisticsRequest request);

    /**
     * 获取转码系统状态
     * 
     * @return 系统状态
     */
    TranscodingSystemStatusResponse getSystemStatus();

    /**
     * 验证转码参数
     * 
     * @param request 转码请求
     * @return 验证结果
     */
    TranscodingValidationResult validateTranscodingRequest(TranscodingTaskRequest request);

    /**
     * 估算转码时间
     * 
     * @param fileSize 文件大小
     * @param duration 视频时长
     * @param targetQuality 目标质量
     * @return 估算时间（分钟）
     */
    Long estimateTranscodingTime(long fileSize, long duration, String targetQuality);

    /**
     * 获取转码队列状态
     * 
     * @return 队列状态信息
     */
    TranscodingQueueStatusResponse getQueueStatus();
}