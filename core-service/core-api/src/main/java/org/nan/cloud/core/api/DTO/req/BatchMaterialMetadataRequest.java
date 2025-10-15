package org.nan.cloud.core.api.DTO.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 批量查询素材元数据请求
 * 
 * 设计理念:
 * - 列表查询保持高性能(MySQL only)
 * - 元数据查询按需批量获取(MongoDB batch)
 * - 支持可配置的返回字段控制
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "批量查询素材元数据请求")
public class BatchMaterialMetadataRequest {
    
    /**
     * 素材ID列表
     */
    @NotEmpty(message = "素材ID列表不能为空")
    @Size(max = 100, message = "单次最多查询100个素材")
    @Schema(description = "素材ID列表", required = true, example = "[1, 2, 3]")
    private List<Long> materialIds;
    
    /**
     * 是否包含缩略图信息
     */
    @Builder.Default
    @Schema(description = "是否包含缩略图信息", example = "true")
    private Boolean includeThumbnails = true;
    
    /**
     * 是否包含基础文件信息
     */
    @Builder.Default
    @Schema(description = "是否包含基础文件信息(MD5、文件大小等)", example = "true")
    private Boolean includeBasicInfo = true;
    
    /**
     * 是否包含图片专属元数据
     */
    @Builder.Default
    @Schema(description = "是否包含图片专属元数据(宽高、EXIF等)", example = "true")
    private Boolean includeImageMetadata = true;
    
    /**
     * 是否包含视频专属元数据
     */
    @Builder.Default
    @Schema(description = "是否包含视频专属元数据(分辨率、时长、编码等)", example = "true")
    private Boolean includeVideoMetadata = true;
    
    /**
     * 是否包含AI分析结果
     * 当前还没有，拓展性字段
     */
    @Builder.Default
    @Schema(description = "是否包含AI分析结果(图像识别、内容安全等)", example = "false", hidden = true)
    private Boolean includeAiAnalysis = false;
    
    /**
     * 是否包含LED业务元数据
     * 当前还没有，拓展性字段
     */
    @Builder.Default
    @Schema(description = "是否包含LED业务元数据(推荐设置、兼容性等)", example = "false", hidden = true)
    private Boolean includeLedMetadata = false;
}