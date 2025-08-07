package org.nan.cloud.core.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.core.domain.Program;
import org.nan.cloud.core.service.converter.ProgramDtoConverter;
import org.nan.cloud.core.repository.ProgramContentRepository;
import org.nan.cloud.core.repository.ProgramRepository;
import org.nan.cloud.core.service.MaterialDependencyService;
import org.nan.cloud.core.service.ProgramVersionService;
import org.nan.cloud.program.document.ProgramContent;
import org.nan.cloud.program.dto.response.ProgramVersionDTO;
import org.nan.cloud.program.dto.response.VersionComparisonDTO;
import org.nan.cloud.program.dto.response.VersionHistoryDTO;
import org.nan.cloud.program.enums.ProgramStatusEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 节目版本控制服务实现
 * 提供高级版本管理功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProgramVersionServiceImpl implements ProgramVersionService {
    
    private final ProgramRepository programRepository;
    private final ProgramContentRepository programContentRepository;
    private final ProgramDtoConverter programDtoConverter;
    private final MaterialDependencyService materialDependencyService;
    private final ObjectMapper objectMapper;
    
    @Override
    public VersionHistoryDTO getVersionHistory(Long sourceProgramId, Long oid) {
        log.debug("Getting version history for source program: {}, oid: {}", sourceProgramId, oid);
        
        // 找到真正的原始节目ID
        Optional<Long> realSourceId = programRepository.findSourceProgramIdByAnyVersion(sourceProgramId);
        Long sourceId = realSourceId.orElse(sourceProgramId);
        
        // 获取所有版本
        List<Program> versions = programRepository.findVersionsBySourceProgramId(sourceId);
        if (versions.isEmpty()) {
            log.warn("No versions found for source program: {}", sourceId);
            return null;
        }
        
        // 获取基础信息
        Program latestProgram = versions.stream()
                .max(Comparator.comparing(Program::getVersion))
                .orElse(versions.get(0));
        
        LocalDateTime firstCreated = versions.stream()
                .min(Comparator.comparing(Program::getCreatedTime))
                .map(Program::getCreatedTime)
                .orElse(LocalDateTime.now());
        
        // 构建版本历史项
        List<VersionHistoryDTO.VersionHistoryItem> historyItems = versions.stream()
                .sorted((v1, v2) -> v2.getVersion().compareTo(v1.getVersion())) // 倒序
                .map(this::buildVersionHistoryItem)
                .collect(Collectors.toList());
        
        return VersionHistoryDTO.builder()
                .sourceProgramId(sourceId)
                .programName(latestProgram.getName())
                .totalVersions(versions.size())
                .latestVersion(latestProgram.getVersion())
                .createdTime(firstCreated)
                .lastUpdatedTime(latestProgram.getUpdatedTime())
                .versions(historyItems)
                .build();
    }
    
    @Override
    public Optional<VersionComparisonDTO> compareVersions(Long sourceProgramId, Integer version1, Integer version2, Long oid) {
        log.debug("Comparing versions {} and {} for source program: {}, oid: {}", 
                version1, version2, sourceProgramId, oid);
        
        // 找到真正的原始节目ID
        Optional<Long> realSourceId = programRepository.findSourceProgramIdByAnyVersion(sourceProgramId);
        Long sourceId = realSourceId.orElse(sourceProgramId);
        
        // 获取两个版本的程序
        Optional<Program> program1Opt = programRepository.findBySourceProgramIdAndVersion(sourceId, version1);
        Optional<Program> program2Opt = programRepository.findBySourceProgramIdAndVersion(sourceId, version2);
        
        if (program1Opt.isEmpty() || program2Opt.isEmpty()) {
            log.warn("One or both versions not found: v{}, v{} for source: {}", 
                    version1, version2, sourceId);
            return Optional.empty();
        }
        
        Program program1 = program1Opt.get();
        Program program2 = program2Opt.get();
        
        // 获取内容数据
        Optional<ProgramContent> content1Opt = programContentRepository.findByProgramIdAndVersion(
                program1.getId(), program1.getVersion());
        Optional<ProgramContent> content2Opt = programContentRepository.findByProgramIdAndVersion(
                program2.getId(), program2.getVersion());
        
        // 构建比较结果
        VersionComparisonDTO comparison = buildVersionComparison(
                program1, program2, 
                content1Opt.orElse(null), content2Opt.orElse(null));
        
        return Optional.of(comparison);
    }
    
    @Override
    public boolean canRollbackToVersion(Long sourceProgramId, Integer targetVersion, Long oid) {
        log.debug("Checking rollback possibility for source program: {}, target version: {}, oid: {}", 
                sourceProgramId, targetVersion, oid);
        
        // 找到真正的原始节目ID
        Optional<Long> realSourceId = programRepository.findSourceProgramIdByAnyVersion(sourceProgramId);
        Long sourceId = realSourceId.orElse(sourceProgramId);
        
        // 检查目标版本是否存在
        Optional<Program> targetProgramOpt = programRepository.findBySourceProgramIdAndVersion(sourceId, targetVersion);
        if (targetProgramOpt.isEmpty()) {
            log.debug("Target version {} not found for source program: {}", targetVersion, sourceId);
            return false;
        }
        
        Program targetProgram = targetProgramOpt.get();
        
        // 检查目标版本的内容是否完整
        Optional<ProgramContent> contentOpt = programContentRepository.findByProgramIdAndVersion(
                targetProgram.getId(), targetProgram.getVersion());
        
        if (contentOpt.isEmpty()) {
            log.debug("Target version {} content not found, cannot rollback", targetVersion);
            return false;
        }
        
        // 检查素材依赖是否仍然有效
        try {
            boolean validMaterials = materialDependencyService.validateMaterialDependencies(
                    contentOpt.get().getOriginalData(), oid).getIsValid();
            
            if (!validMaterials) {
                log.debug("Target version {} has invalid material dependencies, rollback not recommended", targetVersion);
                return false; // 可以根据业务需求决定是否允许
            }
        } catch (Exception e) {
            log.warn("Failed to validate materials for rollback target version: " + targetVersion, e);
            return false;
        }
        
        log.debug("Version {} can be rolled back for source program: {}", targetVersion, sourceId);
        return true;
    }
    
    @Override
    public Optional<String> getVersionChangeSummary(Long programId, Long oid) {
        log.debug("Getting change summary for program: {}, oid: {}", programId, oid);
        
        Optional<Program> programOpt = programRepository.findByIdAndOid(programId, oid);
        if (programOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Program program = programOpt.get();
        
        // 如果是第一个版本，返回创建摘要
        if (program.getVersion() == 1) {
            return Optional.of("初始版本创建");
        }
        
        // 获取前一个版本进行比较
        Long sourceId = program.getIsSourceProgram() ? program.getId() : program.getSourceProgramId();
        Integer previousVersion = program.getVersion() - 1;
        
        Optional<Program> previousProgramOpt = programRepository.findBySourceProgramIdAndVersion(sourceId, previousVersion);
        if (previousProgramOpt.isEmpty()) {
            return Optional.of("版本创建（无法比较前版本）");
        }
        
        // 生成变更摘要
        String summary = generateChangeSummary(previousProgramOpt.get(), program);
        return Optional.of(summary);
    }
    
    @Override
    public List<ProgramVersionDTO> findKeyVersions(Long sourceProgramId, Long oid) {
        log.debug("Finding key versions for source program: {}, oid: {}", sourceProgramId, oid);
        
        // 找到真正的原始节目ID
        Optional<Long> realSourceId = programRepository.findSourceProgramIdByAnyVersion(sourceProgramId);
        Long sourceId = realSourceId.orElse(sourceProgramId);
        
        List<Program> allVersions = programRepository.findVersionsBySourceProgramId(sourceId);
        List<Program> keyVersions = new ArrayList<>();
        
        if (!allVersions.isEmpty()) {
            // 第一个版本（原始版本）
            allVersions.stream()
                    .min(Comparator.comparing(Program::getVersion))
                    .ifPresent(keyVersions::add);
            
            // 第一个发布版本
            allVersions.stream()
                    .filter(p -> ProgramStatusEnum.PUBLISHED.equals(p.getStatus()))
                    .min(Comparator.comparing(Program::getVersion))
                    .ifPresent(firstPublished -> {
                        if (keyVersions.stream().noneMatch(kv -> kv.getId().equals(firstPublished.getId()))) {
                            keyVersions.add(firstPublished);
                        }
                    });
            
            // 最新版本
            allVersions.stream()
                    .max(Comparator.comparing(Program::getVersion))
                    .ifPresent(latest -> {
                        if (keyVersions.stream().noneMatch(kv -> kv.getId().equals(latest.getId()))) {
                            keyVersions.add(latest);
                        }
                    });
        }
        
        return keyVersions.stream()
                .map(programDtoConverter::toProgramVersionDTO)
                .sorted(Comparator.comparing(ProgramVersionDTO::getVersion))
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public int cleanupOldVersions(Long sourceProgramId, int keepCount, Long oid) {
        log.info("Cleaning up old versions for source program: {}, keeping: {}, oid: {}", 
                sourceProgramId, keepCount, oid);
        
        if (keepCount < 1) {
            log.warn("Invalid keepCount: {}, must be at least 1", keepCount);
            return 0;
        }
        
        // 找到真正的原始节目ID
        Optional<Long> realSourceId = programRepository.findSourceProgramIdByAnyVersion(sourceProgramId);
        Long sourceId = realSourceId.orElse(sourceProgramId);
        
        // 获取所有版本，按版本号倒序
        List<Program> allVersions = programRepository.findVersionsBySourceProgramId(sourceId);
        allVersions.sort((v1, v2) -> v2.getVersion().compareTo(v1.getVersion()));
        
        if (allVersions.size() <= keepCount) {
            log.debug("Version count {} <= keep count {}, no cleanup needed", allVersions.size(), keepCount);
            return 0;
        }
        
        // 获取关键版本（永远不删除）
        Set<Long> keyVersionIds = findKeyVersions(sourceId, oid).stream()
                .map(ProgramVersionDTO::getId)
                .collect(Collectors.toSet());
        
        // 确定要删除的版本
        List<Long> toDelete = new ArrayList<>();
        int preserveCount = 0;
        
        for (Program version : allVersions) {
            if (keyVersionIds.contains(version.getId()) || preserveCount < keepCount) {
                preserveCount++;
                continue;
            }
            
            // 只删除草稿状态的版本
            if (ProgramStatusEnum.DRAFT.equals(version.getStatus())) {
                toDelete.add(version.getId());
            }
        }
        
        if (toDelete.isEmpty()) {
            log.debug("No versions eligible for cleanup");
            return 0;
        }
        
        // 执行删除
        int deletedCount = 0;
        for (Long versionId : toDelete) {
            try {
                int result = programRepository.deleteById(versionId);
                if (result > 0) {
                    deletedCount++;
                    // 清理相关数据
                    materialDependencyService.deleteMaterialDependencies(versionId);
                }
            } catch (Exception e) {
                log.error("Failed to delete version: " + versionId, e);
            }
        }
        
        log.info("Cleaned up {} old versions for source program: {}", deletedCount, sourceId);
        return deletedCount;
    }
    
    @Override
    public VersionStatistics getVersionStatistics(Long sourceProgramId, Long oid) {
        log.debug("Getting version statistics for source program: {}, oid: {}", sourceProgramId, oid);
        
        // 找到真正的原始节目ID
        Optional<Long> realSourceId = programRepository.findSourceProgramIdByAnyVersion(sourceProgramId);
        Long sourceId = realSourceId.orElse(sourceProgramId);
        
        List<Program> versions = programRepository.findVersionsBySourceProgramId(sourceId);
        
        VersionStatistics stats = new VersionStatistics();
        stats.setTotalVersions(versions.size());
        
        Map<ProgramStatusEnum, Long> statusCounts = versions.stream()
                .collect(Collectors.groupingBy(
                        Program::getStatus,
                        Collectors.counting()
                ));
        
        stats.setDraftVersions(statusCounts.getOrDefault(ProgramStatusEnum.DRAFT, 0L).intValue());
        stats.setPublishedVersions(statusCounts.getOrDefault(ProgramStatusEnum.PUBLISHED, 0L).intValue());
        // TODO: 添加ARCHIVED状态的统计
        stats.setArchivedVersions(0);
        
        return stats;
    }
    
    // ===== 私有辅助方法 =====
    
    private VersionHistoryDTO.VersionHistoryItem buildVersionHistoryItem(Program program) {
        return VersionHistoryDTO.VersionHistoryItem.builder()
                .programId(program.getId())
                .version(program.getVersion())
                .status(program.getStatus().name())
                .vsnStatus(program.getVsnGenerationStatus().name())
                .createdBy(program.getCreatedBy())
                .createdByName("用户" + program.getCreatedBy()) // TODO: 查询用户名
                .createdTime(program.getCreatedTime())
                .changeSummary(getVersionChangeSummary(program.getId(), program.getOid()).orElse(""))
                .isKeyVersion(isKeyVersion(program))
                .keyVersionType(getKeyVersionType(program))
                .tags(getVersionTags(program))
                .build();
    }
    
    private boolean isKeyVersion(Program program) {
        // 第一个版本或发布版本被认为是关键版本
        return program.getVersion() == 1 || ProgramStatusEnum.PUBLISHED.equals(program.getStatus());
    }
    
    private String getKeyVersionType(Program program) {
        if (program.getVersion() == 1) {
            return "INITIAL";
        }
        if (ProgramStatusEnum.PUBLISHED.equals(program.getStatus())) {
            return "PUBLISHED";
        }
        return null;
    }
    
    private List<String> getVersionTags(Program program) {
        List<String> tags = new ArrayList<>();
        
        if (program.getVersion() == 1) {
            tags.add("初始版本");
        }
        
        if (ProgramStatusEnum.PUBLISHED.equals(program.getStatus())) {
            tags.add("已发布");
        }
        
        // TODO: 可以根据业务需求添加更多标签逻辑
        
        return tags;
    }
    
    private String generateChangeSummary(Program oldProgram, Program newProgram) {
        List<String> changes = new ArrayList<>();
        
        // 比较基本字段
        if (!Objects.equals(oldProgram.getName(), newProgram.getName())) {
            changes.add("名称变更");
        }
        
        if (!Objects.equals(oldProgram.getDescription(), newProgram.getDescription())) {
            changes.add("描述变更");
        }
        
        if (!Objects.equals(oldProgram.getWidth(), newProgram.getWidth()) || 
            !Objects.equals(oldProgram.getHeight(), newProgram.getHeight())) {
            changes.add("尺寸变更");
        }
        
        if (!Objects.equals(oldProgram.getDuration(), newProgram.getDuration())) {
            changes.add("时长变更");
        }
        
        if (!Objects.equals(oldProgram.getStatus(), newProgram.getStatus())) {
            changes.add("状态变更");
        }
        
        // TODO: 比较内容变更（需要解析JSON内容）
        
        if (changes.isEmpty()) {
            return "内容更新";
        }
        
        return String.join("、", changes);
    }
    
    private VersionComparisonDTO buildVersionComparison(Program program1, Program program2, 
                                                       ProgramContent content1, ProgramContent content2) {
        
        // 构建版本信息
        VersionComparisonDTO.VersionInfo version1Info = VersionComparisonDTO.VersionInfo.builder()
                .programId(program1.getId())
                .version(program1.getVersion())
                .status(program1.getStatus().name())
                .createdTime(program1.getCreatedTime())
                .createdByName("用户" + program1.getCreatedBy())
                .build();
        
        VersionComparisonDTO.VersionInfo version2Info = VersionComparisonDTO.VersionInfo.builder()
                .programId(program2.getId())
                .version(program2.getVersion())
                .status(program2.getStatus().name())
                .createdTime(program2.getCreatedTime())
                .createdByName("用户" + program2.getCreatedBy())
                .build();
        
        // 分析差异
        List<VersionComparisonDTO.FieldDifference> differences = analyzeFieldDifferences(program1, program2);
        VersionComparisonDTO.MaterialDifferences materialDiffs = analyzeMaterialDifferences(content1, content2);
        
        // 构建摘要
        VersionComparisonDTO.ComparisonSummary summary = VersionComparisonDTO.ComparisonSummary.builder()
                .totalDifferences(differences.size() + materialDiffs.getTotalMaterialChanges())
                .metadataDifferences((int) differences.stream().filter(d -> "METADATA".equals(d.getFieldCategory())).count())
                .contentDifferences((int) differences.stream().filter(d -> "CONTENT".equals(d.getFieldCategory())).count())
                .materialDifferences(materialDiffs.getTotalMaterialChanges())
                .hasSignificantChanges(differences.size() > 0 || materialDiffs.getTotalMaterialChanges() > 0)
                .changeSeverity(determineChangeSeverity(differences, materialDiffs))
                .build();
        
        return VersionComparisonDTO.builder()
                .sourceProgramId(program1.getSourceProgramId())
                .programName(program2.getName())
                .version1(version1Info)
                .version2(version2Info)
                .summary(summary)
                .differences(differences)
                .materialDifferences(materialDiffs)
                .build();
    }
    
    private List<VersionComparisonDTO.FieldDifference> analyzeFieldDifferences(Program program1, Program program2) {
        List<VersionComparisonDTO.FieldDifference> differences = new ArrayList<>();
        
        // 比较各字段
        addFieldDifferenceIfChanged(differences, "name", "名称", program1.getName(), program2.getName(), "METADATA", "MAJOR");
        addFieldDifferenceIfChanged(differences, "description", "描述", program1.getDescription(), program2.getDescription(), "METADATA", "MINOR");
        addFieldDifferenceIfChanged(differences, "width", "宽度", program1.getWidth(), program2.getWidth(), "METADATA", "MAJOR");
        addFieldDifferenceIfChanged(differences, "height", "高度", program1.getHeight(), program2.getHeight(), "METADATA", "MAJOR");
        addFieldDifferenceIfChanged(differences, "duration", "时长", program1.getDuration(), program2.getDuration(), "CONTENT", "MAJOR");
        addFieldDifferenceIfChanged(differences, "status", "状态", program1.getStatus(), program2.getStatus(), "METADATA", "MINOR");
        
        return differences;
    }
    
    private void addFieldDifferenceIfChanged(List<VersionComparisonDTO.FieldDifference> differences, 
                                           String fieldPath, String fieldName, Object oldValue, Object newValue, 
                                           String category, String severity) {
        if (!Objects.equals(oldValue, newValue)) {
            differences.add(VersionComparisonDTO.FieldDifference.builder()
                    .fieldPath(fieldPath)
                    .fieldName(fieldName)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .changeType("MODIFIED")
                    .fieldCategory(category)
                    .severity(severity)
                    .build());
        }
    }
    
    private VersionComparisonDTO.MaterialDifferences analyzeMaterialDifferences(ProgramContent content1, ProgramContent content2) {
        List<Long> materials1 = content1 != null ? 
                materialDependencyService.parseMaterialReferences(content1.getOriginalData()) : List.of();
        List<Long> materials2 = content2 != null ? 
                materialDependencyService.parseMaterialReferences(content2.getOriginalData()) : List.of();
        
        Set<Long> set1 = new HashSet<>(materials1);
        Set<Long> set2 = new HashSet<>(materials2);
        
        List<Long> added = set2.stream().filter(id -> !set1.contains(id)).collect(Collectors.toList());
        List<Long> removed = set1.stream().filter(id -> !set2.contains(id)).collect(Collectors.toList());
        List<Long> modified = List.of(); // TODO: 实现素材修改检测
        
        return VersionComparisonDTO.MaterialDifferences.builder()
                .addedMaterials(added)
                .removedMaterials(removed)
                .modifiedMaterials(modified)
                .materialNames(new HashMap<>()) // TODO: 查询素材名称
                .totalMaterialChanges(added.size() + removed.size() + modified.size())
                .build();
    }
    
    private String determineChangeSeverity(List<VersionComparisonDTO.FieldDifference> differences, 
                                         VersionComparisonDTO.MaterialDifferences materialDiffs) {
        boolean hasCritical = differences.stream().anyMatch(d -> "CRITICAL".equals(d.getSeverity()));
        boolean hasMajor = differences.stream().anyMatch(d -> "MAJOR".equals(d.getSeverity())) || 
                          materialDiffs.getTotalMaterialChanges() > 0;
        
        if (hasCritical) return "CRITICAL";
        if (hasMajor) return "MAJOR";
        return "MINOR";
    }
}