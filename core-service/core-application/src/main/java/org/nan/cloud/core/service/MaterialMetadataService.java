package org.nan.cloud.core.service;

import org.nan.cloud.core.api.DTO.req.BatchMaterialMetadataRequest;
import org.nan.cloud.core.api.DTO.res.BatchMaterialMetadataResponse;
import org.nan.cloud.core.api.DTO.res.MaterialMetadataItem;

/**
 * 素材元数据服务接口
 * 
 * 专门为节目编辑器设计的元数据服务:
 * - 高效的批量查询，避免N+1问题
 * - 列表查询与元数据查询分离，优化性能
 * - 支持可配置的元数据返回，减少不必要的数据传输
 * - 完善的错误处理和性能监控
 * 
 * 设计原则:
 * 1. 列表查询(MaterialService) → 快速筛选 (MySQL only)
 * 2. 元数据查询(本服务) → 批量详情 (MongoDB batch)
 * 3. 预览接口(FileService) → 统一访问 (File processing)
 * 
 * @author Nan
 * @since 1.0.0
 */
public interface MaterialMetadataService {
    
    /**
     * 批量查询素材元数据
     * 
     * 高性能批量查询设计:
     * - Step1: 批量查询Material基础信息(MySQL IN查询)
     * - Step2: 提取fileId，批量查询MongoDB元数据  
     * - Step3: 内存中组装响应，支持部分成功
     * 
     * @param request 批量查询请求
     * @return 批量元数据响应，包含成功/失败统计
     */
    BatchMaterialMetadataResponse batchGetMaterialMetadata(BatchMaterialMetadataRequest request);
    
    /**
     * 根据素材ID查询单个元数据
     * 
     * 用于单个素材的详情获取，内部调用批量查询逻辑
     * 
     * @param materialId 素材ID
     * @return 元数据详情，不存在返回null
     */
    MaterialMetadataItem getMaterialMetadata(Long materialId);
    
    /**
     * 根据素材ID查询单个元数据(带配置)
     * 
     * @param materialId 素材ID
     * @param includeThumbnails 是否包含缩略图
     * @param includeBasicInfo 是否包含基础信息
     * @param includeImageMetadata 是否包含图片元数据
     * @param includeVideoMetadata 是否包含视频元数据
     * @return 元数据详情，不存在返回null
     */
    MaterialMetadataItem getMaterialMetadata(Long materialId, 
                                           Boolean includeThumbnails,
                                           Boolean includeBasicInfo,
                                           Boolean includeImageMetadata,
                                           Boolean includeVideoMetadata);
    
    /**
     * 检查素材是否存在元数据
     * 
     * 轻量级检查方法，用于验证元数据完整性
     * 
     * @param materialId 素材ID
     * @return 是否存在元数据
     */
    boolean hasMetadata(Long materialId);
    
    /**
     * 获取元数据查询性能统计
     * 
     * 用于监控和优化查询性能
     * 
     * @return 性能统计信息
     */
    String getPerformanceStats();
}