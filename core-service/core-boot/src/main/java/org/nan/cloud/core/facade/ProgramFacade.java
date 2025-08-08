package org.nan.cloud.core.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.model.PageRequestDTO;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.common.web.context.InvocationContextHolder;
import org.nan.cloud.common.web.context.RequestUserInfo;
import org.nan.cloud.core.api.DTO.req.QueryProgramListRequest;
import org.nan.cloud.core.service.ProgramDraftService;
import org.nan.cloud.core.service.ProgramService;
import org.nan.cloud.core.service.converter.ProgramDtoConverter;
import org.nan.cloud.program.document.ProgramContent;
import org.nan.cloud.program.dto.request.CreateProgramRequest;
import org.nan.cloud.program.dto.request.PublishDraftRequest;
import org.nan.cloud.program.dto.request.SaveDraftRequest;
import org.nan.cloud.program.dto.request.UpdateProgramRequest;
import org.nan.cloud.program.dto.response.DraftDTO;
import org.nan.cloud.program.dto.response.ProgramContentDTO;
import org.nan.cloud.program.dto.response.ProgramDTO;
import org.nan.cloud.program.dto.response.ProgramVersionDTO;
import org.nan.cloud.program.enums.ProgramStatusEnum;
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
     * 发布节目（从草稿转换为正式节目）
     */
    public ProgramDTO publishProgram(Long programId, PublishDraftRequest request) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        log.info("Publishing program: programId={}, userId={}, oid={}", 
                programId, userInfo.getUid(), userInfo.getOid());

        return programDraftService.publishDraft(programId, request, userInfo.getUid(), userInfo.getOid());
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

        // TODO: 实现获取节目内容逻辑
        // 1. 根据programId和versionId获取ProgramContent
        // 2. 转换为ProgramContentDTO
        // 3. 如果versionId为null，返回最新版本内容
        
        throw new UnsupportedOperationException("获取节目内容功能待实现");
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
     * 复制节目
     */
    public ProgramDTO copyProgram(Long programId) {
        RequestUserInfo userInfo = InvocationContextHolder.getContext().getRequestUser();
        log.info("Copying program: programId={}, userId={}, oid={}", 
                programId, userInfo.getUid(), userInfo.getOid());

        // TODO: 实现复制节目逻辑
        // 1. 获取源节目详情和内容
        // 2. 创建新节目（名称添加"副本"后缀）
        // 3. 复制所有内容和素材引用
        
        throw new UnsupportedOperationException("复制节目功能待实现");
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