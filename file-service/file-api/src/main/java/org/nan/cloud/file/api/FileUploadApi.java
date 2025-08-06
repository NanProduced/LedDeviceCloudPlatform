package org.nan.cloud.file.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
public interface FileUploadApi {

    String prefix = "/file/upload";

    @PostMapping(prefix + "/single")
    TaskInitResponse uploadSingleAsync(
            @Parameter(description = "上传的文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "上传参数") @ModelAttribute FileUploadRequest uploadRequest);

    /**
     * 初始化分片上传
     * 
     * @param request 分片上传初始化请求
     * @return 分片上传初始化结果
     */
    @PostMapping(prefix + "/chunk/init")
    ChunkUploadInitResponse initChunkUpload(
            @RequestBody ChunkUploadInitRequest request);

    /**
     * 上传文件分片
     * 
     * @param chunk 文件分片
     * @param request 分片上传请求
     * @return 分片上传结果
     */
    @PostMapping(prefix + "/chunk/upload")
    ChunkUploadResponse uploadChunk(
            @Parameter(description = "文件分片") @RequestParam("chunk") MultipartFile chunk,
            @Parameter(description = "分片信息") @ModelAttribute ChunkUploadRequest request);

    /**
     * 完成分片上传
     * 
     * @param request 分片上传完成请求
     * @return 上传完成结果
     */
    @PostMapping(prefix + "/chunk/complete")
    FileUploadResponse completeChunkUpload(
            @RequestBody ChunkUploadCompleteRequest request);

    /**
     * 取消分片上传
     * 
     * @param uploadId 上传任务ID
     * @return 取消结果
     */
    @PostMapping(prefix + "/chunk/cancel/{uploadId}")
    void cancelChunkUpload(
            @Parameter(description = "上传任务ID") @PathVariable String uploadId);

    /**
     * 查询上传进度
     * 
     * @param taskId 上传任务ID
     * @return 上传进度信息
     */
    @GetMapping(prefix + "/progress/{taskId}")
    UploadProgressResponse getUploadProgress(
            @Parameter(description = "上传任务ID") @PathVariable String taskId);

    /**
     * 获取支持的文件类型
     * 
     * @return 支持的文件类型列表
     */
    @GetMapping(prefix + "/supported-types")
    SupportedFileTypesResponse getSupportedFileTypes();
}