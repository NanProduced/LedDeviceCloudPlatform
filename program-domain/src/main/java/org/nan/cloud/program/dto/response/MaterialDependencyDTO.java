package org.nan.cloud.program.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 素材依赖信息DTO
 * 描述节目与素材之间的依赖关系
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MaterialDependencyDTO {
    
    /**
     * 节目ID
     */
    private Long programId;
    
    /**
     * 节目名称
     */
    private String programName;
    
    /**
     * 素材ID
     */
    private Long materialId;
    
    /**
     * 素材名称
     */
    private String materialName;
    
    /**
     * 素材类型（VSN类型码）
     */
    private Integer materialType;
    
    /**
     * 素材分类
     */
    private String materialCategory;
    
    /**
     * 素材文件路径
     */
    private String materialPath;
    
    /**
     * 素材访问URL
     */
    private String materialUrl;
    
    /**
     * 素材MD5值
     */
    private String materialMd5;
    
    /**
     * 素材文件大小（字节）
     */
    private Long materialSize;
    
    /**
     * 依赖关系创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 素材是否可用
     */
    private Boolean isAvailable;
    
    /**
     * 不可用原因
     */
    private String unavailableReason;
}