package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 素材元数据项DTO
 * 
 * 专门为节目编辑器设计的素材元数据响应结构:
 * - 包含前端必需的所有字段
 * - 支持图片/视频/GIF等不同媒体类型
 * - 针对编辑器渲染需求优化
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "素材元数据项")
public class MaterialMetadataItem {
    
    // ========================= 核心关联信息 =========================
    
    @Schema(description = "素材ID", example = "123")
    private Long materialId;
    
    @Schema(description = "文件ID", example = "file_abc123")
    private String fileId;
    
    @Schema(description = "素材名称", example = "产品宣传视频.mp4")
    private String materialName;
    
    @Schema(description = "素材类型", example = "VIDEO")
    private String materialType;
    
    // ========================= 基础文件信息 =========================
    
    @Schema(description = "MD5哈希值", example = "a1b2c3d4e5f6...")
    private String md5Hash;
    
    @Schema(description = "文件扩展名", example = "mp4")
    private String fileExtension;
    
    @Schema(description = "MIME类型", example = "video/mp4")
    private String mimeType;
    
    @Schema(description = "文件大小(字节)", example = "1024000")
    private Long fileSize;
    
    @Schema(description = "文件状态", example = "1")
    private Integer fileStatus;
    
    @Schema(description = "文件状态描述", example = "已完成")
    private String fileStatusDesc;
    
    @Schema(description = "处理进度", example = "100")
    private Integer processProgress;
    
    // ========================= 预览和访问URL =========================
    
    @Schema(description = "预览URL", example = "/file/api/file/preview/file_abc123")
    private String previewUrl;
    
    @Schema(description = "缩略图URL", example = "https://cdn.example.com/thumbnails/file_abc123_300x300.jpg")
    private String thumbnailUrl;
    
    @Schema(description = "流式播放URL(视频专用)", example = "/file/api/file/stream/file_abc123")
    private String streamUrl;
    
    // ========================= 图片专属元数据 =========================
    
    @Schema(description = "图片元数据")
    private ImageMetadata imageMetadata;
    
    // ========================= 视频专属元数据 =========================
    
    @Schema(description = "视频元数据")
    private VideoMetadata videoMetadata;
    
    // ========================= GIF专属元数据 =========================
    
    @Schema(description = "GIF动画元数据")
    private GifMetadata gifMetadata;
    
    // ========================= 时间信息 =========================
    
    @Schema(description = "元数据分析状态", example = "SUCCESS")
    private String analysisStatus;
    
    @Schema(description = "元数据创建时间")
    private LocalDateTime metadataCreatedAt;
    
    @Schema(description = "元数据更新时间")
    private LocalDateTime metadataUpdatedAt;
    
    // ========================= 内嵌类定义 =========================
    
    /**
     * 图片元数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "图片元数据")
    public static class ImageMetadata {
        
        @Schema(description = "图片宽度(像素)", example = "1920")
        private Integer width;
        
        @Schema(description = "图片高度(像素)", example = "1080")
        private Integer height;
        
        @Schema(description = "色深(位)", example = "24")
        private Integer colorDepth;
        
        @Schema(description = "色彩空间", example = "RGB")
        private String colorSpace;
        
        @Schema(description = "是否有透明通道", example = "true")
        private Boolean hasAlpha;
        
        @Schema(description = "水平DPI", example = "72")
        private Integer dpiHorizontal;
        
        @Schema(description = "垂直DPI", example = "72")
        private Integer dpiVertical;
        
        @Schema(description = "图片方向", example = "1")
        private String orientation;
        
        @Schema(description = "拍摄设备", example = "Canon EOS R5")
        private String cameraModel;
        
        @Schema(description = "拍摄时间")
        private LocalDateTime dateTaken;
    }
    
    /**
     * 视频元数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "视频元数据")
    public static class VideoMetadata {
        
        @Schema(description = "视频宽度(像素)", example = "1920")
        private Integer width;
        
        @Schema(description = "视频高度(像素)", example = "1080")
        private Integer height;
        
        @Schema(description = "视频时长(毫秒)", example = "30000")
        private Long durationMs;
        
        @Schema(description = "帧率(fps)", example = "30.0")
        private Double frameRate;
        
        @Schema(description = "视频比特率(bps)", example = "5000000")
        private Long bitrate;
        
        @Schema(description = "视频编码", example = "H.264")
        private String videoCodec;
        
        @Schema(description = "音频编码", example = "AAC")
        private String audioCodec;
        
        @Schema(description = "宽高比", example = "16:9")
        private String aspectRatio;
        
        @Schema(description = "容器格式", example = "MP4")
        private String containerFormat;
        
        @Schema(description = "音频采样率", example = "44100")
        private Integer audioSampleRate;
        
        @Schema(description = "音频声道数", example = "2")
        private Integer audioChannels;
    }
    
    /**
     * GIF动画元数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "GIF动画元数据")
    public static class GifMetadata {
        
        @Schema(description = "图片宽度(像素)", example = "400")
        private Integer width;
        
        @Schema(description = "图片高度(像素)", example = "300")
        private Integer height;
        
        @Schema(description = "是否为动画GIF", example = "true")
        private Boolean isAnimated;
        
        @Schema(description = "帧数", example = "24")
        private Integer frameCount;
        
        @Schema(description = "动画时长(毫秒)", example = "2000")
        private Long durationMs;
        
        @Schema(description = "循环次数", example = "0")
        private Integer loopCount;
        
        @Schema(description = "平均帧延时(毫秒)", example = "83")
        private Integer averageFrameDelayMs;
    }
}