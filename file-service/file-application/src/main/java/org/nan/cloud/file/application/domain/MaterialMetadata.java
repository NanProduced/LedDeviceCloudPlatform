package org.nan.cloud.file.application.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 素材元数据领域模型 - file-service
 * 
 * 专注于文件的技术元数据信息
 * 与core-service的MaterialMetadata职责分离
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
     * 元数据ID (MongoDB _id)
     */
    private String id;

    /**
     * 关联的文件ID
     */
    private String fileId;

    /**
     * 文件原始名称
     */
    private String originalFilename;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * MIME类型
     */
    private String mimeType;

    /**
     * 文件类型 (IMAGE, VIDEO, AUDIO, DOCUMENT等)
     */
    private String fileType;

    /**
     * 文件格式 (jpg, png, mp4, pdf等)
     */
    private String fileFormat;

    /**
     * MD5哈希值
     */
    private String md5Hash;

    // ========== 图片相关元数据 ==========
    
    /**
     * 图片宽度
     */
    private Integer imageWidth;

    /**
     * 图片高度
     */
    private Integer imageHeight;

    /**
     * 颜色深度
     */
    private Integer colorDepth;

    /**
     * 颜色空间
     */
    private String colorSpace;

    /**
     * DPI水平分辨率
     */
    private Integer dpiHorizontal;

    /**
     * DPI垂直分辨率
     */
    private Integer dpiVertical;

    /**
     * 相机制造商
     */
    private String cameraMake;

    /**
     * 相机型号
     */
    private String cameraModel;

    /**
     * 拍摄时间
     */
    private LocalDateTime dateTaken;

    /**
     * GPS纬度
     */
    private Double gpsLatitude;

    /**
     * GPS经度
     */
    private Double gpsLongitude;

    // ========== 视频相关元数据 ==========

    /**
     * 视频时长（秒）
     */
    private Long videoDuration;

    /**
     * 视频编解码器
     */
    private String videoCodec;

    /**
     * 视频比特率
     */
    private Long videoBitrate;

    /**
     * 帧率
     */
    private Double frameRate;

    /**
     * 视频宽度
     */
    private Integer videoWidth;

    /**
     * 视频高度
     */
    private Integer videoHeight;

    /**
     * 宽高比
     */
    private String aspectRatio;

    // ========== 音频相关元数据 ==========

    /**
     * 音频编解码器
     */
    private String audioCodec;

    /**
     * 音频比特率
     */
    private Long audioBitrate;

    /**
     * 采样率
     */
    private Integer sampleRate;

    /**
     * 声道数
     */
    private Integer channels;

    /**
     * 音频时长（秒）
     */
    private Long audioDuration;

    // ========== 文档相关元数据 ==========

    /**
     * 页数（适用于PDF等文档）
     */
    private Integer pageCount;

    /**
     * 文档标题
     */
    private String documentTitle;

    /**
     * 文档作者
     */
    private String documentAuthor;

    /**
     * 文档主题
     */
    private String documentSubject;

    /**
     * 文档关键词
     */
    private String documentKeywords;

    /**
     * 文档创建时间
     */
    private LocalDateTime documentCreated;

    /**
     * 文档修改时间
     */
    private LocalDateTime documentModified;

    // ========== 扩展元数据 ==========

    /**
     * 原始EXIF数据（JSON格式）
     */
    private String exifData;

    /**
     * 其他扩展属性
     */
    private Map<String, Object> additionalProperties;

    // ========== 系统字段 ==========

    /**
     * 分析任务ID
     */
    private String analysisTaskId;

    /**
     * 分析状态
     */
    private String analysisStatus;

    /**
     * 分析错误信息
     */
    private String analysisError;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 组织ID
     */
    private String organizationId;
}