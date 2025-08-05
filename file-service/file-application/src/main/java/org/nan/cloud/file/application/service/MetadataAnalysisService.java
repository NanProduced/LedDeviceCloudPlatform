package org.nan.cloud.file.application.service;

import org.nan.cloud.file.application.domain.FileInfo;
import org.nan.cloud.file.application.domain.MaterialMetadata;

/**
 * 文件元数据分析服务接口
 * 
 * 负责分析各种类型文件的技术元数据：
 * - 图片：EXIF信息、分辨率、色彩空间等
 * - 视频：编码格式、分辨率、时长、帧率等  
 * - 音频：编码格式、采样率、比特率等
 * - 文档：页数、作者、创建时间等
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface MetadataAnalysisService {

    /**
     * 分析文件元数据
     * 
     * @param fileInfo 文件信息
     * @param taskId 关联的任务ID
     * @return 完整的元数据对象
     */
    MaterialMetadata analyzeMetadata(FileInfo fileInfo, String taskId);

    /**
     * 检查文件类型是否支持元数据分析
     * 
     * @param mimeType MIME类型
     * @return 是否支持
     */
    boolean isSupported(String mimeType);

    /**
     * 获取文件类型分类
     * 
     * @param mimeType MIME类型
     * @return 文件类型（IMAGE/VIDEO/AUDIO/DOCUMENT）
     */
    String getFileTypeCategory(String mimeType);
}