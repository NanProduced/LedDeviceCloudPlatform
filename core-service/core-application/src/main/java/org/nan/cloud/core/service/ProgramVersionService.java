package org.nan.cloud.core.service;

import org.nan.cloud.program.dto.response.ProgramVersionDTO;
import org.nan.cloud.program.dto.response.VersionComparisonDTO;
import org.nan.cloud.program.dto.response.VersionHistoryDTO;

import java.util.List;
import java.util.Optional;

/**
 * 节目版本控制服务接口
 * 专门处理版本管理相关的高级功能
 */
public interface ProgramVersionService {
    
    /**
     * 获取版本历史记录（包含变更摘要）
     * @param sourceProgramId 原始节目ID
     * @param oid 组织ID
     * @return 版本历史记录
     */
    VersionHistoryDTO getVersionHistory(Long sourceProgramId, Long oid);
    
    /**
     * 比较两个版本的差异
     * @param sourceProgramId 原始节目ID
     * @param version1 版本1
     * @param version2 版本2
     * @param oid 组织ID
     * @return 版本比较结果
     */
    Optional<VersionComparisonDTO> compareVersions(Long sourceProgramId, Integer version1, Integer version2, Long oid);
    
    /**
     * 检查版本是否可以回滚
     * @param sourceProgramId 原始节目ID
     * @param targetVersion 目标版本号
     * @param oid 组织ID
     * @return 是否可以回滚
     */
    boolean canRollbackToVersion(Long sourceProgramId, Integer targetVersion, Long oid);
    
    /**
     * 获取版本的变更摘要
     * @param programId 节目ID
     * @param oid 组织ID
     * @return 变更摘要
     */
    Optional<String> getVersionChangeSummary(Long programId, Long oid);
    
    /**
     * 查找版本链中的关键版本（如第一个发布版本、最新发布版本等）
     * @param sourceProgramId 原始节目ID
     * @param oid 组织ID
     * @return 关键版本信息
     */
    List<ProgramVersionDTO> findKeyVersions(Long sourceProgramId, Long oid);
    
    /**
     * 清理过期版本（保留关键版本）
     * @param sourceProgramId 原始节目ID
     * @param keepCount 保留的版本数量
     * @param oid 组织ID
     * @return 清理的版本数量
     */
    int cleanupOldVersions(Long sourceProgramId, int keepCount, Long oid);
    
    /**
     * 获取版本的发布状态统计
     * @param sourceProgramId 原始节目ID
     * @param oid 组织ID
     * @return 版本状态统计
     */
    VersionStatistics getVersionStatistics(Long sourceProgramId, Long oid);
    
    /**
     * 版本状态统计信息
     */
    public static class VersionStatistics {
        private int totalVersions;
        private int draftVersions;
        private int publishedVersions;
        private int archivedVersions;
        
        // getters and setters
        public int getTotalVersions() { return totalVersions; }
        public void setTotalVersions(int totalVersions) { this.totalVersions = totalVersions; }
        
        public int getDraftVersions() { return draftVersions; }
        public void setDraftVersions(int draftVersions) { this.draftVersions = draftVersions; }
        
        public int getPublishedVersions() { return publishedVersions; }
        public void setPublishedVersions(int publishedVersions) { this.publishedVersions = publishedVersions; }
        
        public int getArchivedVersions() { return archivedVersions; }
        public void setArchivedVersions(int archivedVersions) { this.archivedVersions = archivedVersions; }
    }
}