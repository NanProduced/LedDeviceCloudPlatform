package org.nan.cloud.file.infrastructure.repository.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.nan.cloud.file.infrastructure.repository.mysql.DO.MaterialFileDO;

import java.util.List;

/**
 * 文件信息Mapper接口
 * 
 * 基于MyBatis Plus的数据访问层，提供基础CRUD操作和自定义查询
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Mapper
public interface MaterialFileMapper extends BaseMapper<MaterialFileDO> {

    /**
     * 根据MD5查找文件（不限制组织，用于秒传检测）
     * 
     * @param md5Hash MD5哈希值
     * @return 文件信息，如果不存在返回null
     */
    @Select("SELECT * FROM material_file WHERE md5_hash = #{md5Hash} AND deleted = 0 LIMIT 1")
    MaterialFileDO findByMd5Hash(String md5Hash);

    /**
     * 根据MD5和组织ID查找文件（用于去重）
     * 
     * @param md5Hash MD5哈希值
     * @param organizationId 组织ID
     * @return 文件信息，如果不存在返回null
     */
    @Select("SELECT mf.* FROM material_file mf " +
            "INNER JOIN material m ON mf.file_id = m.file_id " +
            "WHERE mf.md5_hash = #{md5Hash} AND m.oid = #{organizationId} " +
            "AND mf.deleted = 0 AND m.deleted = 0 " +
            "LIMIT 1")
    MaterialFileDO findByMd5HashAndOrganizationId(String md5Hash, Long organizationId);

    /**
     * 统计组织的文件数量
     * 
     * @param organizationId 组织ID
     * @return 文件数量
     */
    @Select("SELECT COUNT(*) FROM material_file mf " +
            "INNER JOIN material m ON mf.file_id = m.file_id " +
            "WHERE m.oid = #{organizationId} AND mf.deleted = 0 AND m.deleted = 0")
    long countByOrganizationId(Long organizationId);

    /**
     * 计算组织文件总大小
     * 
     * @param organizationId 组织ID
     * @return 总文件大小（字节）
     */
    @Select("SELECT COALESCE(SUM(mf.original_file_size), 0) FROM material_file mf " +
            "INNER JOIN material m ON mf.file_id = m.file_id " +
            "WHERE m.oid = #{organizationId} AND mf.deleted = 0 AND m.deleted = 0")
    long sumFileSizeByOrganizationId(Long organizationId);

    /**
     * 查找指定状态的文件
     * 
     * @param status 文件状态
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 文件列表
     */
    @Select("SELECT * FROM material_file " +
            "WHERE file_status = #{status} AND deleted = 0 " +
            "ORDER BY upload_time DESC " +
            "LIMIT #{offset}, #{limit}")
    List<MaterialFileDO> findByStatus(Integer status, int offset, int limit);

    /**
     * 查找最近上传的文件
     * 
     * @param organizationId 组织ID
     * @param limit 限制数量
     * @return 最近上传的文件列表
     */
    @Select("SELECT mf.* FROM material_file mf " +
            "INNER JOIN material m ON mf.file_id = m.file_id " +
            "WHERE m.oid = #{organizationId} AND mf.deleted = 0 AND m.deleted = 0 " +
            "ORDER BY mf.upload_time DESC " +
            "LIMIT #{limit}")
    List<MaterialFileDO> findRecentUploads(Long organizationId, int limit);

    /**
     * 批量更新文件状态
     * 
     * @param fileIds 文件ID列表
     * @param newStatus 新状态
     * @return 更新的文件数量
     */
    @Select("<script>" +
            "UPDATE material_file SET file_status = #{newStatus}, update_time = NOW() " +
            "WHERE file_id IN " +
            "<foreach collection='fileIds' item='fileId' open='(' separator=',' close=')'>" +
            "#{fileId}" +
            "</foreach>" +
            "AND deleted = 0" +
            "</script>")
    int updateStatusBatch(List<String> fileIds, Integer newStatus);

    /**
     * 查找需要清理的临时文件（超过指定天数未被引用的文件）
     * 
     * @param days 天数
     * @return 需要清理的文件列表
     */
    @Select("SELECT * FROM material_file " +
            "WHERE ref_count = 0 " +
            "AND upload_time < DATE_SUB(NOW(), INTERVAL #{days} DAY) " +
            "AND deleted = 0 " +
            "ORDER BY upload_time ASC")
    List<MaterialFileDO> findTemporaryFiles(int days);
}