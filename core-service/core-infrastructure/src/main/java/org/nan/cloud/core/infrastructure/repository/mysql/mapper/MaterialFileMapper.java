package org.nan.cloud.core.infrastructure.repository.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.MaterialFileDO;

import java.time.LocalDateTime;

/**
 * 素材文件Mapper
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Mapper
public interface MaterialFileMapper extends BaseMapper<MaterialFileDO> {

    /**
     * 更新缩略图路径
     */
    @Update("UPDATE material_file SET thumbnail_path = #{thumbnailPath}, update_time = #{updateTime} WHERE file_id = #{fileId}")
    int updateThumbnailPath(@Param("fileId") String fileId, 
                           @Param("thumbnailPath") String thumbnailPath, 
                           @Param("updateTime") LocalDateTime updateTime);

    /**
     * 更新元数据ID
     */
    @Update("UPDATE material_file SET meta_data_id = #{metadataId}, update_time = #{updateTime} WHERE file_id = #{fileId}")
    int updateMetadataId(@Param("fileId") String fileId, 
                        @Param("metadataId") String metadataId, 
                        @Param("updateTime") LocalDateTime updateTime);

    /**
     * 增加引用计数
     */
    @Update("UPDATE material_file SET ref_count = ref_count + 1, update_time = #{updateTime} WHERE file_id = #{fileId}")
    int incrementRefCount(@Param("fileId") String fileId, 
                         @Param("updateTime") LocalDateTime updateTime);

    /**
     * 减少引用计数
     */
    @Update("UPDATE material_file SET ref_count = ref_count - 1, update_time = #{updateTime} WHERE file_id = #{fileId} AND ref_count > 0")
    int decrementRefCount(@Param("fileId") String fileId, 
                         @Param("updateTime") LocalDateTime updateTime);

    /**
     * 统计文件数量
     */
    @Select("SELECT COUNT(*) FROM material_file WHERE file_id = #{fileId}")
    Long countByFileId(@Param("fileId") String fileId);

    /**
     * 根据MD5查找文件ID
     */
    @Select("SELECT file_id FROM material_file WHERE md5_hash = #{md5Hash} LIMIT 1")
    String findFileIdByMd5(@Param("md5Hash") String md5Hash);
}