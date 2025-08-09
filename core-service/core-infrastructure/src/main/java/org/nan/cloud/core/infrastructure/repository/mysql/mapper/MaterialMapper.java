package org.nan.cloud.core.infrastructure.repository.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.MaterialDO;

import java.util.List;

@Mapper
public interface MaterialMapper extends BaseMapper<MaterialDO> {

    /**
     * 连表查询素材和文件详情
     */
    @Select("""
            SELECT m.*, f.md5_hash, f.original_file_size, f.mime_type, f.file_extension,
                   f.storage_type, f.storage_path, f.upload_time, f.ref_count, f.file_status, f.meta_data_id
            FROM material m 
            LEFT JOIN material_file f ON m.file_id = f.file_id
            WHERE m.mid = #{mid}
            """)
    MaterialDO selectMaterialWithFileById(@Param("mid") Long mid);

    /**
     * 查询用户组下的素材（包含文件信息）
     */
    @Select("""
            SELECT m.*, f.md5_hash, f.original_file_size, f.mime_type, f.file_extension,
                   f.storage_type, f.storage_path, f.upload_time, f.ref_count, f.file_status, f.meta_data_id
            FROM material m 
            LEFT JOIN material_file f ON m.file_id = f.file_id
            WHERE m.oid = #{oid} AND m.ugid = #{ugid}
            ${fidCondition}
            ORDER BY m.create_time DESC
            """)
    List<MaterialDO> selectMaterialsByUserGroup(@Param("oid") Long oid, 
                                               @Param("ugid") Long ugid, 
                                               @Param("fidCondition") String fidCondition);

    /**
     * 查询公共资源组下的素材（包含文件信息）
     */
    @Select("""
            SELECT m.*, f.md5_hash, f.original_file_size, f.mime_type, f.file_extension,
                   f.storage_type, f.storage_path, f.upload_time, f.ref_count, f.file_status, f.meta_data_id
            FROM material m 
            LEFT JOIN material_file f ON m.file_id = f.file_id
            WHERE m.oid = #{oid} AND m.ugid IS NULL
            ${fidCondition}
            ORDER BY m.create_time DESC
            """)
    List<MaterialDO> selectPublicMaterials(@Param("oid") Long oid, 
                                          @Param("fidCondition") String fidCondition);

    /**
     * 查询用户组可见的所有素材（自有+公共）
     */
    @Select("""
            SELECT m.*, f.md5_hash, f.original_file_size, f.mime_type, f.file_extension,
                   f.storage_type, f.storage_path, f.upload_time, f.ref_count, f.file_status, f.meta_data_id
            FROM material m 
            LEFT JOIN material_file f ON m.file_id = f.file_id
            WHERE m.oid = #{oid} AND (m.ugid = #{ugid} OR m.ugid IS NULL)
            ORDER BY m.create_time DESC
            """)
    List<MaterialDO> selectAllVisibleMaterials(@Param("oid") Long oid, @Param("ugid") Long ugid);

    /**
     * 根据文件夹查询素材数量
     */
    @Select("SELECT COUNT(*) FROM material WHERE fid = #{fid}")
    long countByFolder(@Param("fid") Long fid);

    /**
     * 根据用户组查询素材数量
     */
    @Select("SELECT COUNT(*) FROM material WHERE ugid = #{ugid}")
    long countByUserGroup(@Param("ugid") Long ugid);

    /**
     * 查询公共素材数量
     */
    @Select("SELECT COUNT(*) FROM material WHERE oid = #{oid} AND ugid IS NULL")
    long countPublicMaterials(@Param("oid") Long oid);

    /**
     * 根据文件ID查询素材
     */
    @Select("""
            SELECT m.*, f.md5_hash, f.original_file_size, f.mime_type, f.file_extension,
                   f.storage_type, f.storage_path, f.upload_time, f.ref_count, f.file_status, f.meta_data_id
            FROM material m 
            LEFT JOIN material_file f ON m.file_id = f.file_id
            WHERE m.file_id = #{fileId}
            """)
    MaterialDO selectMaterialWithFileByFileId(@Param("fileId") String fileId);

    /**
     * 查询用户组列表下的素材（支持子组）
     * 优化版本：使用IN查询替代单一ugid匹配
     */
    @Select("""
            <script>
            SELECT m.*, f.md5_hash, f.original_file_size, f.mime_type, f.file_extension,
                   f.storage_type, f.storage_path, f.upload_time, f.ref_count, f.file_status, f.meta_data_id
            FROM material m 
            LEFT JOIN material_file f ON m.file_id = f.file_id
            WHERE m.oid = #{oid} 
            <if test="ugidList != null and !ugidList.isEmpty()">
                AND m.ugid IN 
                <foreach collection="ugidList" item="ugid" open="(" separator="," close=")">
                    #{ugid}
                </foreach>
            </if>
            ${fidCondition}
            ORDER BY m.create_time DESC
            </script>
            """)
    List<MaterialDO> selectMaterialsByUserGroupList(@Param("oid") Long oid, 
                                                   @Param("ugidList") List<Long> ugidList, 
                                                   @Param("fidCondition") String fidCondition);

    /**
     * 查询用户组列表可见的所有素材（支持子组+公共素材）
     * 优化版本：支持子组权限的全量素材查询
     */
    @Select("""
            <script>
            SELECT m.*, f.md5_hash, f.original_file_size, f.mime_type, f.file_extension,
                   f.storage_type, f.storage_path, f.upload_time, f.ref_count, f.file_status, f.meta_data_id
            FROM material m 
            LEFT JOIN material_file f ON m.file_id = f.file_id
            WHERE m.oid = #{oid} 
            AND (
                m.ugid IS NULL
                <if test="ugidList != null and !ugidList.isEmpty()">
                    OR m.ugid IN 
                    <foreach collection="ugidList" item="ugid" open="(" separator="," close=")">
                        #{ugid}
                    </foreach>
                </if>
            )
            ORDER BY m.create_time DESC
            </script>
            """)
    List<MaterialDO> selectAllVisibleMaterialsByUserGroupList(@Param("oid") Long oid, 
                                                             @Param("ugidList") List<Long> ugidList);
}