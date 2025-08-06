package org.nan.cloud.file.application.repository;

import org.nan.cloud.common.basic.domain.MaterialMetadata;

/**
 * 素材元数据存储库接口
 * 
 * 负责MaterialMetadata在MongoDB中的CRUD操作
 * 
 * @author LedDeviceCloudPlatform Team  
 * @since 1.0.0
 */
public interface MaterialMetadataRepository {

    /**
     * 保存元数据
     * 
     * @param metadata 元数据对象
     * @return 保存后的元数据ID
     */
    String save(MaterialMetadata metadata);

    /**
     * 根据ID查找元数据
     * 
     * @param id 元数据ID
     * @return 元数据对象
     */
    MaterialMetadata findById(String id);

    /**
     * 根据文件ID查找元数据
     * 
     * @param fileId 文件ID
     * @return 元数据对象
     */
    MaterialMetadata findByFileId(String fileId);

    /**
     * 更新元数据
     * 
     * @param metadata 元数据对象
     * @return 是否更新成功
     */
    boolean update(MaterialMetadata metadata);

    /**
     * 删除元数据
     * 
     * @param id 元数据ID
     * @return 是否删除成功
     */
    boolean delete(String id);

    /**
     * 根据文件ID删除元数据
     * 
     * @param fileId 文件ID
     * @return 是否删除成功
     */
    boolean deleteByFileId(String fileId);
    
    /**
     * 更新缩略图信息
     * 
     * @param fileId 文件ID
     * @param thumbnails 缩略图集合
     * @return 是否更新成功
     */
    boolean updateThumbnails(String fileId, MaterialMetadata.ThumbnailCollection thumbnails);
    
    /**
     * 更新分析状态
     * 
     * @param fileId 文件ID
     * @param status 分析状态
     * @param errorMessage 错误信息（可选）
     * @return 是否更新成功
     */
    boolean updateAnalysisStatus(String fileId, String status, String errorMessage);
}