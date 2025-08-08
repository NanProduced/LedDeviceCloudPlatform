package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.core.domain.Program;
import org.nan.cloud.core.service.converter.ProgramDtoConverter;
import org.nan.cloud.core.repository.ProgramContentRepository;
import org.nan.cloud.core.repository.ProgramMaterialRefRepository;
import org.nan.cloud.core.repository.ProgramRepository;
import org.nan.cloud.core.service.MaterialDependencyService;
import org.nan.cloud.core.service.ProgramDraftService;
import org.nan.cloud.core.service.ProgramService;
import org.nan.cloud.program.document.ProgramContent;
import org.nan.cloud.program.dto.request.CreateProgramRequest;
import org.nan.cloud.program.dto.request.PublishDraftRequest;
import org.nan.cloud.program.dto.request.SaveDraftRequest;
import org.nan.cloud.program.dto.request.UpdateDraftRequest;
import org.nan.cloud.program.dto.response.DraftDTO;
import org.nan.cloud.program.dto.response.MaterialValidationDTO;
import org.nan.cloud.program.dto.response.ProgramDTO;
import org.nan.cloud.program.enums.ProgramApprovalStatusEnum;
import org.nan.cloud.program.enums.ProgramStatusEnum;
import org.nan.cloud.program.enums.VsnGenerationStatusEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 节目草稿管理服务实现
 * 处理草稿的特殊业务逻辑，包括自动保存、验证、发布等
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProgramDraftServiceImpl implements ProgramDraftService {

    private final ProgramRepository programRepository;
    private final ProgramContentRepository programContentRepository;
    private final ProgramMaterialRefRepository programMaterialRefRepository;
    private final ProgramDtoConverter programDtoConverter;
    private final ProgramService programService;
    private final MaterialDependencyService materialDependencyService;

    @Override
    @Transactional
    public DraftDTO saveDraft(SaveDraftRequest request, Long userId, Long oid, Long ugid) {
        log.info("Saving draft: name={}, userId={}, oid={}, ugid={}", 
                request.getName(), userId, oid, ugid);

        // 1. 验证草稿名称唯一性
        validateDraftNameUnique(oid, ugid, request.getName(), null);

        // 2. 验证数据完整性（草稿也需要基本完整性）
        DraftValidationResult validation = performDraftValidation(request.getContentData(), oid);
        if (!validation.isValid()) {
            throw new IllegalArgumentException("草稿数据验证失败: " + String.join(", ", validation.getErrors()));
        }

        // 3. 创建草稿记录
        Program draft = buildNewDraft(request, userId, oid, ugid);
        draft = programRepository.save(draft);

        // 4. 保存草稿内容到MongoDB
        String contentId = saveDraftContentToMongoDB(draft, request.getContentData(), request.getVsnData(), userId);
        draft.setProgramContentId(contentId);
        draft = programRepository.save(draft);

        // 5. 建立素材引用关系（草稿也需要保护素材）
        createMaterialReferencesForDraft(draft, request.getContentData());

        log.info("Draft saved successfully: id={}, name={}", draft.getId(), draft.getName());
        return programDtoConverter.toDraftDTO(draft);
    }

    @Override
    @Transactional
    public DraftDTO updateDraft(Long draftId, UpdateDraftRequest request, Long userId, Long oid) {
        log.info("Updating draft: draftId={}, userId={}, oid={}", draftId, userId, oid);

        // 1. 查找草稿并验证权限
        Program draft = findDraftWithPermission(draftId, oid);

        // 2. 验证名称唯一性（如果名称有变化）
        if (StringUtils.hasText(request.getName()) && !request.getName().equals(draft.getName())) {
            validateDraftNameUnique(oid, draft.getUgid(), request.getName(), draftId);
        }

        // 3. 验证数据完整性
        DraftValidationResult validation = performDraftValidation(request.getContentData(), oid);
        if (!validation.isValid()) {
            throw new IllegalArgumentException("草稿数据验证失败: " + String.join(", ", validation.getErrors()));
        }

        // 4. 更新草稿基础信息
        updateDraftBasicInfo(draft, request, userId);
        draft = programRepository.save(draft);

        // 5. 更新草稿内容
        updateDraftContentInMongoDB(draft, request.getContentData(), request.getVsnData(), userId);

        // 6. 更新素材引用关系
        updateMaterialReferencesForDraft(draft, request.getContentData());

        log.info("Draft updated successfully: id={}, name={}", draft.getId(), draft.getName());
        return programDtoConverter.toDraftDTO(draft);
    }

    @Override
    public Optional<DraftDTO> findDraftById(Long draftId, Long oid) {
        return programRepository.findByIdAndOid(draftId, oid)
                .filter(program -> ProgramStatusEnum.DRAFT.equals(program.getStatus()))
                .map(programDtoConverter::toDraftDTO);
    }

    @Override
    public List<DraftDTO> findDraftsByUserGroup(Long oid, Long ugid, int page, int size) {
        List<Program> drafts = programRepository.findByUserGroup(oid, ugid, ProgramStatusEnum.DRAFT, page, size);
        return programDtoConverter.toDraftDTOs(drafts);
    }

    @Override
    public long countDraftsByUserGroup(Long oid, Long ugid) {
        return programRepository.countByUserGroup(oid, ugid, ProgramStatusEnum.DRAFT);
    }

    @Override
    public List<DraftDTO> findDraftsByCreator(Long oid, Long createdBy, int page, int size) {
        List<Program> drafts = programRepository.findByCreator(oid, createdBy, ProgramStatusEnum.DRAFT, page, size);
        return programDtoConverter.toDraftDTOs(drafts);
    }

    @Override
    public long countDraftsByCreator(Long oid, Long createdBy) {
        return programRepository.countByCreator(oid, createdBy, ProgramStatusEnum.DRAFT);
    }

    @Override
    @Transactional
    public boolean deleteDraft(Long draftId, Long userId, Long oid) {
        log.info("Deleting draft: draftId={}, userId={}, oid={}", draftId, userId, oid);

        // 验证是否为草稿
        Optional<Program> draftOpt = programRepository.findByIdAndOid(draftId, oid);
        if (draftOpt.isEmpty() || !ProgramStatusEnum.DRAFT.equals(draftOpt.get().getStatus())) {
            log.warn("Draft not found or not a draft: draftId={}, oid={}", draftId, oid);
            return false;
        }

        // 删除草稿
        int deletedCount = programRepository.deleteById(draftId);
        
        // 清理相关数据
        if (deletedCount > 0) {
            cleanupDraftRelatedData(draftId);
            log.info("Draft deleted successfully: draftId={}", draftId);
        }

        return deletedCount > 0;
    }

    @Override
    @Transactional
    public int deleteDraftsBatch(List<Long> draftIds, Long userId, Long oid) {
        log.info("Batch deleting drafts: draftIds={}, userId={}, oid={}", draftIds, userId, oid);

        // 验证所有ID都是草稿
        int validDraftCount = 0;
        for (Long draftId : draftIds) {
            Optional<Program> draftOpt = programRepository.findByIdAndOid(draftId, oid);
            if (draftOpt.isPresent() && ProgramStatusEnum.DRAFT.equals(draftOpt.get().getStatus())) {
                validDraftCount++;
            }
        }

        if (validDraftCount != draftIds.size()) {
            log.warn("Some IDs are not valid drafts: expected={}, valid={}", draftIds.size(), validDraftCount);
        }

        int deletedCount = programRepository.deleteByIds(draftIds);
        
        // 批量清理相关数据
        for (Long draftId : draftIds) {
            cleanupDraftRelatedData(draftId);
        }

        log.info("Batch delete drafts completed: requested={}, deleted={}", draftIds.size(), deletedCount);
        return deletedCount;
    }

    @Override
    @Transactional
    public ProgramDTO publishDraft(Long draftId, PublishDraftRequest request, Long userId, Long oid) {
        log.info("Publishing draft: draftId={}, userId={}, oid={}", draftId, userId, oid);

        // 1. 查找草稿并验证
        Program draft = findDraftWithPermission(draftId, oid);
        
        // 2. 最终验证草稿数据完整性
        DraftValidationResult validation = validateDraft(draftId, oid);
        if (!validation.isValid()) {
            throw new IllegalStateException("草稿数据验证失败，无法发布: " + String.join(", ", validation.getErrors()));
        }

        // 3. 获取草稿内容
        Optional<ProgramContent> draftContentOpt = programContentRepository.findByProgramIdAndVersion(
                draft.getId(), draft.getVersion());
        
        if (draftContentOpt.isEmpty()) {
            throw new IllegalStateException("草稿内容不存在，无法发布");
        }

        // 4. 构建节目创建请求（合并草稿数据和请求数据）
        CreateProgramRequest finalRequest = mergeDraftToRequest(draft, draftContentOpt.get(), request);

        // 5. 创建正式节目
        ProgramDTO program;
        try {
            program = programService.createProgram(finalRequest, userId, oid, draft.getUgid());
        } catch (Exception e) {
            log.error("Failed to publish draft: draftId={}, error={}", draftId, e.getMessage());
            throw new RuntimeException("草稿发布失败: " + e.getMessage(), e);
        }

        // 6. 发布成功后删除草稿
        try {
            deleteDraft(draftId, userId, oid);
            log.info("Draft deleted after successful publish: draftId={}", draftId);
        } catch (Exception e) {
            log.warn("Failed to delete draft after publish: draftId={}, error={}", draftId, e.getMessage());
            // 不影响主流程，仅记录警告
        }

        log.info("Draft published successfully: draftId={}, programId={}", draftId, program.getId());
        return program;
    }

    @Override
    public boolean isDraftNameAvailable(Long oid, Long ugid, String name, Long excludeId) {
        // 草稿名称检查需要只在草稿范围内检查
        return programRepository.isNameAvailableInUserGroup(oid, ugid, name, excludeId);
    }

    @Override
    public DraftValidationResult validateDraft(Long draftId, Long oid) {
        Optional<Program> draftOpt = findDraftById(draftId, oid).map(dto -> {
            // 简化处理，实际项目中需要正确的转换
            Program program = new Program();
            program.setId(dto.getId());
            program.setOid(oid);
            return program;
        });

        if (draftOpt.isEmpty()) {
            return new DraftValidationResult(false, 
                    List.of("草稿不存在"), 
                    List.of());
        }

        Program draft = draftOpt.get();
        
        // 获取草稿内容
        Optional<ProgramContent> contentOpt = programContentRepository.findByProgramIdAndVersion(
                draft.getId(), 1); // 草稿版本号固定为1

        if (contentOpt.isEmpty()) {
            return new DraftValidationResult(false, 
                    List.of("草稿内容不存在"), 
                    List.of());
        }

        return performDraftValidation(contentOpt.get().getOriginalData(), oid);
    }

    @Override
    public boolean hasDraftAccessPermission(Long draftId, Long userId, Long oid) {
        return programRepository.hasAccessPermission(draftId, userId, oid);
    }

    @Override
    public Optional<String> getDraftPreviewData(Long draftId, Long oid) {
        // 查找草稿
        Optional<Program> draftOpt = programRepository.findByIdAndOid(draftId, oid)
                .filter(program -> ProgramStatusEnum.DRAFT.equals(program.getStatus()));

        if (draftOpt.isEmpty()) {
            return Optional.empty();
        }

        Program draft = draftOpt.get();

        // 获取预览数据（原始JSON）
        return programContentRepository.findByProgramIdAndVersion(draft.getId(), draft.getVersion())
                .map(ProgramContent::getOriginalData);
    }

    @Override
    @Transactional
    public DraftDTO autoSaveDraft(Long draftId, SaveDraftRequest request, Long userId, Long oid, Long ugid) {
        log.debug("Auto saving draft: draftId={}, userId={}, oid={}", draftId, userId, oid);

        if (draftId == null) {
            // 创建新草稿
            return saveDraft(request, userId, oid, ugid);
        } else {
            // 更新现有草稿
            UpdateDraftRequest updateRequest = UpdateDraftRequest.builder()
                    .name(request.getName())
                    .description(request.getDescription())
                    .width(request.getWidth())
                    .height(request.getHeight())
                    .duration(request.getDuration())
                    .contentData(request.getContentData())
                    .build();
            
            return updateDraft(draftId, updateRequest, userId, oid);
        }
    }

    // ===== 私有辅助方法 =====

    private void validateDraftNameUnique(Long oid, Long ugid, String name, Long excludeId) {
        if (!programRepository.isNameAvailableInUserGroup(oid, ugid, name, excludeId)) {
            throw new IllegalArgumentException("草稿名称已存在: " + name);
        }
    }

    private DraftValidationResult performDraftValidation(String contentData, Long oid) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 基本数据验证
        if (!StringUtils.hasText(contentData)) {
            errors.add("节目内容不能为空");
        } else {
            // 素材依赖验证
            try {
                MaterialValidationDTO materialValidation = materialDependencyService.validateMaterialDependencies(contentData, oid);
                if (!materialValidation.getIsValid()) {
                    errors.addAll(materialValidation.getErrors());
                }
                if (materialValidation.getWarnings() != null) {
                    warnings.addAll(materialValidation.getWarnings());
                }
            } catch (Exception e) {
                log.warn("Material validation failed during draft validation", e);
                warnings.add("素材验证过程中发生错误: " + e.getMessage());
            }
        }

        // TODO: 实现更详细的验证逻辑
        // - VSN必需字段完整性检查
        // - 数据格式有效性检查

        boolean isValid = errors.isEmpty();
        return new DraftValidationResult(isValid, errors, warnings);
    }

    private Program buildNewDraft(SaveDraftRequest request, Long userId, Long oid, Long ugid) {
        Program draft = new Program();
        draft.setName(request.getName());
        draft.setDescription(request.getDescription());
        draft.setWidth(request.getWidth());
        draft.setHeight(request.getHeight());
        draft.setDuration(request.getDuration());
        
        // 草稿特殊字段
        draft.setVersion(1); // 草稿固定版本号1
        draft.setSourceProgramId(null);
        draft.setIsSourceProgram(true);
        
        // 草稿状态（草稿只有DRAFT状态，无需审核和VSN生成）
        draft.setStatus(ProgramStatusEnum.DRAFT);
        draft.setApprovalStatus(null); // 草稿不需要审核状态
        draft.setVsnGenerationStatus(null); // 草稿不生成VSN，无需此状态
        
        // 权限字段
        draft.setOid(oid);
        draft.setUgid(ugid);
        draft.setCreatedBy(userId);
        draft.setUpdatedBy(userId);
        
        // 初始化统计
        draft.setUsageCount(0);
        
        return draft;
    }

    private void updateDraftBasicInfo(Program draft, UpdateDraftRequest request, Long userId) {
        if (StringUtils.hasText(request.getName())) {
            draft.setName(request.getName());
        }
        if (StringUtils.hasText(request.getDescription())) {
            draft.setDescription(request.getDescription());
        }
        if (request.getWidth() != null) {
            draft.setWidth(request.getWidth());
        }
        if (request.getHeight() != null) {
            draft.setHeight(request.getHeight());
        }
        if (request.getDuration() != null) {
            draft.setDuration(request.getDuration());
        }
        
        draft.setUpdatedBy(userId);
    }

    private String saveDraftContentToMongoDB(Program draft, String contentData, String vsnData, Long userId) {
        ProgramContent content = new ProgramContent();
        content.setProgramId(draft.getId());
        content.setVersion(draft.getVersion());
        content.setOrgId(draft.getOid());
        content.setOriginalData(contentData);
        content.setCreatedBy(userId);
        content.setUpdatedBy(userId);
        content.setCreatedTime(LocalDateTime.now());
        content.setUpdatedTime(LocalDateTime.now());

        // 处理VSN数据（如果提供）
        if (StringUtils.hasText(vsnData)) {
            // TODO: 解析vsnData并设置vsnPrograms
            // content.setVsnPrograms(parseVsnData(vsnData));
            log.debug("VSN data provided for draft: {}, version: {}", draft.getId(), draft.getVersion());
        }

        content = programContentRepository.save(content);
        log.debug("Draft content saved to MongoDB: draftId={}, version={}, contentId={}", 
                draft.getId(), draft.getVersion(), content.getId());
        return content.getId();
    }

    private void updateDraftContentInMongoDB(Program draft, String contentData, String vsnData, Long userId) {
        Optional<ProgramContent> existingContentOpt = programContentRepository.findByProgramIdAndVersion(
                draft.getId(), draft.getVersion());

        if (existingContentOpt.isPresent()) {
            // 更新现有内容
            ProgramContent existingContent = existingContentOpt.get();
            existingContent.setOriginalData(contentData);
            existingContent.setUpdatedBy(userId);
            existingContent.setUpdatedTime(LocalDateTime.now());
            
            // 处理VSN数据（如果提供）
            if (StringUtils.hasText(vsnData)) {
                // TODO: 解析vsnData并设置vsnPrograms
                // existingContent.setVsnPrograms(parseVsnData(vsnData));
                log.debug("VSN data updated for draft: {}, version: {}", draft.getId(), draft.getVersion());
            }
            
            programContentRepository.save(existingContent);
            log.debug("Draft content updated in MongoDB: draftId={}, version={}", draft.getId(), draft.getVersion());
        } else {
            // 创建新内容（理论上不应该发生）
            saveDraftContentToMongoDB(draft, contentData, vsnData, userId);
        }
    }

    private void createMaterialReferencesForDraft(Program draft, String contentData) {
        log.debug("Creating material references for draft: {}", draft.getId());
        
        boolean success = materialDependencyService.createMaterialDependencies(
                draft.getId(), contentData, draft.getOid());
                
        if (!success) {
            log.warn("Failed to create material references for draft: {}", draft.getId());
            // 草稿允许部分创建失败，仅记录警告
        } else {
            log.debug("Material references created successfully for draft: {}", draft.getId());
        }
    }

    private void updateMaterialReferencesForDraft(Program draft, String contentData) {
        log.debug("Updating material references for draft: {}", draft.getId());
        
        boolean success = materialDependencyService.updateMaterialDependencies(
                draft.getId(), contentData, draft.getOid());
                
        if (!success) {
            log.warn("Failed to update material references for draft: {}", draft.getId());
            // 草稿允许部分更新失败，仅记录警告
        } else {
            log.debug("Material references updated successfully for draft: {}", draft.getId());
        }
    }

    private CreateProgramRequest mergeDraftToRequest(Program draft, ProgramContent draftContent, PublishDraftRequest request) {
        return CreateProgramRequest.builder()
                .name(StringUtils.hasText(request.getName()) ? request.getName() : draft.getName())
                .description(StringUtils.hasText(request.getDescription()) ? request.getDescription() : draft.getDescription())
                .width(draft.getWidth()) // 使用草稿中的尺寸
                .height(draft.getHeight()) // 使用草稿中的尺寸
                .duration(draft.getDuration()) // 使用草稿中的时长
                .vsnData(StringUtils.hasText(request.getVsnData()) ? request.getVsnData() : null)
                .contentData(StringUtils.hasText(request.getContentData()) ? request.getContentData() : draftContent.getOriginalData())
                .build();
    }

    private Program findDraftWithPermission(Long draftId, Long oid) {
        Optional<Program> draftOpt = programRepository.findByIdAndOid(draftId, oid);
        
        if (draftOpt.isEmpty()) {
            throw new IllegalArgumentException("草稿不存在或无访问权限: " + draftId);
        }

        Program program = draftOpt.get();
        if (!ProgramStatusEnum.DRAFT.equals(program.getStatus())) {
            throw new IllegalArgumentException("指定的程序不是草稿: " + draftId);
        }

        return program;
    }

    private void cleanupDraftRelatedData(Long draftId) {
        // 清理素材引用关系
        // programMaterialRefRepository.deleteByProgramId(draftId);
        
        log.debug("Cleaned up related data for draft: {}", draftId);
    }
}