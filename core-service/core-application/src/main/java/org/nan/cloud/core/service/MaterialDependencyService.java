package org.nan.cloud.core.service;

import org.nan.cloud.program.dto.response.MaterialDependencyDTO;
import org.nan.cloud.program.dto.response.MaterialValidationDTO;

import java.util.List;

/**
 * 素材依赖管理服务接口
 * 处理节目与素材之间的依赖关系管理
 */
public interface MaterialDependencyService {
    
    /**
     * 验证节目内容中的素材依赖
     * @param contentData 节目内容数据（JSON）
     * @param oid 组织ID
     * @return 验证结果
     */
    MaterialValidationDTO validateMaterialDependencies(String contentData, Long oid);
    
    /**
     * 创建节目素材依赖关系
     * @param programId 节目ID
     * @param contentData 节目内容数据（JSON）
     * @param oid 组织ID
     * @return 是否成功
     */
    boolean createMaterialDependencies(Long programId, String contentData, Long oid);
    
    /**
     * 更新节目素材依赖关系
     * @param programId 节目ID
     * @param contentData 节目内容数据（JSON）
     * @param oid 组织ID
     * @return 是否成功
     */
    boolean updateMaterialDependencies(Long programId, String contentData, Long oid);
    
    /**
     * 删除节目素材依赖关系
     * @param programId 节目ID
     * @return 是否成功
     */
    boolean deleteMaterialDependencies(Long programId);
    
    /**
     * 查询节目的素材依赖列表
     * @param programId 节目ID
     * @param oid 组织ID
     * @return 素材依赖列表
     */
    List<MaterialDependencyDTO> findMaterialDependencies(Long programId, Long oid);
    
    /**
     * 查询素材的使用情况（被哪些节目引用）
     * @param materialId 素材ID
     * @param oid 组织ID
     * @return 依赖该素材的节目列表
     */
    List<Long> findProgramsUsingMaterial(Long materialId, Long oid);
    
    /**
     * 批量验证素材可用性
     * @param materialIds 素材ID列表
     * @param oid 组织ID
     * @return 验证结果列表
     */
    List<MaterialValidationDTO> batchValidateMaterials(List<Long> materialIds, Long oid);
    
    /**
     * 检查素材是否可以删除（未被任何节目使用）
     * @param materialId 素材ID
     * @param oid 组织ID
     * @return 是否可以删除
     */
    boolean canDeleteMaterial(Long materialId, Long oid);
    
    /**
     * 解析内容数据中的素材引用
     * @param contentData 节目内容数据（JSON）
     * @return 素材ID列表
     */
    List<Long> parseMaterialReferences(String contentData);
}