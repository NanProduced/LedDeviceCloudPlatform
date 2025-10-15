package org.nan.cloud.core.repository;

import org.nan.cloud.program.document.ProgramContent;

import java.util.List;
import java.util.Optional;

/**
 * 节目内容Repository接口
 * 处理节目详细内容的MongoDB数据访问
 */
public interface ProgramContentRepository {
    
    /**
     * 根据节目ID查询内容
     * @param programId 节目ID
     * @return 节目内容列表
     */
    List<ProgramContent> findByProgramId(Long programId);
    
    /**
     * 根据节目ID和版本查询内容
     * @param programId 节目ID
     * @param version 版本号
     * @return 节目内容
     */
    Optional<ProgramContent> findByProgramIdAndVersion(Long programId, Integer version);
    
    /**
     * 查询节目的最新版本内容
     * @param programId 节目ID
     * @return 最新版本内容
     */
    Optional<ProgramContent> findLatestVersionByProgramId(Long programId);
    
    /**
     * 根据MongoDB文档ID查询内容
     * @param id MongoDB文档ID
     * @return 节目内容
     */
    Optional<ProgramContent> findById(String id);
    
    /**
     * 保存节目内容
     * @param content 节目内容
     * @return 保存后的内容
     */
    ProgramContent save(ProgramContent content);
    
    /**
     * 更新VSN XML内容
     * @param programId 节目ID
     * @param version 版本号
     * @param vsnXml VSN XML内容
     * @return 更新的记录数
     */
    int updateVsnXml(Long programId, Integer version, String vsnXml);
    
    /**
     * 批量更新VSN XML内容
     * @param updates 更新列表，Map<documentId, vsnXml>
     * @return 更新的记录数
     */
    int updateVsnXmlBatch(java.util.Map<String, String> updates);
    
    /**
     * 删除节目内容
     * @param id MongoDB文档ID
     * @return 删除的记录数
     */
    int deleteById(String id);
    
    /**
     * 根据节目ID删除所有版本内容
     * @param programId 节目ID
     * @return 删除的记录数
     */
    int deleteByProgramId(Long programId);
    
    /**
     * 根据节目ID和版本删除内容
     * @param programId 节目ID
     * @param version 版本号
     * @return 删除的记录数
     */
    int deleteByProgramIdAndVersion(Long programId, Integer version);
    
    /**
     * 查询节目的所有版本号
     * @param programId 节目ID
     * @return 版本号列表
     */
    List<Integer> findVersionsByProgramId(Long programId);
    
    /**
     * 获取节目的最大版本号
     * @param programId 节目ID
     * @return 最大版本号
     */
    Optional<Integer> findMaxVersionByProgramId(Long programId);
    
    /**
     * 检查节目版本内容是否存在
     * @param programId 节目ID
     * @param version 版本号
     * @return 是否存在
     */
    boolean existsByProgramIdAndVersion(Long programId, Integer version);
    
    /**
     * 统计节目的版本数量
     * @param programId 节目ID
     * @return 版本数量
     */
    long countVersionsByProgramId(Long programId);
}