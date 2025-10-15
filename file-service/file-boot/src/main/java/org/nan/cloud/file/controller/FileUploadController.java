package org.nan.cloud.file.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.web.DynamicResponse;
import org.nan.cloud.file.api.FileUploadApi;
import org.nan.cloud.file.api.dto.*;
import org.nan.cloud.file.api.dto.TaskInitResponse;
import org.nan.cloud.file.application.service.FileUploadService;
import org.nan.cloud.file.facade.FileUploadFacade;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器
 * 
 * 实现文件上传相关的REST API接口
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "文件上传", description = "文件上传相关接口")
public class FileUploadController implements FileUploadApi {

    private final FileUploadFacade fileUploadFacade;

    private final FileUploadService fileUploadService;

    /**
     * 异步上传单个文件
     * 
     * @param file 上传的文件
     * @param uploadRequest 上传参数
     * @return 任务初始化响应
     */
    @Operation(
            summary = "异步上传单个文件",
            description = "异步上传单个文件，立即返回任务ID",
            tags = {"素材管理", "文件上传"}
    )
    @Override
    public TaskInitResponse uploadSingleAsync(
            @Parameter(description = "上传的文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "上传参数") @ModelAttribute FileUploadRequest uploadRequest) {
        
        log.info("接收异步单文件上传请求 - 文件名: {}, 大小: {}", 
                file.getOriginalFilename(), file.getSize());

        TaskInitResponse response = fileUploadFacade.uploadSingleAsync(file, uploadRequest);
        
        log.info("异步上传任务创建成功 - 任务ID: {}, 文件名: {}", 
                response.getTaskId(), response.getFilename());

        return response;
    }


    @Operation(
            summary = "查询上传进度",
            description = "根据任务Id查询任务具体进度",
            tags = {"素材管理", "文件上传"}
    )
    @Override
    public UploadProgressResponse getUploadProgress(
            @Parameter(description = "上传任务ID") @PathVariable String taskId) {
        
        log.debug("查询上传进度 - 任务ID: {}", taskId);

        return fileUploadService.getUploadProgress(taskId);
    }

    @Operation(
            summary = "服务器支持的文件类型",
            description = "获取服务器支持的文件类型列表",
            tags = {"文件上传"}
    )
    @Override
    public SupportedFileTypesResponse getSupportedFileTypes() {
        
        log.debug("获取支持的文件类型");

        return fileUploadService.getSupportedFileTypes();
    }

    /**
     * 检查文件是否已存在（基于MD5去重）
     * 
     * @param request MD5检查请求
     * @return 文件存在性检查结果
     */
    @Operation(
            summary = "MD5文件去重检查",
            description = "检查文件是否已存在以实现去重",
            tags = {"内部接口"}
    )
    @PostMapping("/file/upload/check-duplicate")
    public FileExistenceCheckResponse checkFileDuplicate(
            @RequestBody FileExistenceCheckRequest request) {
        
        log.info("检查文件重复 - MD5: {}, 组织: {}", request.getMd5Hash(), request.getOrganizationId());

        FileUploadResponse existingFile = fileUploadService.checkFileExists(
                request.getMd5Hash(), request.getOrganizationId());
        
        return FileExistenceCheckResponse.builder()
                .exists(existingFile != null)
                .existingFile(existingFile)
                .build();
    }

    /**
     * 文件存在性检查请求
     */
    @Data
    public static class FileExistenceCheckRequest {
        // Getters and Setters
        private String md5Hash;
        private String organizationId;

    }

    /**
     * 文件存在性检查响应
     */
    @Data
    public static class FileExistenceCheckResponse {
        // Getters
        private boolean exists;
        private FileUploadResponse existingFile;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final FileExistenceCheckResponse response = new FileExistenceCheckResponse();

            public Builder exists(boolean exists) {
                response.exists = exists;
                return this;
            }

            public Builder existingFile(FileUploadResponse existingFile) {
                response.existingFile = existingFile;
                return this;
            }

            public FileExistenceCheckResponse build() {
                return response;
            }
        }

    }
}