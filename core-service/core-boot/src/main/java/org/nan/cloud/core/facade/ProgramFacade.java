package org.nan.cloud.core.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.model.PageRequestDTO;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.common.web.context.InvocationContextHolder;
import org.nan.cloud.common.web.context.RequestUserInfo;
import org.nan.cloud.core.api.DTO.req.QueryProgramListRequest;
import org.nan.cloud.core.api.DTO.req.PendingApprovalForMeRequest;
import org.nan.cloud.core.api.DTO.req.InitiatedApprovalsByMeRequest;
import org.nan.cloud.core.api.DTO.req.AllApprovalsRequest;
import org.nan.cloud.core.service.ProgramDraftService;
import org.nan.cloud.core.repository.ProgramContentRepository;
import org.nan.cloud.program.document.ProgramContent;
import org.nan.cloud.core.service.ProgramService;
import org.nan.cloud.core.service.ProgramApprovalService;
import org.nan.cloud.program.dto.request.CreateProgramRequest;
import org.nan.cloud.program.dto.request.SaveDraftRequest;
import org.nan.cloud.program.dto.request.UpdateProgramRequest;
import org.nan.cloud.program.dto.response.DraftDTO;
import org.nan.cloud.program.dto.response.ProgramContentDTO;
import org.nan.cloud.program.dto.response.ProgramDTO;
import org.nan.cloud.program.dto.response.ProgramVersionDTO;
import org.nan.cloud.program.dto.request.ApprovalRequest;
import org.nan.cloud.program.dto.response.ProgramApprovalDTO;
import org.nan.cloud.program.enums.ProgramStatusEnum;
import org.nan.cloud.program.enums.ProgramApprovalStatusEnum;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 节目管理门面层
 * 整合节目和草稿服务，处理业务协调和用户上下文
 * 符合项目Facade模式，为AOP和统一处理提供中间层
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProgramFacade {

    private final ProgramService programService;
    private final ProgramDraftService programDraftService;
    private final ProgramContentRepository programContentRepository;
    private final ProgramApprovalService programApprovalService;

    /**
     * 创建节目
     */
    public ProgramDTO createProgram(CreateProgramRequest request) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        log.info("Creating program: name={}, userId={}, oid={}", 
                request.getName(), userInfo.getUid(), userInfo.getOid());

        return programService.createProgram(request, userInfo.getUid(), userInfo.getOid(), userInfo.getUgid());
    }

    /**
     * 更新节目
     */
    public ProgramDTO updateProgram(Long programId, UpdateProgramRequest request) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        log.info("Updating program: programId={}, userId={}, oid={}", 
                programId, userInfo.getUid(), userInfo.getOid());

        return programService.updateProgram(programId, request, userInfo.getUid(), userInfo.getOid());
    }

    /**
     * 保存草稿
     */
    public DraftDTO saveDraft(Long programId, SaveDraftRequest request) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        log.info("Saving draft: programId={}, userId={}, oid={}", 
                programId, userInfo.getUid(), userInfo.getOid());

        if (programId != null) {
            // 更新现有草稿（转换为UpdateDraftRequest）
            var updateRequest = convertToUpdateDraftRequest(request);
            return programDraftService.updateDraft(programId, updateRequest, userInfo.getUid(), userInfo.getOid());
        } else {
            // 创建新草稿
            return programDraftService.saveDraft(request, userInfo.getUid(), userInfo.getOid(), userInfo.getUgid());
        }
    }

    /**
     * 获取节目详情
     */
    public ProgramDTO getProgramDetails(Long programId) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        log.debug("Getting program details: programId={}, oid={}", programId, userInfo.getOid());

        Optional<ProgramDTO> programOpt = programService.findProgramById(programId, userInfo.getOid());
        if (programOpt.isEmpty()) {
            // 尝试从草稿中查找
            Optional<DraftDTO> draftOpt = programDraftService.findDraftById(programId, userInfo.getOid());
            if (draftOpt.isPresent()) {
                return convertDraftToProgram(draftOpt.get());
            }
            throw new IllegalArgumentException("节目不存在: " + programId);
        }

        return programOpt.get();
    }

    /**
     * 获取节目内容
     */
    public ProgramContentDTO getProgramContent(Long programId, Integer versionId) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        log.debug("Getting program content: programId={}, versionId={}, oid={}", 
                programId, versionId, userInfo.getOid());

        // 校验节目存在性/访问权限
        ProgramDTO program = getProgramDetails(programId);

        // 查询内容（按版本或最新）
        java.util.Optional<ProgramContent> contentOpt = (versionId == null)
                ? programContentRepository.findLatestVersionByProgramId(programId)
                : programContentRepository.findByProgramIdAndVersion(programId, versionId);

        if (contentOpt.isEmpty()) {
            // 返回空内容的DTO，避免前端报错
            return ProgramContentDTO.builder()
                    .programId(programId)
                    .versionId(versionId)
                    .contentData(null)
                    .vsnData(null)
                    .vsnXml(null)
                    .name(program.getName())
                    .description(program.getDescription())
                    .width(program.getWidth())
                    .height(program.getHeight())
                    .duration(program.getDuration())
                    .thumbnailUrl(program.getThumbnailUrl())
                    .build();
        }

        ProgramContent content = contentOpt.get();

        return ProgramContentDTO.builder()
                .programId(content.getProgramId())
                .versionId(content.getVersion())
                .contentData(content.getOriginalData())
                .vsnData(null) // 目前不直接返回vsnPrograms序列化数据
                .vsnXml(content.getVsnXml())
                .name(program.getName())
                .description(program.getDescription())
                .width(program.getWidth())
                .height(program.getHeight())
                .duration(program.getDuration())
                .thumbnailUrl(program.getThumbnailUrl())
                .build();
    }

    /**
     * 节目列表
     */
    public PageVO<ProgramDTO> listPrograms(PageRequestDTO<QueryProgramListRequest> pageRequestDTO) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        
        log.debug("Listing programs: keyword={}, status={}, page={}, pageSize={}, oid={}, ugid={}", 
                pageRequestDTO.getParams().getKeyword(), 
                pageRequestDTO.getParams().getStatus(), 
                pageRequestDTO.getPageNum(),
                pageRequestDTO.getPageSize(),
                userInfo.getOid(), 
                userInfo.getUgid());

        // 直接使用前端传入的状态，无需转换
        ProgramStatusEnum statusEnum = null;
        if (pageRequestDTO.getParams().getStatus() != null) {
            statusEnum = ProgramStatusEnum.valueOf(pageRequestDTO.getParams().getStatus().toUpperCase());
        }

        // 调用Service层分页查询
        return programService.findProgramsPage(
                userInfo.getOid(), 
                userInfo.getUgid(), 
                pageRequestDTO.getParams().getKeyword(),
                statusEnum,
                pageRequestDTO.getPageNum(), 
                pageRequestDTO.getPageSize());
    }

    /**
     * 删除节目
     */
    public ProgramDTO deleteProgram(Long programId) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        log.info("Deleting program: programId={}, userId={}, oid={}", 
                programId, userInfo.getUid(), userInfo.getOid());

        // 先获取节目信息
        ProgramDTO program = getProgramDetails(programId);
        
        boolean success = programService.deleteProgram(programId, userInfo.getUid(), userInfo.getOid());
        if (!success) {
            throw new RuntimeException("删除节目失败: " + programId);
        }

        // 返回删除标记
        program.setId(programId);
        return program;
    }

    

    /**
     * 获取节目版本列表
     */
    public List<ProgramVersionDTO> getProgramVersions(Long programId) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        log.debug("Getting program versions: programId={}, oid={}", programId, userInfo.getOid());

        return programService.findAllVersions(programId, userInfo.getOid());
    }

    /**
     * 版本回滚
     */
    public ProgramDTO revertToVersion(Long programId, Integer versionId) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        log.info("Reverting program to version: programId={}, versionId={}, userId={}, oid={}", 
                programId, versionId, userInfo.getUid(), userInfo.getOid());

        return programService.rollbackToVersion(programId, versionId, userInfo.getUid(), userInfo.getOid());
    }

    // ===== 节目审核相关方法 =====
    
    /**
     * 提交节目审核申请
     */
    public ProgramApprovalDTO submitProgramApproval(Long programId, Integer versionId) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        log.info("Submitting program approval: programId={}, versionId={}, userId={}, oid={}", 
                programId, versionId, userInfo.getUid(), userInfo.getOid());
        
        return programApprovalService.submitApproval(programId, versionId, userInfo.getUid(), userInfo.getOid());
    }
    
    /**
     * 审核通过
     */
    public boolean approveProgramApproval(Long approvalId, ApprovalRequest request) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        log.info("Approving program: approvalId={}, reviewerId={}, oid={}",
                approvalId, userInfo.getUid(), userInfo.getOid());
        
        return programApprovalService.approveProgram(approvalId, request, userInfo.getUid(), userInfo.getOid());
    }
    
    /**
     * 审核拒绝
     */
    public boolean rejectProgramApproval(Long approvalId, ApprovalRequest request) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        log.info("Rejecting program: approvalId={}, reviewerId={}, oid={}",
                approvalId, userInfo.getUid(), userInfo.getOid());
        
        return programApprovalService.rejectProgram(approvalId, request, userInfo.getUid(), userInfo.getOid());
    }
    
    /**
     * 查询节目审核历史
     */
    public List<ProgramApprovalDTO> getProgramApprovalHistory(Long programId) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        log.debug("Getting program approval history: programId={}, oid={}", programId, userInfo.getOid());
        
        return programApprovalService.getProgramApprovalHistory(programId, userInfo.getOid());
    }
    
    /**
     * 查询节目版本审核状态
     */
    public ProgramApprovalDTO getProgramVersionApproval(Long programId, Integer versionId) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        log.debug("Getting program version approval: programId={}, versionId={}, oid={}", 
                programId, versionId, userInfo.getOid());
        
        Optional<ProgramApprovalDTO> approval = programApprovalService.getProgramVersionApproval(
                programId, versionId, userInfo.getOid());
        
        return approval.orElse(null);
    }
    
    /**
     * 查询组织待审核列表 (已废弃)
     * @deprecated 使用新的三维度接口替代
     */
    @Deprecated
    public PageVO<ProgramApprovalDTO> getPendingApprovals(int page, int size) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        log.debug("Getting pending approvals (deprecated): oid={}, page={}, size={}", userInfo.getOid(), page, size);
        
        return programApprovalService.getPendingApprovals(userInfo.getOid(), page, size);
    }
    
    /**
     * 查询待我审核的节目列表
     */
    public PageVO<ProgramApprovalDTO> getPendingApprovalsForMe(PageRequestDTO<PendingApprovalForMeRequest> request) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        log.debug("Getting pending approvals for me: uid={}, ugid={}, oid={}, page={}, size={}, params={}", 
                userInfo.getUid(), userInfo.getUgid(), userInfo.getOid(), 
                request.getPageNum(), request.getPageSize(), request.getParams());
        
        // 从请求参数中提取状态，如果没有则默认为PENDING
        ProgramApprovalStatusEnum status = (request.getParams() != null && request.getParams().getStatus() != null) 
                ? request.getParams().getStatus() 
                : ProgramApprovalStatusEnum.PENDING;
                
        return programApprovalService.getPendingApprovalsForMe(
                userInfo.getUid(), userInfo.getUgid(), userInfo.getOid(), 
                request.getPageNum(), request.getPageSize());
    }
    
    /**
     * 查询我发起的审核申请列表
     */
    public PageVO<ProgramApprovalDTO> getInitiatedApprovalsByMe(PageRequestDTO<InitiatedApprovalsByMeRequest> request) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        log.debug("Getting initiated approvals by me: uid={}, oid={}, page={}, size={}, params={}", 
                userInfo.getUid(), userInfo.getOid(), 
                request.getPageNum(), request.getPageSize(), request.getParams());
        
        // 从请求参数中提取状态过滤条件
        ProgramApprovalStatusEnum status = (request.getParams() != null) 
                ? request.getParams().getStatus() 
                : null;
        
        return programApprovalService.getInitiatedApprovalsByMe(
                userInfo.getUid(), userInfo.getOid(), 
                request.getPageNum(), request.getPageSize(), status);
    }
    
    /**
     * 查询全部审核记录
     */
    public PageVO<ProgramApprovalDTO> getAllApprovals(PageRequestDTO<AllApprovalsRequest> request) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        log.debug("Getting all approvals: uid={}, ugid={}, oid={}, page={}, size={}, params={}", 
                userInfo.getUid(), userInfo.getUgid(), userInfo.getOid(), 
                request.getPageNum(), request.getPageSize(), request.getParams());
        
        // 从请求参数中提取状态过滤条件
        ProgramApprovalStatusEnum status = (request.getParams() != null) 
                ? request.getParams().getStatus() 
                : null;
        
        return programApprovalService.getAllApprovals(
                userInfo.getUid(), userInfo.getUgid(), userInfo.getOid(), 
                request.getPageNum(), request.getPageSize(), status);
    }
    
    /**
     * 查询审核人员的审核记录
     */
    public PageVO<ProgramApprovalDTO> getReviewerApprovals(Long reviewerId, 
                                                          ProgramApprovalStatusEnum status, 
                                                          int page, int size) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        log.debug("Getting reviewer approvals: reviewerId={}, status={}, oid={}, page={}, size={}", 
                reviewerId, status, userInfo.getOid(), page, size);
        
        return programApprovalService.getReviewerApprovals(reviewerId, status, userInfo.getOid(), page, size);
    }
    
    /**
     * 撤销审核申请
     */
    public boolean withdrawProgramApproval(Long approvalId) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        log.info("Withdrawing program approval: approvalId={}, userId={}, oid={}", 
                approvalId, userInfo.getUid(), userInfo.getOid());
        
        return programApprovalService.withdrawApproval(approvalId, userInfo.getUid(), userInfo.getOid());
    }

    // ===== 模板管理相关方法 =====

    /**
     * 保存节目为模板
     */
    public ProgramDTO saveAsTemplate(CreateProgramRequest request) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        log.info("Saving program as template: name={}, userId={}, oid={}, ugid={}", 
                request.getName(), userInfo.getUid(), userInfo.getOid(), userInfo.getUgid());

        return programService.saveAsTemplate(request, userInfo.getUid(), userInfo.getOid(), userInfo.getUgid());
    }

    /**
     * 更新模板
     */
    public ProgramDTO updateTemplate(Long templateId, UpdateProgramRequest request) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        log.info("Updating template: templateId={}, userId={}, oid={}", 
                templateId, userInfo.getUid(), userInfo.getOid());

        return programService.updateTemplate(templateId, request, userInfo.getUid(), userInfo.getOid());
    }

    /**
     * 模板列表（支持继承）
     */
    public PageVO<ProgramDTO> listTemplates(PageRequestDTO<QueryProgramListRequest> pageRequestDTO) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        
        log.debug("Listing templates: keyword={}, page={}, pageSize={}, oid={}, ugid={}", 
                pageRequestDTO.getParams().getKeyword(), 
                pageRequestDTO.getPageNum(),
                pageRequestDTO.getPageSize(),
                userInfo.getOid(), 
                userInfo.getUgid());

        // 调用Service层模板查询（支持继承）
        return programService.findTemplatesWithInheritance(
                userInfo.getOid(), 
                userInfo.getUgid(), 
                pageRequestDTO.getParams().getKeyword(),
                pageRequestDTO.getPageNum(), 
                pageRequestDTO.getPageSize());
    }

    // ===== 私有辅助方法 =====

    /**
     * SaveDraftRequest转换为UpdateDraftRequest
     */
    private org.nan.cloud.program.dto.request.UpdateDraftRequest convertToUpdateDraftRequest(SaveDraftRequest request) {
        return org.nan.cloud.program.dto.request.UpdateDraftRequest.builder()
                .name(request.getName())
                .description(request.getDescription())
                .width(request.getWidth())
                .height(request.getHeight())
                .duration(request.getDuration())
                .contentData(request.getContentData())
                .build();
    }

    /**
     * DraftDTO转换为ProgramDTO（用于统一接口返回）
     */
    private ProgramDTO convertDraftToProgram(DraftDTO draft) {
        ProgramDTO program = new ProgramDTO();
        program.setId(draft.getId());
        program.setName(draft.getName());
        program.setDescription(draft.getDescription());
        program.setStatus(ProgramStatusEnum.DRAFT);
        program.setWidth(draft.getWidth());
        program.setHeight(draft.getHeight());
        program.setDuration(draft.getDuration());
        program.setCurrentVersion(1); // 草稿固定版本1
        program.setCreatedTime(draft.getCreatedTime());
        program.setUpdatedTime(draft.getUpdatedTime());
        return program;
    }
}