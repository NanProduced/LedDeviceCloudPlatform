package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.core.api.DTO.res.OrgQuotaDetailResponse;
import org.nan.cloud.core.api.DTO.res.QuotaBreakdownResponse;
import org.nan.cloud.core.api.DTO.res.QuotaTrendResponse;
import org.nan.cloud.core.domain.OrgQuota;
import org.nan.cloud.core.repository.OrgQuotaRepository;
import org.nan.cloud.core.repository.QuotaAnalysisRepository;
import org.nan.cloud.core.service.QuotaService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaServiceImpl implements QuotaService {

    private final OrgQuotaRepository orgQuotaRepository;
    private final QuotaAnalysisRepository quotaAnalysisRepository;

    @Override
    public boolean checkQuotaAllow(Long orgId, long bytes, int files) {
        if (orgId == null || bytes < 0 || files < 0) {
            return false;
        }
        OrgQuota quota = orgQuotaRepository.findByOrgId(orgId);
        if (quota == null) {
            return false;
        }
        long usedBytes = quota.getUsedStorageSize() == null ? 0L : quota.getUsedStorageSize();
        int usedFiles = quota.getUsedFileCount() == null ? 0 : quota.getUsedFileCount();
        long maxBytes = quota.getMaxStorageSize() == null ? 0L : quota.getMaxStorageSize();
        int maxFiles = quota.getMaxFileCount() == null ? 0 : quota.getMaxFileCount();

        boolean bytesOk = maxBytes <= 0 || usedBytes + bytes <= maxBytes;
        boolean filesOk = maxFiles <= 0 || usedFiles + files <= maxFiles;
        return bytesOk && filesOk;
    }

    @Override
    public OrgQuotaDetailResponse getOrgQuotaDetail(Long orgId) {
        log.debug("获取组织配额详情: orgId={}", orgId);
        
        OrgQuota quota = orgQuotaRepository.findByOrgId(orgId);
        if (quota == null) {
            log.warn("组织配额不存在: orgId={}", orgId);
            return null;
        }

        // 计算存储信息
        OrgQuotaDetailResponse.StorageQuotaInfo storageInfo = buildStorageInfo(quota);
        
        // 计算文件信息
        OrgQuotaDetailResponse.FileQuotaInfo fileInfo = buildFileInfo(quota);
        
        // 构建告警信息
        OrgQuotaDetailResponse.WarningInfo warningInfo = buildWarningInfo(quota, storageInfo, fileInfo);
        
        // 构建预测信息
        OrgQuotaDetailResponse.PredictionInfo predictionInfo = buildPredictionInfo(orgId, storageInfo);

        return OrgQuotaDetailResponse.builder()
                .orgId(orgId)
                .storageInfo(storageInfo)
                .fileInfo(fileInfo)
                .warningInfo(warningInfo)
                .predictionInfo(predictionInfo)
                .updatedTime(quota.getUpdatedTime())
                .build();
    }

    @Override
    public QuotaTrendResponse getQuotaTrend(Long orgId, String period, Integer days) {
        log.debug("获取配额使用趋势: orgId={}, period={}, days={}", orgId, period, days);
        
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(days != null ? days : 30);
        
        // 获取趋势数据
        List<Map<String, Object>> trendData = quotaAnalysisRepository.getQuotaUsageTrend(
                orgId, startTime, endTime, period);

        // 转换为响应格式
        List<QuotaTrendResponse.TrendDataPoint> storageUsageTrend = convertToTrendDataPoints(trendData, "bytesChange");
        List<QuotaTrendResponse.TrendDataPoint> fileCountTrend = convertToTrendDataPoints(trendData, "filesChange");

        // 构建趋势摘要
        QuotaTrendResponse.TrendSummary summary = buildTrendSummary(orgId, days != null ? days : 30);

        return QuotaTrendResponse.builder()
                .orgId(orgId)
                .period(period)
                .storageUsageTrend(storageUsageTrend)
                .fileCountTrend(fileCountTrend)
                .summary(summary)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public QuotaBreakdownResponse getQuotaBreakdown(Long orgId) {
        log.debug("获取配额使用分解: orgId={}", orgId);

        // 获取按文件类型分解
        List<Map<String, Object>> fileTypeData = quotaAnalysisRepository.getQuotaBreakdownByFileType(orgId);
        List<QuotaBreakdownResponse.FileTypeBreakdown> fileTypeBreakdown = convertToFileTypeBreakdown(fileTypeData);

        // 获取按用户组分解
        List<Map<String, Object>> userGroupData = quotaAnalysisRepository.getQuotaBreakdownByUserGroup(orgId);
        List<QuotaBreakdownResponse.UserGroupBreakdown> userGroupBreakdown = convertToUserGroupBreakdown(userGroupData);

        // 获取按操作类型分解
        List<Map<String, Object>> operationTypeData = quotaAnalysisRepository.getQuotaBreakdownByOperationType(orgId, 30);
        List<QuotaBreakdownResponse.OperationTypeBreakdown> operationTypeBreakdown = convertToOperationTypeBreakdown(operationTypeData);

        // 构建摘要
        QuotaBreakdownResponse.BreakdownSummary summary = buildBreakdownSummary(orgId, fileTypeBreakdown, userGroupBreakdown);

        return QuotaBreakdownResponse.builder()
                .orgId(orgId)
                .fileTypeBreakdown(fileTypeBreakdown)
                .userGroupBreakdown(userGroupBreakdown)
                .operationTypeBreakdown(operationTypeBreakdown)
                .summary(summary)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    // ==================== 私有辅助方法 ====================

    private OrgQuotaDetailResponse.StorageQuotaInfo buildStorageInfo(OrgQuota quota) {
        Long maxSize = quota.getMaxStorageSize() != null ? quota.getMaxStorageSize() : 0L;
        Long usedSize = quota.getUsedStorageSize() != null ? quota.getUsedStorageSize() : 0L;
        Long remainingSize = Math.max(0L, maxSize - usedSize);
        Double usagePercentage = maxSize > 0 ? (usedSize * 100.0 / maxSize) : 0.0;
        
        String status = getQuotaStatus(usagePercentage, quota.getWarningThresholdPercent());

        return OrgQuotaDetailResponse.StorageQuotaInfo.builder()
                .maxSize(maxSize)
                .usedSize(usedSize)
                .remainingSize(remainingSize)
                .usagePercentage(usagePercentage)
                .status(status)
                .build();
    }

    private OrgQuotaDetailResponse.FileQuotaInfo buildFileInfo(OrgQuota quota) {
        Integer maxCount = quota.getMaxFileCount() != null ? quota.getMaxFileCount() : 0;
        Integer usedCount = quota.getUsedFileCount() != null ? quota.getUsedFileCount() : 0;
        Integer remainingCount = Math.max(0, maxCount - usedCount);
        Double usagePercentage = maxCount > 0 ? (usedCount * 100.0 / maxCount) : 0.0;
        
        String status = getQuotaStatus(usagePercentage, quota.getWarningThresholdPercent());

        return OrgQuotaDetailResponse.FileQuotaInfo.builder()
                .maxCount(maxCount)
                .usedCount(usedCount)
                .remainingCount(remainingCount)
                .usagePercentage(usagePercentage)
                .status(status)
                .build();
    }

    private OrgQuotaDetailResponse.WarningInfo buildWarningInfo(OrgQuota quota, 
                                                               OrgQuotaDetailResponse.StorageQuotaInfo storageInfo,
                                                               OrgQuotaDetailResponse.FileQuotaInfo fileInfo) {
        Integer threshold = quota.getWarningThresholdPercent() != null ? quota.getWarningThresholdPercent() : 80;
        
        boolean isStorageWarning = storageInfo.getUsagePercentage() >= threshold;
        boolean isFileWarning = fileInfo.getUsagePercentage() >= threshold;
        boolean isWarning = isStorageWarning || isFileWarning;
        
        String warningMessage = null;
        if (isWarning) {
            if (isStorageWarning && isFileWarning) {
                warningMessage = "存储空间和文件数量均已达到告警阈值";
            } else if (isStorageWarning) {
                warningMessage = "存储空间已达到告警阈值";
            } else {
                warningMessage = "文件数量已达到告警阈值";
            }
        }

        return OrgQuotaDetailResponse.WarningInfo.builder()
                .thresholdPercent(threshold)
                .isWarning(isWarning)
                .warningMessage(warningMessage)
                .build();
    }

    private OrgQuotaDetailResponse.PredictionInfo buildPredictionInfo(Long orgId, 
                                                                     OrgQuotaDetailResponse.StorageQuotaInfo storageInfo) {
        try {
            Map<String, Object> growthAnalysis = quotaAnalysisRepository.getQuotaGrowthAnalysis(orgId, 30);
            
            Object avgGrowthObj = growthAnalysis.get("averageDailyGrowth");
            Long dailyGrowthBytes = avgGrowthObj != null ? ((Number) avgGrowthObj).longValue() : 0L;
            
            Integer estimatedDaysRemaining = null;
            String growthTrend = "STABLE";
            
            if (dailyGrowthBytes > 0 && storageInfo.getRemainingSize() > 0) {
                estimatedDaysRemaining = (int) (storageInfo.getRemainingSize() / dailyGrowthBytes);
                growthTrend = "GROWING";
            } else if (dailyGrowthBytes < 0) {
                growthTrend = "DECLINING";
            }

            return OrgQuotaDetailResponse.PredictionInfo.builder()
                    .estimatedDaysRemaining(estimatedDaysRemaining)
                    .growthTrend(growthTrend)
                    .dailyGrowthBytes(dailyGrowthBytes)
                    .build();
        } catch (Exception e) {
            log.warn("构建预测信息失败: orgId={}, error={}", orgId, e.getMessage());
            return OrgQuotaDetailResponse.PredictionInfo.builder()
                    .growthTrend("STABLE")
                    .dailyGrowthBytes(0L)
                    .build();
        }
    }

    private String getQuotaStatus(Double usagePercentage, Integer warningThreshold) {
        int threshold = warningThreshold != null ? warningThreshold : 80;
        
        if (usagePercentage >= 95) {
            return "CRITICAL";
        } else if (usagePercentage >= threshold) {
            return "WARNING";
        } else {
            return "NORMAL";
        }
    }

    private List<QuotaTrendResponse.TrendDataPoint> convertToTrendDataPoints(List<Map<String, Object>> trendData, String valueField) {
        if (CollectionUtils.isEmpty(trendData)) {
            return new ArrayList<>();
        }

        List<QuotaTrendResponse.TrendDataPoint> dataPoints = new ArrayList<>();
        Long previousValue = null;

        for (Map<String, Object> data : trendData) {
            Object timestampObj = data.get("timestamp");
            Object valueObj = data.get(valueField);
            
            LocalDateTime timestamp = parseTimestamp(timestampObj);
            Long value = valueObj != null ? ((Number) valueObj).longValue() : 0L;
            
            Long deltaValue = previousValue != null ? value - previousValue : 0L;
            Double deltaPercentage = previousValue != null && previousValue != 0 
                    ? (deltaValue * 100.0 / Math.abs(previousValue)) : 0.0;

            dataPoints.add(QuotaTrendResponse.TrendDataPoint.builder()
                    .timestamp(timestamp)
                    .value(value)
                    .deltaValue(deltaValue)
                    .deltaPercentage(deltaPercentage)
                    .build());
            
            previousValue = value;
        }

        return dataPoints;
    }

    private QuotaTrendResponse.TrendSummary buildTrendSummary(Long orgId, int days) {
        try {
            Map<String, Object> growthAnalysis = quotaAnalysisRepository.getQuotaGrowthAnalysis(orgId, days);
            
            Object avgGrowthObj = growthAnalysis.get("averageDailyGrowth");
            Object maxGrowthObj = growthAnalysis.get("maxDailyGrowth");
            
            Long averageDailyGrowth = avgGrowthObj != null ? ((Number) avgGrowthObj).longValue() : 0L;
            Long maxDailyGrowth = maxGrowthObj != null ? ((Number) maxGrowthObj).longValue() : 0L;
            
            String overallTrend = averageDailyGrowth > 0 ? "INCREASING" : 
                                 averageDailyGrowth < 0 ? "DECREASING" : "STABLE";

            return QuotaTrendResponse.TrendSummary.builder()
                    .overallTrend(overallTrend)
                    .averageDailyGrowth(averageDailyGrowth)
                    .maxDailyGrowth(maxDailyGrowth)
                    .growthRate(0.0) // 可以进一步计算
                    .build();
        } catch (Exception e) {
            log.warn("构建趋势摘要失败: orgId={}, error={}", orgId, e.getMessage());
            return QuotaTrendResponse.TrendSummary.builder()
                    .overallTrend("STABLE")
                    .averageDailyGrowth(0L)
                    .maxDailyGrowth(0L)
                    .growthRate(0.0)
                    .build();
        }
    }

    private List<QuotaBreakdownResponse.FileTypeBreakdown> convertToFileTypeBreakdown(List<Map<String, Object>> data) {
        return data.stream().map(item -> {
            String fileType = (String) item.get("fileType");
            Long usedBytes = item.get("usedBytes") != null ? ((Number) item.get("usedBytes")).longValue() : 0L;
            Integer fileCount = item.get("fileCount") != null ? ((Number) item.get("fileCount")).intValue() : 0;
            Long averageFileSize = fileCount > 0 ? usedBytes / fileCount : 0L;

            return QuotaBreakdownResponse.FileTypeBreakdown.builder()
                    .fileType(fileType)
                    .fileTypeDisplayName(getFileTypeDisplayName(fileType))
                    .usedBytes(usedBytes)
                    .fileCount(fileCount)
                    .averageFileSize(averageFileSize)
                    .build();
        }).collect(Collectors.toList());
    }

    private List<QuotaBreakdownResponse.UserGroupBreakdown> convertToUserGroupBreakdown(List<Map<String, Object>> data) {
        return data.stream().map(item -> {
            Long userGroupId = item.get("userGroupId") != null ? ((Number) item.get("userGroupId")).longValue() : 0L;
            Long usedBytes = item.get("usedBytes") != null ? ((Number) item.get("usedBytes")).longValue() : 0L;
            Integer fileCount = item.get("fileCount") != null ? ((Number) item.get("fileCount")).intValue() : 0;
            Integer activeUserCount = item.get("activeUserCount") != null ? ((Number) item.get("activeUserCount")).intValue() : 0;

            return QuotaBreakdownResponse.UserGroupBreakdown.builder()
                    .userGroupId(userGroupId)
                    .userGroupName("用户组-" + userGroupId) // 可以从UserGroup表查询实际名称
                    .usedBytes(usedBytes)
                    .fileCount(fileCount)
                    .activeUserCount(activeUserCount)
                    .build();
        }).collect(Collectors.toList());
    }

    private List<QuotaBreakdownResponse.OperationTypeBreakdown> convertToOperationTypeBreakdown(List<Map<String, Object>> data) {
        return data.stream().map(item -> {
            String operationType = (String) item.get("operationType");
            Long totalBytes = item.get("totalBytes") != null ? ((Number) item.get("totalBytes")).longValue() : 0L;
            Integer operationCount = item.get("operationCount") != null ? ((Number) item.get("operationCount")).intValue() : 0;
            Object lastOpTimeObj = item.get("lastOperationTime");
            LocalDateTime lastOperationTime = parseTimestamp(lastOpTimeObj);

            return QuotaBreakdownResponse.OperationTypeBreakdown.builder()
                    .operationType(operationType)
                    .operationDisplayName(getOperationTypeDisplayName(operationType))
                    .operationCount(operationCount)
                    .totalBytes(totalBytes)
                    .lastOperationTime(lastOperationTime)
                    .build();
        }).collect(Collectors.toList());
    }

    private QuotaBreakdownResponse.BreakdownSummary buildBreakdownSummary(Long orgId,
                                                                         List<QuotaBreakdownResponse.FileTypeBreakdown> fileTypeBreakdown,
                                                                         List<QuotaBreakdownResponse.UserGroupBreakdown> userGroupBreakdown) {
        Long totalUsedBytes = fileTypeBreakdown.stream()
                .mapToLong(QuotaBreakdownResponse.FileTypeBreakdown::getUsedBytes)
                .sum();
        
        Integer totalFileCount = fileTypeBreakdown.stream()
                .mapToInt(QuotaBreakdownResponse.FileTypeBreakdown::getFileCount)
                .sum();

        String topFileTypeByStorage = fileTypeBreakdown.stream()
                .max(Comparator.comparing(QuotaBreakdownResponse.FileTypeBreakdown::getUsedBytes))
                .map(QuotaBreakdownResponse.FileTypeBreakdown::getFileTypeDisplayName)
                .orElse("无");

        String topUserGroupByCount = userGroupBreakdown.stream()
                .max(Comparator.comparing(QuotaBreakdownResponse.UserGroupBreakdown::getFileCount))
                .map(QuotaBreakdownResponse.UserGroupBreakdown::getUserGroupName)
                .orElse("无");

        return QuotaBreakdownResponse.BreakdownSummary.builder()
                .totalUsedBytes(totalUsedBytes)
                .totalFileCount(totalFileCount)
                .topFileTypeByStorage(topFileTypeByStorage)
                .topUserGroupByCount(topUserGroupByCount)
                .statisticTimeRange("最近30天")
                .build();
    }

    private String getFileTypeDisplayName(String fileType) {
        return switch (fileType) {
            case "MATERIAL" -> "素材文件";
            case "VSN" -> "节目文件";
            case "EXPORT_FILE" -> "导出文件";
            default -> fileType;
        };
    }

    private String getOperationTypeDisplayName(String operationType) {
        return switch (operationType) {
            case "MATERIAL_UPLOAD" -> "素材上传";
            case "MATERIAL_DELETE" -> "素材删除";
            case "VSN_UPLOAD" -> "节目生成";
            case "VSN_DELETE" -> "节目删除";
            case "EXPORT_FILE_GENERATE" -> "导出文件生成";
            case "EXPORT_FILE_DELETE" -> "导出文件删除";
            default -> operationType;
        };
    }

    private LocalDateTime parseTimestamp(Object timestampObj) {
        if (timestampObj instanceof LocalDateTime) {
            return (LocalDateTime) timestampObj;
        } else if (timestampObj instanceof String) {
            try {
                return LocalDateTime.parse((String) timestampObj);
            } catch (Exception e) {
                return LocalDateTime.now();
            }
        }
        return LocalDateTime.now();
    }
}

