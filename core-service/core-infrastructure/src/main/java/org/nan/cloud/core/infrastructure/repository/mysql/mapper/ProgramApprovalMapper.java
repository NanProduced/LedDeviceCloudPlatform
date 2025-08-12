package org.nan.cloud.core.infrastructure.repository.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.*;
import org.nan.cloud.program.entity.ProgramApprovalDO;
import org.nan.cloud.program.enums.ProgramApprovalStatusEnum;

import java.util.List;

/**
 * 节目审核记录数据访问Mapper
 * 处理program_approval表的CRUD操作
 */
@Mapper
public interface ProgramApprovalMapper extends BaseMapper<ProgramApprovalDO> {
    
    /**
     * 根据节目ID查询审核记录
     * @param programId 节目ID
     * @return 审核记录列表
     */
    @Select("SELECT * FROM program_approval WHERE program_id = #{programId} ORDER BY created_time DESC")
    List<ProgramApprovalDO> selectByProgramId(@Param("programId") Long programId);
    
    /**
     * 根据节目ID和版本查询审核记录
     * @param programId 节目ID
     * @param version 版本号
     * @return 审核记录
     */
    @Select("SELECT * FROM program_approval WHERE program_id = #{programId} AND program_version = #{version} ORDER BY created_time DESC LIMIT 1")
    ProgramApprovalDO selectByProgramIdAndVersion(@Param("programId") Long programId, @Param("version") Integer version);
    
    /**
     * 查询节目的最新审核记录
     * @param programId 节目ID
     * @return 最新审核记录
     */
    @Select("SELECT * FROM program_approval WHERE program_id = #{programId} ORDER BY program_version DESC, created_time DESC LIMIT 1")
    ProgramApprovalDO selectLatestByProgramId(@Param("programId") Long programId);
    
    /**
     * 分页查询组织下的待审核记录
     * @param page 分页参数
     * @param oid 组织ID
     * @return 待审核记录列表
     */
    @Select("SELECT * FROM program_approval WHERE org_id = #{oid} AND status = 'PENDING' ORDER BY applied_time ASC")
    IPage<ProgramApprovalDO> selectPendingByOrganization(Page<?> page, @Param("oid") Long oid);
    
    /**
     * 统计组织下的待审核数量
     * @param oid 组织ID
     * @return 待审核数量
     */
    @Select("SELECT COUNT(*) FROM program_approval WHERE org_id = #{oid} AND status = 'PENDING'")
    long countPendingByOrganization(@Param("oid") Long oid);
    
    /**
     * 分页查询审核人员的审核记录
     * @param page 分页参数
     * @param reviewerId 审核人员ID
     * @param status 审核状态（可选）
     * @return 审核记录列表
     */
    @Select("<script>" +
            "SELECT * FROM program_approval WHERE reviewer_id = #{reviewerId} " +
            "<if test='status != null'> AND status = #{status} </if>" +
            "ORDER BY reviewed_time DESC" +
            "</script>")
    IPage<ProgramApprovalDO> selectByReviewer(Page<?> page, @Param("reviewerId") Long reviewerId, 
                                            @Param("status") ProgramApprovalStatusEnum status);
    
    /**
     * 统计审核人员的审核数量
     * @param reviewerId 审核人员ID
     * @param status 审核状态（可选）
     * @return 审核数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM program_approval WHERE reviewer_id = #{reviewerId} " +
            "<if test='status != null'> AND status = #{status} </if>" +
            "</script>")
    long countByReviewer(@Param("reviewerId") Long reviewerId, @Param("status") ProgramApprovalStatusEnum status);
    
    /**
     * 更新审核状态和审核信息
     * @param id 审核记录ID
     * @param status 新状态
     * @param reviewerId 审核人员ID
     * @param reviewComment 审核意见
     * @param rejectionReason 拒绝原因（可选）
     * @return 更新的记录数
     */
    @Update("<script>" +
            "UPDATE program_approval SET " +
            "status = #{status}, " +
            "reviewer_id = #{reviewerId}, " +
            "review_comment = #{reviewComment}, " +
            "reviewed_time = NOW(), " +
            "updated_time = NOW() " +
            "<if test='rejectionReason != null'> , rejection_reason = #{rejectionReason} </if>" +
            "WHERE id = #{id}" +
            "</script>")
    int updateApprovalStatus(@Param("id") Long id, 
                           @Param("status") ProgramApprovalStatusEnum status,
                           @Param("reviewerId") Long reviewerId,
                           @Param("reviewComment") String reviewComment,
                           @Param("rejectionReason") String rejectionReason);
    
    /**
     * 检查节目版本是否已通过审核
     * @param programId 节目ID
     * @param version 版本号
     * @return 是否已通过审核
     */
    @Select("SELECT COUNT(*) > 0 FROM program_approval WHERE program_id = #{programId} AND program_version = #{version} AND status = 'APPROVED'")
    boolean isVersionApproved(@Param("programId") Long programId, @Param("version") Integer version);
    
    /**
     * 根据节目ID删除相关审核记录
     * @param programId 节目ID
     * @return 删除的记录数
     */
    @Delete("DELETE FROM program_approval WHERE program_id = #{programId}")
    int deleteByProgramId(@Param("programId") Long programId);
    
    // ===== 新增三维度查询方法 =====
    
    /**
     * 分页查询待我审核的节目列表（基于用户组层级）
     * 业务逻辑：节目的ugid属于指定用户组层级，且审核状态为PENDING
     * @param page 分页参数
     * @param userGroupIds 用户组层级ID列表（当前组+子组）
     * @param oid 组织ID
     * @return 审核记录列表
     */
    @Select("<script>" +
            "SELECT pa.* FROM program_approval pa " +
            "INNER JOIN program p ON pa.program_id = p.id " +
            "WHERE pa.org_id = #{oid} " +
            "AND pa.status = 'PENDING' " +
            "AND p.ugid IN " +
            "<foreach collection='userGroupIds' item='ugid' open='(' separator=',' close=')'>" +
            "#{ugid}" +
            "</foreach> " +
            "ORDER BY pa.applied_time ASC" +
            "</script>")
    IPage<ProgramApprovalDO> selectPendingByUserGroups(Page<?> page, 
                                                      @Param("userGroupIds") List<Long> userGroupIds, 
                                                      @Param("oid") Long oid);
    
    /**
     * 统计待我审核的节目数量（基于用户组层级）
     * @param userGroupIds 用户组层级ID列表
     * @param oid 组织ID
     * @return 数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM program_approval pa " +
            "INNER JOIN program p ON pa.program_id = p.id " +
            "WHERE pa.org_id = #{oid} " +
            "AND pa.status = 'PENDING' " +
            "AND p.ugid IN " +
            "<foreach collection='userGroupIds' item='ugid' open='(' separator=',' close=')'>" +
            "#{ugid}" +
            "</foreach>" +
            "</script>")
    long countPendingByUserGroups(@Param("userGroupIds") List<Long> userGroupIds, @Param("oid") Long oid);
    
    /**
     * 分页查询我发起的审核申请列表
     * 业务逻辑：创建者为指定用户ID的审核记录
     * @param page 分页参数
     * @param createdBy 创建者用户ID
     * @param oid 组织ID
     * @param status 审核状态过滤（可选）
     * @return 审核记录列表
     */
    @Select("<script>" +
            "SELECT * FROM program_approval " +
            "WHERE created_by = #{createdBy} " +
            "AND org_id = #{oid} " +
            "<if test='status != null'> AND status = #{status} </if>" +
            "ORDER BY applied_time DESC" +
            "</script>")
    IPage<ProgramApprovalDO> selectByCreatedBy(Page<?> page, 
                                             @Param("createdBy") Long createdBy, 
                                             @Param("oid") Long oid,
                                             @Param("status") ProgramApprovalStatusEnum status);
    
    /**
     * 统计我发起的审核申请数量
     * @param createdBy 创建者用户ID
     * @param oid 组织ID
     * @param status 审核状态过滤（可选）
     * @return 数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM program_approval " +
            "WHERE created_by = #{createdBy} " +
            "AND org_id = #{oid} " +
            "<if test='status != null'> AND status = #{status} </if>" +
            "</script>")
    long countByCreatedBy(@Param("createdBy") Long createdBy, 
                         @Param("oid") Long oid, 
                         @Param("status") ProgramApprovalStatusEnum status);
    
    /**
     * 分页查询全部审核记录（基于用户组层级）
     * 业务逻辑：节目的ugid属于指定用户组层级的所有审核记录
     * @param page 分页参数
     * @param userGroupIds 用户组层级ID列表（当前组+子组）
     * @param oid 组织ID
     * @param status 审核状态过滤（可选）
     * @return 审核记录列表
     */
    @Select("<script>" +
            "SELECT pa.* FROM program_approval pa " +
            "INNER JOIN program p ON pa.program_id = p.id " +
            "WHERE pa.org_id = #{oid} " +
            "<if test='status != null'> AND pa.status = #{status} </if>" +
            "AND p.ugid IN " +
            "<foreach collection='userGroupIds' item='ugid' open='(' separator=',' close=')'>" +
            "#{ugid}" +
            "</foreach> " +
            "ORDER BY pa.applied_time DESC" +
            "</script>")
    IPage<ProgramApprovalDO> selectAllByUserGroups(Page<?> page, 
                                                  @Param("userGroupIds") List<Long> userGroupIds, 
                                                  @Param("oid") Long oid,
                                                  @Param("status") ProgramApprovalStatusEnum status);
    
    /**
     * 统计全部审核记录数量（基于用户组层级）
     * @param userGroupIds 用户组层级ID列表
     * @param oid 组织ID
     * @param status 审核状态过滤（可选）
     * @return 数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM program_approval pa " +
            "INNER JOIN program p ON pa.program_id = p.id " +
            "WHERE pa.org_id = #{oid} " +
            "<if test='status != null'> AND pa.status = #{status} </if>" +
            "AND p.ugid IN " +
            "<foreach collection='userGroupIds' item='ugid' open='(' separator=',' close=')'>" +
            "#{ugid}" +
            "</foreach>" +
            "</script>")
    long countAllByUserGroups(@Param("userGroupIds") List<Long> userGroupIds, 
                             @Param("oid") Long oid, 
                             @Param("status") ProgramApprovalStatusEnum status);
}