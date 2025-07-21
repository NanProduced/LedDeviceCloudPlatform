package org.nan.cloud.message.infrastructure.mongodb.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import org.nan.cloud.message.infrastructure.mongodb.document.TaskResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 任务结果Repository接口
 * 
 * 提供任务结果的MongoDB数据访问功能，支持任务详情存储和查询。
 * 配合MySQL的任务记录，提供完整的任务管理和结果追溯能力。
 * 
 * 核心功能：
 * - 任务结果的CRUD操作
 * - 按用户和组织查询任务
 * - 任务执行情况统计
 * - 任务性能分析
 * - 输出文件管理
 * 
 * @author Nan
 * @since 1.0.0
 */
@Repository
public interface TaskResultRepository extends MongoRepository<TaskResult, String> {
    
    /**
     * 根据任务ID查询任务结果
     * 
     * @param taskId 任务ID
     * @return 任务结果（可选）
     */
    Optional<TaskResult> findByTaskId(String taskId);
    
    /**
     * 查询用户的任务结果历史
     * 
     * 按完成时间倒序排列，用于任务历史查询页面。
     * 支持多租户数据隔离。
     * 
     * @param userId 用户ID
     * @param organizationId 组织ID
     * @param pageable 分页参数
     * @return 分页任务结果
     */
    Page<TaskResult> findByUserIdAndOrganizationIdOrderByCompletedTimeDesc(
            String userId, String organizationId, Pageable pageable);
    
    /**
     * 根据任务类型查询用户任务
     * 
     * 支持按任务类型筛选，便于用户查找特定类型的任务结果。
     * 
     * @param userId 用户ID
     * @param organizationId 组织ID
     * @param taskType 任务类型
     * @param pageable 分页参数
     * @return 分页任务结果
     */
    Page<TaskResult> findByUserIdAndOrganizationIdAndTaskTypeOrderByCompletedTimeDesc(
            String userId, String organizationId, String taskType, Pageable pageable);
    
    /**
     * 查询指定状态的任务
     * 
     * 用于统计和监控不同状态的任务分布情况。
     * 
     * @param organizationId 组织ID
     * @param status 任务状态
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 指定状态的任务列表
     */
    Page<TaskResult> findByOrganizationIdAndStatusAndCompletedTimeBetween(
            String organizationId, String status, 
            LocalDateTime startTime, LocalDateTime endTime, 
            Pageable pageable);
    
    /**
     * 查询包含输出文件的任务
     * 
     * 用于文件管理和存储空间统计。
     * 
     * @param organizationId 组织ID
     * @param pageable 分页参数
     * @return 包含输出文件的任务列表
     */
    @Query("{ 'organizationId': ?0, 'outputFiles': { $exists: true, $not: { $size: 0 } } }")
    Page<TaskResult> findTasksWithOutputFiles(String organizationId, Pageable pageable);
    
    /**
     * 查询长时间执行的任务
     * 
     * 用于性能分析和异常任务监控。
     * 
     * @param minDurationMs 最小执行时长（毫秒）
     * @param startTime 任务开始时间范围
     * @param endTime 任务结束时间范围
     * @return 长时间执行的任务列表
     */
    @Query("{ 'executionDurationMs': { $gte: ?0 }, 'startedTime': { $gte: ?1, $lte: ?2 } }")
    List<TaskResult> findLongRunningTasks(Long minDurationMs, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 统计用户任务执行情况
     * 
     * 统计用户在指定时间范围内的任务执行情况。
     * 
     * @param userId 用户ID
     * @param organizationId 组织ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 任务数量
     */
    long countByUserIdAndOrganizationIdAndCreatedTimeBetween(
            String userId, String organizationId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 查询失败的任务
     * 
     * 用于错误分析和故障排查，包含详细的错误信息。
     * 
     * @param organizationId 组织ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 失败的任务列表
     */
    List<TaskResult> findByOrganizationIdAndStatusAndCompletedTimeBetween(
            String organizationId, String status, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 根据执行性能查询任务
     * 
     * 查询资源消耗较高的任务，用于性能优化分析。
     * 
     * @param organizationId 组织ID
     * @param minMemoryBytes 最小内存使用量
     * @param minCpuTimeMs 最小CPU时间
     * @param pageable 分页参数
     * @return 高资源消耗的任务列表
     */
    @Query("{ 'organizationId': ?0, " +
           "'performanceMetrics.memoryPeakBytes': { $gte: ?1 }, " +
           "'performanceMetrics.cpuTimeMs': { $gte: ?2 } }")
    Page<TaskResult> findHighResourceTasks(String organizationId, Long minMemoryBytes, 
                                          Long minCpuTimeMs, Pageable pageable);
    
    /**
     * 查询最近的成功任务
     * 
     * 用于展示用户最近成功完成的任务。
     * 
     * @param userId 用户ID
     * @param organizationId 组织ID
     * @param limit 限制数量
     * @return 最近成功的任务列表
     */
    @Query("{ 'userId': ?0, 'organizationId': ?1, 'status': 'SUCCESS' }")
    List<TaskResult> findRecentSuccessTasks(String userId, String organizationId, Pageable pageable);
    
    /**
     * 统计任务类型分布
     * 
     * 统计组织内不同类型任务的数量分布。
     * 
     * @param organizationId 组织ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 任务类型统计结果
     */
    @Query("{ 'organizationId': ?0, 'createdTime': { $gte: ?1, $lte: ?2 } }")
    List<TaskResult> findForTypeStatistics(String organizationId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 查询包含特定结果数据的任务
     * 
     * 根据结果数据中的字段查询任务，支持复杂的业务查询。
     * 
     * @param organizationId 组织ID
     * @param resultFieldPath 结果数据字段路径
     * @param resultFieldValue 结果数据字段值
     * @param pageable 分页参数
     * @return 匹配的任务列表
     */
    @Query("{ 'organizationId': ?0, ?1: ?2 }")
    Page<TaskResult> findByResultData(String organizationId, String resultFieldPath, 
                                     Object resultFieldValue, Pageable pageable);
    
    /**
     * 查询执行时间超过阈值的任务
     * 
     * 用于识别性能异常的任务，进行优化分析。
     * 
     * @param organizationId 组织ID
     * @param taskType 任务类型
     * @param thresholdMs 执行时间阈值（毫秒）
     * @return 执行时间超过阈值的任务列表
     */
    @Query("{ 'organizationId': ?0, 'taskType': ?1, 'executionDurationMs': { $gt: ?2 } }")
    List<TaskResult> findSlowTasks(String organizationId, String taskType, Long thresholdMs);
    
    /**
     * 统计输出文件总大小
     * 
     * 统计用户或组织产生的输出文件总大小，用于存储配额管理。
     * 
     * @param userId 用户ID（可选）
     * @param organizationId 组织ID
     * @return 输出文件总大小统计
     */
    @Query(value = "{ 'userId': ?0, 'organizationId': ?1, 'outputFiles': { $exists: true } }", 
           fields = "{ 'outputFiles.fileSize': 1 }")
    List<TaskResult> findUserOutputFileSizes(String userId, String organizationId);
    
    /**
     * 查询包含错误日志的任务
     * 
     * 用于错误分析和日志审计，查找包含特定错误信息的任务。
     * 
     * @param organizationId 组织ID
     * @param errorKeyword 错误关键词
     * @param pageable 分页参数
     * @return 包含错误日志的任务列表
     */
    @Query("{ 'organizationId': ?0, $or: [ " +
           "{ 'errorMessage': { $regex: ?1, $options: 'i' } }, " +
           "{ 'executionLogs.message': { $regex: ?1, $options: 'i' } } ] }")
    Page<TaskResult> findTasksWithErrorKeyword(String organizationId, String errorKeyword, Pageable pageable);
    
    /**
     * 删除过期的任务结果
     * 
     * 删除创建时间早于指定时间的任务结果，进行数据清理。
     * 
     * @param beforeTime 过期时间点
     * @return 删除的任务数量
     */
    long deleteByCreatedTimeBefore(LocalDateTime beforeTime);
}