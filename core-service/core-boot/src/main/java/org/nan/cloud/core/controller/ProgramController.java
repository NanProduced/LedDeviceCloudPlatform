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
            tags = {"草稿管理"}
    )
    @Override
    public DraftDTO saveDraft(@PathVariable Long programId, 
                            @RequestBody @Validated SaveDraftRequest request) {
        log.info("REST API: Saving draft for program: {}", programId);
        return programFacade.saveDraft(programId, request);
    }

    @Operation(
            summary = "发布节目",
            description = "将草稿转换为正式节目并进入审核流程",
            tags = {"节目管理"}
    )
    @Override
    public ProgramDTO publishProgram(@PathVariable Long programId, 
                                   @RequestBody @Validated PublishDraftRequest request) {
        log.info("REST API: Publishing program: {}", programId);
        return programFacade.publishProgram(programId, request);
    }

    @Operation(
            summary = "获取节目详情",
            description = "获取节目基础元数据信息",
            tags = {"节目查询"}
    )
    @Override
    public ProgramDTO getProgramDetails(@PathVariable Long programId) {
        log.debug("REST API: Getting program details: {}", programId);
        return programFacade.getProgramDetails(programId);
    }

    @Operation(
            summary = "获取节目内容",
            description = "获取节目编辑器内容数据，用于前端回显",
            tags = {"节目查询"}
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
            tags = {"节目查询"}
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
            summary = "复制节目",
            description = "复制现有节目创建新节目",
            tags = {"节目管理"}
    )
    @Override
    public ProgramDTO copyProgram(@PathVariable Long programId) {
        log.info("REST API: Copying program: {}", programId);
        return programFacade.copyProgram(programId);
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
}