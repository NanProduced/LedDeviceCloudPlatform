package org.nan.cloud.core.service;

import org.nan.cloud.core.event.mq.FileUploadEvent;

/**
 * 素材文件服务接口
 * 
 * 负责管理文件实体信息，与Material业务信息分离
 * MaterialFile关注文件的技术属性，Material关注业务属性
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface MaterialFileService {

    /**
     * 创建文件记录（文件上传完成后调用）
     * 
     * @param event 文件上传完成事件
     * @return 是否创建成功
     */
    boolean createMaterialFile(FileUploadEvent event);

    /**
     * 更新文件的缩略图路径
     * 
     * @param fileId 文件ID
     * @param thumbnailPath 缩略图路径
     * @return 是否更新成功
     */
    boolean updateThumbnailPath(String fileId, String thumbnailPath);

    /**
     * 更新文件的元数据ID
     * 
     * @param fileId 文件ID
     * @param metadataId MongoDB元数据ID
     * @return 是否更新成功
     */
    boolean updateMetadataId(String fileId, String metadataId);

    /**
     * 增加文件引用计数
     * 
     * @param fileId 文件ID
     * @return 是否更新成功
     */
    boolean incrementRefCount(String fileId);

    /**
     * 减少文件引用计数
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
     * 根据MD5查找已存在的文件（用于去重）
     * 
     * @param md5Hash MD5哈希值
     * @return 文件ID，如果不存在返回null
     */
    String findFileByMd5(String md5Hash);
}