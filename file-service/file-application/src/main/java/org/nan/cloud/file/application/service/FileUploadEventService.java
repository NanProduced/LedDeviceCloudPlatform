package org.nan.cloud.file.application.service;

import org.nan.cloud.file.api.dto.FileUploadRequest;
import org.nan.cloud.file.api.dto.FileUploadResponse;

public interface FileUploadEventService {

    void publishUploadStarted(String taskId, FileUploadRequest uploadRequest,
                              String originalFilename, Long fileSize, String organizationId);

    void publishUploadTaskCreated(String taskId, String fileId, FileUploadRequest uploadRequest,
                                  String originalFilename, Long fileSize, String organizationId);

    void publishUploadProgress(String taskId, int progress, String status);
    
    /**
     * 发布文件上传进度事件（详细版本）
     * 
     * @param taskId 任务ID
     * @param progress 进度百分比
     * @param status 状态描述
     * @param speed 传输速度（字节/秒）
     * @param uploadedBytes 已上传字节数
     * @param totalBytes 总字节数
     */
    void publishUploadProgress(String taskId, int progress, String status, 
                             long speed, long uploadedBytes, long totalBytes);

    void publishUploadCompleted(String taskId, FileUploadResponse uploadResponse, String organizationId);

    /**
     * 发布文件处理完成事件
     * @param taskId 任务ID
     * @param fileId 文件ID
     * @param metadataId 元数据ID
     * @param organizationId 组织ID
     */
    void publishProcessingCompleted(String taskId, String fileId, String metadataId, String organizationId);

    void publishUploadFailed(String taskId, String errorMessage, String organizationId);
}
