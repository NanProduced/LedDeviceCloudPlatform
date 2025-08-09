package org.nan.cloud.file.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.file.application.domain.FileInfo;
import org.nan.cloud.file.application.service.FilePreviewService;
import org.nan.cloud.common.basic.exception.BaseException;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.file.application.service.FileStorageService;
import org.nan.cloud.file.application.service.ThumbnailService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 文件预览服务实现类
 * 
 * 核心功能实现：
 * 1. 图片预览：直接输出或缩放处理
 * 2. 视频截帧：FFmpeg提取指定时间点帧
 * 3. 缓存管理：本地缓存 + HTTP缓存策略
 * 4. 错误处理：优雅降级和用户友好提示
 * 5. 性能优化：并发处理和资源管理
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FilePreviewServiceImpl implements FilePreviewService {

    private final FileStorageService fileStorageService;
    private final ThumbnailService thumbnailService;

    // 🔧 缓存配置
    private static final String PREVIEW_CACHE_PREFIX = "file_preview:";
    private static final String INFO_CACHE_PREFIX = "file_info:";
    private static final long DEFAULT_CACHE_DURATION = 3600; // 1小时
    private static final long BROWSER_CACHE_DURATION = 86400; // 浏览器缓存24小时
    private static final long CDN_CACHE_DURATION = 2592000; // CDN缓存30天

    // 📊 性能配置
    private static final int MAX_IMAGE_DIMENSION = 4096; // 最大图片尺寸
    private static final int DEFAULT_PREVIEW_SIZE = 300;   // 默认预览尺寸
    private static final double DEFAULT_VIDEO_TIME = 1.0;  // 默认视频截帧时间

    // 🌐 跨域配置
    private static final String ALLOWED_ORIGINS = "*"; // 生产环境应该配置具体域名
    private static final String ALLOWED_METHODS = "GET, HEAD, OPTIONS";
    private static final String ALLOWED_HEADERS = "Origin, Content-Type, Accept, Authorization, Cache-Control, If-Modified-Since, If-None-Match";
    private static final String EXPOSED_HEADERS = "Content-Length, Content-Range, Content-Type, ETag, Last-Modified";
    private static final int PREFLIGHT_MAX_AGE = 86400; // 预检请求缓存24小时

    @Override
    public void handlePreviewRequest(PreviewRequest request, HttpServletResponse response) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 📊 参数验证和标准化
            PreviewRequest normalizedRequest = normalizePreviewRequest(request);
            
            // 🔍 获取文件信息
            org.nan.cloud.file.application.domain.FileInfo fileInfo = getFileInfoInternal(normalizedRequest.getFileId());
            if (fileInfo == null) {
                throw new BaseException(ExceptionEnum.FILE_NOT_FOUND, 
                    "File not found: " + normalizedRequest.getFileId(), HttpStatus.NOT_FOUND);
            }
            
            // ⚡ 检查缓存控制头
            if (handleCacheControl(normalizedRequest, fileInfo, response)) {
                log.debug("缓存命中 - 文件ID: {}, 用时: {}ms", 
                         normalizedRequest.getFileId(), System.currentTimeMillis() - startTime);
                return;
            }
            
            // 🎯 根据文件类型处理预览
            String mimeType = fileInfo.getMimeType().toLowerCase();
            if (mimeType.startsWith("image/")) {
                handleImagePreview(normalizedRequest, fileInfo, response);
            } else if (mimeType.startsWith("video/")) {
                handleVideoPreview(normalizedRequest, fileInfo, response);
            } else {
                throw new BaseException(ExceptionEnum.UNSUPPORTED_FILE_FORMAT, 
                    "Unsupported file type: " + mimeType, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            }
            
            log.debug("预览处理完成 - 文件ID: {}, 用时: {}ms", 
                     normalizedRequest.getFileId(), System.currentTimeMillis() - startTime);
            
        } catch (BaseException e) {
            // 重新抛出BaseException，让GlobalExceptionHandler处理
            throw e;
        } catch (Exception e) {
            log.error("预览处理失败 - 文件ID: {}, 错误: {}", request.getFileId(), e.getMessage(), e);
            throw new BaseException(ExceptionEnum.FILE_PROCESSING_ERROR, 
                "Preview processing failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> handleStreamRequest(StreamRequest request, HttpServletResponse response) {
        // 🔍 获取文件信息
        org.nan.cloud.file.application.domain.FileInfo fileInfo = getFileInfoInternal(request.getFileId());
        if (fileInfo == null) {
            throw new BaseException(ExceptionEnum.FILE_NOT_FOUND, 
                "File not found: " + request.getFileId(), HttpStatus.NOT_FOUND);
        }
        
        // ✅ 检查是否支持流式播放
        if (!isStreamSupported(fileInfo.getMimeType())) {
            throw new BaseException(ExceptionEnum.UNSUPPORTED_FILE_FORMAT, 
                "File format not supported for streaming: " + fileInfo.getMimeType(), 
                HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }
        
        // 📊 处理Range请求
        try {
            return handleRangeRequest(request, fileInfo, response);
        } catch (Exception e) {
            log.error("流式播放处理失败 - 文件ID: {}, 错误: {}", request.getFileId(), e.getMessage(), e);
            throw new BaseException(ExceptionEnum.FILE_SERVICE_UNAVAILABLE, 
                "Streaming service failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void handleDownloadRequest(DownloadRequest request, HttpServletResponse response) {
        // 🔍 获取文件信息
        org.nan.cloud.file.application.domain.FileInfo fileInfo = getFileInfoInternal(request.getFileId());
        if (fileInfo == null) {
            throw new BaseException(ExceptionEnum.FILE_NOT_FOUND, 
                "File not found: " + request.getFileId(), HttpStatus.NOT_FOUND);
        }
        
        try {
            
            // 📊 设置下载响应头
            setDownloadHeaders(fileInfo, request.getForceAttachment(), response);
            
            // 📁 输出文件内容
            try (InputStream inputStream = fileStorageService.getFileStream(request.getFileId());
                 OutputStream outputStream = response.getOutputStream()) {
                
                inputStream.transferTo(outputStream);
                outputStream.flush();
                
                log.info("文件下载完成 - 文件ID: {}, 文件名: {}, 大小: {}", 
                        request.getFileId(), fileInfo.getOriginalFilename(), fileInfo.getFileSize());
            }
            
        } catch (Exception e) {
            log.error("文件下载失败 - 文件ID: {}, 错误: {}", request.getFileId(), e.getMessage(), e);
            throw new BaseException(ExceptionEnum.FILE_SERVICE_UNAVAILABLE, 
                "Download service failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Cacheable(value = "fileInfo", key = "#fileId")
    public Object getFileInfo(String fileId) {
        org.nan.cloud.file.application.domain.FileInfo fileInfo = getFileInfoInternal(fileId);
        if (fileInfo == null) {
            return null;
        }
        
        return FilePreviewService.FileInfo.builder()
                .fileId(fileId)
                .filename(fileInfo.getOriginalFilename())
                .fileSize(fileInfo.getFileSize())
                .mimeType(fileInfo.getMimeType())
                .extension(getFileExtension(fileInfo.getOriginalFilename()))
                .lastModified(getCurrentTimestamp())
                .etag(generateETag(fileId))
                .previewSupported(isPreviewSupported(fileId))
                .streamSupported(isStreamSupported(fileInfo.getMimeType()))
                .build();
    }

    @Override
    public boolean isPreviewSupported(String fileId) {
        try {
            org.nan.cloud.file.application.domain.FileInfo fileInfo = getFileInfoInternal(fileId);
            if (fileInfo == null) {
                return false;
            }
            
            String mimeType = fileInfo.getMimeType().toLowerCase();
            return mimeType.startsWith("image/") || mimeType.startsWith("video/");
            
        } catch (Exception e) {
            log.warn("检查预览支持失败 - 文件ID: {}, 错误: {}", fileId, e.getMessage());
            return false;
        }
    }

    @Override
    public String getFileMimeType(String fileId) {
        try {
            org.nan.cloud.file.application.domain.FileInfo fileInfo = getFileInfoInternal(fileId);
            return fileInfo != null ? fileInfo.getMimeType() : null;
        } catch (Exception e) {
            log.warn("获取文件MIME类型失败 - 文件ID: {}, 错误: {}", fileId, e.getMessage());
            return null;
        }
    }

    @Override
    public int clearPreviewCache(String fileId) {
        // TODO: 实现预览缓存清理逻辑
        log.info("清理预览缓存 - 文件ID: {}", fileId != null ? fileId : "ALL");
        return 0;
    }

    // ========================= 私有方法 =========================

    /**
     * 设置完整的缓存和跨域响应头
     */
    private void setOptimizedResponseHeaders(HttpServletResponse response, String fileId, String contentType, boolean isPreview) {
        // 🌐 跨域配置
        response.setHeader("Access-Control-Allow-Origin", ALLOWED_ORIGINS);
        response.setHeader("Access-Control-Allow-Methods", ALLOWED_METHODS);
        response.setHeader("Access-Control-Allow-Headers", ALLOWED_HEADERS);
        response.setHeader("Access-Control-Expose-Headers", EXPOSED_HEADERS);
        response.setHeader("Access-Control-Max-Age", String.valueOf(PREFLIGHT_MAX_AGE));
        
        // 📊 内容类型
        if (StringUtils.hasText(contentType)) {
            response.setContentType(contentType);
        }
        
        // 🔧 缓存策略：根据内容类型使用不同的缓存时间
        if (isPreview) {
            // 预览接口：平衡性能和更新频率
            response.setHeader("Cache-Control", 
                              String.format("public, max-age=%d, s-maxage=%d, immutable", 
                                          BROWSER_CACHE_DURATION, CDN_CACHE_DURATION));
        } else {
            // 原文件：更长的缓存时间
            response.setHeader("Cache-Control", 
                              String.format("public, max-age=%d, s-maxage=%d, immutable", 
                                          CDN_CACHE_DURATION, CDN_CACHE_DURATION));
        }
        
        // ✅ ETag和Last-Modified
        response.setHeader("ETag", generateStrongETag(fileId, isPreview));
        response.setHeader("Last-Modified", getCurrentTimestamp());
        
        // 🚀 性能优化头
        response.setHeader("Vary", "Accept, Accept-Encoding");
        
        log.debug("设置响应头完成 - 文件ID: {}, 类型: {}, 预览: {}", fileId, contentType, isPreview);
    }
    
    /**
     * 生成强ETag（包含文件ID和预览参数的哈希）
     */
    private String generateStrongETag(String fileId, boolean isPreview) {
        // 构建ETag内容：文件ID + 预览标识 + 当前时间戳（小时级别）
        long hourTimestamp = System.currentTimeMillis() / (1000 * 60 * 60); // 小时级别的时间戳
        String etagContent = fileId + (isPreview ? "_preview" : "_original") + "_" + hourTimestamp;
        int hashCode = etagContent.hashCode();
        return "\"" + Math.abs(hashCode) + "\"";
    }
    
    /**
     * 检查客户端缓存是否有效
     */
    private boolean isClientCacheValid(PreviewRequest request, String fileId, boolean isPreview) {
        String expectedETag = generateStrongETag(fileId, isPreview);
        
        // 🔍 检查If-None-Match头
        if (StringUtils.hasText(request.getIfNoneMatch())) {
            String clientETag = request.getIfNoneMatch().trim();
            if (expectedETag.equals(clientETag)) {
                log.debug("客户端ETag匹配 - 文件ID: {}, ETag: {}", fileId, clientETag);
                return true;
            }
        }
        
        // 🔍 检查If-Modified-Since头（简化实现）
        if (StringUtils.hasText(request.getIfModifiedSince())) {
            // 对于静态资源，可以认为在1小时内没有修改
            // 实际实现应该比较文件的真实修改时间
            log.debug("客户端时间戳检查通过 - 文件ID: {}", fileId);
            return true;
        }
        
        return false;
    }
    
    /**
     * 发送304 Not Modified响应
     */
    private void sendNotModifiedResponse(HttpServletResponse response, String fileId, boolean isPreview) {
        response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        
        // 304响应也需要设置缓存头
        setOptimizedResponseHeaders(response, fileId, null, isPreview);
        
        log.debug("发送304 Not Modified响应 - 文件ID: {}", fileId);
    }

    /**
     * 标准化预览请求参数
     */
    private PreviewRequest normalizePreviewRequest(PreviewRequest request) {
        return PreviewRequest.builder()
                .fileId(request.getFileId())
                .width(normalizeSize(request.getWidth()))
                .height(normalizeSize(request.getHeight()))
                .fit(normalizeString(request.getFit(), "cover"))
                .format(normalizeString(request.getFormat(), "jpg"))
                .quality(normalizeQuality(request.getQuality()))
                .timeOffset(normalizeTimeOffset(request.getTimeOffset()))
                .frameNumber(request.getFrameNumber())
                .userAgent(request.getUserAgent())
                .ifModifiedSince(request.getIfModifiedSince())
                .ifNoneMatch(request.getIfNoneMatch())
                .build();
    }

    /**
     * 获取文件信息（内部方法）
     */
    private org.nan.cloud.file.application.domain.FileInfo getFileInfoInternal(String fileId) {
        try {
            // 🔍 调用文件存储服务获取文件信息
            FileStorageService.FileStorageInfo storageInfo = fileStorageService.getFileStorageInfo(fileId);
            if (storageInfo == null) {
                log.warn("文件不存在或无法访问 - 文件ID: {}", fileId);
                return null;
            }
            
            // 转换为内部FileInfo对象
            return org.nan.cloud.file.application.domain.FileInfo.builder()
                .fileId(fileId)
                .originalFilename(storageInfo.getOriginalFilename())
                .fileSize(storageInfo.getFileSize())
                .mimeType(storageInfo.getMimeType())
                .build();
            
        } catch (Exception e) {
            log.error("获取文件信息失败 - 文件ID: {}, 错误: {}", fileId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 处理缓存控制
     */
    private boolean handleCacheControl(PreviewRequest request, org.nan.cloud.file.application.domain.FileInfo fileInfo, HttpServletResponse response) {
        // 🔍 检查客户端缓存有效性
        if (isClientCacheValid(request, request.getFileId(), true)) {
            sendNotModifiedResponse(response, request.getFileId(), true);
            return true;
        }
        
        return false;
    }

    /**
     * 处理图片预览
     */
    private void handleImagePreview(PreviewRequest request, org.nan.cloud.file.application.domain.FileInfo fileInfo, HttpServletResponse response) throws IOException {
        log.debug("处理图片预览 - 文件ID: {}, 尺寸: {}x{}", 
                 request.getFileId(), request.getWidth(), request.getHeight());
        
        // 📊 设置响应头
        response.setContentType(getOutputMimeType(request.getFormat()));
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");
        
        try (InputStream inputStream = getImagePreviewStream(request, fileInfo);
             OutputStream outputStream = response.getOutputStream()) {
            
            if (inputStream != null) {
                inputStream.transferTo(outputStream);
            } else {
                // 图片处理失败，抛出异常
                log.warn("图片预览流获取失败 - 文件ID: {}", request.getFileId());
                throw new BaseException(ExceptionEnum.FILE_PROCESSING_ERROR, 
                    "Image processing failed for file: " + request.getFileId(), 
                    HttpStatus.UNPROCESSABLE_ENTITY);
            }
            
            outputStream.flush();
        }
    }

    /**
     * 处理视频预览（截帧）
     */
    private void handleVideoPreview(PreviewRequest request, org.nan.cloud.file.application.domain.FileInfo fileInfo, HttpServletResponse response) throws IOException {
        log.debug("处理视频截帧 - 文件ID: {}, 时间: {}s", 
                 request.getFileId(), request.getTimeOffset());
        
        // 📊 设置响应头
        response.setContentType(getOutputMimeType(request.getFormat()));
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");
        
        try (InputStream inputStream = getVideoFrameStream(request, fileInfo);
             OutputStream outputStream = response.getOutputStream()) {
            
            if (inputStream != null) {
                inputStream.transferTo(outputStream);
            } else {
                // 视频截帧失败，抛出异常
                log.warn("视频截帧流获取失败 - 文件ID: {}", request.getFileId());
                throw new BaseException(ExceptionEnum.VIDEO_FRAME_EXTRACTION_FAILED, 
                    "Video frame extraction failed for file: " + request.getFileId(), 
                    HttpStatus.UNPROCESSABLE_ENTITY);
            }
            
            outputStream.flush();
        }
    }

    /**
     * 处理Range请求（流式播放）
     */
    private ResponseEntity<?> handleRangeRequest(StreamRequest request, org.nan.cloud.file.application.domain.FileInfo fileInfo, HttpServletResponse response) {
        try {
            // 📊 解析Range头
            String rangeHeader = request.getRangeHeader();
            if (!StringUtils.hasText(rangeHeader) || !rangeHeader.startsWith("bytes=")) {
                // 无Range请求，返回完整文件
                return handleFullFileResponse(request, fileInfo);
            }
            
            // 🔧 解析Range头
            RangeInfo rangeInfo = parseRangeHeader(rangeHeader, fileInfo.getFileSize());
            if (rangeInfo == null) {
                log.warn("无效的Range头格式 - 文件ID: {}, Range: {}", request.getFileId(), rangeHeader);
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .header("Content-Range", "bytes */" + fileInfo.getFileSize())
                        .build();
            }
            
            // ✅ 验证范围有效性
            if (!rangeInfo.isValid()) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .header("Content-Range", "bytes */" + fileInfo.getFileSize())
                        .build();
            }
            
            // 📊 设置206 Partial Content响应
            long start = rangeInfo.getStart();
            long end = rangeInfo.getEnd();
            long contentLength = rangeInfo.getContentLength();
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", fileInfo.getMimeType());
            headers.set("Content-Range", String.format("bytes %d-%d/%d", start, end, fileInfo.getFileSize()));
            headers.set("Content-Length", String.valueOf(contentLength));
            headers.set("Accept-Ranges", "bytes");
            headers.set("Cache-Control", "public, max-age=" + DEFAULT_CACHE_DURATION);
            
            log.debug("Range请求处理 - 文件ID: {}, 范围: {}-{}/{}", 
                     request.getFileId(), start, end, fileInfo.getFileSize());
            
            // 🔧 读取指定范围的文件内容
            byte[] rangeContent = readFileRange(request.getFileId(), start, contentLength);
            if (rangeContent == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .headers(headers)
                    .body(rangeContent);
            
        } catch (Exception e) {
            log.error("Range请求处理失败 - 文件ID: {}, 错误: {}", request.getFileId(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Range请求处理失败");
        }
    }

    /**
     * 处理完整文件响应
     */
    private ResponseEntity<?> handleFullFileResponse(StreamRequest request, org.nan.cloud.file.application.domain.FileInfo fileInfo) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", fileInfo.getMimeType());
            headers.set("Content-Length", String.valueOf(fileInfo.getFileSize()));
            headers.set("Accept-Ranges", "bytes");
            headers.set("Cache-Control", "public, max-age=" + DEFAULT_CACHE_DURATION);
            headers.set("ETag", generateETag(request.getFileId()));
            headers.set("Last-Modified", getCurrentTimestamp());
            
            // 🔧 获取完整文件流
            try (InputStream fileStream = fileStorageService.getFileStream(request.getFileId())) {
                if (fileStream == null) {
                    return ResponseEntity.notFound().build();
                }
                
                // 📊 读取文件内容到字节数组
                byte[] fileContent = fileStream.readAllBytes();
                
                log.debug("完整文件响应 - 文件ID: {}, 大小: {} bytes", request.getFileId(), fileContent.length);
                
                return ResponseEntity.ok()
                        .headers(headers)
                        .body(fileContent);
                
            } catch (IOException e) {
                log.error("读取完整文件失败 - 文件ID: {}, 错误: {}", request.getFileId(), e.getMessage(), e);
                return ResponseEntity.internalServerError().body("文件读取失败");
            }
        } catch (Exception e) {
            log.error("处理完整文件响应失败 - 文件ID: {}, 错误: {}", request.getFileId(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body("服务暂时不可用");
        }
    }

    /**
     * 获取图片预览流
     */
    private InputStream getImagePreviewStream(PreviewRequest request, org.nan.cloud.file.application.domain.FileInfo fileInfo) {
        try {
            log.debug("生成图片预览 - 文件ID: {}, 目标尺寸: {}x{}", 
                     request.getFileId(), request.getWidth(), request.getHeight());
            
            // 🔧 判断是否需要缩放处理
            boolean needResize = request.getWidth() != null || request.getHeight() != null;
            boolean needFormatConvert = !"jpg".equalsIgnoreCase(request.getFormat()) && 
                                       !getOriginalFormat(fileInfo.getMimeType()).equalsIgnoreCase(request.getFormat());
            
            if (needResize || needFormatConvert) {
                // 需要缩放或格式转换，调用thumbnailService
                return generateThumbnailStream(request, fileInfo);
            } else {
                // 直接返回原始文件流
                return fileStorageService.getFileStream(request.getFileId());
            }
            
        } catch (Exception e) {
            log.error("获取图片预览流失败 - 文件ID: {}, 错误: {}", request.getFileId(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取视频帧流
     */
    private InputStream getVideoFrameStream(PreviewRequest request, org.nan.cloud.file.application.domain.FileInfo fileInfo) {
        try {
            log.debug("生成视频截帧 - 文件ID: {}, 时间: {}s", 
                     request.getFileId(), request.getTimeOffset());
            
            // 🎯 调用thumbnailService进行视频截帧
            // 构建缩略图请求参数
            ThumbnailService.ThumbnailRequest thumbnailRequest = ThumbnailService.ThumbnailRequest.builder()
                .sourceFileId(request.getFileId())
                .targetWidth(request.getWidth())
                .targetHeight(request.getHeight())
                .outputFormat(request.getFormat())
                .quality(request.getQuality())
                .timeOffset(request.getTimeOffset())
                .build();
            
            // 生成视频截帧缩略图
            return thumbnailService.generateVideoFrameThumbnail(thumbnailRequest);
            
        } catch (Exception e) {
            log.error("获取视频帧流失败 - 文件ID: {}, 错误: {}", request.getFileId(), e.getMessage(), e);
            return null;
        }
    }

    // ========================= 工具方法 =========================

    private Integer normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return null;
        }
        return Math.min(size, MAX_IMAGE_DIMENSION);
    }

    private String normalizeString(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.toLowerCase() : defaultValue;
    }

    private Integer normalizeQuality(Integer quality) {
        if (quality == null) {
            return 85;
        }
        return Math.max(1, Math.min(100, quality));
    }

    private Double normalizeTimeOffset(Double timeOffset) {
        if (timeOffset == null || timeOffset < 0) {
            return DEFAULT_VIDEO_TIME;
        }
        return Math.min(timeOffset, 3600.0); // 最大1小时
    }

    private boolean isStreamSupported(String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            return false;
        }
        
        String type = mimeType.toLowerCase();
        return type.equals("video/mp4") || 
               type.equals("video/webm") || 
               type.equals("video/quicktime");
    }

    private String getOutputMimeType(String format) {
        switch (format.toLowerCase()) {
            case "png": return "image/png";
            case "webp": return "image/webp";
            case "gif": return "image/gif";
            default: return "image/jpeg";
        }
    }

    private String getFileExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1).toLowerCase() : "";
    }

    private String generateETag(String fileId) {
        return "\"" + fileId.hashCode() + "\"";
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }

    private void setDownloadHeaders(org.nan.cloud.file.application.domain.FileInfo fileInfo, Boolean forceAttachment, HttpServletResponse response) {
        response.setContentType(fileInfo.getMimeType());
        response.setContentLengthLong(fileInfo.getFileSize());
        
        String disposition = forceAttachment ? "attachment" : "inline";
        response.setHeader("Content-Disposition", 
                          String.format("%s; filename=\"%s\"", disposition, fileInfo.getOriginalFilename()));
        
        response.setHeader("Cache-Control", "public, max-age=" + DEFAULT_CACHE_DURATION);
        response.setHeader("ETag", generateETag(fileInfo.getFileId()));
    }

    // 🗑️ 已移除：sendJsonErrorResponse 方法（使用GlobalExceptionHandler统一处理异常）

    // 🗑️ 已移除：writeDefaultPlaceholder 方法（由前端处理占位图）

    // 🗑️ 已移除：writeVideoPlaceholder 方法（由前端处理占位图）
    
    // 🗑️ 已移除：createTransparentPng 方法（由前端处理占位图）
    
    // 🗑️ 已移除：createVideoPlaceholderPng 方法（由前端处理占位图）

    /**
     * 解析Range头，返回范围信息
     */
    private RangeInfo parseRangeHeader(String rangeHeader, long fileSize) {
        if (!StringUtils.hasText(rangeHeader) || !rangeHeader.startsWith("bytes=")) {
            return null;
        }
        
        String rangeValue = rangeHeader.substring(6);
        try {
            long start = 0;
            long end = fileSize - 1;
            
            if (rangeValue.startsWith("-")) {
                // 后缀范围：bytes=-500
                long suffixLength = Long.parseLong(rangeValue.substring(1));
                start = Math.max(0, fileSize - suffixLength);
                end = fileSize - 1;
            } else if (rangeValue.endsWith("-")) {
                // 前缀范围：bytes=500-
                start = Long.parseLong(rangeValue.substring(0, rangeValue.length() - 1));
                end = fileSize - 1;
            } else if (rangeValue.contains("-")) {
                // 完整范围：bytes=0-499
                String[] parts = rangeValue.split("-", 2);
                if (StringUtils.hasText(parts[0])) {
                    start = Long.parseLong(parts[0]);
                }
                if (StringUtils.hasText(parts[1])) {
                    end = Long.parseLong(parts[1]);
                }
            } else {
                // 单一位置：bytes=500
                start = Long.parseLong(rangeValue);
                end = fileSize - 1;
            }
            
            // 确保end不超过文件大小
            end = Math.min(end, fileSize - 1);
            
            return new RangeInfo(start, end, fileSize);
            
        } catch (NumberFormatException e) {
            log.warn("Range头格式无效: {}", rangeHeader);
            return null;
        }
    }
    
    /**
     * Range信息封装类
     */
    private static class RangeInfo {
        private final long start;
        private final long end;
        private final long fileSize;
        
        public RangeInfo(long start, long end, long fileSize) {
            this.start = start;
            this.end = end;
            this.fileSize = fileSize;
        }
        
        public long getStart() { return start; }
        public long getEnd() { return end; }
        public long getFileSize() { return fileSize; }
        public long getContentLength() { return end - start + 1; }
        
        public boolean isValid() {
            return start >= 0 && end >= start && start < fileSize;
        }
    }

    /**
     * 读取文件指定范围的内容
     */
    private byte[] readFileRange(String fileId, long startPos, long length) {
        try (InputStream fileStream = fileStorageService.getFileStream(fileId)) {
            if (fileStream == null) {
                log.warn("文件不存在或无法访问 - 文件ID: {}", fileId);
                return null;
            }
            
            // 🚀 跳过开始位置之前的字节
            long skipped = fileStream.skip(startPos);
            if (skipped != startPos) {
                log.warn("文件跳过字节数不匹配 - 期望: {}, 实际: {}", startPos, skipped);
            }
            
            // 📊 读取指定长度的内容
            byte[] buffer = new byte[(int) length];
            int totalRead = 0;
            int currentRead;
            
            while (totalRead < length && (currentRead = fileStream.read(buffer, totalRead, 
                    (int) (length - totalRead))) != -1) {
                totalRead += currentRead;
            }
            
            if (totalRead < length) {
                // 实际读取的字节数少于请求的长度，调整数组大小
                byte[] actualContent = new byte[totalRead];
                System.arraycopy(buffer, 0, actualContent, 0, totalRead);
                return actualContent;
            }
            
            log.debug("文件范围读取完成 - 文件ID: {}, 开始: {}, 长度: {}, 实际读取: {}", 
                     fileId, startPos, length, totalRead);
            
            return buffer;
            
        } catch (IOException e) {
            log.error("读取文件范围失败 - 文件ID: {}, 开始: {}, 长度: {}, 错误: {}", 
                     fileId, startPos, length, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取原始文件格式
     */
    private String getOriginalFormat(String mimeType) {
        if (mimeType == null) return "jpg";
        
        if (mimeType.contains("png")) return "png";
        if (mimeType.contains("gif")) return "gif";
        if (mimeType.contains("webp")) return "webp";
        if (mimeType.contains("bmp")) return "bmp";
        
        return "jpg"; // 默认
    }
    
    /**
     * 生成缩略图流
     */
    private InputStream generateThumbnailStream(PreviewRequest request, org.nan.cloud.file.application.domain.FileInfo fileInfo) {
        try {
            // 构建缩略图请求
            ThumbnailService.ThumbnailRequest thumbnailRequest = ThumbnailService.ThumbnailRequest.builder()
                .sourceFileId(request.getFileId())
                .targetWidth(request.getWidth())
                .targetHeight(request.getHeight())
                .fit(request.getFit())
                .outputFormat(request.getFormat())
                .quality(request.getQuality())
                .build();
            
            // 调用thumbnailService生成缩略图
            return thumbnailService.generateThumbnail(thumbnailRequest);
            
        } catch (Exception e) {
            log.error("生成缩略图流失败 - 文件ID: {}, 错误: {}", request.getFileId(), e.getMessage(), e);
            return null;
        }
    }
}