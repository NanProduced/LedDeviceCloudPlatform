package org.nan.cloud.file.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.nan.cloud.common.web.DynamicResponse;
import org.nan.cloud.file.api.dto.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传API接口
 * 
 * 功能说明：
 * - 支持单文件和批量文件上传
 * - 支持大文件分片上传
 * - 支持断点续传
 * - 文件类型验证和安全检查
 * - 上传进度实时推送
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Tag(name = "文件上传", description = "文件上传相关接口")
@RequestMapping("/file/upload")
public interface FileUploadApi {

    /**
     * 单文件上传
     * 
     * @param file 上传的文件
     * @param uploadRequest 上传参数
     * @return 上传结果
     */
    @Operation(summary = "单文件上传", description = "上传单个文件到服务器")
    @PostMapping("/single")
    DynamicResponse<FileUploadResponse> uploadSingle(
            @Parameter(description = "上传的文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "上传参数") @ModelAttribute FileUploadRequest uploadRequest);

    /**
     * 批量文件上传
     * 
     * @param files 上传的文件列表
     * @param uploadRequest 上传参数
     * @return 批量上传结果
     */
    @Operation(summary = "批量文件上传", description = "批量上传多个文件")
    @PostMapping("/batch")
    DynamicResponse<BatchFileUploadResponse> uploadBatch(
            @Parameter(description = "上传的文件列表") @RequestParam("files") MultipartFile[] files,
            @Parameter(description = "上传参数") @ModelAttribute FileUploadRequest uploadRequest);

    /**
     * 初始化分片上传
     * 
     * @param request 分片上传初始化请求
     * @return 分片上传初始化结果
     */
    @Operation(summary = "初始化分片上传", description = "为大文件创建分片上传任务")
    @PostMapping("/chunk/init")
    DynamicResponse<ChunkUploadInitResponse> initChunkUpload(
            @RequestBody ChunkUploadInitRequest request);

    /**
     * 上传文件分片
     * 
     * @param chunk 文件分片
     * @param request 分片上传请求
     * @return 分片上传结果
     */
    @Operation(summary = "上传文件分片", description = "上传文件的一个分片")
    @PostMapping("/chunk/upload")
    DynamicResponse<ChunkUploadResponse> uploadChunk(
            @Parameter(description = "文件分片") @RequestParam("chunk") MultipartFile chunk,
            @Parameter(description = "分片信息") @ModelAttribute ChunkUploadRequest request);

    /**
     * 完成分片上传
     * 
     * @param request 分片上传完成请求
     * @return 上传完成结果
     */
    @Operation(summary = "完成分片上传", description = "合并所有分片完成文件上传")
    @PostMapping("/chunk/complete")
    DynamicResponse<FileUploadResponse> completeChunkUpload(
            @RequestBody ChunkUploadCompleteRequest request);

    /**
     * 取消分片上传
     * 
     * @param uploadId 上传任务ID
     * @return 取消结果
     */
    @Operation(summary = "取消分片上传", description = "取消正在进行的分片上传任务")
    @PostMapping("/chunk/cancel/{uploadId}")
    DynamicResponse<Void> cancelChunkUpload(
            @Parameter(description = "上传任务ID") @PathVariable String uploadId);

    /**
     * 查询上传进度
     * 
     * @param taskId 上传任务ID
     * @return 上传进度信息
     */
    @Operation(summary = "查询上传进度", description = "查询文件上传任务的进度")
    @GetMapping("/progress/{taskId}")
    DynamicResponse<UploadProgressResponse> getUploadProgress(
            @Parameter(description = "上传任务ID") @PathVariable String taskId);

    /**
     * 获取支持的文件类型
     * 
     * @return 支持的文件类型列表
     */
    @Operation(summary = "获取支持的文件类型", description = "获取系统支持上传的文件类型")
    @GetMapping("/supported-types")
    DynamicResponse<SupportedFileTypesResponse> getSupportedFileTypes();
}