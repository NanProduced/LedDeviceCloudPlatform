package org.nan.cloud.core.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.core.repository.ProgramMaterialRefRepository;
import org.nan.cloud.core.service.MaterialDependencyService;
import org.nan.cloud.program.dto.response.MaterialDependencyDTO;
import org.nan.cloud.program.dto.response.MaterialValidationDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
    private final ObjectMapper objectMapper;
    
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
            
            // TODO: 实际验证素材存在性和权限
            // 这里需要调用素材服务验证素材是否存在且有访问权限
            List<Long> missingMaterials = validateMaterialsExist(materialIds, oid);
            
            boolean isValid = missingMaterials.isEmpty();
            List<String> errors = new ArrayList<>();
            if (!isValid) {
                errors.add("以下素材不存在或无访问权限: " + missingMaterials);
            }
            
            return MaterialValidationDTO.builder()
                    .isValid(isValid)
                    .totalMaterials(materialIds.size())
                    .validMaterials(materialIds.size() - missingMaterials.size())
                    .invalidMaterials(missingMaterials.size())
                    .missingMaterialIds(missingMaterials)
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
            List<Long> materialIds = parseMaterialReferences(contentData);
            
            if (materialIds.isEmpty()) {
                log.debug("No material references found in program: {}", programId);
                return true;
            }
            
            // TODO: 实际创建ProgramMaterialRef记录
            // programMaterialRefRepository.batchInsert(programId, materialIds, oid);
            
            log.debug("Created {} material dependencies for program: {}", materialIds.size(), programId);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to create material dependencies for program: " + programId, e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean updateMaterialDependencies(Long programId, String contentData, Long oid) {
        log.debug("Updating material dependencies for program: {}", programId);
        
        try {
            // 先删除现有依赖关系
            deleteMaterialDependencies(programId);
            
            // 重新创建依赖关系
            return createMaterialDependencies(programId, contentData, oid);
            
        } catch (Exception e) {
            log.error("Failed to update material dependencies for program: " + programId, e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean deleteMaterialDependencies(Long programId) {
        log.debug("Deleting material dependencies for program: {}", programId);
        
        try {
            // TODO: 实际删除ProgramMaterialRef记录
            // int deletedCount = programMaterialRefRepository.deleteByProgramId(programId);
            int deletedCount = 0; // 临时返回
            
            log.debug("Deleted {} material dependencies for program: {}", deletedCount, programId);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to delete material dependencies for program: " + programId, e);
            return false;
        }
    }
    
    @Override
    public List<MaterialDependencyDTO> findMaterialDependencies(Long programId, Long oid) {
        log.debug("Finding material dependencies for program: {}, oid: {}", programId, oid);
        
        try {
            // TODO: 实际查询ProgramMaterialRef记录并转换为DTO
            // List<ProgramMaterialRef> refs = programMaterialRefRepository.findByProgramIdAndOid(programId, oid);
            // return refs.stream().map(this::convertToDTO).collect(Collectors.toList());
            
            return List.of(); // 临时返回空列表
            
        } catch (Exception e) {
            log.error("Failed to find material dependencies for program: " + programId, e);
            return List.of();
        }
    }
    
    @Override
    public List<Long> findProgramsUsingMaterial(Long materialId, Long oid) {
        log.debug("Finding programs using material: {}, oid: {}", materialId, oid);
        
        try {
            // TODO: 实际查询使用该素材的节目列表
            // return programMaterialRefRepository.findProgramIdsByMaterialIdAndOid(materialId, oid);
            
            return List.of(); // 临时返回空列表
            
        } catch (Exception e) {
            log.error("Failed to find programs using material: " + materialId, e);
            return List.of();
        }
    }
    
    @Override
    public List<MaterialValidationDTO> batchValidateMaterials(List<Long> materialIds, Long oid) {
        log.debug("Batch validating {} materials for oid: {}", materialIds.size(), oid);
        
        return materialIds.stream()
                .map(id -> validateSingleMaterial(id, oid))
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean canDeleteMaterial(Long materialId, Long oid) {
        log.debug("Checking if material can be deleted: {}, oid: {}", materialId, oid);
        
        List<Long> usingPrograms = findProgramsUsingMaterial(materialId, oid);
        boolean canDelete = usingPrograms.isEmpty();
        
        log.debug("Material {} can be deleted: {}, used by {} programs", 
                materialId, canDelete, usingPrograms.size());
        
        return canDelete;
    }
    
    @Override
    public List<Long> parseMaterialReferences(String contentData) {
        if (!StringUtils.hasText(contentData)) {
            return List.of();
        }
        
        try {
            JsonNode rootNode = objectMapper.readTree(contentData);
            Set<Long> materialIds = new java.util.HashSet<>();
            
            // 递归解析JSON，提取所有materialId字段
            extractMaterialIds(rootNode, materialIds);
            
            List<Long> result = new ArrayList<>(materialIds);
            log.debug("Parsed {} material references from content data", result.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("Failed to parse material references from content data", e);
            return List.of();
        }
    }
    
    // ===== 私有辅助方法 =====
    
    private List<Long> validateMaterialsExist(List<Long> materialIds, Long oid) {
        // TODO: 实际调用素材服务验证素材存在性
        // 这里需要集成素材管理服务，验证素材是否存在且有访问权限
        
        List<Long> missingMaterials = new ArrayList<>();
        
        for (Long materialId : materialIds) {
            // 临时逻辑：假设所有素材都存在
            // 实际实现需要查询素材服务
            boolean exists = checkMaterialExists(materialId, oid);
            if (!exists) {
                missingMaterials.add(materialId);
            }
        }
        
        return missingMaterials;
    }
    
    private boolean checkMaterialExists(Long materialId, Long oid) {
        // TODO: 实际检查素材是否存在
        // 可能需要调用file-service或者查询素材数据库
        log.debug("Checking material existence: {}, oid: {}", materialId, oid);
        
        // 临时返回true，实际需要实现素材存在性检查
        return true;
    }
    
    private MaterialValidationDTO validateSingleMaterial(Long materialId, Long oid) {
        boolean exists = checkMaterialExists(materialId, oid);
        
        return MaterialValidationDTO.builder()
                .isValid(exists)
                .totalMaterials(1)
                .validMaterials(exists ? 1 : 0)
                .invalidMaterials(exists ? 0 : 1)
                .missingMaterialIds(exists ? List.of() : List.of(materialId))
                .errors(exists ? List.of() : List.of("素材不存在: " + materialId))
                .build();
    }
    
    private void extractMaterialIds(JsonNode node, Set<Long> materialIds) {
        if (node.isObject()) {
            // 查找materialId字段
            if (node.has("materialId")) {
                JsonNode materialIdNode = node.get("materialId");
                if (materialIdNode.isNumber()) {
                    materialIds.add(materialIdNode.asLong());
                } else if (materialIdNode.isTextual()) {
                    try {
                        materialIds.add(Long.parseLong(materialIdNode.asText()));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid materialId format: {}", materialIdNode.asText());
                    }
                }
            }
            
            // 递归处理所有字段
            node.fields().forEachRemaining(entry -> 
                    extractMaterialIds(entry.getValue(), materialIds));
                    
        } else if (node.isArray()) {
            // 递归处理数组元素
            for (JsonNode element : node) {
                extractMaterialIds(element, materialIds);
            }
        }
    }
}