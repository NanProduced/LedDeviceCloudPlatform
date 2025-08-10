package org.nan.cloud.core.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.model.PageRequestDTO;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.api.DTO.req.QueryProgramListRequest;
import org.nan.cloud.core.api.ProgramApi;
import org.nan.cloud.core.facade.ProgramFacade;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 节目管理控制器
 * 实现前端对接需求的10个核心API端点
 * 集成统一响应格式和异常处理
 */
@Slf4j
@Tag(name = "Program(节目管理)", description = "节目创建、编辑、发布、版本管理等核心功能")
@RestController
@RequiredArgsConstructor
public class ProgramController implements ProgramApi {

    private final ProgramFacade programFacade;

    @Operation(
            summary = "创建节目",
            description = "创建新的节目，支持vsnData和contentData双数据结构",
            tags = {"节目管理"}
    )
    @Override
    public ProgramDTO createProgram(@RequestBody @Validated CreateProgramRequest request) {
        log.info("REST API: Creating program with name: {}", request.getName());
        return programFacade.createProgram(request);
    }

    @Operation(
            summary = "更新节目",
            description = "更新现有节目，创建新版本进入审核流程",
            tags = {"节目管理"}
    )
    @Override
    public ProgramDTO updateProgram(@PathVariable Long programId, 
                                  @RequestBody @Validated UpdateProgramRequest request) {
        log.info("REST API: Updating program: {}", programId);
        return programFacade.updateProgram(programId, request);
    }

    @Operation(
            summary = "保存草稿",
            description = "保存节目编辑器状态为草稿，不进入审核流程",
            tags = {"节目管理", "草稿管理"}
    )
    @Override
    public DraftDTO saveDraft(@PathVariable Long programId, 
                            @RequestBody @Validated SaveDraftRequest request) {
        log.info("REST API: Saving draft for program: {}", programId);
        return programFacade.saveDraft(programId, request);
    }

    @Operation(
            summary = "获取节目详情",
            description = "获取节目基础元数据信息",
            tags = {"节目管理", "节目查询"}
    )
    @Override
    public ProgramDTO getProgramDetails(@PathVariable Long programId) {
        log.debug("REST API: Getting program details: {}", programId);
        return programFacade.getProgramDetails(programId);
    }

    @Operation(
            summary = "获取节目内容",
            description = "获取节目编辑器内容数据，用于前端回显",
            tags = {"节目管理", "节目查询"}
    )
    @Override
    public ProgramContentDTO getProgramContent(@PathVariable Long programId, 
                                             @RequestParam(required = false) Integer versionId) {
        log.debug("REST API: Getting program content: {}, version: {}", programId, versionId);
        return programFacade.getProgramContent(programId, versionId);
    }

    @Operation(
            summary = "节目列表",
            description = "分页查询节目列表，支持关键词和状态过滤",
            tags = {"节目管理", "节目查询"}
    )
    @Override
    public PageVO<ProgramDTO> listPrograms(@RequestBody PageRequestDTO<QueryProgramListRequest> pageRequestDTO) {
        log.debug("REST API: Listing programs with keyword: {}, status: {}", pageRequestDTO.getParams().getKeyword(), pageRequestDTO.getParams().getStatus());
        return programFacade.listPrograms(pageRequestDTO);
    }

    @Operation(
            summary = "删除节目",
            description = "删除指定节目（软删除）",
            tags = {"节目管理"}
    )
    @Override
    public ProgramDTO deleteProgram(@PathVariable Long programId) {
        log.info("REST API: Deleting program: {}", programId);
        return programFacade.deleteProgram(programId);
    }

    

    @Operation(
            summary = "获取节目版本列表",
            description = "获取节目的所有版本历史",
            tags = {"版本管理"}
    )
    @Override
    public List<ProgramVersionDTO> getProgramVersions(@PathVariable Long programId) {
        log.debug("REST API: Getting program versions: {}", programId);
        return programFacade.getProgramVersions(programId);
    }

    @Operation(
            summary = "版本回滚",
            description = "将指定版本内容恢复为当前编辑版本",
            tags = {"版本管理"}
    )
    @Override
    public ProgramDTO revertToVersion(@PathVariable Long programId, 
                                    @PathVariable Integer versionId) {
        log.info("REST API: Reverting program {} to version: {}", programId, versionId);
        return programFacade.revertToVersion(programId, versionId);
    }

    // ===== 节目审核相关API实现 =====

    @Operation(
            summary = "提交节目审核申请",
            description = "提交指定版本的节目审核申请",
            tags = {"节目审核"}
    )
    @Override
    public ProgramApprovalDTO submitProgramApproval(@PathVariable Long programId,
                                                  @PathVariable Integer versionId) {
        log.info("REST API: Submitting program approval - programId: {}, versionId: {}", programId, versionId);
        return programFacade.submitProgramApproval(programId, versionId);
    }

    @Operation(
            summary = "审核通过",
            description = "审核通过指定的节目申请",
            tags = {"节目审核"}
    )
    @Override
    public boolean approveProgramApproval(@PathVariable Long approvalId,
                                        @RequestBody @Validated ApprovalRequest request) {
        log.info("REST API: Approving program - approvalId: {}", approvalId);
        return programFacade.approveProgramApproval(approvalId, request);
    }

    @Operation(
            summary = "审核拒绝",
            description = "审核拒绝指定的节目申请",
            tags = {"节目审核"}
    )
    @Override
    public boolean rejectProgramApproval(@PathVariable Long approvalId,
                                       @RequestBody @Validated ApprovalRequest request) {
        log.info("REST API: Rejecting program - approvalId: {}", approvalId);
        return programFacade.rejectProgramApproval(approvalId, request);
    }

    @Operation(
            summary = "查询节目审核历史",
            description = "查询指定节目的所有审核记录",
            tags = {"节目审核"}
    )
    @Override
    public List<ProgramApprovalDTO> getProgramApprovalHistory(@PathVariable Long programId) {
        log.debug("REST API: Getting program approval history - programId: {}", programId);
        return programFacade.getProgramApprovalHistory(programId);
    }

    @Operation(
            summary = "查询节目版本审核状态",
            description = "查询指定节目版本的审核状态",
            tags = {"节目审核"}
    )
    @Override
    public ProgramApprovalDTO getProgramVersionApproval(@PathVariable Long programId,
                                                      @PathVariable Integer versionId) {
        log.debug("REST API: Getting program version approval - programId: {}, versionId: {}", 
                programId, versionId);
        return programFacade.getProgramVersionApproval(programId, versionId);
    }

    @Operation(
            summary = "查询组织待审核列表",
            description = "分页查询当前组织下的待审核节目列表",
            tags = {"节目审核"}
    )
    @Override
    public PageVO<ProgramApprovalDTO> getPendingApprovals(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        log.debug("REST API: Getting pending approvals - page: {}, size: {}", page, size);
        return programFacade.getPendingApprovals(page, size);
    }

    @Operation(
            summary = "查询审核人员的审核记录",
            description = "分页查询指定审核人员的审核记录",
            tags = {"节目审核"}
    )
    @Override
    public PageVO<ProgramApprovalDTO> getReviewerApprovals(@PathVariable Long reviewerId,
                                                          @RequestParam(required = false) ProgramApprovalStatusEnum status,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        log.debug("REST API: Getting reviewer approvals - reviewerId: {}, status: {}, page: {}, size: {}", 
                reviewerId, status, page, size);
        return programFacade.getReviewerApprovals(reviewerId, status, page, size);
    }

    @Operation(
            summary = "撤销审核申请",
            description = "撤销指定的节目审核申请",
            tags = {"节目审核"}
    )
    @Override
    public boolean withdrawProgramApproval(@PathVariable Long approvalId) {
        log.info("REST API: Withdrawing program approval - approvalId: {}", approvalId);
        return programFacade.withdrawProgramApproval(approvalId);
    }
}