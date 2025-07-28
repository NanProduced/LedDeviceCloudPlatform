package org.nan.cloud.file.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.web.DynamicResponse;
import org.nan.cloud.file.api.FileUploadApi;
import org.nan.cloud.file.api.dto.*;
import org.nan.cloud.file.application.service.FileUploadService;
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

    private final FileUploadService fileUploadService;

    @Override
    public FileUploadResponse uploadSingle(
            @Parameter(description = "上传的文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "上传参数") @ModelAttribute FileUploadRequest uploadRequest) {
        
        log.info("接收单文件上传请求 - 文件名: {}, 大小: {}, 组织: {}", 
                file.getOriginalFilename(), file.getSize(), uploadRequest.getOrganizationId());

        FileUploadResponse response = fileUploadService.uploadSingle(file, uploadRequest);
        
        log.info("单文件上传成功 - 文件ID: {}, 文件名: {}", 
                response.getFileId(), response.getOriginalFilename());

        return response;
    }

    @Override
    public BatchFileUploadResponse uploadBatch(
            @Parameter(description = "上传的文件列表") @RequestParam("files") MultipartFile[] files,
            @Parameter(description = "上传参数") @ModelAttribute FileUploadRequest uploadRequest) {
        
        log.info("接收批量文件上传请求 - 文件数量: {}, 组织: {}", 
                files.length, uploadRequest.getOrganizationId());

        BatchFileUploadResponse response = fileUploadService.uploadBatch(files, uploadRequest);
        
        log.info("批量文件上传完成 - 总数: {}, 成功: {}, 失败: {}", 
                response.getTotalFiles(), response.getSuccessCount(), response.getFailedCount());
        
        return response;
    }

    @Override
    public ChunkUploadInitResponse initChunkUpload(
            @RequestBody ChunkUploadInitRequest request) {
        
        log.info("初始化分片上传 - 文件名: {}, 文件大小: {}, 分片大小: {}", 
                request.getFilename(), request.getFileSize(), request.getChunkSize());

        ChunkUploadInitResponse response = fileUploadService.initChunkUpload(request);
        
        log.info("分片上传初始化成功 - 上传ID: {}, 总分片数: {}", 
                response.getUploadId(), response.getTotalChunks());
        
        return response;
    }

    @Override
    public ChunkUploadResponse uploadChunk(
            @Parameter(description = "文件分片") @RequestParam("chunk") MultipartFile chunk,
            @Parameter(description = "分片信息") @ModelAttribute ChunkUploadRequest request) {
        
        log.debug("上传文件分片 - 上传ID: {}, 分片号: {}/{}", 
                request.getUploadId(), request.getChunkNumber(), request.getTotalChunks());

        ChunkUploadResponse response = fileUploadService.uploadChunk(chunk, request);
        
        log.debug("文件分片上传成功 - 上传ID: {}, 分片号: {}", 
                request.getUploadId(), request.getChunkNumber());
        
        return response;
    }

    @Override
    public FileUploadResponse completeChunkUpload(
            @RequestBody ChunkUploadCompleteRequest request) {
        
        log.info("完成分片上传 - 上传ID: {}", request.getUploadId());

        FileUploadResponse response = fileUploadService.completeChunkUpload(request);
        
        log.info("分片上传完成 - 文件ID: {}, 上传ID: {}", 
                response.getFileId(), request.getUploadId());
        
        return response;
    }

    @Override
    public void cancelChunkUpload(
            @Parameter(description = "上传任务ID") @PathVariable String uploadId) {
        
        log.info("取消分片上传 - 上传ID: {}", uploadId);

        fileUploadService.cancelChunkUpload(uploadId);
        
        log.info("分片上传已取消 - 上传ID: {}", uploadId);
    }

    @Override
    public UploadProgressResponse getUploadProgress(
            @Parameter(description = "上传任务ID") @PathVariable String taskId) {
        
        log.debug("查询上传进度 - 任务ID: {}", taskId);

        return fileUploadService.getUploadProgress(taskId);
    }

    @Override
    public SupportedFileTypesResponse getSupportedFileTypes() {
        
        log.debug("获取支持的文件类型");

        return fileUploadService.getSupportedFileTypes();
    }

    /**
     * 获取文件上传统计信息
     * 
     * @param organizationId 组织ID
     * @return 上传统计信息
     */
    @Operation(summary = "获取上传统计", description = "获取组织的文件上传统计信息")
    @GetMapping("/file/upload/statistics/{organizationId}")
    public FileUploadStatistics getUploadStatistics(
            @Parameter(description = "组织ID") @PathVariable String organizationId) {
        
        log.info("获取上传统计信息 - 组织: {}", organizationId);

        // TODO: 实现上传统计功能
        return FileUploadStatistics.builder()
                .organizationId(organizationId)
                .totalFiles(0L)
                .totalSize(0L)
                .todayUploads(0L)
                .thisWeekUploads(0L)
                .thisMonthUploads(0L)
                .build();
    }

    /**
     * 检查文件是否已存在（基于MD5去重）
     * 
     * @param request MD5检查请求
     * @return 文件存在性检查结果
     */
    @Operation(summary = "MD5文件去重检查", description = "检查文件是否已存在以实现去重")
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
     * 文件上传统计信息
     */
    public static class FileUploadStatistics {
        private String organizationId;
        private Long totalFiles;
        private Long totalSize;
        private Long todayUploads;
        private Long thisWeekUploads;
        private Long thisMonthUploads;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final FileUploadStatistics statistics = new FileUploadStatistics();

            public Builder organizationId(String organizationId) {
                statistics.organizationId = organizationId;
                return this;
            }

            public Builder totalFiles(Long totalFiles) {
                statistics.totalFiles = totalFiles;
                return this;
            }

            public Builder totalSize(Long totalSize) {
                statistics.totalSize = totalSize;
                return this;
            }

            public Builder todayUploads(Long todayUploads) {
                statistics.todayUploads = todayUploads;
                return this;
            }

            public Builder thisWeekUploads(Long thisWeekUploads) {
                statistics.thisWeekUploads = thisWeekUploads;
                return this;
            }

            public Builder thisMonthUploads(Long thisMonthUploads) {
                statistics.thisMonthUploads = thisMonthUploads;
                return this;
            }

            public FileUploadStatistics build() {
                return statistics;
            }
        }

        // Getters
        public String getOrganizationId() { return organizationId; }
        public Long getTotalFiles() { return totalFiles; }
        public Long getTotalSize() { return totalSize; }
        public Long getTodayUploads() { return todayUploads; }
        public Long getThisWeekUploads() { return thisWeekUploads; }
        public Long getThisMonthUploads() { return thisMonthUploads; }
    }

    /**
     * 文件存在性检查请求
     */
    public static class FileExistenceCheckRequest {
        private String md5Hash;
        private String organizationId;

        // Getters and Setters
        public String getMd5Hash() { return md5Hash; }
        public void setMd5Hash(String md5Hash) { this.md5Hash = md5Hash; }

        public String getOrganizationId() { return organizationId; }
        public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    }

    /**
     * 文件存在性检查响应
     */
    public static class FileExistenceCheckResponse {
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

        // Getters
        public boolean isExists() { return exists; }
        public FileUploadResponse getExistingFile() { return existingFile; }
    }
}