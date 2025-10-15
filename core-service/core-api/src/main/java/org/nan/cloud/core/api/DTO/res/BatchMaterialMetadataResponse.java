package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 批量查询素材元数据响应
 * 
 * 高效的批量元数据查询响应设计:
 * - 包含成功查询的元数据列表
 * - 明确标识未找到的素材ID
 * - 提供查询统计信息，便于前端处理
 * - 支持部分成功的查询结果
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "批量查询素材元数据响应")
public class BatchMaterialMetadataResponse {
    
    // ========================= 核心数据 =========================
    
    @Schema(description = "元数据列表")
    private List<MaterialMetadataItem> metadataList;
    
    @Schema(description = "未找到元数据的素材ID列表")
    private List<Long> notFoundMaterialIds;
    
    @Schema(description = "查询失败的素材ID及错误信息")
    private Map<Long, String> failedMaterialIds;
    
    // ========================= 查询统计信息 =========================
    
    @Schema(description = "查询总数", example = "10")
    private Integer totalRequested;
    
    @Schema(description = "成功查询数", example = "8")
    private Integer successCount;
    
    @Schema(description = "未找到数", example = "1")
    private Integer notFoundCount;
    
    @Schema(description = "查询失败数", example = "1")
    private Integer failedCount;
    
    @Schema(description = "查询成功率", example = "0.8")
    private Double successRate;
    
    // ========================= 性能信息 =========================
    
    @Schema(description = "查询耗时(毫秒)", example = "156")
    private Long queryTimeMs;
    
    @Schema(description = "MySQL查询耗时(毫秒)", example = "45")
    private Long mysqlQueryTimeMs;
    
    @Schema(description = "MongoDB查询耗时(毫秒)", example = "89")
    private Long mongodbQueryTimeMs;
    
    @Schema(description = "数据组装耗时(毫秒)", example = "22")
    private Long assemblyTimeMs;
    
    // ========================= 查询元信息 =========================
    
    @Schema(description = "查询执行时间")
    private LocalDateTime queryExecutedAt;
    
    @Schema(description = "查询请求ID", example = "req_20241201_001")
    private String requestId;
    
    @Schema(description = "是否使用了缓存", example = "false")
    private Boolean cacheUsed;
    
    @Schema(description = "响应数据版本", example = "v1.0")
    private String responseVersion;
    
    // ========================= 便捷方法 =========================
    
    /**
     * 判断是否所有请求都成功
     */
    public boolean isAllSuccessful() {
        return successCount != null && totalRequested != null 
               && successCount.equals(totalRequested);
    }
    
    /**
     * 判断是否有部分失败
     */
    public boolean hasPartialFailure() {
        return successCount != null && totalRequested != null 
               && successCount < totalRequested && successCount > 0;
    }
    
    /**
     * 判断是否完全失败
     */
    public boolean isCompletelyFailed() {
        return successCount != null && successCount == 0;
    }
    
    /**
     * 获取查询汇总信息
     */
    public String getSummary() {
        if (totalRequested == null || successCount == null) {
            return "查询统计信息不完整";
        }
        
        return String.format("查询完成: 总数 %d, 成功 %d, 未找到 %d, 失败 %d, 成功率 %.1f%%",
                totalRequested, successCount, 
                notFoundCount != null ? notFoundCount : 0,
                failedCount != null ? failedCount : 0,
                successRate != null ? successRate * 100 : 0.0);
    }
    
    /**
     * 获取性能统计信息
     */
    public String getPerformanceSummary() {
        return String.format("性能统计: 总耗时 %dms (MySQL: %dms, MongoDB: %dms, 组装: %dms)",
                queryTimeMs != null ? queryTimeMs : 0,
                mysqlQueryTimeMs != null ? mysqlQueryTimeMs : 0,
                mongodbQueryTimeMs != null ? mongodbQueryTimeMs : 0,
                assemblyTimeMs != null ? assemblyTimeMs : 0);
    }
}