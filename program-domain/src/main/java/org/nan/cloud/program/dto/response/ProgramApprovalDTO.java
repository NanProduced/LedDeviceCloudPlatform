package org.nan.cloud.program.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.nan.cloud.program.enums.ProgramApprovalStatusEnum;

import java.time.LocalDateTime;

/**
 * 节目审核记录响应DTO
 */
@Data
public class ProgramApprovalDTO {
    
    /**
     * 审核记录ID
     */
    private Long id;
    
    /**
     * 节目ID
     */
    private Long programId;
    
    /**
     * 节目名称
     */
    private String programName;
    
    /**
     * 节目版本号
     */
    private Integer programVersion;
    
    /**
     * 审核状态
     */
    private ProgramApprovalStatusEnum status;
    
    /**
     * 审核状态显示名称
     */
    private String statusName;
    
    /**
     * 申请审核时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime appliedTime;
    
    /**
     * 审核完成时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewedTime;
    
    /**
     * 审核者用户ID
     */
    private Long reviewerId;
    
    /**
     * 审核者姓名
     */
    private String reviewerName;
    
    /**
     * 审核意见/备注
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
    
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;
    
    /**
     * 创建者用户ID（申请人）
     */
    private Long createdBy;
    
    /**
     * 申请人姓名
     */
    private String applicantName;
}