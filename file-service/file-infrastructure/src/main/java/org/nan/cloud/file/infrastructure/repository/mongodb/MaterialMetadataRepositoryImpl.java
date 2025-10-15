package org.nan.cloud.file.infrastructure.repository.mongodb;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.domain.MaterialMetadata;
import org.nan.cloud.file.application.repository.MaterialMetadataRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 素材元数据MongoDB存储库实现
 * 
 * 特性：
 * 1. 直接使用统一的MaterialMetadata模型，无需Document转换
 * 2. 保持_class字段，确保跨服务兼容性
 * 3. 支持缩略图信息的完整存储和查询
 * 4. 优化的MongoDB操作性能
 * 
 * ⚠️  字段映射说明：
 * - MaterialMetadata类无MongoDB注解，依赖Spring Data默认映射
 * - 查询时字段名必须与MongoDB文档字段名完全一致
 * - 主要查询字段：id (ObjectId), fileId (String)
 * - 避免对嵌套字段（如basicInfo.fileSize）进行复杂查询
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
            if (metadata.getCreatedAt() == null) {
                metadata.setCreatedAt(LocalDateTime.now());
            }
            metadata.setUpdatedAt(LocalDateTime.now());
            
            MaterialMetadata saved = mongoTemplate.save(metadata, COLLECTION_NAME);
            log.debug("✅ 元数据保存成功 - ID: {}, 文件ID: {}, 类型: {}", 
                    saved.getId(), saved.getFileId(), 
                    saved.getBasicInfo() != null ? saved.getBasicInfo().getFileType() : "UNKNOWN");
            return saved.getId();
        } catch (Exception e) {
            log.error("❌ 保存元数据失败 - 文件ID: {}, 错误: {}", metadata.getFileId(), e.getMessage(), e);
            throw new RuntimeException("保存元数据失败", e);
        }
    }

    @Override
    public MaterialMetadata findById(String id) {
        try {
            MaterialMetadata metadata = mongoTemplate.findById(id, MaterialMetadata.class, COLLECTION_NAME);
            if (metadata != null) {
                log.debug("✅ 查询元数据成功 - ID: {}, 文件ID: {}", id, metadata.getFileId());
            } else {
                log.debug("⚠️ 元数据不存在 - ID: {}", id);
            }
            return metadata;
        } catch (Exception e) {
            log.error("❌ 查询元数据失败 - ID: {}, 错误: {}", id, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public MaterialMetadata findByFileId(String fileId) {
        try {
            Query query = new Query(Criteria.where("fileId").is(fileId));
            MaterialMetadata metadata = mongoTemplate.findOne(query, MaterialMetadata.class, COLLECTION_NAME);
            if (metadata != null) {
                log.debug("✅ 根据文件ID查询元数据成功 - 文件ID: {}, 元数据ID: {}", fileId, metadata.getId());
            } else {
                log.debug("⚠️ 文件ID对应的元数据不存在 - 文件ID: {}", fileId);
            }
            return metadata;
        } catch (Exception e) {
            log.error("❌ 根据文件ID查询元数据失败 - 文件ID: {}, 错误: {}", fileId, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean update(MaterialMetadata metadata) {
        try {
            metadata.setUpdatedAt(LocalDateTime.now());
            MaterialMetadata saved = mongoTemplate.save(metadata, COLLECTION_NAME);
            log.debug("✅ 元数据更新成功 - ID: {}, 文件ID: {}", saved.getId(), saved.getFileId());
            return true;
        } catch (Exception e) {
            log.error("❌ 更新元数据失败 - ID: {}, 错误: {}", metadata.getId(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean delete(String id) {
        try {
            Query query = new Query(Criteria.where("id").is(id));
            mongoTemplate.remove(query, MaterialMetadata.class, COLLECTION_NAME);
            log.debug("✅ 元数据删除成功 - ID: {}", id);
            return true;
        } catch (Exception e) {
            log.error("❌ 删除元数据失败 - ID: {}, 错误: {}", id, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean deleteByFileId(String fileId) {
        try {
            Query query = new Query(Criteria.where("fileId").is(fileId));
            mongoTemplate.remove(query, MaterialMetadata.class, COLLECTION_NAME);
            log.debug("✅ 根据文件ID删除元数据成功 - 文件ID: {}", fileId);
            return true;
        } catch (Exception e) {
            log.error("❌ 根据文件ID删除元数据失败 - 文件ID: {}, 错误: {}", fileId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 更新缩略图信息
     * 
     * @param fileId 文件ID
     * @param thumbnails 缩略图集合
     * @return 是否更新成功
     */
    public boolean updateThumbnails(String fileId, MaterialMetadata.ThumbnailCollection thumbnails) {
        try {
            Query query = new Query(Criteria.where("fileId").is(fileId));
            Update update = new Update()
                    .set("thumbnails", thumbnails)
                    .set("updatedAt", LocalDateTime.now());
            
            mongoTemplate.updateFirst(query, update, MaterialMetadata.class, COLLECTION_NAME);
            log.debug("✅ 缩略图信息更新成功 - 文件ID: {}, 缩略图数量: {}", 
                    fileId, thumbnails.getAllThumbnails() != null ? thumbnails.getAllThumbnails().size() : 0);
            return true;
        } catch (Exception e) {
            log.error("❌ 更新缩略图信息失败 - 文件ID: {}, 错误: {}", fileId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 更新分析状态
     * 
     * @param fileId 文件ID
     * @param status 分析状态
     * @param errorMessage 错误信息（可选）
     * @return 是否更新成功
     */
    public boolean updateAnalysisStatus(String fileId, String status, String errorMessage) {
        try {
            Query query = new Query(Criteria.where("fileId").is(fileId));
            Update update = new Update()
                    .set("analysisStatus", status)
                    .set("updatedAt", LocalDateTime.now());
                    
            if (errorMessage != null) {
                update.set("analysisError", errorMessage);
            }
            
            mongoTemplate.updateFirst(query, update, MaterialMetadata.class, COLLECTION_NAME);
            log.debug("✅ 分析状态更新成功 - 文件ID: {}, 状态: {}", fileId, status);
            return true;
        } catch (Exception e) {
            log.error("❌ 更新分析状态失败 - 文件ID: {}, 错误: {}", fileId, e.getMessage(), e);
            return false;
        }
    }
}