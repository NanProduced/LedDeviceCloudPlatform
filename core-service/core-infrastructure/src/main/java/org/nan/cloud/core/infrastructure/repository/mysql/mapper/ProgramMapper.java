package org.nan.cloud.core.infrastructure.repository.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.nan.cloud.program.entity.ProgramDO;
import org.nan.cloud.program.enums.ProgramStatusEnum;
import org.nan.cloud.program.enums.VsnGenerationStatusEnum;

import java.util.List;

/**
 * 节目数据访问Mapper
 * 处理program表的CRUD操作
 */
@Mapper
public interface ProgramMapper extends BaseMapper<ProgramDO> {
    
    /**
     * 根据用户组查询节目列表
     * @param page 分页参数
     * @param oid 组织ID
     * @param ugid 用户组ID
     * @param status 节目状态（可选）
     * @return 节目列表
     */
    @Select("<script>" +
            "SELECT * FROM program WHERE deleted = 0 AND org_id = #{oid} AND user_group_id = #{ugid} " +
            "<if test='status != null'> AND status = #{status} </if>" +
            "ORDER BY updated_time DESC" +
            "</script>")
    IPage<ProgramDO> selectPageByUserGroup(Page<?> page, @Param("oid") Long oid, 
                                          @Param("ugid") Long ugid, @Param("status") ProgramStatusEnum status);
    
    /**
     * 根据创建者查询节目列表
     * @param page 分页参数
     * @param oid 组织ID
     * @param createdBy 创建者用户ID
     * @param status 节目状态（可选）
     * @return 节目列表
     */
    @Select("<script>" +
            "SELECT * FROM program WHERE deleted = 0 AND org_id = #{oid} AND created_by = #{createdBy} " +
            "<if test='status != null'> AND status = #{status} </if>" +
            "ORDER BY updated_time DESC" +
            "</script>")
    IPage<ProgramDO> selectPageByCreator(Page<?> page, @Param("oid") Long oid, 
                                        @Param("createdBy") Long createdBy, @Param("status") ProgramStatusEnum status);
    
    /**
     * 根据组织查询节目列表（管理员视图）
     * @param page 分页参数
     * @param oid 组织ID
     * @param status 节目状态（可选）
     * @return 节目列表
     */
    @Select("<script>" +
            "SELECT * FROM program WHERE deleted = 0 AND org_id = #{oid} " +
            "<if test='status != null'> AND status = #{status} </if>" +
            "ORDER BY updated_time DESC" +
            "</script>")
    IPage<ProgramDO> selectPageByOrganization(Page<?> page, @Param("oid") Long oid, @Param("status") ProgramStatusEnum status);
    
    /**
     * 检查节目名称在用户组内是否唯一
     * @param oid 组织ID
     * @param ugid 用户组ID
     * @param name 节目名称
     * @param excludeId 排除的节目ID
     * @return 存在的记录数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM program WHERE deleted = 0 AND org_id = #{oid} AND user_group_id = #{ugid} " +
            "AND name = #{name} " +
            "<if test='excludeId != null'> AND id != #{excludeId} </if>" +
            "</script>")
    long countByNameInUserGroup(@Param("oid") Long oid, @Param("ugid") Long ugid, 
                               @Param("name") String name, @Param("excludeId") Long excludeId);
    
    /**
     * 增加使用次数
     * @param id 节目ID
     * @return 更新的记录数
     */
    @Update("UPDATE program SET usage_count = usage_count + 1, updated_time = NOW() WHERE id = #{id}")
    int incrementUsageCount(@Param("id") Long id);
    
    /**
     * 查询热门节目（按使用次数排序）
     * @param oid 组织ID
     * @param ugid 用户组ID（可选）
     * @param limit 限制数量
     * @return 热门节目列表
     */
    @Select("<script>" +
            "SELECT * FROM program WHERE deleted = 0 AND org_id = #{oid} " +
            "<if test='ugid != null'> AND user_group_id = #{ugid} </if>" +
            "AND status = 'PUBLISHED' " +
            "ORDER BY usage_count DESC, updated_time DESC LIMIT #{limit}" +
            "</script>")
    List<ProgramDO> selectPopularPrograms(@Param("oid") Long oid, @Param("ugid") Long ugid, @Param("limit") int limit);
    
    /**
     * 检查用户是否有节目访问权限
     * @param programId 节目ID
     * @param userId 用户ID
     * @param oid 用户所属组织ID
     * @return 可访问的记录数
     */
    @Select("SELECT COUNT(*) FROM program p " +
            "WHERE p.deleted = 0 AND p.id = #{programId} AND p.org_id = #{oid} " +
            "AND (p.created_by = #{userId} OR p.user_group_id IN " +
            "(SELECT ug.id FROM user_group ug " +
            " JOIN user_group_rel ugr ON ug.id = ugr.user_group_id " +
            " WHERE ugr.user_id = #{userId} AND ug.deleted = 0))")
    long countAccessibleByUser(@Param("programId") Long programId, @Param("userId") Long userId, @Param("oid") Long oid);
    
    /**
     * 更新VSN生成状态
     * @param id 节目ID
     * @param status VSN生成状态
     * @param vsnFilePath VSN文件路径（可选）
     * @param errorMessage 错误信息（可选）
     * @return 更新的记录数
     */
    @Update("<script>" +
            "UPDATE program SET " +
            "vsn_generation_status = #{status}, " +
            "updated_time = NOW() " +
            "<if test='vsnFilePath != null'>, vsn_file_path = #{vsnFilePath} </if>" +
            "<if test='errorMessage != null'>, vsn_generation_error = #{errorMessage} </if>" +
            "WHERE id = #{id}" +
            "</script>")
    int updateVsnGenerationStatus(@Param("id") Long id, @Param("status") VsnGenerationStatusEnum status,
                                  @Param("vsnFilePath") String vsnFilePath, @Param("errorMessage") String errorMessage);
    
    // ===== 版本链查询方法 =====
    
    /**
     * 根据原始节目ID查询所有版本
     * @param sourceProgramId 原始节目ID
     * @return 版本列表（按版本号排序）
     */
    @Select("SELECT * FROM program WHERE deleted = 0 AND " +
            "(source_program_id = #{sourceProgramId} OR (id = #{sourceProgramId} AND is_source_program = true)) " +
            "ORDER BY version ASC")
    List<ProgramDO> selectVersionsBySourceProgramId(@Param("sourceProgramId") Long sourceProgramId);
    
    /**
     * 获取节目的最新版本
     * @param sourceProgramId 原始节目ID
     * @return 最新版本节目
     */
    @Select("SELECT * FROM program WHERE deleted = 0 AND " +
            "(source_program_id = #{sourceProgramId} OR (id = #{sourceProgramId} AND is_source_program = true)) " +
            "ORDER BY version DESC LIMIT 1")
    ProgramDO selectLatestVersionBySourceProgramId(@Param("sourceProgramId") Long sourceProgramId);
    
    /**
     * 根据任意版本ID查找原始节目ID
     * @param programId 任意版本的节目ID
     * @return 原始节目ID
     */
    @Select("SELECT CASE " +
            "WHEN is_source_program = true THEN id " +
            "ELSE source_program_id " +
            "END as source_id " +
            "FROM program WHERE id = #{programId} AND deleted = 0")
    Long selectSourceProgramIdByAnyVersion(@Param("programId") Long programId);
    
    /**
     * 检查指定版本是否存在
     * @param sourceProgramId 原始节目ID
     * @param version 版本号
     * @return 存在的记录数
     */
    @Select("SELECT COUNT(*) FROM program WHERE deleted = 0 AND " +
            "(source_program_id = #{sourceProgramId} OR (id = #{sourceProgramId} AND is_source_program = true)) " +
            "AND version = #{version}")
    long countBySourceProgramIdAndVersion(@Param("sourceProgramId") Long sourceProgramId, @Param("version") Integer version);
    
    /**
     * 获取节目的最大版本号
     * @param sourceProgramId 原始节目ID
     * @return 最大版本号
     */
    @Select("SELECT MAX(version) FROM program WHERE deleted = 0 AND " +
            "(source_program_id = #{sourceProgramId} OR (id = #{sourceProgramId} AND is_source_program = true))")
    Integer selectMaxVersionBySourceProgramId(@Param("sourceProgramId") Long sourceProgramId);
    
    /**
     * 根据原始节目ID和版本号查询特定版本
     * @param sourceProgramId 原始节目ID
     * @param version 版本号
     * @return 特定版本节目
     */
    @Select("SELECT * FROM program WHERE deleted = 0 AND " +
            "(source_program_id = #{sourceProgramId} OR (id = #{sourceProgramId} AND is_source_program = true)) " +
            "AND version = #{version}")
    ProgramDO selectBySourceProgramIdAndVersion(@Param("sourceProgramId") Long sourceProgramId, @Param("version") Integer version);
}