package org.nan.cloud.core.service;

import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.domain.ProgramApproval;
import org.nan.cloud.program.dto.request.ApprovalRequest;
import org.nan.cloud.program.dto.response.ProgramApprovalDTO;
import org.nan.cloud.program.enums.ProgramApprovalStatusEnum;

import java.util.List;
import java.util.Optional;

/**
 * 节目审核服务接口
 * 处理节目审核流程的核心业务逻辑
 */
public interface ProgramApprovalService {
    
    /**
     * 提交节目审核申请
     * @param programId 节目ID
     * @param programVersion 节目版本号
     * @param userId 申请人用户ID
     * @param oid 组织ID
     * @return 审核记录信息
     */
    ProgramApprovalDTO submitApproval(Long programId, Integer programVersion, Long userId, Long oid);
    
    /**
     * 审核通过
     * @param approvalId 审核记录ID
     * @param request 审核请求
     * @param reviewerId 审核人员ID
     * @param oid 组织ID
     * @return 审核结果
     */
    boolean approveProgram(Long approvalId, ApprovalRequest request, Long reviewerId, Long oid);
    
    /**
     * 审核拒绝
     * @param approvalId 审核记录ID
     * @param request 审核请求（包含拒绝原因）
     * @param reviewerId 审核人员ID
     * @param oid 组织ID
     * @return 审核结果
     */
    boolean rejectProgram(Long approvalId, ApprovalRequest request, Long reviewerId, Long oid);
    
    /**
     * 查询节目的审核记录
     * @param programId 节目ID
     * @param oid 组织ID
     * @return 审核记录列表
     */
    List<ProgramApprovalDTO> getProgramApprovalHistory(Long programId, Long oid);
    
    /**
     * 查询节目特定版本的审核记录
     * @param programId 节目ID
     * @param programVersion 版本号
     * @param oid 组织ID
     * @return 审核记录
     */
    Optional<ProgramApprovalDTO> getProgramVersionApproval(Long programId, Integer programVersion, Long oid);
    
    /**
     * 查询节目最新的审核记录
     * @param programId 节目ID
     * @param oid 组织ID
     * @return 最新审核记录
     */
    Optional<ProgramApprovalDTO> getLatestProgramApproval(Long programId, Long oid);
    
    /**
     * 分页查询组织下的待审核列表 (已废弃)
     * @param oid 组织ID
     * @param page 页码，从1开始
     * @param size 每页大小
     * @return 分页结果
     * @deprecated 使用新的三维度接口替代
     */
    @Deprecated
    PageVO<ProgramApprovalDTO> getPendingApprovals(Long oid, int page, int size);
    
    /**
     * 查询待我审核的节目列表
     * 业务逻辑：节目的ugid属于当前用户组或子组，且审核状态为PENDING
     * @param userId 当前用户ID
     * @param userUgid 当前用户的用户组ID
     * @param oid 组织ID
     * @param page 页码，从1开始
     * @param size 每页大小
     * @return 分页结果
     */
    PageVO<ProgramApprovalDTO> getPendingApprovalsForMe(Long userId, Long userUgid, Long oid, int page, int size);
    
    /**
     * 查询我发起的审核申请列表
     * 业务逻辑：创建者为当前用户ID的所有审核记录
     * @param userId 当前用户ID
     * @param oid 组织ID
     * @param page 页码，从1开始
     * @param size 每页大小
     * @param status 审核状态过滤（可选）
     * @return 分页结果
     */
    PageVO<ProgramApprovalDTO> getInitiatedApprovalsByMe(Long userId, Long oid, int page, int size, ProgramApprovalStatusEnum status);
    
    /**
     * 查询全部审核记录
     * 业务逻辑：ugid属于当前用户组层级的所有审核记录
     * @param userId 当前用户ID
     * @param userUgid 当前用户的用户组ID
     * @param oid 组织ID
     * @param page 页码，从1开始
     * @param size 每页大小
     * @param status 审核状态过滤（可选）
     * @return 分页结果
     */
    PageVO<ProgramApprovalDTO> getAllApprovals(Long userId, Long userUgid, Long oid, int page, int size, ProgramApprovalStatusEnum status);
    
    /**
     * 分页查询审核人员的审核记录
     * @param reviewerId 审核人员ID
     * @param status 审核状态（可选）
     * @param oid 组织ID
     * @param page 页码，从1开始
     * @param size 每页大小
     * @return 分页结果
     */
    PageVO<ProgramApprovalDTO> getReviewerApprovals(Long reviewerId, ProgramApprovalStatusEnum status, Long oid, int page, int size);
    
    /**
     * 检查节目版本是否已通过审核
     * @param programId 节目ID
     * @param programVersion 版本号
     * @param oid 组织ID
     * @return 是否已通过审核
     */
    boolean isVersionApproved(Long programId, Integer programVersion, Long oid);
    
    /**
     * 检查节目是否需要审核
     * 根据组织设置判断是否启用审核流程
     * @param oid 组织ID
     * @return 是否需要审核
     */
    boolean isApprovalRequired(Long oid);
    
    /**
     * 撤销审核申请
     * 仅允许撤销待审核状态的申请
     * @param approvalId 审核记录ID
     * @param userId 申请撤销的用户ID
     * @param oid 组织ID
     * @return 撤销结果
     */
    boolean withdrawApproval(Long approvalId, Long userId, Long oid);
    
    /**
     * 获取组织待审核数量统计
     * @param oid 组织ID
     * @return 待审核数量
     */
    long getPendingApprovalCount(Long oid);
    
    /**
     * 获取审核人员的审核数量统计
     * @param reviewerId 审核人员ID
     * @param status 审核状态（可选）
     * @param oid 组织ID
     * @return 审核数量
     */
    long getReviewerApprovalCount(Long reviewerId, ProgramApprovalStatusEnum status, Long oid);
}