package org.nan.cloud.file.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.file.api.FilePreviewApi;
import org.nan.cloud.file.application.domain.FileInfo;
import org.nan.cloud.file.application.service.FilePreviewService;
import org.nan.cloud.common.web.IgnoreDynamicResponse;
import org.nan.cloud.common.basic.exception.BaseException;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 文件预览控制器
 * 
 * 统一的文件预览接口，专为节目编辑器设计:
 * - 图片：直接输出缩略图或原图
 * - 视频：输出指定时间点的截帧图片
 * - 支持缩放、格式转换、质量调整
 * - 304缓存支持，跨域配置
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "文件预览", description = "统一文件预览接口")
public class FilePreviewController implements FilePreviewApi {

    private final FilePreviewService filePreviewService;

    /**
     * 统一预览接口 - 图片直接输出，视频截帧输出
     * 
     * GET /file/api/file/preview/{fileId}?w=300&h=300&t=5.0
     * 
     * @param fileId 文件ID
     * @param w 输出宽度（可选）
     * @param h 输出高度（可选）
     * @param fit 适应方式：cover, contain, fill, inside, outside（可选）
     * @param format 输出格式：jpg, png, webp, gif（可选）
     * @param q 图片质量：1-100（可选）
     * @param t 视频时间点（秒，视频专用）
     * @param frame 视频帧数（可选，替代t参数）
     * @param request HTTP请求
     * @param response HTTP响应
     */
    @Operation(
        summary = "统一素材预览接口",
        description = "支持图片直接预览和视频截帧预览，专为节目编辑器设计的高性能接口",
            tags = {"素材管理", "素材预览"}
    )
    @Override
    @IgnoreDynamicResponse // 直接操作response，跳过统一包装
    public void previewFile(
            @Parameter(description = "文件ID", required = true) 
            @PathVariable String fileId,
            
            @Parameter(description = "输出宽度（像素）", example = "300") 
            @RequestParam(required = false) Integer w,
            
            @Parameter(description = "输出高度（像素）", example = "300") 
            @RequestParam(required = false) Integer h,
            
            @Parameter(description = "适应方式", example = "cover") 
            @RequestParam(required = false, defaultValue = "cover") String fit,
            
            @Parameter(description = "输出格式", example = "jpg") 
            @RequestParam(required = false, defaultValue = "jpg") String format,
            
            @Parameter(description = "图片质量 1-100", example = "85") 
            @RequestParam(required = false, defaultValue = "85") Integer q,
            
            @Parameter(description = "视频时间点（秒）", example = "1.0") 
            @RequestParam(required = false, defaultValue = "1.0") Double t,
            
            @Parameter(description = "视频帧数（替代t参数）") 
            @RequestParam(required = false) Integer frame,
            
            HttpServletRequest request,
            HttpServletResponse response) {
        
        // 📊 构建预览参数
        FilePreviewService.PreviewRequest previewRequest = FilePreviewService.PreviewRequest.builder()
            .fileId(fileId)
            .width(w)
            .height(h)
            .fit(fit)
            .format(format)
            .quality(q)
            .timeOffset(t)
            .frameNumber(frame)
            .userAgent(request.getHeader("User-Agent"))
            .ifModifiedSince(request.getHeader("If-Modified-Since"))
            .ifNoneMatch(request.getHeader("If-None-Match"))
            .build();
        
        log.debug("处理文件预览请求 - 文件ID: {}, 参数: {}x{}, 格式: {}, 时间: {}s", 
                 fileId, w, h, format, t);
        
        // 🔧 调用预览服务处理 - 异常由GlobalExceptionHandler统一处理
        filePreviewService.handlePreviewRequest(previewRequest, response);
    }

    /**
     * 流式播放接口 - 支持Range请求的视频播放
     * 
     * GET /file/api/file/stream/{fileId}
     * 
     * @param fileId 文件ID
     * @param request HTTP请求
     * @param response HTTP响应
     */
    @Operation(
        summary = "素材流式文件播放",
        description = "支持Range请求的视频流式播放接口，用于视频预览播放",
        tags = {"素材管理", "素材预览"}
    )
    @Override
    @IgnoreDynamicResponse // 直接操作response，跳过统一包装
    public ResponseEntity<?> streamFile(
            @Parameter(description = "文件ID", required = true) 
            @PathVariable String fileId,
            
            HttpServletRequest request,
            HttpServletResponse response) {
        
        log.debug("处理文件流式播放请求 - 文件ID: {}, Range: {}", 
                 fileId, request.getHeader("Range"));
        
        // 📊 构建流式播放参数
        FilePreviewService.StreamRequest streamRequest = FilePreviewService.StreamRequest.builder()
            .fileId(fileId)
            .rangeHeader(request.getHeader("Range"))
            .userAgent(request.getHeader("User-Agent"))
            .ifModifiedSince(request.getHeader("If-Modified-Since"))
            .build();
        
        // 🔧 调用流式播放服务处理 - 异常由GlobalExceptionHandler统一处理
        return filePreviewService.handleStreamRequest(streamRequest, response);
    }

    /**
     * 原始文件下载接口
     * 
     * GET /file/api/file/download/{fileId}
     * 
     * @param fileId 文件ID
     * @param request HTTP请求
     * @param response HTTP响应
     */
    @Operation(
        summary = "原始文件下载",
        description = "下载文件的原始版本，保持原有格式和质量",
        tags = {"素材管理"}
    )
    @IgnoreDynamicResponse // 直接操作response，跳过统一包装
    public void downloadFile(
            @Parameter(description = "文件ID", required = true) 
            @PathVariable String fileId,
            
            @Parameter(description = "是否强制下载（attachment）") 
            @RequestParam(required = false, defaultValue = "true") Boolean attachment,
            
            HttpServletRequest request,
            HttpServletResponse response) {
        
        log.info("处理文件下载请求 - 文件ID: {}, 强制下载: {}", fileId, attachment);
        
        // 📊 构建下载参数
        FilePreviewService.DownloadRequest downloadRequest = FilePreviewService.DownloadRequest.builder()
            .fileId(fileId)
            .forceAttachment(attachment)
            .userAgent(request.getHeader("User-Agent"))
            .build();
        
        // 🔧 调用下载服务处理 - 异常由GlobalExceptionHandler统一处理
        filePreviewService.handleDownloadRequest(downloadRequest, response);
    }

    /**
     * 获取文件基础信息接口（不下载文件内容）
     * 
     * GET /file/api/file/info/{fileId}
     * 
     * @param fileId 文件ID
     * @return 文件基础信息
     */
    @Operation(
        summary = "获取文件信息",
        description = "获取文件的基础信息，不返回文件内容",
        tags = {"素材管理"}
    )
    @GetMapping("/file/preview/info/{fileId}")
    public FileInfo getFileInfo(
            @Parameter(description = "文件ID", required = true) 
            @PathVariable String fileId) {
        
        log.debug("获取文件信息 - 文件ID: {}", fileId);
        
        // 🔧 调用文件信息服务
        FileInfo fileInfo = filePreviewService.getFileInfo(fileId);
        
        // 如果文件不存在，抛出标准异常，由GlobalExceptionHandler处理
        if (fileInfo == null) {
            throw new BaseException(ExceptionEnum.FILE_NOT_FOUND, 
                "文件不存在或已被删除: " + fileId, org.springframework.http.HttpStatus.NOT_FOUND);
        }
        
        // 直接返回业务数据，BaseResponseAdvice会自动包装为DynamicResponse.success(fileInfo)
        return fileInfo;
    }

    // ========================= 私有方法 =========================
    
    // 原有的错误处理方法已移除，现在使用标准的BaseException机制
    // 所有异常将由GlobalExceptionHandler统一处理，符合common-web规范
}