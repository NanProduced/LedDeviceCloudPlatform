package org.nan.cloud.program.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 版本历史记录DTO
 * 描述节目的完整版本历史
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VersionHistoryDTO {
    
    /**
     * 原始节目ID
     */
    private Long sourceProgramId;
    
    /**
     * 节目名称
     */
    private String programName;
    
    /**
     * 总版本数
     */
    private Integer totalVersions;
    
    /**
     * 最新版本号
     */
    private Integer latestVersion;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdatedTime;
    
    /**
     * 版本列表（按版本号倒序）
     */
    private List<VersionHistoryItem> versions;
    
    /**
     * 版本历史项目
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class VersionHistoryItem {
        
        /**
         * 节目ID
         */
        private Long programId;
        
        /**
         * 版本号
         */
        private Integer version;
        
        /**
         * 版本状态
         */
        private String status;
        
        /**
         * VSN生成状态
         */
        private String vsnStatus;
        
        /**
         * 创建者ID
         */
        private Long createdBy;
        
        /**
         * 创建者名称
         */
        private String createdByName;
        
        /**
         * 创建时间
         */
        private LocalDateTime createdTime;
        
        /**
         * 变更摘要
         */
        private String changeSummary;
        
        /**
         * 是否为关键版本（如首次发布）
         */
        private Boolean isKeyVersion;
        
        /**
         * 关键版本类型
         */
        private String keyVersionType;
        
        /**
         * 版本标签
         */
        private List<String> tags;
    }
}