package org.nan.cloud.core.repository;

import org.nan.cloud.core.event.mq.FileUploadEvent;

/**
 * 素材文件存储库接口
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface MaterialFileRepository {

    /**
     * 创建文件记录
     * 
     * @param event 文件上传完成事件
     */
    void createMaterialFile(FileUploadEvent event);

    /**
     * 更新缩略图路径
     * 
     * @param fileId 文件ID
     * @param thumbnailPath 缩略图路径
     * @return 是否更新成功
     */
    boolean updateThumbnailPath(String fileId, String thumbnailPath);

    /**
     * 更新元数据ID
     * 
     * @param fileId 文件ID
     * @param metadataId 元数据ID
     * @return 是否更新成功
     */
    boolean updateMetadataId(String fileId, String metadataId);

    /**
     * 增加引用计数
     * 
     * @param fileId 文件ID
     * @return 是否更新成功
     */
    boolean incrementRefCount(String fileId);

    /**
     * 减少引用计数
     * 
     * @param fileId 文件ID
     * @return 是否更新成功
     */
    boolean decrementRefCount(String fileId);

    /**
     * 检查文件是否存在
     * 
     * @param fileId 文件ID
     * @return 是否存在
     */
    boolean existsFile(String fileId);

    /**
     * 根据MD5查找文件
     * 
     * @param md5Hash MD5哈希值
     * @return 文件ID
     */
    String findFileByMd5(String md5Hash);
}