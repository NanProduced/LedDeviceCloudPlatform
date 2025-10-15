package org.nan.cloud.common.basic.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 素材元数据统一模型 - 跨服务共享
 * 
 * 用于MongoDB存储和跨服务查询，采用嵌套结构支持复杂业务需求。
 * 所有服务统一使用此模型，确保兼容性。
 * 
 * 存储策略：
 * - MongoDB集合：material_metadata
 * - 支持多种文件类型的元数据
 * - 包含完整的缩略图信息
 * - 自动生成_class字段确保跨服务兼容
 * 
 * ⚠️  重要使用说明：
 * 1. 【无MongoDB注解】
 *    - 此类作为通用模型，不包含@Document、@Field等MongoDB注解
 *    - 各服务使用时需要手动处理字段映射关系
 *    - 依赖Spring Data MongoDB的默认字段映射规则
 * 
 * 2. 【字段名称映射】
 *    - Java字段名（驼峰）→ MongoDB字段名（驼峰）
 *    - 示例：fileId → fileId, createdAt → createdAt
 *    - ⚠️ 务必确认字段名称与实际MongoDB文档一致
 * 
 * 3. 【查询操作建议】
 *    - 主要查询字段：id (ObjectId), fileId
 *    - 推荐查询模式：根据fileId或ObjectId进行简单查询
 *    - ⚠️ 避免复杂的聚合操作和深度嵌套字段查询
 *    - ⚠️ 复杂查询可能导致性能问题和字段映射错误
 * 
 * 4. 【跨服务兼容性】
 *    - file-service：负责创建和更新元数据
 *    - core-service：负责查询和展示元数据
 *    - 两服务使用相同类路径，_class字段自然兼容
 * 
 * 5. 【扩展性考虑】
 *    - 新增字段时保持向后兼容
 *    - 嵌套结构支持复杂业务场景
 *    - 预留AI分析和LED业务扩展能力
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
     * 原始文件名
     */
    private String originalFilename;

    /**
     * 分析任务ID
     */
    private String analysisTaskId;

    /**
     * 组织ID
     */
    private String organizationId;

    /**
     * 基础文件信息
     */
    private FileBasicInfo basicInfo;

    /**
     * 缩略图信息（所有生成的缩略图）
     */
    private ThumbnailCollection thumbnails;

    /**
     * 图片元数据（仅图片文件）
     */
    private ImageMetadata imageMetadata;

    /**
     * 视频元数据（仅视频文件）
     */
    private VideoMetadata videoMetadata;

    /**
     * 音频元数据（仅音频文件）
     */
    private AudioMetadata audioMetadata;

    /**
     * 文档元数据（仅文档文件）
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
     * 分析状态
     */
    private String analysisStatus;

    /**
     * 分析错误信息
     */
    private String analysisError;

    /**
     * 元数据创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 元数据更新时间
     */
    private LocalDateTime updatedAt;

    // ========================= 基础信息类 =========================

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
        private String fileType; // IMAGE, VIDEO, AUDIO, DOCUMENT
        private String fileFormat; // jpg, mp4, pdf等
        private String fileExtension;
        private Long fileSize;
        private String md5Hash;
        private String sha256Hash; // 可选
        private String encoding; // 文件编码
        private Map<String, Object> additionalProperties; // Tika解析的额外属性
    }

    /**
     * 缩略图集合信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThumbnailCollection {
        /**
         * 主缩略图（存储在MySQL中的那个）
         */
        private ThumbnailInfo primaryThumbnail;
        
        /**
         * 所有生成的缩略图
         */
        private List<ThumbnailInfo> allThumbnails;
        
        /**
         * 缩略图生成状态
         */
        private String generationStatus; // SUCCESS, FAILED, PROCESSING
        
        /**
         * 缩略图生成时间
         */
        private LocalDateTime generatedAt;
        
        /**
         * 失败原因（如果生成失败）
         */
        private String errorMessage;
    }

    /**
     * 缩略图信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThumbnailInfo {
        private String size; // 尺寸规格：150x150, 300x300, 600x600
        private Integer width;
        private Integer height;
        private String storageUrl; // 缩略图存储URL
        private String storagePath; // 缩略图存储路径
        private Long fileSize; // 缩略图文件大小
        private String format; // 缩略图格式：jpg, png等
        private Boolean isPrimary; // 是否为主缩略图
    }

    // ========================= 图片元数据 =========================

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
        private Integer colorDepth; // 色深
        private String colorSpace; // 色彩空间：RGB, CMYK, Gray等
        private Boolean hasAlpha; // 是否有透明通道
        private Integer dpiHorizontal; // 水平DPI
        private Integer dpiVertical; // 垂直DPI
        
        // GIF动画相关字段
        private Boolean isAnimated; // 是否为动画GIF
        private Integer frameCount; // 动画帧数
        private Long animationDuration; // 动画总时长(毫秒)
        private Integer loopCount; // 循环次数，0表示无限循环
        private Double averageFrameDelay; // 平均帧延迟(毫秒)
        
        // EXIF信息
        private ExifInfo exifInfo;
        
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
        private LocalDateTime dateTaken; // 拍摄时间
        private String lensModel; // 镜头型号
        private Double focalLength; // 焦距
        private String aperture; // 光圈
        private String shutterSpeed; // 快门速度
        private Integer iso; // ISO感光度
        private String orientation; // 方向
        
        // GPS信息
        private Double gpsLatitude; // 纬度
        private Double gpsLongitude; // 经度
        private Double gpsAltitude; // 海拔
        private String gpsLocation; // 地理位置描述
        
        private Map<String, Object> additionalExif; // 其他EXIF数据
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
        private String qualityLevel; // 质量等级：高清, 标清等
    }

    // ========================= 视频元数据 =========================

    /**
     * 视频元数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoMetadata {
        private Integer videoWidth;
        private Integer videoHeight;
        private Long videoDuration; // 时长(秒)
        private Double frameRate; // 帧率
        private Long videoBitrate; // 视频比特率
        private String videoCodec; // 视频编码：H.264, H.265等
        private String aspectRatio; // 宽高比：16:9, 4:3等
        private String containerFormat; // 容器格式：MP4, AVI等
        
        // 音频信息（视频中的音频流）
        private AudioStreamInfo audioStream;
        
        // 视频质量分析
        private VideoQualityInfo qualityInfo;
    }

    /**
     * 音频流信息（视频中的音频）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AudioStreamInfo {
        private String audioCodec; // 音频编码
        private Long audioBitrate; // 音频比特率
        private Integer sampleRate; // 采样率
        private Integer channels; // 声道数
        private String channelLayout; // 声道布局
        private Long audioDuration; // 音频时长
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
        private String qualityLevel; // 质量等级：超清, 高清, 标清等
        private Boolean hasInterlacing; // 是否隔行扫描
        private Double averageBitrate; // 平均比特率
        private Boolean hasAudioIssues; // 是否有音频问题
        private Boolean hasVideoIssues; // 是否有视频问题
    }

    // ========================= 音频元数据 =========================

    /**
     * 音频元数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AudioMetadata {
        private Long audioDuration; // 时长(秒)
        private Long audioBitrate; // 比特率
        private Integer sampleRate; // 采样率
        private Integer channels; // 声道数
        private String channelLayout; // 声道布局：mono, stereo等
        private String audioCodec; // 编码格式：MP3, AAC, FLAC等
        private String containerFormat; // 容器格式
        
        // 音乐标签信息（ID3等）
        private MusicTagInfo musicTagInfo;
        
        // 音频质量分析
        private AudioQualityInfo qualityInfo;
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

    // ========================= 文档元数据 =========================

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
        private String documentAuthor; // 作者
        private String documentTitle; // 标题
        private String documentSubject; // 主题
        private String documentKeywords; // 关键词
        private String creator; // 创建软件
        private LocalDateTime documentCreated; // 文档创建时间
        private LocalDateTime documentModified; // 文档修改时间
        
        // 文档内容信息
        private DocumentContentInfo contentInfo;
    }

    /**
     * 文档内容信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentContentInfo {
        private String extractedText; // 提取的文本内容（摘要）
        private Integer tableCount; // 表格数量
        private Integer imageCount; // 图片数量
        private Map<String, Object> structure; // 文档结构信息
    }

    // ========================= LED业务元数据 =========================

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
        
        // 使用限制和兼容性
        private List<String> suitableEnvironments; // 适用环境
        private List<String> unsuitableEnvironments; // 不适用环境
        private String ageRating; // 年龄分级
        private List<LedScreenCompatibility> screenCompatibility; // 屏幕兼容性
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

    // ========================= AI分析结果 =========================

    /**
     * AI分析结果（预留扩展）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiAnalysisResult {
        // 图像识别标签
        private List<ImageRecognitionTag> imageRecognitionTags;
        
        // 内容安全检测
        private ContentSafetyCheck contentSafetyCheck;
        
        // 分析时间和版本
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
        private List<String> riskTypes; // 风险类型
        private Double safetyScore; // 安全评分 0-100
        private String reviewComment; // 审核意见
    }
}