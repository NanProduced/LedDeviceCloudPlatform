package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.domain.MaterialMetadata;
import org.nan.cloud.core.api.DTO.req.BatchMaterialMetadataRequest;
import org.nan.cloud.core.api.DTO.res.BatchMaterialMetadataResponse;
import org.nan.cloud.core.api.DTO.res.MaterialMetadataItem;
import org.nan.cloud.core.domain.Material;
import org.nan.cloud.core.repository.MaterialMetadataRepository;
import org.nan.cloud.core.repository.MaterialRepository;
import org.nan.cloud.core.service.MaterialMetadataService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 素材元数据服务实现
 * 
 * 高性能批量查询实现:
 * 1. 数据源分离: MySQL(基础信息) + MongoDB(元数据)
 * 2. 批量操作: 减少数据库查询次数，避免N+1问题
 * 3. 内存组装: 高效的数据关联和转换
 * 4. 性能监控: 详细的执行时间统计
 * 5. 错误处理: 支持部分成功，不影响整体查询
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MaterialMetadataServiceImpl implements MaterialMetadataService {
    
    private final MaterialRepository materialRepository;
    private final MaterialMetadataRepository materialMetadataRepository;
    
    // 性能统计计数器
    private final AtomicLong totalQueries = new AtomicLong(0);
    private final AtomicLong totalQueryTime = new AtomicLong(0);
    private final AtomicLong totalMaterialsQueried = new AtomicLong(0);
    
    @Override
    public BatchMaterialMetadataResponse batchGetMaterialMetadata(BatchMaterialMetadataRequest request) {
        StopWatch stopWatch = new StopWatch("BatchMaterialMetadata");
        stopWatch.start("validation");
        
        // 输入验证
        if (request == null || CollectionUtils.isEmpty(request.getMaterialIds())) {
            return buildEmptyResponse("请求参数为空或素材ID列表为空", stopWatch);
        }
        
        List<Long> materialIds = request.getMaterialIds();
        String requestId = generateRequestId();
        
        log.debug("开始批量查询素材元数据 - 请求ID: {}, 素材数量: {}", requestId, materialIds.size());
        
        stopWatch.stop();
        
        try {
            // Step 1: 批量查询Material基础信息 (MySQL)
            stopWatch.start("mysql-query");
            Map<Long, Material> materialMap = batchQueryMaterials(materialIds);
            stopWatch.stop();
            long mysqlTime = stopWatch.lastTaskInfo().getTimeMillis();
            
            // Step 2: 提取fileId列表，批量查询MongoDB元数据
            stopWatch.start("mongodb-query");
            List<String> fileIds = materialMap.values().stream()
                .map(Material::getFileId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
            Map<String, MaterialMetadata> metadataMap = batchQueryMetadata(fileIds);
            stopWatch.stop();
            long mongodbTime = stopWatch.lastTaskInfo().getTimeMillis();
            
            // Step 3: 组装响应数据
            stopWatch.start("assembly");
            BatchMaterialMetadataResponse response = assembleResponse(
                request, materialIds, materialMap, metadataMap, requestId, mysqlTime, mongodbTime
            );
            stopWatch.stop();
            long assemblyTime = stopWatch.lastTaskInfo().getTimeMillis();
            
            // 更新性能统计
            updatePerformanceStats(materialIds.size(), stopWatch.getTotalTimeMillis());
            
            // 完善响应中的性能信息
            response.setAssemblyTimeMs(assemblyTime);
            
            log.info("批量查询完成 - 请求ID: {}, 总耗时: {}ms, 成功: {}/{}", 
                    requestId, stopWatch.getTotalTimeMillis(), 
                    response.getSuccessCount(), response.getTotalRequested());
            
            return response;
            
        } catch (Exception e) {
            log.error("批量查询素材元数据失败 - 请求ID: {}, 错误: {}", requestId, e.getMessage(), e);
            return buildErrorResponse(materialIds, "查询过程中发生系统错误: " + e.getMessage(), stopWatch);
        }
    }
    
    @Override
    public MaterialMetadataItem getMaterialMetadata(Long materialId) {
        return getMaterialMetadata(materialId, true, true, true, true);
    }
    
    @Override
    public MaterialMetadataItem getMaterialMetadata(Long materialId, 
                                                   Boolean includeThumbnails,
                                                   Boolean includeBasicInfo,
                                                   Boolean includeImageMetadata,
                                                   Boolean includeVideoMetadata) {
        if (materialId == null) {
            return null;
        }
        
        // 构建单个素材的批量查询请求
        BatchMaterialMetadataRequest request = BatchMaterialMetadataRequest.builder()
            .materialIds(Collections.singletonList(materialId))
            .includeThumbnails(includeThumbnails)
            .includeBasicInfo(includeBasicInfo)
            .includeImageMetadata(includeImageMetadata)
            .includeVideoMetadata(includeVideoMetadata)
            .includeAiAnalysis(false)
            .includeLedMetadata(false)
            .build();
            
        BatchMaterialMetadataResponse response = batchGetMaterialMetadata(request);
        
        if (response != null && !CollectionUtils.isEmpty(response.getMetadataList())) {
            return response.getMetadataList().get(0);
        }
        
        return null;
    }
    
    @Override
    public boolean hasMetadata(Long materialId) {
        if (materialId == null) {
            return false;
        }
        
        try {
            Material material = materialRepository.getMaterialById(materialId);
            if (material == null || material.getFileId() == null) {
                return false;
            }
            
            return materialMetadataRepository.existsByFileId(material.getFileId());
        } catch (Exception e) {
            log.warn("检查素材元数据存在性失败 - 素材ID: {}, 错误: {}", materialId, e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getPerformanceStats() {
        long queries = totalQueries.get();
        long totalTime = totalQueryTime.get();
        long totalMaterials = totalMaterialsQueried.get();
        
        if (queries == 0) {
            return "暂无查询统计数据";
        }
        
        double avgTime = (double) totalTime / queries;
        double avgMaterialsPerQuery = (double) totalMaterials / queries;
        
        return String.format(
            "性能统计 - 总查询次数: %d, 总耗时: %dms, 平均耗时: %.1fms/次, " +
            "总素材数: %d, 平均素材数: %.1f个/次",
            queries, totalTime, avgTime, totalMaterials, avgMaterialsPerQuery
        );
    }
    
    // ========================= 私有方法 =========================
    
    /**
     * 批量查询Material对象
     */
    private Map<Long, Material> batchQueryMaterials(List<Long> materialIds) {
        try {
            List<Material> materials = materialRepository.batchGetMaterialsByIds(materialIds);
            log.debug("MySQL批量查询完成 - 请求: {}, 返回: {}", materialIds.size(), materials.size());
            
            return materials.stream()
                .collect(Collectors.toMap(Material::getMid, Function.identity()));
        } catch (Exception e) {
            log.error("批量查询Material失败 - 素材IDs: {}, 错误: {}", materialIds, e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * 批量查询MaterialMetadata对象
     */
    private Map<String, MaterialMetadata> batchQueryMetadata(List<String> fileIds) {
        if (CollectionUtils.isEmpty(fileIds)) {
            return new HashMap<>();
        }
        
        try {
            List<MaterialMetadata> metadataList = materialMetadataRepository.batchFindByFileIds(fileIds);
            log.debug("MongoDB批量查询完成 - 请求: {}, 返回: {}", fileIds.size(), metadataList.size());
            
            return metadataList.stream()
                .collect(Collectors.toMap(MaterialMetadata::getFileId, Function.identity()));
        } catch (Exception e) {
            log.error("批量查询MaterialMetadata失败 - 文件IDs: {}, 错误: {}", fileIds, e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * 组装响应数据
     */
    private BatchMaterialMetadataResponse assembleResponse(
            BatchMaterialMetadataRequest request,
            List<Long> materialIds,
            Map<Long, Material> materialMap,
            Map<String, MaterialMetadata> metadataMap,
            String requestId,
            long mysqlTime,
            long mongodbTime) {
        
        List<MaterialMetadataItem> metadataList = new ArrayList<>();
        List<Long> notFoundIds = new ArrayList<>();
        Map<Long, String> failedIds = new HashMap<>();
        
        for (Long materialId : materialIds) {
            try {
                Material material = materialMap.get(materialId);
                if (material == null) {
                    notFoundIds.add(materialId);
                    continue;
                }
                
                MaterialMetadata metadata = metadataMap.get(material.getFileId());
                if (metadata == null) {
                    notFoundIds.add(materialId);
                    continue;
                }
                
                MaterialMetadataItem item = buildMetadataItem(material, metadata, request);
                metadataList.add(item);
                
            } catch (Exception e) {
                log.warn("组装素材元数据失败 - 素材ID: {}, 错误: {}", materialId, e.getMessage());
                failedIds.put(materialId, e.getMessage());
            }
        }
        
        return BatchMaterialMetadataResponse.builder()
            .metadataList(metadataList)
            .notFoundMaterialIds(notFoundIds)
            .failedMaterialIds(failedIds)
            .totalRequested(materialIds.size())
            .successCount(metadataList.size())
            .notFoundCount(notFoundIds.size())
            .failedCount(failedIds.size())
            .successRate((double) metadataList.size() / materialIds.size())
            .queryTimeMs(mysqlTime + mongodbTime)
            .mysqlQueryTimeMs(mysqlTime)
            .mongodbQueryTimeMs(mongodbTime)
            .queryExecutedAt(LocalDateTime.now())
            .requestId(requestId)
            .cacheUsed(false)
            .responseVersion("v1.0")
            .build();
    }
    
    /**
     * 构建单个素材元数据项
     */
    private MaterialMetadataItem buildMetadataItem(Material material, MaterialMetadata metadata, 
                                                  BatchMaterialMetadataRequest request) {
        MaterialMetadataItem.MaterialMetadataItemBuilder builder = MaterialMetadataItem.builder()
            .materialId(material.getMid())
            .fileId(material.getFileId())
            .materialName(material.getMaterialName())
            .materialType(material.getMaterialType());
        
        // 基础文件信息
        if (Boolean.TRUE.equals(request.getIncludeBasicInfo()) && metadata.getBasicInfo() != null) {
            MaterialMetadata.FileBasicInfo basicInfo = metadata.getBasicInfo();
            builder.md5Hash(basicInfo.getMd5Hash())
                   .fileExtension(basicInfo.getFileExtension())
                   .mimeType(basicInfo.getMimeType())
                   .fileSize(basicInfo.getFileSize());
        }
        
        // 文件状态信息
        builder.fileStatus(material.getFileStatus())
               .fileStatusDesc(material.getFileStatusDescription())
               .processProgress(calculateProcessProgress(material.getFileStatus()));
        
        // 预览URL
        builder.previewUrl("/file/api/file/preview/" + material.getFileId())
               .streamUrl("/file/api/file/stream/" + material.getFileId());
        
        // 缩略图信息
        if (Boolean.TRUE.equals(request.getIncludeThumbnails()) && metadata.getThumbnails() != null 
            && metadata.getThumbnails().getPrimaryThumbnail() != null) {
            builder.thumbnailUrl(metadata.getThumbnails().getPrimaryThumbnail().getStorageUrl());
        }
        
        // 图片元数据
        if (Boolean.TRUE.equals(request.getIncludeImageMetadata()) && 
            "IMAGE".equals(material.getMaterialType()) && metadata.getImageMetadata() != null) {
            builder.imageMetadata(buildImageMetadata(metadata.getImageMetadata()));
        }
        
        // 视频元数据
        if (Boolean.TRUE.equals(request.getIncludeVideoMetadata()) && 
            "VIDEO".equals(material.getMaterialType()) && metadata.getVideoMetadata() != null) {
            builder.videoMetadata(buildVideoMetadata(metadata.getVideoMetadata()));
        }
        
        // GIF元数据 (特殊处理)
        if (Boolean.TRUE.equals(request.getIncludeImageMetadata()) && 
            "IMAGE".equals(material.getMaterialType()) && 
            "gif".equalsIgnoreCase(material.getFileExtension())) {
            builder.gifMetadata(buildGifMetadata(metadata));
        }
        
        // 元数据状态信息
        builder.analysisStatus(metadata.getAnalysisStatus())
               .metadataCreatedAt(metadata.getCreatedAt())
               .metadataUpdatedAt(metadata.getUpdatedAt());
        
        return builder.build();
    }
    
    /**
     * 构建图片元数据
     */
    private MaterialMetadataItem.ImageMetadata buildImageMetadata(MaterialMetadata.ImageMetadata source) {
        MaterialMetadataItem.ImageMetadata.ImageMetadataBuilder builder = 
            MaterialMetadataItem.ImageMetadata.builder()
                .width(source.getWidth())
                .height(source.getHeight())
                .colorDepth(source.getColorDepth())
                .colorSpace(source.getColorSpace())
                .hasAlpha(source.getHasAlpha())
                .dpiHorizontal(source.getDpiHorizontal())
                .dpiVertical(source.getDpiVertical());
        
        // EXIF信息
        if (source.getExifInfo() != null) {
            builder.orientation(source.getExifInfo().getOrientation())
                   .cameraModel(source.getExifInfo().getCameraModel())
                   .dateTaken(source.getExifInfo().getDateTaken());
        }
        
        return builder.build();
    }
    
    /**
     * 构建视频元数据
     */
    private MaterialMetadataItem.VideoMetadata buildVideoMetadata(MaterialMetadata.VideoMetadata source) {
        return MaterialMetadataItem.VideoMetadata.builder()
            .width(source.getVideoWidth())
            .height(source.getVideoHeight())
            .durationMs(source.getVideoDuration() != null ? source.getVideoDuration() * 1000L : null) // 秒→毫秒
            .frameRate(source.getFrameRate())
            .bitrate(source.getVideoBitrate())
            .videoCodec(source.getVideoCodec())
            .aspectRatio(source.getAspectRatio())
            .containerFormat(source.getContainerFormat())
            .audioCodec(source.getAudioStream() != null ? source.getAudioStream().getAudioCodec() : null)
            .audioSampleRate(source.getAudioStream() != null ? source.getAudioStream().getSampleRate() : null)
            .audioChannels(source.getAudioStream() != null ? source.getAudioStream().getChannels() : null)
            .build();
    }
    
    /**
     * 构建GIF元数据
     */
    private MaterialMetadataItem.GifMetadata buildGifMetadata(MaterialMetadata metadata) {
        // GIF信息存储在imageMetadata中
        MaterialMetadata.ImageMetadata imgMeta = metadata.getImageMetadata();
        if (imgMeta == null) {
            log.debug("GIF文件缺少图片元数据信息 - 返回默认GIF元数据");
            return createDefaultGifMetadata();
        }
        
        return MaterialMetadataItem.GifMetadata.builder()
            .width(imgMeta.getWidth())
            .height(imgMeta.getHeight())
            .isAnimated(imgMeta.getIsAnimated() != null ? imgMeta.getIsAnimated() : true) // 默认为动画
            .frameCount(imgMeta.getFrameCount())
            .durationMs(imgMeta.getAnimationDuration()) // 已经是毫秒单位
            .loopCount(imgMeta.getLoopCount() != null ? imgMeta.getLoopCount() : 0) // 默认无限循环
            .averageFrameDelayMs(imgMeta.getAverageFrameDelay() != null ? 
                                imgMeta.getAverageFrameDelay().intValue() : null)
            .build();
    }
    
    /**
     * 创建默认GIF元数据（当分析数据不完整时）
     */
    private MaterialMetadataItem.GifMetadata createDefaultGifMetadata() {
        return MaterialMetadataItem.GifMetadata.builder()
            .width(null)
            .height(null)
            .isAnimated(true) // GIF文件默认假设为动画
            .frameCount(null) // 未知帧数
            .durationMs(null) // 未知时长
            .loopCount(0) // 默认无限循环
            .averageFrameDelayMs(null) // 未知帧延迟
            .build();
    }
    
    /**
     * 计算处理进度
     */
    private Integer calculateProcessProgress(Integer fileStatus) {
        if (fileStatus == null) {
            return null;
        }
        return switch (fileStatus) {
            case 1 -> 100; // 已完成
            case 2 -> 65;  // 处理中
            case 3 -> 0;   // 失败
            default -> null;
        };
    }
    
    /**
     * 构建空响应
     */
    private BatchMaterialMetadataResponse buildEmptyResponse(String reason, StopWatch stopWatch) {
        return BatchMaterialMetadataResponse.builder()
            .metadataList(new ArrayList<>())
            .notFoundMaterialIds(new ArrayList<>())
            .failedMaterialIds(new HashMap<>())
            .totalRequested(0)
            .successCount(0)
            .notFoundCount(0)
            .failedCount(0)
            .successRate(0.0)
            .queryTimeMs(stopWatch.getTotalTimeMillis())
            .queryExecutedAt(LocalDateTime.now())
            .requestId(generateRequestId())
            .cacheUsed(false)
            .responseVersion("v1.0")
            .build();
    }
    
    /**
     * 构建错误响应
     */
    private BatchMaterialMetadataResponse buildErrorResponse(List<Long> materialIds, String errorMsg, StopWatch stopWatch) {
        Map<Long, String> failedIds = materialIds.stream()
            .collect(Collectors.toMap(Function.identity(), id -> errorMsg));
            
        return BatchMaterialMetadataResponse.builder()
            .metadataList(new ArrayList<>())
            .notFoundMaterialIds(new ArrayList<>())
            .failedMaterialIds(failedIds)
            .totalRequested(materialIds.size())
            .successCount(0)
            .notFoundCount(0)
            .failedCount(materialIds.size())
            .successRate(0.0)
            .queryTimeMs(stopWatch.getTotalTimeMillis())
            .queryExecutedAt(LocalDateTime.now())
            .requestId(generateRequestId())
            .cacheUsed(false)
            .responseVersion("v1.0")
            .build();
    }
    
    /**
     * 生成请求ID
     */
    private String generateRequestId() {
        return "req_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
    
    /**
     * 更新性能统计
     */
    private void updatePerformanceStats(int materialCount, long timeMs) {
        totalQueries.incrementAndGet();
        totalQueryTime.addAndGet(timeMs);
        totalMaterialsQueried.addAndGet(materialCount);
    }
}