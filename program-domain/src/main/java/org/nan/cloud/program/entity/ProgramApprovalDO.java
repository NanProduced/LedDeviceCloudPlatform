package org.nan.cloud.program.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.nan.cloud.program.enums.ProgramApprovalStatusEnum;

import java.time.LocalDateTime;

/**
 * 节目审核记录表 - MySQL实体
 * 记录节目的审核流程和历史
 */
@Data
@TableName("program_approval")
public class ProgramApprovalDO {
    
    /**
     * 审核记录ID（主键）
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
     * 审核状态
     */
    @TableField("status")
    private ProgramApprovalStatusEnum status;
    
    /**
     * 申请审核时间
     */
    @TableField("applied_time")
    private LocalDateTime appliedTime;
    
    /**
     * 审核完成时间
     */
    @TableField("reviewed_time")
    private LocalDateTime reviewedTime;
    
    /**
     * 审核者用户ID
     */
    @TableField("reviewer_id")
    private Long reviewerId;
    
    
    /**
     * 审核意见/备注
     */
    @TableField("review_comment")
    private String reviewComment;
    
    /**
     * 拒绝原因
     * 当审核状态为REJECTED时填写
     */
    @TableField("rejection_reason")
    private String rejectionReason;
    
    /**
     * 所属组织ID
     */
    @TableField("org_id")
    private Long orgId;
    
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
     * 创建者用户ID（申请人）
     */
    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private Long createdBy;
}