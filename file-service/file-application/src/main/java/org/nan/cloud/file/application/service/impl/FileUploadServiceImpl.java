package org.nan.cloud.file.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.file.api.dto.*;
import org.nan.cloud.file.application.service.*;
import org.nan.cloud.file.application.domain.FileInfo;
import org.nan.cloud.file.application.domain.TaskContext;
import org.nan.cloud.file.application.repository.FileInfoRepository;
import org.nan.cloud.file.application.repository.MaterialMetadataRepository;
import org.nan.cloud.file.application.service.FileUploadEventService;
import org.nan.cloud.file.application.domain.MaterialMetadata;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 文件上传服务实现
 * 
 * TODO: 实现以下核心依赖服务：
 * - StorageService: 文件存储服务（本地存储、OSS存储）
 * - FileValidationService: 文件验证服务（格式、大小、安全检查）
 * - ProgressTrackingService: 进度跟踪服务（上传进度管理）
 * - ThumbnailService: 缩略图生成服务（图片/视频缩略图）
 * - FileInfoRepository: 数据访问层（文件元数据持久化）
 * - FileInfo领域对象: 核心领域模型
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    private final FileInfoRepository fileInfoRepository;
    
    // 支持多种存储策略
    private final StorageService storageService;
    
    // 文件格式和安全验证
    private final FileValidationService fileValidationService;
    
    // 上传进度跟踪
    private final ProgressTrackingService progressTrackingService;
    
    // 缩略图生成
    private final ThumbnailService thumbnailService;
    
    // 元数据分析服务
    private final MetadataAnalysisService metadataAnalysisService;
    
    // 元数据存储库
    private final MaterialMetadataRepository materialMetadataRepository;
    
    // 文件上传事件发布器
    private final FileUploadEventService eventPublisher;
    
    // 任务上下文服务
    private final TaskContextService taskContextService;


    @Override
    @Transactional
    public TaskInitResponse uploadSingleAsync(MultipartFile file, FileUploadRequest request) {
        log.info("开始异步单文件上传 - 文件名: {}, 大小: {}, 组织: {}", 
                file.getOriginalFilename(), file.getSize(), request.getOid());

        try {
            // 1. 文件验证
            FileValidationService.FileValidationResult validationResult = validateFile(file, request);
            if (!validationResult.isValid()) {
                throw new IllegalArgumentException("文件验证失败: " + validationResult.getErrorMessage());
            }

            // 2. 生成任务ID和文件ID
            String taskId = generateTaskId();
            String fileId = generateFileId();
            
            // 3. 立即创建任务上下文，状态为PENDING
            taskContextService.createTaskContext(taskId, fileId, request.getUid(), request.getOid(), 
                    file.getOriginalFilename(), file.getSize());
            
            // 4. 创建进度跟踪记录
            progressTrackingService.initializeProgress(taskId, file.getSize());
            
            // 5. 构建响应对象，立即返回给前端
            TaskInitResponse response = TaskInitResponse.builder()
                    .taskId(taskId)
                    .taskType("MATERIAL_UPLOAD")
                    .status("PENDING")
                    .filename(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .organizationId(request.getOid().toString())
                    .userId(request.getUid().toString())
                    .estimatedDuration("约30秒-2分钟")
                    .createTime(LocalDateTime.now())
                    .progressSubscriptionUrl("/stomp/topic/task/" + taskId) // 见StompTopic.java
                    .message("任务已创建，正在处理中...")
                    .build();
            
            // 6. 发布文件上传任务消息到core-service处理
            // 阶段1：创建任务和素材实体
            eventPublisher.publishUploadTaskCreated(taskId, fileId, request, file.getOriginalFilename(), 
                    file.getSize(), request.getOid().toString());
            
            // 7. 启动异步上传处理
            performAsyncUploadAsync(taskId, fileId, file, request);
            
            log.debug("异步单文件上传任务已创建 - taskId: {}, fileId: {}", taskId, fileId);
            return response;

        } catch (Exception e) {
            log.error("异步单文件上传初始化失败 - 文件名: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("异步上传初始化失败: " + e.getMessage(), e);
        }
    }



    @Override
    public UploadProgressResponse getUploadProgress(String taskId) {
        return progressTrackingService.getProgress(taskId);
    }

    @Override
    public SupportedFileTypesResponse getSupportedFileTypes() {
        return fileValidationService.getSupportedFileTypes();
    }

    @Override
    public FileValidationService.FileValidationResult validateFile(MultipartFile file, FileUploadRequest request) {
        return fileValidationService.validate(file, request);
    }

    @Override
    public String calculateMD5(MultipartFile file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(file.getBytes());
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
            
        } catch (Exception e) {
            log.error("计算文件MD5失败", e);
            throw new RuntimeException("计算文件MD5失败", e);
        }
    }

    @Override
    public FileUploadResponse checkFileExists(String md5Hash, String organizationId) {
        return fileInfoRepository.findByMd5HashAndOrganizationId(md5Hash, organizationId)
                .map(this::convertToUploadResponse)
                .orElse(null);
    }

    /**
     * 根据MD5检查文件是否已存在（不限制组织）
     */
    private FileInfo checkFileExistsByMd5(String md5Hash) {
        return fileInfoRepository.findByMd5Hash(md5Hash).orElse(null);
    }
    
    /**
     * 基于已存在文件创建新的文件信息记录
     */
    private FileInfo createFileInfoFromExisting(FileInfo existingFileInfo, String newFileId, 
                                               MultipartFile file, FileUploadRequest request) {
        return FileInfo.builder()
                .fileId(newFileId)
                .originalFilename(file.getOriginalFilename())
                .fileSize(existingFileInfo.getFileSize())
                .mimeType(existingFileInfo.getMimeType())
                .md5Hash(existingFileInfo.getMd5Hash())
                .fileExtension(existingFileInfo.getFileExtension())
                .storagePath(existingFileInfo.getStoragePath()) // 引用相同存储路径
                .storageType(existingFileInfo.getStorageType())
                .uploadTime(LocalDateTime.now()) // 新的上传时间
                .updateTime(LocalDateTime.now())
                .refCount(1L) // 新关联的引用计数为1
                .fileStatus(1) // 已完成状态
                .build();
    }

    // 私有辅助方法

    private String generateFileId() {
        return "file_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String generateTaskId() {
        return "task_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String generateUploadId() {
        return "upload_" + UUID.randomUUID().toString().replace("-", "");
    }

    private FileInfo createFileInfo(MultipartFile file, FileUploadRequest request, 
                                   String fileId, String md5Hash, String storagePath) {
        return FileInfo.builder()
                .fileId(fileId)
                .originalFilename(file.getOriginalFilename())
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .md5Hash(md5Hash)
                .fileExtension(getFileExtension(file.getOriginalFilename()))
                .storagePath(storagePath)
                .storageType("LOCAL") // 开发环境使用本地存储
                .uploadTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .refCount(1L)
                .fileStatus(1) // 默认已完成状态
                .build();
    }


    private FileUploadResponse buildUploadResponse(FileInfo fileInfo, String taskId) {
        return FileUploadResponse.builder()
                .fileId(fileInfo.getFileId())
                .originalFilename(fileInfo.getOriginalFilename())
                .fileSize(fileInfo.getFileSize())
                .fileType(determineFileTypeFromMime(fileInfo.getMimeType()))
                .mimeType(fileInfo.getMimeType())
                .md5Hash(fileInfo.getMd5Hash())
                .storagePath(fileInfo.getStoragePath())
                .accessUrl(storageService.generateAccessUrl(fileInfo.getStoragePath()))
                .status(FileUploadResponse.UploadStatus.SUCCESS)
                .taskId(taskId)
                .uploadTime(fileInfo.getUploadTime())
                .build();
    }

    private FileUploadResponse convertToUploadResponse(FileInfo fileInfo) {
        return FileUploadResponse.builder()
                .fileId(fileInfo.getFileId())
                .originalFilename(fileInfo.getOriginalFilename())
                .fileSize(fileInfo.getFileSize())
                .fileType(determineFileTypeFromMime(fileInfo.getMimeType()))
                .mimeType(fileInfo.getMimeType())
                .md5Hash(fileInfo.getMd5Hash())
                .storagePath(fileInfo.getStoragePath())
                .accessUrl(storageService.generateAccessUrl(fileInfo.getStoragePath()))
                .status(FileUploadResponse.UploadStatus.SUCCESS)
                .uploadTime(fileInfo.getUploadTime())
                .build();
    }

    private void asyncGenerateThumbnail(FileInfo fileInfo) {
        // 异步生成缩略图，带回调机制
        thumbnailService.generateThumbnailAsync(fileInfo, (fileId, result) -> {
            if (result.isSuccess() && result.getThumbnails() != null && !result.getThumbnails().isEmpty()) {
                // 获取主缩略图路径
                String primaryThumbnailPath = choosePrimaryThumbnail(result);

                // 更新MaterialFile表
                fileInfoRepository.updateFileThumbnail(fileId, primaryThumbnailPath);

                log.info("缩略图生成回调成功 - 文件ID: {}, 主缩略图: {}", fileId, primaryThumbnailPath);
            } else {
                log.warn("缩略图生成失败 - 文件ID: {}, 错误: {}", fileId, 
                        result.getErrorMessage() != null ? result.getErrorMessage() : "未知错误");
            }
        });
    }

    /**
     * 获取主缩略图路径（300x300优先，否则选择第一个）
     * @param result
     * @return
     */
    private static String choosePrimaryThumbnail(ThumbnailService.ThumbnailResult result) {
        String primaryThumbnailPath = null;
        for (ThumbnailService.ThumbnailInfo thumbnail : result.getThumbnails()) {
            if (thumbnail.getWidth() == 300 && thumbnail.getHeight() == 300) {
                primaryThumbnailPath = thumbnail.getThumbnailPath();
                break;
            }
        }

        // 如果没有300x300的，选择第一个作为主缩略图
        if (primaryThumbnailPath == null) {
            primaryThumbnailPath = result.getThumbnails().get(0).getThumbnailPath();
        }
        return primaryThumbnailPath;
    }

    /**
     * 异步分析和存储文件元数据
     * 
     * @param fileInfo 文件信息
     * @param taskId 任务ID
     * @return 元数据ID
     */
    private String asyncAnalyzeAndStoreMetadata(FileInfo fileInfo, String taskId) {
        try {
            log.info("开始分析文件元数据 - 文件ID: {}, 任务ID: {}", fileInfo.getFileId(), taskId);
            
            // 检查是否支持元数据分析
            if (!metadataAnalysisService.isSupported(fileInfo.getMimeType())) {
                log.debug("文件类型不支持元数据分析 - 文件ID: {}, MIME: {}", 
                        fileInfo.getFileId(), fileInfo.getMimeType());
                return null;
            }

            // 分析元数据
            MaterialMetadata metadata = metadataAnalysisService.analyzeMetadata(fileInfo, taskId);
            if (metadata == null) {
                log.warn("元数据分析失败 - 文件ID: {}", fileInfo.getFileId());
                return null;
            }

            // 存储到MongoDB
            String metadataId = materialMetadataRepository.save(metadata);
            
            log.info("文件元数据分析和存储完成 - 文件ID: {}, 元数据ID: {}", 
                    fileInfo.getFileId(), metadataId);
            return metadataId;
            
        } catch (Exception e) {
            log.error("分析和存储文件元数据失败 - 文件ID: {}, 错误: {}", 
                    fileInfo.getFileId(), e.getMessage(), e);
            return null;
        }
    }

    private String asyncStartTranscoding(FileInfo fileInfo, FileUploadRequest request) {
        // 异步启动转码任务
        return null;
    }

    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    private boolean isVideoFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("video/");
    }


    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex + 1) : "";
    }

    /**
     * 根据MIME类型确定文件类型
     */
    private String determineFileTypeFromMime(String mimeType) {
        if (mimeType == null) {
            return "DOCUMENT";
        }
        
        if (mimeType.startsWith("image/")) {
            return "IMAGE";
        } else if (mimeType.startsWith("video/")) {
            return "VIDEO";
        } else if (mimeType.startsWith("audio/")) {
            return "AUDIO";
        } else {
            return "DOCUMENT";
        }
    }

    // ==================== 异步处理方法 ====================

    /**
     * 异步执行文件上传处理
     * 
     * 使用Spring的@Async注解实现异步处理，在后台线程中执行实际的文件上传操作。
     * 包含完整的进度跟踪和事件发布流程。
     * 
     * @param taskId 任务ID
     * @param fileId 文件ID 
     * @param file 上传的文件
     * @param request 上传请求参数
     */
    @org.springframework.scheduling.annotation.Async("fileUploadTaskExecutor")
    public void performAsyncUploadAsync(String taskId, String fileId, MultipartFile file, FileUploadRequest request) {
        log.info("开始异步上传处理 - 任务ID: {}, 文件ID: {}, 文件名: {}", 
                taskId, fileId, file.getOriginalFilename());
        
        try {
            // 1. 更新任务状态为上传中
            taskContextService.updateTaskStatus(taskId, TaskContext.TaskStatus.UPLOADING);
            
            // 2. 发布上传开始事件
            eventPublisher.publishUploadStarted(taskId, request, file.getOriginalFilename(), 
                    file.getSize(), request.getOid().toString());
            
            // 3. 计算文件MD5
            String md5Hash = calculateMD5(file);
            updateProgressByPercent(taskId, 10, "计算文件哈希完成...");
            
            // 4. 检查文件是否已存在（异步场景下的秒传处理）
            FileInfo existingFileInfo = checkFileExistsByMd5(md5Hash);
            if (existingFileInfo != null) {
                log.info("异步上传发现重复文件，执行秒传逻辑 - MD5: {}", md5Hash);
                handleAsyncInstantUpload(taskId, fileId, existingFileInfo, file, request);
                return;
            }
            
            // 5. 存储文件到物理存储（使用正确的方法签名）
            updateProgressByPercent(taskId, 20, "开始文件存储...");
            String storagePath = storageService.store(file, request, fileId);
            updateProgressByPercent(taskId, 60, "文件存储完成...");
            
            // 6. 创建FileInfo对象（使用正确的方法签名）
            updateProgressByPercent(taskId, 90, "创建文件信息...");
            FileInfo fileInfo = createFileInfo(file, request, fileId, md5Hash, storagePath);
            fileInfoRepository.save(fileInfo);
            
            // 7. 更新任务状态为已上传
            taskContextService.updateTaskStatus(taskId, TaskContext.TaskStatus.UPLOADED);
            
            // 8. 构建上传响应对象（使用正确的方法签名）
            FileUploadResponse uploadResponse = buildUploadResponse(fileInfo, taskId);
            
            // 9. 异步处理，文件元数据解析（存入mongoDB）
            String metadataId = asyncAnalyzeAndStoreMetadata(fileInfo, taskId);

            // 10. 异步处理：自动生成缩略图（对图片和视频文件）
            if (isImageFile(file) || isVideoFile(file)) {
                asyncGenerateThumbnail(fileInfo);
            }

            // 11. 更新元数据ID到文件信息
            if (metadataId != null) {
                fileInfoRepository.updateFileMetadata(fileId, metadataId);
            }
            
            // 12. 更新进度为完成（在发布事件之前）
            progressTrackingService.completeProgress(taskId);
            taskContextService.updateTaskProgress(taskId, 100);
            
            // 13. 发布上传完成事件（包含完整文件信息）
            eventPublisher.publishUploadCompleted(taskId, uploadResponse, request.getOid().toString());

            // 14. 发布文件处理完成事件（包含元数据ID）
            if (metadataId != null) {
                eventPublisher.publishProcessingCompleted(taskId, fileId, metadataId, request.getOid().toString());
            }
            
            log.info("异步文件上传完成 - 任务ID: {}, 文件ID: {}, 存储路径: {}", 
                    taskId, fileId, storagePath);
                    
        } catch (Exception e) {
            log.error("异步文件上传失败 - 任务ID: {}, 文件名: {}, 错误: {}", 
                    taskId, file.getOriginalFilename(), e.getMessage(), e);
            
            // 处理上传失败
            handleAsyncUploadFailure(taskId, e.getMessage(), request.getOid().toString());
        }
    }
    
    /**
     * 异步场景下的秒传处理
     */
    private void handleAsyncInstantUpload(String taskId, String newFileId, FileInfo existingFileInfo, 
                                        MultipartFile file, FileUploadRequest request) {
        try {
            // 1. 快速更新进度到80%
            updateProgressByPercent(taskId, 80, "检测到重复文件，执行秒传...");
            
            // 2. 创建新的文件信息记录（引用已存在的物理文件）
            FileInfo newFileInfo = createFileInfoFromExisting(existingFileInfo, newFileId, file, request);
            fileInfoRepository.save(newFileInfo);
            
            // 3. 更新原文件的引用计数
            existingFileInfo.setRefCount(existingFileInfo.getRefCount() + 1);
            fileInfoRepository.save(existingFileInfo);
            
            // 4. 构建响应对象
            FileUploadResponse uploadResponse = buildUploadResponse(newFileInfo, taskId);
            
            // 5. 更新进度为完成（在发布事件之前）
            progressTrackingService.completeProgress(taskId);
            taskContextService.updateTaskStatus(taskId, TaskContext.TaskStatus.COMPLETED);
            taskContextService.updateTaskProgress(taskId, 100);
            
            // 6. 发布上传完成事件
            eventPublisher.publishUploadCompleted(taskId, uploadResponse, request.getOid().toString());
            
            log.info("异步秒传完成 - 任务ID: {}, 新文件ID: {}, 原文件ID: {}", 
                    taskId, newFileId, existingFileInfo.getFileId());
                    
        } catch (Exception e) {
            log.error("异步秒传失败 - 任务ID: {}, 错误: {}", taskId, e.getMessage(), e);
            handleAsyncUploadFailure(taskId, "秒传失败: " + e.getMessage(), request.getOid().toString());
        }
    }
    
    /**
     * 处理异步上传失败
     */
    private void handleAsyncUploadFailure(String taskId, String errorMessage, String organizationId) {
        try {
            // 1. 更新任务状态为失败
            TaskContext context = taskContextService.getTaskContext(taskId);
            if (context != null) {
                context.markFailed(errorMessage);
                taskContextService.updateTaskStatus(taskId, TaskContext.TaskStatus.FAILED);
            }
            
            // 2. 发布上传失败事件
            eventPublisher.publishUploadFailed(taskId, errorMessage, organizationId);
            
        } catch (Exception e) {
            log.error("处理异步上传失败时出错 - 任务ID: {}, 错误: {}", taskId, e.getMessage(), e);
        }
    }
    
    /**
     * 更新上传进度 - 统一通过ProgressTrackingService管理
     * 
     * @param taskId 任务ID
     * @param uploadedBytes 已上传字节数（用于计算真实进度和传输速度）
     * @param status 状态描述
     */
    private void updateProgress(String taskId, long uploadedBytes, String status) {
        try {
            // 1. 通过ProgressTrackingService更新进度（包含速度计算等）
            progressTrackingService.updateProgress(taskId, uploadedBytes);
            
            // 2. 获取计算后的进度信息
            UploadProgressResponse progressResponse = progressTrackingService.getProgress(taskId);
            if (progressResponse != null) {
                // 3. 更新任务上下文中的进度
                taskContextService.updateTaskProgress(taskId, progressResponse.getProgress().intValue());
                
                // 4. 发布进度事件到前端（包含丰富的进度信息）
                eventPublisher.publishUploadProgress(taskId, progressResponse.getProgress().intValue(), status,
                        progressResponse.getUploadSpeed(), progressResponse.getUploadedSize(), progressResponse.getTotalSize());
            }
            
            log.debug("更新上传进度 - 任务ID: {}, 已上传: {}字节, 状态: {}", taskId, uploadedBytes, status);
            
        } catch (Exception e) {
            log.warn("更新上传进度失败 - 任务ID: {}, 错误: {}", taskId, e.getMessage());
        }
    }
    
    /**
     * 便捷方法：基于百分比更新进度（兼容现有代码）
     */
    private void updateProgressByPercent(String taskId, int progressPercent, String status) {
        try {
            // 获取任务上下文中的总文件大小
            TaskContext context = taskContextService.getTaskContext(taskId);
            if (context != null && context.getFileSize() != null) {
                long totalSize = context.getFileSize();
                long uploadedBytes = (long) (totalSize * progressPercent / 100.0);
                updateProgress(taskId, uploadedBytes, status);
            } else {
                // 如果无法获取文件大小，直接更新任务上下文
                taskContextService.updateTaskProgress(taskId, progressPercent);
                eventPublisher.publishUploadProgress(taskId, progressPercent, status);
            }
        } catch (Exception e) {
            log.warn("按百分比更新进度失败 - 任务ID: {}, 错误: {}", taskId, e.getMessage());
        }
    }
}