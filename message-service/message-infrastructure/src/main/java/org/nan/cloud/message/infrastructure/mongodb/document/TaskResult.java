package org.nan.cloud.message.infrastructure.mongodb.document;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

/**
 * 任务结果详情文档
 * 
 * 存储异步任务的详细执行结果、输出数据和错误信息。
 * 与MySQL中的UserTaskRecord形成关联，通过taskId进行关联查询。
 * 支持大数据量结果存储和快速检索。
 * 
 * 业务场景：
 * - 数据导出任务结果存储
 * - 报告生成任务详情
 * - 批量设备操作结果
 * - 系统备份任务记录
 * - 长时间运行任务的中间结果
 * 
 * 索引设计：
 * - 单字段索引：taskId（唯一），userId
 * - 复合索引：userId + createdTime（用户任务历史查询）
 * - 复合索引：organizationId + taskType（组织任务统计）
 * 
 * @author Nan
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "task_results")
@CompoundIndexes({
    @CompoundIndex(name = "idx_user_created", def = "{'userId': 1, 'createdTime': -1}"),
    @CompoundIndex(name = "idx_org_type", def = "{'organizationId': 1, 'taskType': 1}"),
    @CompoundIndex(name = "idx_status_created", def = "{'status': 1, 'completedTime': -1}")
})
public class TaskResult {
    
    /**
     * MongoDB文档ID
     */
    @Id
    private String id;
    
    /**
     * 任务唯一标识
     * 与MySQL中的task_id字段对应
     */
    @Indexed(unique = true)
    @Field("taskId")
    private String taskId;
    
    /**
     * 用户ID
     * 发起任务的用户
     */
    @Indexed
    @Field("userId")
    private String userId;
    
    /**
     * 用户姓名
     * 冗余存储，便于显示
     */
    @Field("userName")
    private String userName;
    
    /**
     * 组织ID
     * 用于多租户数据隔离
     */
    @Indexed
    @Field("organizationId")
    private String organizationId;
    
    /**
     * 任务类型
     */
    @Indexed
    @Field("taskType")
    private String taskType;
    
    /**
     * 任务名称
     */
    @Field("taskName")
    private String taskName;
    
    /**
     * 任务状态
     * SUCCESS, FAILED, CANCELLED
     */
    @Indexed
    @Field("status")
    private String status;
    
    /**
     * 任务执行结果数据
     * 
     * 根据任务类型存储不同的结果数据：
     * - 数据导出：exportType, fileUrl, fileName, fileSize, recordCount等
     * - 报告生成：reportType, reportUrl, generationTime, dataRange等
     * - 批量设备控制：deviceCount, successCount, failedCount, deviceResults等
     * - 系统备份：backupType, backupSize, backupLocation, checksum等
     */
    @Field("resultData")
    private Map<String, Object> resultData;
    
    /**
     * 任务执行参数
     * 记录任务执行时的输入参数，便于重现和调试
     */
    @Field("executionParams")
    private Map<String, Object> executionParams;
    
    /**
     * 任务执行日志
     * 存储任务执行过程中的关键日志信息
     */
    @Field("executionLogs")
    private List<TaskExecutionLog> executionLogs;
    
    /**
     * 错误信息
     * 任务失败时的详细错误描述
     */
    @Field("errorMessage")
    private String errorMessage;
    
    /**
     * 错误堆栈跟踪
     * 开发调试时的详细错误信息
     */
    @Field("errorStackTrace")
    private String errorStackTrace;
    
    /**
     * 任务执行性能指标
     * 记录任务执行的性能数据
     */
    @Field("performanceMetrics")
    private TaskPerformanceMetrics performanceMetrics;
    
    /**
     * 输出文件列表
     * 任务产生的文件信息
     */
    @Field("outputFiles")
    private List<TaskOutputFile> outputFiles;
    
    /**
     * 任务创建时间
     */
    @Indexed
    @Field("createdTime")
    private LocalDateTime createdTime;
    
    /**
     * 任务开始时间
     */
    @Field("startedTime")
    private LocalDateTime startedTime;
    
    /**
     * 任务完成时间
     */
    @Indexed
    @Field("completedTime")
    private LocalDateTime completedTime;
    
    /**
     * 任务执行时长（毫秒）
     */
    @Field("executionDurationMs")
    private Long executionDurationMs;
    
    /**
     * 任务执行日志内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskExecutionLog {
        
        /**
         * 日志时间戳
         */
        private LocalDateTime timestamp;
        
        /**
         * 日志级别
         * INFO, WARN, ERROR, DEBUG
         */
        private String level;
        
        /**
         * 日志消息
         */
        private String message;
        
        /**
         * 日志上下文数据
         */
        private Map<String, Object> context;
    }
    
    /**
     * 任务性能指标内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskPerformanceMetrics {
        
        /**
         * CPU使用时间（毫秒）
         */
        private Long cpuTimeMs;
        
        /**
         * 内存峰值使用量（字节）
         */
        private Long memoryPeakBytes;
        
        /**
         * 磁盘IO读取量（字节）
         */
        private Long diskReadBytes;
        
        /**
         * 磁盘IO写入量（字节）
         */
        private Long diskWriteBytes;
        
        /**
         * 网络IO下载量（字节）
         */
        private Long networkDownloadBytes;
        
        /**
         * 网络IO上传量（字节）
         */
        private Long networkUploadBytes;
        
        /**
         * 数据库查询次数
         */
        private Integer databaseQueryCount;
        
        /**
         * 处理的记录数量
         */
        private Long processedRecordCount;
    }
    
    /**
     * 任务输出文件内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskOutputFile {
        
        /**
         * 文件名
         */
        private String fileName;
        
        /**
         * 文件URL
         */
        private String fileUrl;
        
        /**
         * 文件大小（字节）
         */
        private Long fileSize;
        
        /**
         * 文件类型
         * excel, pdf, csv, json, xml等
         */
        private String fileType;
        
        /**
         * 文件MD5校验和
         */
        private String md5Checksum;
        
        /**
         * 文件过期时间
         */
        private LocalDateTime expiresAt;
        
        /**
         * 文件描述
         */
        private String description;
        
        /**
         * 下载次数统计
         */
        private Integer downloadCount;
        
        /**
         * 最后下载时间
         */
        private LocalDateTime lastDownloadTime;
    }
    
    /**
     * 计算任务执行时长
     * 
     * @return 执行时长（毫秒）
     */
    public Long calculateExecutionDuration() {
        if (startedTime != null && completedTime != null) {
            return java.time.Duration.between(startedTime, completedTime).toMillis();
        }
        return null;
    }
    
    /**
     * 检查任务是否成功
     * 
     * @return 是否成功
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }
    
    /**
     * 检查任务是否失败
     * 
     * @return 是否失败
     */
    public boolean isFailed() {
        return "FAILED".equals(status);
    }
    
    /**
     * 获取输出文件总大小
     * 
     * @return 文件总大小（字节）
     */
    public Long getTotalOutputFileSize() {
        if (outputFiles == null || outputFiles.isEmpty()) {
            return 0L;
        }
        return outputFiles.stream()
                .mapToLong(file -> file.getFileSize() != null ? file.getFileSize() : 0L)
                .sum();
    }
    
    /**
     * 检查是否有可下载的文件
     * 
     * @return 是否有输出文件
     */
    public boolean hasOutputFiles() {
        return outputFiles != null && !outputFiles.isEmpty();
    }
    
    /**
     * 添加执行日志
     * 
     * @param level 日志级别
     * @param message 日志消息
     * @param context 上下文数据
     */
    public void addExecutionLog(String level, String message, Map<String, Object> context) {
        if (executionLogs == null) {
            executionLogs = new java.util.ArrayList<>();
        }
        
        TaskExecutionLog log = TaskExecutionLog.builder()
                .timestamp(LocalDateTime.now())
                .level(level)
                .message(message)
                .context(context)
                .build();
        
        executionLogs.add(log);
    }
}