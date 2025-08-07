package org.nan.cloud.program.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.nan.cloud.program.enums.ProgramApprovalStatusEnum;
import org.nan.cloud.program.enums.ProgramStatusEnum;

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
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;
    
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
    @TableField("status")
    private ProgramStatusEnum status;
    
    /**
     * 审核状态
     * 使用ProgramApprovalStatusEnum枚举
     */
    @TableField("approval_status")
    private ProgramApprovalStatusEnum approvalStatus;
    
    /**
     * 当前版本号
     * 从1开始，每次编辑递增
     */
    @TableField("current_version")
    private Integer currentVersion;
    
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
     * 被终端或终端组使用的次数统计
     */
    @TableField("usage_count")
    private Integer usageCount;
    
    /**
     * 所属组织ID
     */
    @TableField("org_id")
    private String orgId;
    
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
    private String createdBy;
    
    /**
     * 更新者用户ID
     */
    @TableField(value = "updated_by", fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;
    
    /**
     * 逻辑删除标记
     * 0=未删除, 1=已删除
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}