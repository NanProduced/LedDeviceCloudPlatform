package org.nan.cloud.core.api.DTO.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nan.cloud.program.enums.ProgramApprovalStatusEnum;

import java.io.Serial;
import java.io.Serializable;

/**
 * 我发起的审核申请查询请求参数
 * 业务逻辑：创建者为当前用户ID的所有审核记录
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InitiatedApprovalsByMeRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 关键词搜索（可选）
     * 支持节目名称、节目描述搜索
     */
    private String keyword;

    /**
     * 审核状态过滤（可选）
     * 支持PENDING, APPROVED, REJECTED等状态过滤
     */
    private ProgramApprovalStatusEnum status;

    /**
     * 节目状态过滤（可选）
     * 可以按节目本身的状态进行过滤
     */
    private String programStatus;

    /**
     * 时间范围过滤 - 开始时间（可选）
     * 格式：yyyy-MM-dd HH:mm:ss
     */
    private String startTime;

    /**
     * 时间范围过滤 - 结束时间（可选）
     * 格式：yyyy-MM-dd HH:mm:ss
     */
    private String endTime;
}