package org.nan.cloud.file.application.repository;

import org.nan.cloud.file.application.domain.FileInfo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 文件信息数据访问接口
 * 
 * 定义文件信息的数据持久化操作
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface FileInfoRepository {

    /**
     * 保存文件信息
     * 
     * @param fileInfo 文件信息
     * @return 保存后的文件信息
     */
    FileInfo save(FileInfo fileInfo);

    /**
     * 根据文件ID查找文件
     * 
     * @param fileId 文件ID
     * @return 文件信息，如果不存在返回空
     */
    Optional<FileInfo> findByFileId(String fileId);

    /**
     * 根据MD5和组织ID查找文件（用于去重）
     * 
     * @param md5Hash MD5哈希值
     * @param organizationId 组织ID
     * @return 文件信息，如果不存在返回空
     */
    Optional<FileInfo> findByMd5HashAndOrganizationId(String md5Hash, String organizationId);

    /**
     * 根据组织ID分页查询文件列表
     * 
     * @param organizationId 组织ID
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 文件列表
     */
    List<FileInfo> findByOrganizationId(String organizationId, int page, int size);

    /**
     * 根据文件夹ID查询文件列表
     * 
     * @param folderId 文件夹ID
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 文件列表
     */
    List<FileInfo> findByFolderId(String folderId, int page, int size);

    /**
     * 根据文件类型查询文件列表
     * 
     * @param fileType 文件类型
     * @param organizationId 组织ID
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 文件列表
     */
    List<FileInfo> findByFileTypeAndOrganizationId(String fileType, String organizationId, int page, int size);

    /**
     * 搜索文件（根据文件名、描述、标签）
     * 
     * @param keyword 关键词
     * @param organizationId 组织ID
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 文件列表
     */
    List<FileInfo> searchFiles(String keyword, String organizationId, int page, int size);

    /**
     * 统计组织的文件数量
     * 
     * @param organizationId 组织ID
     * @return 文件数量
     */
    long countByOrganizationId(String organizationId);

    /**
     * 统计组织各类型文件数量
     * 
     * @param organizationId 组织ID
     * @return 各类型文件数量映射
     */
    List<FileTypeCount> countByFileTypeAndOrganizationId(String organizationId);

    /**
     * 计算组织文件总大小
     * 
     * @param organizationId 组织ID
     * @return 总文件大小（字节）
     */
    long sumFileSizeByOrganizationId(String organizationId);

    /**
     * 查找需要清理的临时文件
     * 
     * @param beforeTime 时间阈值
     * @return 需要清理的文件列表
     */
    List<FileInfo> findTemporaryFilesBefore(LocalDateTime beforeTime);

    /**
     * 查找指定状态的文件
     * 
     * @param status 文件状态
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 文件列表
     */
    List<FileInfo> findByStatus(FileInfo.FileStatus status, int page, int size);

    /**
     * 批量更新文件状态
     * 
     * @param fileIds 文件ID列表
     * @param newStatus 新状态
     * @return 更新的文件数量
     */
    int updateStatusBatch(List<String> fileIds, FileInfo.FileStatus newStatus);

    /**
     * 软删除文件（更新状态为已删除）
     * 
     * @param fileId 文件ID
     * @return 是否删除成功
     */
    boolean deleteByFileId(String fileId);

    /**
     * 批量软删除文件
     * 
     * @param fileIds 文件ID列表
     * @return 删除的文件数量
     */
    int deleteBatchByFileIds(List<String> fileIds);

    /**
     * 物理删除文件记录
     * 
     * @param fileId 文件ID
     * @return 是否删除成功
     */
    boolean physicalDeleteByFileId(String fileId);

    /**
     * 更新文件访问统计
     * 
     * @param fileId 文件ID
     * @param accessCount 访问次数
     * @param lastAccessTime 最后访问时间
     * @return 是否更新成功
     */
    boolean updateAccessStatistics(String fileId, long accessCount, LocalDateTime lastAccessTime);

    /**
     * 查找最近上传的文件
     * 
     * @param organizationId 组织ID
     * @param limit 限制数量
     * @return 最近上传的文件列表
     */
    List<FileInfo> findRecentUploads(String organizationId, int limit);

    /**
     * 查找最热门的文件（按访问次数）
     * 
     * @param organizationId 组织ID
     * @param limit 限制数量
     * @return 最热门的文件列表
     */
    List<FileInfo> findMostPopular(String organizationId, int limit);

    /**
     * 文件类型统计结果
     */
    class FileTypeCount {
        private String fileType;
        private long count;

        public FileTypeCount() {}

        public FileTypeCount(String fileType, long count) {
            this.fileType = fileType;
            this.count = count;
        }

        public String getFileType() {
            return fileType;
        }

        public void setFileType(String fileType) {
            this.fileType = fileType;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }
}