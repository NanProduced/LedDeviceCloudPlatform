package org.nan.cloud.file.infrastructure.repository.mongodb;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.file.application.domain.MaterialMetadata;
import org.nan.cloud.file.application.repository.MaterialMetadataRepository;
import org.nan.cloud.file.infrastructure.mongodb.document.MaterialMetadataDocument;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

/**
 * 素材元数据MongoDB存储库实现
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MaterialMetadataRepositoryImpl implements MaterialMetadataRepository {

    private final MongoTemplate mongoTemplate;
    
    private static final String COLLECTION_NAME = "material_metadata";

    @Override
    public String save(MaterialMetadata metadata) {
        try {
            MaterialMetadataDocument document = toDocument(metadata);
            MaterialMetadataDocument saved = mongoTemplate.save(document, COLLECTION_NAME);
            log.debug("元数据保存成功 - ID: {}, 文件ID: {}", saved.getId(), saved.getFileId());
            return saved.getId();
        } catch (Exception e) {
            log.error("保存元数据失败 - 文件ID: {}, 错误: {}", metadata.getFileId(), e.getMessage(), e);
            throw new RuntimeException("保存元数据失败", e);
        }
    }

    @Override
    public MaterialMetadata findById(String id) {
        try {
            MaterialMetadataDocument document = mongoTemplate.findById(id, MaterialMetadataDocument.class, COLLECTION_NAME);
            return document != null ? toDomain(document) : null;
        } catch (Exception e) {
            log.error("查询元数据失败 - ID: {}, 错误: {}", id, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public MaterialMetadata findByFileId(String fileId) {
        try {
            Query query = new Query(Criteria.where("file_id").is(fileId));
            MaterialMetadataDocument document = mongoTemplate.findOne(query, MaterialMetadataDocument.class, COLLECTION_NAME);
            return document != null ? toDomain(document) : null;
        } catch (Exception e) {
            log.error("根据文件ID查询元数据失败 - 文件ID: {}, 错误: {}", fileId, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean update(MaterialMetadata metadata) {
        try {
            MaterialMetadataDocument document = toDocument(metadata);
            MaterialMetadataDocument saved = mongoTemplate.save(document, COLLECTION_NAME);
            log.debug("元数据更新成功 - ID: {}, 文件ID: {}", saved.getId(), saved.getFileId());
            return true;
        } catch (Exception e) {
            log.error("更新元数据失败 - ID: {}, 错误: {}", metadata.getId(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean delete(String id) {
        try {
            Query query = new Query(Criteria.where("id").is(id));
            mongoTemplate.remove(query, MaterialMetadataDocument.class, COLLECTION_NAME);
            log.debug("元数据删除成功 - ID: {}", id);
            return true;
        } catch (Exception e) {
            log.error("删除元数据失败 - ID: {}, 错误: {}", id, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean deleteByFileId(String fileId) {
        try {
            Query query = new Query(Criteria.where("file_id").is(fileId));
            mongoTemplate.remove(query, MaterialMetadataDocument.class, COLLECTION_NAME);
            log.debug("根据文件ID删除元数据成功 - 文件ID: {}", fileId);
            return true;
        } catch (Exception e) {
            log.error("根据文件ID删除元数据失败 - 文件ID: {}, 错误: {}", fileId, e.getMessage(), e);
            return false;
        }
    }

    // ========== 私有转换方法 ==========

    /**
     * 领域对象转MongoDB文档
     */
    private MaterialMetadataDocument toDocument(MaterialMetadata metadata) {
        if (metadata == null) {
            return null;
        }

        return MaterialMetadataDocument.builder()
                .id(metadata.getId())
                .fileId(metadata.getFileId())
                .originalFilename(metadata.getOriginalFilename())
                .fileSize(metadata.getFileSize())
                .mimeType(metadata.getMimeType())
                .fileType(metadata.getFileType())
                .fileFormat(metadata.getFileFormat())
                .md5Hash(metadata.getMd5Hash())
                
                // 图片相关
                .imageWidth(metadata.getImageWidth())
                .imageHeight(metadata.getImageHeight())
                .colorDepth(metadata.getColorDepth())
                .colorSpace(metadata.getColorSpace())
                .dpiHorizontal(metadata.getDpiHorizontal())
                .dpiVertical(metadata.getDpiVertical())
                .cameraMake(metadata.getCameraMake())
                .cameraModel(metadata.getCameraModel())
                .dateTaken(metadata.getDateTaken())
                .gpsLatitude(metadata.getGpsLatitude())
                .gpsLongitude(metadata.getGpsLongitude())
                
                // 视频相关
                .videoDuration(metadata.getVideoDuration())
                .videoCodec(metadata.getVideoCodec())
                .videoBitrate(metadata.getVideoBitrate())
                .frameRate(metadata.getFrameRate())
                .videoWidth(metadata.getVideoWidth())
                .videoHeight(metadata.getVideoHeight())
                .aspectRatio(metadata.getAspectRatio())
                
                // 音频相关
                .audioCodec(metadata.getAudioCodec())
                .audioBitrate(metadata.getAudioBitrate())
                .sampleRate(metadata.getSampleRate())
                .channels(metadata.getChannels())
                .audioDuration(metadata.getAudioDuration())
                
                // 文档相关
                .pageCount(metadata.getPageCount())
                .documentTitle(metadata.getDocumentTitle())
                .documentAuthor(metadata.getDocumentAuthor())
                .documentSubject(metadata.getDocumentSubject())
                .documentKeywords(metadata.getDocumentKeywords())
                .documentCreated(metadata.getDocumentCreated())
                .documentModified(metadata.getDocumentModified())
                
                // 扩展字段
                .exifData(metadata.getExifData())
                .additionalProperties(metadata.getAdditionalProperties())
                
                // 系统字段
                .analysisTaskId(metadata.getAnalysisTaskId())
                .analysisStatus(metadata.getAnalysisStatus())
                .analysisError(metadata.getAnalysisError())
                .createdAt(metadata.getCreatedAt())
                .updatedAt(metadata.getUpdatedAt())
                .organizationId(metadata.getOrganizationId())
                .build();
    }

    /**
     * MongoDB文档转领域对象
     */
    private MaterialMetadata toDomain(MaterialMetadataDocument document) {
        if (document == null) {
            return null;
        }

        return MaterialMetadata.builder()
                .id(document.getId())
                .fileId(document.getFileId())
                .originalFilename(document.getOriginalFilename())
                .fileSize(document.getFileSize())
                .mimeType(document.getMimeType())
                .fileType(document.getFileType())
                .fileFormat(document.getFileFormat())
                .md5Hash(document.getMd5Hash())
                
                // 图片相关
                .imageWidth(document.getImageWidth())
                .imageHeight(document.getImageHeight())
                .colorDepth(document.getColorDepth())
                .colorSpace(document.getColorSpace())
                .dpiHorizontal(document.getDpiHorizontal())
                .dpiVertical(document.getDpiVertical())
                .cameraMake(document.getCameraMake())
                .cameraModel(document.getCameraModel())
                .dateTaken(document.getDateTaken())
                .gpsLatitude(document.getGpsLatitude())
                .gpsLongitude(document.getGpsLongitude())
                
                // 视频相关
                .videoDuration(document.getVideoDuration())
                .videoCodec(document.getVideoCodec())
                .videoBitrate(document.getVideoBitrate())
                .frameRate(document.getFrameRate())
                .videoWidth(document.getVideoWidth())
                .videoHeight(document.getVideoHeight())
                .aspectRatio(document.getAspectRatio())
                
                // 音频相关
                .audioCodec(document.getAudioCodec())
                .audioBitrate(document.getAudioBitrate())
                .sampleRate(document.getSampleRate())
                .channels(document.getChannels())
                .audioDuration(document.getAudioDuration())
                
                // 文档相关
                .pageCount(document.getPageCount())
                .documentTitle(document.getDocumentTitle())
                .documentAuthor(document.getDocumentAuthor())
                .documentSubject(document.getDocumentSubject())
                .documentKeywords(document.getDocumentKeywords())
                .documentCreated(document.getDocumentCreated())
                .documentModified(document.getDocumentModified())
                
                // 扩展字段
                .exifData(document.getExifData())
                .additionalProperties(document.getAdditionalProperties())
                
                // 系统字段
                .analysisTaskId(document.getAnalysisTaskId())
                .analysisStatus(document.getAnalysisStatus())
                .analysisError(document.getAnalysisError())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .organizationId(document.getOrganizationId())
                .build();
    }
}