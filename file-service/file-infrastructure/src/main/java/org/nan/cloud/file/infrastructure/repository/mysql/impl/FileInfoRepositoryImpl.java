package org.nan.cloud.file.infrastructure.repository.mysql.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.file.application.domain.FileInfo;
import org.nan.cloud.file.application.repository.FileInfoRepository;
import org.nan.cloud.file.infrastructure.repository.mysql.DO.MaterialFileDO;
import org.nan.cloud.file.infrastructure.repository.mysql.mapper.MaterialFileMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 文件信息数据访问实现
 * 
 * 基于MyBatis Plus的数据访问层实现，负责：
 * 1. FileInfo领域模型与MaterialFileDO的双向转换
 * 2. 数据库CRUD操作的封装
 * 3. 复杂查询逻辑的实现
 * 4. 事务管理和异常处理
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class FileInfoRepositoryImpl implements FileInfoRepository {

    private final MaterialFileMapper materialFileMapper;

    @Override
    public FileInfo save(FileInfo fileInfo) {
        log.debug("保存文件信息 - 文件ID: {}", fileInfo.getFileId());
        
        try {
            // 转换为DO对象
            MaterialFileDO materialFileDO = convertToMaterialFileDO(fileInfo);
            
            // 判断是插入还是更新
            MaterialFileDO existing = materialFileMapper.selectById(fileInfo.getFileId());
            if (existing == null) {
                // 检查MD5是否已存在（处理去重逻辑）
                MaterialFileDO existingByMd5 = null;
                if (fileInfo.getMd5Hash() != null && !fileInfo.getMd5Hash().trim().isEmpty()) {
                    existingByMd5 = materialFileMapper.findByMd5Hash(fileInfo.getMd5Hash());
                }
                
                if (existingByMd5 != null) {
                    // MD5已存在，增加引用计数而不是插入新记录
                    existingByMd5.setRefCount(existingByMd5.getRefCount() + 1);
                    existingByMd5.setUpdateTime(LocalDateTime.now());
                    int result = materialFileMapper.updateById(existingByMd5);
                    if (result <= 0) {
                        throw new RuntimeException("更新文件引用计数失败");
                    }
                    log.info("文件MD5重复，增加引用计数 - MD5: {}, 原文件ID: {}, 引用计数: {}", 
                            fileInfo.getMd5Hash(), existingByMd5.getFileId(), existingByMd5.getRefCount());
                    
                    // 返回现有文件信息，但使用新的文件ID
                    FileInfo resultFile = convertToFileInfo(existingByMd5);
                    resultFile.setFileId(fileInfo.getFileId()); // 保持原请求的文件ID
                    return resultFile;
                } else {
                    // 插入新记录
                    materialFileDO.setCreateTime(LocalDateTime.now());
                    materialFileDO.setUpdateTime(LocalDateTime.now());
                    int result = materialFileMapper.insert(materialFileDO);
                    if (result <= 0) {
                        throw new RuntimeException("插入文件信息失败");
                    }
                    log.debug("文件信息插入成功 - 文件ID: {}", fileInfo.getFileId());
                }
            } else {
                // 更新现有记录
                materialFileDO.setUpdateTime(LocalDateTime.now());
                int result = materialFileMapper.updateById(materialFileDO);
                if (result <= 0) {
                    throw new RuntimeException("更新文件信息失败");
                }
                log.debug("文件信息更新成功 - 文件ID: {}", fileInfo.getFileId());
            }
            
            return convertToFileInfo(materialFileDO);
            
        } catch (Exception e) {
            log.error("保存文件信息失败 - 文件ID: {}, 错误: {}", fileInfo.getFileId(), e.getMessage(), e);
            throw new RuntimeException("保存文件信息失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<FileInfo> findByFileId(String fileId) {
        log.debug("根据文件ID查找文件 - 文件ID: {}", fileId);
        
        try {
            MaterialFileDO materialFileDO = materialFileMapper.selectById(fileId);
            if (materialFileDO == null) {
                log.debug("文件不存在 - 文件ID: {}", fileId);
                return Optional.empty();
            }
            
            FileInfo fileInfo = convertToFileInfo(materialFileDO);
            log.debug("文件查找成功 - 文件ID: {}, 文件名: {}", fileId, fileInfo.getOriginalFilename());
            return Optional.of(fileInfo);
            
        } catch (Exception e) {
            log.error("根据文件ID查找文件失败 - 文件ID: {}, 错误: {}", fileId, e.getMessage(), e);
            throw new RuntimeException("查找文件失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateFileMetadata(String fileId, String metadataId) {
        MaterialFileDO materialFileDO = new MaterialFileDO();
        materialFileDO.setFileId(fileId);
        materialFileDO.setMetadataId(metadataId);
        materialFileMapper.updateById(materialFileDO);
    }

    @Override
    public void updateFileThumbnail(String fileId, String thumbnailPath) {
        MaterialFileDO materialFileDO = new MaterialFileDO();
        materialFileDO.setFileId(fileId);
        materialFileDO.setThumbnailPath(thumbnailPath);
        materialFileMapper.updateById(materialFileDO);
    }

    @Override
    public Optional<FileInfo> findByMd5Hash(String md5Hash) {
        log.debug("根据MD5查找文件 - MD5: {}", md5Hash);
        
        try {
            MaterialFileDO materialFileDO = materialFileMapper.findByMd5Hash(md5Hash);
            if (materialFileDO == null) {
                log.debug("MD5对应的文件不存在 - MD5: {}", md5Hash);
                return Optional.empty();
            }
            
            FileInfo fileInfo = convertToFileInfo(materialFileDO);
            log.debug("MD5文件查找成功 - MD5: {}, 文件ID: {}", md5Hash, fileInfo.getFileId());
            return Optional.of(fileInfo);
            
        } catch (Exception e) {
            log.error("根据MD5查找文件失败 - MD5: {}, 错误: {}", md5Hash, e.getMessage(), e);
            throw new RuntimeException("查找文件失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<FileInfo> findByMd5HashAndOrganizationId(String md5Hash, String organizationId) {
        log.debug("根据MD5和组织ID查找文件 - MD5: {}, 组织ID: {}", md5Hash, organizationId);
        
        try {
            Long orgId = Long.valueOf(organizationId);
            MaterialFileDO materialFileDO = materialFileMapper.findByMd5HashAndOrganizationId(md5Hash, orgId);
            if (materialFileDO == null) {
                log.debug("MD5和组织ID对应的文件不存在 - MD5: {}, 组织ID: {}", md5Hash, organizationId);
                return Optional.empty();
            }
            
            FileInfo fileInfo = convertToFileInfo(materialFileDO);
            log.debug("MD5和组织ID文件查找成功 - MD5: {}, 组织ID: {}, 文件ID: {}", 
                    md5Hash, organizationId, fileInfo.getFileId());
            return Optional.of(fileInfo);
            
        } catch (NumberFormatException e) {
            log.error("组织ID格式错误 - 组织ID: {}", organizationId);
            throw new IllegalArgumentException("组织ID格式错误: " + organizationId);
        } catch (Exception e) {
            log.error("根据MD5和组织ID查找文件失败 - MD5: {}, 组织ID: {}, 错误: {}", 
                    md5Hash, organizationId, e.getMessage(), e);
            throw new RuntimeException("查找文件失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<FileInfo> findByOrganizationId(String organizationId, int page, int size) {
        log.debug("分页查询组织文件 - 组织ID: {}, 页码: {}, 大小: {}", organizationId, page, size);
        
        try {
            Long orgId = Long.valueOf(organizationId);
            int offset = page * size;
            
            // 通过material表关联查询
            LambdaQueryWrapper<MaterialFileDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.last("LIMIT " + offset + ", " + size);
            
            List<MaterialFileDO> materialFileDOs = materialFileMapper.selectList(queryWrapper);
            List<FileInfo> fileInfos = materialFileDOs.stream()
                    .map(this::convertToFileInfo)
                    .collect(Collectors.toList());
            
            log.debug("组织文件分页查询完成 - 组织ID: {}, 返回数量: {}", organizationId, fileInfos.size());
            return fileInfos;
            
        } catch (Exception e) {
            log.error("分页查询组织文件失败 - 组织ID: {}, 错误: {}", organizationId, e.getMessage(), e);
            throw new RuntimeException("查询文件失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<FileInfo> findByFolderId(String folderId, int page, int size) {
        // 此方法需要与material表关联查询，暂时返回空列表
        log.debug("根据文件夹ID查询文件 - 文件夹ID: {}, 页码: {}, 大小: {}", folderId, page, size);
        return List.of();
    }

    @Override
    public List<FileInfo> findByFileTypeAndOrganizationId(String fileType, String organizationId, int page, int size) {
        // 此方法需要与material表关联查询，暂时返回空列表
        log.debug("根据文件类型和组织ID查询文件 - 文件类型: {}, 组织ID: {}, 页码: {}, 大小: {}", 
                fileType, organizationId, page, size);
        return List.of();
    }

    @Override
    public List<FileInfo> searchFiles(String keyword, String organizationId, int page, int size) {
        // 此方法需要与material表关联查询，暂时返回空列表
        log.debug("搜索文件 - 关键词: {}, 组织ID: {}, 页码: {}, 大小: {}", keyword, organizationId, page, size);
        return List.of();
    }

    @Override
    public long countByOrganizationId(String organizationId) {
        log.debug("统计组织文件数量 - 组织ID: {}", organizationId);
        
        try {
            Long orgId = Long.valueOf(organizationId);
            long count = materialFileMapper.countByOrganizationId(orgId);
            log.debug("组织文件数量统计完成 - 组织ID: {}, 数量: {}", organizationId, count);
            return count;
            
        } catch (Exception e) {
            log.error("统计组织文件数量失败 - 组织ID: {}, 错误: {}", organizationId, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public List<FileTypeCount> countByFileTypeAndOrganizationId(String organizationId) {
        // 此方法需要复杂的统计查询，暂时返回空列表
        log.debug("统计组织各类型文件数量 - 组织ID: {}", organizationId);
        return List.of();
    }

    @Override
    public long sumFileSizeByOrganizationId(String organizationId) {
        log.debug("计算组织文件总大小 - 组织ID: {}", organizationId);
        
        try {
            Long orgId = Long.valueOf(organizationId);
            long totalSize = materialFileMapper.sumFileSizeByOrganizationId(orgId);
            log.debug("组织文件总大小计算完成 - 组织ID: {}, 总大小: {} 字节", organizationId, totalSize);
            return totalSize;
            
        } catch (Exception e) {
            log.error("计算组织文件总大小失败 - 组织ID: {}, 错误: {}", organizationId, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public List<FileInfo> findTemporaryFilesBefore(LocalDateTime beforeTime) {
        log.debug("查找临时文件 - 时间阈值: {}", beforeTime);
        
        try {
            int days = 7; // 默认7天前的文件
            List<MaterialFileDO> materialFileDOs = materialFileMapper.findTemporaryFiles(days);
            List<FileInfo> fileInfos = materialFileDOs.stream()
                    .map(this::convertToFileInfo)
                    .collect(Collectors.toList());
            
            log.debug("查找临时文件完成 - 数量: {}", fileInfos.size());
            return fileInfos;
            
        } catch (Exception e) {
            log.error("查找临时文件失败 - 时间阈值: {}, 错误: {}", beforeTime, e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public List<FileInfo> findByStatus(FileInfo.FileStatus status, int page, int size) {
        log.debug("根据状态查询文件 - 状态: {}, 页码: {}, 大小: {}", status, page, size);
        
        try {
            int offset = page * size;
            Integer statusValue = mapFileStatusToInteger(status);
            List<MaterialFileDO> materialFileDOs = materialFileMapper.findByStatus(statusValue, offset, size);
            List<FileInfo> fileInfos = materialFileDOs.stream()
                    .map(this::convertToFileInfo)
                    .collect(Collectors.toList());
            
            log.debug("根据状态查询文件完成 - 状态: {}, 数量: {}", status, fileInfos.size());
            return fileInfos;
            
        } catch (Exception e) {
            log.error("根据状态查询文件失败 - 状态: {}, 错误: {}", status, e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public int updateStatusBatch(List<String> fileIds, FileInfo.FileStatus newStatus) {
        log.debug("批量更新文件状态 - 文件数量: {}, 新状态: {}", fileIds.size(), newStatus);
        
        try {
            Integer statusValue = mapFileStatusToInteger(newStatus);
            int updateCount = materialFileMapper.updateStatusBatch(fileIds, statusValue);
            log.debug("批量更新文件状态完成 - 更新数量: {}", updateCount);
            return updateCount;
            
        } catch (Exception e) {
            log.error("批量更新文件状态失败 - 文件数量: {}, 错误: {}", fileIds.size(), e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public boolean deleteByFileId(String fileId) {
        log.debug("软删除文件 - 文件ID: {}", fileId);
        
        try {
            LambdaUpdateWrapper<MaterialFileDO> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(MaterialFileDO::getFileId, fileId)
                        .set(MaterialFileDO::getDeleted, 1)
                        .set(MaterialFileDO::getUpdateTime, LocalDateTime.now());
            
            int result = materialFileMapper.update(null, updateWrapper);
            boolean success = result > 0;
            log.debug("软删除文件结果 - 文件ID: {}, 成功: {}", fileId, success);
            return success;
            
        } catch (Exception e) {
            log.error("软删除文件失败 - 文件ID: {}, 错误: {}", fileId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public int deleteBatchByFileIds(List<String> fileIds) {
        log.debug("批量软删除文件 - 文件数量: {}", fileIds.size());
        
        try {
            LambdaUpdateWrapper<MaterialFileDO> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.in(MaterialFileDO::getFileId, fileIds)
                        .set(MaterialFileDO::getDeleted, 1)
                        .set(MaterialFileDO::getUpdateTime, LocalDateTime.now());
            
            int deleteCount = materialFileMapper.update(null, updateWrapper);
            log.debug("批量软删除文件完成 - 删除数量: {}", deleteCount);
            return deleteCount;
            
        } catch (Exception e) {
            log.error("批量软删除文件失败 - 文件数量: {}, 错误: {}", fileIds.size(), e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public boolean physicalDeleteByFileId(String fileId) {
        log.debug("物理删除文件 - 文件ID: {}", fileId);
        
        try {
            int result = materialFileMapper.deleteById(fileId);
            boolean success = result > 0;
            log.debug("物理删除文件结果 - 文件ID: {}, 成功: {}", fileId, success);
            return success;
            
        } catch (Exception e) {
            log.error("物理删除文件失败 - 文件ID: {}, 错误: {}", fileId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean updateAccessStatistics(String fileId, long accessCount, LocalDateTime lastAccessTime) {
        // 当前表结构没有访问统计字段，暂时返回true
        log.debug("更新文件访问统计 - 文件ID: {}, 访问次数: {}, 最后访问时间: {}", 
                fileId, accessCount, lastAccessTime);
        return true;
    }

    @Override
    public List<FileInfo> findRecentUploads(String organizationId, int limit) {
        log.debug("查找最近上传的文件 - 组织ID: {}, 限制数量: {}", organizationId, limit);
        
        try {
            Long orgId = Long.valueOf(organizationId);
            List<MaterialFileDO> materialFileDOs = materialFileMapper.findRecentUploads(orgId, limit);
            List<FileInfo> fileInfos = materialFileDOs.stream()
                    .map(this::convertToFileInfo)
                    .collect(Collectors.toList());
            
            log.debug("查找最近上传的文件完成 - 组织ID: {}, 数量: {}", organizationId, fileInfos.size());
            return fileInfos;
            
        } catch (Exception e) {
            log.error("查找最近上传的文件失败 - 组织ID: {}, 错误: {}", organizationId, e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public List<FileInfo> findMostPopular(String organizationId, int limit) {
        // 当前表结构没有访问统计字段，暂时返回空列表
        log.debug("查找最热门的文件 - 组织ID: {}, 限制数量: {}", organizationId, limit);
        return List.of();
    }

    // 私有辅助方法

    /**
     * 将FileInfo转换为MaterialFileDO
     */
    private MaterialFileDO convertToMaterialFileDO(FileInfo fileInfo) {
        return MaterialFileDO.builder()
                .fileId(fileInfo.getFileId())
                .md5Hash(fileInfo.getMd5Hash())
                .originalFileSize(fileInfo.getFileSize())
                .mimeType(fileInfo.getMimeType())
                .fileExtension(fileInfo.getFileExtension())
                .storageType(fileInfo.getStorageType())
                .storagePath(fileInfo.getStoragePath())
                .uploadTime(fileInfo.getUploadTime())
                .refCount(fileInfo.getRefCount())
                .fileStatus(fileInfo.getFileStatus())
                .thumbnailPath(fileInfo.getThumbnailPath())
                .metadataId(fileInfo.getMetaDataId())
                .updateTime(fileInfo.getUpdateTime())
                .build();
    }

    /**
     * 将MaterialFileDO转换为FileInfo
     */
    private FileInfo convertToFileInfo(MaterialFileDO materialFileDO) {
        return FileInfo.builder()
                .fileId(materialFileDO.getFileId())
                .md5Hash(materialFileDO.getMd5Hash())
                .fileSize(materialFileDO.getOriginalFileSize())
                .mimeType(materialFileDO.getMimeType())
                .fileExtension(materialFileDO.getFileExtension())
                .storageType(materialFileDO.getStorageType())
                .storagePath(materialFileDO.getStoragePath())
                .uploadTime(materialFileDO.getUploadTime())
                .updateTime(materialFileDO.getUpdateTime())
                .refCount(materialFileDO.getRefCount())
                .fileStatus(materialFileDO.getFileStatus())
                .thumbnailPath(materialFileDO.getThumbnailPath())
                .metaDataId(materialFileDO.getMetadataId())
                .build();
    }

    /**
     * 将FileStatus枚举转换为数据库整数值
     */
    private Integer mapFileStatusToInteger(FileInfo.FileStatus status) {
        switch (status) {
            case UPLOADING:
                return 0;
            case UPLOADED:
            case COMPLETED:
                return 1;
            case PROCESSING:
                return 2;
            case FAILED:
                return 3;
            case DELETED:
                return 4;
            default:
                return 1; // 默认为已完成
        }
    }
}