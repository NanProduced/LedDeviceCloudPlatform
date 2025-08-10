package org.nan.cloud.core.api;

import org.nan.cloud.common.basic.model.PageRequestDTO;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.api.DTO.req.QueryProgramListRequest;
import org.nan.cloud.program.dto.request.CreateProgramRequest;
import org.nan.cloud.program.dto.request.PublishDraftRequest;
import org.nan.cloud.program.dto.request.SaveDraftRequest;
import org.nan.cloud.program.dto.request.UpdateProgramRequest;
import org.nan.cloud.program.dto.response.DraftDTO;
import org.nan.cloud.program.dto.response.ProgramContentDTO;
import org.nan.cloud.program.dto.response.ProgramDTO;
import org.nan.cloud.program.dto.response.ProgramVersionDTO;
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
     * 1) 创建节目
     * POST /core/api/program/create
     * Resp.data: { programId, versionId, status }
     */
    @PostMapping(PREFIX + "/create")
    ProgramDTO createProgram(@RequestBody CreateProgramRequest request);

    /**
     * 2) 更新节目（覆盖最新）
     * PUT /core/api/program/{programId}
     * Resp.data: { programId, versionId }
     */
    @PutMapping(PREFIX + "/{programId}")
    ProgramDTO updateProgram(@PathVariable Long programId, 
                           @RequestBody UpdateProgramRequest request);

    /**
     * 3) 保存草稿（不变更正式版本，保留独立版本号与编辑状态）
     * POST /core/api/program/{programId}/draft
     * Resp.data: { programId, draftVersionId, savedAt }
     */
    @PostMapping(PREFIX + "/{programId}/draft")
    DraftDTO saveDraft(@PathVariable Long programId, 
                      @RequestBody SaveDraftRequest request);

    /**
     * 5) 获取节目详情（基础元数据）
     * GET /core/api/program/{programId}
     * Resp.data: { programId, name, description, width, height, duration, status, thumbnailUrl, latestVersionId, createdAt, updatedAt }
     */
    @GetMapping(PREFIX + "/{programId}")
    ProgramDTO getProgramDetails(@PathVariable Long programId);

    /**
     * 6) 获取节目内容（用于回显）
     * GET /core/api/program/{programId}/content
     * Query: versionId?（若不传则返回最新版本内容）
     * Resp.data: { contentData, vsnData }
     */
    @GetMapping(PREFIX + "/{programId}/content")
    ProgramContentDTO getProgramContent(@PathVariable Long programId, 
                                      @RequestParam(required = false) Integer versionId);

    /**
     * 7) 节目列表
     * GET /core/api/program/list?keyword=&status=&page=&pageSize=&sortBy=&sortOrder=
     * Resp.data: { items: ProgramSummary[], total, page, pageSize }
     */
    @GetMapping(PREFIX + "/list")
    PageVO<ProgramDTO> listPrograms(@RequestBody PageRequestDTO<QueryProgramListRequest> pageRequestDTO);

    /**
     * 8) 删除节目
     * DELETE /core/api/program/{programId}
     * Resp.data: { programId, deleted: true }
     */
    @DeleteMapping(PREFIX + "/{programId}")
    ProgramDTO deleteProgram(@PathVariable Long programId);

    

    /**
     * 10) 版本管理
     * GET /core/api/program/{programId}/versions
     * Resp.data: { items: [{versionId, type: "draft|release", status, createdAt, remark?}], total }
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
}