package org.nan.cloud.program.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 版本比较结果DTO
 * 描述两个版本之间的差异
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VersionComparisonDTO {
    
    /**
     * 原始节目ID
     */
    private Long sourceProgramId;
    
    /**
     * 节目名称
     */
    private String programName;
    
    /**
     * 版本1信息
     */
    private VersionInfo version1;
    
    /**
     * 版本2信息
     */
    private VersionInfo version2;
    
    /**
     * 差异摘要
     */
    private ComparisonSummary summary;
    
    /**
     * 详细差异列表
     */
    private List<FieldDifference> differences;
    
    /**
     * 素材差异
     */
    private MaterialDifferences materialDifferences;
    
    /**
     * 版本信息
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class VersionInfo {
        private Long programId;
        private Integer version;
        private String status;
        private LocalDateTime createdTime;
        private String createdByName;
    }
    
    /**
     * 比较摘要
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ComparisonSummary {
        private Integer totalDifferences;
        private Integer metadataDifferences;
        private Integer contentDifferences;
        private Integer materialDifferences;
        private Boolean hasSignificantChanges;
        private String changeSeverity; // MINOR, MAJOR, CRITICAL
    }
    
    /**
     * 字段差异
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class FieldDifference {
        private String fieldPath;
        private String fieldName;
        private Object oldValue;
        private Object newValue;
        private String changeType; // ADDED, REMOVED, MODIFIED
        private String fieldCategory; // METADATA, CONTENT, LAYOUT
        private String severity; // MINOR, MAJOR, CRITICAL
    }
    
    /**
     * 素材差异
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class MaterialDifferences {
        private List<Long> addedMaterials;
        private List<Long> removedMaterials;
        private List<Long> modifiedMaterials;
        private Map<Long, String> materialNames;
        private Integer totalMaterialChanges;
    }
}