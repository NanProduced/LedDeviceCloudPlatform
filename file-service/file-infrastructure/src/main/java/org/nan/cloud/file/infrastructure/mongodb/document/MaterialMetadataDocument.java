package org.nan.cloud.file.infrastructure.mongodb.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 素材元数据MongoDB文档映射类
 * 
 * 对应MongoDB中的material_metadata集合
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "material_metadata")
public class MaterialMetadataDocument {

    @Id
    private String id;

    @Field("file_id")
    private String fileId;

    @Field("original_filename")
    private String originalFilename;

    @Field("file_size")
    private Long fileSize;

    @Field("mime_type")
    private String mimeType;

    @Field("file_type")
    private String fileType;

    @Field("file_format")
    private String fileFormat;

    @Field("md5_hash")
    private String md5Hash;

    // ========== 图片相关元数据 ==========
    
    @Field("image_width")
    private Integer imageWidth;

    @Field("image_height")
    private Integer imageHeight;

    @Field("color_depth")
    private Integer colorDepth;

    @Field("color_space")
    private String colorSpace;

    @Field("dpi_horizontal")
    private Integer dpiHorizontal;

    @Field("dpi_vertical")
    private Integer dpiVertical;

    @Field("camera_make")
    private String cameraMake;

    @Field("camera_model")
    private String cameraModel;

    @Field("date_taken")
    private LocalDateTime dateTaken;

    @Field("gps_latitude")
    private Double gpsLatitude;

    @Field("gps_longitude")
    private Double gpsLongitude;

    // ========== 视频相关元数据 ==========

    @Field("video_duration")
    private Long videoDuration;

    @Field("video_codec")
    private String videoCodec;

    @Field("video_bitrate")
    private Long videoBitrate;

    @Field("frame_rate")
    private Double frameRate;

    @Field("video_width")
    private Integer videoWidth;

    @Field("video_height")
    private Integer videoHeight;

    @Field("aspect_ratio")
    private String aspectRatio;

    // ========== 音频相关元数据 ==========

    @Field("audio_codec")
    private String audioCodec;

    @Field("audio_bitrate")
    private Long audioBitrate;

    @Field("sample_rate")
    private Integer sampleRate;

    @Field("channels")
    private Integer channels;

    @Field("audio_duration")
    private Long audioDuration;

    // ========== 文档相关元数据 ==========

    @Field("page_count")
    private Integer pageCount;

    @Field("document_title")
    private String documentTitle;

    @Field("document_author")
    private String documentAuthor;

    @Field("document_subject")
    private String documentSubject;

    @Field("document_keywords")
    private String documentKeywords;

    @Field("document_created")
    private LocalDateTime documentCreated;

    @Field("document_modified")
    private LocalDateTime documentModified;

    // ========== 扩展元数据 ==========

    @Field("exif_data")
    private String exifData;

    @Field("additional_properties")
    private Map<String, Object> additionalProperties;

    // ========== 系统字段 ==========

    @Field("analysis_task_id")
    private String analysisTaskId;

    @Field("analysis_status")
    private String analysisStatus;

    @Field("analysis_error")
    private String analysisError;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Field("organization_id")
    private String organizationId;
}