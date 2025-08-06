package org.nan.cloud.file.application.service;

import org.nan.cloud.file.api.dto.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务接口
 * 
 * 提供文件上传相关的业务逻辑处理：
 * - 单文件和批量文件上传
 * - 大文件分片上传
 * - 文件验证和安全检查
 * - 上传进度跟踪
 * - 存储策略选择
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface FileUploadService {

    /**
     * 异步单文件上传
     * 
     * @param file 上传的文件
     * @param request 上传请求参数
     * @return 任务初始化响应
     */
    TaskInitResponse uploadSingleAsync(MultipartFile file, FileUploadRequest request);

    /**
     * 初始化分片上传
     * 
     * @param request 分片上传初始化请求
     * @return 初始化结果
     */
    ChunkUploadInitResponse initChunkUpload(ChunkUploadInitRequest request);

    /**
     * 上传文件分片
     * 
     * @param chunk 文件分片
     * @param request 分片上传请求
     * @return 分片上传结果
     */
    ChunkUploadResponse uploadChunk(MultipartFile chunk, ChunkUploadRequest request);

    /**
     * 完成分片上传
     * 
     * @param request 分片上传完成请求
     * @return 完成结果
     */
    FileUploadResponse completeChunkUpload(ChunkUploadCompleteRequest request);

    /**
     * 取消分片上传
     * 
     * @param uploadId 上传ID
     */
    void cancelChunkUpload(String uploadId);

    /**
     * 获取上传进度
     * 
     * @param taskId 任务ID
     * @return 进度信息
     */
    UploadProgressResponse getUploadProgress(String taskId);

    /**
     * 获取支持的文件类型
     * 
     * @return 支持的文件类型
     */
    SupportedFileTypesResponse getSupportedFileTypes();

    /**
     * 验证文件是否合法
     * 
     * @param file 文件
     * @param request 上传请求
     * @return 验证结果
     */
    FileValidationService.FileValidationResult validateFile(MultipartFile file, FileUploadRequest request);

    /**
     * 计算文件MD5哈希值
     * 
     * @param file 文件
     * @return MD5哈希值
     */
    String calculateMD5(MultipartFile file);

    /**
     * 检查文件是否已存在（基于MD5去重）
     * 
     * @param md5Hash MD5哈希值
     * @param organizationId 组织ID
     * @return 存在的文件信息，不存在返回null
     */
    FileUploadResponse checkFileExists(String md5Hash, String organizationId);
}