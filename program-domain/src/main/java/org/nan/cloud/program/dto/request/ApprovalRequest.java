package org.nan.cloud.program.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 节目审核请求DTO
 * 用于审核操作的请求参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequest {
    
    /**
     * 审核意见/备注
     * 审核通过或拒绝时的说明
     */
    private String comment;
    
    /**
     * 拒绝原因
     * 当审核拒绝时的具体原因说明
     */
    private String rejectionReason;
}