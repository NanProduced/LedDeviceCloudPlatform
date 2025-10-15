package org.nan.cloud.program.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 节目素材引用关系表 - MySQL实体
 * 记录节目使用了哪些素材，用于素材依赖管理
 */
@Data
@TableName("program_material_ref")
public class ProgramMaterialRefDO {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 节目ID
     */
    @TableField("program_id")
    private Long programId;
    
    /**
     * 节目版本号
     */
    @TableField("program_version")
    private Integer programVersion;
    
    /**
     * 素材ID（关联material表）
     */
    @TableField("material_id")
    private Long materialId;
    
    /**
     * 素材类型
     * IMAGE, VIDEO, AUDIO, TEXT等
     */
    @TableField("material_type")
    private String materialType;
    
    /**
     * 在节目中的使用序号
     * 同一素材可能在节目中多次使用
     */
    @TableField("usage_index")
    private Integer usageIndex;
    
    /**
     * 素材在VSN中的路径
     * 记录素材在VSN结构中的位置，便于查找和替换
     */
    @TableField("vsn_path")
    private String vsnPath;
    
    /**
     * 创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    
    /**
     * 创建者用户ID
     */
    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private Long createdBy;
}