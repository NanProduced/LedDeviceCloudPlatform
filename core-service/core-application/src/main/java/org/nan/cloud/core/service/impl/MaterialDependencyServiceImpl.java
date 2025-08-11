package org.nan.cloud.core.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.exception.BaseException;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.common.basic.utils.JsonUtils;
import org.nan.cloud.core.repository.ProgramMaterialRefRepository;
import org.nan.cloud.core.repository.MaterialRepository;
import org.nan.cloud.core.domain.Material;
import org.nan.cloud.core.repository.ProgramRepository;
import org.nan.cloud.core.domain.Program;
import org.nan.cloud.core.domain.ProgramMaterialRef;
import org.nan.cloud.core.service.MaterialDependencyService;
import org.nan.cloud.program.dto.response.MaterialDependencyDTO;
import org.nan.cloud.program.dto.response.MaterialValidationDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 素材依赖管理服务实现
 * 解析节目内容中的素材引用，管理依赖关系
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialDependencyServiceImpl implements MaterialDependencyService {
    
    private final ProgramMaterialRefRepository programMaterialRefRepository;
    private final MaterialRepository materialRepository;
    private final ProgramRepository programRepository;
    
    @Override
    public MaterialValidationDTO validateMaterialDependencies(String contentData, Long oid) {
        log.debug("Validating material dependencies for oid: {}", oid);
        
        try {
            // 解析内容数据，提取素材引用
            List<Long> materialIds = parseMaterialReferences(contentData);
            
            if (materialIds.isEmpty()) {
                return MaterialValidationDTO.builder()
                        .isValid(true)
                        .totalMaterials(0)
                        .validMaterials(0)
                        .invalidMaterials(0)
                        .missingMaterialIds(List.of())
                        .errors(List.of())
                        .build();
            }
            
            // 2) 批量存在性校验（使用 distinct 降低查询成本）
            Set<Long> distinctIds = new HashSet<>(materialIds);
            List<Material> materials = materialRepository.batchGetMaterialsByIds(new ArrayList<>(distinctIds));
            Set<Long> existsSet = materials == null ? Set.of() : materials.stream()
                    .map(Material::getMid)
                    .collect(Collectors.toSet());

            // 不存在的素材
            List<Long> missingByExistence = distinctIds.stream()
                    .filter(id -> !existsSet.contains(id))
                    .toList();

            // 3) 批量组织归属校验（仅对已存在的素材，避免N+1查询）
            Set<Long> belongsSet = materialRepository.batchCheckBelongsToOrg(oid, new ArrayList<>(existsSet));
            List<Long> invalidOrg = existsSet.stream()
                    .filter(mid -> !belongsSet.contains(mid))
                    .toList();

            // 合并缺失列表
            List<Long> missing = new ArrayList<>();
            missing.addAll(missingByExistence);
            missing.addAll(invalidOrg);

            boolean isValid = missing.isEmpty();
            List<String> errors = isValid
                    ? List.of()
                    : List.of("以下素材不存在或不属于该组织: " + missing);

            return MaterialValidationDTO.builder()
                    .isValid(isValid)
                    .totalMaterials(materialIds.size())
                    .validMaterials(materialIds.size() - missing.size())
                    .invalidMaterials(missing.size())
                    .missingMaterialIds(missing)
                    .errors(errors)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to validate material dependencies", e);
            return MaterialValidationDTO.builder()
                    .isValid(false)
                    .totalMaterials(0)
                    .validMaterials(0)
                    .invalidMaterials(0)
                    .missingMaterialIds(List.of())
                    .errors(List.of("内容解析失败: " + e.getMessage()))
                    .build();
        }
    }
    
    @Override
    @Transactional
    public boolean createMaterialDependencies(Long programId, String contentData, Long oid) {
        log.debug("Creating material dependencies for program: {}", programId);
        
        try {
            // 解析素材引用（包含路径，用于后续追踪）
            List<ParsedRef> parsedRefs = parseMaterialRefsInternal(contentData);
            List<Long> materialIds = parsedRefs.stream().map(p -> p.materialId).toList();
            
            if (materialIds.isEmpty()) {
                log.debug("No material references found in program: {}", programId);
                return true;
            }

            // 查询节目，获取版本与创建者
            Program program = programRepository.findById(programId)
                    .orElseThrow(() -> new BaseException(ExceptionEnum.PROGRAM_NOT_EXISTS, "节目不存在: " + programId));

            // 批量查询素材，便于补充类型
            List<Material> materials = materialRepository.batchGetMaterialsByIds(new ArrayList<>(new LinkedHashSet<>(materialIds)));
            Map<Long, Material> idToMaterial = (materials == null)
                    ? Map.of()
                    : materials.stream().collect(Collectors.toMap(Material::getMid, m -> m));

            // 组装引用记录（usageIndex 按出现顺序从1开始）
            List<ProgramMaterialRef> refs = new ArrayList<>();
            for (int i = 0; i < parsedRefs.size(); i++) {
                Long mid = parsedRefs.get(i).materialId;
                ProgramMaterialRef ref = new ProgramMaterialRef();
                ref.setProgramId(programId);
                ref.setProgramVersion(program.getVersion());
                ref.setMaterialId(mid);
                Material mm = idToMaterial.get(mid);
                ref.setMaterialType(mm != null ? mm.getMaterialType() : "UNKNOWN");
                ref.setUsageIndex(i + 1);
                ref.setVsnPath(parsedRefs.get(i).vsnPath);
                ref.setCreatedBy(program.getCreatedBy());
                refs.add(ref);
            }

            programMaterialRefRepository.saveBatch(refs);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to create material dependencies for program: {}", programId, e);
            return false;
        }
    }

    /**
     * 内部解析：返回 materialId + json路径
     */
    private List<ParsedRef> parseMaterialRefsInternal(String contentData) throws Exception {
        if (!StringUtils.hasText(contentData)) {
            return List.of();
        }
        JsonNode rootNode = JsonUtils.getDefaultObjectMapper().readTree(contentData);
        List<ParsedRef> refs = new java.util.ArrayList<>();
        extractMaterialIdsWithPath(rootNode, "", refs);
        return refs;
    }
    
    @Override
    @Transactional
    public boolean updateMaterialDependencies(Long programId, String contentData, Long oid) {
        log.debug("Updating material dependencies for program: {}", programId);
        
        try {
            // 先按版本删除现有依赖关系
            Program program = programRepository.findById(programId)
                    .orElseThrow(() -> new BaseException(ExceptionEnum.PROGRAM_NOT_EXISTS, "节目不存在: " + programId));
            programMaterialRefRepository.deleteByProgramIdAndVersion(programId, program.getVersion());
            
            // 重新创建依赖关系
            return createMaterialDependencies(programId, contentData, oid);
            
        } catch (Exception e) {
            log.error("Failed to update material dependencies for program: {}", programId, e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean deleteMaterialDependencies(Long programId) {
        log.debug("Deleting material dependencies for program: {}", programId);
        
        try {
            int deletedCount = programMaterialRefRepository.deleteByProgramId(programId);
            log.debug("Deleted {} material dependencies for program: {}", deletedCount, programId);
            return deletedCount >= 0;
            
        } catch (Exception e) {
            log.error("Failed to delete material dependencies for program: {}", programId, e);
            return false;
        }
    }
    
    @Override
    public List<MaterialDependencyDTO> findMaterialDependencies(Long programId, Long oid) {
        log.debug("Finding material dependencies for program: {}, oid: {}", programId, oid);
        
        try {
            List<ProgramMaterialRef> refs = programMaterialRefRepository.findByProgramId(programId);
            if (refs == null || refs.isEmpty()) {
                return List.of();
            }
            // 填充素材名称等可读信息
            List<Long> mids = refs.stream().map(ProgramMaterialRef::getMaterialId).distinct().toList();
            List<Material> mats = materialRepository.batchGetMaterialsByIds(mids);
            java.util.Map<Long, Material> idToMaterial = (mats == null)
                    ? java.util.Map.of()
                    : mats.stream().collect(java.util.stream.Collectors.toMap(Material::getMid, m -> m));

            return refs.stream().map(ref -> {
                Material m = idToMaterial.get(ref.getMaterialId());
                return MaterialDependencyDTO.builder()
                        .programId(ref.getProgramId())
                        .materialId(ref.getMaterialId())
                        .materialName(m != null ? m.getMaterialName() : null)
                        .materialCategory(ref.getMaterialType())
                        .createdTime(ref.getCreatedTime())
                        .isAvailable(Boolean.TRUE)
                        .build();
            }).collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("Failed to find material dependencies for program: {}", programId, e);
            return List.of();
        }
    }
    
    @Override
    public List<Long> findProgramsUsingMaterial(Long materialId, Long oid) {
        log.debug("Finding programs using material: {}, oid: {}", materialId, oid);
        
        try {
            return programMaterialRefRepository.findByMaterialId(materialId)
                    .stream()
                    .map(ProgramMaterialRef::getProgramId)
                    .distinct()
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("Failed to find programs using material: {}", materialId, e);
            return List.of();
        }
    }
    
    @Override
    public List<MaterialValidationDTO> batchValidateMaterials(List<Long> materialIds, Long oid) {
        log.debug("Batch validating {} materials for oid: {}", materialIds.size(), oid);
        if (materialIds.isEmpty()) {
            return List.of();
        }

        // 去重后批量查询，但结果仍按原顺序逐项返回
        List<Long> distinctIds = new ArrayList<>(new LinkedHashSet<>(materialIds));
        List<Material> materials = materialRepository.batchGetMaterialsByIds(distinctIds);
        Map<Long, Material> idToMaterial = (materials == null)
                ? java.util.Map.of()
                : materials.stream().collect(Collectors.toMap(Material::getMid, m -> m));

        // 批量校验归属关系，避免N+1查询
        Set<Long> belongsSet = materialRepository.batchCheckBelongsToOrg(oid, distinctIds);
        
        return materialIds.stream().map(mid -> {
            Material m = idToMaterial.get(mid);
            boolean exists = m != null;
            boolean belongs = exists && belongsSet.contains(mid);
            boolean ok = exists && belongs;
            return MaterialValidationDTO.builder()
                    .isValid(ok)
                    .totalMaterials(1)
                    .validMaterials(ok ? 1 : 0)
                    .invalidMaterials(ok ? 0 : 1)
                    .missingMaterialIds(ok ? List.of() : List.of(mid))
                    .errors(ok ? List.of() : List.of("素材不存在或不属于该组织: " + mid))
                    .build();
        }).collect(Collectors.toList());
    }
    
    @Override
    public boolean canDeleteMaterial(Long materialId, Long oid) {
        log.debug("Checking if material can be deleted: {}, oid: {}", materialId, oid);
        
        long cnt = programMaterialRefRepository.countUsageByMaterialId(materialId);
        boolean canDelete = cnt == 0L;
        
        log.debug("Material {} can be deleted: {}, used by {} programs", 
                materialId, canDelete, cnt);
        
        return canDelete;
    }
    
    @Override
    public List<Long> parseMaterialReferences(String contentData) {
        if (!StringUtils.hasText(contentData)) {
            return List.of();
        }
        
        try {
            JsonNode rootNode = JsonUtils.getDefaultObjectMapper().readTree(contentData);
            List<ParsedRef> refs = new java.util.ArrayList<>();
            // 递归解析JSON，提取所有materialId字段及路径
            extractMaterialIdsWithPath(rootNode, "", refs);

            List<Long> result = refs.stream().map(r -> r.materialId).toList();
            log.debug("Parsed {} material references from content data", result.size());
            return result;
            
        } catch (Exception e) {
            log.error("Failed to parse material references from content data", e);
            return List.of();
        }
    }
    
    // ===== 私有辅助方法 =====
    
    // 移除临时占位的单条校验/存在性检查方法，统一走批量校验逻辑
    
    private void extractMaterialIdsWithPath(JsonNode node, String path, List<ParsedRef> refs) {
        if (node.isObject()) {
            if (node.has("materialId")) {
                JsonNode materialIdNode = node.get("materialId");
                Long mid = null;
                if (materialIdNode.isNumber()) {
                    mid = materialIdNode.asLong();
                } else if (materialIdNode.isTextual()) {
                    try { mid = Long.parseLong(materialIdNode.asText()); } catch (NumberFormatException ignored) {}
                }
                if (mid != null) {
                    refs.add(new ParsedRef(mid, path));
                }
            }
            node.fields().forEachRemaining(entry -> {
                String childPath = path.isEmpty() ? entry.getKey() : path + "." + entry.getKey();
                extractMaterialIdsWithPath(entry.getValue(), childPath, refs);
            });
        } else if (node.isArray()) {
            int idx = 0;
            for (JsonNode element : node) {
                String childPath = path + "[" + idx + "]";
                extractMaterialIdsWithPath(element, childPath, refs);
                idx++;
            }
        }
    }

    /**
     * 解析结果封装（最小集合：materialId + json路径）
     */
    private record ParsedRef(Long materialId, String vsnPath) {}
}