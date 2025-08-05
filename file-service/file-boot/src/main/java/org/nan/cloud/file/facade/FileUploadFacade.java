package org.nan.cloud.file.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.web.context.InvocationContextHolder;
import org.nan.cloud.common.web.context.RequestUserInfo;
import org.nan.cloud.file.api.dto.FileUploadRequest;
import org.nan.cloud.file.api.dto.FileUploadResponse;
import org.nan.cloud.file.api.dto.TaskInitResponse;
import org.nan.cloud.file.application.service.FileUploadService;
import org.nan.cloud.file.application.service.FileValidationService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传门面服务
 * 
 * 职责：
 * 1. 作为Controller和Application层之间的中间层
 * 2. 处理跨模块依赖和复杂业务编排
 * 3. 填充请求上下文信息（如组织ID）
 * 4. 统一异常处理和日志记录
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadFacade {

    private final FileUploadService fileUploadService;
    private final FileValidationService fileValidationService;

    /**
     * 单文件上传门面方法
     * 
     * @param file 上传的文件
     * @param uploadRequest 上传请求参数
     * @return 上传响应
     */
    public FileUploadResponse uploadSingle(MultipartFile file, FileUploadRequest uploadRequest) {
        log.info("门面层处理单文件上传 - 文件名: {}, 大小: {}", 
                file.getOriginalFilename(), file.getSize());

        try {
            // 1. 填充上下文信息
            enrichUploadRequest(uploadRequest);
            
            // 2. 参数验证
            validateUploadRequest(file, uploadRequest);
            
            // 3. 调用应用服务执行上传
            FileUploadResponse response = fileUploadService.uploadSingle(file, uploadRequest);
            
            log.info("门面层单文件上传完成 - 文件ID: {}, 任务ID: {}", 
                    response.getFileId(), response.getTaskId());
            
            return response;
            
        } catch (Exception e) {
            log.error("门面层单文件上传失败 - 文件名: {}, 错误: {}", 
                    file.getOriginalFilename(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 填充上传请求的上下文信息
     * 
     * @param uploadRequest 上传请求
     */
    private void enrichUploadRequest(FileUploadRequest uploadRequest) {
        // 从用户上下文获取组织ID
        RequestUserInfo requestUser = InvocationContextHolder.getContext().getRequestUser();
        uploadRequest.setOid(requestUser.getOid());
        uploadRequest.setUid(requestUser.getUid());
        uploadRequest.setUgid(requestUser.getUgid());
        
        log.debug("填充上传请求上下文 - 组织ID: {} , 用户ID: {}", requestUser.getOid(), requestUser.getUid());
    }

    /**
     * 验证上传请求参数
     * 
     * @param file 上传文件
     * @param uploadRequest 上传请求
     */
    private void validateUploadRequest(MultipartFile file, FileUploadRequest uploadRequest) {
        // 基础业务参数验证
        if (uploadRequest.getOid() == null) {
            throw new IllegalArgumentException("无法获取用户组织信息");
        }
        
        // 使用统一的文件验证服务进行详细验证
        FileValidationService.FileValidationResult validationResult = 
                fileValidationService.validate(file, uploadRequest);
        
        if (!validationResult.isValid()) {
            throw new IllegalArgumentException(validationResult.getErrorMessage());
        }
        
        log.debug("上传请求参数验证通过 - 文件名: {}, 大小: {}, 组织: {}", 
                file.getOriginalFilename(), file.getSize(), uploadRequest.getOid());
    }

    /**
     * 异步单文件上传门面方法
     * 
     * @param file 上传的文件
     * @param uploadRequest 上传请求参数
     * @return 任务初始化响应
     */
    public TaskInitResponse uploadSingleAsync(MultipartFile file, FileUploadRequest uploadRequest) {
        log.info("门面层处理异步单文件上传 - 文件名: {}, 大小: {}", 
                file.getOriginalFilename(), file.getSize());

        try {
            // 1. 填充上下文信息
            enrichUploadRequest(uploadRequest);
            
            // 2. 参数验证
            validateUploadRequest(file, uploadRequest);
            
            // 3. 调用应用服务执行异步上传
            TaskInitResponse response = fileUploadService.uploadSingleAsync(file, uploadRequest);
            
            log.info("门面层异步上传任务创建完成 - 任务ID: {}", response.getTaskId());
            
            return response;
            
        } catch (Exception e) {
            log.error("门面层异步单文件上传失败 - 文件名: {}, 错误: {}", 
                    file.getOriginalFilename(), e.getMessage(), e);
            throw e;
        }
    }
}
