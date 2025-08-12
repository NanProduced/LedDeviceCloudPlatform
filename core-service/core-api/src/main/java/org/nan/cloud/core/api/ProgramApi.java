package org.nan.cloud.core.api;

import org.nan.cloud.common.basic.model.PageRequestDTO;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.api.DTO.req.QueryProgramListRequest;
import org.nan.cloud.core.api.DTO.req.PendingApprovalForMeRequest;
import org.nan.cloud.core.api.DTO.req.InitiatedApprovalsByMeRequest;
import org.nan.cloud.core.api.DTO.req.AllApprovalsRequest;
import org.nan.cloud.program.dto.request.CreateProgramRequest;
import org.nan.cloud.program.dto.request.PublishDraftRequest;
import org.nan.cloud.program.dto.request.SaveDraftRequest;
import org.nan.cloud.program.dto.request.UpdateProgramRequest;
import org.nan.cloud.program.dto.response.DraftDTO;
import org.nan.cloud.program.dto.response.ProgramContentDTO;
import org.nan.cloud.program.dto.response.ProgramDTO;
import org.nan.cloud.program.dto.response.ProgramVersionDTO;
import org.nan.cloud.program.dto.request.ApprovalRequest;
import org.nan.cloud.program.dto.response.ProgramApprovalDTO;
import org.nan.cloud.program.enums.ProgramApprovalStatusEnum;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 节目管理API接口定义
 * 基于前端对接需求文档定义的10个核心端点
 * 完全兼容前端期望的响应格式和URL规范
 */
public interface ProgramApi {

    String PREFIX = "/program";

    /**
     * 创建节目
     */
    @PostMapping(PREFIX + "/create")
    ProgramDTO createProgram(@RequestBody CreateProgramRequest request);

    /**
     * 更新节目（覆盖最新）
     */
    @PutMapping(PREFIX + "/{programId}")
    ProgramDTO updateProgram(@PathVariable Long programId, 
                           @RequestBody UpdateProgramRequest request);

    /**
     * 保存草稿（不变更正式版本，保留独立版本号与编辑状态）
     */
    @PostMapping(PREFIX + "/draft")
    DraftDTO saveDraft(@RequestParam Long programId,
                      @RequestBody SaveDraftRequest request);

    /**
     * 获取节目详情（基础元数据）
     */
    @GetMapping(PREFIX + "/{programId}")
    ProgramDTO getProgramDetails(@PathVariable Long programId);

    /**
     * 获取节目内容（用于回显）
     * Query: versionId?（若不传则返回最新版本内容）
     */
    @GetMapping(PREFIX + "/{programId}/content")
    ProgramContentDTO getProgramContent(@PathVariable Long programId, 
                                      @RequestParam(required = false) Integer versionId);

    /**
     * 节目列表
     */
    @PostMapping(PREFIX + "/list")
    PageVO<ProgramDTO> listPrograms(@RequestBody PageRequestDTO<QueryProgramListRequest> pageRequestDTO);

    /**
     * 删除节目
     */
    @DeleteMapping(PREFIX + "/{programId}")
    ProgramDTO deleteProgram(@PathVariable Long programId);

    

    /**
     *  版本管理
     * 
     * POST /core/api/program/{programId}/versions/{versionId}/revert
     * 将指定版本内容恢复为"当前编辑版本"（不直接发布）
     * Resp.data: { programId, newVersionId }
     */
    @GetMapping(PREFIX + "/{programId}/versions")
    List<ProgramVersionDTO> getProgramVersions(@PathVariable Long programId);

    @PostMapping(PREFIX + "/{programId}/versions/{versionId}/revert")
    ProgramDTO revertToVersion(@PathVariable Long programId, 
                             @PathVariable Integer versionId);

    // ===== 节目审核相关API =====
    
    /**
     * 提交节目审核申请
     */
    @PostMapping(PREFIX + "/{programId}/versions/{versionId}/approval/submit")
    ProgramApprovalDTO submitProgramApproval(@PathVariable Long programId,
                                           @PathVariable Integer versionId);
    
    /**
     * 审核通过
     */
    @PostMapping(PREFIX + "/approval/{approvalId}/approve")
    boolean approveProgram(@PathVariable Long approvalId,
                                 @RequestBody ApprovalRequest request);
    
    /**
     * 审核拒绝
     */
    @PostMapping(PREFIX + "/approval/{approvalId}/reject")
    boolean rejectProgram(@PathVariable Long approvalId,
                                @RequestBody ApprovalRequest request);
    
    /**
     * 查询节目审核历史
     */
    @GetMapping(PREFIX + "/{programId}/approval/history")
    List<ProgramApprovalDTO> getProgramApprovalHistory(@PathVariable Long programId);
    
    /**
     * 查询节目版本审核状态
     */
    @GetMapping(PREFIX + "/{programId}/versions/{versionId}/approval")
    ProgramApprovalDTO getProgramVersionApproval(@PathVariable Long programId,
                                               @PathVariable Integer versionId);
    
    /**
     * 查询组织待审核列表 (已废弃 - 使用新的三维度接口)
     * @deprecated 使用 getPendingApprovalsForMe, getInitiatedApprovalsByMe, getAllApprovals 替代
     */
    @Deprecated
    @GetMapping(PREFIX + "/approval/pending")
    PageVO<ProgramApprovalDTO> getPendingApprovals(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size);
            
    /**
     * 查询待我审核的节目列表
     * 业务逻辑：节目的ugid属于当前用户组或子组，且审核状态为PENDING
     */
    @PostMapping(PREFIX + "/approval/pending-for-me")
    PageVO<ProgramApprovalDTO> getPendingApprovalsForMe(
            @RequestBody PageRequestDTO<PendingApprovalForMeRequest> request);
            
    /**
     * 查询我发起的审核申请列表  
     * 业务逻辑：创建者为当前用户ID的所有审核记录
     */
    @PostMapping(PREFIX + "/approval/initiated-by-me")
    PageVO<ProgramApprovalDTO> getInitiatedApprovalsByMe(
            @RequestBody PageRequestDTO<InitiatedApprovalsByMeRequest> request);
            
    /**
     * 查询全部审核记录
     * 业务逻辑：ugid属于当前用户组层级的所有审核记录
     */
    @PostMapping(PREFIX + "/approval/all")  
    PageVO<ProgramApprovalDTO> getAllApprovals(
            @RequestBody PageRequestDTO<AllApprovalsRequest> request);
    
    /**
     * 查询审核人员的审核记录
     */
    @GetMapping(PREFIX + "/approval/reviewer/{reviewerId}")
    PageVO<ProgramApprovalDTO> getReviewerApprovals(
            @PathVariable Long reviewerId,
            @RequestParam(required = false) ProgramApprovalStatusEnum status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size);
    
    /**
     * 撤销审核申请
     */
    @DeleteMapping(PREFIX + "/approval/{approvalId}")
    boolean withdrawProgramApproval(@PathVariable Long approvalId);

    // ===== 模板管理相关API =====

    /**
     * 保存为模板
     */
    @PostMapping(PREFIX + "/template/create")
    ProgramDTO saveAsTemplate(@RequestBody CreateProgramRequest request);

    /**
     * 更新模板
     */
    @PutMapping(PREFIX + "/template/{templateId}")
    ProgramDTO updateTemplate(@PathVariable Long templateId,
                            @RequestBody UpdateProgramRequest request);

    /**
     * 获取模板列表 
     * 支持继承（父级组可看到子组模板）
     */
    @PostMapping(PREFIX + "/template/list")
    PageVO<ProgramDTO> listTemplates(@RequestBody PageRequestDTO<QueryProgramListRequest> pageRequestDTO);
}