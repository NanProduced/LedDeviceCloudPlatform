package org.nan.cloud.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 素材元数据 - MongoDB文档结构
 * 存储素材的详细元数据信息，用于素材详情展示
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MaterialMetadata {

    /**
     * MongoDB文档ID
     */
    private String id;

    /**
     * 对应的文件ID（关联material_file表）
     */
    private String fileId;

    /**
     * 基础文件信息
     */
    private FileBasicInfo basicInfo;

    /**
     * 图片元数据（仅图片类型素材）
     */
    private ImageMetadata imageMetadata;

    /**
     * 视频元数据（仅视频类型素材）
     */
    private VideoMetadata videoMetadata;

    /**
     * 音频元数据（仅音频类型素材）
     */
    private AudioMetadata audioMetadata;

    /**
     * 文档元数据（仅文档类型素材）
     */
    private DocumentMetadata documentMetadata;

    /**
     * LED业务相关元数据
     */
    private LedBusinessMetadata ledBusinessMetadata;

    /**
     * AI分析结果（可选）
     */
    private AiAnalysisResult aiAnalysisResult;

    /**
     * 处理历史记录
     */
    private List<ProcessingHistory> processingHistory;

    /**
     * 元数据创建时间
     */
    private LocalDateTime createTime;

    /**
     * 元数据更新时间
     */
    private LocalDateTime updateTime;

    // ========================= 内部类定义 =========================

    /**
     * 基础文件信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileBasicInfo {
        private String fileName;
        private String mimeType;
        private String fileExtension;
        private Long fileSize;
        private String md5Hash;
        private String sha256Hash;
        private LocalDateTime fileCreateTime;
        private LocalDateTime fileModifyTime;
        private String encoding; // 文件编码
        private Map<String, Object> customProperties; // 自定义属性
    }

    /**
     * 图片元数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageMetadata {
        private Integer width;
        private Integer height;
        private Integer bitDepth; // 位深度
        private String colorSpace; // 色彩空间：RGB, CMYK, Gray等
        private Boolean hasAlpha; // 是否有透明通道
        private String format; // PNG, JPEG, GIF, WEBP等
        private Double dpi; // 分辨率DPI
        
        // EXIF信息
        private ExifInfo exifInfo;
        
        // 缩略图信息
        private List<ThumbnailInfo> thumbnails;
        
        // 图片质量分析
        private ImageQualityInfo qualityInfo;
    }

    /**
     * EXIF信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExifInfo {
        private String cameraMake; // 相机制造商
        private String cameraModel; // 相机型号
        private LocalDateTime shootingTime; // 拍摄时间
        private String lensModel; // 镜头型号
        private Double focalLength; // 焦距
        private String aperture; // 光圈
        private String shutterSpeed; // 快门速度
        private Integer iso; // ISO感光度
        private GpsInfo gpsInfo; // GPS信息
        private String orientation; // 方向
        private Map<String, Object> additionalExif; // 其他EXIF数据
    }

    /**
     * GPS信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GpsInfo {
        private Double latitude; // 纬度
        private Double longitude; // 经度
        private Double altitude; // 海拔
        private String location; // 地理位置描述
    }

    /**
     * 缩略图信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThumbnailInfo {
        private String size; // 尺寸规格：small, medium, large
        private Integer width;
        private Integer height;
        private String storageUrl; // 缩略图存储URL
        private Long fileSize;
    }

    /**
     * 图片质量分析
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageQualityInfo {
        private Double sharpness; // 清晰度评分 0-100
        private Double brightness; // 亮度 0-100
        private Double contrast; // 对比度 0-100
        private Double saturation; // 饱和度 0-100
        private Boolean hasBlur; // 是否模糊
        private Boolean hasNoise; // 是否有噪点
    }

    /**
     * 视频元数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoMetadata {
        private Integer width;
        private Integer height;
        private Double duration; // 时长(秒)
        private Double frameRate; // 帧率
        private Long bitrate; // 比特率
        private String codecName; // 编码格式：H.264, H.265, VP9等
        private String containerFormat; // 容器格式：MP4, AVI, MKV等
        private String aspectRatio; // 宽高比：16:9, 4:3等
        
        // 音视频流信息
        private List<VideoStreamInfo> videoStreams;
        private List<AudioStreamInfo> audioStreams;
        
        // 关键帧信息
        private List<KeyFrameInfo> keyFrames;
        
        // 预览信息
        private VideoPreviewInfo previewInfo;
        
        // 视频质量分析
        private VideoQualityInfo qualityInfo;
    }

    /**
     * 视频流信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoStreamInfo {
        private Integer streamIndex;
        private String codecName;
        private Long bitrate;
        private Double frameRate;
        private String pixelFormat;
        private String profile;
        private String level;
    }

    /**
     * 音频流信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AudioStreamInfo {
        private Integer streamIndex;
        private String codecName;
        private Long bitrate;
        private Integer sampleRate;
        private Integer channels;
        private String channelLayout;
        private Double duration;
    }

    /**
     * 关键帧信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeyFrameInfo {
        private Double timestamp; // 时间戳(秒)
        private String thumbnailUrl; // 关键帧缩略图URL
    }

    /**
     * 视频预览信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoPreviewInfo {
        private String posterUrl; // 封面图URL
        private String previewGifUrl; // 预览GIF URL
        private List<ThumbnailInfo> thumbnails; // 不同尺寸的封面图
    }

    /**
     * 视频质量分析
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoQualityInfo {
        private String resolution; // 分辨率等级：4K, 1080P, 720P等
        private String qualityLevel; // 质量等级：高清, 标清, 超清等
        private Boolean hasInterlacing; // 是否隔行扫描
        private Double averageBitrate; // 平均比特率
        private Boolean hasAudioIssues; // 是否有音频问题
        private Boolean hasVideoIssues; // 是否有视频问题
    }

    /**
     * 音频元数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AudioMetadata {
        private Double duration; // 时长(秒)
        private Long bitrate; // 比特率
        private Integer sampleRate; // 采样率
        private Integer channels; // 声道数
        private String channelLayout; // 声道布局：mono, stereo等
        private String codecName; // 编码格式：MP3, AAC, FLAC等
        private String container; // 容器格式
        
        // 音乐标签信息（ID3等）
        private MusicTagInfo musicTagInfo;
        
        // 音频质量分析
        private AudioQualityInfo qualityInfo;
        
        // 音频波形数据（可选）
        private AudioWaveformInfo waveformInfo;
    }

    /**
     * 音乐标签信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MusicTagInfo {
        private String title; // 标题
        private String artist; // 艺术家
        private String album; // 专辑
        private String genre; // 流派
        private Integer year; // 年份
        private Integer track; // 曲目号
        private String albumArtist; // 专辑艺术家
        private String composer; // 作曲家
        private String comment; // 备注
        private String albumArtUrl; // 专辑封面URL
    }

    /**
     * 音频质量分析
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AudioQualityInfo {
        private String qualityLevel; // 音质等级：无损, 高品质, 标准等
        private Double dynamicRange; // 动态范围
        private Double loudness; // 响度
        private Boolean hasClipping; // 是否有削波失真
        private Boolean hasSilence; // 是否有静音段
        private Double signalToNoiseRatio; // 信噪比
    }

    /**
     * 音频波形信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AudioWaveformInfo {
        private String waveformImageUrl; // 波形图片URL
        private List<Double> peakData; // 峰值数据
        private Integer samplePoints; // 采样点数
    }

    /**
     * 文档元数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentMetadata {
        private Integer pageCount; // 页数
        private Integer wordCount; // 字数
        private String language; // 语言
        private String encoding; // 编码
        private String author; // 作者
        private String title; // 标题
        private String subject; // 主题
        private List<String> keywords; // 关键词
        private String creator; // 创建软件
        private LocalDateTime documentCreateTime; // 文档创建时间
        private LocalDateTime documentModifyTime; // 文档修改时间
        
        // OCR提取的文本内容（可选）
        private DocumentContentInfo contentInfo;
        
        // 文档预览信息
        private DocumentPreviewInfo previewInfo;
    }

    /**
     * 文档内容信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentContentInfo {
        private String extractedText; // 提取的文本内容
        private List<String> extractedImages; // 提取的图片URL列表
        private Integer tableCount; // 表格数量
        private Integer imageCount; // 图片数量
        private Map<String, Object> structure; // 文档结构信息
    }

    /**
     * 文档预览信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentPreviewInfo {
        private List<String> pagePreviewUrls; // 各页预览图URL
        private String coverImageUrl; // 封面图URL
        private List<ThumbnailInfo> thumbnails; // 不同尺寸的预览图
    }

    /**
     * LED业务相关元数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LedBusinessMetadata {
        // 推荐显示设置
        private List<RecommendedDisplaySize> recommendedSizes;
        private Integer recommendedPlayDuration; // 推荐播放时长(秒)
        private Integer recommendedLoopCount; // 推荐循环次数
        private String playbackMode; // 播放模式：once, loop, auto等
        
        // 内容分类和标签
        private List<String> contentTags; // 内容标签：广告, 公告, 娱乐等
        private List<String> sceneTags; // 场景标签：商场, 学校, 户外等
        private String contentCategory; // 内容分类
        private String businessType; // 业务类型
        
        // 使用限制
        private List<String> suitableEnvironments; // 适用环境
        private List<String> unsuitableEnvironments; // 不适用环境
        private String ageRating; // 年龄分级
        private List<String> restrictions; // 使用限制
        
        // LED屏幕适配信息
        private List<LedScreenCompatibility> screenCompatibility;
    }

    /**
     * 推荐显示尺寸
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendedDisplaySize {
        private Integer width;
        private Integer height;
        private String sizeType; // 尺寸类型：small, medium, large等
        private String description; // 描述
    }

    /**
     * LED屏幕兼容性
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LedScreenCompatibility {
        private String screenSize; // 屏幕尺寸规格
        private String resolution; // 分辨率
        private String pixelPitch; // 像素间距
        private Boolean isCompatible; // 是否兼容
        private String compatibilityNote; // 兼容性说明
    }

    /**
     * AI分析结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiAnalysisResult {
        // 图像识别结果
        private List<ImageRecognitionTag> imageRecognitionTags;
        
        // 人脸检测结果
        private List<FaceDetectionInfo> faceDetectionInfo;
        
        // OCR文字识别结果
        private List<OcrTextInfo> ocrTextInfo;
        
        // 内容安全检测
        private ContentSafetyCheck contentSafetyCheck;
        
        // 情感分析（针对音频/视频）
        private EmotionAnalysisInfo emotionAnalysisInfo;
        
        // 分析时间
        private LocalDateTime analysisTime;
        private String analysisVersion; // AI模型版本
    }

    /**
     * 图像识别标签
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageRecognitionTag {
        private String tag; // 标签名称
        private Double confidence; // 置信度 0-1
        private String category; // 分类：人物, 物品, 场景等
        private BoundingBox boundingBox; // 边界框（可选）
    }

    /**
     * 边界框
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BoundingBox {
        private Integer x;
        private Integer y;
        private Integer width;
        private Integer height;
    }

    /**
     * 人脸检测信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FaceDetectionInfo {
        private Integer faceCount; // 人脸数量
        private List<FaceInfo> faces; // 人脸详情
    }

    /**
     * 人脸信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FaceInfo {
        private BoundingBox boundingBox; // 人脸位置
        private Double confidence; // 置信度
        private String gender; // 性别
        private Integer estimatedAge; // 估计年龄
        private String emotion; // 情绪
        private List<String> attributes; // 其他属性
    }

    /**
     * OCR文字识别信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OcrTextInfo {
        private String text; // 识别的文字
        private BoundingBox boundingBox; // 文字位置
        private Double confidence; // 置信度
        private String language; // 语言
    }

    /**
     * 内容安全检测
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentSafetyCheck {
        private Boolean isSafe; // 是否安全
        private String riskLevel; // 风险等级：low, medium, high
        private List<String> riskTypes; // 风险类型：violence, adult, terrorism等
        private Double safetyScore; // 安全评分 0-100
        private String reviewComment; // 审核意见
    }

    /**
     * 情感分析信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmotionAnalysisInfo {
        private String overallEmotion; // 整体情感：positive, negative, neutral
        private Double emotionScore; // 情感评分 -1到1
        private Map<String, Double> emotionBreakdown; // 情感细分
        private List<EmotionTimestamp> emotionTimeline; // 情感时间轴（视频/音频）
    }

    /**
     * 情感时间戳
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmotionTimestamp {
        private Double timestamp; // 时间戳(秒)
        private String emotion; // 情感
        private Double score; // 评分
    }

    /**
     * 处理历史记录
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessingHistory {
        private String processType; // 处理类型：upload, transcode, analyze等
        private String processStatus; // 处理状态：success, failed, processing
        private LocalDateTime processTime; // 处理时间
        private String processDetails; // 处理详情
        private String errorMessage; // 错误信息（如果失败）
        private Map<String, Object> processParams; // 处理参数
    }
}