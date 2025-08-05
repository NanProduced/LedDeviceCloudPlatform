package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.core.event.mq.FileUploadEvent;
import org.nan.cloud.core.repository.MaterialFileRepository;
import org.nan.cloud.core.service.MaterialFileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 素材文件服务实现
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialFileServiceImpl implements MaterialFileService {

    private final MaterialFileRepository materialFileRepository;

    @Override
    @Transactional
    public boolean createMaterialFile(FileUploadEvent event) {
        try {
            // 检查文件是否已存在（避免重复创建）
            if (existsFile(event.getFileId())) {
                log.warn("文件已存在，跳过创建 - 文件ID: {}", event.getFileId());
                return true;
            }

            materialFileRepository.createMaterialFile(event);
            log.info("素材文件记录创建成功 - 文件ID: {}, 大小: {}, 类型: {}", 
                    event.getFileId(), event.getFileSize(), event.getMimeType());
            return true;
            
        } catch (Exception e) {
            log.error("创建素材文件记录失败 - 文件ID: {}, 错误: {}", event.getFileId(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean updateThumbnailPath(String fileId, String thumbnailPath) {
        try {
            boolean success = materialFileRepository.updateThumbnailPath(fileId, thumbnailPath);
            if (success) {
                log.info("缩略图路径更新成功 - 文件ID: {}, 路径: {}", fileId, thumbnailPath);
            } else {
                log.warn("缩略图路径更新失败 - 文件ID: {}", fileId);
            }
            return success;
            
        } catch (Exception e) {
            log.error("更新缩略图路径失败 - 文件ID: {}, 错误: {}", fileId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean updateMetadataId(String fileId, String metadataId) {
        try {
            boolean success = materialFileRepository.updateMetadataId(fileId, metadataId);
            if (success) {
                log.info("元数据ID更新成功 - 文件ID: {}, 元数据ID: {}", fileId, metadataId);
            } else {
                log.warn("元数据ID更新失败 - 文件ID: {}", fileId);
            }
            return success;
            
        } catch (Exception e) {
            log.error("更新元数据ID失败 - 文件ID: {}, 错误: {}", fileId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean incrementRefCount(String fileId) {
        try {
            boolean success = materialFileRepository.incrementRefCount(fileId);
            if (success) {
                log.debug("文件引用计数增加成功 - 文件ID: {}", fileId);
            }
            return success;
            
        } catch (Exception e) {
            log.error("增加文件引用计数失败 - 文件ID: {}, 错误: {}", fileId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean decrementRefCount(String fileId) {
        try {
            boolean success = materialFileRepository.decrementRefCount(fileId);
            if (success) {
                log.debug("文件引用计数减少成功 - 文件ID: {}", fileId);
            }
            return success;
            
        } catch (Exception e) {
            log.error("减少文件引用计数失败 - 文件ID: {}, 错误: {}", fileId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean existsFile(String fileId) {
        try {
            return materialFileRepository.existsFile(fileId);
        } catch (Exception e) {
            log.error("检查文件存在性失败 - 文件ID: {}, 错误: {}", fileId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String findFileByMd5(String md5Hash) {
        try {
            return materialFileRepository.findFileByMd5(md5Hash);
        } catch (Exception e) {
            log.error("通过MD5查找文件失败 - MD5: {}, 错误: {}", md5Hash, e.getMessage(), e);
            return null;
        }
    }
}