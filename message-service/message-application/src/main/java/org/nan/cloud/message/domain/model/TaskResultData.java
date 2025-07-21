package org.nan.cloud.message.domain.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 任务结果领域模型
 * 
 * 任务结果的领域对象，封装任务执行的详细结果信息。
 * 避免application层直接依赖infrastructure层的具体实现类。
 * 
 * @author Nan
 * @since 1.0.0
 */
@Data
public class TaskResultData {
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 组织ID
     */
    private String organizationId;
    
    /**
     * 任务类型
     */
    private String taskType;
    
    /**
     * 任务名称
     */
    private String taskName;
    
    /**
     * 任务状态
     */
    private String status;
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 错误消息
     */
    private String errorMessage;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 开始时间
     */
    private LocalDateTime startedTime;
    
    /**
     * 完成时间
     */
    private LocalDateTime completedTime;
    
    /**
     * 执行时长（毫秒）
     */
    private Long executionDurationMs;
    
    /**
     * 结果数据
     */
    private Map<String, Object> resultData;
    
    /**
     * 执行参数
     */
    private Map<String, Object> executionParams;
    
    /**
     * 执行日志
     */
    private List<LogEntry> executionLogs;
    
    /**
     * 性能指标
     */
    private PerformanceMetrics performanceMetrics;
    
    /**
     * 输出文件
     */
    private List<OutputFile> outputFiles;
    
    /**
     * 日志条目
     */
    @Data
    public static class LogEntry {
        private LocalDateTime timestamp;
        private String level;
        private String message;
        private String source;
        private Map<String, Object> context;
    }
    
    /**
     * 性能指标
     */
    @Data
    public static class PerformanceMetrics {
        private Long memoryPeakBytes;
        private Long cpuTimeMs;
        private Integer threadCount;
        private Long diskSpaceUsedBytes;
        private Long networkBytesTransferred;
        private Map<String, Object> customMetrics;
    }
    
    /**
     * 输出文件
     */
    @Data
    public static class OutputFile {
        private String fileName;
        private String filePath;
        private String fileType;
        private Long fileSize;
        private String checksum;
        private LocalDateTime createdTime;
        private Map<String, Object> metadata;
    }
}