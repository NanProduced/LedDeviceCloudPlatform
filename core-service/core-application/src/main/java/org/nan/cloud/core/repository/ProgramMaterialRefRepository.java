package org.nan.cloud.core.repository;

import org.nan.cloud.core.domain.ProgramMaterialRef;

import java.util.List;
import java.util.Map;

/**
 * 节目素材引用Repository接口
 * 处理节目与素材关系的MySQL数据访问
 */
public interface ProgramMaterialRefRepository {
    
    /**
     * 根据节目ID查询素材引用列表
     * @param programId 节目ID
     * @return 素材引用列表
     */
    List<ProgramMaterialRef> findByProgramId(Long programId);
    
    /**
     * 根据节目ID和版本查询素材引用列表
     * @param programId 节目ID
     * @param version 版本号
     * @return 素材引用列表
     */
    List<ProgramMaterialRef> findByProgramIdAndVersion(Long programId, Integer version);
    
    /**
     * 根据素材ID查询引用该素材的节目列表
     * @param materialId 素材ID
     * @return 节目素材引用列表
     */
    List<ProgramMaterialRef> findByMaterialId(Long materialId);
    
    /**
     * 检查素材是否被节目使用
     * @param materialId 素材ID
     * @return 是否被使用
     */
    boolean isMaterialUsed(Long materialId);
    
    /**
     * 批量保存节目素材引用
     * @param refs 素材引用列表
     * @return 保存的记录数
     */
    void saveBatch(List<ProgramMaterialRef> refs);
    
    /**
     * 根据节目ID删除所有引用
     * @param programId 节目ID
     * @return 删除的记录数
     */
    int deleteByProgramId(Long programId);
    
    /**
     * 根据节目ID和版本删除引用
     * @param programId 节目ID
     * @param version 版本号
     * @return 删除的记录数
     */
    int deleteByProgramIdAndVersion(Long programId, Integer version);
    
    /**
     * 统计素材被使用的次数
     * @param materialId 素材ID
     * @return 使用次数
     */
    long countUsageByMaterialId(Long materialId);
    
    /**
     * 查询节目的素材类型统计
     * @param programId 节目ID
     * @param version 版本号
     * @return 素材类型统计Map
     */
    Map<String, Integer> getMaterialTypeStatistics(Long programId, Integer version);
}