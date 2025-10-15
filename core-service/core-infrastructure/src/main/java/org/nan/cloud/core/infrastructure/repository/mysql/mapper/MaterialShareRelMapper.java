package org.nan.cloud.core.infrastructure.repository.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.MaterialShareRelDO;

import java.util.List;

@Mapper
public interface MaterialShareRelMapper extends BaseMapper<MaterialShareRelDO> {

    /**
     * 查询分享给指定用户组的素材分享记录
     */
    @Select("""
            SELECT msr.*, m.material_name, m.material_type, m.description as material_description,
                   f.original_file_size, f.mime_type, f.file_extension, f.file_status
            FROM material_share_rel msr
            LEFT JOIN material m ON msr.resource_id = m.mid 
            LEFT JOIN material_file f ON m.file_id = f.file_id
            WHERE msr.shared_to = #{ugid} AND msr.resource_type = 1
            ${fidCondition}
            ORDER BY msr.shared_time DESC
            """)
    List<MaterialShareRelDO> selectSharedMaterials(@Param("ugid") Long ugid, 
                                                  @Param("fidCondition") String fidCondition);

    /**
     * 查询分享给指定用户组的文件夹分享记录
     */
    @Select("""
            SELECT msr.*, folder.folder_name, folder.description as folder_description
            FROM material_share_rel msr
            LEFT JOIN folder ON msr.resource_id = folder.fid
            WHERE msr.shared_to = #{ugid} AND msr.resource_type = 2
            ORDER BY msr.shared_time DESC
            """)
    List<MaterialShareRelDO> selectSharedFolders(@Param("ugid") Long ugid);

    /**
     * 检查素材是否被分享给指定用户组
     */
    @Select("""
            SELECT COUNT(*) > 0 
            FROM material_share_rel 
            WHERE resource_id = #{mid} 
            AND resource_type = 1 
            AND shared_to = #{ugid}
            """)
    boolean existsSharedMaterialToUserGroup(@Param("mid") Long mid, @Param("ugid") Long ugid);

    /**
     * 根据分享记录ID查询详情
     */
    @Select("""
            SELECT msr.*, m.material_name, m.material_type, m.description as material_description,
                   f.original_file_size, f.mime_type, f.file_extension, f.file_status,
                   ug_from.name as shared_from_group_name,
                   ug_to.name as shared_to_group_name
            FROM material_share_rel msr
            LEFT JOIN material m ON msr.resource_id = m.mid 
            LEFT JOIN material_file f ON m.file_id = f.file_id
            LEFT JOIN user_group ug_from ON msr.shared_from = ug_from.ugid
            LEFT JOIN user_group ug_to ON msr.shared_to = ug_to.ugid
            WHERE msr.share_id = #{shareId}
            """)
    MaterialShareRelDO selectSharedMaterialDetailById(@Param("shareId") Long shareId);
}