package org.nan.cloud.core.api.DTO.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nan.cloud.program.enums.ProgramApprovalStatusEnum;

import java.io.Serial;
import java.io.Serializable;

/**
 * 待我审核的节目查询请求参数
 * 业务逻辑：节目的ugid属于当前用户组层级，且审核状态为PENDING
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingApprovalForMeRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 关键词搜索（可选）
     * 支持节目名称、节目描述搜索
     */
    private String keyword;

    /**
     * 审核状态过滤（可选）
     * 默认为PENDING，支持过滤其他状态
     */
    private ProgramApprovalStatusEnum status;

    /**
     * 节目状态过滤（可选）
     * 可以按节目本身的状态进行过滤
     */
    private String programStatus;
}