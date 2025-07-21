package org.nan.cloud.message.infrastructure.converter;

import org.nan.cloud.message.domain.model.TaskResultData;
import org.nan.cloud.message.infrastructure.mongodb.document.TaskResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务结果转换器
 * 
 * 负责在领域模型（TaskResultData）和基础设施层实体（TaskResult）之间进行转换，
 * 隔离不同层次的数据模型，保持架构的清晰性。
 * 
 * @author Nan
 * @since 1.0.0
 */
@Component
public class TaskResultConverter {
    
    /**
     * 将领域模型转换为基础设施实体
     * 
     * @param domainModel 领域模型
     * @return 基础设施实体
     */
    public TaskResult toInfrastructureEntity(TaskResultData domainModel) {
        if (domainModel == null) {
            return null;
        }
        
        return TaskResult.builder()
            .taskId(domainModel.getTaskId())
            .userId(domainModel.getUserId())
            .organizationId(domainModel.getOrganizationId())
            .taskType(domainModel.getTaskType())
            .taskName(domainModel.getTaskName())
            .status(domainModel.getStatus())
            .success(domainModel.isSuccess())
            .errorMessage(domainModel.getErrorMessage())
            .createdTime(domainModel.getCreatedTime())
            .startedTime(domainModel.getStartedTime())
            .completedTime(domainModel.getCompletedTime())
            .executionDurationMs(domainModel.getExecutionDurationMs())
            .resultData(domainModel.getResultData())
            .executionParams(domainModel.getExecutionParams())
            .executionLogs(convertLogEntries(domainModel.getExecutionLogs()))
            .performanceMetrics(convertPerformanceMetrics(domainModel.getPerformanceMetrics()))
            .outputFiles(convertOutputFiles(domainModel.getOutputFiles()))
            .build();
    }
    
    /**
     * 将基础设施实体转换为领域模型
     * 
     * @param infrastructureEntity 基础设施实体
     * @return 领域模型
     */
    public TaskResultData toDomainModel(TaskResult infrastructureEntity) {
        if (infrastructureEntity == null) {
            return null;
        }
        
        TaskResultData domainModel = new TaskResultData();
        domainModel.setTaskId(infrastructureEntity.getTaskId());
        domainModel.setUserId(infrastructureEntity.getUserId());
        domainModel.setOrganizationId(infrastructureEntity.getOrganizationId());
        domainModel.setTaskType(infrastructureEntity.getTaskType());
        domainModel.setTaskName(infrastructureEntity.getTaskName());
        domainModel.setStatus(infrastructureEntity.getStatus());
        domainModel.setSuccess(infrastructureEntity.isSuccess());
        domainModel.setErrorMessage(infrastructureEntity.getErrorMessage());
        domainModel.setCreatedTime(infrastructureEntity.getCreatedTime());
        domainModel.setStartedTime(infrastructureEntity.getStartedTime());
        domainModel.setCompletedTime(infrastructureEntity.getCompletedTime());
        domainModel.setExecutionDurationMs(infrastructureEntity.getExecutionDurationMs());
        domainModel.setResultData(infrastructureEntity.getResultData());
        domainModel.setExecutionParams(infrastructureEntity.getExecutionParams());
        domainModel.setExecutionLogs(convertLogEntriesFromInfra(infrastructureEntity.getExecutionLogs()));
        domainModel.setPerformanceMetrics(convertPerformanceMetricsFromInfra(infrastructureEntity.getPerformanceMetrics()));
        domainModel.setOutputFiles(convertOutputFilesFromInfra(infrastructureEntity.getOutputFiles()));
        
        return domainModel;
    }
    
    // ==================== 私有转换方法 ====================
    
    private List<TaskResult.LogEntry> convertLogEntries(List<TaskResultData.LogEntry> domainLogEntries) {
        if (domainLogEntries == null) {
            return null;
        }
        
        return domainLogEntries.stream()
            .map(entry -> {
                TaskResult.LogEntry infraEntry = new TaskResult.LogEntry();
                infraEntry.setTimestamp(entry.getTimestamp());
                infraEntry.setLevel(entry.getLevel());
                infraEntry.setMessage(entry.getMessage());
                infraEntry.setSource(entry.getSource());
                infraEntry.setContext(entry.getContext());
                return infraEntry;
            })
            .collect(Collectors.toList());
    }
    
    private TaskResult.PerformanceMetrics convertPerformanceMetrics(TaskResultData.PerformanceMetrics domainMetrics) {
        if (domainMetrics == null) {
            return null;
        }
        
        TaskResult.PerformanceMetrics infraMetrics = new TaskResult.PerformanceMetrics();
        infraMetrics.setMemoryPeakBytes(domainMetrics.getMemoryPeakBytes());
        infraMetrics.setCpuTimeMs(domainMetrics.getCpuTimeMs());
        infraMetrics.setThreadCount(domainMetrics.getThreadCount());
        infraMetrics.setDiskSpaceUsedBytes(domainMetrics.getDiskSpaceUsedBytes());
        infraMetrics.setNetworkBytesTransferred(domainMetrics.getNetworkBytesTransferred());
        infraMetrics.setCustomMetrics(domainMetrics.getCustomMetrics());
        return infraMetrics;
    }
    
    private List<TaskResult.OutputFile> convertOutputFiles(List<TaskResultData.OutputFile> domainOutputFiles) {
        if (domainOutputFiles == null) {
            return null;
        }
        
        return domainOutputFiles.stream()
            .map(file -> {
                TaskResult.OutputFile infraFile = new TaskResult.OutputFile();
                infraFile.setFileName(file.getFileName());
                infraFile.setFilePath(file.getFilePath());
                infraFile.setFileType(file.getFileType());
                infraFile.setFileSize(file.getFileSize());
                infraFile.setChecksum(file.getChecksum());
                infraFile.setCreatedTime(file.getCreatedTime());
                infraFile.setMetadata(file.getMetadata());
                return infraFile;
            })
            .collect(Collectors.toList());
    }
    
    private List<TaskResultData.LogEntry> convertLogEntriesFromInfra(List<TaskResult.LogEntry> infraLogEntries) {
        if (infraLogEntries == null) {
            return null;
        }
        
        return infraLogEntries.stream()
            .map(entry -> {
                TaskResultData.LogEntry domainEntry = new TaskResultData.LogEntry();
                domainEntry.setTimestamp(entry.getTimestamp());
                domainEntry.setLevel(entry.getLevel());
                domainEntry.setMessage(entry.getMessage());
                domainEntry.setSource(entry.getSource());
                domainEntry.setContext(entry.getContext());
                return domainEntry;
            })
            .collect(Collectors.toList());
    }
    
    private TaskResultData.PerformanceMetrics convertPerformanceMetricsFromInfra(TaskResult.PerformanceMetrics infraMetrics) {
        if (infraMetrics == null) {
            return null;
        }
        
        TaskResultData.PerformanceMetrics domainMetrics = new TaskResultData.PerformanceMetrics();
        domainMetrics.setMemoryPeakBytes(infraMetrics.getMemoryPeakBytes());
        domainMetrics.setCpuTimeMs(infraMetrics.getCpuTimeMs());
        domainMetrics.setThreadCount(infraMetrics.getThreadCount());
        domainMetrics.setDiskSpaceUsedBytes(infraMetrics.getDiskSpaceUsedBytes());
        domainMetrics.setNetworkBytesTransferred(infraMetrics.getNetworkBytesTransferred());
        domainMetrics.setCustomMetrics(infraMetrics.getCustomMetrics());
        return domainMetrics;
    }
    
    private List<TaskResultData.OutputFile> convertOutputFilesFromInfra(List<TaskResult.OutputFile> infraOutputFiles) {
        if (infraOutputFiles == null) {
            return null;
        }
        
        return infraOutputFiles.stream()
            .map(file -> {
                TaskResultData.OutputFile domainFile = new TaskResultData.OutputFile();
                domainFile.setFileName(file.getFileName());
                domainFile.setFilePath(file.getFilePath());
                domainFile.setFileType(file.getFileType());
                domainFile.setFileSize(file.getFileSize());
                domainFile.setChecksum(file.getChecksum());
                domainFile.setCreatedTime(file.getCreatedTime());
                domainFile.setMetadata(file.getMetadata());
                return domainFile;
            })
            .collect(Collectors.toList());
    }
}