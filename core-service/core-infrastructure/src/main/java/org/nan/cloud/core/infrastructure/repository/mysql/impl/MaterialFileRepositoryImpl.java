package org.nan.cloud.core.infrastructure.repository.mysql.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.core.event.mq.FileUploadEvent;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.MaterialFileDO;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.MaterialFileMapper;
import org.nan.cloud.core.repository.MaterialFileRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 素材文件存储库实现
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MaterialFileRepositoryImpl implements MaterialFileRepository {

    private final MaterialFileMapper materialFileMapper;

    @Override
    public void createMaterialFile(FileUploadEvent event) {
        MaterialFileDO materialFile = MaterialFileDO.builder()
                .fileId(event.getFileId())
                .md5Hash(event.getMd5Hash())
                .originalFileSize(event.getFileSize())
                .mimeType(event.getMimeType())
                .fileExtension(getFileExtension(event.getOriginalFilename()))
                .storageType("LOCAL") // 默认本地存储，可以从配置或事件中获取
                .storagePath(event.getStoragePath())
                .uploadTime(LocalDateTime.now())
                .refCount(1L) // 初始引用计数为1
                .fileStatus(1) // 1表示已完成
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        materialFileMapper.insert(materialFile);
        log.debug("MaterialFile记录创建成功 - 文件ID: {}", event.getFileId());
    }

    @Override
    public boolean updateThumbnailPath(String fileId, String thumbnailPath) {
        int affected = materialFileMapper.updateThumbnailPath(fileId, thumbnailPath, LocalDateTime.now());
        return affected > 0;
    }

    @Override
    public boolean updateMetadataId(String fileId, String metadataId) {
        int affected = materialFileMapper.updateMetadataId(fileId, metadataId, LocalDateTime.now());
        return affected > 0;
    }

    @Override
    public boolean incrementRefCount(String fileId) {
        int affected = materialFileMapper.incrementRefCount(fileId, LocalDateTime.now());
        return affected > 0;
    }

    @Override
    public boolean decrementRefCount(String fileId) {
        int affected = materialFileMapper.decrementRefCount(fileId, LocalDateTime.now());
        return affected > 0;
    }

    @Override
    public boolean existsFile(String fileId) {
        Long count = materialFileMapper.countByFileId(fileId);
        return count != null && count > 0;
    }

    @Override
    public String findFileByMd5(String md5Hash) {
        return materialFileMapper.findFileIdByMd5(md5Hash);
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex + 1) : "";
    }
}