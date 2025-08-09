package org.nan.cloud.file.application.service;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 文件预览服务接口
 * 
 * 统一的文件预览服务，专为节目编辑器设计：
 * - 图片：直接输出缩略图或原图
 * - 视频：输出指定时间点的截帧图片
 * - GIF：支持动画帧提取和静态预览
 * - 支持缩放、格式转换、质量调整
 * - 304缓存支持，跨域配置
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface FilePreviewService {

    /**
     * 处理统一预览请求
     * 
     * @param request 预览请求参数
     * @param response HTTP响应对象
     */
    void handlePreviewRequest(PreviewRequest request, HttpServletResponse response);

    /**
     * 处理流式播放请求
     * 
     * @param request 流式请求参数
     * @param response HTTP响应对象
     * @return 响应实体
     */
    ResponseEntity<?> handleStreamRequest(StreamRequest request, HttpServletResponse response);

    /**
     * 处理文件下载请求
     * 
     * @param request 下载请求参数
     * @param response HTTP响应对象
     */
    void handleDownloadRequest(DownloadRequest request, HttpServletResponse response);

    /**
     * 获取文件基础信息
     * 
     * @param fileId 文件ID
     * @return 文件信息对象
     */
    Object getFileInfo(String fileId);

    /**
     * 检查文件是否支持预览
     * 
     * @param fileId 文件ID
     * @return 是否支持预览
     */
    boolean isPreviewSupported(String fileId);

    /**
     * 获取文件MIME类型
     * 
     * @param fileId 文件ID
     * @return MIME类型
     */
    String getFileMimeType(String fileId);

    /**
     * 清理预览缓存
     * 
     * @param fileId 文件ID（可选，为空则清理所有缓存）
     * @return 清理的缓存数量
     */
    int clearPreviewCache(String fileId);

    /**
     * 预览请求参数
     */
    @Data
    @Builder
    class PreviewRequest {
        /** 文件ID */
        private String fileId;
        
        /** 输出宽度（像素） */
        private Integer width;
        
        /** 输出高度（像素） */
        private Integer height;
        
        /** 适应方式：cover, contain, fill, inside, outside */
        @Builder.Default
        private String fit = "cover";
        
        /** 输出格式：jpg, png, webp, gif */
        @Builder.Default
        private String format = "jpg";
        
        /** 图片质量：1-100 */
        @Builder.Default
        private Integer quality = 85;
        
        /** 视频时间点（秒） */
        @Builder.Default
        private Double timeOffset = 1.0;
        
        /** 视频帧数（替代timeOffset） */
        private Integer frameNumber;
        
        /** 用户代理 */
        private String userAgent;
        
        /** If-Modified-Since头 */
        private String ifModifiedSince;
        
        /** If-None-Match头 */
        private String ifNoneMatch;
    }

    /**
     * 流式播放请求参数
     */
    @Data
    @Builder
    class StreamRequest {
        /** 文件ID */
        private String fileId;
        
        /** Range请求头 */
        private String rangeHeader;
        
        /** 用户代理 */
        private String userAgent;
        
        /** If-Modified-Since头 */
        private String ifModifiedSince;
    }

    /**
     * 下载请求参数
     */
    @Data
    @Builder
    class DownloadRequest {
        /** 文件ID */
        private String fileId;
        
        /** 是否强制下载（attachment模式） */
        @Builder.Default
        private Boolean forceAttachment = true;
        
        /** 用户代理 */
        private String userAgent;
    }

    /**
     * 文件基础信息
     */
    @Data
    @Builder
    class FileInfo {
        /** 文件ID */
        private String fileId;
        
        /** 文件名 */
        private String filename;
        
        /** 文件大小（字节） */
        private Long fileSize;
        
        /** MIME类型 */
        private String mimeType;
        
        /** 文件扩展名 */
        private String extension;
        
        /** 最后修改时间 */
        private String lastModified;
        
        /** ETag */
        private String etag;
        
        /** 是否支持预览 */
        private Boolean previewSupported;
        
        /** 是否支持流式播放 */
        private Boolean streamSupported;
    }

    /**
     * 预览处理结果
     */
    @Data
    @Builder
    class PreviewResult {
        /** 是否成功 */
        private boolean success;
        
        /** 错误消息 */
        private String errorMessage;
        
        /** 处理耗时（毫秒） */
        private long processingTime;
        
        /** 缓存命中 */
        private boolean cacheHit;
        
        /** 输出字节数 */
        private long outputBytes;
        
        /** 实际输出尺寸 */
        private String outputDimensions;
    }

    /**
     * 支持的图片格式
     */
    enum SupportedImageFormat {
        JPEG("jpg", "image/jpeg", true),
        PNG("png", "image/png", true),
        WEBP("webp", "image/webp", true),
        GIF("gif", "image/gif", true),
        BMP("bmp", "image/bmp", false),
        TIFF("tiff", "image/tiff", false);

        private final String extension;
        private final String mimeType;
        private final boolean outputSupported;

        SupportedImageFormat(String extension, String mimeType, boolean outputSupported) {
            this.extension = extension;
            this.mimeType = mimeType;
            this.outputSupported = outputSupported;
        }

        public String getExtension() { return extension; }
        public String getMimeType() { return mimeType; }
        public boolean isOutputSupported() { return outputSupported; }
    }

    /**
     * 支持的视频格式
     */
    enum SupportedVideoFormat {
        MP4("mp4", "video/mp4", true),
        AVI("avi", "video/x-msvideo", true),
        MOV("mov", "video/quicktime", true),
        WMV("wmv", "video/x-ms-wmv", false),
        FLV("flv", "video/x-flv", false),
        WEBM("webm", "video/webm", true);

        private final String extension;
        private final String mimeType;
        private final boolean streamSupported;

        SupportedVideoFormat(String extension, String mimeType, boolean streamSupported) {
            this.extension = extension;
            this.mimeType = mimeType;
            this.streamSupported = streamSupported;
        }

        public String getExtension() { return extension; }
        public String getMimeType() { return mimeType; }
        public boolean isStreamSupported() { return streamSupported; }
    }

    /**
     * 适应方式枚举
     */
    enum ResizeFit {
        /** 覆盖整个区域，可能裁剪 */
        COVER("cover"),
        /** 完全包含，可能留白 */
        CONTAIN("contain"),
        /** 填充整个区域，可能变形 */
        FILL("fill"),
        /** 缩小到内部，不放大 */
        INSIDE("inside"),
        /** 放大到外部，不缩小 */
        OUTSIDE("outside");

        private final String value;

        ResizeFit(String value) {
            this.value = value;
        }

        public String getValue() { return value; }

        public static ResizeFit fromString(String value) {
            for (ResizeFit fit : values()) {
                if (fit.value.equalsIgnoreCase(value)) {
                    return fit;
                }
            }
            return COVER; // 默认值
        }
    }
}