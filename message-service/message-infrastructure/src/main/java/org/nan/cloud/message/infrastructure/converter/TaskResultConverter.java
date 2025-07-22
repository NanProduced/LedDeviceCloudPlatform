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
    
    private List<TaskResult.TaskExecutionLog> convertLogEntries(List<TaskResultData.LogEntry> domainLogEntries) {
        if (domainLogEntries == null) {
            return null;
        }
        
        return domainLogEntries.stream()
            .map(entry -> {
                TaskResult.TaskExecutionLog infraEntry = new TaskResult.TaskExecutionLog();
                infraEntry.setTimestamp(entry.getTimestamp());
                infraEntry.setLevel(entry.getLevel());
                infraEntry.setMessage(entry.getMessage());
                // infraEntry.setSource(entry.getSource()); // TaskExecutionLog没有source字段
                infraEntry.setContext(entry.getContext());
                return infraEntry;
            })
            .collect(Collectors.toList());
    }
    
    private TaskResult.TaskPerformanceMetrics convertPerformanceMetrics(TaskResultData.PerformanceMetrics domainMetrics) {
        if (domainMetrics == null) {
            return null;
        }
        
        TaskResult.TaskPerformanceMetrics infraMetrics = new TaskResult.TaskPerformanceMetrics();
        infraMetrics.setMemoryPeakBytes(domainMetrics.getMemoryPeakBytes());
        infraMetrics.setCpuTimeMs(domainMetrics.getCpuTimeMs());
        // infraMetrics.setThreadCount(domainMetrics.getThreadCount()); // TaskPerformanceMetrics没有threadCount字段
        infraMetrics.setDiskReadBytes(domainMetrics.getDiskSpaceUsedBytes()); // 使用diskReadBytes代替
        infraMetrics.setNetworkDownloadBytes(domainMetrics.getNetworkBytesTransferred()); // 使用networkDownloadBytes代替
        // infraMetrics.setCustomMetrics(domainMetrics.getCustomMetrics()); // TaskPerformanceMetrics没有customMetrics字段
        return infraMetrics;
    }
    
    private List<TaskResult.TaskOutputFile> convertOutputFiles(List<TaskResultData.OutputFile> domainOutputFiles) {
        if (domainOutputFiles == null) {
            return null;
        }
        
        return domainOutputFiles.stream()
            .map(file -> {
                TaskResult.TaskOutputFile infraFile = new TaskResult.TaskOutputFile();
                infraFile.setFileName(file.getFileName());
                infraFile.setFileUrl(file.getFilePath()); // 使用fileUrl代替filePath
                infraFile.setFileType(file.getFileType());
                infraFile.setFileSize(file.getFileSize());
                infraFile.setMd5Checksum(file.getChecksum()); // 使用md5Checksum代替checksum
                // infraFile.setCreatedTime(file.getCreatedTime()); // TaskOutputFile没有createdTime字段
                infraFile.setDescription(file.getMetadata() != null ? file.getMetadata().toString() : null); // 使用description代替metadata
                return infraFile;
            })
            .collect(Collectors.toList());
    }
    
    private List<TaskResultData.LogEntry> convertLogEntriesFromInfra(List<TaskResult.TaskExecutionLog> infraLogEntries) {
        if (infraLogEntries == null) {
            return null;
        }
        
        return infraLogEntries.stream()
            .map(entry -> {
                TaskResultData.LogEntry domainEntry = new TaskResultData.LogEntry();
                domainEntry.setTimestamp(entry.getTimestamp());
                domainEntry.setLevel(entry.getLevel());
                domainEntry.setMessage(entry.getMessage());
                // domainEntry.setSource(entry.getSource()); // TaskExecutionLog没有source字段
                domainEntry.setContext(entry.getContext());
                return domainEntry;
            })
            .collect(Collectors.toList());
    }
    
    private TaskResultData.PerformanceMetrics convertPerformanceMetricsFromInfra(TaskResult.TaskPerformanceMetrics infraMetrics) {
        if (infraMetrics == null) {
            return null;
        }
        
        TaskResultData.PerformanceMetrics domainMetrics = new TaskResultData.PerformanceMetrics();
        domainMetrics.setMemoryPeakBytes(infraMetrics.getMemoryPeakBytes());
        domainMetrics.setCpuTimeMs(infraMetrics.getCpuTimeMs());
        // domainMetrics.setThreadCount(infraMetrics.getThreadCount()); // TaskPerformanceMetrics没有threadCount字段
        domainMetrics.setDiskSpaceUsedBytes(infraMetrics.getDiskReadBytes()); // 使用diskReadBytes
        domainMetrics.setNetworkBytesTransferred(infraMetrics.getNetworkDownloadBytes()); // 使用networkDownloadBytes
        // domainMetrics.setCustomMetrics(infraMetrics.getCustomMetrics()); // TaskPerformanceMetrics没有customMetrics字段
        return domainMetrics;
    }
    
    private List<TaskResultData.OutputFile> convertOutputFilesFromInfra(List<TaskResult.TaskOutputFile> infraOutputFiles) {
        if (infraOutputFiles == null) {
            return null;
        }
        
        return infraOutputFiles.stream()
            .map(file -> {
                TaskResultData.OutputFile domainFile = new TaskResultData.OutputFile();
                domainFile.setFileName(file.getFileName());
                domainFile.setFilePath(file.getFileUrl()); // 使用fileUrl
                domainFile.setFileType(file.getFileType());
                domainFile.setFileSize(file.getFileSize());
                domainFile.setChecksum(file.getMd5Checksum()); // 使用md5Checksum
                // domainFile.setCreatedTime(file.getCreatedTime()); // TaskOutputFile没有createdTime字段
                // domainFile.setMetadata(file.getMetadata()); // 使用description作为metadata
                return domainFile;
            })
            .collect(Collectors.toList());
    }
}