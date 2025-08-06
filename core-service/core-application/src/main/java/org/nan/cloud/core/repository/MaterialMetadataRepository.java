package org.nan.cloud.core.repository;

import org.nan.cloud.common.basic.domain.MaterialMetadata;

/**
 * 素材元数据仓储接口
 * 
 * 负责素材元数据的MongoDB存储操作
 * 包括详细的文件元数据、AI分析结果、LED业务属性等
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface MaterialMetadataRepository {

    /**
     * 保存素材元数据到MongoDB
     * @param metadata 素材元数据
     * @return MongoDB文档ID
     */
    String save(MaterialMetadata metadata);

    /**
     * 根据ID查询素材元数据
     * @param id MongoDB文档ID
     * @return 素材元数据
     */
    MaterialMetadata findById(String id);

    /**
     * 根据文件ID查询素材元数据
     * @param fileId 文件ID
     * @return 素材元数据
     */
    MaterialMetadata findByFileId(String fileId);

    /**
     * 更新素材元数据
     * @param metadata 素材元数据
     */
    void update(MaterialMetadata metadata);

    /**
     * 删除素材元数据
     * @param id MongoDB文档ID
     */
    void deleteById(String id);

    /**
     * 批量删除素材元数据
     * @param fileIds 文件ID列表
     */
    void deleteByFileIds(java.util.List<String> fileIds);
}