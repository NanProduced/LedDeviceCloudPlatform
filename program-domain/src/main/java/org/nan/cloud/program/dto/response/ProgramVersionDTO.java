package org.nan.cloud.program.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nan.cloud.program.enums.ProgramStatusEnum;
import org.nan.cloud.program.enums.VsnGenerationStatusEnum;

import java.time.LocalDateTime;

/**
 * 节目版本信息DTO
 * 用于版本控制相关功能的数据传输
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgramVersionDTO {
    
    /**
     * 节目ID
     */
    private Long id;
    
    /**
     * 节目名称
     */
    private String name;
    
    /**
     * 节目描述
     */
    private String description;
    
    /**
     * 版本号
     */
    private Integer version;
    
    /**
     * 原始节目ID（版本链的根节目ID）
     */
    private Long sourceProgramId;
    
    /**
     * 是否为原始节目
     */
    private Boolean isSourceProgram;
    
    /**
     * 节目状态
     */
    private ProgramStatusEnum status;
    
    /**
     * VSN生成状态
     */
    private VsnGenerationStatusEnum vsnGenerationStatus;
    
    /**
     * VSN文件路径
     */
    private String vsnFilePath;
    
    /**
     * 节目宽度（像素）
     */
    private Integer width;
    
    /**
     * 节目高度（像素）
     */
    private Integer height;
    
    /**
     * 节目时长（毫秒）
     */
    private Long duration;
    
    /**
     * 缩略图URL
     */
    private String thumbnailUrl;
    
    /**
     * 使用次数
     */
    private Integer usageCount;
    
    /**
     * 总版本数（仅在版本链查询时填充）
     */
    private Integer totalVersions;
    
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
     * 更新时间
     */
    private LocalDateTime updatedTime;
    
    /**
     * 组织ID
     */
    private Long oid;
    
    /**
     * 用户组ID
     */
    private Long ugid;
    
    // ===== 版本控制特有字段 =====
    
    /**
     * 是否为关键版本
     */
    private Boolean isKeyVersion;
    
    /**
     * 关键版本类型（INITIAL, PUBLISHED, MILESTONE等）
     */
    private String keyVersionType;
    
    /**
     * 版本变更摘要
     */
    private String changeSummary;
    
    /**
     * 版本标签
     */
    private java.util.List<String> tags;
}