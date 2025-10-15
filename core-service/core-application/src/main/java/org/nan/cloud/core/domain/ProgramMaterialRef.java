package org.nan.cloud.core.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 节目素材引用关系领域类
 * 记录节目中使用的素材引用关系
 */
@Data
public class ProgramMaterialRef {
    
    /**
     * 引用关系ID
     */
    private Long id;
    
    /**
     * 节目ID
     */
    private Long programId;
    
    /**
     * 节目版本号
     */
    private Integer programVersion;
    
    /**
     * 素材ID
     */
    private Long materialId;
    
    /**
     * 素材类型
     */
    private String materialType;
    
    /**
     * 在节目中的使用序号
     */
    private Integer usageIndex;
    
    /**
     * VSN中的路径
     */
    private String vsnPath;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 创建者用户ID
     */
    private Long createdBy;
}