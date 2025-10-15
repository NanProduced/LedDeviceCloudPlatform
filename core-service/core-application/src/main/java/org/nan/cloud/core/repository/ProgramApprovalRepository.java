package org.nan.cloud.core.repository;

import org.nan.cloud.core.domain.ProgramApproval;
import org.nan.cloud.program.enums.ProgramApprovalStatusEnum;

import java.util.List;
import java.util.Optional;

/**
 * 节目审核Repository接口
 * 处理节目审核记录的MySQL数据访问
 */
public interface ProgramApprovalRepository {

    /**
     * 根据审核id查审核记录
     * @param approvalId
     * @return
     */
    ProgramApproval findByApprovalId(Long approvalId);

    /**
     * 根据节目ID查询审核记录
     * @param programId 节目ID
     * @return 审核记录列表
     */
    List<ProgramApproval> findByProgramId(Long programId);
    
    /**
     * 根据节目ID和版本查询审核记录
     * @param programId 节目ID
     * @param version 版本号
     * @return 审核记录
     */
    Optional<ProgramApproval> findByProgramIdAndVersion(Long programId, Integer version);
    
    /**
     * 查询最新的审核记录
     * @param programId 节目ID
     * @return 最新审核记录
     */
    Optional<ProgramApproval> findLatestByProgramId(Long programId);
    
    /**
     * 查询组织下的待审核记录
     * @param oid 组织ID
     * @param page 页码，从1开始
     * @param size 每页大小
     * @return 待审核记录列表
     */
    List<ProgramApproval> findPendingByOrganization(Long oid, int page, int size);
    
    /**
     * 统计组织下的待审核数量
     * @param oid 组织ID
     * @return 待审核数量
     */
    long countPendingByOrganization(Long oid);
    
    /**
     * 查询审核人员的审核记录
     * @param reviewerId 审核人员ID
     * @param status 审核状态（可选）
     * @param page 页码，从1开始
     * @param size 每页大小
     * @return 审核记录列表
     */
    List<ProgramApproval> findByReviewer(Long reviewerId, ProgramApprovalStatusEnum status, int page, int size);
    
    /**
     * 统计审核人员的审核数量
     * @param reviewerId 审核人员ID
     * @param status 审核状态（可选）
     * @return 审核数量
     */
    long countByReviewer(Long reviewerId, ProgramApprovalStatusEnum status);
    
    /**
     * 保存审核记录
     * @param approval 审核记录
     * @return 保存后的审核记录
     */
    ProgramApproval save(ProgramApproval approval);
    
    /**
     * 更新审核状态
     * @param approvalId 审核记录ID
     * @param status 新状态
     * @param reviewerId 审核人员ID
     * @param comment 审核意见
     * @return 更新的记录数
     */
    int updateApprovalStatus(Long approvalId, ProgramApprovalStatusEnum status, 
                           Long reviewerId, String comment);
    
    /**
     * 检查节目版本是否已通过审核
     * @param programId 节目ID
     * @param version 版本号
     * @return 是否已通过审核
     */
    boolean isVersionApproved(Long programId, Integer version);
    
    /**
     * 删除审核记录
     * @param approvalId 审核记录ID
     * @return 删除的记录数
     */
    int deleteById(Long approvalId);
    
    /**
     * 根据节目ID删除相关审核记录
     * @param programId 节目ID
     * @return 删除的记录数
     */
    int deleteByProgramId(Long programId);
    
    // ===== 新增三维度查询方法 =====
    
    /**
     * 查询待我审核的节目列表（基于用户组层级）
     * 业务逻辑：节目的ugid属于指定用户组层级，且审核状态为PENDING
     * @param userGroupIds 用户组层级ID列表（当前组+子组）
     * @param oid 组织ID
     * @param page 页码，从1开始
     * @param size 每页大小
     * @return 审核记录列表
     */
    List<ProgramApproval> findPendingByUserGroups(List<Long> userGroupIds, Long oid, int page, int size);
    
    /**
     * 统计待我审核的节目数量（基于用户组层级）
     * @param userGroupIds 用户组层级ID列表
     * @param oid 组织ID
     * @return 数量
     */
    long countPendingByUserGroups(List<Long> userGroupIds, Long oid);
    
    /**
     * 查询我发起的审核申请列表
     * 业务逻辑：创建者为指定用户ID的审核记录
     * @param createdBy 创建者用户ID
     * @param oid 组织ID
     * @param status 审核状态过滤（可选）
     * @param page 页码，从1开始
     * @param size 每页大小
     * @return 审核记录列表
     */
    List<ProgramApproval> findByCreatedBy(Long createdBy, Long oid, ProgramApprovalStatusEnum status, int page, int size);
    
    /**
     * 统计我发起的审核申请数量
     * @param createdBy 创建者用户ID
     * @param oid 组织ID
     * @param status 审核状态过滤（可选）
     * @return 数量
     */
    long countByCreatedBy(Long createdBy, Long oid, ProgramApprovalStatusEnum status);
    
    /**
     * 查询全部审核记录（基于用户组层级）
     * 业务逻辑：节目的ugid属于指定用户组层级的所有审核记录
     * @param userGroupIds 用户组层级ID列表（当前组+子组）
     * @param oid 组织ID
     * @param status 审核状态过滤（可选）
     * @param page 页码，从1开始
     * @param size 每页大小
     * @return 审核记录列表
     */
    List<ProgramApproval> findAllByUserGroups(List<Long> userGroupIds, Long oid, ProgramApprovalStatusEnum status, int page, int size);
    
    /**
     * 统计全部审核记录数量（基于用户组层级）
     * @param userGroupIds 用户组层级ID列表
     * @param oid 组织ID
     * @param status 审核状态过滤（可选）
     * @return 数量
     */
    long countAllByUserGroups(List<Long> userGroupIds, Long oid, ProgramApprovalStatusEnum status);
}