package org.nan.cloud.message.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.api.dto.response.TaskResultResponse;
import org.nan.cloud.message.api.dto.response.TaskHistoryResponse;
import org.nan.cloud.message.domain.model.TaskResultData;
import org.nan.cloud.message.infrastructure.converter.TaskResultConverter;
import org.nan.cloud.message.infrastructure.mongodb.document.TaskResult;
import org.nan.cloud.message.infrastructure.mongodb.repository.TaskResultRepository;
import org.nan.cloud.message.infrastructure.mysql.entity.UserTaskRecord;
import org.nan.cloud.message.infrastructure.mysql.mapper.UserTaskRecordMapper;
import org.nan.cloud.message.infrastructure.redis.manager.MessageCacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 任务结果持久化聚合仓储实现
 * 
 * 实现TaskResultPersistenceRepositoryInterface接口，
 * 统一协调MySQL和MongoDB的任务结果数据操作，支持任务结果的多重通知机制。
 * 解决用户退出登录后任务结果丢失的问题，提供完整的任务生命周期管理。
 * 
 * 数据分工策略：
 * - MySQL：存储任务基础信息、状态跟踪、通知记录
 * - MongoDB：存储任务详细结果、执行日志、性能指标
 * - Redis：缓存任务会话映射、实时状态信息
 * 
 * 核心功能：
 * - 任务结果的完整保存和查询
 * - 任务会话关系管理
 * - 多重通知机制支持
 * - 任务历史记录管理
 * - 任务性能分析
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TaskResultPersistenceRepository implements org.nan.cloud.message.domain.repository.TaskResultPersistenceRepositoryInterface {
    
    private final UserTaskRecordMapper userTaskRecordMapper;
    private final TaskResultRepository taskResultRepository;
    private final MessageCacheManager messageCacheManager;
    private final TaskResultConverter taskResultConverter;
    
    // ==================== 任务记录管理 ====================
    
    /**
     * 创建新任务记录
     * 
     * 在用户发起任务时调用，记录任务基础信息和会话关联。
     * 
     * @param taskId 任务ID
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param organizationId 组织ID
     * @param taskType 任务类型
     * @param taskName 任务名称
     */
    @Transactional(rollbackFor = Exception.class)
    public void createTaskRecord(String taskId, String userId, String sessionId, 
                               String organizationId, String taskType, String taskName) {
        try {
            log.info("创建任务记录: taskId={}, userId={}, sessionId={}", taskId, userId, sessionId);
            
            // 1. 保存到MySQL
            UserTaskRecord taskRecord = new UserTaskRecord();
            taskRecord.setTaskId(taskId);
            taskRecord.setUserId(userId);
            taskRecord.setSessionId(sessionId);
            taskRecord.setOrganizationId(organizationId);
            taskRecord.setTaskType(taskType);
            taskRecord.setTaskName(taskName);
            taskRecord.setStatus("PENDING");
            taskRecord.setProgress(0);
            taskRecord.setIsResultViewed(false);
            taskRecord.setNotificationStatus("NOT_SENT");
            taskRecord.setCreatedTime(LocalDateTime.now());
            
            userTaskRecordMapper.insert(taskRecord);
            log.debug("任务基础记录保存成功: taskId={}", taskId);
            
            // 2. 记录会话映射到Redis
            messageCacheManager.recordTaskSession(taskId, sessionId, 24);
            
            log.info("任务记录创建完成: taskId={}", taskId);
            
        } catch (Exception e) {
            log.error("创建任务记录失败: taskId={}, error={}", taskId, e.getMessage(), e);
            throw new RuntimeException("创建任务记录失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 更新任务状态和进度
     * 
     * @param taskId 任务ID
     * @param status 新状态
     * @param progress 进度百分比
     * @param errorMessage 错误消息（可选）
     */
    public void updateTaskStatus(String taskId, String status, Integer progress, String errorMessage) {
        try {
            log.debug("更新任务状态: taskId={}, status={}, progress={}", taskId, status, progress);
            
            int updated = userTaskRecordMapper.updateTaskStatus(taskId, status, progress, errorMessage);
            
            if (updated > 0) {
                log.debug("任务状态更新成功: taskId={}, status={}", taskId, status);
            } else {
                log.warn("任务状态更新无影响: taskId={}, status={}", taskId, status);
            }
            
        } catch (Exception e) {
            log.error("更新任务状态失败: taskId={}, status={}, error={}", 
                     taskId, status, e.getMessage(), e);
        }
    }
    
    /**
     * 保存任务完整结果
     * 
     * 任务执行完成时调用，保存详细的执行结果和性能数据。
     * 
     * @param taskResult 任务结果详情
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveTaskResult(TaskResultData taskResult) {
        // 转换为infrastructure层的实体
        TaskResult infraTaskResult = taskResultConverter.toInfrastructureEntity(taskResult);
        try {
            log.info("保存任务结果: taskId={}, status={}", 
                    taskResult.getTaskId(), taskResult.getStatus());
            
            // 1. 更新MySQL任务状态
            String status = taskResult.getStatus();
            Integer progress = taskResult.isSuccess() ? 100 : 0;
            String errorMessage = taskResult.getErrorMessage();
            
            userTaskRecordMapper.updateTaskStatus(taskResult.getTaskId(), status, progress, errorMessage);
            log.debug("任务基础状态更新完成: taskId={}", taskResult.getTaskId());
            
            // 2. 保存详细结果到MongoDB
            infraTaskResult.setCompletedTime(LocalDateTime.now());
            if (infraTaskResult.getStartedTime() != null) {
                infraTaskResult.setExecutionDurationMs(
                    java.time.Duration.between(infraTaskResult.getStartedTime(), infraTaskResult.getCompletedTime())
                        .toMillis());
            }
            
            taskResultRepository.save(infraTaskResult);
            log.debug("任务详细结果保存完成: taskId={}", taskResult.getTaskId());
            
            log.info("任务结果保存完成: taskId={}", taskResult.getTaskId());
            
        } catch (Exception e) {
            log.error("保存任务结果失败: taskId={}, error={}", 
                     taskResult.getTaskId(), e.getMessage(), e);
            throw new RuntimeException("保存任务结果失败: " + e.getMessage(), e);
        }
    }
    
    // ==================== 任务结果查询 ====================
    
    /**
     * 获取完整任务结果
     * 
     * 关联查询MySQL和MongoDB，返回完整的任务信息。
     * 
     * @param taskId 任务ID
     * @return 完整任务结果
     */
    public Optional<TaskResultResponse> getCompleteTaskResult(String taskId) {
        try {
            // 1. 从MySQL获取基础记录
            UserTaskRecord taskRecord = userTaskRecordMapper.selectByTaskId(taskId);
            if (taskRecord == null) {
                log.debug("任务记录不存在: taskId={}", taskId);
                return Optional.empty();
            }
            
            // 2. 从MongoDB获取详细结果
            Optional<TaskResult> taskResultOpt = taskResultRepository.findByTaskId(taskId);
            
            // 3. 构建完整响应
            TaskResultResponse response = buildTaskResultResponse(taskRecord, taskResultOpt.orElse(null));
            
            log.debug("获取完整任务结果成功: taskId={}", taskId);
            return Optional.of(response);
            
        } catch (Exception e) {
            log.error("获取完整任务结果失败: taskId={}, error={}", taskId, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * 查询用户未查看的完成任务
     * 
     * 用于用户上线时推送未查看的任务结果。
     * 
     * @param userId 用户ID
     * @param organizationId 组织ID
     * @return 未查看的完成任务列表
     */
    public List<TaskResultResponse> getUnviewedCompletedTasks(String userId, String organizationId) {
        try {
            log.debug("查询用户未查看完成任务: userId={}, organizationId={}", userId, organizationId);
            
            // 1. 从MySQL查询未查看的完成任务
            List<UserTaskRecord> unviewedTasks = userTaskRecordMapper
                .selectUnviewedCompletedTasks(userId, organizationId);
            
            if (unviewedTasks.isEmpty()) {
                return List.of();
            }
            
            // 2. 批量获取详细结果
            List<String> taskIds = unviewedTasks.stream()
                .map(UserTaskRecord::getTaskId)
                .collect(Collectors.toList());
            
            Map<String, TaskResult> resultMap = taskResultRepository
                .findAll()
                .stream()
                .filter(result -> taskIds.contains(result.getTaskId()))
                .collect(Collectors.toMap(TaskResult::getTaskId, result -> result));
            
            // 3. 构建响应列表
            List<TaskResultResponse> responses = unviewedTasks.stream()
                .map(record -> {
                    TaskResult result = resultMap.get(record.getTaskId());
                    return buildTaskResultResponse(record, result);
                })
                .collect(Collectors.toList());
            
            log.debug("查询用户未查看完成任务完成: userId={}, count={}", userId, responses.size());
            return responses;
            
        } catch (Exception e) {
            log.error("查询用户未查看完成任务失败: userId={}, error={}", userId, e.getMessage(), e);
            return List.of();
        }
    }
    
    /**
     * 分页查询用户任务历史
     * 
     * @param userId 用户ID
     * @param organizationId 组织ID
     * @param taskType 任务类型（可选）
     * @param status 任务状态（可选）
     * @param page 页码
     * @param size 每页大小
     * @return 分页任务历史
     */
    public TaskHistoryResponse getTaskHistoryByPage(String userId, String organizationId, 
                                                  String taskType, String status, 
                                                  int page, int size) {
        try {
            log.debug("分页查询用户任务历史: userId={}, organizationId={}, page={}, size={}", 
                     userId, organizationId, page, size);
            
            // 1. 分页查询MySQL记录
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<UserTaskRecord> mysqlPage = 
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
            
            com.baomidou.mybatisplus.core.metadata.IPage<UserTaskRecord> taskPage = 
                userTaskRecordMapper.selectUserTasksByPage(mysqlPage, userId, organizationId, taskType, status);
            
            // 2. 批量获取详细结果
            List<String> taskIds = taskPage.getRecords().stream()
                .map(UserTaskRecord::getTaskId)
                .collect(Collectors.toList());
            
            Map<String, TaskResult> resultMap;
            if (!taskIds.isEmpty()) {
                resultMap = taskResultRepository
                    .findAll()
                    .stream()
                    .filter(result -> taskIds.contains(result.getTaskId()))
                    .collect(Collectors.toMap(TaskResult::getTaskId, result -> result));
            } else {
                resultMap = Map.of();
            }

            // 3. 构建响应
            List<TaskResultResponse> taskResults = taskPage.getRecords().stream()
                .map(record -> {
                    TaskResult result = resultMap.get(record.getTaskId());
                    return buildTaskResultResponse(record, result);
                })
                .collect(Collectors.toList());
            
            TaskHistoryResponse response = TaskHistoryResponse.builder()
                .tasks(taskResults)
                .total(taskPage.getTotal())
                .page(page)
                .size(size)
                .build();
            
            log.debug("分页查询用户任务历史完成: userId={}, total={}", userId, response.getTotal());
            return response;
            
        } catch (Exception e) {
            log.error("分页查询用户任务历史失败: userId={}, error={}", userId, e.getMessage(), e);
            return TaskHistoryResponse.builder()
                .tasks(List.of())
                .total(0L)
                .page(page)
                .size(size)
                .build();
        }
    }
    
    // ==================== 多重通知机制支持 ====================
    
    /**
     * 获取任务对应的会话ID
     * 
     * 用于任务完成时的会话级通知。
     * 优先从Redis缓存获取，缓存不存在时从MySQL查询。
     * 
     * @param taskId 任务ID
     * @return 会话ID
     */
    public String getTaskSessionId(String taskId) {
        try {
            // 1. 先从缓存获取
            String sessionId = messageCacheManager.getTaskSession(taskId);
            if (sessionId != null) {
                return sessionId;
            }
            
            // 2. 缓存不存在时从MySQL查询
            UserTaskRecord taskRecord = userTaskRecordMapper.selectByTaskId(taskId);
            if (taskRecord != null) {
                return taskRecord.getSessionId();
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("获取任务会话ID失败: taskId={}, error={}", taskId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 更新任务通知状态
     * 
     * 记录任务结果通知的发送状态，支持多重通知机制的状态跟踪。
     * 
     * @param taskId 任务ID
     * @param notificationStatus 通知状态
     */
    public void updateNotificationStatus(String taskId, String notificationStatus) {
        try {
            log.debug("更新任务通知状态: taskId={}, notificationStatus={}", taskId, notificationStatus);
            
            int updated = userTaskRecordMapper.updateNotificationStatus(taskId, notificationStatus);
            
            if (updated > 0) {
                log.debug("任务通知状态更新成功: taskId={}, notificationStatus={}", 
                         taskId, notificationStatus);
            } else {
                log.warn("任务通知状态更新无影响: taskId={}", taskId);
            }
            
        } catch (Exception e) {
            log.error("更新任务通知状态失败: taskId={}, notificationStatus={}, error={}", 
                     taskId, notificationStatus, e.getMessage(), e);
        }
    }
    
    /**
     * 标记任务结果为已查看
     * 
     * @param taskId 任务ID
     * @param userId 用户ID（安全检查）
     */
    public void markTaskResultAsViewed(String taskId, String userId) {
        try {
            log.debug("标记任务结果已查看: taskId={}, userId={}", taskId, userId);
            
            int updated = userTaskRecordMapper.markResultAsViewed(taskId, userId);
            
            if (updated > 0) {
                log.debug("任务结果已查看标记成功: taskId={}", taskId);
            } else {
                log.warn("任务结果已查看标记无影响: taskId={}, userId={}", taskId, userId);
            }
            
        } catch (Exception e) {
            log.error("标记任务结果已查看失败: taskId={}, userId={}, error={}", 
                     taskId, userId, e.getMessage(), e);
        }
    }
    
    /**
     * 清理会话相关的任务映射
     * 
     * 用户退出登录时调用，清理会话ID避免内存泄漏。
     * 
     * @param sessionId 会话ID
     */
    public void cleanupSessionTasks(String sessionId) {
        try {
            log.debug("清理会话任务映射: sessionId={}", sessionId);
            
            // 1. 清理MySQL中的会话关联
            int updated = userTaskRecordMapper.cleanupSessionTasks(sessionId);
            
            // 2. Redis中的映射会自动过期，无需手动清理
            
            log.debug("会话任务映射清理完成: sessionId={}, updated={}", sessionId, updated);
            
        } catch (Exception e) {
            log.error("清理会话任务映射失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 构建任务结果响应
     */
    private TaskResultResponse buildTaskResultResponse(UserTaskRecord record, TaskResult result) {
        return TaskResultResponse.builder()
            .taskId(record.getTaskId())
            .userId(record.getUserId())
            .organizationId(record.getOrganizationId())
            .taskType(record.getTaskType())
            .taskName(record.getTaskName())
            .status(record.getStatus())
            .progress(record.getProgress())
            .errorMessage(record.getErrorMessage())
            .createdTime(record.getCreatedTime())
            .startedTime(record.getStartedTime())
            .completedTime(record.getCompletedTime())
            .isResultViewed(record.getIsResultViewed())
            .notificationStatus(record.getNotificationStatus())
            // MongoDB详细数据
            .resultData(result != null ? result.getResultData() : null)
            .executionParams(result != null ? result.getExecutionParams() : null)
            .outputFiles(result != null ? result.getOutputFiles().stream().map(e -> e.getFileUrl()).toList() : null)
            .executionDurationMs(result != null ? result.getExecutionDurationMs() : null)
            .build();
    }
}