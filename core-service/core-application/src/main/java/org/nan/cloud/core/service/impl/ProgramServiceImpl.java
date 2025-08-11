package org.nan.cloud.core.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.common.basic.utils.JsonUtils;
import org.nan.cloud.core.domain.Program;
import org.nan.cloud.core.event.mq.VsnGenerationRequestEvent;
import org.nan.cloud.core.service.converter.ProgramDtoConverter;
import org.nan.cloud.core.repository.ProgramApprovalRepository;
import org.nan.cloud.core.repository.ProgramContentRepository;
import org.nan.cloud.core.repository.ProgramMaterialRefRepository;
import org.nan.cloud.core.repository.ProgramRepository;
import org.nan.cloud.core.service.MaterialDependencyService;
import org.nan.cloud.core.service.ProgramService;
import org.nan.cloud.core.service.ProgramApprovalService;
import org.nan.cloud.core.service.VsnEventPublisher;
import org.nan.cloud.program.document.ProgramContent;
import org.nan.cloud.program.document.VsnProgram;
import org.nan.cloud.program.dto.request.CreateProgramRequest;
import org.nan.cloud.program.dto.request.UpdateProgramRequest;
import org.nan.cloud.program.dto.response.MaterialValidationDTO;
import org.nan.cloud.program.dto.response.ProgramDTO;
import org.nan.cloud.program.dto.response.ProgramDetailDTO;
import org.nan.cloud.program.dto.response.ProgramVersionDTO;
import org.nan.cloud.program.enums.ProgramApprovalStatusEnum;
import org.nan.cloud.program.enums.ProgramStatusEnum;
import org.nan.cloud.program.enums.VsnGenerationStatusEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 节目管理服务实现
 * 实现节目的核心业务逻辑，包括版本控制、VSN生成等
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProgramServiceImpl implements ProgramService {

    private final ProgramRepository programRepository;
    private final ProgramContentRepository programContentRepository;
    private final ProgramDtoConverter programDtoConverter;
    private final VsnEventPublisher vsnEventPublisher;
    private final MaterialDependencyService materialDependencyService;
    private final ProgramApprovalService programApprovalService;

    @Override
    @Transactional
    public ProgramDTO createProgram(CreateProgramRequest request, Long userId, Long oid, Long ugid) {
        log.info("Creating program: name={}, userId={}, oid={}, ugid={}", 
                request.getName(), userId, oid, ugid);

        // 1. 验证节目名称唯一性
        validateProgramNameUnique(oid, ugid, request.getName(), null);

        // 2. 验证素材依赖关系
        validateMaterialDependencies(request.getContentData(), oid);

        // 3. 创建节目基础记录（版本1）
        Program program = buildNewProgram(request, userId, oid, ugid);
        program = programRepository.save(program);

        // 4. 保存节目内容到MongoDB
        String contentId = saveContentToMongoDB(program, request.getContentData(), request.getVsnData(), userId);
        program.setProgramContentId(contentId);
        program = programRepository.save(program);

        // 5. 建立素材引用关系
        createMaterialReferences(program, request.getContentData());

        // 6. 发送VSN生成请求
        publishVsnGenerationRequest(program, VsnGenerationRequestEvent.EventType.GENERATE);

        // 7. 自动提交审核申请（节目创建后自动进入审核流程）
        createApprovalRecord(program.getId(), program.getVersion(), userId, oid);

        log.info("Program created successfully: id={}, name={}", program.getId(), program.getName());
        return programDtoConverter.toProgramDTO(program);
    }

    @Override
    @Transactional
    public ProgramDTO updateProgram(Long programId, UpdateProgramRequest request, Long userId, Long oid) {
        log.info("Updating program: programId={}, userId={}, oid={}", programId, userId, oid);

        // 1. 查找原节目并验证权限
        Program originalProgram = findProgramWithPermission(programId, oid);

        // 2. 验证节目名称唯一性（如果名称有变化）
        if (StringUtils.hasText(request.getName()) && !request.getName().equals(originalProgram.getName())) {
            validateProgramNameUnique(oid, originalProgram.getUgid(), request.getName(), null);
        }

        // 3. 验证素材依赖关系
        validateMaterialDependencies(request.getContentData(), oid);

        // 4. 创建新版本（版本控制核心逻辑）
        Program newVersionProgram = createNewVersionFromExisting(originalProgram, request, userId);
        newVersionProgram = programRepository.save(newVersionProgram);

        // 5. 保存新版本内容到MongoDB
        String contentId = saveContentToMongoDB(newVersionProgram, request.getContentData(), request.getVsnData(), userId);
        newVersionProgram.setProgramContentId(contentId);
        newVersionProgram = programRepository.save(newVersionProgram);

        // 6. 建立素材引用关系
        createMaterialReferences(newVersionProgram, request.getContentData());

        // 7. 发送VSN生成请求
        publishVsnGenerationRequest(newVersionProgram, VsnGenerationRequestEvent.EventType.GENERATE);

        log.info("Program updated successfully: newId={}, version={}", 
                newVersionProgram.getId(), newVersionProgram.getVersion());
        return programDtoConverter.toProgramDTO(newVersionProgram);
    }

    @Override
    public Optional<ProgramDTO> findProgramById(Long programId, Long oid) {
        return programRepository.findByIdAndOid(programId, oid)
                .map(programDtoConverter::toProgramDTO);
    }

    @Override
    public Optional<ProgramDetailDTO> findProgramDetails(Long programId, Long oid) {
        Optional<Program> programOpt = programRepository.findByIdAndOid(programId, oid);
        if (programOpt.isEmpty()) {
            return Optional.empty();
        }

        Program program = programOpt.get();
        ProgramDTO programDTO = programDtoConverter.toProgramDTO(program);

        // 查询MongoDB内容
        Optional<ProgramContent> contentOpt = Optional.empty();
        if (StringUtils.hasText(program.getProgramContentId())) {
            contentOpt = programContentRepository.findByProgramIdAndVersion(
                    program.getId(), program.getVersion());
        }

        // 构建详情DTO
        ProgramDetailDTO detailDTO = ProgramDetailDTO.builder()
                .program(programDTO)
                .contentData(contentOpt.map(ProgramContent::getOriginalData).orElse(null))
                .vsnXml(contentOpt.map(ProgramContent::getVsnXml).orElse(null))
                .build();

        return Optional.of(detailDTO);
    }

    @Override
    public List<ProgramDTO> findProgramsByUserGroup(Long oid, Long ugid, ProgramStatusEnum status, int page, int size) {
        List<Program> programs = programRepository.findByUserGroup(oid, ugid, status, page, size);
        return programDtoConverter.toProgramDTOs(programs);
    }

    @Override
    public PageVO<ProgramDTO> findProgramsPage(Long oid, Long ugid, String keyword, ProgramStatusEnum status, int page, int size) {
        log.debug("Finding programs page: oid={}, ugid={}, keyword={}, status={}, page={}, size={}", 
                oid, ugid, keyword, status, page, size);
        
        // 使用MyBatis Plus分页查询
        PageVO<Program> programPage = programRepository.findProgramsPage(oid, ugid, keyword, status, page, size);
        
        // 转换为DTO
        List<ProgramDTO> programDTOs = programDtoConverter.toProgramDTOs(programPage.getRecords());
        
        // 直接使用MyBatis Plus的分页信息
        PageVO<ProgramDTO> programDTOPageVO = programPage.withRecords(programDTOs);
        return programDTOPageVO;
    }

    @Override
    public long countProgramsByUserGroup(Long oid, Long ugid, ProgramStatusEnum status) {
        return programRepository.countByUserGroup(oid, ugid, status);
    }

    @Override
    public List<ProgramDTO> findProgramsByCreator(Long oid, Long createdBy, ProgramStatusEnum status, int page, int size) {
        List<Program> programs = programRepository.findByCreator(oid, createdBy, status, page, size);
        return programDtoConverter.toProgramDTOs(programs);
    }

    @Override
    public long countProgramsByCreator(Long oid, Long createdBy, ProgramStatusEnum status) {
        return programRepository.countByCreator(oid, createdBy, status);
    }

    @Override
    @Transactional
    public boolean deleteProgram(Long programId, Long userId, Long oid) {
        log.info("Deleting program: programId={}, userId={}, oid={}", programId, userId, oid);

        // 验证权限
        Optional<Program> programOpt = programRepository.findByIdAndOid(programId, oid);
        if (programOpt.isEmpty()) {
            log.warn("Program not found or no permission: programId={}, oid={}", programId, oid);
            return false;
        }

        // 删除节目（软删除）
        int deletedCount = programRepository.deleteById(programId);
        
        // 清理相关数据
        if (deletedCount > 0) {
            cleanupProgramRelatedData(programId);
            log.info("Program deleted successfully: programId={}", programId);
        }

        return deletedCount > 0;
    }

    @Override
    @Transactional
    public int deleteProgramsBatch(List<Long> programIds, Long userId, Long oid) {
        log.info("Batch deleting programs: programIds={}, userId={}, oid={}", programIds, userId, oid);

        int deletedCount = programRepository.deleteByIds(programIds);
        
        // 批量清理相关数据
        for (Long programId : programIds) {
            cleanupProgramRelatedData(programId);
        }

        log.info("Batch delete completed: requested={}, deleted={}", programIds.size(), deletedCount);
        return deletedCount;
    }

    @Override
    @Transactional
    public boolean updateProgramStatus(Long programId, ProgramStatusEnum status, Long userId, Long oid) {
        log.info("Updating program status: programId={}, status={}, userId={}, oid={}", 
                programId, status, userId, oid);

        // 验证权限
        if (!hasAccessPermission(programId, userId, oid)) {
            log.warn("No permission to update program status: programId={}, userId={}, oid={}", 
                    programId, userId, oid);
            return false;
        }

        int updatedCount = programRepository.updateStatus(programId, status, userId);
        
        if (updatedCount > 0) {
            log.info("Program status updated successfully: programId={}, status={}", programId, status);
        }

        return updatedCount > 0;
    }

    @Override
    public boolean isProgramNameAvailable(Long oid, Long ugid, String name, Long excludeId) {
        return programRepository.isNameAvailableInUserGroup(oid, ugid, name, excludeId);
    }

    @Override
    @Transactional
    public boolean incrementUsageCount(Long programId) {
        int updatedCount = programRepository.incrementUsageCount(programId);
        return updatedCount > 0;
    }

    @Override
    public List<ProgramDTO> findPopularPrograms(Long oid, Long ugid, int limit) {
        List<Program> programs = programRepository.findPopularPrograms(oid, ugid, limit);
        return programDtoConverter.toProgramDTOs(programs);
    }

    @Override
    public boolean hasAccessPermission(Long programId, Long userId, Long oid) {
        return programRepository.hasAccessPermission(programId, userId, oid);
    }

    // ===== 版本控制相关方法实现 =====

    @Override
    public List<ProgramVersionDTO> findAllVersions(Long sourceProgramId, Long oid) {
        // 先找到原始节目ID
        Optional<Long> realSourceId = programRepository.findSourceProgramIdByAnyVersion(sourceProgramId);
        Long sourceId = realSourceId.orElse(sourceProgramId);

        List<Program> versions = programRepository.findVersionsBySourceProgramId(sourceId);
        return programDtoConverter.toProgramVersionDTOs(versions);
    }

    @Override
    public Optional<ProgramDTO> findLatestVersion(Long sourceProgramId, Long oid) {
        // 先找到原始节目ID
        Optional<Long> realSourceId = programRepository.findSourceProgramIdByAnyVersion(sourceProgramId);
        Long sourceId = realSourceId.orElse(sourceProgramId);

        return programRepository.findLatestVersionBySourceProgramId(sourceId)
                .map(programDtoConverter::toProgramDTO);
    }

    @Override
    public Optional<ProgramDTO> findSpecificVersion(Long sourceProgramId, Integer version, Long oid) {
        return programRepository.findBySourceProgramIdAndVersion(sourceProgramId, version)
                .map(programDtoConverter::toProgramDTO);
    }

    @Override
    @Transactional
    public ProgramDTO createNewVersion(Long baseProgramId, UpdateProgramRequest request, Long userId, Long oid) {
        log.info("Creating new version: baseProgramId={}, userId={}, oid={}", baseProgramId, userId, oid);

        // 查找基础版本程序
        Program baseProgram = findProgramWithPermission(baseProgramId, oid);

        // 创建新版本
        return updateProgram(baseProgramId, request, userId, oid);
    }

    @Override
    @Transactional
    public ProgramDTO rollbackToVersion(Long sourceProgramId, Integer targetVersion, Long userId, Long oid) {
        log.info("Rolling back to version: sourceProgramId={}, targetVersion={}, userId={}, oid={}", 
                sourceProgramId, targetVersion, userId, oid);

        // 查找目标版本
        Optional<Program> targetProgramOpt = programRepository.findBySourceProgramIdAndVersion(
                sourceProgramId, targetVersion);
        
        if (targetProgramOpt.isEmpty()) {
            throw new IllegalArgumentException("Target version not found: " + targetVersion);
        }

        Program targetProgram = targetProgramOpt.get();

        // 获取目标版本的内容
        Optional<ProgramContent> targetContentOpt = programContentRepository.findByProgramIdAndVersion(
                targetProgram.getId(), targetProgram.getVersion());

        if (targetContentOpt.isEmpty()) {
            throw new IllegalStateException("Target version content not found");
        }

        // 创建基于目标版本的更新请求
        UpdateProgramRequest rollbackRequest = UpdateProgramRequest.builder()
                .name(targetProgram.getName())
                .description(targetProgram.getDescription())
                .width(targetProgram.getWidth())
                .height(targetProgram.getHeight())
                .contentData(targetContentOpt.get().getOriginalData())
                .build();

        // 创建新版本（实际是回滚版本）
        return createNewVersion(sourceProgramId, rollbackRequest, userId, oid);
    }

    @Override
    public Optional<ProgramVersionDTO> getVersionChainInfo(Long programId, Long oid) {
        Optional<Program> programOpt = programRepository.findByIdAndOid(programId, oid);
        if (programOpt.isEmpty()) {
            return Optional.empty();
        }

        Program program = programOpt.get();
        
        // 获取版本链统计信息
        Long sourceId = program.getIsSourceProgram() ? program.getId() : program.getSourceProgramId();
        long versionCount = programRepository.countVersionsBySourceProgramId(sourceId);

        ProgramVersionDTO versionDTO = programDtoConverter.toProgramVersionDTO(program);
        versionDTO.setTotalVersions((int) versionCount);
        versionDTO.setSourceProgramId(sourceId);

        return Optional.of(versionDTO);
    }

    // ===== 私有辅助方法 =====

    private void validateProgramNameUnique(Long oid, Long ugid, String name, Long excludeId) {
        if (!programRepository.isNameAvailableInUserGroup(oid, ugid, name, excludeId)) {
            throw new IllegalArgumentException("节目名称已存在: " + name);
        }
    }

    private void validateMaterialDependencies(String contentData, Long oid) {
        log.debug("Validating material dependencies for oid: {}", oid);
        
        MaterialValidationDTO validation = materialDependencyService.validateMaterialDependencies(contentData, oid);
        
        if (!validation.getIsValid()) {
            String errorMessage = "素材依赖验证失败: " + String.join(", ", validation.getErrors());
            log.warn("Material dependency validation failed for oid {}: {}", oid, errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        
        log.debug("Material dependency validation passed for oid: {}, {} materials validated", 
                oid, validation.getTotalMaterials());
    }

    private Program buildNewProgram(CreateProgramRequest request, Long userId, Long oid, Long ugid) {
        Program program = new Program();
        program.setName(request.getName());
        program.setDescription(request.getDescription());
        program.setWidth(request.getWidth());
        program.setHeight(request.getHeight());
        program.setDuration(request.getDuration());
        
        // 版本控制字段（新节目）
        program.setVersion(1);
        program.setSourceProgramId(null);
        program.setIsSourceProgram(true);
        
        // 正式节目状态管理（创建时无programStatus，只有审核状态）
        program.setStatus(null); // 正式节目创建时没有节目状态
        program.setApprovalStatus(ProgramApprovalStatusEnum.PENDING); // 进入待审核状态
        program.setVsnGenerationStatus(VsnGenerationStatusEnum.PENDING); // 需要生成VSN
        
        // 权限字段
        program.setOid(oid);
        program.setUgid(ugid);
        program.setCreatedBy(userId);
        program.setUpdatedBy(userId);
        
        // 初始化使用统计
        program.setUsageCount(0);
        
        return program;
    }

    private Program createNewVersionFromExisting(Program original, UpdateProgramRequest request, Long userId) {
        // 获取原始节目ID
        Long sourceId = original.getIsSourceProgram() ? original.getId() : original.getSourceProgramId();
        
        // 获取下一个版本号
        Integer nextVersion = programRepository.getNextVersionNumber(sourceId);

        Program newVersion = new Program();
        
        // 复制基础信息
        newVersion.setName(StringUtils.hasText(request.getName()) ? request.getName() : original.getName());
        newVersion.setDescription(StringUtils.hasText(request.getDescription()) ? 
                request.getDescription() : original.getDescription());
        newVersion.setWidth(request.getWidth() != null ? request.getWidth() : original.getWidth());
        newVersion.setHeight(request.getHeight() != null ? request.getHeight() : original.getHeight());
        newVersion.setDuration(request.getDuration() != null ? request.getDuration() : original.getDuration());
        
        // 版本控制字段（新版本）
        newVersion.setVersion(nextVersion);
        newVersion.setSourceProgramId(sourceId);
        newVersion.setIsSourceProgram(false);
        
        // 新版本状态管理（新版本需要重新审核）
        newVersion.setStatus(null); // 新版本创建时没有节目状态
        newVersion.setApprovalStatus(ProgramApprovalStatusEnum.PENDING); // 需要重新审核
        newVersion.setVsnGenerationStatus(VsnGenerationStatusEnum.PENDING); // 需要生成新VSN
        
        // 权限字段（继承原程序）
        newVersion.setOid(original.getOid());
        newVersion.setUgid(original.getUgid());
        newVersion.setCreatedBy(userId);
        newVersion.setUpdatedBy(userId);
        
        // 初始化使用统计
        newVersion.setUsageCount(0);
        
        return newVersion;
    }

    private String saveContentToMongoDB(Program program, String contentData, String vsnData, Long userId) {
        ProgramContent content = new ProgramContent();
        content.setProgramId(program.getId());
        content.setVersion(program.getVersion());
        content.setOrgId(program.getOid());
        content.setOriginalData(contentData);
        content.setCreatedBy(userId);
        content.setUpdatedBy(userId);
        content.setCreatedTime(LocalDateTime.now());
        content.setUpdatedTime(LocalDateTime.now());
        
        // 处理VSN数据（如果提供）
        if (StringUtils.hasText(vsnData)) {
            content.setVsnPrograms(JsonUtils.fromJson(vsnData, new  TypeReference<List<VsnProgram>>() {}));
            log.debug("VSN data provided for program: {}, version: {}", program.getId(), program.getVersion());
        }

        // 保存到MongoDB
        content = programContentRepository.save(content);
        log.debug("Program content saved to MongoDB: programId={}, version={}, contentId={}", 
                program.getId(), program.getVersion(), content.getId());
        return content.getId();
    }

    private void createMaterialReferences(Program program, String contentData) {
        log.debug("Creating material references for program: {}", program.getId());
        
        boolean success = materialDependencyService.createMaterialDependencies(
                program.getId(), contentData, program.getOid());
                
        if (!success) {
            log.warn("Failed to create material references for program: {}", program.getId());
            // 不抛异常，允许程序继续运行，仅记录警告
        } else {
            log.debug("Material references created successfully for program: {}", program.getId());
        }
    }

    private void publishVsnGenerationRequest(Program program, VsnGenerationRequestEvent.EventType eventType) {
        VsnGenerationRequestEvent event = VsnGenerationRequestEvent.builder()
                .eventType(eventType.getValue())
                .programId(program.getId())
                .version(program.getVersion())
                .organizationId(program.getOid())
                .userGroupId(program.getUgid())
                .userId(program.getCreatedBy())
                .programName(program.getName())
                .width(program.getWidth())
                .height(program.getHeight())
                .contentId(program.getProgramContentId())
                .priority(VsnGenerationRequestEvent.Priority.NORMAL.getValue())
                .timestamp(LocalDateTime.now())
                .build();

        // 触发MQ发布
        vsnEventPublisher.publishVsnGenerationRequest(event);
        log.debug("VSN generation request published for program: {}", program.getId());
    }

    private Program findProgramWithPermission(Long programId, Long oid) {
        return programRepository.findByIdAndOid(programId, oid)
                .orElseThrow(() -> new IllegalArgumentException("节目不存在或无访问权限: " + programId));
    }

    private void cleanupProgramRelatedData(Long programId) {
        // 清理素材引用关系
        // programMaterialRefRepository.deleteByProgramId(programId);
        
        // 清理审核记录
        // programApprovalRepository.deleteByProgramId(programId);
        
        log.debug("Cleaned up related data for program: {}", programId);
    }
    
    /**
     * 创建审核记录
     * 节目创建后自动进入审核流程
     * @param programId 节目ID
     * @param programVersion 节目版本
     * @param userId 提交者用户ID
     * @param oid 组织ID
     */
    private void createApprovalRecord(Long programId, Integer programVersion, Long userId, Long oid) {
        try {
            log.debug("🔄 创建节目审核记录 - programId: {}, version: {}, userId: {}, oid: {}", 
                    programId, programVersion, userId, oid);
            
            // 调用审核服务创建审核申请
            programApprovalService.submitApproval(programId, programVersion, userId, oid);
            
            log.info("✅ 节目审核记录创建成功 - programId: {}, version: {}", programId, programVersion);
        } catch (Exception e) {
            log.error("❌ 创建节目审核记录失败 - programId: {}, version: {}, error: {}", 
                    programId, programVersion, e.getMessage(), e);
            // 不抛出异常，避免影响节目创建流程
            // 审核记录创建失败不应该阻止节目创建
        }
    }
}