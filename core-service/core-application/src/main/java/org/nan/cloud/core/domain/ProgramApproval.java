package org.nan.cloud.core.domain;

import lombok.Data;
import org.nan.cloud.program.enums.ProgramApprovalStatusEnum;

import java.time.LocalDateTime;

/**
 * 节目审核记录领域类
 * 记录节目审核的完整流程和状态变更
 */
@Data
public class ProgramApproval {
    
    /**
     * 审核记录ID
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
     * 审核状态
     */
    private ProgramApprovalStatusEnum status;
    
    /**
     * 申请审核时间
     */
    private LocalDateTime appliedTime;
    
    /**
     * 审核完成时间
     */
    private LocalDateTime reviewedTime;
    
    /**
     * 审核人员ID
     */
    private Long reviewerId;
    
    /**
     * 审核人员姓名
     */
    private String reviewerName;
    
    /**
     * 审核意见
     */
    private String reviewComment;
    
    /**
     * 拒绝原因
     */
    private String rejectionReason;
    
    /**
     * 所属组织ID
     */
    private Long oid;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
    
    /**
     * 创建者用户ID
     */
    private Long createdBy;
}