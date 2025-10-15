package org.nan.cloud.core.api.DTO.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 配额使用分解响应DTO
 * 用于显示配额使用的详细分类统计
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaBreakdownResponse {

    /**
     * 组织ID
     */
    private Long orgId;

    /**
     * 按文件类型分解
     */
    private List<FileTypeBreakdown> fileTypeBreakdown;

    /**
     * 按用户组分解
     */
    private List<UserGroupBreakdown> userGroupBreakdown;

    /**
     * 按操作类型分解
     */
    private List<OperationTypeBreakdown> operationTypeBreakdown;

    /**
     * 统计摘要
     */
    private BreakdownSummary summary;

    /**
     * 数据生成时间
     */
    private LocalDateTime generatedAt;

    /**
     * 按文件类型的配额分解
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileTypeBreakdown {
        /**
         * 文件类型: MATERIAL, VSN, EXPORT_FILE
         */
        private String fileType;
        
        /**
         * 文件类型显示名称
         */
        private String fileTypeDisplayName;
        
        /**
         * 占用存储空间（字节）
         */
        private Long usedBytes;
        
        /**
         * 文件数量
         */
        private Integer fileCount;
        
        /**
         * 占总存储的百分比
         */
        private Double storagePercentage;
        
        /**
         * 占总文件数的百分比
         */
        private Double countPercentage;
        
        /**
         * 平均文件大小（字节）
         */
        private Long averageFileSize;
    }

    /**
     * 按用户组的配额分解
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserGroupBreakdown {
        /**
         * 用户组ID
         */
        private Long userGroupId;
        
        /**
         * 用户组名称
         */
        private String userGroupName;
        
        /**
         * 占用存储空间（字节）
         */
        private Long usedBytes;
        
        /**
         * 文件数量
         */
        private Integer fileCount;
        
        /**
         * 占总存储的百分比
         */
        private Double storagePercentage;
        
        /**
         * 占总文件数的百分比
         */
        private Double countPercentage;
        
        /**
         * 活跃用户数
         */
        private Integer activeUserCount;
    }

    /**
     * 按操作类型的配额分解
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperationTypeBreakdown {
        /**
         * 操作类型
         */
        private String operationType;
        
        /**
         * 操作显示名称
         */
        private String operationDisplayName;
        
        /**
         * 操作次数
         */
        private Integer operationCount;
        
        /**
         * 涉及存储空间（字节）
         */
        private Long totalBytes;
        
        /**
         * 占总操作的百分比
         */
        private Double operationPercentage;
        
        /**
         * 最近操作时间
         */
        private LocalDateTime lastOperationTime;
    }

    /**
     * 分解统计摘要
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BreakdownSummary {
        /**
         * 总存储使用量（字节）
         */
        private Long totalUsedBytes;
        
        /**
         * 总文件数量
         */
        private Integer totalFileCount;
        
        /**
         * 最占空间的文件类型
         */
        private String topFileTypeByStorage;
        
        /**
         * 最多文件的用户组
         */
        private String topUserGroupByCount;
        
        /**
         * 最活跃的操作类型
         */
        private String mostActiveOperationType;
        
        /**
         * 统计的时间范围
         */
        private String statisticTimeRange;
        
        /**
         * 扩展信息
         */
        private Map<String, Object> extras;
    }
}