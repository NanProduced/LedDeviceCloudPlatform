package org.nan.cloud.program.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.nan.cloud.program.enums.ProgramApprovalStatusEnum;
import org.nan.cloud.program.enums.ProgramStatusEnum;
import org.nan.cloud.program.enums.VsnGenerationStatusEnum;

import java.time.LocalDateTime;

/**
 * 节目信息表 - MySQL实体
 * 存储节目的基础元数据信息
 */
@Data
@TableName("program")
public class ProgramDO {
    
    /**
     * 节目ID（主键）
     * 使用Long类型自增主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 节目名称
     */
    @TableField("name")
    private String name;
    
    /**
     * 节目描述
     */
    @TableField("description")
    private String description;
    
    /**
     * 节目状态
     * 使用ProgramStatusEnum枚举
     */
    @TableField("program_status")
    private ProgramStatusEnum programStatus;
    
    /**
     * 审核状态
     * 使用ProgramApprovalStatusEnum枚举
     */
    @TableField("approval_status")
    private ProgramApprovalStatusEnum approvalStatus;
    
    /**
     * 版本号
     * 从1开始，每次编辑创建新记录时递增
     */
    @TableField("version")
    private Integer version;
    
    /**
     * 原始节目ID
     * 指向版本号为1的原始节目，用于版本链管理
     * 原始节目此字段为null或自身ID
     */
    @TableField("source_program_id")
    private Long sourceProgramId;
    
    /**
     * 是否为原始节目
     * true=版本1的原始节目，false=基于原始节目的编辑版本
     */
    @TableField("is_source_program")
    private Boolean isSourceProgram;
    
    /**
     * 节目宽度（像素）
     */
    @TableField("width")
    private Integer width;
    
    /**
     * 节目高度（像素）
     */
    @TableField("height")
    private Integer height;
    
    /**
     * 节目时长（毫秒）
     * 计算得出的总播放时长
     */
    @TableField("duration")
    private Long duration;
    
    /**
     * 节目缩略图URL
     */
    @TableField("thumbnail_url")
    private String thumbnailUrl;
    
    /**
     * 使用次数
     * 统计被引用的次数（不涉及具体发布逻辑）
     */
    @TableField("usage_count")
    private Integer usageCount;
    
    /**
     * VSN文件ID
     * 生成的VSN文件的唯一标识
     */
    @TableField("vsn_file_id")
    private String vsnFileId;
    
    /**
     * VSN文件路径
     * VSN文件在文件系统或对象存储中的完整路径
     */
    @TableField("vsn_file_path")
    private String vsnFilePath;
    
    /**
     * VSN生成状态
     * 跟踪VSN文件的生成进度和状态
     */
    @TableField("vsn_generation_status")
    private VsnGenerationStatusEnum vsnGenerationStatus;
    
    /**
     * VSN生成错误信息
     * 当VSN生成失败时，记录详细的错误信息用于排查
     */
    @TableField("vsn_generation_error")
    private String vsnGenerationError;
    
    /**
     * 所属组织ID
     */
    @TableField("org_id")
    private Long oid;

    /**
     * 所属用户组ID
     * 节目归属于用户组，不归属于用户
     */
    @TableField("user_group_id")
    private Long ugid;
    
    /**
     * 创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    
    /**
     * 更新时间
     */
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
    
    /**
     * 创建者用户ID
     */
    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private Long createdBy;
    
    /**
     * 更新者用户ID
     */
    @TableField(value = "updated_by", fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
    
    /**
     * 逻辑删除标记
     * 0=未删除, 1=已删除
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}