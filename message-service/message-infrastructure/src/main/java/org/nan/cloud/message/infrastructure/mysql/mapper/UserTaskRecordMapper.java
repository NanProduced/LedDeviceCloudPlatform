package org.nan.cloud.message.infrastructure.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.nan.cloud.message.infrastructure.mysql.entity.UserTaskRecord;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户任务记录Mapper接口
 * 
 * 提供用户任务的数据库访问功能，支持任务生命周期管理和结果通知。
 * 解决用户退出登录后任务结果丢失问题，支持多重通知机制。
 * 
 * 核心功能：
 * - 任务记录的CRUD操作
 * - 任务状态跟踪和更新
 * - 未查看任务结果查询
 * - 任务历史记录管理
 * - 会话关联查询
 * 
 * @author Nan
 * @since 1.0.0
 */
@Mapper
public interface UserTaskRecordMapper extends BaseMapper<UserTaskRecord> {
    
    /**
     * 根据任务ID查询任务记录
     * 
     * @param taskId 任务ID
     * @return 任务记录
     */
    @Select("SELECT * FROM user_task_record WHERE task_id = #{taskId}")
    UserTaskRecord selectByTaskId(@Param("taskId") String taskId);
    
    /**
     * 查询用户未查看的完成任务
     * 
     * 用于用户上线时推送未查看的任务结果。
     * 只查询已完成（成功或失败）但未查看的任务。
     * 
     * @param userId 用户ID
     * @param organizationId 组织ID
     * @return 未查看的完成任务列表
     */
    @Select("SELECT * FROM user_task_record " +
            "WHERE user_id = #{userId} " +
            "AND organization_id = #{organizationId} " +
            "AND status IN ('SUCCESS', 'FAILED') " +
            "AND is_result_viewed = false " +
            "ORDER BY completed_time DESC")
    List<UserTaskRecord> selectUnviewedCompletedTasks(@Param("userId") String userId,
                                                      @Param("organizationId") String organizationId);
    
    /**
     * 分页查询用户任务历史
     * 
     * 支持按任务类型和状态筛选，用于任务历史查询页面。
     * 
     * @param page 分页参数
     * @param userId 用户ID
     * @param organizationId 组织ID
     * @param taskType 任务类型（可选）
     * @param status 任务状态（可选）
     * @return 分页任务记录
     */
    @Select("<script>" +
            "SELECT * FROM user_task_record " +
            "WHERE user_id = #{userId} " +
            "AND organization_id = #{organizationId} " +
            "<if test='taskType != null and taskType != \"\"'>" +
            "AND task_type = #{taskType} " +
            "</if>" +
            "<if test='status != null and status != \"\"'>" +
            "AND status = #{status} " +
            "</if>" +
            "ORDER BY created_time DESC" +
            "</script>")
    IPage<UserTaskRecord> selectUserTasksByPage(Page<UserTaskRecord> page,
                                               @Param("userId") String userId,
                                               @Param("organizationId") String organizationId,
                                               @Param("taskType") String taskType,
                                               @Param("status") String status);
    
    /**
     * 更新任务状态和进度
     * 
     * 用于任务执行过程中的状态更新。
     * 
     * @param taskId 任务ID
     * @param status 新状态
     * @param progress 进度百分比
     * @param errorMessage 错误消息（可选）
     * @return 更新的记录数
     */
    @Update("<script>" +
            "UPDATE user_task_record SET " +
            "status = #{status}, " +
            "progress = #{progress}, " +
            "<if test='errorMessage != null'>" +
            "error_message = #{errorMessage}, " +
            "</if>" +
            "<if test='status == \"PROCESSING\"'>" +
            "started_time = NOW(), " +
            "</if>" +
            "<if test='status == \"SUCCESS\" or status == \"FAILED\" or status == \"CANCELLED\"'>" +
            "completed_time = NOW(), " +
            "</if>" +
            "updated_time = NOW() " +
            "WHERE task_id = #{taskId}" +
            "</script>")
    int updateTaskStatus(@Param("taskId") String taskId,
                        @Param("status") String status,
                        @Param("progress") Integer progress,
                        @Param("errorMessage") String errorMessage);
    
    /**
     * 标记任务结果为已查看
     * 
     * 用户查看任务结果后调用，避免重复推送。
     * 
     * @param taskId 任务ID
     * @param userId 用户ID（安全检查）
     * @return 更新的记录数
     */
    @Update("UPDATE user_task_record SET " +
            "is_result_viewed = true, " +
            "updated_time = NOW() " +
            "WHERE task_id = #{taskId} " +
            "AND user_id = #{userId}")
    int markResultAsViewed(@Param("taskId") String taskId,
                          @Param("userId") String userId);
    
    /**
     * 更新任务通知状态
     * 
     * 记录任务结果通知的发送状态，支持多重通知机制。
     * 
     * @param taskId 任务ID
     * @param notificationStatus 通知状态
     * @return 更新的记录数
     */
    @Update("UPDATE user_task_record SET " +
            "notification_status = #{notificationStatus}, " +
            "updated_time = NOW() " +
            "WHERE task_id = #{taskId}")
    int updateNotificationStatus(@Param("taskId") String taskId,
                                @Param("notificationStatus") String notificationStatus);
    
    /**
     * 清理会话相关的任务映射
     * 
     * 用户退出登录时调用，清理会话ID避免内存泄漏。
     * 将会话ID设置为NULL，但保留任务记录。
     * 
     * @param sessionId 会话ID
     * @return 更新的记录数
     */
    @Update("UPDATE user_task_record SET " +
            "session_id = NULL, " +
            "updated_time = NOW() " +
            "WHERE session_id = #{sessionId} " +
            "AND status IN ('PENDING', 'PROCESSING')")
    int cleanupSessionTasks(@Param("sessionId") String sessionId);
    
    /**
     * 查询长时间运行的任务
     * 
     * 用于任务超时监控，查询运行时间过长的任务。
     * 
     * @param timeoutMinutes 超时时间（分钟）
     * @return 超时任务列表
     */
    @Select("SELECT * FROM user_task_record " +
            "WHERE status = 'PROCESSING' " +
            "AND started_time < DATE_SUB(NOW(), INTERVAL #{timeoutMinutes} MINUTE)")
    List<UserTaskRecord> selectTimeoutTasks(@Param("timeoutMinutes") int timeoutMinutes);
    
    /**
     * 统计用户任务执行情况
     * 
     * 用于生成用户任务统计报表。
     * 
     * @param userId 用户ID
     * @param organizationId 组织ID
     * @param startTime 统计开始时间
     * @param endTime 统计结束时间
     * @return 统计结果Map
     */
    @Select("SELECT " +
            "COUNT(*) as total_count, " +
            "SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) as success_count, " +
            "SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed_count, " +
            "SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelled_count, " +
            "SUM(CASE WHEN status IN ('PENDING', 'PROCESSING') THEN 1 ELSE 0 END) as running_count " +
            "FROM user_task_record " +
            "WHERE user_id = #{userId} " +
            "AND organization_id = #{organizationId} " +
            "AND created_time BETWEEN #{startTime} AND #{endTime}")
    java.util.Map<String, Long> getTaskStatistics(@Param("userId") String userId,
                                                  @Param("organizationId") String organizationId,
                                                  @Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);
}