package org.nan.cloud.core.infrastructure.repository.mongodb.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.core.domain.MaterialMetadata;
import org.nan.cloud.core.repository.MaterialMetadataRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 素材元数据仓储MongoDB实现
 * 
 * 负责MaterialMetadata在MongoDB中的持久化操作
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
            MaterialMetadata saved = mongoTemplate.save(metadata, COLLECTION_NAME);
            log.info("保存素材元数据成功 - ID: {}, 文件ID: {}", saved.getId(), saved.getFileId());
            return saved.getId();
        } catch (Exception e) {
            log.error("保存素材元数据失败 - 文件ID: {}, 错误: {}", metadata.getFileId(), e.getMessage(), e);
            throw new RuntimeException("保存素材元数据失败", e);
        }
    }

    @Override
    public MaterialMetadata findById(String id) {
        try {
            MaterialMetadata metadata = mongoTemplate.findById(id, MaterialMetadata.class, COLLECTION_NAME);
            if (metadata != null) {
                log.debug("查询素材元数据成功 - ID: {}", id);
            } else {
                log.warn("未找到素材元数据 - ID: {}", id);
            }
            return metadata;
        } catch (Exception e) {
            log.error("查询素材元数据失败 - ID: {}, 错误: {}", id, e.getMessage(), e);
            throw new RuntimeException("查询素材元数据失败", e);
        }
    }

    @Override
    public MaterialMetadata findByFileId(String fileId) {
        try {
            Query query = new Query(Criteria.where("fileId").is(fileId));
            MaterialMetadata metadata = mongoTemplate.findOne(query, MaterialMetadata.class, COLLECTION_NAME);
            if (metadata != null) {
                log.debug("根据文件ID查询素材元数据成功 - 文件ID: {}", fileId);
            } else {
                log.warn("未找到素材元数据 - 文件ID: {}", fileId);
            }
            return metadata;
        } catch (Exception e) {
            log.error("根据文件ID查询素材元数据失败 - 文件ID: {}, 错误: {}", fileId, e.getMessage(), e);
            throw new RuntimeException("查询素材元数据失败", e);
        }
    }

    @Override
    public void update(MaterialMetadata metadata) {
        try {
            MaterialMetadata saved = mongoTemplate.save(metadata, COLLECTION_NAME);
            log.info("更新素材元数据成功 - ID: {}, 文件ID: {}", saved.getId(), saved.getFileId());
        } catch (Exception e) {
            log.error("更新素材元数据失败 - ID: {}, 错误: {}", metadata.getId(), e.getMessage(), e);
            throw new RuntimeException("更新素材元数据失败", e);
        }
    }

    @Override
    public void deleteById(String id) {
        try {
            Query query = new Query(Criteria.where("id").is(id));
            mongoTemplate.remove(query, MaterialMetadata.class, COLLECTION_NAME);
            log.info("删除素材元数据成功 - ID: {}", id);
        } catch (Exception e) {
            log.error("删除素材元数据失败 - ID: {}, 错误: {}", id, e.getMessage(), e);
            throw new RuntimeException("删除素材元数据失败", e);
        }
    }

    @Override
    public void deleteByFileIds(List<String> fileIds) {
        try {
            Query query = new Query(Criteria.where("fileId").in(fileIds));
            long deletedCount = mongoTemplate.remove(query, MaterialMetadata.class, COLLECTION_NAME).getDeletedCount();
            log.info("批量删除素材元数据成功 - 删除数量: {}, 文件ID列表: {}", deletedCount, fileIds);
        } catch (Exception e) {
            log.error("批量删除素材元数据失败 - 文件ID列表: {}, 错误: {}", fileIds, e.getMessage(), e);
            throw new RuntimeException("批量删除素材元数据失败", e);
        }
    }
}