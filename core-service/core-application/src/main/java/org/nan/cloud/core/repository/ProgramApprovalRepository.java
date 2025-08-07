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
     * @param reviewerName 审核人员姓名
     * @param comment 审核意见
     * @return 更新的记录数
     */
    int updateApprovalStatus(Long approvalId, ProgramApprovalStatusEnum status, 
                           Long reviewerId, String reviewerName, String comment);
    
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
}