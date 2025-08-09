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
 * - 支持单文件异步上传
 * - 文件类型验证和安全检查
 * - 上传进度实时推送
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface FileUploadApi {

    String prefix = "/file/upload";

    /**
     * 异步上传单个文件(素材)
     * 
     * @param file 上传的文件
     * @param uploadRequest 上传参数
     * @return 任务初始化响应
     */
    @PostMapping(prefix + "/single")
    TaskInitResponse uploadSingleAsync(
            @Parameter(description = "上传的文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "上传参数") @ModelAttribute FileUploadRequest uploadRequest);

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